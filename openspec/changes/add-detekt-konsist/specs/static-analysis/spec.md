# Spec Delta

## Purpose

Lets developers run static analysis on the mobile codebase from the terminal on demand: detekt for code smells and likely bugs, Konsist for architecture rules, and a project rule set for custom detekt rules.

## ADDED Requirements

### Requirement: One command runs detekt on the whole codebase
Running `./gradlew detekt` SHALL run detekt on every module that applies the detekt convention plugin and on the build-logic included build. All KMP modules, `:androidApp` and `:tooling` opt in.

#### Scenario: Whole codebase
- **WHEN** a developer runs `./gradlew detekt`
- **THEN** detekt analyzes the Kotlin sources of every KMP module, `:androidApp`, `:tooling` and build-logic

#### Scenario: Module without the plugin
- **WHEN** a module does not apply the detekt convention plugin
- **THEN** `./gradlew detekt` does not analyze it

### Requirement: One module runs through its task path
A single module SHALL be analyzed with `./gradlew <module path>:detekt`. There SHALL be no property to select modules.

#### Scenario: Single module
- **WHEN** a developer runs `./gradlew :feature:boards:data:detekt`
- **THEN** only `:feature:boards:data` is analyzed

### Requirement: detekt fails on any finding
A detekt run SHALL fail when it reports at least one finding and SHALL succeed when it reports none. There SHALL be no baseline file: every finding in existing code counts.

#### Scenario: Finding present
- **WHEN** an analyzed file has a finding from an active rule
- **THEN** the detekt command fails and names the rule, file and line

#### Scenario: No findings
- **WHEN** no analyzed file has a finding
- **THEN** the detekt command succeeds

### Requirement: Production and test code are analyzed
detekt SHALL analyze the production and test source sets of every opted-in module. Generated sources under build directories SHALL NOT be analyzed.

#### Scenario: Test code
- **WHEN** a file in a module's `commonTest`, `androidHostTest`, `androidDeviceTest`, `iosTest` or `src/test` has a finding
- **THEN** `./gradlew detekt` reports it

#### Scenario: Generated code
- **WHEN** a generated file (compose resources, BuildKonfig, OpenAPI DTOs) would break a rule
- **THEN** `./gradlew detekt` does not report it

### Requirement: Type resolution where the platform allows it
Code compiled for Android (`commonMain`, `androidMain`, Android host tests and JVM code) SHALL be analyzed with type resolution. `iosMain` and `iosTest` SHALL be analyzed without type resolution, so rules that need type information are skipped there.

#### Scenario: iOS-only file
- **WHEN** a file in `iosMain` breaks a rule that does not need type information
- **THEN** `./gradlew detekt` reports it

#### Scenario: Shared file with a type-aware finding
- **WHEN** a file in `commonMain` breaks an active rule that needs type information
- **THEN** `./gradlew detekt` reports it

### Requirement: Default rules with project overrides
detekt SHALL use its default active rules, changed only by the overrides in `config/detekt/detekt.yml`. Rules that check formatting covered by ktlint SHALL be inactive, and no ktlint wrapper rules SHALL run.

#### Scenario: Formatting is left to ktlint
- **WHEN** a file's only problems are a 130-character line and trailing whitespace
- **THEN** `./gradlew detekt` reports no `MaxLineLength` and no `TrailingWhitespace` finding

### Requirement: build-logic uses default rules only
build-logic SHALL be analyzed with the same detekt version and `config/detekt/detekt.yml`, without the `cardigan` rule set.

#### Scenario: build-logic finding
- **WHEN** a convention plugin source file has a finding from a default rule
- **THEN** `./gradlew detekt` reports it and fails

### Requirement: HTML and SARIF reports
Each detekt run SHALL write an HTML and a SARIF report per analyzed module. `./gradlew detekt` SHALL also write one merged SARIF report under `build/reports/detekt/` of the root project.

#### Scenario: Reports after a run
- **WHEN** a developer runs `./gradlew detekt`, whether it passes or fails
- **THEN** each analyzed module has an HTML and a SARIF report, and the root has one merged SARIF report

### Requirement: Mocking library imports are reported in test code
The `cardigan` rule set SHALL contain the rule `NoMockingLibraries`. It SHALL report every import of a declaration in `org.mockito` or `io.mockk` or their subpackages, including star and aliased imports, in test source sets only.

#### Scenario: MockK import in a test
- **WHEN** a file in a test source set contains `import io.mockk.every`
- **THEN** detekt reports `cardigan:NoMockingLibraries` at that import

#### Scenario: Star and aliased imports
- **WHEN** a test file contains `import org.mockito.*` or `import org.mockito.Mockito.mock as m`
- **THEN** each import is reported

