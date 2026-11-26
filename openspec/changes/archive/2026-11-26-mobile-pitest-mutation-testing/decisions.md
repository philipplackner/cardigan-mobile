# Decisions: mobile-pitest-mutation-testing

Confirmed with the user on 2026-11-23. The orchestrator session (`cardigan-f2`) asked the questions and relayed the answers, so they could be batched with the backend and web changes. The careful-planning interview was not run in this session.

Amended on 2026-11-24 (D8, D10, D11 changed; D18–D20 added): the mutation report now leaves out Compose UI code and other code that adds no meaningful mutants, so the total score mostly reflects testable logic. Relayed by `cardigan-f2`.

Scope given by the user:
- One Gradle task the user runs by hand that produces an HTML mutation testing report.
- Not part of the development workflow: no build, check or CI integration, no agent hooks, no git hooks, no thresholds that fail builds unless explicitly passed.
- It only sets up the starting point for measuring mutation scores.

## Summary

Add a manual `mutationReport` task that runs PIT on the Android host JVM tests of the selected KMP modules and writes one aggregated HTML report plus a terminal summary. A minimum score is an opt-in property. Incremental analysis was dropped (D12). Task and property names match the backend. Normal builds, tests and the gates are unaffected. Compose UI code, design tokens, navigation wiring and compiler-generated code are not mutated (D10, D11).

## Decisions

