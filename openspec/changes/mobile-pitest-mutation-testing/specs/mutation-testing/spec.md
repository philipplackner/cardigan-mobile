# Spec Delta

## Purpose

Lets developers measure from the terminal how well the mobile Android host tests detect injected faults, for any set of modules, as an HTML mutation report.

## ADDED Requirements

### Requirement: One manual command produces an HTML mutation report
Running `./gradlew mutationReport` SHALL run mutation testing for the selected modules and write one HTML report at `build/reports/pitest/index.html`. It SHALL print the mutation score, test strength, mutant counts and the report path.

#### Scenario: Full run
- **WHEN** a developer runs `./gradlew mutationReport`
- **THEN** the HTML report exists at `build/reports/pitest/index.html` and contains mutants from `:core:domain` and `:feature:boards:presentation`
- **AND** the terminal shows the mutation score, test strength, mutant counts and the report path

### Requirement: Score and test strength use PIT's detected statuses
The mutation score SHALL be killed mutants divided by all mutants. Test strength SHALL be killed mutants divided by mutants with coverage. Mutants with the PIT statuses `KILLED`, `TIMED_OUT`, `MEMORY_ERROR`, `RUN_ERROR` and `NON_VIABLE` SHALL count as killed, for the summary and for `mutation.minScore`.

#### Scenario: Detected statuses count as killed
- **WHEN** a module's results contain mutants with the statuses `KILLED`, `TIMED_OUT`, `MEMORY_ERROR`, `RUN_ERROR` and `NON_VIABLE`
- **THEN** the summary counts all of them as killed, and its numbers match the HTML report

### Requirement: Modules opt in with one plugin id
A module SHALL be mutation tested only if it applies the mutation convention plugin. All KMP modules opt in. `:androidApp` does not. Opted-in modules without host tests SHALL be skipped with a note instead of failing. A module whose classes are all excluded SHALL be treated like a module without host tests. When no selected module produces mutants, the command SHALL succeed, write no combined report, print that there were no mutants, and ignore `mutation.minScore`.

#### Scenario: Module without the plugin
- **WHEN** a developer runs `./gradlew mutationReport -Pmutation.modules=:androidApp`
- **THEN** the build fails before any test runs and lists the valid modules

#### Scenario: Module without tests
- **WHEN** a developer runs `./gradlew mutationReport -Pmutation.modules=:feature:labels:domain`
- **THEN** the run succeeds, says the module has no host tests, prints that there were no mutants, and writes no combined report

#### Scenario: No mutants with a threshold
- **WHEN** a developer runs `./gradlew mutationReport -Pmutation.modules=:feature:labels:domain -Pmutation.minScore=80`
- **THEN** the run succeeds and prints that there were no mutants

### Requirement: Module selection
`-Pmutation.modules` SHALL take a comma-separated list of module paths, trimmed and deduplicated, and limit the run to them. Without the property, all opted-in modules SHALL run. An empty value, an empty list entry or an unknown module SHALL fail the build before any test runs.

#### Scenario: Property left out
- **WHEN** a developer runs `./gradlew mutationReport` without `mutation.modules`
- **THEN** all opted-in modules run

#### Scenario: Spaces and duplicates
- **WHEN** a developer runs `./gradlew mutationReport "-Pmutation.modules= :core:domain , :core:domain"`
- **THEN** only `:core:domain` runs, once

#### Scenario: Empty value
- **WHEN** a developer runs `./gradlew mutationReport -Pmutation.modules=`
- **THEN** the build fails before any test runs and names the property

#### Scenario: Empty entry
- **WHEN** a developer runs `./gradlew mutationReport -Pmutation.modules=:core:domain,` or `-Pmutation.modules=:core:domain,,:core:data`
- **THEN** the build fails before any test runs and names the property

#### Scenario: Selected modules
- **WHEN** a developer runs `./gradlew mutationReport -Pmutation.modules=:core:domain,:feature:boards:data`
- **THEN** the report contains mutants only from those two modules

#### Scenario: Unknown module
- **WHEN** a developer runs `./gradlew mutationReport -Pmutation.modules=:feature:nope`
- **THEN** the build fails before any test runs and lists the valid modules

### Requirement: Mutants are killed by Android host tests only
Mutants SHALL be tested only with each module's Android host JVM tests. `*UiTest`, iOS tests and Android device tests SHALL NOT run.

#### Scenario: No iOS or device tests
- **WHEN** a developer runs `./gradlew mutationReport -Pmutation.modules=:feature:transfer:presentation`
- **THEN** no iOS simulator test, device test or `*UiTest` class runs

### Requirement: Failing tests stop the run
If a host test of a selected module fails without mutation, the command SHALL fail with that test named. It SHALL delete any existing combined report before running PIT, and SHALL NOT write a new one.

#### Scenario: Red test suite
- **WHEN** a host test in `:core:domain` fails and a developer runs `./gradlew mutationReport -Pmutation.modules=:core:domain`
- **THEN** the command fails, names the failing test, and no combined report exists at `build/reports/pitest/index.html`, even if an earlier run wrote one

### Requirement: Excluded code
Generated code, Koin DI modules, debug-only fixtures, previews and androidMain `actual`s SHALL NOT be mutated. Design tokens in the design system theme and navigation routes and graphs SHALL NOT be mutated either. All of them SHALL be matched by name or package pattern, so new code that follows the project layout is excluded without a build change. Window size logic in the theme package and code outside `navigation` packages SHALL stay in.