#### Scenario: Production code
- **WHEN** a file in `commonMain` imports `io.mockk.every`
- **THEN** `NoMockingLibraries` does not report it

#### Scenario: Suppressed import
- **WHEN** a test file annotated `@file:Suppress("NoMockingLibraries")` imports `io.mockk.every`
- **THEN** `NoMockingLibraries` does not report it

### Requirement: Mocking rule message
Each `NoMockingLibraries` finding SHALL have the message `Import of '<path>' is not allowed. Use fakes instead of mocking libraries.`, where `<path>` is the import path as written without an alias, including `.*` for star imports.

#### Scenario: Plain import message
- **WHEN** a test file contains `import io.mockk.every`
- **THEN** the finding's message is `Import of 'io.mockk.every' is not allowed. Use fakes instead of mocking libraries.`

#### Scenario: Star import message
- **WHEN** a test file contains `import io.mockk.*`
- **THEN** the finding's message is `Import of 'io.mockk.*' is not allowed. Use fakes instead of mocking libraries.`

#### Scenario: Aliased import message
- **WHEN** a test file contains `import org.mockito.Mockito.mock as m`
- **THEN** the finding's message is `Import of 'org.mockito.Mockito.mock' is not allowed. Use fakes instead of mocking libraries.`

### Requirement: Mocking rule is off unless configured
`NoMockingLibraries` SHALL report nothing unless the detekt config activates it. `config/detekt/detekt.yml` SHALL activate it.

#### Scenario: Not configured
- **WHEN** detekt runs with a config that does not mention the `cardigan` rule set
- **THEN** `NoMockingLibraries` reports nothing

### Requirement: Mocking rule ignores everything but those imports
`NoMockingLibraries` SHALL NOT report fully qualified references without an import, imports of packages that only share a name prefix with `org.mockito` or `io.mockk`, or imports that differ from them only in letter case.

#### Scenario: Fully qualified reference
- **WHEN** a test file calls `io.mockk.mockk<Foo>()` without importing it
- **THEN** `NoMockingLibraries` does not report it

#### Scenario: Similar package name
- **WHEN** a test file imports `io.mockkfake.Thing` or `org.mockitox.Thing`
- **THEN** `NoMockingLibraries` does not report it

#### Scenario: Different letter case
- **WHEN** a test file imports `Org.Mockito.Mockito`
- **THEN** `NoMockingLibraries` does not report it

### Requirement: Konsist checks run on demand
Running `./gradlew konsist` SHALL run all Konsist checks and nothing else. It SHALL fail when a check fails.

#### Scenario: Konsist run
- **WHEN** a developer runs `./gradlew konsist`
- **THEN** the Konsist checks run and the unit tests of the custom detekt rules do not

### Requirement: Domain code stays free of other layers and frameworks
A Konsist check SHALL fail when a file in a `*.domain` package of a core or feature domain module imports from `*.data`, `*.presentation`, `*.designsystem`, `io.ktor`, `org.koin`, `androidx` (including `androidx.compose`) or `org.jetbrains.compose`.

#### Scenario: Domain imports Ktor
- **WHEN** a file in `:feature:boards:domain` imports `io.ktor.client.HttpClient`
- **THEN** `./gradlew konsist` fails and names the file and import

#### Scenario: Clean domain
- **WHEN** domain files import only the Kotlin standard library, kotlinx libraries and other domain packages
- **THEN** the check passes

### Requirement: One command runs both tools
Running `./gradlew staticAnalysis` SHALL run detekt and the Konsist checks. It SHALL run both even when one fails, and fail afterwards if either failed.

#### Scenario: detekt fails
- **WHEN** detekt has a finding and the Konsist checks pass
- **THEN** `./gradlew staticAnalysis` still runs the Konsist checks, then fails

### Requirement: Static analysis stays on demand
detekt and Konsist SHALL run only when one of their tasks is requested. The gate commands, `testAndroidHostTest`, `check`, `coverageReport` and `mutationReport` SHALL NOT run them or include `:tooling`.

#### Scenario: Gate run
- **WHEN** a developer runs the gate commands
- **THEN** no detekt task and no Konsist check runs

#### Scenario: check run
- **WHEN** a developer runs `./gradlew check`
- **THEN** no detekt task and no Konsist check runs

### Requirement: Custom rule tests run separately
`./gradlew :tooling:test` SHALL run the unit tests of the custom detekt rules and SHALL NOT run the Konsist checks.

#### Scenario: Rule tests
- **WHEN** a developer runs `./gradlew :tooling:test`
- **THEN** the `NoMockingLibraries` tests run and no Konsist check runs
