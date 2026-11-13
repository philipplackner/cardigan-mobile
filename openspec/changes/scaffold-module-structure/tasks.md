# Tasks

Gates referenced below: `./gradlew :androidApp:assembleDebug testAndroidHostTest iosSimulatorArm64Test :composeApp:linkDebugFrameworkIosSimulatorArm64`, plus `xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -sdk iphonesimulator -destination 'generic/platform=iOS Simulator' build` whenever `iosApp/` changed.

## 1. Version catalog and build-logic

- [x] 1.1 Pick the latest stable versions of Koin, navigation-compose, lifecycle-savedstate, savedstate, kotlinx-serialization, kotlinx-coroutines, Ktor, BuildKonfig, AssertK and Turbine that are compatible with Kotlin 2.4.20, CMP 1.12.1, AGP 9.1.1 and Gradle 9.5.1. If one has no compatible release, stop and ask. Verify: a list of chosen versions with their sources.
- [x] 1.2 Update `gradle/libs.versions.toml`: replace `android-*Sdk` with the `project*` keys (D23, same values), add the libraries from 1.1, the Gradle plugin artifacts (`android-gradlePlugin`, `kotlin-gradlePlugin`, `compose-gradlePlugin`, `composeMultiplatform-gradlePlugin`, `buildkonfig-gradlePlugin`), the `kotlinSerialization` and `buildkonfig` plugins, and `convention-*` plugin aliases with `version = "unspecified"`. Remove the unused wizard entries (`junit`, `kotlin-testJunit`, espresso, appcompat, core-ktx, testExt). Verify: `grep -n "projectMinSdkVersion\|convention-kmp-library" gradle/libs.versions.toml` finds both.
- [x] 1.3 Create `build-logic/settings.gradle.kts`, `build-logic/gradle.properties` and `build-logic/convention/build.gradle.kts` (JDK 17 toolchain, `compileOnly` plugin artifacts, `validatePlugins` strict, plugin registrations under `com.plcoding.cardiganapp.convention.*`). Verify: `./gradlew -p build-logic :convention:validatePlugins` succeeds once 1.4–1.5 exist.
- [x] 1.4 Add the helpers `ProjectExt.kt` (`libs`), `PathUtil.kt` (`pathToPackageName`, `pathToFrameworkName`), `KotlinAndroid.kt` (`configureKotlin`, JVM 17) and `KotlinMultiplatform.kt` (android target with namespace/SDKs, `iosArm64` + `iosSimulatorArm64`, compiler flags, host and device test builders, `*UiTest`-only host exclusion with `isFailOnNoMatchingTests = false`). Verify: build-logic compiles.
- [x] 1.5 Add the plugins `KmpLibrary`, `CmpLibrary`, `CmpFeature`, `CmpApplication`, `AndroidApplication`, `AndroidApplicationCompose` and `BuildKonfig` per design §1 and §4. BuildKonfig reads `BASE_URL_ANDROID` / `BASE_URL_IOS` with `java.util.Properties`, fails with the messages from design §4 (missing key, and a value that isn't an http(s) URL ending in `/api/v1`), and uses per-target configs. No desktop, `iosX64`, Room, desugaring or material-icons. Verify: `./gradlew -p build-logic :convention:validatePlugins` succeeds.

## 2. Module skeleton and rename

- [x] 2.1 `git mv shared composeApp`. Delete `Greeting.kt`, `GreetingUtil.kt`, `Platform*.kt`, the demo drawable and the three wizard test files. Reduce `App.kt` to an empty `@Composable fun App()`. Keep `MainViewController.kt`. Rewrite `composeApp/build.gradle.kts` to apply `convention.cmp.application`. Verify: `git status` shows the rename, and `grep -rn "Greeting\|getPlatform" composeApp androidApp` finds nothing.
- [x] 2.2 Update `settings.gradle.kts` (`includeBuild("build-logic")`, `TYPESAFE_PROJECT_ACCESSORS`, include all 21 modules) and the root `build.gradle.kts` (`apply false` for every plugin the convention plugins apply). Verify: `./gradlew projects` lists `:composeApp`, `:androidApp`, 4 core modules and 15 feature modules.
- [x] 2.3 Create build files for `:core:domain`, `:core:data` (Ktor + Koin core, no `buildkonfig` yet), `:core:presentation` and `:core:designsystem` per design §2. Verify: `./gradlew :core:domain:compileKotlinIosSimulatorArm64 :core:presentation:compileAndroidMain` succeeds (task names adjusted to what the KMP Android plugin generates).
- [x] 2.4 Create `build.gradle.kts` for `domain`, `data` and `presentation` of `auth`, `boards`, `cards`, `labels` and `transfer`, with only the plugins and project dependencies from design §2. Verify: `./gradlew projects` lists them, and `for f in auth boards cards labels transfer; do grep -n 'projects\.feature\.' feature/$f/*/build.gradle.kts | grep -v "projects\.feature\.$f\."; done` prints nothing.
- [x] 2.5 Wire `:composeApp` to depend on every core and feature module. Switch `:androidApp` to `convention.android.application.compose` with `implementation(projects.composeApp)` and activity-compose. Remove the preview of the deleted demo from `MainActivity` and leave it hosting `App()`. Verify: `./gradlew :androidApp:assembleDebug` succeeds.
- [x] 2.6 Rename the iOS framework: build phase → `:composeApp:embedAndSignAppleFrameworkForXcode`, `import ComposeApp` in Swift files. Verify: the `xcodebuild` simulator build succeeds.
- [x] 2.7 Run all gates and the `xcodebuild` build. Verify they pass, then commit `build: add build-logic convention plugins and module skeleton`.

## 3. Result wrapper and data errors (`:core:domain`)

- [x] 3.1 Add `Error`, `Result`, `EmptyResult`, `map`, `onSuccess`, `onFailure`, `asEmptyResult` and `DataError.Network` with the entries from design §5. Verify: `:core:domain` compiles for Android and iOS.
- [x] 3.2 List the break vectors (wrong-branch side effects, chaining order, `Unit` mapping, nullable data, mapper throwing), then write `ResultTest` in `commonTest`, one test per distinct vector. Verify: `./gradlew :core:domain:testAndroidHostTest :core:domain:iosSimulatorArm64Test` passes.
- [x] 3.3 Run all gates. Verify they pass, then commit `feat(core): add result wrapper and data errors`.

## 4. UiText and ObserveAsEvents (`:core:presentation`)

- [x] 4.1 Add `UiText` (design §6) and `ObserveAsEvents` (lifecycle-aware, `Dispatchers.Main.immediate`). Verify: `:core:presentation` compiles for Android and iOS.
- [x] 4.2 Write `UiTextTest` in `commonTest` for `DynamicString.asStringAsync()`: empty, emoji/combining characters, and `%s`/`%1$d` passed through unchanged. Verify: `./gradlew :core:presentation:testAndroidHostTest :core:presentation:iosSimulatorArm64Test` passes.
- [x] 4.3 Run all gates. Verify they pass, then commit `feat(core): add ui text and observe-as-events`.

## 5. HttpClient and safe calls (`:core:data`)

- [ ] 5.1 Ask the user to add `BASE_URL_ANDROID` and `BASE_URL_IOS` to their `local.properties` (examples from design §4) and wait for confirmation. Never edit `local.properties` yourself. Verify: the user confirms.
- [ ] 5.2 Apply `convention.buildkonfig` to `:core:data`. Check that the generated `BuildKonfig.BASE_URL` differs per target (Android vs iOS sources under `build/buildkonfig`). If the Android target name isn't `android`, fix the target config. Then ask the user to comment out `BASE_URL_IOS` in `local.properties`, run `./gradlew :core:data:help`, and confirm the output contains `Missing BASE_URL_IOS in local.properties`. Ask the user to restore the key. Verify: both checks are observed and the user confirms the key is restored.
- [ ] 5.3 Add `HttpClientFactory`, `constructRoute`, `get`/`post`/`patch`/`delete`, `safeCall`, `responseToResult` and `expect platformSafeCall` with Android and iOS actuals and `NSError.toNetworkError()` per design §7. Verify: `:core:data` compiles for Android and iOS.
- [ ] 5.4 List the break vectors from design §10, then write `SafeCallIntegrationTest` (`commonTest`, `MockEngine`), the Android java.net exception tests (`androidHostTest`) and the `NSError` mapping tests (`iosTest`). Verify: `./gradlew :core:data:testAndroidHostTest :core:data:iosSimulatorArm64Test` passes.
- [ ] 5.5 Add the `INTERNET` permission, the debug-only manifest overlay with `network_security_config.xml`, and `NSAllowsLocalNetworking` in `Info.plist`. Verify: the merged debug manifest (`androidApp/build/intermediates/merged_manifests/debug/...`) has `networkSecurityConfig` and the release merged manifest doesn't. `plutil -p iosApp/iosApp/Info.plist` shows the ATS key.
- [ ] 5.6 Run all gates and the `xcodebuild` build. Verify they pass, then commit `feat(core): add http client and safe call helpers`.

## 6. Koin startup

- [ ] 6.1 Add `coreDataModule` and `expect`/`actual coreDataPlatformModule` (OkHttp / Darwin engine) in `:core:data`, and `initKoin(config)` in `:composeApp` `commonMain`. Verify: `:composeApp` compiles for Android and iOS.
- [ ] 6.2 Add `CardiganApplication` in `:androidApp` that calls `initKoin { androidContext(...) }` and register it in the manifest. Call `InitKoinKt.doInitKoin(config: nil)` in `iOSApp.init()`. Verify: `./gradlew :androidApp:assembleDebug` and the `xcodebuild` build succeed.
- [ ] 6.3 Add a `commonTest` in `:core:data` that starts Koin with `coreDataModule` plus a test module overriding the engine with `MockEngine`, resolves `HttpClient`, sends one `get<Unit>("/ping")` through it, and asserts the `MockEngine` recorded exactly one request to `${BuildKonfig.BASE_URL}/ping`. `stopKoin()` runs in `@AfterTest`. Verify: it passes on Android host and iOS simulator.
- [ ] 6.4 Launch the app on an Android emulator and an iOS simulator. Verify: both start to a blank screen without a crash (Koin starts once).
- [ ] 6.5 Run all gates and the `xcodebuild` build. Verify they pass, then commit `feat: start koin on android and ios`.

## 7. Documentation

- [ ] 7.1 Update `AGENTS.md`: `BASE_URL_ANDROID` / `BASE_URL_IOS` (with `/api/v1`) instead of `BASE_URL`, the current-state paragraph (scaffold done), and the exceptions to "add each dependency in the change that first needs it" from D2/D3. Show the user the diff before committing. Verify: the user approves and all gates pass, then commit `docs: update agents.md for module skeleton`.
- [ ] 7.2 Tell the user about the conflicts found, without editing anything: the skill's `*IntegrationTest` host exclusion, the skill's `API_KEY` buildkonfig example, and CLAUDE.md's `RATE_LIMIT_EXCEEDED` vs the shared spec's `RATE_LIMITED`. Verify: they are mentioned in the apply summary.

## Workflow follow-up

- Archive the change after review with `/opsx:archive`.
