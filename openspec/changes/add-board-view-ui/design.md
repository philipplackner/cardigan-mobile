# Design

## Context

See proposal.md for the why and decisions.md for every settled choice. Relevant current state:

- `:feature:boards` has only build files. The board list change (`add-board-list-ui`) is planned in parallel and lands first. This change is rebased onto it (D5). It brings `BoardRepository`, the reusable rename/delete board dialogs, the sidebar/phone menu (D3), and the shared name rule and its strings in core (D35).
- `:core:domain` has `Board(id, name, key)`, `BoardKeyRule`, `Result` and `DataError`. `:core:presentation` has `UiText`, `ObserveAsEvents` and `ToastController`. `:composeApp` has the NavHost, the app toast host and `initKoin(isDebug)` from add-transfer-ui.
- `:core:designsystem` has `BoardColumn`, `BoardCard`, `AddCardButton`, `AddColumnButton`, `Menu`/`MenuItem`, `CardiganDialog`, `Input`/`Field`, `RefreshBox`, `Skeleton`, `EmptyState`, `LabelChip`, `TitleAppBar` and `LocalWindowClass` (Phone/Tablet/Desktop, breakpoints in one place).
- `be-boards` is archived and implemented: the board and column endpoints exist, with integer positions and a move endpoint that returns all columns. There is no backend change for cards yet. Cards in the board load, card create (quick add) and the card count come from the later `be-cards` change. The repository interfaces here follow the shared specs, not a guessed wire format, and this change runs on fakes either way.

### Backend dependencies

- Cards on the board, quick add and the card count depend on `be-cards`.
- If the connect change runs before `be-cards`, that change handles the gap. This change doesn't plan for it.

## Goals / Non-Goals

**Goals:**
- A board view that later changes extend without restructuring: drag & drop adds a card move repository and gestures on the tiles, search & filter adds a filter over the same state, card detail plugs into `onCardClick`.
- All writes through one serialized path in the ViewModel, so responses are applied to a consistent local board.

**Non-Goals:**
- HTTP data sources, DTOs and mappers (`mob-boards-connect`, `mob-cards-connect`).
- Any change to `:core:designsystem` component visuals. Only the layout adaptation described under "Column list" if it turns out to be needed.

## Decisions

### Module and package layout

```
:core:domain            board/  BoardContent, Column, Card, Card.displayId()
                        label/  Label, LabelColor (20 palette names), LabelSource, label order
:feature:boards:domain  detail/ BoardDetailRepository, CardCreateRepository, error types
:feature:boards:data    detail/ FixtureBoardDetailRepository, FixtureCardCreateRepository, FixtureLabelSource (temporary, D6)
:feature:boards:presentation
                        detail/ BoardViewModel, BoardState/Action/Event, BoardRoot, BoardScreen, column/tile/dialog composables
                        navigation/ BoardRoute, boardViewDestination()
:composeApp             route wiring and callbacks
```

The board list's code sits in its own package. The only shared files are the feature's Koin modules and the nav graph registration (D5). The alternative of one shared repository was rejected in D5.

### Domain models (D7)

- `Card(id: Uuid, number: Long, title: String, labelIds: List<Uuid>)`. `displayId(boardKey)` builds `{key}-{number}` (`domain-card` "Card display ID"). Card detail adds the fields it needs (description, timestamps) when it first needs them.
- `Column(id: Uuid, name: String, cards: List<Card>)`. Order is the list order. Positions are dense (`domain-ordering`), so a list index is the position. Storing a separate `position` would only duplicate the index.
- `BoardContent(board: Board, columns: List<Column>)`, with `cardCount` derived from the columns.
- `Label(id: Uuid, name: String, color: LabelColor)`. `LabelColor` is a domain enum of the 20 palette names (`domain-label` "Label color is a palette name"). `:core:designsystem` already maps palette colors to theme colors. A small presentation mapper connects the two.
- `fun interface LabelSource { suspend fun labels(): Result<List<Label>, DataError> }`. It is read-only. The labels change implements it (D7).
- Label order on a tile: sorted by case-folded name. Common Kotlin has no full Unicode case folding, so `lowercase()` (locale-independent) is used for now (see Risks).

