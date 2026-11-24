# Tasks

## 1. PIT in build-logic

- [x] 1.1 Confirm the latest stable PIT version that runs on the project's JDK and Kotlin 2.4.20 bytecode, from its release notes. Add the `pitest` version, the `pitest-command-line` and `pitest-aggregator` libraries, and the `convention-mutation` and `convention-mutation-root` plugin aliases to `libs.versions.toml`. Verify: `./gradlew help` succeeds and downloads no PIT artifact.
- [x] 1.2 Move the coverage exclusion patterns to a shared list in build-logic that both Kover and PIT read. Verify: `./gradlew coverageReport -Pcoverage.modules=:core:data` lists the same classes as before.
- [x] 1.3 Add `MutationConventionPlugin` (`convention.mutation`) with the per-module `pitestAndroidHost` task from design.md decision 1. Register it and apply it to `:core:domain` only. Verify: `./gradlew :core:domain:pitestAndroidHost` writes an HTML report under `core/domain/build/reports/pitest/` with killed mutants in `normalizePassword`, and PIT runs the same host test classes as `testAndroidHostTest` (no `*UiTest`).

## 2. Opt in every KMP module

- [x] 2.1 Apply `alias(libs.plugins.convention.mutation)` to all 20 KMP modules, not `:androidApp`. Verify: each module's `pitestAndroidHost` succeeds, `:feature:labels:domain` is skipped with a note, and running two modules at once never overlaps their PIT runs.
- [x] 2.2 Check exclusions and Kotlin filters on real reports: no mutants in BuildKonfig, `Res`, `di`, `fixture`, `*Debug*Overlay*`, previews, `ComposableSingletons`, `Android*` or `*_androidKt` classes, data class `equals`/`hashCode`/`toString`/`copy`/`component*`, `$WhenMappings` or `Intrinsics` calls. Composables in `:feature:boards:presentation` show as not covered. Record the remaining noise categories in the change's review notes.
- [x] 2.3 Check that a deliberately failing unmutated test fails the module task and names the test, then revert it. Check that `:feature:boards:presentation` and `:core:designsystem` show no false timeouts.

## 3. Root task

- [x] 3.1 Add `convention.mutation.root` and apply it in the root build, with `mutation.modules` validation. Verify: no property runs all modules; `:core:domain,:feature:boards:data` runs exactly those; `" :core:domain , :core:domain"` runs only `:core:domain`; `:feature:nope` and `:androidApp` fail before any test runs and list the valid modules; `-Pmutation.modules=`, `:core:domain,` and `:core:domain,,:core:data` fail before any test runs and name the property.
- [x] 3.2 Register `mutationReport` with aggregation and the terminal summary. Verify: `build/reports/pitest/index.html` exists and shows all selected modules; the summary prints mutation score, test strength, counts and the path; a red suite fails and leaves no combined report, even when an earlier run wrote one; `-Pmutation.modules=:feature:labels:domain` succeeds, prints that there were no mutants and writes no combined report; the same command run twice reuses the configuration cache and executes PIT both times (never UP-TO-DATE or FROM-CACHE).
- [x] 3.3 Add `mutation.threads` and `mutation.minScore`. Verify: `-Pmutation.threads=2` runs PIT with 2 threads; `0`, `-1` and `abc` fail before tests run; a run without `minScore` succeeds at any score; `-Pmutation.minScore=100` writes the report and then fails with score and threshold; `-Pmutation.minScore=0` and `0.5` succeed; a threshold equal to the score succeeds; `minScore` with no mutants succeeds; `120` and `abc` fail before tests run; `./gradlew :core:domain:pitestAndroidHost -Pmutation.threads=abc` fails before tests run; an abbreviated task name such as `mR` fails with a message to use the full name.
- [x] 3.4 Verify no history is used (D12, incremental dropped): two runs of `./gradlew mutationReport -Pmutation.modules=:core:domain` both execute PIT and analyze every mutant, and no `history.bin` is written under `core/domain/build/`.
- [x] 3.5 Verify the workflow is untouched: `./gradlew testAndroidHostTest` and `./gradlew :core:domain:check` run no PIT task and download no PIT artifact; all four gates pass.
- [x] 3.6 Count `KILLED`, `TIMED_OUT`, `MEMORY_ERROR`, `RUN_ERROR` and `NON_VIABLE` as killed in the summary and for `mutation.minScore` (D14). Verify: unit tests in `build-logic` for the counting pass, and a `mutationReport` run prints the same totals as the HTML report.

## 4. Docs

- [x] 4.1 Update `CLAUDE.md`: a Mutation testing row in Tech Stack (PIT, host tests only), and `mutationReport` with `mutation.modules`, `mutation.threads` and `mutation.minScore` in the Commands table, marked "Not a gate". Verify: every documented command runs as written. Run `openspec validate mobile-pitest-mutation-testing --strict`.

## Workflow follow-up

- Commits follow CLAUDE.md (commit once the gates pass), as relayed by the orchestrator with the user's approval to implement.
- Archive the change after review.
