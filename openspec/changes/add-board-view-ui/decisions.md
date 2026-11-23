# Decisions: add-board-view-ui

Confirmed with the user via the coordinator session (cardiganapp-18) on 2026-11-21. The questions (MBV-1 to MBV-16) were sent to that session, which merged them with the questions of the five other sessions (mobile and web × auth, board list, board view) and relayed the user's answers. This follows the add-transfer-ui and add-design-system precedent. The D-numbers below are the coordinator's shared numbers, so the gaps belong to the other sessions. D30 and up are this change's own items. They were either settled from Figma and confirmed in the same round, or are follow-up questions.

## Summary

Build the mobile board view for chunk `mob-board-view-ui` in `:feature:boards`. It covers loading a board, the column strip on phone, tablet and desktop, quick add of cards, the column menu (move left/right, rename, delete), adding columns, the board ⋯ menu (rename, export, delete), pull to refresh, and the loading, empty and error states. Every write waits for the server. The feature runs against repository interfaces tested with test doubles, plus a temporary in-memory fixture for manual checks. Card drag & drop and search & filter are separate, later changes.

## Decisions

### D1 — Scope: three changes
- **Question**: One change for board view, card drag & drop and search & filter, or several?
- **Decision**: Three changes, in order: board view → card drag & drop → search & filter. This change covers the board view, quick add, the column menu and the board ⋯ menu only.
- **Alternatives**: One change for all three; two changes (board view + search, then drag & drop).
- **Why**: Follows the breakdown chunks 14, 27 and 28, with smaller reviews. Same split on web.
- **Depends on**: —

### D3 — App shell
- **Question**: Who builds the tablet/desktop sidebar and the phone menu?
- **Decision**: The board list change owns the sidebar and the phone menu. The board view renders only its content area.
- **Alternatives**: The board view builds them.
- **Why**: One owner for app navigation.
- **Depends on**: —

### D5 — Ownership inside `:feature:boards`
- **Question**: How do board list and board view share `:feature:boards`?
- **Decision**: Board list owns `BoardRepository` and the reusable rename board and delete board dialogs. Board view adds its own board-detail repository in the same module, reuses those dialogs, and is rebased onto the board list change.
- **Alternatives**: One shared repository interface agreed up front.
- **Why**: Separate packages, few shared files (Koin module, nav graph).
- **Depends on**: —

### D6 — Data while the backend is missing
- **Question**: How is the feature built and checked without backend endpoints?
- **Decision**: Repository interfaces plus test doubles. Also a temporary in-memory fixture repository and a debug entry for manual checks, removed in their own commit before archive.
- **Alternatives**: Interfaces and test doubles only.
- **Why**: Same as add-transfer-ui. Lets the user look at the screen on devices.
- **Depends on**: —

### D7 — Shared models
- **Question**: Where do card, column and label models live, given features can't depend on each other?
- **Decision**: `Card`, `Column`, the board content model and a minimal `Label` (id, name, color), plus a read-only `LabelSource` interface, go to `:core:domain`. `boards` owns card create (and later card move) repositories. `cards` owns card edit and delete. `labels` implements `LabelSource`.
- **Alternatives**: Each feature keeps its own card model and maps from the same DTO.
- **Why**: Tiles, card detail and labels show the same data.
- **Depends on**: D5

### D9 — Counts
- **Question**: How are counts written?
- **Decision**: Plural string resources. Zero parts are dropped.
- **Alternatives**: —
- **Why**: Shared rule across all sessions.
- **Depends on**: —

### D12 — Validation timing
- **Question**: When do field errors show, and when is the submit button disabled?
- **Decision**: Errors show on the first submit, then update live while typing. The button is disabled only while an error shows or a save runs.
- **Alternatives**: —
- **Why**: Shared rule across all sessions.
- **Depends on**: —

### D13 — Name error messages
- **Question**: What do name field errors say?
- **Decision**: "Enter a name." / "Use at most {n} characters." / "Use a single line without control characters."
- **Alternatives**: —
- **Why**: Shared copy across all sessions.
- **Depends on**: —