### Repositories and errors

```kotlin
interface BoardDetailRepository {
    suspend fun loadBoard(boardId: Uuid): Result<BoardContent, BoardLoadError>
    suspend fun addColumn(boardId: Uuid, name: String): Result<Column, ColumnWriteError>
    suspend fun renameColumn(boardId: Uuid, columnId: Uuid, name: String): Result<Column, ColumnWriteError>
    suspend fun moveColumn(boardId: Uuid, columnId: Uuid, position: Int): Result<List<Uuid>, ColumnWriteError>
    suspend fun deleteColumn(boardId: Uuid, columnId: Uuid): EmptyResult<ColumnWriteError>
}

interface CardCreateRepository {
    suspend fun createCard(boardId: Uuid, columnId: Uuid, title: String): Result<Card, CardCreateError>
}
```

- `BoardLoadError`: `BoardNotFound`, `Failed`.
- `ColumnWriteError`: `BoardNotFound`, `ColumnNotFound`, `Stale` (a move answered with `OUT_OF_RANGE`), `InvalidName(rule)`, `Failed`.
- `CardCreateError`: `BoardNotFound`, `ColumnNotFound`, `InvalidTitle(rule)`, `Failed`.
- `Failed` covers network failures, unknown outcomes, 5xx and unknown codes (`api-errors` "Clients handle unknown error codes by status"). `InvalidName`/`InvalidTitle` carry the shared text rule's error, so server field errors and device checks show the same message.
- `moveColumn` returns the column IDs in their new order. `api-conventions` "Move responses contain all affected items" says the server returns all columns with positions. The connect change maps that response to the ordered IDs.
- `deleteColumn` maps a 404 for the column to success (D15, `api-conventions` "Retries after a lost response").
- Card create always adds at the bottom of the column (Figma, D22). How the position is sent is up to the cards contract.

### ViewModel and state

One `BoardViewModel` per board route, with `boardId` from `SavedStateHandle`.

- `BoardState`: `content: LoadState` (`Loading`, `Error`, `Loaded(BoardContent, labelsById)`), `headerHint` (name and key from the route, D39), `isRefreshing`, `refreshingColumnId`, `inlineField: InlineField?` (one of `QuickAdd(columnId)` or `AddColumn`, each with text, isSaving, error and showErrors), `dialog: BoardDialog?` (`RenameColumn`, `DeleteColumn` with name and card count, and `DiscardInlineField` with the pending switch target), and `savingColumnMoveId`. A single `inlineField` keeps "one inline field at a time" structural.
- **Switching inline fields** (D41): opening another field closes an empty one. If the open field holds text (after trim), the target is held in the `DiscardInlineField` dialog. "Keep editing", back and outside tap clear the dialog and emit an event that scrolls the open field into view and focuses it. "Discard" replaces `inlineField` with the target and emits a focus event for it. While the open field is saving, the open action is ignored. Explicit Cancel/back drops the text without asking (D22). The dialog reuses the same dialog component as card detail's "Unsaved changes", with a secondary and a danger button.
- Load and refresh fetch the board and `LabelSource.labels()` in parallel. If either fails, the load fails: the first load shows the error state, and a refresh keeps the board and shows the refresh toast (D16). A label ID without a matching label is skipped on the tile.
- **One write at a time.** All writes and refreshes go through one `Mutex` in the order they arrive. Each response is applied to the state the previous write left (D21): add appends, rename replaces, move reorders by the returned IDs, delete removes, card create appends to its column. Without this, a move response could be applied on top of a board that a parallel delete had already changed. The rejected alternative was a full reload after every write, which D21 rules out.
- **Saving state** (D15, D36): while a dialog or inline field is saving, actions from it are ignored (Enter, Cancel, dismiss). The Root passes `isSaving` to the dialog's `dismissOnBackPress`/`dismissOnClickOutside`.
- **Validation** (D12, D13, D33): each name field, quick add included, tracks `showErrors`, which becomes true on the first submit. After that, the error is recomputed on every change. The board list's shared name rule in core (D35) normalizes the text, and the normalized value is sent. Rename compares normalized old and new names (D19).
- **Failures** map in one place:
  - `Failed`: the matching D15 toast through `ToastController`. Inputs stay.
  - `InvalidName`/`InvalidTitle`: the error under the field, with `showErrors = true`.
  - `ColumnNotFound`: close the dialog or field, reload the board, and show D23's "no longer exists" toast with the column name captured before the write.
  - `Stale` (move only): reload the board and show D40's "The board changed on another device" toast.
  - `BoardNotFound` from any call: show the "This board no longer exists." toast and emit `BoardEvent.BoardMissing`. The Root calls `onBoardMissing`.
