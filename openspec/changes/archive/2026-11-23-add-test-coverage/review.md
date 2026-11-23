# Artifact review (light): add-test-coverage

**Verdict:** Ready
**Reviewed:** 2026-11-23, schema `careful-planning`, repo kind `code`

| Pass | Blocker | Major | Minor |
|---|---|---|---|
| Testability | 0 | 2 | 3 |
| Security | 0 | 1 | 0 |
| Hidden assumptions | 0 | 2 | 0 |
| Design contract | 0 | 1 | 3 |

## Findings

### T1: "No added runtime" can't be tested
**Severity:** major · **Status:** applied
**Location:** specs/test-coverage/spec.md > Requirement: Normal test runs are not instrumented
**Current text:** "SHALL run tests without coverage instrumentation, with the same results and no added runtime."
**Failure scenario:** Timing noise means no test can show "no added runtime". Task 2.3's "about as long, median of 3" passes with the Kover agent still attached if the overhead is small on today's small suite, and the overhead then grows with the suite.
**Fix:** "Any other run, including the `testAndroidHostTest` gate, SHALL run tests with no Kover agent in the test JVM's arguments and SHALL write no Kover binary report under any module's `build/kover/`." Add to the Gate run scenario: "**AND** the host `Test` tasks' JVM arguments contain no `-javaagent` for Kover". Catches: an implementation that leaves instrumentation on but discards the output.

### T2: Reuse checked in the wrong direction
**Severity:** major · **Status:** applied
**Location:** specs/test-coverage/spec.md > Requirement: Normal test runs are not instrumented > Scenario: Gate run after a coverage run
**Current text:** "**WHEN** a developer runs `./gradlew coverageReport` and then `./gradlew testAndroidHostTest` **THEN** the second run doesn't reuse instrumented test results as if they were plain ones"
**Failure scenario:** The harmful case is the reverse. After a plain gate run, `coverageReport` treats the host `Test` tasks as UP-TO-DATE or FROM-CACHE, no binary report exists, and the report shows 0% or no classes. Reusing an instrumented result for a gate run only skips tests whose results are identical. Task 2.3 also checks only the harmless direction.
**Fix:** Add a scenario: "#### Scenario: Coverage run after a gate run — **WHEN** a developer runs `./gradlew testAndroidHostTest` and then `./gradlew coverageReport -Pcoverage.modules=:core:domain` **THEN** the host tests of `:core:domain` run again with instrumentation, and the report shows covered lines for `normalizePassword`". Add the same check to task 2.3. Catches: Kover registering its binary output only when instrumentation is on, so the plain result looks up to date.

### T3: "Every module" can't be read from a merged report
**Severity:** minor · **Status:** applied
**Location:** specs/test-coverage/spec.md > Requirement: Modules opt in with one plugin id > Scenario: Opted-in module is reported
**Current text:** "the report contains every module that applies the plugin"
**Failure scenario:** Kover's merged XML and HTML group by package, not module, and a module whose classes are all excluded (composables, `di`, generated) has no entries. A tester can't check "every module", and an implementation that forgets one module with only excluded code passes.
**Fix:** "**THEN** the terminal summary shows 20 modules, and the report contains classes from the packages of `:core:data`, `:feature:boards:presentation` and `:composeApp`". Catches: an implementation that collects modules before all subprojects are evaluated and misses some.

### T4: "No report is written" is ambiguous with a stale report
**Severity:** minor · **Status:** applied
**Location:** specs/test-coverage/spec.md > Requirement: Coverage comes from Android host tests only > Scenario: Failing host test
**Current text:** "**THEN** the command fails with the test failure and no report is written"
**Failure scenario:** The reports live at fixed paths. After a failed run the previous report is still there, so a developer reads stale numbers. With `--continue`, the merged report could be written from the modules that passed.
**Fix:** "**THEN** the command fails with the test failure, and the XML and HTML reports at the fixed paths are not created or updated in this run, also when the command is run with `--continue`". Catches: wiring the report tasks with `mustRunAfter` instead of `dependsOn` on the test tasks.

