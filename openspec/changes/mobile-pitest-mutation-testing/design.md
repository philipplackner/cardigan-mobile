# Design

## Context

See proposal.md for the motivation and decisions.md for the settled choices (D1–D17).

What shapes the approach:
- Every KMP module goes through `convention.kmp.library`. It sets up the AGP 9 `com.android.kotlin.multiplatform.library` target with a host test (`testAndroidHostTest`, a Gradle `Test` task), a device test and two iOS targets. There is no `java` plugin and no `com.android.library` variant API. The existing PIT Gradle plugins hook into those.
- Host tests run on JUnit 4, which PIT supports without a plugin.
- The host `Test` tasks exclude `*UiTest`.
- Configuration cache is on. The coverage setup (`convention.kover`, `convention.coverage`) already has a root task with a module property, threshold parsing and a terminal summary task to copy from.

## Goals / Non-Goals

**Goals:**
- One manual root task, one HTML report.
- No effect on any other task or on the gates.
- The same shape as `coverageReport`, and the same task and property names as the backend (D17).

**Non-Goals:**
- Making the score a gate, or tracking it across runs.
- Mutation testing on iOS or device tests.
- Filtering every Kotlin compiler artifact (accepted noise, D8).

## Decisions

### 1. Run PIT's CLI from our own convention plugin (D1)

`convention.mutation` registers `pitestAndroidHost`, a `JavaExec` task per module that runs `org.pitest.mutationtest.commandline.MutationCoverageReport`. Inputs come from the module's `testAndroidHostTest` task: its `classpath` (main classes, test classes, dependencies, the Android stub jar) and `testClassesDirs`. The task's `jvmArgs` and `systemProperties` are passed through `--jvmArgs`. PIT's own classpath comes from a detached configuration with the catalog's `pitest-command-line`, resolved only at execution, so nothing is downloaded unless the task runs.

Arguments:
- `--targetClasses com.plcoding.cardiganapp.*`
- `--targetTests com.plcoding.cardiganapp.*`, `--excludedTestClasses *UiTest`
- `--sourceDirs` the module's existing `commonMain/kotlin` and `androidMain/kotlin`
- `--mutators DEFAULTS` (D9)
- `--excludedClasses` the coverage name and package patterns translated to PIT globs (D10), plus `*$WhenMappings`. The patterns move to a shared list in build-logic, so coverage and mutation can't drift apart.
- `--excludedMethods equals,hashCode,toString,copy,copy$default,component*` and `--avoidCallsTo kotlin.jvm.internal,kotlin.ResultKt` (D8). PIT can't tell data classes apart, so these names are excluded on all classes. Hand-written `equals`, `hashCode`, `toString` and `copy` are not mutated (accepted, D8).
- `--threads` from `mutation.threads`, default `Runtime.availableProcessors()` (D13). The module PIT tasks share one Gradle `BuildService` with `maxParallelUsages = 1`, so only one module runs PIT at a time even though configuration cache runs tasks of different projects in parallel. Each run uses `mutation.threads` threads.
- `--failWhenNoMutations=false`, so a module whose classes are all excluded is treated like a module without host tests
- `--outputFormats HTML,XML`, `--exportLineCoverage`, `--timestampedReports=false`, report dir `<module>/build/reports/pitest/`
- no `--historyInputLocation` / `--historyOutputLocation`: open-source PIT 1.25.8+ has no history store (D12, dropped)

`pitestAndroidHost` and `mutationReport` are never up to date and never cached (`outputs.upToDateWhen { false }`, `doNotCacheIf`). Results are never reused.

The task depends on the host test classes being compiled, not on the test task itself. Modules with no host test classes are skipped with a log line. A failing unmutated suite makes PIT exit non-zero, which fails the task.

Alternatives: `info.solidsoft.pitest` and `pl.droidsonroids.pitest`, see D1.

### 2. Root task, aggregation and summary (D4–D7, D14)

The root part goes into a new root plugin `convention.mutation.root`, applied in the root `build.gradle.kts` next to `convention.coverage`. Keeping it separate means changes to one tool don't touch the other.

- It collects opted-in modules and validates `mutation.modules`: property left out = all modules; empty value or empty list entry fails; entries trimmed and deduplicated; unknown modules fail with the valid list. This is stricter than the mobile `coverage.modules`, which treats an empty value as all modules (D4). The mutation parser is separate, so coverage behavior doesn't change in this change.
- It parses `mutation.threads` (positive integer) and `mutation.minScore` (0–100) at configuration time, when `mutationReport` or any `pitestAndroidHost` task is requested by full name, so invalid values fail before any test runs. Module tasks read the same parsed values. `mutation.modules` applies only to `mutationReport`. An abbreviated task name that resolves to a mutation task fails with a message to use the full name, as the coverage tasks do.
- `mutationReport` deletes any existing `build/reports/pitest/` before the module tasks run, so a red run leaves no stale combined report. It depends on the selected modules' `pitestAndroidHost`, then runs `ReportAggregator` from `pitest-aggregator` over their `mutations.xml`, `linecoverage.xml`, source and class dirs. It writes `build/reports/pitest/index.html`, then prints the summary by summing the per-module `mutations.xml` files of the selected modules that ran in this invocation.
- When no selected module produced mutants, it skips the aggregator, writes no combined report, prints that there were no mutants, succeeds and ignores `mutation.minScore`.
- Mutation score = killed / all mutants. Test strength = killed / mutants with coverage. Killed means PIT's detected statuses: `KILLED`, `TIMED_OUT`, `MEMORY_ERROR`, `RUN_ERROR` and `NON_VIABLE`, so the terminal numbers match the HTML report. The backend uses the same list.
- When `mutation.minScore` is set and the score is below it, the task fails after the report is written and prints both values. The score is compared unrounded, and a score equal to the threshold passes.

Module PIT tasks run only when `mutationReport` or a module task is requested by name, so `check`, `build` and the gates never reach them.

### 3. Composables (D11)

Kept in. Uncovered mutants are cheap and show as "no coverage". The summary prints test strength next to the score.

## Risks / Trade-offs

- [Kotlin noise lowers scores] → D8 filters cover the most common cases. Remaining noise is accepted for a baseline.
- [Host tests that depend on the working directory or Compose resources behave differently under PIT] → each module runs with its own host test classpath, JVM args and working dir. Checked per module in task 2.3.
- [Coroutine tests time out under mutation] → PIT's default timeouts. Raise them only if task 2.3 shows false timeouts.
- [Long runs on presentation modules] → `mutation.modules`, `mutation.threads`.
- [AGP adds test-task setup PIT doesn't see] → task 1.3 compares PIT's test discovery with the host test report.