### D15 — Saving and failures in dialogs
- **Question**: What happens while a write runs and when it fails?
- **Decision**: While saving, the button shows a spinner, fields and Cancel are disabled, and the dialog can't be dismissed. On a network error or 5xx, the dialog stays open with the input and an error toast shows: "Couldn't add the column. Check your connection and try again." / "Couldn't rename “{name}”. Check your connection and try again." / "Couldn't move “{name}”. Check your connection and try again." / "Couldn't delete “{name}”. Check your connection and try again." / "Couldn't add the card. Check your connection and try again." Server field errors show under the field. No success toast. A 404 on delete counts as deleted.
- **Alternatives**: Wait for designs.
- **Why**: Figma has no failure states for these actions.
- **Depends on**: —

### D16 — Refresh
- **Question**: How does the user refresh the board?
- **Decision**: Pull to refresh on every window size, no refresh button. Pulling any column's card list reloads the whole board. On failure, the board stays as it is and the toast "Couldn't refresh the board. Check your connection and try again." shows.
- **Alternatives**: Phone only; a refresh item in the board menu.
- **Why**: Figma's app-only note: pull to refresh instead of the refresh button.
- **Depends on**: —

### D19 — Unchanged rename
- **Question**: What happens when a rename is saved without a change?
- **Decision**: The dialog closes without a request.
- **Alternatives**: —
- **Why**: Shared rule across all sessions.
- **Depends on**: —

### D20 — Missing frames
- **Question**: What to build where Figma has no phone or tablet frame?
- **Decision**: Phone add column and rename column reuse the desktop pattern (inline add column field, rename dialog). Tablet portrait uses the desktop variant of each state where no tablet frame exists.
- **Alternatives**: Phone variants on tablet; wait for designs.
- **Why**: Only "Board · Tablet 834" exists. Add column and rename column are drawn for desktop only.
- **Depends on**: —

### D21 — Write timing
- **Question**: Which writes are optimistic?
- **Decision**: None in this change. Column create, rename, move and delete, and card create, wait for the server. The response is applied to the loaded board locally, with no full reload. Only card moves (later drag & drop change) are optimistic.
- **Alternatives**: Optimistic column moves.
- **Why**: User decision. IDs and card numbers come from the backend, and POSTs must not be retried automatically (`api-conventions` "Retries after a lost response").
- **Depends on**: —

### D22 — Quick add
- **Question**: How does quick add behave?
- **Decision**: Enter or "Add card" creates the card. The field stays open, empty and focused. Empty or whitespace-only input does nothing. Cancel or back discards without asking. A tap outside keeps the field open if it has text and closes it if it is empty. Only one quick add field is open at a time. The title is at most 200 characters. Amended on 2026-11-21: switching to another field while this one holds text asks first instead of dropping the text (D41).
- **Alternatives**: —
- **Why**: Figma annotation (marked "proposal"), confirmed as final.
- **Depends on**: —

### D23 — Empty column delete, stale data, missing board
- **Question**: What happens for delete of an empty column, stale moves, missing columns and a missing board?
- **Decision**: An empty column uses a confirm dialog: "Delete column “{name}”?" / "This can't be undone." / "Delete column". A stale move (`OUT_OF_RANGE`) or a column that no longer exists reloads the board and shows the toast "“{name}” no longer exists. The board was reloaded." A board that no longer exists (404) goes back to the board list with the toast "This board no longer exists."
- **Alternatives**: Delete an empty column without a dialog; show the load error state for a 404.
- **Why**: Shared decision that all deletes confirm. `api-conventions` "Clients recover from a stale move".
- **Depends on**: D15

### D24 — Phone column scrolling
- **Question**: Free scroll or snap?
- **Decision**: Free horizontal scroll, no snapping.
- **Alternatives**: Snap to the next column start.
- **Why**: User decision.
- **Depends on**: —