- **Process death**: the route argument, the open field or dialog (column ID, kind) and its text are saved in `SavedStateHandle`. Saving flags are not saved. After a restore, the user submits again.
- **Pull to refresh with an open field** (D37): the refresh replaces `content`. A quick add field whose column is gone closes. Other fields and dialogs keep their text.

### Layout per window class (D20, D24)

- Header (D39): with 0 cards, the count part is dropped (phone "{key}" only). While loading or after a load error, it uses `headerHint` from the route, or shows no title. The board ⋯ button shows only in `Loaded`.
- The Root reads `LocalWindowClass`. `Phone` uses the phone header (back button, name, "{key} · {n} cards", board ⋯ button) and 300 dp columns. `Tablet` and `Desktop` use the desktop header (name, key tag, count, board ⋯ button) and the desktop column width and spacing. The values come from `get_design_context` on the frames.
- The column strip is a `LazyRow` with default fling and no snapping (D24). The end padding keeps the next column partly visible on phone.
- Search and filter controls from the frames are left out (D31). The header leaves room for them, so the search & filter change can add them without moving other elements.

### Column list and pull to refresh (D16)

Each column's cards sit in their own vertical scroll inside a `RefreshBox`. A pull in any column calls `Refresh(columnId)`. The indicator shows in that column and the whole board reloads. `BoardColumn` takes its cards as a `ColumnScope` slot. If that doesn't allow a lazy vertical list with nested scrolling, the task adds a slot variant to `BoardColumn` without changing its look. Columns are short today (no column holds hundreds of cards in the fixture), so a non-lazy `Column` with `verticalScroll` is acceptable if the lazy slot causes trouble. Card drag & drop will need the column's item positions, which both options provide.

### Dialogs and menus

- Column menu: `Menu` anchored to the column's ⋯ button, with the move items enabled from the column's index (D30). Picking an item closes the menu right away. A move shows no extra progress UI. The column stays in place until the response arrives (D21).
- Rename column: `CardiganDialog` with `Field` "Name", "Cancel" and "Save" (D20 reuses the desktop frame on phone).
- Delete column: `CardiganDialog` with the with-cards or empty copy (D23). The card count is taken from the loaded board when the dialog opens.
- Add column: an inline `Field` at the end of the strip with "Add column" and "Cancel" (D20). On a board without columns, the field replaces the empty state. After success the field closes and the strip scrolls to the new column with `animateScrollToItem` (D32).
- Board menu: "Rename board" and "Delete board" open the board list's dialogs (D5). Their API is whatever the board list change exposes. The board view needs two results: renamed (new name → update the header) and deleted (→ `onBoardDeleted`, back to the board list with no toast, D34). "Export as JSON" calls `onExportClick(boardId, boardName)`.

### Navigation and wiring

