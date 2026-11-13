# Design

## Context

See proposal.md for motivation and decisions.md for every settled choice (D-numbers below refer to it).

Current state: Gradle 9.5.1, Kotlin 2.4.20, AGP 9.1.1, Compose Multiplatform 1.12.1. One KMP module `:shared` (namespace `com.plcoding.cardiganapp.shared`, JVM 11, framework `Shared`) and the `:androidApp` shell that applies AGP plugins directly. `iosApp` builds the framework through `:shared:embedAndSignAppleFrameworkForXcode`. A JDK 17 is installed locally, so `jvmToolchain(17)` in `build-logic` needs no toolchain resolver.

Constraints: CLAUDE.md wins over the `kmp-module-structure` skill. That means no desktop target, no `iosX64`, no Room, the `buildkonfig` plugin reads base URLs instead of `API_KEY`, and the package root is `com.plcoding.cardiganapp`.

## Goals / Non-Goals

**Goals:**
- Every module from CLAUDE.md's architecture exists and builds on Android and both iOS targets.
- Module build files are a few lines each. Everything shared lives in convention plugins.
- The core utilities compile, are covered by tests on both platforms, and match the shared API conventions.
- All four gates and the `xcodebuild` simulator build pass at every commit.

**Non-Goals:**
- Any Compose UI beyond an empty `App()`. No theme, navigation graph or routes.
- Tests for `ObserveAsEvents` and `UiText.Resource`. Both need Compose UI testing or resources. They come with the first screen that uses them, through `*UiTest`.
- Pinning library versions in this document. Versions are chosen at apply time (latest stable compatible with Kotlin 2.4.20 / CMP 1.12.1) and recorded in the catalog.

## Decisions

### 1. build-logic layout

`build-logic/` is an included build (`pluginManagement { includeBuild("build-logic") }`) with one `:convention` project. It reads `../gradle/libs.versions.toml` as `libs`. The plugins depend `compileOnly` on the AGP, Kotlin, Compose and BuildKonfig Gradle plugin artifacts. Package `com.plcoding.cardiganapp.convention`.

| Plugin id (`com.plcoding.cardiganapp.convention.*`) | Applies | Used by |
|---|---|---|
| `kmp.library` | `com.android.kotlin.multiplatform.library`, Kotlin MPP, serialization; android target + `iosArm64` + `iosSimulatorArm64`; host and device test builders; kotlinx-serialization-json; test stack in `commonTest` | core/feature `domain` and `data`, `:core:domain`, `:core:data` |
| `cmp.library` | `kmp.library` + Compose compiler + `org.jetbrains.compose`; compose runtime/foundation/ui/material3/resources/ui-tooling-preview; `androidMainImplementation` ui-tooling for previews | `:core:presentation`, `:core:designsystem` |
| `cmp.feature` | `cmp.library` + `:core:presentation`, `:core:designsystem`, Koin (BOM, compose, compose-viewmodel, android), lifecycle viewmodel / runtime-compose / viewmodel-savedstate, savedstate, navigation-compose | `:feature:*:presentation` |
| `cmp.application` | Same targets as `kmp.library` plus Compose; iOS framework `ComposeApp`, static | `:composeApp` |
| `android.application` | `com.android.application`; app id, SDK levels and version from `project*` catalog keys; JVM 17 | `:androidApp` |
| `android.application.compose` | `android.application` + Compose compiler; `debugImplementation` ui-tooling | `:androidApp` |
| `buildkonfig` | `com.codingfeline.buildkonfig` with per-target `BASE_URL` (see 4) | `:core:data` |

Shared helpers: `Project.libs`, `pathToPackageName()` (`com.plcoding.cardiganapp` + module path, e.g. `com.plcoding.cardiganapp.feature.boards.data`), `pathToFrameworkName()`, `configureKotlinMultiplatform()`, `configureKotlin()`.

Deviations from the skill, all from decisions.md or CLAUDE.md: no `configureDesktopTarget`, no `iosX64`, no `room` plugin, no desugaring (D21), no material-icons (D4), JVM 17 everywhere (D20). `android.application.compose` uses the JetBrains `compose-uiTooling` artifact already in the catalog instead of adding the AndroidX Compose BOM. That avoids two Compose version sources in one app.

`kmp.library` sets compiler flags `-Xexpect-actual-classes` and opt-ins for `kotlin.time.ExperimentalTime` and `kotlin.uuid.ExperimentalUuidApi`.

