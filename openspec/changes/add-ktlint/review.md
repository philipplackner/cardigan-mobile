# Artifact review (light): add-ktlint

**Verdict:** Ready
**Reviewed:** 2026-11-26, schema `careful-planning`, repo kind `code`

| Pass | Blocker | Major | Minor |
|---|---|---|---|
| Testability | 0 | 1 | 4 |
| Security | 0 | 0 | 0 |
| Hidden assumptions | 0 | 2 | 1 |
| Design contract | 0 | 0 | 2 |

## Findings

### T1: "Fixable violation" scenario uses a rule ktlint can't autocorrect
**Severity:** major · **Status:** applied
**Location:** specs/code-style/spec.md > Requirement: One command formats the whole codebase > Scenario: Fixable violation
**Current text:** "**WHEN** a file uses a wildcard import of a single used class and a developer runs `./gradlew ktlintFormat` **THEN** the file is rewritten and a following `./gradlew ktlintCheck` reports no violation for it"
**Failure scenario:** ktlint's `no-wildcard-imports` rule has no autocorrect, because ktlint has no type resolution and can't know which names the wildcard brings in. A correct setup fails this scenario: `ktlintFormat` leaves the import and fails. The implementer either "fixes" a working setup or marks the scenario as passing without running it.
**Fix:** "**WHEN** a file has a line with trailing spaces and a statement indented with 3 spaces, and a developer runs `./gradlew ktlintFormat` **THEN** the file is rewritten without the trailing spaces and with 4-space indentation, and a following `./gradlew ktlintCheck` reports no violation for it"
Catches: a format task that is wired to the check-only task, or that runs with `ignoreFailures` and writes nothing.

### T2: "Unfixable violation" scenario names no violation
**Severity:** minor · **Status:** applied
**Location:** specs/code-style/spec.md > Requirement: One command formats the whole codebase > Scenario: Unfixable violation
**Current text:** "**WHEN** a file has a violation ktlint cannot fix automatically"
**Failure scenario:** The tester has to guess which rule can't be autocorrected (see T1, where the spec itself guessed wrong).
**Fix:** "**WHEN** a function without `@Composable` is named `LoadBoards` and a developer runs `./gradlew ktlintFormat` **THEN** the build fails, the function name is unchanged, and the console names the file, the line and the function-naming rule"
Catches: `ignoreFailures = true` on the format tasks, or a format task that silently passes.

### T3: Generated-code scenario passes even without the exclude filter
**Severity:** minor · **Status:** applied
**Location:** specs/code-style/spec.md > Requirement: Generated code and build output are not checked > Scenario: Violation in generated code
**Current text:** "**WHEN** a file under a module's `build/` directory has a violation and a developer runs `./gradlew ktlintCheck` **THEN** the build does not report it"
**Failure scenario:** ktlint-gradle only checks files of registered source sets. A random file under `build/` is never seen, so the test passes even when the `filter { exclude ... }` from design.md decision 1 is missing. The real risk is generated sources registered on a source set (BuildKonfig in `:core:data`, compose resource accessors).
**Fix:** "**WHEN** a module's generated Kotlin sources that are registered on a source set (the BuildKonfig output of `:core:data` or the compose resource accessors of `:core:designsystem`, under `build/generated/`) contain a violation, and a developer runs `./gradlew ktlintCheck` **THEN** no violation is reported for any file under that module's build directory"
Catches: a convention plugin without the exclude filter.

### T4: Format scope for build-logic and scripts has no scenario
**Severity:** minor · **Status:** applied
**Location:** specs/code-style/spec.md > Requirement: One command formats the whole codebase
**Current text:** "Running `./gradlew ktlintFormat` SHALL rewrite every file in the same scope as `ktlintCheck`"
**Failure scenario:** The root-to-build-logic wiring is separate for check and format (design.md decision 3, task 2.5). An implementation that wires only `ktlintCheck` to build-logic passes every scenario of this requirement.
**Fix:** Add scenario: "#### Scenario: Fixable violation in build-logic and a build script
- **WHEN** a build-logic convention plugin and a module's `build.gradle.kts` each have a line with trailing spaces and a developer runs `./gradlew ktlintFormat` from the repo root
- **THEN** both files are rewritten without the trailing spaces"
Catches: root `ktlintFormat` not depending on build-logic's format tasks.

