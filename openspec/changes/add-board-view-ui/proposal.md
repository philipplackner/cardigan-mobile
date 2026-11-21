# Proposal

## Why

The board view is where the work happens, and card detail, card drag & drop, search & filter and export all hook into it. Chunk `mob-board-view-ui` builds it now against repository interfaces, so those changes can build on it and `mob-boards-connect` only has to add the HTTP calls once `be-boards` and `be-cards` exist (D1, D6).

## What Changes

- **Board view screen** in `:feature:boards:presentation`, content area only (the sidebar and phone menu come from the board list change, D3):
  - Header: board name, key and card count ("CRD · 12 cards" on phone, name + key tag + count on tablet/desktop). With 0 cards, no count. While loading or after a load error, the header uses the board list's name and key if it has them. The board ⋯ button shows only once the board has loaded (D39). Phone has a back button.
  - Columns in position order, each with name, card count, ⋯ menu, card tiles (display ID, title, label chips) and "Add card".
  - Phone: 300-wide columns in a free horizontal scroll, the next column peeking in (D24). Tablet and desktop: the desktop layout (D20).
  - "Add column" at the end of the strip, opening an inline name field. The field closes after a successful add and the strip scrolls to the new column. On a board without columns, the field replaces the empty state (D32).
  - States: loading skeleton, "Couldn’t load this board" with "Try again", "No cards yet" in an empty column, "This board has no columns" with "Add column".
  - Pull to refresh on every column's card list reloads the whole board (D16).
- **Quick add card** at the bottom of each column, title only, with name errors under the field (D22, D33). Switching to another field while this one holds text asks before dropping the text. The dialog is still being settled (MBV-24).
- **Column menu**: Move left, Move right (disabled at the ends), Rename (dialog), Delete column (confirm dialog that names the card count, or the empty-column variant) (D23, D30).
- **Board ⋯ menu**: Rename board and Delete board open the board list change's reusable dialogs (D5). After a delete, the app goes back to the board list without a toast (D34). Export as JSON opens transfer's export flow.
- **Writes wait for the server** and apply the response locally (D21). Failures follow D15 and D23: error toasts, inputs kept, stale data reloads the board, a missing board goes back to the board list.
- **Card tap** calls an `onCardClick` callback. It does nothing until `mob-card-detail-ui` (D30).
- **`:core:domain`**: `Card`, `Column`, the board content model, a minimal `Label` and the read-only `LabelSource` interface (D7).
- **`:feature:boards:domain`**: the board-detail repository (load board, column create/rename/move/delete) and the card create repository, with their error types.
- **Temporary**: an in-memory fixture repository and a debug entry for manual checks, removed in their own commit before archive (D6).
- Search and the label filter controls are not shown yet (D31).

### Copy

From Figma, unchanged: "Add card", "Add column", "Cancel", "Move left", "Move right", "Rename", "Delete column", "Rename column", "Name", "Save", "Delete column “{name}”?", "Its {count} cards will be deleted too. This can’t be undone.", "Delete column and {count} cards", "Rename board", "Export as JSON", "Delete board", "No cards yet", "This board has no columns", "Add a column to start adding cards.", "Couldn’t load this board", "Check your connection and try again.", "Try again", "{count} cards" (plurals, D9).

Confirmed via the coordinator (D13, D15, D16, D23):
- "Enter a name." / "Use at most {n} characters." / "Use a single line without control characters."
- "Couldn't add the column. Check your connection and try again."
- "Couldn't rename “{name}”. Check your connection and try again."
- "Couldn't move “{name}”. Check your connection and try again."
- "Couldn't delete “{name}”. Check your connection and try again."
- "Couldn't add the card. Check your connection and try again."
- "Couldn't refresh the board. Check your connection and try again."
- "Delete column “{name}”?" / "This can't be undone." / "Delete column" (empty column)
- "“{name}” no longer exists. The board was reloaded."
- "Couldn't move “{name}”. The board changed on another device, so it was reloaded." (D40)
- "This board no longer exists."

Accessibility labels, accepted via the coordinator (D38): "Back", "Board actions", "Actions for column “{name}”", "Add card to “{name}”", "Add column", and a card tile reads "{displayId}, {title}, labels {a}, {b}".

Pending (MBV-24): the discard dialog when switching away from an inline field with text.

## Capabilities

### New Capabilities
- `board-view`: how the app shows one board on Android and iOS and lets the user add cards and manage columns: layouts per window class, loading and error states, pull to refresh, quick add, the column and board menus, and how writes, failures and stale data behave.

### Modified Capabilities
- (none) `design-system` keeps its requirements. The board view uses existing components.

### Shared-spec requirements

This change implements or depends on:
- cardigan-shared-specs `domain-board`: "Board name" and "Board key format" (shown in the header), "Deleting a board removes its content" (through the board list dialogs).
- cardigan-shared-specs `domain-column`: "Column name", "A column belongs to exactly one board" (a board can have zero columns), "Deleting a column removes its cards".
- cardigan-shared-specs `domain-card`: "Card title", "Card display ID", "Card labels are a set sorted by name", "Card number is unique per board and never reused" (quick add waits for the number).
- cardigan-shared-specs `domain-label`: "Labels are shared across a user's boards", "Label color is a palette name".
- cardigan-shared-specs `domain-ordering`: "Columns have dense positions in their board", "Cards have dense positions in their column", "Order is defined only by positions", "Concurrent changes resolve as last write wins".
- cardigan-shared-specs `api-conventions`: "Move target is the final position", "Clients recover from a stale move", "Move responses contain all affected items", "Retries after a lost response", "Deleting a missing resource is not found".
- cardigan-shared-specs `api-errors`: "Field errors identify the field and rule", "Other users' data is not found, never forbidden", "Clients handle unknown error codes by status".
- cardigan-shared-specs `text-length`: "Text is normalized before validation", "Length is counted in Unicode code points", "Invalid characters are rejected", "Clients validate with the same rule".

## Impact

- **New code** in `feature/boards/{domain,data,presentation}` (board-detail package, next to the board list's), `:core:domain` (models, `LabelSource`) and `:composeApp` (board view route, callbacks to the board list, card detail and export).
- **Depends on the board list change** (`add-board-list-ui`): this change is rebased onto it and uses its `Board` repository, rename/delete board dialogs, sidebar and the shared name rule and strings in core (D3, D5, D35).
- **Transfer**: the "Export as JSON" item opens `ExportBoardRoute(boardId, boardName)`.
- **Labels**: until the labels change implements `LabelSource`, the fixture provides labels.
- **Dependencies**: none new.
- **Backend**: no endpoints for this yet. `be-boards` plans boards and columns. Cards are not planned yet. The repository interfaces follow the shared specs, and `mob-boards-connect` / `mob-cards-connect` map the real contract.
- **Tests**: unit tests for the ViewModel, validation and the local apply of responses. `*UiTest`s for the screen and Root + real ViewModel with fakes. Run on Android host and the iOS simulator.
