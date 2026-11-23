# Decisions: add-test-coverage

Confirmed with the user on 2026-11-23. The orchestrator session (cardigan-3c) asked the questions and relayed the answers, so they could be batched with the backend and web changes. The careful-planning interview was not run in this session.

## Summary

Add the tooling to generate a test coverage report for the mobile code from the terminal. The report can cover one module, all modules, or any set of modules. A second command shows coverage only for the lines changed against a base branch. Nothing is enforced: no gates, no thresholds by default, no CI. No coverage policy is written into the shared specs or the platform specs.

## Decisions

### D1 — Coverage tool
- **Question**: Add Kover to the version catalog and build-logic?
- **Decision**: Yes. Use the latest stable Kover Gradle plugin, checked against Kotlin 2.4 and AGP 9. Wire it through a new convention plugin in `build-logic`, so a module opts in with one plugin id.
- **Alternatives**: AGP/JaCoCo.
- **Why**: Kover supports KMP host tests. JaCoCo has poor support for the KMP Android target.
- **Depends on**: —

### D2 — Which tests count
- **Question**: Which tests count toward coverage?
- **Decision**: Android host JVM tests only (`testAndroidHostTest`).
- **Alternatives**: host + on-device tests merged through AGP/JaCoCo.
- **Why**: Simple and fast. Device tests aren't a gate.
- **Depends on**: D1

### D3 — Compose code
- **Question**: How to treat Compose UI code, since `*UiTest` doesn't run on the host?
- **Decision**: Exclude every `@Composable` function from the report, previews included.
- **Alternatives**: keep it in and accept low numbers; run UI tests on the host with Robolectric.
- **Why**: The report then shows how well the logic is tested. UI stays unmeasured.
- **Depends on**: D2

### D4 — Other exclusions
- **Question**: What else is excluded?
- **Decision**: Generated code (BuildKonfig, Compose resources `Res` / `*.generated.*`, future OpenAPI DTOs), Koin DI modules, debug-only fixtures (`fixture/`, `DebugOverlay`) and androidMain `actual`s. Express the exclusions as patterns or annotations, not as lists of class names.
- **Alternatives**: any subset.
- **Why**: Cleaner numbers. The exclusions also hide that code from the diff report, which is accepted.
- **Depends on**: D3

### D5 — Modules that opt in
- **Question**: Which modules opt in from the start?
- **Decision**: All KMP modules: the four core modules, all 15 feature modules and `:composeApp`. `:androidApp` stays out.
- **Alternatives**: domain + data + core only; none.
- **Depends on**: D1

### D6 — Choosing modules for a report
- **Question**: How do you pick the modules for one report?
- **Decision**: A root aggregate task plus a property: `./gradlew coverageReport -Pcoverage.modules=:core:data,:feature:boards:data`. Without the property, the report covers every module that opted in.
- **Alternatives**: Kover's per-module tasks only; named presets.
- **Depends on**: D5

### D7 — Report formats
- **Question**: Which report formats?
- **Decision**: XML and HTML, plus a terminal summary of total line and branch coverage after the report.
- **Alternatives**: XML only; XML + HTML.
- **Depends on**: D6

### D8 — Thresholds on overall coverage
- **Question**: Should low coverage fail the build?
- **Decision**: Nothing fails by default. `coverageReport` accepts optional `-Pcoverage.minLine=<percent>` and `-Pcoverage.minBranch=<percent>`. When one is given and its total isn't met, the command fails. The thresholds aren't enforced anywhere else.
- **Alternatives**: a minimum per module that always fails the build; a minimum for the total only.
- **Depends on**: D6

### D9 — Threshold on changed lines
- **Question**: Should diff coverage fail under a threshold?
- **Decision**: Report-only. No `--fail-under`.
- **Alternatives**: fail under a given percentage.
- **Depends on**: D11

### D10 — Commit gates
- **Question**: Does coverage become a commit gate?
- **Decision**: No. It runs on demand only.
- **Alternatives**: Kover verify as a gate; diff-cover as a gate.
- **Depends on**: —

