# Decisions: scaffold-module-structure

Confirmed with Philipp Lackner on 2026-11-13.

## Summary

Turn the KMP wizard scaffold into the full module layout from `kmp-module-structure` (with the CLAUDE.md changes): `build-logic` convention plugins, the four core modules, and `domain`/`data`/`presentation` modules for all five features, with the Gradle config fully wired. Feature modules stay empty. The core modules get the project-wide utilities (Result and errors, UiText, ObserveAsEvents, safe call helpers, HttpClient factory), Koin is started on both platforms, and the app can reach the local dev backend.

## Decisions

### D1 — App content after the scaffold
- **Question**: What should the app show after the scaffold?
- **Decision**: `App()` in `:composeApp` is an empty composable. Delete `Greeting`, `GreetingUtil`, `Platform` (+ actuals), the demo drawable and the wizard tests. `MainActivity` and `MainViewController` only host `App()`.
- **Alternatives**: Move the wizard demo into `:composeApp` unchanged.
- **Why**: The skeleton should contain no demo code.
- **Depends on**: —

### D2 — Libraries wired by the convention plugins
- **Question**: Which libraries should the convention plugins wire now?
- **Decision**: The full skill set now: `cmp.feature` adds Koin (BOM, compose, compose-viewmodel, android variants), JetBrains navigation-compose, lifecycle viewmodel/runtime-compose/viewmodel-savedstate, savedstate. `kmp.library` adds the serialization plugin and kotlinx-serialization-json. All go into the version catalog.
- **Alternatives**: Lean plugins, with each library added in the change that first uses it (the CLAUDE.md default).
- **Why**: User choice. Later features only apply `cmp.feature`. This is an explicit exception to "add each dependency in the change that first needs it".
- **Depends on**: —

### D3 — Libraries in the data modules
- **Question**: Should data modules get their libraries now?
- **Decision**: Yes. `:core:data` and every `:feature:*:data` get Ktor client core, content-negotiation, serialization-kotlinx-json, auth, logging (common), OkHttp (Android), Darwin (iOS), and Koin core.
- **Alternatives**: Only `:core:data`; or no Ktor yet.
- **Why**: Consistent with D2.
- **Depends on**: D2

### D4 — No Material Icons
- **Question**: Include the skill's `material-icons-core` in `cmp.library`?
- **Decision**: No. Icons come only from `:core:designsystem` vector drawables.
- **Alternatives**: Include it as in the skill.
- **Why**: CLAUDE.md forbids Material Icons as a stand-in for Figma icons.
- **Depends on**: D2

### D5 — BuildKonfig plugin
- **Question**: Create and apply the `buildkonfig` convention plugin now?
- **Decision**: Create it now and apply it to `:core:data` only. It reads `BASE_URL` from `local.properties`.
- **Alternatives**: Add it later; create it without applying it; apply it to all data modules.
- **Why**: `BASE_URL` is only needed by the safe call helpers in `:core:data`.
- **Depends on**: —

### D6 — Missing BASE_URL
- **Question**: How should the build behave when `BASE_URL` is missing?
- **Decision**: Fail the build with a clear message naming `BASE_URL` and `local.properties`. Each developer adds `BASE_URL` to their `local.properties`.
- **Alternatives**: Fall back to `http://10.0.2.2:8080`.
- **Why**: A silent fallback would give the iOS simulator the wrong host.
- **Depends on**: D5

### D7 — Where `/api/v1` lives
- **Question**: Where does the `/api/v1` prefix live?
- **Decision**: In `BASE_URL` itself, e.g. `http://10.0.2.2:8080/api/v1`. `constructRoute` only joins `BASE_URL` and the route. Data sources pass routes like `/boards`.
- **Alternatives**: `constructRoute` adds it; data sources pass it.
- **Why**: User choice.
- **Depends on**: D5

### D8 — OpenAPI wiring
- **Question**: Include `syncOpenApiSpec` and DTO generation?
- **Decision**: Out of scope for this change.
- **Alternatives**: Add the task only; add task and generator.
- **Why**: The backend has no spec snapshot yet.
- **Depends on**: —

### D9 — Utilities implemented in this change
- **Question**: Which project-wide utilities should this change implement?
- **Decision**: All four groups: (a) `:core:domain` `Error`, `Result`, `EmptyResult`, `map`/`onSuccess`/`onFailure`/`asEmptyResult`, `DataError`; (b) `:core:presentation` `UiText` (`DynamicString`, `Resource`, `asString`, `asStringAsync`) and `ObserveAsEvents`; (c) `:core:data` safe call helpers; (d) `:core:data` `HttpClientFactory`.
- **Alternatives**: Any subset.
- **Why**: User request added mid-interview: the project-wide utilities belong to the skeleton.
- **Depends on**: —

### D10 — DataError shape
- **Question**: Keep the skill's `DataError.Local` branch?
- **Decision**: `DataError` has only the `Network` branch.
- **Alternatives**: Keep `Local` as in the skill.
- **Why**: The app has no database and nothing produces local errors.
- **Depends on**: D9

