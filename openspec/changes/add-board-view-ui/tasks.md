# Tasks

All gates from CLAUDE.md (`:androidApp:assembleDebug`, `testAndroidHostTest`, `iosSimulatorArm64Test`, `:composeApp:linkDebugFrameworkIosSimulatorArm64`) must pass before each group's commit.

## 1. Prerequisites

- [ ] 1.1 Rebase onto the merged `add-board-list-ui` work. Note in decisions.md how the board view uses the board list's rename/delete board dialogs, its open-board callback (passing name and key for D39) and the shared name rule and strings in core (D35). Verify: the branch builds with all gates green before any board view code is added.

## 2. Domain

- [ ] 2.1 Add `BoardContent`, `Column`, `Card` with `displayId(boardKey)`, `Label`, `LabelColor` (20 names), `LabelSource` and the label order to `:core:domain` (design "Domain models"). Unit test `displayId` (number 1, 109, large numbers, no leading zeros) and label order (`frontend`, `Bug`, `api` → `api`, `Bug`, `frontend`, equal names that differ only in case). Verify: the tests pass on Android host and the iOS simulator.
- [ ] 2.2 Add `BoardDetailRepository`, `CardCreateRepository`, `BoardLoadError`, `ColumnWriteError` and `CardCreateError` to `:feature:boards:domain` (design "Repositories and errors"). Verify: compiles for Android and iOS. Commit `feat(core): ...` and `feat(boards): ...` separately.

## 3. ViewModel

- [ ] 3.1 Add `BoardState`, `BoardAction`, `BoardEvent` and `BoardViewModel` with load, retry and pull to refresh (labels fetched in parallel, D16), the header hint from the route (D39), the `Mutex` write path and `SavedStateHandle` restore (design "ViewModel and state"). Unit test the load, retry, refresh and restore break vectors from design "Testing", and the header with and without a hint. Verify: the tests pass on both targets.
- [ ] 3.2 Add the column writes (add with close and scroll-to-new-column event (D32), rename with the D19 no-op, move left/right, delete with the 404-as-done rule) and their response apply functions. Unit test the column break vectors, the stale move (D40), missing column and missing board paths (D23), delete board → `onBoardDeleted` without a toast (D34), and the ordered apply of back-to-back writes. Verify: the tests pass on both targets.
- [ ] 3.3 Add quick add (D22): one open inline field, Enter/submit, whitespace no-op, cancel, outside tap, keep text on failure, saving state (D36), validation timing (D12/D13/D33), and the discard dialog on a switch with text (D41). Unit test each case, including a refresh that removes the field's column (D37). Verify: the tests pass on both targets.
- [ ] 3.4 Map every error to its toast or field error in one place (D15, D16, D23), with string resources for all copy from proposal.md "Copy" and plurals (D9). Unit test with a fake `ToastController` that each error shows the exact message once. Verify: the tests pass on both targets. Commit `feat(boards): add board view model`.

## 4. Screen

- [ ] 4.1 Load the `figma:figma-design-to-code` skill and run `get_design_context` and `get_screenshot` on the Board view frames (Board, Loading, Couldn’t load, New board, All columns deleted, Column menu, Add column, Rename column, Delete column with cards, Quick add card, Board menu) for Phone 390, Tablet 834 and Desktop 1440 where they exist. Verify: the widths, spacing and tokens per window class are recorded in a "Figma values" section at the end of design.md.
- [ ] 4.2 Build `BoardScreen` for each window class (D20, D24, D31): header, column strip, columns with tiles and label chips, "Add card", "No cards yet", add column field, empty board, loading skeleton and load error. Add `@Preview`s in `commonMain` for each state and window class. Verify: `BoardScreenUiTest` covers each state on the phone and desktop layouts, and on tablet for the main board. It passes on the iOS simulator and Android host.
- [ ] 4.3 Add per-column pull to refresh (design "Column list and pull to refresh"). Adapt `BoardColumn` only if its slot can't hold the scrolling list, keeping its look. Verify: a `*UiTest` swipe-down on a column sends `Refresh(columnId)`, and the design-system previews are unchanged.
- [ ] 4.4 Build the column menu, rename dialog, both delete dialogs, the quick add field and the board ⋯ menu with saving states (D15), the D41 discard dialog and the D38 accessibility labels. Verify: `BoardRootUiTest` (Root + real ViewModel + fakes) covers add card twice in a row, move right, move left disabled on the first column, rename unchanged, rename failure toast, discard dialog Keep editing and Discard, delete with cards, delete empty, and a missing board leading to `onBoardMissing`. It passes on both targets. Commit `feat(boards): add board view screen`.

## 5. Navigation and wiring

- [ ] 5.1 Add `BoardRoute(boardId)` and `boardViewDestination(...)` (design "Navigation and wiring"), and register the board view Koin module. Verify: the Koin `verify()` test passes.
- [ ] 5.2 Wire `:composeApp`: open-board from the board list, `onBack`, `onExportClick` → `ExportBoardRoute` (`launchSingleTop`), `onBoardMissing`/`onBoardDeleted` → back to the board list, `onCardClick` no-op, and the board list's rename/delete dialogs from the board menu. Verify: all gates pass, and a `*UiTest` in `:composeApp` or the feature checks that "Export as JSON" navigates with the board's ID and name. Commit `feat(boards): wire board view navigation`.

## 6. Fixture and manual check

- [ ] 6.1 Add `FixtureBoardDetailRepository`, `FixtureCardCreateRepository` and `FixtureLabelSource` with the Figma sample board and a new-board sample, bound only when `isDebug` is true. Add a temporary debug entry if the board list doesn't open boards yet (D6). Verify: the Koin `verify()` test passes for both values of `isDebug`. Commit `chore(boards): add temporary board view fixture`.
- [ ] 6.2 Manual check on an Android emulator and the iOS simulator, phone and tablet, light and dark: every state from 4.2, quick add, column menu actions, refresh, both delete dialogs, and export from the board menu. Compare each screen with its Figma frame. Verify: the user confirms the check.

## 7. Clean-up

- [ ] 7.1 Remove the fixtures, their Koin bindings and any debug entry in their own commit. Verify: no `Fixture*` class is left in `:feature:boards`, and all gates pass. Commit `chore(boards): remove temporary board view fixture`.

## Workflow follow-up

- Hand-off for `mob-boards-connect`: map `be-boards` responses to `BoardContent`/`Column`, the move response to the ordered IDs, 404 on board and column, `OUT_OF_RANGE` to `Stale`, and name field errors to `InvalidName`.
- Hand-off for `mob-cards-connect`: implement `CardCreateRepository` once `be-cards` defines the card contract.
- Archive the change after review.
