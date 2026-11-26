# Decisions: improve-test-suite-quality

Confirmed with Philipp Lackner on 2026-11-26 (asked through the orchestrator session cardiganapp-65).

## Summary

Raise the mobile test suite to 80% total line coverage and an 80% mutation score in every module that has mutants. Do this mostly with new tests. Coverage and mutation testing also get closer exclusion rules: coverage drops design tokens and nav wiring like PIT already does, and PIT drops code it can't kill (inline functions, NavController helpers, code inside composables, two runtime-bound bits). The 80% numbers are a one-time goal for this change and are not written down as a lasting rule anywhere.

## Decisions

### D1 — Coverage target
- **Question**: How should the 80% coverage target be measured?
- **Decision**: Total line coverage across all measured files reaches 80%. Branch coverage is not a target.
- **Alternatives**: Total line and branch; per module, line and branch.
- **Why**: User's choice. Per file can't be reached with host tests.
- **Depends on**: —

### D2 — Closing the line gap
- **Question**: How should the line gap be closed, given that most of it is design tokens and Compose leftovers?
- **Decision**: Align the coverage exclusions with mutation testing. Design tokens in the theme package and nav routes/graphs leave the coverage report. Then cover the remaining logic with tests.
- **Alternatives**: Test the tokens against the Figma values; both.
- **Why**: User's choice. Tokens are already out of mutation testing.
- **Depends on**: D1

### D3 — Mutation target
- **Question**: How should the 80% mutation score be measured?
- **Decision**: Every Gradle module that has mutants reaches 80%, including `:core:presentation`. Per module, not per file.
- **Alternatives**: Total score.
- **Why**: User's own answer, given once for both the web and the mobile change. Web maps it to its own units (app, core, each feature). Mobile keeps Gradle modules.
- **Depends on**: —

### D4 — New mutation exclusions
- **Question**: Which areas that can't be killed, or only through UI tests, should mutation testing leave out?
- **Decision**: All four: inline functions, NavController/NavHostController helpers, local functions and effects inside composables, and the runtime-bound bits (`UiText.asStringAsync`, the `HttpClientFactory` plugin-config lambdas). The spec scenario that says `openBoardId` in `AppShellKt` must have mutants is reversed. `destinationFor` in `SessionGateKt` stays in.
- **Alternatives**: Any subset; refactor the nav helpers into pure functions and test them.
- **Why**: User's choice.
- **Depends on**: —

### D5 — How the new mutation exclusions match
- **Question**: Generic rules or named lists?
- **Decision**: Generic rules in `build-logic:pitest-filters`: inline functions from Kotlin metadata, nav helpers by their NavController/NavHostController receiver, composable-local code by extending the existing Compose UI filter. Only the two runtime-bound bits are matched by name.
- **Alternatives**: A named list for everything.
- **Why**: Matches the existing spec rule that new code following the layout is excluded without a build change.
- **Depends on**: D4

### D6 — Coverage does not share the new mutation exclusions
- **Question**: Should coverage also leave out the D4 areas?
- **Decision**: No. Coverage leaves out only the tokens and nav wiring from D2. The D4 areas stay measured.
- **Alternatives**: Also leave out the nav helpers, composable-local code, or the runtime-bound bits.
- **Why**: User's choice (re-asked as a coverage-only question). They are about 30 lines, and the target is reachable without hiding them.
- **Depends on**: D2, D4

### D7 — Product code changes
- **Question**: May the change touch product code beyond tests?
- **Decision**: Yes, with behavior unchanged: small fixes, dead-code removal, refactoring equivalent mutants away, and moving preview data from Root/Screen files into `*Previews.kt` files. Each goes in its own commit. Anything users can see goes back to the user first.
- **Alternatives**: Tests only; tests plus bug fixes.
- **Why**: User's choice.
- **Depends on**: —

### D8 — Flaky timing test
- **Question**: Should this change fix the flaky wall-clock test, and how?
- **Decision**: Fix it in this change. `EmailRuleTest.specNearMatchThroughTheRuleIsTooLongFast` drops its wall-clock assertion and keeps the `TooLong` assertion.
- **Alternatives**: Leave it; a loose time limit.
- **Why**: A failing test stops both `coverageReport` and `mutationReport`, so the measurements would fail at random.
- **Depends on**: —

