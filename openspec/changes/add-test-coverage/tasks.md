# Tasks

## 1. Kover in build-logic

- [x] 1.1 Confirm the latest stable Kover version that works with Kotlin 2.4.20, AGP 9.1.1 and configuration cache, from its release notes. Confirm that diff-cover 11.0.4 exists on PyPI and reads Kover's XML. Pin 11.0.4, the same version as the other repos; if it doesn't work, report back instead of picking another version. Add `kover`, `diffCover` and `diffCoverExcludeNewer` (an ISO date on or after the 11.0.4 release) versions, the Kover plugin and library entries, and the `convention-kover` and `convention-coverage` plugin aliases to `libs.versions.toml`. Add Kover as `compileOnly` in `build-logic/convention/build.gradle.kts`, and load it with `apply false` in the root `build.gradle.kts`. Verify: `./gradlew help` succeeds.
- [x] 1.2 Add `KoverConventionPlugin` (`convention.kover`) with the filters from design.md decision 3, register it, and apply it to `:core:domain` only. Verify: `./gradlew :core:domain:koverXmlReport` runs the host tests and writes a report with covered lines for `core.domain` classes. Note the Kover variant name the KMP Android target produces. If Kover doesn't work with AGP 9.1.1, stop and report.

## 2. Opt in every KMP module

- [x] 2.1 Apply `alias(libs.plugins.convention.kover)` to all 20 KMP modules (core, feature, `:composeApp`), not `:androidApp`. Verify: each module's `koverXmlReport` succeeds.
- [x] 2.2 Check the filters on real reports. A presentation module (`:feature:boards:presentation`) lists ViewModels and no composables, previews or `ComposableSingletons`. `:core:designsystem` lists no `generated.resources` classes. `:core:data` lists no `di` or `BuildKonfig` classes and no `*_androidKt` classes. `:feature:auth:data` lists no `fixture` classes. `:feature:transfer:presentation` lists no `AndroidFileSharer` or `SaveExportActivity`. `:composeApp` lists no class matching `*Debug*Overlay*`. Adjust patterns where a check fails, and report any gap that would need hard-coded class names.
- [x] 2.3 Turn instrumentation off unless a coverage task is requested (design.md decision 4a). Verify:
  - `./gradlew testAndroidHostTest --rerun-tasks` takes about as long as on `master`. Compare the median of 3 runs each, with the build cache off.
  - no `build/kover/` binary reports are written by a gate run, and the host `Test` tasks' JVM arguments contain no Kover `-javaagent`
  - running `coverageReport` and then `testAndroidHostTest` doesn't reuse instrumented results for the gate run
  - running `testAndroidHostTest` and then `coverageReport -Pcoverage.modules=:core:domain` reruns the tests with instrumentation and shows covered lines for `normalizePassword`
  - `./gradlew :core:domain:check` runs no Kover task and succeeds
  - all four gates pass
  If any of these can't be met, stop and report it as a finding instead of accepting the slowdown.

## 3. Root coverage tasks

- [x] 3.1 Add `CoverageConventionPlugin` (`convention.coverage`) and apply it in the root build. It collects opted-in modules, reads and checks `coverage.modules`, and adds `kover(project(…))` for the selection. Verify:
  - `./gradlew coverageReport` reports all 20 modules
  - `-Pcoverage.modules=:feature:boards:data` reports only that module
  - `-Pcoverage.modules=:core:data,:feature:boards:data` reports exactly those two
  - `:feature:nope` and `:androidApp` each fail before any test runs, with the list of valid modules
  - `-Pcoverage.modules=` reports all modules, and `" :core:data, :core:data,"` reports only `:core:data`
- [x] 3.2 Register `coverageReport`: the merged XML + HTML, the terminal summary (line %, branch %, module count, HTML path), and the optional `coverage.minLine` / `coverage.minBranch` with validation, checked in the summary step against the printed counters (no Kover verify rule). Verify:
  - the summary is printed and both reports exist
  - `-Pcoverage.minLine=100` fails after writing the reports and shows the actual value and the threshold
  - `-Pcoverage.minLine=0` succeeds, and a decimal such as `-Pcoverage.minLine=0.5` is accepted
  - a deliberately failing host test leaves the existing reports untouched, also with `--continue`
  - `-Pcoverage.minBranch=abc` and `-Pcoverage.minLine=120` fail before tests run
  - a run without thresholds always succeeds
  - the same command run twice reuses the configuration cache
  - `./gradlew cR` and `./gradlew kXR` fail with "Coverage needs instrumentation. Run with the full task name, e.g. ./gradlew coverageReport." before any task runs (no test task in the output), and fail the same way when repeated with configuration cache on. Check `./gradlew dC` the same way after 3.3
- [x] 3.3 Register `diffCoverage`: depends on the root XML report, checks for `uv` and the base ref, and runs `uvx --exclude-newer <catalog date> diff-cover==<catalog version>` with the selected modules' existing `commonMain`/`androidMain` source roots, `--compare-branch` from `coverage.compareBranch` (default `origin/master`), `--include-untracked`, and the HTML report at `build/reports/diff-coverage/index.html`, without `--fail-under`. Verify:
  - on a throwaway local change to a tested and an untested function in `:core:domain`, the terminal shows the file with uncovered line numbers, and the HTML exists
  - a new untracked Kotlin file in `:core:domain` shows up in the report
  - `-Pcoverage.compareBranch=HEAD~1` changes the reported lines
  - `-Pcoverage.compareBranch=does-not-exist` fails and names the ref
  - with `uv` removed from `PATH`, the task fails with the install link
  - 0% diff coverage still exits 0
  - revert the throwaway change afterwards

## 4. Docs and finish

- [x] 4.1 Update `CLAUDE.md` (the `AGENTS.md` it points to):
  - Tech Stack: a Coverage row for Kover (host tests only) and diff-cover via `uvx`
  - Commands table: `coverageReport` and `diffCoverage` with their properties, marked "Not a gate"
  - Testing: one short paragraph on the exclusions and the androidMain naming rules (`*.android.kt` facades, `Android*` classes)
  - API contract: generated DTOs go under a package with a `generated` segment
  Verify: every documented command runs as written.
- [x] 4.2 Run all four gates: `:androidApp:assembleDebug`, `testAndroidHostTest`, `iosSimulatorArm64Test` and `:composeApp:linkDebugFrameworkIosSimulatorArm64`. Run `openspec validate add-test-coverage --strict`. Verify: all pass.

## Workflow follow-up

- Commit only after the user's OK is relayed (orchestrated change; overrides CLAUDE.md's automatic commits for this change only): `build: add kover coverage convention plugins` and `docs: document coverage commands`, each once the gates pass.
- Archive the change after review.
