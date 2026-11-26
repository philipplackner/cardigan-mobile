# Test-first workflow: project facts

The tdd-* agents and the `openspec-apply-tdd` skill read this file for
everything that depends on this codebase. Written by `install-openspec-tdd`;
keep it current when the build or the test setup changes. Where test code
lives is configured in `tdd.conf` next to this file, which the hook and the
scripts use.

## Schemas

- Test-first schema: `careful-planning-tdd`
- Plain schema (changes with nothing a test could observe): `careful-planning`
- The `Workflow` line of a change (test-first or plain, and which parts) is in: `decisions.md` of the change

## Project rules

Read these for code style, architecture, commit rules and test conventions:

- `AGENTS.md` (`CLAUDE.md` is a symlink to it). It wins over every skill. Its Testing, Decision Rules and Git Workflow sections apply in full.
- `.claude/skills/kmp-unit-testing/SKILL.md`: unit tests, the adversarial break-vector method, fakes, dispatchers.
- `.claude/skills/kmp-integration-testing/SKILL.md`: data sources and `HttpClient` with Ktor `MockEngine`. Ignore its Room/DataStore/offline-first parts (AGENTS.md: no local persistence).
- `.claude/skills/kmp-ui-testing/SKILL.md`: `runComposeUiTest`, Root + real ViewModel tests, the Robot pattern.
- `.claude/skills/kmp-presentation-mvi/SKILL.md`, `kmp-data-layer`, `kmp-error-handling`, `kmp-di-koin`, `kmp-navigation`, `kmp-compose-components`, `kmp-compose-architecture`, `kmp-module-structure`: production code patterns.
- `openspec/config.yaml`: shared product behavior lives in the `cardigan-shared-specs` reference.

## Test code