#### Scenario: Generated and wiring code
- **WHEN** a full report is generated
- **THEN** it has no mutants in BuildKonfig, Compose resources (`Res`), `di` or `fixture` packages, `ComposableSingletons`, `*PreviewKt`/`*PreviewsKt` classes, or classes matching `*Debug*Overlay*`, `*_androidKt`, `*.Android*` or `*Activity`

#### Scenario: Tokens and navigation wiring
- **WHEN** a full report is generated
- **THEN** it has no mutants in `AppSpacing`, `LabelColors`, `ShadowsKt`, `*Route` classes or `*GraphKt`/`*NavHostKt` classes in `navigation` packages
- **AND** it still has mutants in `WindowClassKt`, `BoardListReloader` and `AppShellKt`

### Requirement: Compose UI code is not mutated
Methods annotated `@Composable`, methods that return `Modifier`, lambdas declared in either, and classes they enclose SHALL NOT be mutated. Other code in the same files SHALL stay in. The mutation report SHALL exclude this code although the coverage report only excludes `@Composable` methods.

#### Scenario: Screen file
- **WHEN** a report is generated for `:feature:boards:presentation`
- **THEN** it has no mutants in `BoardListScreen`, in its `onClick` lambdas, in `drawDividerAbove`, or in the `LaunchedEffect` lambdas of `BoardScreen`
- **AND** `BoardViewModel` still has mutants

#### Scenario: Non-UI function next to composables
- **WHEN** a report is generated for `:composeApp`
- **THEN** `destinationFor` in `SessionGateKt` and `openBoardId` in `AppShellKt` have mutants

### Requirement: Kotlin compiler noise is filtered
Calls into `kotlin.jvm.internal`, `kotlin.ResultKt` and `co.touchlab.kermit` SHALL NOT be mutated. Methods named `equals`, `hashCode`, `toString`, `copy`, `copy$default` and `component<N>`, and classes matching `*$WhenMappings`, `*$$serializer` or `*$$inlined$*`, SHALL NOT be mutated. Coroutine `invokeSuspend` methods SHALL stay in.

#### Scenario: No mutants in compiler-generated code
- **WHEN** a report is generated for `:feature:boards:domain`
- **THEN** it has no mutant in a `kotlin.jvm.internal.Intrinsics` call, in a `$WhenMappings` class, or in an `equals`, `hashCode`, `toString`, `copy` or `component<N>` method

#### Scenario: Serializers and inline copies
- **WHEN** a full report is generated
- **THEN** it has no mutant in a `$$serializer` class or a `$$inlined$` class
- **AND** `invokeSuspend` methods of `BoardViewModel` still have mutants

### Requirement: Thread count
The run SHALL use as many threads as the machine has processors. `-Pmutation.threads=<n>` SHALL override it with a positive whole number. Any other value SHALL fail the build before any test runs. Module runs SHALL NOT overlap, so the thread count is the total for the machine.

#### Scenario: Modules run one at a time
- **WHEN** a developer runs `./gradlew mutationReport -Pmutation.modules=:core:domain,:core:data`
- **THEN** PIT for `:core:data` starts only after PIT for `:core:domain` has finished, or the other way round

#### Scenario: Override
- **WHEN** a developer runs `./gradlew mutationReport -Pmutation.threads=2`
- **THEN** PIT runs with 2 threads

#### Scenario: Invalid value
- **WHEN** a developer runs `./gradlew mutationReport -Pmutation.threads=0`
- **THEN** the build fails before any test runs and names the property

### Requirement: Gradle never reuses mutation results
Mutation tasks SHALL never be up to date or restored from the build cache. Every run SHALL analyze all mutants and SHALL NOT read or write PIT history.

#### Scenario: Repeated run with the build cache on
- **WHEN** a developer runs `./gradlew mutationReport -Pmutation.modules=:core:domain` twice with no code change
- **THEN** the second run executes PIT, and no mutation task is reported as UP-TO-DATE or FROM-CACHE

### Requirement: Optional minimum score
Low scores SHALL NOT fail the command unless `-Pmutation.minScore=<0-100>` is passed. When it is passed and the total mutation score is below it, the command SHALL fail after writing the report and show the score and the threshold. The score is compared unrounded. A score equal to the threshold passes. A value outside 0 to 100 or not a number SHALL fail the build before any test runs.

#### Scenario: Score equals threshold
- **WHEN** the total mutation score is exactly the `mutation.minScore` value
- **THEN** `mutationReport` succeeds

#### Scenario: No threshold
- **WHEN** a selected module has a mutation score of 0% and no `mutation.minScore` is passed
- **THEN** `mutationReport` succeeds

#### Scenario: Threshold not met
- **WHEN** a developer runs `./gradlew mutationReport -Pmutation.modules=:core:domain -Pmutation.minScore=100` and the score is below 100
- **THEN** the report is written, and the command fails showing the score and the threshold

#### Scenario: Invalid threshold
- **WHEN** a developer runs `./gradlew mutationReport -Pmutation.minScore=abc`
- **THEN** the build fails before any test runs and names the property

### Requirement: Mutation testing stays out of the normal workflow
No other task SHALL run mutation testing or resolve PIT. No PIT task SHALL be in the task graph of `check`, `build`, `testAndroidHostTest` or any of the four gates, and none of them SHALL resolve a PIT artifact.

#### Scenario: Gate run
- **WHEN** a developer runs `./gradlew testAndroidHostTest` or `./gradlew :core:domain:check`
- **THEN** no mutation task runs and no PIT artifact is downloaded