### T5: Report location is vague
**Severity:** minor · **Status:** applied
**Location:** specs/code-style/spec.md > Requirement: Violations are written to report files > Scenario: Report files after a check
**Current text:** "**THEN** SARIF and Checkstyle report files exist under `build/reports` of the checked projects"
**Failure scenario:** "the checked projects" doesn't say which. Reports written only for the root project, or only for one source set, would pass a loose check.
**Fix:** "**WHEN** a developer runs `./gradlew ktlintCheck` and it completes **THEN** for each ktlint check task that ran, a `.sarif` and a Checkstyle `.xml` report exist under `<project>/build/reports/ktlint/`, including at least `:core:domain`, `:androidApp` and build-logic's `:convention`"
Catches: reporters configured only in the build-logic copy, or only on the root project.

### A1: A failing module stops the run, so "check the whole codebase" only holds with `--continue`
**Severity:** major · **Status:** applied
**Location:** specs/code-style/spec.md > Requirement: One command checks the whole codebase; design.md > 5. One-time reformat; design.md > 6. Docs
**Current text:** "It SHALL fail when there is at least one violation, print each violation with file, line and rule in the console" and "run `./gradlew ktlintFormat`, then fix the remaining violations by hand"
**Failure scenario:** With `ignoreFailures = false`, Gradle stops scheduling tasks after the first failed ktlint task. `ktlintCheck` prints the violations of one or a few source sets, not "each violation". Worse, during the reformat, `ktlintFormat` stops at the first module with an unfixable violation and leaves the remaining modules unformatted. The implementer then sees a misleadingly small diff in task 3.1 and runs format many times. Two implementers would handle this differently.
**Fix:** In the spec requirement, replace the sentence with: "It SHALL fail when there is at least one violation. When run with `--continue`, it SHALL check every project and print every violation with file, line and rule. Without `--continue` it MAY stop after the first failing task." In design.md decision 5, replace the first sentence with: "After the setup commit, run `./gradlew ktlintFormat --continue`, then fix the remaining violations by hand until `./gradlew ktlintCheck --continue` passes (D9)." In design.md decision 6 and task 2.7, add: "The Commands table notes that `--continue` lists the violations of all modules in one run." In task 3.1, use `./gradlew ktlintFormat --continue`.

### A2: The `/build/` exclude matches the absolute path
**Severity:** major · **Status:** applied
**Location:** design.md > 1. ktlint-gradle applied through a convention plugin
**Current text:** "`filter { exclude { it.file.path.contains("/build/") } }`"
**Failure scenario:** `it.file.path` is absolute. If a developer clones the repo below any directory named `build` (for example `~/build/CardiganApp`), every file is excluded and `ktlintCheck` passes with zero files checked. The failure is silent.
**Fix:** "`filter { exclude { it.file.toPath().startsWith(project.layout.buildDirectory.get().asFile.toPath()) } }`, so only files inside the project's own build directory are excluded, whatever the checkout path. build-logic uses the same expression."

### A3: Undefined what to do if the backend `.editorconfig` doesn't exist yet
**Severity:** minor · **Status:** applied
**Location:** tasks.md > 1.2; design.md > Open Questions
**Current text:** "Compare with the backend's `.editorconfig` (`cardigan.backend.dir`, read only) once it exists; the files must match apart from the Composable line. Verify by diffing the two files"
**Failure scenario:** If `be-ktlint` hasn't landed, one implementer blocks on it and another ships the key list from design.md and never compares. The verify step can't run.
**Fix:** "If the backend's `.editorconfig` doesn't exist yet, use the keys from design.md decision 4, tell the user that the comparison is still open, and leave this task unchecked until the diff has been done."

