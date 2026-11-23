# Design

## Context

See proposal.md for the motivation and decisions.md for the settled choices (D1–D20).

What shapes the approach:
- Every KMP module goes through `convention.kmp.library`. It sets up the AGP 9 `com.android.kotlin.multiplatform.library` target with a host test, a device test, and the two iOS targets.
- The host `Test` tasks filter out `*UiTest`.
- Build-logic adds Gradle plugins as `compileOnly`. The root build loads each plugin once with `apply false`.
- Configuration cache is on. Isolated projects is off.
- Kover supports the KMP Android library plugin since 0.9.2. It needed fixes for host test lookup in 0.9.3, 0.9.7 and 0.9.8. Kover can't measure Kotlin/Native.

## Goals / Non-Goals

**Goals:**
- One plugin id per module to opt in. One root task for any combination of modules.
- The same task and property names as the backend (D14).
- No new tool to install for `coverageReport`. Only `uv` for `coverageDiff`.

**Non-Goals:**
- Making `*UiTest` run on the host, or measuring iOS or device tests.
- Per-module thresholds, per-package thresholds, or thresholds on diff coverage.
- Splitting the report by module beyond what Kover's HTML already shows.

## Decisions

### 1. Two convention plugins

- **`com.plcoding.cardiganapp.convention.kover`** (module). It applies `org.jetbrains.kotlinx.kover` and sets the report filters (decision 3). It doesn't add any tasks of its own. Each KMP module's `build.gradle.kts` adds `alias(libs.plugins.convention.kover)`.
- **`com.plcoding.cardiganapp.convention.coverage`** (root). It applies Kover to the root project, adds each selected module as a `kover(project(path))` dependency, sets the same filters on the merged report, and registers `coverageReport` and `coverageDiff`.

Alternative: let `convention.kmp.library` apply Kover by itself. Rejected, because D1 asks for opt-in with one plugin id. An explicit line per module also makes it obvious which modules are measured.

### 2. Selecting modules

The root plugin reads `coverage.modules` through `providers.gradleProperty`:
- It collects the opted-in modules with `subprojects { pluginManager.withPlugin("…convention.kover") { … } }`.
- After evaluation, it checks every requested path against that set. If a path is unknown or not opted in, it throws a `GradleException` that lists the valid paths. This runs at configuration time, so it happens before any test runs.
- It then adds `kover(project(path))` only for the selected modules. When the property is empty, it adds every opted-in module.

The root project itself has no sources, so the merged report contains only the selected modules.

Cross-project configuration like this is allowed with configuration cache. It isn't compatible with isolated projects, which this repo doesn't use. A different `coverage.modules` value is a different configuration-cache entry. That's expected.

Alternative: Kover's per-module `koverXmlReport` tasks, merged by hand. Rejected in D6.

### 3. Filters (D3, D4)

The same filter block goes on the module report and on the root merged report:

| What | Kover filter |
|---|---|
| Composables, previews included | `annotatedBy("androidx.compose.runtime.Composable")`, plus classes `*ComposableSingletons*` |
| Generated code | classes `*.BuildKonfig`, packages `*.generated.*` (Compose resources generate into `…generated.resources`) |
| Koin modules | packages `*.di` and `*.di.*` |
| Debug fixtures | packages `*.fixture` and `*.fixture.*`, classes `*Debug*Overlay*` |
| androidMain `actual`s | classes `*_androidKt` and `*_androidKt$*` (file facades of `*.android.kt`), classes `*.Android*`, `inheritedFrom("android.app.Activity")` |

Kover filters by class, not by source set, so androidMain needs these naming rules:
- Top-level `actual` functions live in `*.android.kt` files, which every androidMain file except the activity already does.
- Classes declared in androidMain start with `Android`. Today that's only `AndroidFileSharer`.
- Activities are matched by superclass.

These rules go into the docs (task 4.1). A class that breaks them shows up in the report. That's visible, not silent.