- Where tests live: the `commonTest` source set of the module under test (`<module>/src/commonTest/kotlin/...`, same package as the code). Platform-only tests go in `androidHostTest` or `iosTest`. `androidDeviceTest` holds device-only UI tests and is not a gate. The `build-logic` included build has JVM tests in `build-logic/convention/src/test/`.
- Frameworks and conventions: `kotlin.test`, AssertK, Turbine, kotlinx-coroutines-test, Ktor `MockEngine`, Compose `ui-test` (`runComposeUiTest`). Fakes, never mocks (no MockK or Mockito in `commonTest`). Fake `Clock` and a pinned non-UTC time zone. `MockEngine` handlers return JSON shaped like the DTOs in `api/openapi.json`, built with fixtures, never a guess. Classes for UI tests end in `UiTest`. No tests against a running backend. A test is done when it is green on Android host and iOS simulator.
- Test levels, cheapest first: unit (ViewModels, validators, mappers, reducers, in `commonTest`, no I/O) → integration (data sources + `HttpClient` + safe call helpers + mappers, network faked with `MockEngine`, in the data module's `commonTest`) → UI (`*UiTest`, screens and Root + real ViewModel through `runComposeUiTest`).

## Commands

| Purpose | Command |
|---|---|
| Full check. Must pass before every commit | `./gradlew :androidApp:assembleDebug testAndroidHostTest iosSimulatorArm64Test :composeApp:linkDebugFrameworkIosSimulatorArm64`. When a change touches `iosApp/`, also build the iOS app with `xcodebuild` for the simulator. When it touches `build-logic/`, also `./gradlew -p build-logic :convention:test`. |
| Compile or type-check the tests without running them | `./gradlew compileAndroidHostTestSources compileTestKotlinIosSimulatorArm64` (one module: prefix both with `:<module>:`). For `build-logic`: `./gradlew -p build-logic :convention:compileTestKotlin`. |
| Run all tests | `./gradlew testAndroidHostTest iosSimulatorArm64Test` (plus `./gradlew -p build-logic :convention:test` for build-logic) |
| Run the tests of one area (`<area>` as defined below) | `./gradlew <area>:testAndroidHostTest <area>:iosSimulatorArm64Test`, for example `./gradlew :feature:boards:data:testAndroidHostTest :feature:boards:data:iosSimulatorArm64Test`. Filter host tests with `--tests '<fully qualified class>'`. For `build-logic`: `./gradlew -p build-logic :convention:test`. |

Run tests so they never write new expected-value files (snapshot or golden
files) on their own: the project has no snapshot, golden or screenshot tests,
so no flag is needed. Do not add a snapshot or screenshot test tool without
asking the user.

## Areas

An area is one Gradle module. Map a changed file to its area by its path up
to `src/`, with `/` replaced by `:`:

- `core/<name>/...` → `:core:<name>` (`domain`, `data`, `presentation`, `designsystem`)
- `feature/<feature>/<layer>/...` → `:feature:<feature>:<layer>` (features `auth`, `boards`, `cards`, `labels`, `transfer`; layers `domain`, `data`, `presentation`)
- `composeApp/...` → `:composeApp`
- `androidApp/...` → `:androidApp` (thin shell, no tests)
- `iosApp/...` → no Gradle area; check with `xcodebuild` for the simulator
- `build-logic/convention/...` and `build-logic/pitest-filters/...` → the `build-logic` included build, run with `-p build-logic` as above
- `api/openapi.json` → `:core:data` (DTOs are generated from it into `:core:data`'s build directory)

## Scaffold

- A scaffold body is: `TODO()` in Kotlin (`= TODO()` for functions and getters, `get() = TODO()` for properties). Data classes, sealed interfaces, enums, State/Action/Event types and interfaces are written in full, since they have no logic. A composable scaffold is an empty body with the full parameter list.
- Calling it fails with: `kotlin.NotImplementedError: An operation is not implemented.`

## Acceptance tests

- Level: feature UI through the Root composable with a real ViewModel, run with `runComposeUiTest` in the feature presentation module's `commonTest` (`*UiTest` classes). Collaborators below the ViewModel (data sources, repositories, `SessionManager`) are fakes from the module's test code.
- How they are written: build the Root with a real ViewModel and fake data sources, drive it through the UI, and assert what the user sees. Find nodes by text loaded from the module's string resources, by content description from string resources, or by a test tag, exactly as the design names them. Existing examples: `feature/auth/presentation/src/commonTest/.../login/LoginUiTest.kt`, `feature/boards/presentation/src/commonTest/.../detail/BoardRootUiTest.kt`. When a flow crosses features, the test goes in `:composeApp` like `composeApp/src/commonTest/.../AppFlowsUiTest.kt`. If the design does not name a string resource, content description or test tag that a test needs to find an element, stop and report it as an open question; do not invent one. Use the Robot pattern from `kmp-ui-testing` for multi-step flows.
- They use only the public interface of that level, never types or
  functions the change adds.

## Mutation testing

- Tool: PIT, through the project's own `mutation` convention plugin. It covers Android host tests of KMP modules only (no iOS, no `build-logic`).
- Command for one area: `./gradlew mutationReport -Pmutation.modules=<area>`, for example `-Pmutation.modules=:feature:boards:data`. Several areas: comma-separated; they run one after another. `-Pmutation.threads=<n>` sets PIT threads. Use the full task name; abbreviations fail.
- Report: `<module dir>/build/reports/pitest/mutations.xml` per module (PIT XML, one `<mutation>` element with `status`, `sourceFile`, `mutatedClass`, `mutatedMethod`, `lineNumber`, `mutator`), and a combined HTML report at `build/reports/pitest/index.html`.
- Survived: `status='SURVIVED'` or `status='NO_COVERAGE'`.
- Already excluded by rule (do not chase them): `@Composable` functions and their lambdas, `Modifier` functions, `inline` functions, `NavController` receivers, previews, `di` and `fixture` packages, generated code, design tokens, routes and graphs, `equals`/`hashCode`/`toString`/`copy`/`componentN`. See AGENTS.md, Testing.
- A run takes minutes per module. Not a gate.

## Commits

- Conventional Commits with a scope where it fits (`feat(boards): ...`, `fix(auth): ...`, `test(cards): ...`, `refactor: ...`, `build: ...`, `docs: ...`, `chore(deps): ...`). Imperative mood, lowercase, no trailing period.
- Commit only when the full check passes. One logical change per commit. Auth changes (session and token storage, the HTTP client's auth handling) get their own commits. A change to `api/openapi.json` goes in its own commit with the mapper and code changes it forces.
- Commit locally only. Never push, never amend or rewrite commits, never use `--no-verify`, never commit `local.properties`.
- End every commit message with the attribution line from the session's system instructions, if one is given.
