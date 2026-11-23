# test-coverage Specification

## Purpose

Lets developers measure from the terminal how much of the mobile Kotlin code the Android host tests cover. They can do this for any set of modules, and for only the lines changed against a base branch.

## Requirements

### Requirement: Modules opt in with one plugin id
A module SHALL be included in coverage reports only if it applies the coverage convention plugin. Applying that one plugin id SHALL be the only step a module needs to opt in. Every KMP module opts in. `:androidApp` does not.

#### Scenario: Opted-in module is reported
- **WHEN** a developer runs `./gradlew coverageReport` without `coverage.modules`
- **THEN** the terminal summary shows 20 modules, and the report contains classes from the packages of `:core:data`, `:feature:boards:presentation` and `:composeApp`

#### Scenario: Module without the plugin is left out
- **WHEN** a developer runs `./gradlew coverageReport` without `coverage.modules`
- **THEN** the report contains no classes from `:androidApp`

### Requirement: Coverage comes from Android host tests only
The coverage data SHALL come only from the Android host JVM test run of each selected module. iOS tests, Android device tests and tests excluded from the host run (`*UiTest`) SHALL NOT contribute coverage.

#### Scenario: Host tests are run for the report
- **WHEN** a developer runs `./gradlew coverageReport -Pcoverage.modules=:core:domain`
- **THEN** the Android host tests of `:core:domain` run, and no iOS simulator or device tests run

#### Scenario: Failing host test
- **WHEN** a host test in a selected module fails
- **THEN** the command fails with the test failure, and the XML and HTML reports at the fixed paths are not created or updated in this run, also when the command is run with `--continue`

### Requirement: Normal test runs are not instrumented
Coverage instrumentation SHALL be active only when `coverageReport` or `coverageDiff` is requested in the same build. Any other run, including the `testAndroidHostTest` gate, SHALL run tests with no Kover agent in the test JVM's arguments and SHALL write no Kover binary report under any module's `build/kover/`.

#### Scenario: Gate run
- **WHEN** a developer runs `./gradlew testAndroidHostTest`
- **THEN** the tests run without coverage instrumentation, and no coverage data is written
- **AND** the host `Test` tasks' JVM arguments contain no `-javaagent` for Kover

#### Scenario: Coverage run
- **WHEN** a developer runs `./gradlew coverageReport`
- **THEN** the host tests of the selected modules run with instrumentation, and the report contains their coverage

#### Scenario: Gate run after a coverage run
- **WHEN** a developer runs `./gradlew coverageReport` and then `./gradlew testAndroidHostTest`
- **THEN** the second run doesn't reuse instrumented test results as if they were plain ones, and it runs without instrumentation

#### Scenario: Coverage run after a gate run
- **WHEN** a developer runs `./gradlew testAndroidHostTest` and then `./gradlew coverageReport -Pcoverage.modules=:core:domain`
- **THEN** the host tests of `:core:domain` run again with instrumentation, and the report shows covered lines for `normalizePassword`

### Requirement: Coverage tasks refuse to run uninstrumented
If `coverageReport`, `coverageDiff` or any Kover report task is about to run while instrumentation is off, for example because it was called by an abbreviated name, the build SHALL fail before any task runs, with the message "Coverage needs instrumentation. Run with the full task name, e.g. ./gradlew coverageReport." No test SHALL run and no report SHALL be written. `check` and `build` SHALL NOT run any Kover task.

#### Scenario: Abbreviated task name
- **WHEN** a developer runs `./gradlew cR`, `./gradlew cD` or `./gradlew kXR`
- **THEN** the build fails with that message before any task runs, and no test runs

#### Scenario: Repeated abbreviated run
- **WHEN** a developer runs `./gradlew cR` twice in a row with configuration cache on
- **THEN** both runs fail the same way

#### Scenario: check and build are unaffected
- **WHEN** a developer runs `./gradlew :core:domain:check`
- **THEN** no Kover task runs, and the build doesn't fail with the instrumentation message

### Requirement: Excluded code
The reports SHALL leave out every `@Composable` function, previews included. They SHALL also leave out generated code, Koin DI module definitions, debug-only fixtures and androidMain `actual` implementations. These exclusions SHALL be matched by annotation or name pattern, so new code that follows the project layout is excluded without a build change.

#### Scenario: Composable function is excluded
- **WHEN** a selected module contains a `@Composable` screen and a ViewModel
- **THEN** the report lists the ViewModel and does not list the screen function

#### Scenario: Generated and wiring code is excluded
- **WHEN** a report is generated for all modules
- **THEN** it lists no BuildKonfig class, no Compose resources (`Res`) class, no class in a `di` or `fixture` package, and no class whose name matches `*Debug*Overlay*` (today `DebugSessionOverlay`)

#### Scenario: androidMain actual is excluded
- **WHEN** a report is generated for `:core:domain`
- **THEN** the Android `actual` of `toNfc` is not listed, and the common code that calls it is