**Alternatives:** keep per-module build files (rejected: duplication across 19 modules); a precompiled script plugin layout (rejected: the skill's binary plugins are the project standard).

### 2. Module graph

`settings.gradle.kts` enables `TYPESAFE_PROJECT_ACCESSORS` and includes `:composeApp`, `:androidApp`, the four core modules and the 15 feature modules.

| Module | Plugin(s) | Project dependencies |
|---|---|---|
| `:core:domain` | `kmp.library` | — (+ kotlinx-coroutines-core) |
| `:core:data` | `kmp.library`, `buildkonfig` | `core:domain` (+ Ktor, Koin core) |
| `:core:presentation` | `cmp.library` | `core:domain` (+ lifecycle-runtime-compose, coroutines) |
| `:core:designsystem` | `cmp.library` | — |
| `:feature:<f>:domain` | `kmp.library` | `core:domain` |
| `:feature:<f>:data` | `kmp.library` | `feature:<f>:domain`, `core:domain`, `core:data` (+ Ktor, Koin core, per D3) |
| `:feature:<f>:presentation` | `cmp.feature` | `feature:<f>:domain`, `core:domain` (+ core:presentation/designsystem via plugin) |
| `:composeApp` | `cmp.application` | every core and feature module, Koin |
| `:androidApp` | `android.application.compose` | `:composeApp`, activity-compose, koin-android |

`<f>` ∈ `auth`, `boards`, `cards`, `labels`, `transfer`. Feature modules contain only `build.gradle.kts` (D25). Directories `src/commonMain/kotlin` are not created for them, since Git doesn't track empty folders.

### 3. Rename `:shared` → `:composeApp`

Use `git mv shared composeApp` so history follows the files. Then delete the wizard files (D1). `App.kt` becomes an empty `@Composable fun App()`, and `MainViewController.kt` keeps `ComposeUIViewController { App() }`. The namespace becomes `com.plcoding.cardiganapp.composeapp`. Kotlin packages stay `com.plcoding.cardiganapp`.

In `iosApp`: the build phase calls `:composeApp:embedAndSignAppleFrameworkForXcode`. Swift files `import ComposeApp`. `iOSApp.init()` calls `InitKoinKt.doInitKoin(config: nil)`.

### 4. Base URL through BuildKonfig (D5–D7, D26)

The `buildkonfig` plugin reads `local.properties` with `java.util.Properties` through `providers.fileContents`. It avoids AGP's internal `gradleLocalProperties` API, which can break on AGP updates. It requires `BASE_URL_ANDROID` and `BASE_URL_IOS`. If either is missing, it throws `GradleException("Missing BASE_URL_ANDROID in local.properties (e.g. http://10.0.2.2:8080/api/v1)")` (and the same for iOS). It also fails, with `GradleException("BASE_URL_ANDROID in local.properties must be an http(s) URL ending in /api/v1, got <value>")` (and the same for iOS), when a value doesn't start with `http://` or `https://` or doesn't end in `/api/v1` after trimming a trailing `/`.

BuildKonfig needs the field in `defaultConfigs`. It is set to the Android value there and overridden in `targetConfigs` for `android`, `iosArm64` and `iosSimulatorArm64`, so common code sees one `BuildKonfig.BASE_URL`. Package: `com.plcoding.cardiganapp.core.data`.

`constructRoute(route)` only joins: it trims a trailing `/` from the base and adds a leading `/` to the route if missing. Routes that already start with the base URL pass through unchanged.

**Alternative:** `expect val baseUrl` per platform with hard-coded values (rejected: physical devices need per-developer LAN addresses).

### 5. Core domain utilities (D9, D10, D11)

Package `com.plcoding.cardiganapp.core.domain` (subpackages are an apply-time detail):

- `interface Error`, `sealed interface Result<out D, out E : Error>` with `Success` / `Error`, `typealias EmptyResult<E> = Result<Unit, E>`.
- `map`, `onSuccess`, `onFailure`, `asEmptyResult`, as in `kmp-error-handling`.
- `sealed interface DataError : Error { enum class Network : DataError { BAD_REQUEST, UNAUTHORIZED, FORBIDDEN, NOT_FOUND, REQUEST_TIMEOUT, CONFLICT, PAYLOAD_TOO_LARGE, UNSUPPORTED_MEDIA_TYPE, UNPROCESSABLE_ENTITY, TOO_MANY_REQUESTS, NO_INTERNET, SERVER_ERROR, SERVICE_UNAVAILABLE, SERIALIZATION, UNKNOWN } }`. There is no `Local` branch.

`DataError` stays a sealed interface with one branch, so a `Local` branch can be added later without changing call sites.

### 6. Core presentation utilities (D9)

- `UiText` as in `kmp-presentation-mvi` (`DynamicString`, `Resource(StringResource, args)`, `@Composable asString()`, `suspend asStringAsync()`).
- `ObserveAsEvents(flow, key1, key2, onEvent)`: `LaunchedEffect(lifecycleOwner.lifecycle, key1, key2)`, `repeatOnLifecycle(STARTED)`, then `withContext(Dispatchers.Main.immediate) { flow.collect(onEvent) }`. `Main.immediate` makes sure events sent while the composable is going away are not lost. `LocalLifecycleOwner` comes from the JetBrains lifecycle-runtime-compose artifact.

### 7. Core data: HttpClient and safe calls (D9, D11–D14)

- `HttpClientFactory.create(engine: HttpClientEngine): HttpClient` installs `ContentNegotiation` with `Json { ignoreUnknownKeys = true }` and `defaultRequest { contentType(ContentType.Application.Json) }`. No Auth, Logging or timeout plugin. Unknown keys must be ignored per shared `api-conventions` "Unknown fields are ignored".
- `HttpClient.get/post/patch/delete` (`suspend inline reified`) → `safeCall { ... }` → `platformSafeCall` (`expect`/`actual`) → `responseToResult`.
- `responseToResult`: on 2xx, if `T` is `Unit` it returns `Success(Unit)` without reading the body (204 and empty bodies), otherwise `body<T>()`. Error statuses map per D11, including 503 → `SERVICE_UNAVAILABLE` and other 5xx → `SERVER_ERROR`. The body is never parsed for errors yet: problem details `code` handling is deferred, and non-problem bodies from proxies must work the same (shared `api-errors` "Clients handle unknown error codes by status").
- `platformSafeCall` catches, on both platforms: `HttpRequestTimeoutException` → `REQUEST_TIMEOUT`, `SerializationException` and Ktor `ContentConvertException` → `SERIALIZATION` (Ktor wraps body decoding failures in `JsonConvertException`, so catching only `SerializationException` would miss them), any other `Exception` → `UNKNOWN`. `CancellationException` is always rethrown.
  - Android adds `UnknownHostException`, `UnresolvedAddressException`, `ConnectException` → `NO_INTERNET` and `SocketTimeoutException` → `REQUEST_TIMEOUT`.
  - iOS adds `DarwinHttpRequestException` → `NSError.toNetworkError()`. For domain `NSURLErrorDomain`: `NSURLErrorNotConnectedToInternet` (-1009), `NSURLErrorNetworkConnectionLost` (-1005), `NSURLErrorCannotFindHost` (-1003), `NSURLErrorCannotConnectToHost` (-1004), `NSURLErrorDNSLookupFailed` (-1006) → `NO_INTERNET`; `NSURLErrorTimedOut` (-1001) → `REQUEST_TIMEOUT`; every other code → `UNKNOWN`. Any other domain → `UNKNOWN`. This matches Android, where `UnknownHostException`/`ConnectException` → `NO_INTERNET` and `SocketTimeoutException` → `REQUEST_TIMEOUT`. `NSError.toNetworkError()` is a separate internal function, so it can be tested without building a Ktor exception.
  - Since no `HttpTimeout` plugin is installed, the platform exceptions above are the only production timeout path. `HttpRequestTimeoutException` is still caught in case the plugin is added later.
- No `printStackTrace`. Logging goes through Kermit only, and Kermit isn't added yet (D13).

### 8. Koin (D16)

- `:core:data`: `val coreDataModule = module { includes(coreDataPlatformModule); single { HttpClientFactory.create(get()) } }` and `expect val coreDataPlatformModule`, with actuals providing `OkHttp.create()` / `Darwin.create()`.
- `:composeApp` `commonMain`: `fun initKoin(config: KoinAppDeclaration? = null)`, which calls `startKoin { config?.invoke(this); modules(coreDataModule) }`.
- `:androidApp`: `CardiganApplication : Application` calls `initKoin { androidContext(this@CardiganApplication) }` and is registered in the manifest.
- iOS: `iOSApp.init()` calls it exactly once. Koin is never started in `MainViewController`.

### 9. Local network access (D17)

- Main manifest: `<uses-permission android:name="android.permission.INTERNET"/>`.
- `androidApp/src/debug/AndroidManifest.xml` sets `android:networkSecurityConfig="@xml/network_security_config"`. `src/debug/res/xml/network_security_config.xml` has `<base-config cleartextTrafficPermitted="true">`. It's a base config, not a domain list, because physical devices use per-developer LAN addresses. Release has no override, so cleartext stays blocked (the default since API 28; release also gets no config that permits it).
- `Info.plist`: `NSAppTransportSecurity` → `NSAllowsLocalNetworking = YES`.

### 10. Tests (D18, D19)

- `kmp.library` adds to `commonTest`: kotlin-test, AssertK, Turbine, kotlinx-coroutines-test, ktor-client-mock.
- `configureKotlinMultiplatform()` sets up `withHostTestBuilder {}` and `withDeviceTestBuilder { sourceSetTreeName = "test" }`. `tasks.withType<Test>` excludes only `*UiTest`, with `isFailOnNoMatchingTests = false`, so modules without tests pass.
- `:core:domain` `commonTest`: `ResultTest` (map, onSuccess, onFailure, asEmptyResult, chaining, that actions don't run on the wrong branch).
- `:core:presentation` `commonTest`: `UiTextTest` for `DynamicString.asStringAsync()`, including empty, emoji/unicode and format characters such as `%s`, which must pass through unchanged.
- `:core:data` `commonTest`: `SafeCallIntegrationTest` with `MockEngine`. It covers each status named in D11, plus 502 and 504 → `SERVER_ERROR`, and 418 and 451 → `UNKNOWN`, 204 → `Unit`, malformed JSON → `SERIALIZATION`, unknown fields ignored, a non-problem-details error body, `HttpRequestTimeoutException`, cancellation rethrown, PATCH/DELETE methods, and URL joining with and without slashes. `androidHostTest`: java.net exceptions → `NO_INTERNET` / `REQUEST_TIMEOUT`. `iosTest`: one test per code listed in §7, one `NSURLErrorDomain` code not listed (e.g. `NSURLErrorBadURL`, -1000) → `UNKNOWN`, and a foreign domain with code -1009 → `UNKNOWN`.
- Test JSON is shaped by small `@Serializable` test fixtures. No backend DTOs exist yet, and none are invented.

### 11. Commits

These follow CLAUDE.md, with every gate green before each commit:
1. `build: add build-logic convention plugins and module skeleton`: catalog, `build-logic`, rename, all modules with build files, empty `App()`, iOS framework rename. `:core:data` is created without the `buildkonfig` plugin here.
2. `feat(core): add result wrapper and data errors`, with tests.
3. `feat(core): add ui text and observe-as-events`, with tests.
4. `feat(core): add http client and safe call helpers`: buildkonfig, safe calls, factory, network config, with tests.
5. `feat: start koin on android and ios`: Koin modules, `initKoin`, Application, iOS init (+ `xcodebuild`).
6. `docs: update agents.md for module skeleton`. Skill conflicts are reported only, not edited (tasks 7.2).

## Risks / Trade-offs

- [`BASE_URL_*` missing breaks every build, including IDE sync] → Clear error message. Developers are told in the apply summary. Add the keys to `local.properties` before running the commit 4 gates.
- [BuildKonfig with the new `com.android.kotlin.multiplatform.library` target name may not match `android` in `targetConfigs`] → Check during apply by inspecting the generated `BuildKonfig` per target. If the name differs, use the actual target name. The approach doesn't change.
- [Library versions compatible with Kotlin 2.4.20 / AGP 9.1 / Gradle 9.5] → Choose at apply time and verify through the gates. If a library has no compatible release, stop and ask instead of downgrading Kotlin.
- [Unused dependencies in empty feature modules (D2, D3) slow builds slightly] → Accepted by decision.
- [`NSAllowsLocalNetworking` is in the shared `Info.plist`, so release iOS builds keep it] → It only exempts local hosts (unqualified names, `.local`, IP addresses), and public hosts stay HTTPS-only. This matches CLAUDE.md. Raise it again when a production URL is decided.
- [One `BASE_URL` per platform for all build types, so release builds embed the dev HTTP URL and fail every request on Android] → Release builds aren't supported by this change. Per-build-type URLs are settled together with the production URL open item.
- [`ObserveAsEvents` and `UiText.Resource` ship without tests] → Covered later by the first screen's `*UiTest`.
- [Ktor exception types differ across versions] → Covered by the platform tests in decision 10.

## Migration Plan

Developer-local only. Pull, add `BASE_URL_ANDROID` and `BASE_URL_IOS` to `local.properties`, sync Gradle, and clean the Xcode build folder once (the framework name changed). Rollback means reverting the commits. Nothing is deployed.