### D1: Both commits touch build-logic, but build-logic's tests aren't run
**Severity:** minor · **Status:** applied
**Location:** tasks.md > 2.8 and 3.3
**Current text:** "Run all four gates (`:androidApp:assembleDebug`, `testAndroidHostTest`, `iosSimulatorArm64Test`, `:composeApp:linkDebugFrameworkIosSimulatorArm64`)"
**Failure scenario:** `.claude/tdd/project.md` requires `./gradlew -p build-logic :convention:test` when a change touches `build-logic/`. The setup commit adds a plugin there and the reformat rewrites its sources (including the coverage/mutation exclusion code), which only those tests cover.
**Fix:** In 2.8 and 3.3, append: "and `./gradlew -p build-logic :convention:test`, since both commits touch build-logic".

### D2: Overlap with `add-detekt-konsist` on two files
**Severity:** minor · **Status:** applied
**Location:** design.md > 3. Root project and build-logic; tasks.md > 2.4, 2.7, Workflow follow-up
**Current text:** "A new minimal `build-logic/build.gradle.kts` applies it to build-logic's root project" and "the "Lint / format" tech stack row"
**Failure scenario:** `add-detekt-konsist` may also create `build-logic/build.gradle.kts` (its proposal: "`build-logic/build.gradle.kts` or its modules' build files") and also edits the single "Lint / format" row in CLAUDE.md. Whichever lands second "creates" a file that exists or overwrites the other tool's row text.
**Fix:** Add to "Workflow follow-up": "If `add-detekt-konsist` landed first, `build-logic/build.gradle.kts` and the CLAUDE.md "Lint / format" row may already exist: extend them, keeping the detekt content, instead of creating or replacing them."

## Trust boundaries
- Third-party build plugin and ktlint engine downloaded from the Gradle Plugin Portal and Maven Central: a compromised or typosquatted artifact runs in the build. Versions are pinned (D17); there's no dependency verification, but that's true of every existing dependency, so not a finding for this change.
- Report files (SARIF, Checkstyle) contain local file paths only and stay under `build/`, which isn't committed. No exposure.
- No network, auth, user input, imported files or runtime code paths are touched. Formatting changes no behavior, and the gates guard that.

No security findings.

## Coverage
Checked all 7 requirements and 12 scenarios in `specs/code-style/spec.md`, plus decisions.md, proposal.md, design.md and tasks.md. All five schema artifacts exist. Read for context: root and build-logic settings and build files, `KoverConventionPlugin.kt`, `gradle.properties` (configuration cache is on), `.claude/tdd/project.md`, and the `add-detekt-konsist` artifacts for overlap. Confirmed that build-logic is included as a regular build too, so `gradle.includedBuild("build-logic")` from design.md decision 3 works. Not verified: ktlint-gradle 14.2.0 internals for removing the `check` dependency (design.md already plans a dry-run check), and whether ktlint 1.8.0 parses Kotlin 2.4 (already a listed risk). No shared-spec requirements are involved.

Note: the review was requested as `mobile-ktlint`; no change with that name exists, so `add-ktlint` was reviewed.

## Applied changes
All 10 findings applied as recommended, with no edits to the fix text.

- `specs/code-style/spec.md`: T1 (fixable scenario now trailing spaces + 3-space indent), T2 (`LoadBoards` unfixable scenario), T3 (generated sources registered on a source set), T4 (new build-logic/build-script format scenario), T5 (report path and projects), A1 (`--continue` wording in the check requirement).
- `design.md`: A2 (exclude by build directory, not path substring), A1 (`--continue` in decisions 5 and 6), A3 (Open Questions fallback).
- `tasks.md`: A1 (2.6, 2.7, 3.1, 3.2), A2 (2.1), A3 (1.2), D1 (2.8, 3.3), D2 (Workflow follow-up). No checkbox changed.
- `proposal.md`: A1 (`--continue` note in Commands).
- `decisions.md`: not edited. No fix contradicts it (D3 is kept: `--continue` is a Gradle flag, not an alias task).
- No non-standard artifacts in the change folder.
- `openspec validate add-ktlint --strict`: valid.
- Accepted risks: none.
