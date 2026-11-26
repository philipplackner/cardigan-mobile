# Tasks

One-time targets for this change (don't write them into the build, CLAUDE.md, skills or main specs): total line coverage ≥ 80%, and a mutation score ≥ 80% in every module that has mutants. Every commit needs the four gates green. New tests go in `commonTest`, use fakes, and must pass on the Android host and the iOS simulator.

## 1. Flaky timing test

- [x] 1.1 In `EmailRuleTest`, remove the `measureTime` block, the duration assertion and the `println` from `specNearMatchThroughTheRuleIsTooLongFast`. Keep the `TooLong` assertion and rename the test to describe what it checks (design §8). The three `*RejectedFast` tests that use `assertPatternRejectsFast` keep their limit (D8 names only one test). If one of them fails a `coverageReport` or `mutationReport` run, stop and ask. Verify: `./gradlew :feature:auth:domain:testAndroidHostTest` and `./gradlew coverageReport -Pcoverage.modules=:feature:auth:domain` pass. Commit `test(auth): drop wall-clock assertion from email length test`.

## 2. Coverage leaves out tokens and nav wiring

- [x] 2.1 Move the token and nav globs from `MutationExclusions.classGlobs` to `CodeExclusions.classes`. Leave the compiler-noise globs in `MutationExclusions` (design §1). Update `MutationExclusionsTest` so it covers the new split. Verify: `./gradlew -p build-logic test` passes.
- [x] 2.2 Check the test-coverage delta scenarios on a real `./gradlew coverageReport`:
  - no theme token classes and no `*Route`/`*GraphKt`/`*NavigationKt`/`*NavHostKt` classes in `navigation` packages
  - `WindowClassKt`, `SessionGateKt`, `AppShellKt`, `safeCall`, `UiText` and `HttpClientFactory` are still listed
  - total line coverage is about 82%

  Verify: inspect `build/reports/kover/report.xml`, and run `./gradlew mutationReport -Pmutation.modules=:core:designsystem,:feature:boards:presentation`, whose mutant counts must match the baseline.
- [x] 2.3 Update the coverage bullet in CLAUDE.md's Testing section: coverage now also leaves out design tokens and navigation routes/graphs. The mutation bullet says these come from the shared list. Don't add targets. Verify: re-read both bullets against `CodeExclusions`. Commit 2.1–2.3 as `build: leave design tokens and nav wiring out of coverage`.

## 3. New PIT filter rules

- [x] 3.1 Add `kotlin-metadata-jvm` to the version catalog with version ref `kotlin`, and as `implementation` of `build-logic:pitest-filters` (D11, design §3). Verify: `./gradlew -p build-logic build` passes, and `./gradlew testAndroidHostTest --dry-run` resolves no PIT or metadata artifact.
- [x] 3.2 Add the inline-function interceptor `FKOTLININLINE`: read Kotlin metadata and drop mutants in methods whose JVM signature belongs to an `inline` function, and in classes enclosed by such a function, directly or through outer classes (design §2). Unreadable metadata fails the run with the class name (design, Risks). Write unit tests next to `ComposeUiFilterTest` for: inline vs non-inline overload, inline in an `object`, `suspend inline`, reified inline, a `suspend inline` function whose body starts a suspend lambda (mutants in the enclosed `$<name>$<n>` class are dropped), and unreadable metadata failing with the class name. Verify: `./gradlew -p build-logic test` passes.
- [x] 3.3 Add the navigation-receiver interceptor `FNAVRECEIVER`. It drops top-level functions (static file-facade methods) whose first JVM parameter is `NavController`, `NavHostController` or `NavDestination`, plus their lambdas and the classes they enclose. Unit tests cover:
  - each receiver type
  - a non-nav first parameter that stays in
  - nested lambdas
  - a same-prefix sibling that stays in
  - a member extension in an `object` that stays in (only top-level functions are matched)

  Verify: `./gradlew -p build-logic test` passes.
- [x] 3.4 Extend `ComposeUiFilter` so local functions declared in UI methods (`<uiMethod>$<name>`) are treated as enclosed. Add tests for a local function in a composable, and for a same-prefix top-level function that stays in. Verify: `./gradlew -p build-logic test` passes, and the existing `ComposeUiFilterTest` cases stay green.
- [x] 3.5 Add the named-member interceptor `FNAMEDEXCLUDE`, fed by a `Class#methodGlob` list in `MutationExclusions` with `UiText*#asStringAsync*` (the interface, its `$DefaultImpls` class and the bridge methods in its nested implementations) and `HttpClientFactory#create$lambda*`. The class part is a glob on the binary name. Unit tests cover: a match, an `asStringAsync` bridge in a nested implementation class and in `$DefaultImpls` being dropped, a same-named method in another class that stays in, and the other members of both classes that stay in. Verify: `./gradlew -p build-logic test` passes.
- [x] 3.6 Enable the three new features in `MutationExclusions.features`. Check every mutation-testing delta scenario on a real `./gradlew mutationReport`:
  - inline helpers and the `HttpClientExtKt$<name>$<n>` classes they enclose dropped, while `constructRoute`, `toCommonNetworkError` and `asEmptyResult` keep their mutants
  - nav helpers dropped, while `destinationFor` keeps its mutants
  - `BoardRootKt` has no mutants, while `BoardViewModel` still does
  - named members dropped

  Also check that mutant counts in modules the new rules don't touch (for example `:feature:auth:domain`) are unchanged. Verify: inspect the `mutations.xml` files and the summary.
- [x] 3.7 Update the mutation bullet in CLAUDE.md's Testing section: mutation testing also leaves out inline functions and the classes they enclose, top-level functions whose receiver or first parameter is a NavController/NavHostController/NavDestination, local functions inside composables, `UiText.asStringAsync` and the `HttpClientFactory` config lambdas. Don't add targets. Verify: re-read the bullet against `MutationExclusions`. Commit group 3 as `build: leave unkillable inline, navigation and composable-local code out of mutation testing`, or split it into one commit per rule if that's clearer.

## 4. Fresh baseline

- [x] 4.1 Run `./gradlew coverageReport` and `./gradlew mutationReport`. Record the total line coverage and the per-module mutation scores in this change's notes, together with the classes that have the most surviving and no-coverage mutants per module. Verify: the numbers are written down, and the module order for groups 5–9 follows the biggest gaps (design §5).

## 5. Core modules

- [x] 5.1 `:core:designsystem`: test `AnchoredPositionProvider.calculatePosition` for every `PopupAlignment` and edge clamping, plus `ToastHostState.show` and `ToastVisuals`. Verify: the module's mutation score is ≥ 80% (`./gradlew mutationReport -Pmutation.modules=:core:designsystem`), and the tests pass on the host and iOS. Commit `test(designsystem): …`.
- [x] 5.2 `:core:domain`: kill the survivors in `LabelOrder`/`compareByCodePoints` (surrogate pairs vs U+E000..U+FFFF, ties), `BoardKeyRule` (`normalize`, `validate`, `isValid`), and the text rules (`isUnicodeWhiteSpace`, `hasInvalidCharacter`), plus `Board`/`Card` fields through behavior. Verify: the module's score is ≥ 80%, and the tests pass on the host and iOS. Commit `test(core): …`.
- [x] 5.3 `:core:data` and `:core:presentation`: confirm both are ≥ 80% after the exclusions. Kill any survivor left. Cover `UiText` logic outside `asStringAsync`. Verify: per-module scores ≥ 80%. Commit only if tests were added.

## 6. composeApp

- [x] 6.1 Test `destinationFor` for every `SessionState` (SignedOut expired / not expired, SignedIn, Restoring), and kill the remaining survivor. Verify: `:composeApp` score ≥ 80%, and the tests pass on the host and iOS. Commit `test: …`.

## 7. Boards domain and data

- [x] 7.1 `:feature:boards:domain`: kill the `BoardKeyPrefill.fromName` survivors, and the no-coverage mutants in `BoardSummary`/`BoardSaveError` through behavior that reads them. Verify: module score ≥ 80%. Commit `test(boards): …`.
- [x] 7.2 `:feature:boards:data`: test the `NotConnectedRemoteBoardDataSource` and `NotConnectedBoardDetailRepository` failures, and the `InMemoryBoardRepository` survivors (`applySave`, `refresh`, `collect`, `clear`, `createBoard`; `setIfSession` is inline and leaves the report after group 3). Verify: module score ≥ 80%, and the tests pass on the host and iOS. Commit `test(boards): …`.

## 8. Boards presentation

- [x] 8.1 `BoardViewModel`: assert the intermediate states in `addColumn`, `createCard`, `reconciled`, `applyColumnOrder`, `persist` and the coroutine paths (design §6). Where a survivor is equivalent, refactor it away in its own commit, with behavior unchanged (design §7). Verify: the `BoardViewModel` survivors drop well below the baseline of 76, and the tests pass on the host and iOS. Commit `test(boards): …` (plus `refactor(boards): …` for any refactor).
- [x] 8.2 Rename/New/Delete/BoardList ViewModels, `BoardsSidebarViewModel`, `BoardDialog`, `BoardState` header fields, `BoardListReloader`, `RelativeTimeFormatter`, and `BoardFieldErrors.toUiText`: kill the survivors and the no-coverage mutants. Verify: `:feature:boards:presentation` score ≥ 80%. Commit `test(boards): …`.

## 9. Transfer and auth

- [x] 9.1 `:feature:transfer:presentation`: `ImportBoardViewModel` (`onUploadFailed`, `upload`, `finish`, `onImportWithKey`, `onKeyChange`), `ExportBoardViewModel` coroutine paths, and `TransferUiText`. Verify: module score ≥ 80%, and the tests pass on the host and iOS. Commit `test(transfer): …`.
- [x] 9.2 `:feature:auth:presentation`: `LoginViewModel` (`withError`, `onFocusChange`, restore/save), `RegisterState` and `LoginState` derived fields. Verify: the module stays ≥ 80% with margin. Commit `test(auth): …`.
- [x] 9.3 Check that `:feature:transfer:data`, `:feature:transfer:domain` and `:feature:auth:domain` are still ≥ 80%, and kill the `JsonExportFileChecker` survivors if the score dropped. Verify: per-module scores ≥ 80%.

## 10. Preview data and dead code

- [x] 10.1 Move the preview data lists from `RegisterRoot`, `LoginRoot`, `NewBoardRoot`, `RenameBoardRoot`, `BoardListScreen` and `ImportBoardScreen` into `*Previews.kt` files in the same package. Previews must look the same. Verify: the previews still compile (`:androidApp:assembleDebug`), and the coverage report no longer lists those lines. Commit `refactor: move preview data into preview files`.
- [x] 10.2 Remove any dead code found during groups 5–9, with behavior unchanged, one commit each. If a change would affect anything users can see, ask the user first. Verify: gates green.

## 11. Final check

- [x] 11.1 Run `./gradlew coverageReport -Pcoverage.minLine=80` (the flag is passed on this run only) and `./gradlew mutationReport`. Confirm that total line coverage is ≥ 80%, and that every module listed in the mutation summary is ≥ 80%. Record the final numbers next to the baselines in this change's notes. Verify: both commands succeed, and no module in the summary is below 80%.
- [x] 11.2 Run all four gates and `./gradlew -p build-logic test`. Check that no build file sets a default for `coverage.minLine`, `coverage.minBranch` or `mutation.minScore`, and that CLAUDE.md, the skills and the main specs don't mention the 80% targets. Verify: `grep -rn "80" CLAUDE.md .claude/skills build-logic openspec/specs` shows no target.

## Workflow follow-up

- Archive the change after review. The archive syncs the `test-coverage` and `mutation-testing` deltas into the main specs.
