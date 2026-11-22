# Tasks

Each group ends with the gates from CLAUDE.md (`:androidApp:assembleDebug`, `testAndroidHostTest`, `iosSimulatorArm64Test`, `:composeApp:linkDebugFrameworkIosSimulatorArm64`) and its own commit.

## 1. Shared text rule and plural text (core)

- [x] 1.1 Add `expect fun String.toNfc()` / `toNfd()` in `:core:domain` with Android (`java.text.Normalizer`) and iOS (`NSString` canonical mapping) actuals. Verify with a common test that decomposed "é" becomes one code point and back, green on Android host and the iOS simulator.
- [x] 1.2 Add `normalizeText`, `codePointLength` and `validateName` → `NameError?` in `:core:domain` per design.md. Verify with adversarial unit tests covering every case listed under design.md Tests.
- [x] 1.3 Add `UiText.PluralResource` to `:core:presentation`, plus the name error strings ("Enter a name.", "Use at most {n} characters.", "Use a single line without control characters.") and `NameError.toUiText()`. Verify with a unit test of the mapping and a `*UiTest` that renders a plural for 0, 1 and 2.
- [x] 1.4 Commit `feat(core): add shared name rule and plural text`.

## 2. Boards domain

- [x] 2.1 Add `BoardSummary`, `BoardSaveError` with its field error types, `BoardRepository` and `RemoteBoardDataSource` in `boards:domain`, and the build file dependencies (`api(core.domain)`, kotlinx-datetime). Verify that the module compiles on both targets.
- [x] 2.2 Add `BoardKeyPrefill.fromName` (D7). Verify with unit tests: "Garden irrigation" → GARD, "Ärger" → ARGE, "ß", "Ø", digits only, "X 1", emoji, empty.
- [x] 2.3 Add `BoardOrder` per `api-conventions` "Board list order". Verify with unit tests for each tie-break level, including equal millisecond times and id string order.
- [x] 2.4 Commit `feat(boards): add board list domain`.

## 3. Board store

