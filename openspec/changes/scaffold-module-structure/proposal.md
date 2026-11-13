# Proposal

## Why

The repo is still the KMP wizard scaffold (`:shared` + `:androidApp`). Every feature needs the module layout, convention plugins and shared utilities first, so they have to exist before any feature work can start. Doing it once now keeps every later feature change small and consistent.

## What Changes

- Rename `:shared` to `:composeApp` and the iOS framework `Shared` to `ComposeApp`. Update the Xcode build phase and Swift import. Delete the wizard demo code (`Greeting`, `Platform`, demo drawable, demo tests). `App()` becomes an empty composable.
- Add `build-logic` with convention plugins `kmp.library`, `cmp.library`, `cmp.feature`, `cmp.application`, `android.application`, `android.application.compose` and `buildkonfig` under `com.plcoding.cardiganapp.convention.*`. Android + `iosArm64` + `iosSimulatorArm64` only. JVM 17, no desugaring, no Room, no Material Icons.
- Move SDK levels and app id to `project*` keys in the version catalog. Add Koin, navigation-compose, lifecycle/savedstate, kotlinx-serialization, Ktor (core, content-negotiation, JSON, auth, logging, OkHttp, Darwin), BuildKonfig, AssertK, Turbine, coroutines-test and ktor-client-mock.
- Add `:core:domain`, `:core:data`, `:core:presentation`, `:core:designsystem`, and `domain`/`data`/`presentation` modules for `auth`, `boards`, `cards`, `labels` and `transfer`. Feature modules have build files only.
- Implement the shared utilities:
  - `:core:domain`: `Error`, `Result`, `EmptyResult`, `map`/`onSuccess`/`onFailure`/`asEmptyResult`, `DataError.Network`
  - `:core:presentation`: `UiText`, `ObserveAsEvents`
  - `:core:data`: `HttpClientFactory` (JSON only, engine passed in), safe call helpers `get`/`post`/`patch`/`delete` with platform exception mapping for OkHttp and Darwin, a per-platform `BASE_URL` (including `/api/v1`) read from `BASE_URL_ANDROID` / `BASE_URL_IOS` in `local.properties`
- Start Koin on both platforms through `initKoin()` in `:composeApp`, from a new Android `Application` and from the iOS shell.
- Allow cleartext HTTP to the dev backend in Android debug builds and via `NSAllowsLocalNetworking` on iOS. Add the `INTERNET` permission.
- Add unit tests for the `Result` helpers and `UiText`, and `MockEngine` integration tests for the safe call helpers.
- **BREAKING** (developer setup only): every developer must add `BASE_URL_ANDROID` and `BASE_URL_IOS` to `local.properties`, each an http(s) URL ending in `/api/v1`, or the build fails. Gradle paths change from `:shared:*` to `:composeApp:*`.

## Capabilities

### New Capabilities

None. This change sets up tooling and internal utilities and adds no user-visible behavior (`skip_specs: true`).

### Modified Capabilities

None.

## Shared-spec requirements

This change implements or depends on these shared requirements (`cardigan-shared-specs`):

- `api-conventions` — "All endpoints use a versioned base path": the base URLs include `/api/v1`.
- `api-conventions` — "Updates are partial": a `patch` helper exists.
- `api-conventions` — "Success status codes and bodies": a 204 with no body maps to success with `Unit`.
- `api-conventions` — "Unknown fields are ignored": the client JSON ignores unknown keys.
- `api-errors` — "Top-level error codes": every status in the catalog maps to a `DataError.Network` value.
- `api-errors` — "Clients handle unknown error codes by status": errors are mapped by HTTP status alone, also for bodies that are not problem details.

## Impact

- **Build**: new `build-logic` included build; all module build files rewritten; `settings.gradle.kts`, root `build.gradle.kts` and `gradle/libs.versions.toml` change. Typesafe project accessors enabled.
- **Android shell**: `:androidApp` uses the convention plugins, gains an `Application` class, `INTERNET` permission and a debug network security config.
- **iOS shell**: `iosApp` build phase, framework import, Koin start and `Info.plist` change. The `xcodebuild` gate applies.
- **Developer setup**: `BASE_URL_ANDROID` (e.g. `http://10.0.2.2:8080/api/v1`) and `BASE_URL_IOS` (e.g. `http://localhost:8080/api/v1`) required in `local.properties`.
- **Docs**: CLAUDE.md example URLs and the `*IntegrationTest` host-test exclusion in `kmp-module-structure` need updating. CLAUDE.md's `RATE_LIMIT_EXCEEDED` conflicts with the shared spec's `RATE_LIMITED`.