### D11 — HTTP status mapping
- **Question**: How should HTTP statuses map to `DataError.Network`?
- **Decision**: Cover every status in the shared error catalog: 400 `BAD_REQUEST`, 401 `UNAUTHORIZED`, 403 `FORBIDDEN`, 404 `NOT_FOUND`, 408 `REQUEST_TIMEOUT`, 409 `CONFLICT`, 413 `PAYLOAD_TOO_LARGE`, 415 `UNSUPPORTED_MEDIA_TYPE` (new), 422 `UNPROCESSABLE_ENTITY` (new), 429 `TOO_MANY_REQUESTS`, other 5xx `SERVER_ERROR`, other statuses `UNKNOWN`. Plus `NO_INTERNET`, `SERIALIZATION`, `SERVICE_UNAVAILABLE` (503) from the skill.
- **Alternatives**: The skill's mapping as-is (400/403/404/415/422 → `UNKNOWN`).
- **Why**: Shared `api-errors` requires clients to handle errors by status.
- **Depends on**: D9, D10

### D12 — HTTP helpers
- **Question**: Which HTTP helpers should `:core:data` provide?
- **Decision**: `get`, `post`, `patch`, `delete`. A 204 / empty body maps to `Result.Success(Unit)` when the response type is `Unit`.
- **Alternatives**: The skill's `get`/`post`/`delete` only.
- **Why**: Shared `api-conventions` uses PATCH for every update and 204 for deletes.
- **Depends on**: D9

### D13 — HttpClient logging
- **Question**: Should the HttpClient log requests now?
- **Decision**: No. No Ktor `Logging` plugin installed and no Kermit yet. (The `ktor-client-logging` artifact is still on the classpath per D3.)
- **Alternatives**: Kermit-backed logging gated by a BuildKonfig `DEBUG` field.
- **Why**: The debug flag mechanism for KMP libraries needs its own decision.
- **Depends on**: D9

### D14 — HttpClient auth
- **Question**: (Follows from CLAUDE.md, confirmed in the summary.) Install the Ktor `Auth` plugin?
- **Decision**: No. `HttpClientFactory` installs ContentNegotiation (JSON, ignore unknown keys) and default JSON content type. The engine is passed in.
- **Alternatives**: —
- **Why**: The body-based refresh contract is still an open backend item.
- **Depends on**: D9

### D15 — DataError.toUiText
- **Question**: Implement `DataError.toUiText()` now?
- **Decision**: Deferred. It needs user-facing copy and there is no network-error design.
- **Alternatives**: Implement with copy the user provides.
- **Why**: CLAUDE.md forbids inventing copy.
- **Depends on**: D9, D10

### D16 — Koin startup
- **Question**: Start Koin in this change?
- **Decision**: Yes. `initKoin()` in `:composeApp`, called from a new Android `Application` class and from the iOS shell. Registers the core data module (HttpClient) and the platform modules that provide the engine (OkHttp / Darwin). No feature modules.
- **Alternatives**: Wire dependencies only; start later.
- **Why**: The DI graph exists from day one. Touches `iosApp/`, so the `xcodebuild` gate applies.
- **Depends on**: D2, D9

### D17 — Local network setup
- **Question**: Add the plumbing to reach the dev backend?
- **Decision**: Yes. Android `INTERNET` permission, a debug-only network security config allowing cleartext, and `NSAllowsLocalNetworking` in the iOS `Info.plist`. Release stays HTTPS-only.
- **Alternatives**: Later.
- **Why**: The HttpClient is built in this change.
- **Depends on**: D9

### D18 — Tests
- **Question**: Which tests should this change include?
- **Decision**: Add AssertK, Turbine, kotlinx-coroutines-test and ktor-client-mock to the catalog and to `kmp.library`'s `commonTest`. Unit tests for the `Result` helpers and `UiText.DynamicString`. `MockEngine` integration tests for the safe call helpers and the status mapping. Green on Android host and iOS simulator.
- **Alternatives**: Stack only; no tests.
- **Why**: CLAUDE.md testing rules.
- **Depends on**: D9

### D19 — Host test filter
- **Question**: (Conflict, settled by CLAUDE.md.) Which tests are excluded from the Android host run?
- **Decision**: Only `*UiTest`. Integration tests run on the host.
- **Alternatives**: The skill's exclusion of `*UiTest` and `*IntegrationTest`.
- **Why**: CLAUDE.md says integration tests run on the Android host; CLAUDE.md wins. The skill should be fixed.
- **Depends on**: D18

### D20 — JVM target
- **Question**: JVM target for all modules?
- **Decision**: 17, with a JDK 17 toolchain in `build-logic`.
- **Alternatives**: Keep 11; 21.
- **Why**: Matches the skill.
- **Depends on**: —