- `@Serializable data class BoardRoute(val boardId: String, val name: String? = null, val key: String? = null)` in `:feature:boards:presentation`. The board list passes the name and key it already has, so the header can show them while loading (D39). Without them, for example from the debug entry, the header has no title until the load finishes.
- Logout is not part of the board view. It lives in the board list's menu and sidebar and uses the auth change's `SessionManager` / `AppViewModel.onLogout()`.
- `NavGraphBuilder.boardViewDestination(onBack, onCardClick, onExportClick, onBoardMissing, onBoardDeleted)`.
- `:composeApp` wires `onExportClick` to `ExportBoardRoute(boardId, boardName)` with `launchSingleTop`, `onBoardMissing` and `onBoardDeleted` to pop back to the board list route, `onCardClick` to a no-op until card detail, and the board list's "open board" to `BoardRoute`.
- The toasts are shown from the ViewModel through `ToastController`, so they survive the navigation.

### Fixture and debug entry (D6)

`FixtureBoardDetailRepository`, `FixtureCardCreateRepository` and `FixtureLabelSource` in `:feature:boards:data` hold the Figma sample board ("Cardigan v1", key `CRD`, 4 columns, 12 cards, labels api/infra/web/mobile/design/bug/docs) and a new-board sample, with a short delay. They are bound only when `isDebug` is true, the same way add-transfer-ui did. If the board list change already opens boards, its board tiles are the debug entry. Otherwise a temporary debug route opens the sample board. The fixtures and any debug route are removed in their own commit before archive.

### Accessibility

Every icon button and tile gets a content description from string resources, and every touch target is at least 48 dp (CLAUDE.md). The labels are the ones accepted in D38. A tile merges its descendants into one node with the D38 text, so a screen reader reads it as one item.

### Testing

- Unit tests (`commonTest` of `:feature:boards:presentation`) for `BoardViewModel` with fake repositories, a fake `LabelSource`, a fake `ToastController` and a test dispatcher. Break vectors:
  - Double submit while saving.
  - Enter during a save.
  - A second quick add opened while one is saving, while one is empty, and while one holds text, then Keep editing, Discard, and back on the dialog.
  - Add column with text → quick add, and quick add with text → Add column.
  - Quick add with a pasted line break, and with 201 code points.
  - A move on the only column, the first and the last column.
  - A move answered `Stale`.
  - Rename of a missing column, and rename with only surrounding spaces.
  - Delete answered `ColumnNotFound`.
  - `BoardNotFound` from each write.
  - Refresh during a write, and refresh failure with an open field.
  - A refresh that removes the column of an open quick add.
  - Labels failing while the board loads.
  - A card with an unknown label ID.
  - Restore after process death with each field and dialog open.
  - Writes applied in order when two arrive back to back.
- Unit tests for `Card.displayId`, label ordering and the response apply functions.
- `BoardScreenUiTest` for each state and window class. `BoardRootUiTest` with the real ViewModel and fakes for quick add, the column menu, rename, delete and failure toasts. Both run on Android host and the iOS simulator.

## Risks / Trade-offs

- [Rebase onto the board list change] → Separate packages, and only the Koin module and nav graph are shared. Tasks start with the rebase and stop if the board list hooks (dialogs, text rule, open-board callback) are missing.
- [The card contract doesn't exist yet] → The interfaces follow the shared specs. `mob-cards-connect` may have to map extra response fields. The interfaces return domain types only, so the presentation layer won't change.
- [`lowercase()` is not full case folding] → It matches for nearly all label names. The labels change owns the comparator and can replace it in one place.
- [Nested scrolling (horizontal strip, vertical columns, pull to refresh) behaves differently on iOS] → `*UiTest`s run on the iOS simulator, and manual checks on both platforms use the fixture.

## Open Questions

- The exact API of the board list's rename/delete board dialogs. It doesn't change this design: the board view needs only the renamed and deleted results.