### T5: Property parsing edge cases are open
**Severity:** minor · **Status:** applied
**Location:** specs/test-coverage/spec.md > Requirement: Report for a chosen set of modules; Requirement: Optional coverage thresholds
**Current text:** "a comma-separated list of Gradle project paths" / "each a percentage from 0 to 100"
**Failure scenario:** `-Pcoverage.modules=` (empty), `:core:data, :core:domain` (space), a trailing comma, `-Pcoverage.minLine=-5` and `-Pcoverage.minLine=72.5` aren't specified. Design says decimals are allowed; the spec doesn't.
**Fix:** Append to the modules requirement: "Entries are trimmed, empty entries and duplicates are ignored, and an empty value means every opted-in module." Append to the thresholds requirement: "Values are decimal numbers from 0 to 100 inclusive. The command fails only when the total is strictly below the value." Catches: `" :core:domain"` reported as an unknown module.

### S1: Transitive Python dependencies of diff-cover aren't pinned
**Severity:** major · **Status:** applied
**Location:** design.md > 6. `diffCoverage`; specs/test-coverage/spec.md > Requirement: Diff coverage tool is fetched on demand > Scenario: Same version for everyone
**Current text:** "`uvx diff-cover==<pinned> …`" / "both runs use the same diff-cover version"
**Failure scenario:** `uvx` resolves diff-cover's dependencies (Jinja2, Pygments, pluggy, chardet) to their latest versions on each fresh cache. A compromised or broken new release of one of them runs on every developer machine with access to the repo. Two developers on the same commit can also get different dependency sets, which the scenario doesn't catch.
**Fix:** In design.md 6: "Run `uvx --exclude-newer <date> diff-cover==<pinned>`, with the date stored in `libs.versions.toml` as `diffCoverExcludeNewer`, so the whole dependency set is fixed. Update the date only together with the diff-cover version." In the scenario: "**THEN** both runs use the same diff-cover version and resolve the same versions of its dependencies". Check whether the backend and web repos want the same flag (D14, D16).

### A1: Two sources of truth for thresholds
**Severity:** major · **Status:** applied
**Location:** design.md > 5. `coverageReport` > Thresholds
**Current text:** "parsed as decimals from 0 to 100" … "a Kover verify rule is added on the total: `bound { coverageUnits = LINE / BRANCH; minValue = … }`"
**Failure scenario:** The summary prints a percentage computed from the XML, while `koverVerify` computes and rounds its own value, and Kover's `minValue` may not take decimals. The terminal shows "line 80.0%" and `-Pcoverage.minLine=80` still fails, or `72.5` can't be passed to Kover at all. Two implementers would pick different rounding.
**Fix:** Replace the Thresholds bullets with: "The summary step compares the thresholds with the same LINE and BRANCH counters it prints (covered / (covered + missed) × 100, not rounded), after the reports are written, and fails with `Line coverage 72.4% is below coverage.minLine=80`. No Kover verify rule is used." Drop `koverVerify` from the task list in 3.2.

### A2: Kover's `koverVerify` may hang off `check`, so the guard breaks `check` and `build`
**Severity:** major · **Status:** applied
**Location:** design.md > 4a (guard); specs/test-coverage/spec.md > Requirement: Coverage tasks refuse to run uninstrumented
**Current text:** "if the flag is false and the graph contains `coverageReport`, `diffCoverage` or any Kover report task (root or module, matched by type or by a `kover` name prefix), it throws"
**Failure scenario:** Kover wires `koverVerify` into `check` by default. After this change, `./gradlew check` or `./gradlew build` (and IDE actions that call them) put `koverVerify` in the graph with instrumentation off, and fail with "Coverage needs instrumentation". The spec doesn't say whether `koverVerify` counts as a "report task".
**Fix:** Add to design.md 4a: "The module plugin removes Kover's `koverVerify` from `check`, so `./gradlew check` and `./gradlew build` behave as before and never trigger the guard. Task 2.3 verifies `./gradlew :core:domain:check` succeeds." Add to the spec requirement: "`check` and `build` SHALL NOT run any Kover task."

### D1: Design 4a contradicts D20 and its own guard
**Severity:** major · **Status:** applied
**Location:** design.md > 4a. Instrumentation only for coverage runs
**Current text:** "Gradle's abbreviated task names (`cR`) aren't matched and get no coverage. That's acceptable: the report then shows no data rather than slowing down other runs."
**Failure scenario:** D20 and the spec say `cR` fails before any task runs. An implementer reading this bullet first builds the silent empty report and skips the guard, or treats the two as alternatives.
**Fix:** "Gradle's abbreviated task names (`cR`) aren't matched, so instrumentation stays off. The guard below makes such a run fail before any task runs (D20)."