### D21 — Desugaring
- **Question**: Include core library desugaring?
- **Decision**: No.
- **Alternatives**: Include it as in the skill.
- **Why**: minSdk 26 and no `java.*` in common code; one fewer dependency.
- **Depends on**: —

### D22 — iOS framework name
- **Question**: What should the iOS framework be called after the rename?
- **Decision**: `ComposeApp`. Update `import`, the Xcode build phase (`:composeApp:embedAndSignAppleFrameworkForXcode`) and Swift call sites.
- **Alternatives**: Keep `Shared`.
- **Why**: Matches the skill.
- **Depends on**: —

### D23 — Version catalog keys
- **Question**: How should SDK levels and app id be stored?
- **Decision**: The skill's keys: `projectApplicationId`, `projectMinSdkVersion`, `projectCompileSdkVersion`, `projectTargetSdkVersion`, `projectVersionCode`, `projectVersionName`. They replace `android-compileSdk`/`android-minSdk`/`android-targetSdk`. Values stay as today (compile/target 37, min 26).
- **Alternatives**: Keep the wizard keys.
- **Why**: Matches the skill's convention plugins.
- **Depends on**: —

### D24 — Specs
- **Question**: Should this change carry a spec delta?
- **Decision**: No. Set `skip_specs: true`.
- **Alternatives**: A small `mobile-networking` capability.
- **Why**: Internal tooling and utilities. The debug-only cleartext rule is already in CLAUDE.md and checked by tasks.
- **Depends on**: D17

### D26 — Per-platform base URL
- **Question**: The Android emulator needs `10.0.2.2`, the iOS simulator `localhost`. How does one `BASE_URL` serve both?
- **Decision**: Two keys in `local.properties`: `BASE_URL_ANDROID` and `BASE_URL_IOS`, both including `/api/v1`. BuildKonfig target configs expose one `BASE_URL` constant per platform to common code. The build fails with a clear message if either key is missing. This refines D5–D7: wherever they say `BASE_URL` in `local.properties`, read these two keys.
- **Alternatives**: One key with an optional iOS override; one key with the LAN IP.
- **Why**: Both simulators work out of the box without editing the file when switching platforms.
- **Depends on**: D5, D6, D7

### D25 — Empty feature modules
- **Question**: (Confirmed in the summary.) What do feature modules contain?
- **Decision**: Only `build.gradle.kts` with their convention plugin(s) and project dependencies per the skill's dependency rules. No source files. `:composeApp` depends on all of them.
- **Alternatives**: —
- **Why**: Original request: skeleton without real source code.
- **Depends on**: D2, D3

## Facts

- The repo is the wizard scaffold: `:shared` + `:androidApp`, no `build-logic` — source: `settings.gradle.kts`
- Kotlin 2.4.20, AGP 9.1.1, Compose Multiplatform 1.12.1, compile/target SDK 37, min SDK 26 — source: `gradle/libs.versions.toml`
- The Xcode build phase runs `./gradlew :shared:embedAndSignAppleFrameworkForXcode`; Swift does `import Shared` — source: `iosApp/iosApp.xcodeproj/project.pbxproj`, `iosApp/iosApp/ContentView.swift`
- iOS deployment target is already 18.2 — source: `project.pbxproj`
- The Android manifest has no `INTERNET` permission; `Info.plist` has no ATS settings — source: `androidApp/src/main/AndroidManifest.xml`, `iosApp/iosApp/Info.plist`
- The backend has no OpenAPI snapshot (`app/src/test/resources/openapi/` missing) — source: `cardigan.backend.dir` checkout
- `local.properties` has `cardigan.backend.dir` but no `BASE_URL` — source: `local.properties`
- Updates use PATCH; deletes return 204 with no body — source: shared spec `api-conventions` (Requirements "Updates are partial", "Success status codes and bodies")
- Error statuses used: 400, 401, 404, 409, 413, 415, 422, 429, 500; unknown codes are handled by status — source: shared spec `api-errors` (Requirements "Top-level error codes", "Clients handle unknown error codes by status")
- The shared spec names the 429 code `RATE_LIMITED`; CLAUDE.md says `RATE_LIMIT_EXCEEDED` — source: shared spec `api-errors`, `CLAUDE.md`. Does not affect this change; CLAUDE.md should be corrected.
- The skill excludes `*IntegrationTest` from host tests, which conflicts with CLAUDE.md — source: `.claude/skills/kmp-module-structure/SKILL.md`

## Out of scope

- OpenAPI sync task and DTO generation
- Ktor `Auth` plugin, token storage, refresh and logout
- Ktor `Logging` plugin and Kermit
- `DataError.toUiText()` and any error copy
- Theme, tokens, fonts, icons and components in `:core:designsystem`
- Navigation graph, routes and any screen
- Any source code in feature modules
- ProblemDetail `code` parsing and feature error types
- Room, DataStore, desktop target, `iosX64`

## Deferred

- How the Ktor debug flag works for logging (with Kermit) — settle before: the change that adds logging
- How ProblemDetail `code` values reach feature error types — settle before: the first feature data source that needs it