Future OpenAPI DTOs: the generator change has to put them under a package that contains a `generated` segment, so `*.generated.*` covers them. Task 4.1 records this as a note in `CLAUDE.md`'s API contract section.

### 4. Which tests feed Kover (D2)

Kover's variant for the KMP Android target collects coverage from the host `Test` task. The existing `*UiTest` filter stays, so UI tests don't run under Kover. iOS and device test tasks aren't JVM `Test` tasks, so Kover never sees them.

Task 1.2 checks which Kover variant name the AGP 9 KMP target produces. The root report uses the total variant if it includes host tests, and the named Android variant otherwise.

### 4a. Instrumentation only for coverage runs (D17)

The module plugin applies Kover only when a coverage task was requested (D22):
- At configuration time it reads `gradle.startParameter.taskNames`. That's part of the configuration-cache key, so this is safe with configuration cache.
- If none of the names resolves to `coverageReport`, `coverageDiff` or a `kover*` task, the module plugin doesn't apply Kover at all. Turning instrumentation off with `disabledForAll` isn't enough: Kover still wires `koverFindJar` into every host `Test` task.
- Names are matched on the last path segment (`:coverageReport`, `coverageReport`). Gradle's abbreviated task names (`cR`) aren't matched, so Kover isn't applied to the modules. The root plugin always applies Kover, so `kXR` still resolves (to the root task), and the guard below makes such a run fail before any task runs (D20).
- When Kover is applied, the module plugin sets `verify.onCheck = false`, so `check` never runs `koverVerify`. Task 2.3 verifies `./gradlew :core:domain:check` succeeds.

Test task outputs must not mix instrumented and plain runs. Instrumentation changes the test task's JVM arguments and adds a binary report output, so Gradle's up-to-date and build-cache checks treat the two runs as different. Task 2.3 confirms this in both directions: a gate run after a coverage run, and a coverage run after a gate run (which must rerun the tests with instrumentation, not report 0%).

Alternative: check `gradle.taskGraph.whenReady`. Rejected, because Kover's instrumentation settings are read before the task graph is ready and that callback doesn't fit well with configuration cache.

Turning instrumentation off with `disabledForAll` was tried first. It kept the Kover agent out of the test JVM, but left `koverFindJar` in every test run, so it was replaced by not applying Kover at all (D22).

Guard against an uninstrumented coverage run (D20), the same as in the backend repo:
- The root plugin computes the same "coverage requested" flag from `startParameter.taskNames`.
- In `gradle.taskGraph.whenReady`, if the flag is false and the graph contains `coverageReport`, `coverageDiff` or any Kover report task (root or module, matched by type or by a `kover` name prefix), it throws a `GradleException` with "Coverage needs instrumentation. Run with the full task name, e.g. ./gradlew coverageReport."
- This fails before any task runs, so no tests run.
- With configuration cache: `whenReady` runs during configuration, and a configuration that throws isn't stored. So a repeated `cR` configures again and fails again, and never reuses a cache entry that skips the check.
- Using `whenReady` here doesn't conflict with decision 4a. The instrumentation switch is still set from task names at configuration time; `whenReady` only checks the result.

### 5. `coverageReport`

`coverageReport` is a root task that depends on:
- `koverXmlReport` and `koverHtmlReport` of the root (merged)

It runs a small summary step that reads the root counters (`LINE`, `BRANCH`) from the merged XML and prints:

```
Coverage (N modules): line 72.4%, branch 58.1%
HTML: build/reports/kover/html/index.html
```

This is a custom step, not `koverLog`, because `koverLog` prints one unit per configuration and the summary needs both.