### D2: `*DebugOverlay*` matches nothing in the code
**Severity:** minor · **Status:** applied
**Location:** design.md > 3. Filters; specs/test-coverage/spec.md > Scenario: Generated and wiring code is excluded
**Current text:** "classes `*DebugOverlay*`" / "and no `DebugOverlay` class"
**Failure scenario:** The overlay is `DebugSessionOverlay` in `composeApp/.../session/DebugSessionOverlay.kt`, so the pattern never matches and the scenario can't fail. Any non-composable helper in that file (facade `DebugSessionOverlayKt`) is reported.
**Fix:** Design: "classes `*Debug*Overlay*`". Spec: "no class whose name matches `*Debug*Overlay*` (today `DebugSessionOverlay`)".

### D3: Design cites D1–D15 but decisions go to D20
**Severity:** minor · **Status:** applied
**Location:** design.md > Context
**Current text:** "decisions.md for the settled choices (D1–D15)"
**Failure scenario:** A reader skips D16–D20 (diff-cover pin, no instrumentation, untracked files, manual verification, the guard).
**Fix:** "decisions.md for the settled choices (D1–D20)"

### D4: Commit rule in tasks.md differs from CLAUDE.md
**Severity:** minor · **Status:** applied with edits
**Location:** tasks.md > Workflow follow-up
**Current text:** "Commit only after the user's OK is relayed"
**Failure scenario:** CLAUDE.md says to commit automatically once a chunk is done and the gates pass. An implementer has to pick one. CLAUDE.md wins by its own rule unless this change is meant to override it.
**Fix:** If the wait is intended for the orchestrated run: "Commit only after the user's OK is relayed (orchestrated change; overrides CLAUDE.md's automatic commits for this change only)." Otherwise: "Commit each chunk once the gates pass, as in CLAUDE.md."

## Trust boundaries
- Network (PyPI via `uvx`): a malicious new release of a diff-cover dependency runs locally. diff-cover itself is pinned; its dependencies are not. Not stopped (S1).
- Gradle properties (`coverage.compareBranch`, `coverage.modules`): values come from the developer running the command; a ref starting with `-` could be read as a git option, but the attacker would be the same person. Not a realistic gap.
- git working tree (untracked files fed to diff-cover): only reads local files the developer owns. No gap.
- Report output (`build/reports/`): HTML contains source, stays local, and `build/` is ignored by git. No gap.
- No auth, user input, imported files, deep links, IPC or third-party services in the app are touched.

## Coverage
Checked all 10 requirements and 25 scenarios in `specs/test-coverage/spec.md`, plus proposal.md, design.md, tasks.md and decisions.md. Confirmed in code: 21 Gradle modules, the `*UiTest` filter, the androidMain files and their `.android.kt` naming, `toNfc`, the `fixture` package, `AndroidFileSharer`, `SaveExportActivity` and `DebugSessionOverlay`. No other active changes. Not read: Kover and diff-cover docs or release notes (A2 and A1's claims about Kover defaults are for task 1.2/2.3 to confirm), the backend repo's guard implementation (D20), and the build-logic plugins beyond the test filter. The shared-spec store wasn't read, since the change depends on no shared requirement.

## Applied changes
Applied on 2026-11-23 at the user's request ("apply all recommendations"). All 12 findings applied.

- `specs/test-coverage/spec.md`: T1, T2 (new scenario "Coverage run after a gate run"), T3, T4, T5, A2 (new sentence and scenario "check and build are unaffected"), S1, D2.
- `design.md`: D1, D2, D3, A1 (thresholds checked in the summary step, no Kover verify rule), A2, T2, S1 (`--exclude-newer` and `diffCoverExcludeNewer`).
- `tasks.md`: 1.1 (catalog date), 2.2 (`*Debug*Overlay*` check), 2.3 (agent, reverse-reuse and `check` checks), 3.1 (property parsing), 3.2 (threshold in summary step, decimals, failing-test check), 3.3 (`--exclude-newer`), Workflow follow-up (D4).
- `proposal.md`: the `uvx` command (S1).
- D4 applied with edits: of the two options, the "orchestrated override" wording was used, which keeps the existing intent of waiting for the relayed OK. Switch to "Commit each chunk once the gates pass, as in CLAUDE.md" if that wasn't intended.
- No contradictions with decisions.md. S1 extends D16 (same diff-cover version) but doesn't change it; whether the backend and web repos adopt `--exclude-newer` is left to them.
- No non-standard artifacts in the change folder. `openspec validate add-test-coverage --strict` passes.
- No code exists yet for this change, so the `*Debug*Overlay*` pattern lives in design.md, the spec and task 2.2 until implementation.