### D30 — Settled from Figma
- **Question**: Confirm the parts Figma already answers.
- **Decision**: Column menu: Move left, Move right, Rename, Delete column. Move left is disabled on the first column, Move right on the last. Board ⋯ menu: Rename board, Export as JSON, Delete board. Loading, "Couldn’t load this board", new board with default columns ("No cards yet") and "This board has no columns" states as drawn. Tapping a card calls a callback that opens card detail. It does nothing until `mob-card-detail-ui`.
- **Alternatives**: —
- **Why**: Figma page 9:6. Confirmed via the coordinator.
- **Depends on**: —

### D31 — Search and filter controls
- **Question**: Does this change show the search and label filter controls drawn in the top bar?
- **Decision**: No. They arrive with the search & filter change.
- **Alternatives**: —
- **Why**: Follows from D1.
- **Depends on**: D1

The follow-up answers below (D32–D40) were confirmed by the user via the coordinator on 2026-11-21.

### D32 — Add column after success (MBV-17)
- **Question**: What happens to the add column field after a successful add?
- **Decision**: The field closes and the strip scrolls to the new column. In the "This board has no columns" state, "Add column" opens the same field in place of the empty state.
- **Alternatives**: The field stays open and empty for the next column.
- **Why**: User decision.
- **Depends on**: D20, D21

### D33 — Quick add validation display (MBV-18)
- **Question**: How does quick add show a too-long or control-character title, given Figma has no error slot?
- **Decision**: The D13 messages show under the quick add field, after the first submit and then live (D12).
- **Alternatives**: Stop input at 200 code points and turn pasted line breaks and tabs into spaces.
- **Why**: One rule everywhere. The app never changes the user's text silently.
- **Depends on**: D12, D13, D22

### D34 — After deleting the board from the board view (MBV-19)
- **Question**: What happens after "Delete board" from the board view menu succeeds?
- **Decision**: The app goes back to the board list, with no toast. The board list's own delete flow shows none either.
- **Alternatives**: Back with a toast.
- **Why**: Same result as deleting from the board list.
- **Depends on**: D5

### D35 — Shared name rule (MBV-20)
- **Question**: Who builds the shared text rule and the D13 strings?
- **Decision**: The board list change puts the shared name rule and its strings in core. The board view reuses them.
- **Alternatives**: The board view builds it.
- **Why**: Board list lands first (D5).
- **Depends on**: D5, D13

### D36 — Inline fields while saving (MBV-21)
- **Question**: How do quick add and add column behave while their request runs?
- **Decision**: D15's saving state applies to them: spinner on the Add button, field and Cancel disabled, Enter ignored. On failure the text stays and the error toast shows.
- **Alternatives**: —
- **Why**: Same behavior as the dialogs.
- **Depends on**: D15

### D37 — Pull to refresh with an open field (MBV-22)
- **Question**: What happens when the user refreshes while an inline field holds text?
- **Decision**: The refresh runs. Open fields keep their text. A field whose column no longer exists after the reload closes.
- **Alternatives**: Disable pull to refresh while a field is open.
- **Why**: User decision.
- **Depends on**: D16

### D38 — Accessibility labels (MBV-23)
- **Question**: Which accessibility labels do the controls get? Figma has none.
- **Decision**: "Back", "Board actions", "Actions for column “{name}”", "Add card to “{name}”", "Add column". A card tile reads "{displayId}, {title}, labels {a}, {b}". Other labels follow the same style.
- **Alternatives**: —
- **Why**: Accepted as proposed. CLAUDE.md requires labels from string resources.
- **Depends on**: —

### D39 — Header while loading, on error and with no cards
- **Question**: What does the header show in the loading and error states, and for a board with 0 cards?
- **Decision**: With 0 cards, the header shows only the name and key, with no count. While loading and on a load error, the header uses the board list's data for this board if it is available. Otherwise it shows no title. The board ⋯ button is hidden until the board has loaded.
- **Alternatives**: —
- **Why**: User decision.
- **Depends on**: D9

