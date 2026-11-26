# Spec Delta

## Purpose

Lets developers check the mobile Kotlin code against the project's ktlint rule settings and auto-format it from the terminal, without enforcing either in the normal workflow.

## ADDED Requirements

### Requirement: One command checks the whole codebase
Running `./gradlew ktlintCheck` SHALL check every Kotlin source and test file of every module, all `*.gradle.kts` scripts and the build-logic sources against the rule settings. It SHALL fail when there is at least one violation and pass when there are none. When run with `--continue`, it SHALL check every project and print every violation with file, line and rule in the console. Without `--continue` it MAY stop after the first failing task.

#### Scenario: Clean codebase
- **WHEN** a developer runs `./gradlew ktlintCheck` on code with no violations
- **THEN** the build succeeds

#### Scenario: Violation in a module source
- **WHEN** a file in `feature/boards/presentation/src/commonMain` has a line longer than 120 characters and a developer runs `./gradlew ktlintCheck`
- **THEN** the build fails and the console names that file, the line and the max-line-length rule

#### Scenario: Violation in an iOS source set
- **WHEN** a file in an `iosMain` source set has a violation and a developer runs `./gradlew ktlintCheck`
- **THEN** the build fails and names that file

#### Scenario: Violation in a build script or build-logic
- **WHEN** a violation is in a `*.gradle.kts` script or in a build-logic convention plugin and a developer runs `./gradlew ktlintCheck`
- **THEN** the build fails and names that file

### Requirement: One command formats the whole codebase
Running `./gradlew ktlintFormat` SHALL rewrite every file in the same scope as `ktlintCheck` to fix all violations that ktlint can fix automatically. It SHALL report violations it cannot fix and fail if any remain.

#### Scenario: Fixable violation
- **WHEN** a file has a line with trailing spaces and a statement indented with 3 spaces, and a developer runs `./gradlew ktlintFormat`
- **THEN** the file is rewritten without the trailing spaces and with 4-space indentation, and a following `./gradlew ktlintCheck` reports no violation for it

#### Scenario: Fixable violation in build-logic and a build script
- **WHEN** a build-logic convention plugin and a module's `build.gradle.kts` each have a line with trailing spaces and a developer runs `./gradlew ktlintFormat` from the repo root
- **THEN** both files are rewritten without the trailing spaces

#### Scenario: Unfixable violation
- **WHEN** a function without `@Composable` is named `LoadBoards` and a developer runs `./gradlew ktlintFormat`
- **THEN** the build fails, the function name is unchanged, and the console names the file, the line and the function-naming rule

### Requirement: Generated code and build output are not checked
Files under any `build/` directory, including generated code, SHALL NOT be checked or formatted.

#### Scenario: Violation in generated code
- **WHEN** a module's generated Kotlin sources that are registered on a source set (the BuildKonfig output of `:core:data` or the compose resource accessors of `:core:designsystem`, under `build/generated/`) contain a violation, and a developer runs `./gradlew ktlintCheck`
- **THEN** no violation is reported for any file under that module's build directory

### Requirement: Rule settings come from the root .editorconfig
The rules SHALL use the `ktlint_official` code style with a maximum line length of 120, set in the root `.editorconfig`. The function-naming rule SHALL NOT report functions annotated with `@Composable`, and SHALL still report other functions. No custom rules SHALL run.

#### Scenario: PascalCase composable
- **WHEN** a `@Composable` function is named `BoardListScreen` and a developer runs `./gradlew ktlintCheck`
- **THEN** no function-naming violation is reported for it

#### Scenario: PascalCase regular function
- **WHEN** a function without `@Composable` is named `LoadBoards` and a developer runs `./gradlew ktlintCheck`
- **THEN** a function-naming violation is reported for it

### Requirement: Violations are written to report files
`./gradlew ktlintCheck` SHALL write its results as SARIF and Checkstyle report files under the `build/reports` directory of each checked project, in addition to the console output.

#### Scenario: Report files after a check
- **WHEN** a developer runs `./gradlew ktlintCheck` and it completes
- **THEN** for each ktlint check task that ran, a `.sarif` and a Checkstyle `.xml` report exist under `<project>/build/reports/ktlint/`, including at least `:core:domain`, `:androidApp` and build-logic's `:convention`

### Requirement: Checks run only when called
No other task, gate, git hook or build step SHALL run ktlint checks or formatting. Builds and tests SHALL succeed regardless of ktlint violations.

#### Scenario: Build with a violation
- **WHEN** the code has a ktlint violation and a developer runs `./gradlew check` or `./gradlew :androidApp:assembleDebug`
- **THEN** no ktlint task runs and the result does not depend on the violation

### Requirement: The codebase passes the check
After this change, `./gradlew ktlintCheck` SHALL pass on the repository. A violation that can only be fixed by changing behavior or a client-visible name SHALL be suppressed at that spot for that rule only.

#### Scenario: Fresh checkout
- **WHEN** a developer runs `./gradlew ktlintCheck` on a fresh checkout after this change
- **THEN** the build succeeds