Thresholds:
- `coverage.minLine` and `coverage.minBranch` are parsed as decimals from 0 to 100 at configuration time. An invalid value fails before any test runs.
- The summary step compares the thresholds with the same LINE and BRANCH counters it prints (covered / (covered + missed) × 100, not rounded), after the reports are written, and fails with `Line coverage 72.4% is below coverage.minLine=80`. It fails only when the value is strictly below the threshold.
- No Kover verify rule is used, so `koverVerify` isn't wired in.

Output paths are Kover's root defaults: `build/reports/kover/report.xml` and `build/reports/kover/html/`.

### 6. `coverageDiff` (D9, D11, D12)

`coverageDiff` is a root `Exec` task that depends on the root `koverXmlReport`. It runs:

```
uvx --exclude-newer <diffCoverExcludeNewer> diff-cover==<pinned> build/reports/kover/report.xml \
  --compare-branch=<coverage.compareBranch or origin/master> \
  --src-roots <each selected module>/src/commonMain/kotlin <each>/src/androidMain/kotlin \
  --include-untracked \
  --format html:build/reports/diff-coverage/index.html
```

- No `--fail-under`, so the exit code doesn't depend on coverage.
- diff-cover counts committed, staged and unstaged changes by default (D11). `--include-untracked` adds new files that aren't in git yet (D18).
- The source roots are computed from the selected modules. Only roots that exist on disk are passed.
- diff-cover reads JaCoCo-format XML, which Kover writes.
- Kover's XML names files by package plus file name, and KMP source directories follow the package path, so files map back correctly.
- Before running diff-cover, the task checks two things and fails with a clear message:
  - `uv` is missing: the message links https://docs.astral.sh/uv/getting-started/installation/
  - the base ref doesn't resolve (`git rev-parse --verify <ref>`): the message names the ref and suggests `git fetch origin`
- The task doesn't fetch from the remote. A stale `origin/master` is the developer's call.
- The diff-cover version lives in `libs.versions.toml` (`diffCover = "…"`), so one place pins it. The catalog only stores the string. It isn't a Gradle dependency.
- `--exclude-newer` fixes the whole Python dependency set, not just diff-cover. The date is stored next to it as `diffCoverExcludeNewer` and is only updated together with the diff-cover version. Whether the backend and web repos adopt the same flag is for them to decide (D14, D16).

Alternative: a repo shell script. Rejected in D12.

### 7. Versions

Kover and diff-cover are pinned to the latest stable release that works with Kotlin 2.4.20, AGP 9.1.1 and configuration cache. diff-cover is pinned to 11.0.4, the same version the web repo uses (D16). Kover is believed to be 0.9.9 at the time of writing. Task 1.1 confirms both against their release notes.

### 8. Verification

Convention plugins are verified by hand, by running the commands in tasks.md (D19). No Gradle TestKit tests.

## Risks / Trade-offs

- [Kover's KMP Android support is recent and has been patched several times] → Task 1.2 spikes it on `:core:domain` first: check that host tests produce data and the variant name is correct, before rolling out to every module. If it doesn't work with AGP 9.1.1, stop and report back instead of switching tools.
- [Lambdas inside `@Composable` functions compile to separate synthetic methods that aren't annotated, so they may still be counted] → Task 2.2 checks a presentation module's report. If composable lambdas show up, add the matching class/method patterns. If they can't be matched cleanly, report the gap instead of hand-listing classes.
- [The androidMain naming rules can be broken by new code] → The rules are documented, and a broken rule only makes code visible in the report.
- [Two modules with the same package and file name would collide in diff-cover's source mapping] → Packages follow the module path, so collisions aren't expected. Noted in the docs.
- [Instrumentation could still reach a gate run, for example through an abbreviated task name, a cached instrumented test result, or a Kover change] → Decision 4a, checked by task 2.3. A remaining slowdown is reported as a finding, not accepted.
- [`coverageReport` runs every selected module's host tests, which takes as long as the test gate] → Accepted. It's an on-demand command.

## Migration Plan

Additive only. Rollback is reverting the change's commits. Nothing is persisted outside `build/`.