### D1 — How PIT is wired into Gradle
- **Question**: Which Gradle integration runs PIT?
- **Decision**: Our own convention plugin that runs PIT's command line entry point (`pitest-command-line`) in a `JavaExec` task, using the classpath and test class dirs of each module's `testAndroidHostTest` task.
- **Alternatives**: `info.solidsoft.pitest` (needs the `java` plugin's source sets); `pl.droidsonroids.pitest` (needs `com.android.library`/`com.android.application` variants).
- **Why**: Neither plugin fits modules built on the AGP 9 `com.android.kotlin.multiplatform.library` target.
- **Depends on**: —

### D2 — Which tests count
- **Question**: Which tests are used to kill mutants?
- **Decision**: Android host JVM tests only, the same set as `coverageReport`. `*UiTest` stays excluded.
- **Alternatives**: none practical. PIT only runs on the JVM.
- **Why**: Code tested only by `*UiTest` or iOS tests shows as not covered, which is accepted.
- **Depends on**: D1

### D3 — Modules that opt in
- **Question**: Which modules can be mutation tested, and how do they opt in?
- **Decision**: A new module convention plugin id `com.plcoding.cardiganapp.convention.mutation`, applied to all 20 KMP modules. `:androidApp` stays out. Modules without host tests are skipped with a note.
- **Alternatives**: domain and data modules only; reuse the `convention.kover` opt-in.
- **Depends on**: D1

### D4 — Choosing modules for a run
- **Question**: How does the user pick modules?
- **Decision**: `-Pmutation.modules=:core:domain,:feature:boards:data` on the root task. If the property is left out, all opted-in modules run. An empty value (`-Pmutation.modules=`) fails, and so does an empty entry in a list (`:core:domain,` or `:a,,:b`). Entries are trimmed and deduplicated. Unknown modules fail with the list of valid ones. All failures happen before any test runs. These are the rules of the backend's `coverage.modules`, and they apply to backend and mobile.
- **Alternatives**: per-module tasks only; both; the mobile `coverage.modules` rule, where an empty value means all modules.
- **Why**: Changed by the user after the first round, to match the backend.
- **Depends on**: D3

### D5 — Task name
- **Question**: What is the task called?
- **Decision**: `mutationReport`. The backend uses the same name.
- **Alternatives**: `pitest`, `mutationTest`.
- **Depends on**: —

### D6 — Report layout
- **Question**: One combined report or one per module?
- **Decision**: PIT runs once per selected module. `pitest-aggregator` merges the results into one HTML report at `build/reports/pitest/index.html`. Per-module reports stay in each module's `build/reports/pitest/`.
- **Alternatives**: per-module reports only; one PIT run over a merged classpath.
- **Why**: Each module's tests keep their own classpath.
- **Depends on**: D4

### D7 — Report formats and terminal output
- **Question**: Which outputs?
- **Decision**: HTML report plus a terminal summary: mutation score, test strength, mutant counts and the report path. PIT's XML is also written, because the aggregator needs it.
- **Alternatives**: HTML only.
- **Depends on**: D6

### D8 — Kotlin support
- **Question**: How to handle mutants in Kotlin compiler-generated code?
- **Decision**: No Kotlin plugin. Filter in config: `avoidCallsTo` for `kotlin.jvm.internal`, `kotlin.ResultKt` and `co.touchlab.kermit`, exclude data class `equals`, `hashCode`, `toString`, `copy` and `component*` methods, and exclude `$WhenMappings`, `$$serializer` and `$$inlined$` classes. Coroutine `invokeSuspend` bodies stay in, because they hold ViewModel logic.
- **Alternatives**: Arcmutate Kotlin plugin (commercial); `pitest-kotlin` (unmaintained); excluding all `invokeSuspend` methods (rejected 2026-11-24: hides ViewModel `launch` bodies, most of them killed).
- **Why**: Remaining noise (coroutine state machine branches, default argument bridges) is accepted for a baseline.
- **Known effect (accepted 2026-11-24)**: mutants in code inlined from `MutableStateFlow.update {}` can turn its compare-and-set loop into an endless loop. They end as `TIMED_OUT`, which counts as killed (D14), so the score is a few points higher than the tests deserve. Fixing it would need filtering by the inlined source lines (SMAP).
- **Amended**: 2026-11-24, added `co.touchlab.kermit`, `$$serializer` and `$$inlined$`.
- **Depends on**: D1

### D9 — Mutators
- **Question**: Which mutator set?
- **Decision**: `DEFAULTS`.
- **Alternatives**: `STRONGER`, `ALL`.
- **Depends on**: —

### D10 — Excluded code
- **Question**: What is left out of mutation?
- **Decision**: The coverage report's name and package patterns are the shared base: `*.generated.*`, `BuildKonfig`, `di` and `fixture` packages, `*Debug*Overlay*`, previews, `ComposableSingletons`, androidMain `actual`s (`*_androidKt`, `Android*`, `*Activity`). On top, a PIT-only list (D20): `$$serializer` and `$$inlined$` classes (D8), design tokens in `*.designsystem.theme` (`*Colors*`, `App*`, `ShadowsKt`, `TextStylesKt`, `TypographyKt`; `WindowBreakpoints` and `WindowClassKt` stay in), and navigation wiring in `*.navigation` packages (`*Route`, `*GraphKt`, `*NavigationKt`, `*NavHostKt` and their nested classes; `BoardListReloader` and helpers outside `navigation` packages, such as `AppShellKt` and `SessionGateKt`, stay in).
- **Alternatives**: no exclusions; the shared list only.
- **Why**: tokens and nav graphs hold no testable logic but made up a large share of the uncovered mutants.
- **Amended**: 2026-11-24, added the PIT-only list.
- **Depends on**: D20

### D11 — Composable functions
- **Question**: How to treat `@Composable` code, which host tests don't run?
- **Decision**: Leave it out. PIT's built-in `FANN` filter skips methods annotated `@Composable` (the annotation has binary retention, and FANN reads it from the bytecode), which also covers composable lambda bodies. Its annotation list replaces PIT's defaults, so `Generated`, `DoNotMutate` and `CoverageIgnore` are listed again. A custom PIT filter (D18) also drops what FANN misses: methods that return `Modifier`, lambdas declared in a composable or `Modifier` function (`<name>$lambda…`), and classes enclosed by any of these, such as `LaunchedEffect` suspend lambdas. Test strength is still printed next to the score.
- **Earlier decision**: keep composables in as "no coverage" mutants (they were 88% of all mutants, so the total score was about 5%).
- **Alternatives**: FANN only (leaves the inner lambdas in); a custom filter for everything; name patterns.
- **Why**: Changed by the user on 2026-11-24, so the total score mostly reflects testable logic: ViewModels, reducers, domain, data and mappers.
- **Depends on**: D10, D18

### D12 — Incremental analysis (dropped)
- **Question**: Use PIT's history files for faster reruns?
- **Decision**: Dropped during implementation. There is no `mutation.incremental` property; every run analyzes all mutants. The backend drops it too. Web keeps Stryker's own incremental mode.
- **Earlier decision**: opt-in through `-Pmutation.incremental` (bare flag or `true` on, `false` off, anything else fails), with history under `build/`.
- **Why**: Open-source PIT has had no history store since 1.25.8; incremental history now needs the commercial Arcmutate plugin. With a history file set, PIT 1.30.x fails with "History has been enabled but no history plugin has been installed/activated" (found by the backend).
- **Alternatives**: always off; always on.
- **Depends on**: —

### D13 — Threads and timeouts
- **Question**: How many threads and which timeouts?
- **Decision**: Threads = available processors, overridable with `-Pmutation.threads=<n>`. PIT's default timeouts.
- **Alternatives**: fixed thread count; longer timeouts.
- **Depends on**: —

### D14 — Thresholds
- **Question**: Can a low score fail the run?
- **Decision**: Only with an optional `-Pmutation.minScore=<0-100>`. It is never set by default and fails the run only when passed and not met. The backend has the same property; web gets an optional break value.
- **Score**: killed / all mutants. Killed means PIT's detected statuses: `KILLED`, `TIMED_OUT`, `MEMORY_ERROR`, `RUN_ERROR` and `NON_VIABLE`, so the terminal numbers match the HTML report. Aligned with the backend by the user on 2026-11-24.
- **Alternatives**: no thresholds at all.
- **Depends on**: D7

### D15 — New dependencies
- **Question**: Approve adding PIT to the version catalog?
- **Decision**: Yes: `pitest-command-line` and `pitest-aggregator`, latest stable, pinned in the catalog, resolved only when the task runs. No JUnit plugin, because PIT supports JUnit 4 itself.
- **Depends on**: D1, D6

### D16 — Docs
- **Question**: Where is it documented?
- **Decision**: Commands table and Tech Stack in `CLAUDE.md`, marked "Not a gate". No mutation policy in cardigan-shared-specs.
- **Alternatives**: README only; no docs.
- **Depends on**: —

### D17 — Cross-repo names
- **Question**: Do names match the other repos?
- **Decision**: Yes. `mutationReport`, `mutation.modules`, `mutation.threads` and `mutation.minScore` have the same names and meaning as on the backend.
- **Depends on**: D4, D5, D12, D13, D14

### D18 — Compose UI filter
- **Question**: Where does the custom PIT filter live and how does PIT load it?
- **Decision**: A Java `java-library` project `pitest-filters` in the `build-logic` included build, compiled against `pitest-entry` (`compileOnly`, same pinned PIT version as D15). It registers a `MutationInterceptorFactory` (feature `FCOMPOSEUI`, on by default) in `META-INF/services`. The module PIT task adds it to PIT's main-process classpath. The root `settings.gradle.kts` also includes `build-logic` as a normal build, because a build included only in `pluginManagement` doesn't substitute normal dependencies. Unit tests with synthetic ASM `ClassNode`s live in the `convention` project's tests.
- **Alternatives**: classes inside the convention plugin jar; Kotlin (would put kotlin-stdlib on PIT's classpath).
- **Why**: `pitest-entry` is part of the approved PIT dependency (confirmed by the user on 2026-11-24). Java keeps the plugin free of other dependencies.
- **Depends on**: D11, D15

### D19 — `:core:designsystem` stays in
- **Question**: Should `:core:designsystem` still be mutation tested once composables and tokens are left out?
- **Decision**: Yes. What's left is real logic, such as `WindowClassKt` and `AnchoredPositionProvider`.
- **Alternatives**: drop its opt-in.
- **Depends on**: D3, D11

### D20 — Shared and PIT-only exclusions
- **Question**: Do Kover and PIT still share one exclusion list?
- **Decision**: `CodeExclusions` stays the shared base and Kover is unchanged. PIT adds `MutationExclusions` (D10), FANN and the Compose UI filter (D11, D18) on top, so the mutation report excludes more code than the coverage report.
- **Alternatives**: move the PIT extras into the shared list (would change coverage numbers); fully separate lists.
- **Depends on**: D10

## Facts

- KMP modules use the AGP 9 `com.android.kotlin.multiplatform.library` target with a host test (`testAndroidHostTest`) and a device test. No `java` plugin, no `com.android.library`. — source: `build-logic/convention/src/main/kotlin/com/plcoding/cardiganapp/convention/KotlinMultiplatform.kt`
- Host tests run on JUnit 4 (`kotlin-test` resolves `kotlin-test-junit` and `junit:junit:4.13.2`). — source: `./gradlew :core:domain:dependencies --configuration androidHostTestRuntimeClasspath`
- The host `Test` tasks exclude `*UiTest`. — source: `KotlinMultiplatform.kt`
- 20 KMP modules opt into Kover. `:feature:cards:*` and `:feature:labels:*` have no tests yet. — source: `settings.gradle.kts`, `openspec/specs/test-coverage/spec.md`
- Coverage exclusions are in `applyCoverageExclusions`. — source: `build-logic/convention/src/main/kotlin/com/plcoding/cardiganapp/convention/Coverage.kt`
- Composables are spread over files without a naming pattern (`BoardColumnParts.kt`, `BoardDialogs.kt`, `BoardsSidebarSection.kt`). — source: `feature/boards/presentation/src/commonMain`
- `@Composable` is stored as an invisible (binary retention) method annotation, on composable functions and on composable lambda bodies, which Kotlin 2.4 compiles to static `<name>$lambda$N` methods. — source: `javap -v` on `core/designsystem` classes
- PIT 1.30's `FANN` filter reads invisible annotations, but doesn't drop non-composable lambdas declared inside a composable. — source: prototype run on 2026-11-24 (516 such mutants left)

## Out of scope

- Build, check, CI, git hook or agent hook integration.
- iOS and Android device tests.
- Making `*UiTest` run on the host.
- Diff-based mutation testing against a branch.

## Deferred

None.
