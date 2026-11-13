# Cardigan App

Cardigan is a kanban app for developers. This repo is the Kotlin Multiplatform client for Android and iOS, with all UI in Compose Multiplatform. It talks to the Cardigan Spring Boot backend over a REST API under `/api/v1`. A React web client uses the same backend and the same designs. It lives in a different repo.

This file is the project's system prompt. The `kmp-*` skills in `.claude/skills/` hold the detailed rules and code patterns. This file gives the big picture, the decisions specific to this project, and how to work here. **When this file and a skill disagree, this file wins.** Mention the conflict when you hit it, so the skill can be fixed.

---

## Tech Stack

| Area | Choice |
|---|---|
| Targets | Android (minSdk 26) and iOS 18.2+ (`iosArm64`, `iosSimulatorArm64`), phones and tablets. No desktop, no web, no `iosX64`. |
| Build | Kotlin 2.4, Gradle 9, AGP 9 (`com.android.kotlin.multiplatform.library` for KMP modules), version catalog, convention plugins in `build-logic/` |
| UI | Compose Multiplatform with Material 3 as the base for design system components. `iosApp` is a thin SwiftUI shell that hosts `MainViewController` and starts Koin. No SwiftUI screens. |
| Presentation | MVI with JetBrains `ViewModel`, `SavedStateHandle`, `collectAsStateWithLifecycle` |
| Navigation | JetBrains `navigation-compose`, type-safe `@Serializable` routes |
| DI | Koin |
| Networking | Ktor client (OkHttp on Android, Darwin on iOS), kotlinx.serialization |
| API contract | Kotlin DTOs **generated** from the backend's OpenAPI spec with OpenAPI Generator, models only (see [API Contract](#api-contract)) |
| Local storage | No database. Encrypted secure storage for the refresh token (see [Authentication](#authentication)). |
| Config | BuildKonfig, values from `local.properties` |
| Resources | Compose Multiplatform resources (`Res.string`, `Res.drawable`, `Res.font`) |
| Icons | SVGs exported from Figma, converted to vector drawable XML in `:core:designsystem` |
| Fonts | Geist and Geist Mono, bundled as compose resources |
| Time / IDs | `kotlin.time`, kotlinx-datetime, `kotlin.uuid.Uuid`. No `java.*` in common code. |
| Logging | Kermit, debug builds only |
| Lint / format | None for now. `kotlin.code.style=official` and the IDE formatter. |
| Tests | `kotlin.test`, AssertK, Turbine, kotlinx-coroutines-test, Ktor `MockEngine`, Compose `ui-test` (`runComposeUiTest`) |

**Current state:** the module skeleton is in place: `build-logic` convention plugins, `:composeApp`, the four core modules, and `domain`/`data`/`presentation` modules for all five features. Feature modules contain only build files. The core modules have `Result`/`DataError`, `UiText`, `ObserveAsEvents`, the `HttpClientFactory` and safe call helpers, and Koin starts on both platforms. There is no navigation, no theme and no screen yet. `App()` is empty.

Add each dependency in the change that first needs it, and ask before adding one that isn't in the table above. Exceptions, already wired by the convention plugins: `cmp.feature` brings Koin, navigation-compose, lifecycle and savedstate to every feature presentation module; `kmp.library` brings kotlinx-serialization-json and the test stack; `:core:data` and every feature `data` module have Ktor (core, content negotiation, JSON, auth, logging, OkHttp, Darwin) and Koin core.

---

## Architecture

The full rules are in `kmp-module-structure`. In short:

```
:androidApp                     thin Android shell: Activity, Application (starts Koin), manifest
iosApp/                         thin SwiftUI shell: hosts MainViewController, starts Koin
:composeApp                     KMP library: App(), NavHost, initKoin. The only module that knows every feature.
:build-logic                    convention plugins
:core:domain                    Result, Error, DataError, shared domain models and interfaces
:core:data                      HttpClient factory, safe call helpers, session and token storage, generated API models
:core:presentation              UiText, ObserveAsEvents, shared error-to-UiText mappings
:core:designsystem              AppTheme, tokens, typography, fonts, icons, shared components
:feature:<name>:domain          feature models, data source interfaces, feature error types
:feature:<name>:data            Ktor data sources, mappers from generated DTOs to domain models
:feature:<name>:presentation    ViewModels, Root/Screen composables, routes, nav graph
```

- Features never depend on each other. Code moves to `core` when a second feature needs it.
- Cross-feature navigation goes through callbacks wired in `:composeApp`, as in `kmp-navigation`.
- Root/Screen split with State, Action and optional Event, as in `kmp-presentation-mvi`.
- Everything lives in `commonMain`. Platform code goes behind an interface or `expect`/`actual`, with implementations in `androidMain` **and** `iosMain`. Screens never branch on the platform.

**Changes from the skills:**

- Rename the scaffold's `:shared` module to `:composeApp`.
- Drop the desktop target and `iosX64` from every convention plugin and example.
- No `room` convention plugin, no database modules, no DataStore for tokens (see [Local data](#local-data) and [Authentication](#authentication)).
- The `buildkonfig` convention plugin reads `BASE_URL_ANDROID` and `BASE_URL_IOS`, not `API_KEY`, and exposes them as one `BuildKonfig.BASE_URL` per platform.
- Package root is `com.plcoding.cardiganapp`. Convention plugin IDs use `com.plcoding.cardiganapp.convention.*`.

### Features

| Feature | Covers | Figma page |
|---|---|---|
| `auth` | log in, register, session expired, log out | Log in & register |
| `boards` | board list (grid, phone list), new/rename/delete board, board view (columns, quick add, drag & drop, column menu, search, label filter) | Board list, Board view |
| `cards` | card detail (side panel / full screen), card labels, unsaved changes | Card detail |
| `labels` | labels page, new/edit/delete label | Labels |
| `transfer` | export from the board menu, import dialog | Import & export |

Each feature gets `domain`, `data` and `presentation` modules. Don't create another feature module, or a new `core` module, without asking.

---

## Figma: The Single Source of Truth for UI

**Figma is the only source for how the UI looks and behaves.** Layout, spacing, colors, type, components, states, copy and responsive behavior all come from Figma. Never invent UI.

### When Figma can't be read

If a Figma MCP call fails (not authenticated, rate limited, node not found, timeout, empty result), **stop and tell the user exactly which call failed and for which node.** Then wait. Don't build the UI from memory, from another screen, from Material defaults, or from a guess at what it probably looks like. Don't build a "temporary" version to replace later.

### When something isn't designed

If a screen, state or component you need isn't in Figma (for example a rate-limit error, a network error, an offline state), **stop and ask.** List the missing states. Build them only once a design exists or the user tells you which existing pattern to reuse.

The same applies when Figma conflicts with the API contract or with a decision in this file. Ask, don't pick.

### Where to find things

File: [Cardigan Designs](https://www.figma.com/design/MALwbOph7Uj2QtiTy21JaQ/Cardigan-Designs?node-id=0-1) · file key `MALwbOph7Uj2QtiTy21JaQ`

**Start at the Overview page.** It explains how the file is organized, links every page, and lists the UI decisions. Then open the page for the feature you're working on. Each feature has its own page:

| Page | Node | What's there |
|---|---|---|
| Overview | `0:1` | Index: how to read the file, page list, UI decisions |
| Foundations | `9:2` | Color tokens (light and dark), 20-color label palette, type, spacing, radius, breakpoints |
| Design System | `9:3` | All components with variants, slots and properties |
| Log in & register | `9:4` | Log in, register, live password rules, errors, loading |
| Board list | `9:5` | Board grid and phone list, new board, rename, delete, empty and error states |
| Board view | `9:6` | Columns, quick add, drag & drop, column menu, search, label filter |
| Card detail | `9:7` | Side panel on tablet/desktop, full screen on phone, labels, unsaved changes |
| Labels | `9:8` | Labels page, new/edit/delete dialogs, empty and loading states |
| Import & export | `9:9` | Export from the board menu, import dialog with drop zone, errors, key clash |

Page node IDs are stable. Frame IDs inside pages change as designs change, so look them up with `get_metadata` on the page instead of copying them into code or docs.

### How to read the file

- Each screen page groups frames into **sections by flow**. Frames are named **`State · Size`**, for example `Wrong email or password · Phone 390`.
- Three sizes: **phone 390**, **tablet portrait 834**, **desktop 1440**. The app implements all three: phone, tablet portrait, and the desktop layout for tablet landscape and other wide windows.
- Every screen is built from Design System instances. Implement the component in `:core:designsystem` once and reuse it, don't rebuild it per screen.
- Colors, spacing and radius are Figma variables. The dark theme is the same frames with the Color collection switched to Dark mode.
- **Annotations on frames** explain behavior. Follow them. Notes marked as **app-only** apply to this app. Notes marked as web-only don't.
- The UI decisions on the Overview page are product decisions. Don't work around them. The one deliberate addition is accessibility actions for moving cards (see [Accessibility](#accessibility)).
- The breakpoints on Foundations are marked "(proposal)". Use them, but define them in one place in `:core:designsystem`, because they may change. Pick the layout from the window size (window size classes), never from the device type or platform.

### Workflow

1. `create_session` with the file key before the first Figma call of a task.
2. `get_metadata` on the feature's page to find the frames for every state and size you need.
3. Load the `figma:figma-design-to-code` skill, then call `get_design_context` for each frame. Check the result with `get_screenshot`.
4. Use `get_variable_defs` for tokens. Theme tokens in `:core:designsystem` follow the Figma variable names (`background/surface` → `background.surface`, `text/muted`, `accent/default`, ...).
5. Treat generated markup as a reference. Map it to `:core:designsystem` components and theme tokens. Never paste raw hex values, pixel values or absolute positioning from it.
6. Look at **every size** of a screen before building it. Responsive behavior comes from comparing the phone, tablet and desktop frames.
7. Copy text in the designs is the source for the English string resources.

### Icons

Icons are exported from Figma as SVG and converted to vector drawable XML in `core/designsystem/src/commonMain/composeResources/drawable/`. Strip hard-coded fills so the icon is tinted by `Icon`'s `tint` / `LocalContentColor`. Expose each icon on the project icon object, as in `kmp-compose-components`. Add an icon when a feature first needs it. Don't use Material Icons as a stand-in for an icon that exists in Figma.

---

## Project Decisions

### Backend and environments

- The API base path is `/api/v1`. All routes are `/api/v1/<plural-resource>`.
- The base URL comes from BuildKonfig, read from `BASE_URL_ANDROID` and `BASE_URL_IOS` in `local.properties`. Both include `/api/v1`, and the build fails if either is missing or doesn't end in `/api/v1`. For now there is only local development: `BASE_URL_ANDROID=http://10.0.2.2:8080/api/v1` for the emulator, `BASE_URL_IOS=http://localhost:8080/api/v1` for the simulator. A physical device needs the dev machine's LAN address. Data sources pass routes without the prefix, e.g. `/boards`.
- Plain HTTP is allowed for local development only: an Android network security config in the debug build, and `NSAllowsLocalNetworking` on iOS. Release builds stay HTTPS-only.
- The production URL and any staging environment are open (see [Open Items](#open-items)).

### API contract

The backend generates its OpenAPI spec from code and commits it as a snapshot. This app generates its DTOs from it.

- The location of the local backend checkout is set per developer as `cardigan.backend.dir` in `local.properties`. Use only that path for the backend, never another checkout or a guessed location. If the property is missing, ask the user for it. The backend's committed spec snapshot lives in `app/src/test/resources/openapi/` inside that repo.
- The Gradle task `syncOpenApiSpec` copies the spec from there to `api/openapi.json` in this repo. It fails with a clear message if the property or the file is missing. The copy is committed, so contract changes show up in this repo's diff.
- Read the backend repo when you need context (its `AGENTS.md`, controllers, DTOs, error codes), but never change files there.
- DTOs are **generated** from `api/openapi.json` with OpenAPI Generator (`kotlin` generator, multiplatform, kotlinx.serialization, **models only**) into the build directory of `:core:data`. Generated code isn't committed and is never edited. No generated API client classes: requests go through our own Ktor data sources and safe call helpers from `kmp-error-handling`.
- Feature data modules keep the rest of `kmp-data-layer`: data source interfaces in `domain`, Ktor implementations in `data`, hand-written mappers from generated DTOs to domain models, `Result` and `DataError` for failures. Generated DTOs never leave the `data` layer.
- **Never invent an endpoint, field, enum value or error code.** If the spec doesn't have what a screen needs, stop and tell the user what's missing. The backend may not have built it yet.
- Errors come back as ProblemDetail with a `code` property. Each operation in the spec lists the codes it can return. Known codes map to feature error types (implementing `Error`) in the feature's data layer. `DataError` stays generic. How the code travels from the safe call helper to the feature isn't built yet. Propose it when the first endpoint needs it.
- IDs are UUID strings, mapped to `Uuid` or kept as `String` consistently per feature. Instants are ISO date-time strings in UTC, mapped to `kotlin.time.Instant` in the mappers.
- `MockEngine` responses in tests follow the spec's DTOs, never a guess.

### Authentication

Scope is **register, login, refresh, logout only**. No email verification, no password reset, no change password, no account or settings screen (log out lives in a menu, as in Figma). Don't build anything beyond that unless asked.

- **Access token:** short-lived JWT, held **in memory only**, sent as `Authorization: Bearer` through Ktor's `Auth` bearer plugin.
- **Refresh token:** returned in the response body for mobile clients. Stored **encrypted**: iOS Keychain, Android Keystore-backed encryption. Both behind one interface in `:core:domain`, implemented with `expect`/`actual` on the platform APIs in `:core:data`. This replaces the plain DataStore storage in `kmp-data-layer`. Ask before adding a library for it.
- Refresh tokens rotate on every use. Persist the new refresh token before the refreshed request continues. Concurrent `401`s must lead to a single refresh call.
- On app start, restore the session by calling the refresh endpoint with the stored refresh token.
- Logout is triggered from menus in other features, so the logout call and session clearing sit behind an interface in `:core:domain`, implemented in `:core:data`.
- On logout or a failed refresh: clear both tokens, clear the Ktor bearer token cache, clear in-memory feature data, go to login. The "Session expired" state in Figma is shown when the session ended without an explicit logout.
- **Backend dependency:** the body-based refresh token for mobile needs a backend change. Until the spec shows it, don't build the refresh flow against a guessed contract. Ask.
- Rate limiting follows the shared `api-auth-tokens` and `api-errors` specs. Login, register and refresh are rate limited, counted per client IP, and login also per email. A request over the limit gets `429` with code `RATE_LIMITED` and a `Retry-After` header in whole seconds. Use only `Retry-After` to decide when to try again. There is no design for this state yet, so ask before building its UI.

### Local data

The app is network-only for now. There is no Room database and no offline-first sync. Data loaded from the backend lives in memory (ViewModels or in-memory data sources). Ignore the Room, DataStore and offline-first parts of `kmp-data-layer` and `kmp-integration-testing`. Adding persistence is a decision for the user.

### Theme

Light and dark always follow the system setting (`isSystemInDarkTheme()`). There is no theme switch in the app and no dynamic color on Android. Figma tokens map to a custom Material 3 color scheme plus extended colors in `AppTheme`. Components read colors only from the theme.

### i18n

English only for now, but all user-facing text comes from compose string resources (`Res.string.*`), as in `kmp-compose-components`. No language switcher. Don't add other languages unless asked.

### Platform features

- **Files (import/export):** file picking and sharing/saving go through our own interfaces with `expect`/`actual` implementations: Activity Result APIs on Android, `UIDocumentPickerViewController` and the share sheet on iOS. No library.
- **Drag & drop:** not set up yet. If Compose foundation isn't enough, propose a library and ask before adding it.
- No push notifications, no background work, no deep links.

### Monitoring

No crash reporting and no analytics. Logging goes through Kermit and only in debug builds. Don't add a crash reporting or analytics SDK without asking.

### Platform support

Android minSdk 26 (target and compile SDK from the version catalog). iOS 18.2 and later. Phones and tablets on both platforms, in portrait and landscape.

---

## Accessibility

The rules in `kmp-compose-components` and `kmp-compose-architecture` apply. Specific to this project:

- Figma decides that cards move between columns **by drag & drop only**. Screen reader users (TalkBack, VoiceOver) get **semantic custom accessibility actions** on each card, such as move to another column and move up or down. These have no visible UI. Don't add other visible move UI (a "Move to…" menu) without a design.
- Known gap, accepted for now: users who can't drag and don't use a screen reader (some switch-access users) may have no way to move cards.
- Every interactive element has a content description from string resources and a touch target of at least 48dp.

---

## Testing

All test types below are expected. See `kmp-unit-testing`, `kmp-integration-testing` and `kmp-ui-testing` for the details.

| Type | Where | What's real | Runs on |
|---|---|---|---|
| Unit | `commonTest` of the module | Pure logic: ViewModels, validators, mappers, reducers. Fakes for collaborators, fake `Clock`, pinned non-UTC time zone. No I/O. | Android host + iOS simulator |
| Integration | `commonTest` of the data module | Data sources, `HttpClient`, safe call helpers, mappers. Network faked with Ktor `MockEngine`. | Android host + iOS simulator |
| UI | `commonTest`, classes named `*UiTest` | Screens, and Root + real ViewModel, via `runComposeUiTest`. Feature E2E flows with `MockEngine` at the boundary. | iOS simulator per commit. Android device only on request or before a release. |

- Write unit tests the adversarial way: list the break vectors first, then one test per distinct angle.
- `MockEngine` handlers return JSON shaped like the spec's DTOs, built with fixtures.
- Fakes, not mocks. No MockK or Mockito in `commonTest`.
- No tests against a running backend or any deployed environment.
- A test is done when it's green on Android and iOS.

### Commands

| Command | What it does |
|---|---|
| `./gradlew syncOpenApiSpec` | Copy the OpenAPI spec from `cardigan.backend.dir` to `api/openapi.json` |
| `./gradlew :androidApp:assembleDebug` | Build the Android app |
| `./gradlew testAndroidHostTest` | Unit and integration tests on the host JVM, all modules |
| `./gradlew iosSimulatorArm64Test` | All shared tests on the iOS simulator, all modules |
| `./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64` | Link the iOS framework |
| `./gradlew connectedAndroidDeviceTest` | Android device tests (needs a running emulator). Not a gate. |

Tasks that don't exist yet get added in the change that sets up the tool.

**The gates:** `:androidApp:assembleDebug`, `testAndroidHostTest`, `iosSimulatorArm64Test` and `:composeApp:linkDebugFrameworkIosSimulatorArm64` must all pass before a commit. When a change touches `iosApp/`, also build the iOS app with `xcodebuild` for the simulator.

---

## Git Workflow

- **Commit automatically** as soon as a logical chunk of work is done **and** all gates pass. Don't batch several chunks into one commit, and don't wait until the end of the session.
- **Never commit** when a gate fails or hasn't been run. Fix it first, or tell the user what is broken.
- Conventional Commits with a scope where it fits: `feat(boards): ...`, `fix(auth): ...`, `refactor: ...`, `test(cards): ...`, `build: ...`, `docs: ...`, `chore(deps): ...`. Imperative mood, lowercase, no trailing period.
- One logical change per commit. Auth changes (session and token storage, the HTTP client's auth handling) go into their own commits.
- A spec change (`api/openapi.json`) goes into its own commit, together with the mapper and code changes it forces.
- Commit locally only. **Never push.** The user pushes.
- Never amend or rewrite existing commits, never skip hooks (`--no-verify`), never commit `local.properties` or other secrets.

---

## Decision Rules

- **Never assume a decision the user hasn't made.** If something isn't specified, ask. Don't silently pick a default.
- **Never make product or design decisions.** That includes screens, states, layout, copy, validation rules and limits, what happens after an action, and anything else users see. Figma and the API spec answer these. If they don't, ask.
- Ask before adding a dependency, a third-party service or SDK, a new feature module, or a new `core` module.
- **If you think a different approach would be better than one the user decided on, propose it.** Explain the trade-off briefly, then keep following the existing decision until the user agrees to change it.
- Raise known trade-offs when they become relevant instead of solving them silently. Examples: a Figma design that conflicts with the API contract, a state that has no design, a breaking change in the spec, an accessibility gap, behavior that differs between Android and iOS.
- If a skill's example conflicts with this file, follow this file and mention the conflict.

---

## Open Items

Ask before building anything that depends on these:

- The backend's OpenAPI snapshot. The backend is still the Spring Initializr skeleton and doesn't have the snapshot folder yet. Until it exists, there is no contract to generate from.
- The backend change for the body-based refresh token for mobile.
- Designs for: rate limit (`429`), network error, offline.
- Whether logout gets a rate limit. The shared spec rate limits only login, register and refresh.
- Production URL and any staging environment.
- How ProblemDetail `code` values reach feature error types.
- Drag & drop implementation (Compose foundation or a library).
- Final breakpoints.

---

## Skill Map

| Skill | Use it when |
|---|---|
| `kmp-module-structure` | Setting up the layout, adding a module, convention plugins, `build-logic`, version catalog, adding a dependency |
| `kmp-navigation` | Adding a screen or route, nav graphs, cross-feature navigation, back handling |
| `kmp-presentation-mvi` | Creating a screen, ViewModel, State/Action/Event, Root/Screen split, UiText, UI models, `SavedStateHandle` |
| `kmp-compose-architecture` | Deciding where state lives, side effects, lazy layouts, recomposition, screen previews |
| `kmp-compose-components` | Building any composable that isn't a Root/Screen, design system work, icons, string resources, previews, accessibility |
| `kmp-data-layer` | Calling an endpoint, data sources, DTO mappers, domain models, the Ktor client, token storage |
| `kmp-error-handling` | `Result`, `DataError`, feature error types, safe call helpers, mapping errors to `UiText` |
| `kmp-di-koin` | Koin modules, platform modules, `initKoin`, injecting ViewModels |
| `kmp-unit-testing` | Tests for ViewModels, validators, mappers and other pure logic |
| `kmp-integration-testing` | Tests that cross the network boundary with `MockEngine` |
| `kmp-ui-testing` | Compose UI tests with `runComposeUiTest`, feature E2E flows, the Robot pattern |
| `figma:figma-design-to-code` | Before every `get_design_context` call |