### D40 — Stale move copy (amends D23)
- **Question**: What does the toast say when a column move is answered with `OUT_OF_RANGE`?
- **Decision**: "Couldn't move “{name}”. The board changed on another device, so it was reloaded." A column that no longer exists keeps D23's "“{name}” no longer exists. The board was reloaded."
- **Alternatives**: D23's text for both.
- **Why**: A stale move doesn't mean the column is gone.
- **Depends on**: D23

### D41 — Switching inline fields with text (MBV-24)
- **Question**: What happens when the user opens another inline field while the open one holds text?
- **Decision**: A discard dialog that reuses the Card detail "Unsaved changes" pattern: "Discard changes?" / "The card you started in “{columnA}” hasn’t been added." (for an add column field: "The column you started hasn’t been added.") / "Keep editing" (secondary) / "Discard" (danger). "Keep editing" closes the dialog. The new field doesn't open, and the open field keeps its text, scrolls back into view and gets focus. "Discard" drops the text, closes the open field and opens the new one with focus. Back or a tap outside the dialog counts as "Keep editing". Every switch between inline fields asks: quick add → quick add, quick add → "Add column", and "Add column" with text → quick add. An empty field closes silently. Explicit Cancel/back in the open field still drops the text without asking (D22). Menus and dialogs don't close an open field, so they don't ask. While the open field is saving, opening another inline field does nothing.
- **Alternatives**: Copy that quotes the typed title; card detail copy as is; asking only for quick add → quick add.
- **Why**: The user doesn't want typed text dropped silently. Confirmed by the user via the coordinator on 2026-11-21, the same for web.
- **Depends on**: D22, D32, D36

### D42 — Tap outside the add column field
- **Question**: What does a tap outside the add column field do? Figma and D22 only cover quick add.
- **Decision**: The same as quick add (D22): a tap outside closes the field when it is empty and keeps it open with its text when it holds text. No discard dialog.
- **Alternatives**: Keep the field open on any outside tap; only Cancel, back and Escape close it.
- **Why**: User decision on 2026-11-23. Both inline fields behave the same.
- **Depends on**: D22, D32

### D43 — "No cards yet" while quick add is open
- **Question**: Does an empty column show "No cards yet" while its quick add field is open?
- **Decision**: No. The quick add field takes its place. "No cards yet" shows again when the field closes.
- **Alternatives**: Show both.
- **Why**: User decision on 2026-11-23. Figma doesn't draw this combination.
- **Depends on**: D22, D30

### D44 — Discard dialog after process death
- **Question**: Is an open "Discard changes?" dialog restored after process death?
- **Decision**: No. The open inline field comes back with its text; the pending switch is dropped. The user taps the other field again if they still want to switch.
- **Alternatives**: Restore the dialog with its pending target.
- **Why**: User decision on 2026-11-23.
- **Depends on**: D41

## Board list hooks (task 1.1, 2026-11-23)

The board list change (`add-board-list-ui`) is merged into the base branch. The board view uses these parts of it, as they exist in the code:

- **Rename and delete board dialogs (D5, D34):** the board ⋯ menu calls `NavController.navigateToRenameBoard(boardId)` and `navigateToDeleteBoard(boardId)` from `BoardsGraph.kt`. The dialogs report back to the entry that opened them through its saved state, read with `ObserveBoardDialogResults(entry, onRenamed, onDeleted)`. The board view's destination calls it with its own back stack entry: `onRenamed(newName)` updates the header, `onDeleted()` leads to `onBoardDeleted` (back to the board list, no toast). The dialogs update the board list store themselves.
- **Open board (D39):** `boardsGraph(onOpenBoard = (boardId, name, key) -> Unit)` already passes the board's name and key. `:composeApp` maps it to `BoardRoute(boardId, name, key)`, so the header can show them while the board loads.
- **Missing board (D23, review D2):** `onBoardMissing` calls `BoardListReloader.reload()` and pops back to `BoardListRoute`. The reload drops the board from the list and the sidebar. A failed reload shows `boardsReloadFailedToast()`, as every other caller does.
- **Shared name rule and strings (D35):** `normalizeText`, `validateName` and `codePointLength` in `core/domain/.../text/TextRules.kt`, `NameError` and `NameError.toUiText()` in `:core:presentation` (with the D13 strings). Column names use `validateName(normalized, 30)`, card titles `validateName(normalized, 200)`. Plurals use `UiText.PluralResource`.
- **App shell:** `AppShell` draws the sidebar and phone menu. The board view draws only its content area and handles window insets like the board list (safeDrawing, including the display cutout).