### D11 — Diff base
- **Question**: Which base does the diff compare against?
- **Decision**: `origin/master` by default, overridable with `-Pcoverage.compareBranch=<ref>`. Staged and unstaged changes count (diff-cover's default). Untracked files count too (D18).
- **Alternatives**: merge-base with local `master`; always required as a parameter.
- **Depends on**: —

### D12 — Running diff-cover
- **Question**: How is diff-cover installed and run?
- **Decision**: diff-cover is https://github.com/Bachmann1234/diff_cover. It runs as `uvx diff-cover==<pinned>` from a Gradle task: `./gradlew diffCoverage [-Pcoverage.modules=…] [-Pcoverage.compareBranch=…]`. The task runs the tests and builds the Kover XML first, then passes the KMP source roots. No shell script. Output goes to the terminal, plus an HTML report under `build/`.
- **Alternatives**: a repo shell script; pipx global install with documented commands.
- **Depends on**: D6, D11

### D13 — CI
- **Question**: CI or local only?
- **Decision**: Local only.
- **Alternatives**: a GitHub Actions workflow that posts diff coverage on PRs.
- **Depends on**: —

### D14 — Names shared across repos
- **Question**: Should the command names match the other Cardigan repos?
- **Decision**: Yes. Use the same task and property names as the backend: `coverageReport`, `diffCoverage`, `coverage.modules`, `coverage.compareBranch`, `coverage.minLine`, `coverage.minBranch`.
- **Depends on**: D6, D8, D12

### D15 — Where coverage policy lives
- **Question**: Should coverage policy go into cardigan-shared-specs?
- **Decision**: No. No shared-spec entry and no enforced rules in the platform specs. This repo's spec describes the tooling only.
- **Depends on**: D8, D9, D10

### D16 — Same diff-cover version in every repo
- **Question**: Which diff-cover version is pinned?
- **Decision**: 11.0.4, the version the web repo pins, so all repos match. Task 1.1 confirms it.
- **Alternatives**: pin each repo separately.
- **Depends on**: D12, D14

### D17 — Normal test runs stay uninstrumented
- **Question**: May Kover instrument or slow down normal test runs?
- **Decision**: No. Kover instruments tests only when `coverageReport` or `diffCoverage` is part of the run. A normal `testAndroidHostTest` gate run is not instrumented and not slower. If that can't be guaranteed, the implementation reports it as a finding instead of accepting the slowdown.
- **Alternatives**: accept the instrumentation overhead on every test run.
- **Why**: Coverage runs on demand only (D10).
- **Depends on**: D10

### D18 — Untracked files count
- **Question**: Do new, untracked files count in diff coverage?
- **Decision**: Yes. `diffCoverage` passes `--include-untracked`.
- **Depends on**: D11, D12

### D19 — Verifying build-logic
- **Question**: How are the convention plugins verified?
- **Decision**: Manually, by running the documented commands as listed in tasks.md. No Gradle TestKit tests.
- **Depends on**: —

### D20 — Coverage task run without instrumentation
- **Question**: What happens when `coverageReport` or `diffCoverage` runs without instrumentation (for example when called by an abbreviated name like `cR`)?
- **Decision**: The build fails before any task runs, so no tests run and no report is written. The message is "Coverage needs instrumentation. Run with the full task name, e.g. ./gradlew coverageReport." The approach and wording are the same as the backend repo.
- **Depends on**: D14, D17

## Facts

- 21 Gradle modules besides `build-logic`. `:androidApp` is a plain Android app with no tests. The other 20 are KMP modules built on `convention.kmp.library` — source: `settings.gradle.kts`, `build-logic/convention/src/main/kotlin/`
- KMP modules use the AGP 9 `com.android.kotlin.multiplatform.library` target with `androidHostTest` and `androidDeviceTest`, plus `iosArm64` and `iosSimulatorArm64` — source: `KotlinMultiplatform.kt`
- The host test run filters out `*UiTest` — source: `KotlinMultiplatform.kt`
- Kover added support for the KMP Android library plugin in 0.9.2, with follow-up fixes in 0.9.3, 0.9.7 and 0.9.8. The latest found release is 0.9.9 — source: Kover GitHub releases and CHANGELOG
- diff-cover needs `--src-roots` to map JaCoCo-format XML back to files. The web session found 11.0.4 as the latest release on PyPI — source: PyPI, relayed by the orchestrator
- Configuration cache is on. Isolated projects is off — source: `gradle.properties`
- androidMain files use the `.android.kt` suffix, except `SaveExportActivity.kt`. The only named class declared in androidMain besides the activity is the private `AndroidFileSharer` — source: `find … src/androidMain`
- Koin modules live in `di` packages. The fixture lives in a `fixture` package — source: `grep "module {"`
- uv and pipx are installed locally. diff-cover isn't — source: `which`
- No CI configuration exists in the repo.

## Out of scope

- iOS coverage and on-device Android coverage.
- Running `*UiTest` on the host.
- CI, PR comments, badges.
- Thresholds enforced anywhere, and commit gates.
- Coverage policy in cardigan-shared-specs or platform specs.

## Deferred

- The exact Kover version to pin — settle before: implementation (latest stable that works with Kotlin 2.4.20 and AGP 9.1.1).