### D9 — Targets are one-time
- **Question**: Where do the 80% numbers live?
- **Decision**: Only in this change's proposal and tasks. No default threshold in the build. Nothing in CLAUDE.md, skills or main specs. CLAUDE.md's description of what coverage and mutation testing leave out is updated to match D2 and D4 (the exclusions, not the targets).
- **Alternatives**: —
- **Why**: Given in the orchestrator brief.
- **Depends on**: D2, D4

### D10 — Test rules and verification
- **Question**: (settled from CLAUDE.md, confirmed in the summary)
- **Decision**: New tests go in `commonTest`, use fakes and not mocks, and must be green on the Android host and the iOS simulator. All four gates pass before each commit. At the end, run `coverageReport` and `mutationReport` and read the total line coverage and the per-module mutation scores.
- **Alternatives**: —
- **Why**: Project rules.
- **Depends on**: D1, D3

### D11 — Dependency for inline detection
- **Question**: May `kotlin-metadata-jvm` be added to `build-logic:pitest-filters` so inline functions are detected generically?
- **Decision**: Yes. Add `org.jetbrains.kotlin:kotlin-metadata-jvm`, versioned with Kotlin and pinned in the catalog. It is on PIT's classpath only during `mutationReport` and never in the app.
- **Alternatives**: A named list for inline functions.
- **Why**: Needed for the generic rule in D5. Non-reified inline functions carry no bytecode marker other than Kotlin metadata.
- **Depends on**: D5

### D12 — Tests for the new filter rules
- **Question**: How should the new PIT filter rules be tested?
- **Decision**: Unit tests with synthetic classes next to `ComposeUiFilterTest` in `build-logic/convention/src/test` (kotlin.test, no new dependency), plus the real-report checks from the spec scenarios.
- **Alternatives**: Real-report checks only. That was chosen first, based on my wrong claim that unit tests would need a new test framework, and replaced after the correction.
- **Why**: Same pattern as the existing Compose UI filter. Pins edge cases such as overloads and nested lambdas.
- **Depends on**: D5, D11

## Facts

- Coverage today: line 66.0% (1569/2379), branch 77.5% (1111/1433), 20 modules. — source: `./gradlew coverageReport` on 2026-11-26
- Of the 810 uncovered lines: 440 are design tokens, 251 are Compose leftovers (lambdas inside composables, preview data, design system components), 51 are nav routes/graphs and 68 are logic. — source: `build/reports/kover/report.xml`
- With the PIT token and nav globs applied to Kover, line coverage is 82.0% (1569/1913) before any new test. — source: simulated on `report.xml` with `MutationExclusions.classGlobs`
- Mutation today: total 68.4% (1010/1477), test strength 83.8%. Per module: `:composeApp` 22.2%, `:core:data` 15.8%, `:core:designsystem` 19.1%, `:core:presentation` 42.9%, `:core:domain` 68.9%, `:feature:auth:domain` 97.3%, `:feature:auth:presentation` 81.7%, `:feature:boards:data` 63.4%, `:feature:boards:domain` 75.0%, `:feature:boards:presentation` 70.9%, `:feature:transfer:data` 87.5%, `:feature:transfer:domain` 85.0%, `:feature:transfer:presentation` 75.8%. — source: `./gradlew mutationReport` on 2026-11-26
- 82 mutants sit in inline functions (HttpClientExt 60, `forEachCodePoint` 16, `Result` helpers 6) and can't be killed, because tests run the inlined copy. — source: `*/build/reports/pitest/mutations.xml`
- `EmailRuleTest.specNearMatchThroughTheRuleIsTooLongFast` took 262 ms under Kover instrumentation against a 100 ms limit and failed the first coverage run. — source: `feature/auth/domain` test results
- `build-logic/convention/src/test` already has `ComposeUiFilterTest`, `MutationExclusionsTest` and `MutationCountsTest` on kotlin.test, with `pitest-filters` as a test dependency. — source: `build-logic/convention/build.gradle.kts`
- `*UiTest` classes don't run on the Android host, so they add nothing to coverage or mutation scores. — source: `test-coverage` and `mutation-testing` specs
- The token and nav exclusions live in `MutationExclusions.classGlobs`. The shared list for both tools is `CodeExclusions`. — source: `build-logic/convention/.../convention/`

## Out of scope

- A branch coverage target.
- Compose UI tests written to raise coverage or mutation scores.
- CI gates or lasting thresholds for coverage or mutation score.
- Coverage or mutation for `:androidApp`, iOS tests or device tests.

## Deferred

None.