### Requirement: Report for a chosen set of modules
`coverageReport` SHALL accept an optional `coverage.modules` property: a comma-separated list of Gradle project paths. With the property, the report SHALL cover exactly those modules. Without it, the report SHALL cover every opted-in module. An unknown path, or a module that hasn't opted in, SHALL fail the command before any test runs. Entries are trimmed, empty entries and duplicates are ignored, and an empty value means every opted-in module.

#### Scenario: Single module
- **WHEN** a developer runs `./gradlew coverageReport -Pcoverage.modules=:feature:boards:data`
- **THEN** the report contains only classes from `:feature:boards:data`

#### Scenario: Combination of modules
- **WHEN** a developer runs `./gradlew coverageReport -Pcoverage.modules=:core:data,:feature:boards:data`
- **THEN** one merged report contains the classes of both modules and no others

#### Scenario: Module that can't be reported
- **WHEN** `coverage.modules` contains `:feature:nope` or `:androidApp`
- **THEN** the command fails with a message that names the bad entry and lists the modules that can be selected

### Requirement: Report outputs
`coverageReport` SHALL write one merged XML report and one merged HTML report to fixed paths under the root `build/reports/` directory. After writing them, it SHALL print the total line and branch coverage of the selected modules, and the path of the HTML report, to the terminal.

#### Scenario: Successful report
- **WHEN** `coverageReport` finishes
- **THEN** the XML and HTML reports exist, and the terminal shows the total line coverage and branch coverage as percentages, plus the HTML report path

### Requirement: Optional coverage thresholds
`coverageReport` SHALL accept optional `coverage.minLine` and `coverage.minBranch` properties, each a percentage from 0 to 100. Values are decimal numbers from 0 to 100 inclusive. When one is given and the total for the selected modules is strictly below it, the command SHALL fail after the reports are written. Without them, low coverage SHALL never fail the command. No other task, gate or CI SHALL enforce coverage.

#### Scenario: No threshold
- **WHEN** a developer runs `./gradlew coverageReport` with 10% line coverage
- **THEN** the command succeeds

#### Scenario: Threshold not met
- **WHEN** a developer runs `./gradlew coverageReport -Pcoverage.minLine=80` and total line coverage is 72%
- **THEN** the reports are written, and the command fails with a message that shows the actual value and the threshold

#### Scenario: Threshold met
- **WHEN** a developer runs `./gradlew coverageReport -Pcoverage.minBranch=50` and total branch coverage is 61%
- **THEN** the command succeeds

#### Scenario: Invalid threshold
- **WHEN** `coverage.minLine` is `abc` or `120`
- **THEN** the command fails before any test runs, with a message that the value must be a number from 0 to 100

### Requirement: Diff coverage against a base branch
`coverageDiff` SHALL report coverage only for lines changed against a base ref. Committed, staged, unstaged and untracked changes all count. The base SHALL be `origin/master` unless `coverage.compareBranch` names another ref. It SHALL accept `coverage.modules` with the same meaning and validation as `coverageReport`, and it SHALL build that report first.

#### Scenario: Default base
- **WHEN** a developer on a feature branch runs `./gradlew coverageDiff`
- **THEN** the terminal shows, for each changed Kotlin file in the selected modules, the percentage of changed lines covered and the uncovered line numbers, plus the overall diff coverage

#### Scenario: New untracked file
- **WHEN** a developer adds a new, untracked Kotlin file with tested code in a selected module and runs `./gradlew coverageDiff`
- **THEN** that file's lines are included in the diff coverage

#### Scenario: Other base
- **WHEN** a developer runs `./gradlew coverageDiff -Pcoverage.compareBranch=HEAD~3`
- **THEN** only lines changed since `HEAD~3` are reported

#### Scenario: Changed lines that aren't measured
- **WHEN** the diff changes only excluded code, non-Kotlin files or modules outside the selection
- **THEN** those lines are not counted, and the command still succeeds

#### Scenario: Base ref missing
- **WHEN** `coverage.compareBranch` names a ref that doesn't exist
- **THEN** the command fails with a message that names the ref

### Requirement: Diff coverage output and exit
`coverageDiff` SHALL print its result to the terminal and write an HTML report to a fixed path under the root `build/reports/` directory. It SHALL NOT fail because diff coverage is low.

#### Scenario: Low diff coverage
- **WHEN** none of the changed lines are covered
- **THEN** the command succeeds, prints 0% diff coverage, and writes the HTML report

### Requirement: Diff coverage tool is fetched on demand
`coverageDiff` SHALL run a pinned diff-cover version through `uv`, without a separate install step. If `uv` isn't available, the command SHALL fail with a message that says `uv` is needed and how to install it.

#### Scenario: uv missing
- **WHEN** a developer without `uv` on the `PATH` runs `./gradlew coverageDiff`
- **THEN** the command fails with a message that names `uv` and links its install instructions

#### Scenario: Same version for everyone
- **WHEN** two developers run `./gradlew coverageDiff` on the same commit
- **THEN** both runs use the same diff-cover version and resolve the same versions of its dependencies