- [ ] 3.1 Implement `InMemoryBoardRepository` (single-flight refresh, insert/move/remove by `BoardOrder`, delete 404 = success, rename 404 removes). Verify with unit tests against a fake `RemoteBoardDataSource`: server order kept, failed refresh keeps the list, concurrent refreshes make one call, create/rename/delete updates, both 404 paths, `ensureLoaded()` joining a running load, a create, rename and delete that each complete during a running refresh are still in the store after it returns, and `clear()` resets `boards` to null and drops a refresh or save that returns after it (bound as add-auth-ui's `SessionScopedStore` if that has landed).
- [ ] 3.2 Add `FixtureBoardDataSource` and `fixtureBoardsModule` in `boards:data/fixture` with the Figma boards and the trigger names from design.md, plus `boardsDataModule` binding the repository as a `single`. Verify with a unit test per trigger name.
- [ ] 3.3 Commit `feat(boards): add in-memory board store and temporary fixture`.

## 4. Board list presentation basics

- [ ] 4.1 Add `RelativeTimeFormatter` with injected `Clock` and `TimeZone`, the plural and short-month string resources, and the "Updated {time}" text. Verify with unit tests in a pinned non-UTC zone for every boundary in the spec, a DST change day and New Year.
- [ ] 4.2 Add `BoardItemUi` mapping and the board count / card count plurals. Verify with unit tests for 0, 1 and many.
- [ ] 4.3 Add `BoardListReloader` (single, app scope, `isReloading`, refresh toast on failure). Verify with unit tests: failure shows the D12 toast once, and overlapping calls make one refresh.

## 5. Board list screen

- [ ] 5.1 Add `BoardListRoute`, State, Action, Event and `BoardListViewModel` (first load, retry, pull to refresh through the reloader, menu state, navigation events, including `OpenBoard(id, name, key)`). Verify with ViewModel unit tests: Loading → Loaded, Loading → Error → Retry, empty, refresh failure keeps items, list updates when the store changes.
- [ ] 5.2 Build `BoardListScreen` for Phone (large title, collapse, rows, footer count, ⋯ menu with "Signed in as", empty-state buttons) and Tablet/Desktop (page header with count and Import, adaptive tile grid), with loading skeletons, empty and error states in a `RefreshBox`, and previews per state and window class in `commonMain`. Verify against `get_design_context` values of every Figma 9:5 frame for these states (frame IDs via `get_metadata`, annotations via `use_figma`) and with `*UiTest`s for each state, the collapse, the menus, accessible names in the D22 style and 48 dp targets.
- [ ] 5.3 Commit `feat(boards): add board list screen`.

## 6. Board dialogs

- [ ] 6.1 Add `NewBoardRoute` + `NewBoardViewModel` + dialog UI: prefill, uppercase key, first-submit-then-live validation, local clash, saving lock, server field errors, network toast, `Created(id, name, key)` event. Verify with ViewModel unit tests for each spec scenario and a `*UiTest` for the dialog on Phone and Desktop.
- [ ] 6.2 Add `RenameBoardRoute` + ViewModel + dialog UI: read-only key, unchanged-name close, 404 close + toast, network toast, `Renamed(newName)` result. Verify with unit tests and a `*UiTest` on Phone and Desktop.
- [ ] 6.3 Add `DeleteBoardRoute` + ViewModel + dialog UI with the 0 / 1 / N card bodies, 404 as success, the network toast and the `Deleted` result. Verify with unit tests and a `*UiTest`.
- [ ] 6.4 Add `boardsGraph(...)` with the list and the three dialog destinations (transfer's dialog properties, no dim, `launchSingleTop`), plus `navigateToNewBoard/RenameBoard/DeleteBoard` the per-caller `onRenamed` / `onDeleted` callbacks (D21) and `boardsPresentationModule`. Verify with a `*UiTest` that opens each dialog from the list menu, that back during a save keeps the dialog, and that a test caller receives `Renamed` and `Deleted`.
- [ ] 6.5 Commit `feat(boards): add new, rename and delete board dialogs`.

## 7. Sidebar board section

- [ ] 7.1 Add `BoardsSidebarSection` and its ViewModel over `repository.boards`, with the New board entry. Verify with a `*UiTest` that a create, rename and delete in the store show in the section.
- [ ] 7.2 Commit `feat(boards): add sidebar board section`.

## 8. App shell and wiring

- [ ] 8.1 Add `AppShell` in `:composeApp` (Phone: NavHost only; Tablet/Desktop: `Sidebar` with All boards, Labels, `BoardsSidebarSection`, email/avatar and Log out). Null email hides the email and avatar, which needs a small `Sidebar` change if the current API can't. Feed `accountEmail` and `onLogOut` from add-auth-ui's `SessionManager.state` and `AppViewModel.onLogout()` if it has landed, otherwise null/no-op with a TODO (D20). Verify with `*UiTest`s for both window classes, including a null email.
- [ ] 8.2 Replace the add-transfer-ui (and, if landed, add-auth-ui) placeholders: `BoardListRoute` as start (inside the session gate if it exists), remove `AppStartRoute`, `onImported`/`onBoardListChanged` → `BoardListReloader.reload()`, Import entries → `navController.navigate(ImportBoardRoute) { launchSingleTop = true }`, start the first load with `repository.ensureLoaded()` once signed in, `BoardKeyOwnerLookup` over the store. Map `onOpenBoard(id, name, key)` to `BoardRoute(id, name, key)` if add-board-view-ui has landed (otherwise a no-op with a TODO), add a no-op TODO callback for Labels, and load `fixtureBoardsModule`. Verify with a `*UiTest` E2E: import success → list reloads, and the key clash dialog shows the loaded board's name.
- [ ] 8.3 Commit `feat: add app shell and wire the board list`.

## 9. Manual verification

- [ ] 9.1 Run the app on an Android emulator (phone and tablet) and the iOS simulator (iPhone and iPad, portrait and landscape). Walk every state and every fixture trigger. Check pull to refresh, the title collapse, the dialog layouts per size and dark theme against Figma 9:5. Done when the walkthrough finds no mismatch, or each mismatch is fixed and committed.

## 10. Remove the temporary fixture

- [ ] 10.1 Delete `boards:data/fixture` and `fixtureBoardsModule` (the D3 debug entry), and leave `RemoteBoardDataSource` unbound with a TODO for `mob-boards-connect`, or bind a source that returns `Network` errors if Koin needs a binding to start. Verify with the gates, then commit `chore(boards): remove temporary board fixture`.

## Workflow follow-up

- Hand the dialog routes and `BoardRepository` to the `mob-board-view-ui` session for its rebase.
- Archive the change after review, before `mob-boards-connect` starts.
