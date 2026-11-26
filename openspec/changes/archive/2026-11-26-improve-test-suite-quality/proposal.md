# Proposal

## Why

Coverage and mutation testing now run on demand, and the first full runs show where the suite is weak. Total line coverage is 66.0%. The mutation score is 68.4%, and some modules score much lower: `:core:data` 15.8%, `:core:designsystem` 19.1% and `:composeApp` 22.2%. Part of the gap is real missing tests, mostly in ViewModels. The rest is noise: design tokens counted in coverage, and mutants in code the host tests can never reach. This change fixes both, so the numbers point at real gaps.

## What Changes

One-time goals for this change only. They are not documented as a lasting rule or set as a default threshold:

- Total line coverage of all measured files reaches **80%** (today 66.0%).
- Every Gradle module that has mutants reaches a mutation score of **80%** (today 68.4% in total).

To get there:

- **Coverage exclusions**: the coverage report also leaves out design tokens in the design system theme package and nav routes/graphs. Mutation testing already leaves them out. That alone gives 82.0% line coverage.
- **Mutation exclusions**: mutation testing also leaves out inline functions and the classes they enclose, top-level helpers whose receiver or first parameter is a NavController, NavHostController or NavDestination, local functions and effects inside composables, `UiText.asStringAsync`, and the `HttpClientFactory` plugin-config lambdas. All are matched by generic rules, except the last two, which are named. The existing scenario that `openBoardId` in `AppShellKt` must have mutants is reversed. Coverage keeps measuring these areas.
- **New tests** for the weak spots found in the reports (details in design.md and tasks.md):
  - `:feature:boards:presentation`: `BoardViewModel` (91 mutants not killed), Rename/New/Delete/BoardList ViewModels, `BoardDialog`, `BoardState`
  - `:core:designsystem`: `AnchoredPositionProvider` popup math, `ToastHostState`, `ToastVisuals`
  - `:core:domain`: `LabelOrder` code-point comparison, `BoardKeyRule`, text rules
  - `:feature:boards:data`: `InMemoryBoardRepository` and the NotConnected placeholder data sources
  - `:composeApp`: `destinationFor`
  - `:feature:auth:presentation` and `:feature:transfer:*`: `LoginViewModel`, `RegisterState`, `ImportBoardViewModel`, `ExportBoardViewModel`
  - The uncovered logic files: `TransferUiText`, `BoardKeyErrorText`, `BoardFieldErrors`, `UiText`, `RelativeTimeFormatter`
- **Product code changes with behavior unchanged**, each in its own commit: moving preview data from Root/Screen files into `*Previews.kt`, refactoring equivalent mutants away, removing dead code, and small fixes for bugs the new tests find. Anything users can see goes back to the user first.
- **Flaky test fix**: `EmailRuleTest.specNearMatchThroughTheRuleIsTooLongFast` drops its 100 ms wall-clock assertion. Under coverage instrumentation the check took 262 ms and failed the run.
- CLAUDE.md's description of the coverage and mutation exclusions is updated. The targets are not added there.

No app behavior changes. No change is breaking for users. For developers, coverage and mutation numbers shift because the measured set changes.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `test-coverage`: the "Excluded code" requirement also leaves out design tokens in the theme package and nav routes/graphs.
- `mutation-testing`: the "Excluded code" and "Compose UI code is not mutated" requirements also leave out inline functions and the classes they enclose, NavController/NavDestination helpers, local functions inside composables and two named runtime-bound members. The `openBoardId` scenario is reversed.

## Shared specs

This change implements no shared-spec requirement and changes no shared behavior. The new tests check existing mobile code against these shared requirements, without changing them:

- `domain-user`: "Email format has one reference pattern", "Register enforces the email format" (`EmailRuleTest`)
- `text-length`: "Text is normalized before validation", "Length is counted in Unicode code points", "Invalid characters are rejected", "Clients validate with the same rule" (text rule tests)
- `domain-board`: "Board key format", "Board name" (`BoardKeyRule`, board field error tests)
- `domain-label`: "Label name" (`LabelOrder` tests)

## Impact

- `build-logic/convention`: `CodeExclusions` / `MutationExclusions` (token and nav globs move to the shared list) and the PIT feature string.
- `build-logic/pitest-filters`: new filter rules for inline functions, NavController receivers and composable-local functions, plus the two named members. One new build-only dependency, `kotlin-metadata-jvm` (confirmed), on PIT's classpath only.
- `commonTest` in most KMP modules: new and stronger tests.
- `commonMain`: preview data moves and behavior-neutral refactors only.
- `CLAUDE.md` Testing section: the exclusion lists.
- Main specs `test-coverage` and `mutation-testing` on archive.
- No app dependency, API or UI change. The web change (`web-test-suite-improvements`) runs in parallel and does not depend on this one.