## Facts

- The shared specs are the OpenSpec store `cardigan-shared-specs` at `~/openspec/cardigan-shared-specs`. `~/cardigan-specs` is an old layout and is not used. — source: `openspec store list`
- Column names are 1–30, single line. Columns have dense integer positions. Deleting a column deletes its cards. — source: cardigan-shared-specs `domain-column`, `domain-ordering`
- Card titles are 1–200, single line. Cards get a backend-assigned number. The display ID is `{boardKey}-{number}`. Labels on a card show sorted by case-folded name. — source: cardigan-shared-specs `domain-card`
- Labels belong to the user, not the board. — source: cardigan-shared-specs `domain-label`
- A move sends the final index. `OUT_OF_RANGE` on a move means stale data: reload and tell the user. A column move returns all columns. — source: cardigan-shared-specs `api-conventions`
- No automatic retry of a POST after an unknown outcome. A repeated DELETE that gets 404 counts as deleted. — source: cardigan-shared-specs `api-conventions` "Retries after a lost response"
- Text is normalized (line breaks, NFC, trim), counted in code points, and control characters are rejected in single-line fields. Clients use the same rule. — source: cardigan-shared-specs `text-length`
- `be-boards` is archived (`2026-11-20-be-boards`), and the backend's OpenAPI snapshot has boards and columns next to auth, labels and `users/me`: `GET /boards/{id}` with embedded columns, `POST /boards/{id}/columns` (adds at the end), `PATCH …/columns/{id}`, `POST …/columns/{id}/move` (returns all columns), `DELETE …/columns/{id}`. No `cardCount` yet. Cards (`be-cards`) are not planned yet. `api/openapi.json` hasn't been synced into this repo yet. — source: backend `openspec/changes/archive/2026-11-20-be-boards/`, `app/src/test/resources/openapi/openapi.json` (checked 2026-11-22)
- Figma page 9:6 has phone and desktop frames for every state and a tablet frame only for the main board. The phone annotation: columns are 300 wide, swipe sideways. — source: Figma `MALwbOph7Uj2QtiTy21JaQ` page 9:6
- `:core:designsystem` already has `BoardColumn`, `BoardCard`, `AddCardButton`, `AddColumnButton`, `Menu`, `CardiganDialog`, `RefreshBox`, `Skeleton`, `EmptyState`, `LabelChip` and the window classes. `:core:presentation` has `ToastController`. — source: `core/designsystem`, `core/presentation`
- Transfer exposes `ExportBoardRoute(boardId, boardName)` as a dialog destination. — source: `feature/transfer/presentation/.../TransferRoutes.kt`
- The mobile auth change provides `SessionManager` in `:core:domain` and `AppViewModel.onLogout()` in `:composeApp`. The board view doesn't call them. Logout lives in the board list's menu and sidebar. — source: coordinator, 2026-11-21
- Figma Card detail has a "Discard changes?" dialog: "Your changes to CRD-109 haven’t been saved." / "Keep editing" / "Discard". — source: Figma 9:7 "Unsaved changes · Phone 390" and "· Desktop 1440"

## Out of scope

- Card drag & drop, including its accessibility move actions (next change, `mob-card-dnd`).
- Search and label filter (later change, `mob-search-filter`).
- The sidebar and phone menu (board list, D3).
- The rename board and delete board dialogs themselves (board list, D5).
- Card detail, card edit and delete (`mob-card-detail-ui`).
- HTTP data sources and DTO mappers (`mob-boards-connect`, `mob-cards-connect`).

## Deferred

- (none)
