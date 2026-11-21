# Proposal

## Why

The app has no home screen yet. `App()` starts on an empty route, and import has no entry point. Chunk `mob-board-list-ui` builds the board list, the app shell around it and the board repository interface now, against test doubles. Then `mob-boards-connect` only has to add the HTTP data source once the backend's board endpoints exist.

## What Changes

- **Board list screen** (`boards` feature), the app's start destination:
  - Phone: a list of board rows under a large "Boards" title with the board count. The title collapses into the bar on scroll. The list ends with the board count.
  - Tablet and desktop: a page header with the count and an Import button, above a grid of board tiles.
  - Each item shows name, key, card count ("1 card" / "N cards") and the relative updated time (D6). Tapping an item opens the board, passing its id, name and key (D23).
  - States: loading skeletons, "No boards yet" (phone: New board and Import buttons), "Couldn't load your boards" with Try again.
  - Pull to refresh on every size. No refresh button (D12). A failed refresh keeps the list and shows an error toast.
- **New board dialog**: name and key fields. The key is prefilled from the name until the user edits it (D7). Local key and name checks, including a clash with a loaded board (D14). Errors show on the first submit and then live (D8). On success it closes and opens the new board (D10).
- **Rename board and Delete board dialogs**, from the row/tile ⋯ menu. Built so the board view can reuse them, and they report `Renamed(newName)` / `Deleted` to the caller (D21). Unchanged rename closes without a request (D15). The delete body names the card count only (D4, D5).
- **Saving and failures** (D11): spinner on the primary button, inputs and Cancel disabled, no dismiss. Error toasts for network and server failures. Server field errors under the field. A rename of a missing board closes and removes it. A delete of a missing board counts as done.
- **App shell in `:composeApp`** (D1):
  - Tablet/desktop: the design-system Sidebar with All boards, Labels, the board list section, New board, and the account email with Log out.
  - Phone: the ⋯ menu with "Signed in as {email}" (D18), Import board, Labels and Log out.
  - The email and Log out come from add-auth-ui's `SessionManager` and `AppViewModel.onLogout()` (D20). Labels and opening a board are no-op callbacks with TODOs until their chunks land. Log out has no confirmation (D17).
- **Board store** (D2): one in-memory board repository that exposes the loaded boards to the list, the sidebar and transfer's key lookup. It replaces the placeholders that add-transfer-ui left in `:composeApp`: the import reload hook, the start route, and `BoardKeyOwnerLookup { null }`.
- **Shared text rule** in core (D9): name normalization and validation per `text-length`, with the shared name error strings for board, column and label names. Platform NFC/NFD sits behind `expect`/`actual`.
- **Temporary fixture** (D3): an in-memory fixture data source and a debug entry for manual checks. Removed in its own commit before archive.

### Copy

From Figma (page 9:5), unchanged: "Boards", "{n} boards", "{n} cards", "Updated {time}", "No boards yet", "Create a board to start adding cards, or import a board from a JSON export.", "New board", "Import", "Import board", "All boards", "Labels", "Log out", "Couldn’t load your boards", "Check your connection and try again.", "Try again", "Refreshing", "Rename", "Delete board", "Name", "Key", "2–5 uppercase letters. Card IDs will look like {KEY}-1. The key can’t be changed later.", "New boards start with the columns To do, In progress and Done.", "Cancel", "Create board", "Rename board", "The key can’t be changed.", "Save", "Delete board “{name}”?", "Signed in as {email}".

Confirmed via the coordinator (shared with web):
- Plurals: "1 board" / "{n} boards", "1 card" / "{n} cards" (D5).
- Delete body: "Its {n} cards will be deleted too. This can’t be undone." With 1 card: "Its 1 card will be deleted too. This can’t be undone." With 0 cards: "This can’t be undone." (D4, D5).
- Relative time: "Just now", "{n} minutes ago", "{n} hours ago", "Yesterday", "Oct 9", "Oct 9, 2025" (D6). The singular forms "1 minute ago" and "1 hour ago" follow D5's plural rule.
- Name errors: "Enter a name." / "Use at most {n} characters." / "Use a single line without control characters." (D9).
- Toasts: "Couldn't create the board. Check your connection and try again." / "Couldn't rename “{name}”. Check your connection and try again." / "Couldn't delete “{name}”. Check your connection and try again." / "“{name}” no longer exists." / "Couldn't refresh your boards. Check your connection and try again." (D11, D12).
- Key errors reuse the strings in `:core:presentation` from add-transfer-ui, including "You already have a board with the key {key}." An unknown name field error code shows "Use a single line without control characters." as the general invalid-value message (`api-errors`).

## Capabilities

### New Capabilities
- `board-list`: how the Android and iOS app shows the user's boards and manages them: phone list and tablet/desktop grid, loading/empty/error states, pull to refresh, the New board, Rename board and Delete board dialogs, and the app shell's board section and phone menu.

### Modified Capabilities
- (none) `design-system` keeps its requirements. The components already exist.

### Shared-spec requirements

This change implements or depends on:
- cardigan-shared-specs `domain-board`:
  - "Board name": 1–100, single line, not unique.
  - "Board key format": 2–5 letters A–Z, lowercase converted.
  - "Board key is unique among a user's existing boards": the local clash check and 409 handling.
  - "Board key never changes": the key field in Rename is read-only.
  - "Board timestamps": the updated time shown in the list.
  - "Deleting a board removes its content": the delete dialog body.
- cardigan-shared-specs `text-length`:
  - "Text is normalized before validation".
  - "Length is counted in Unicode code points".
  - "Invalid characters are rejected".
  - "Backend stores and returns normalized text": the list shows the returned name.
  - "Clients validate with the same rule".
- cardigan-shared-specs `api-conventions`:
  - "Board list order": the client keeps the server's order.
  - "Lists are complete and unpaginated": all boards load at once, and the list ends with the count.
  - "Updates are partial": rename sends only the name.
  - "Success status codes and bodies".
  - "Deleting a missing resource is not found" and "Retries after a lost response": a delete 404 counts as deleted, and a create is never repeated automatically.
  - "UUIDs are lowercase canonical strings".
- cardigan-shared-specs `api-errors`:
  - "Field errors identify the field and rule".
  - "Field error codes".
  - "One field error per field": local checks use the same order.
  - "Limit errors carry the limit": `{n}` in "Use at most {n} characters.".
  - "Uniqueness clashes are conflicts".
  - "Other users' data is not found, never forbidden".
  - "Clients handle unknown error codes by status".
  - "Error text is for developers only".

## Impact

- **New code**:
  - `feature/boards/{domain,data,presentation}`.
  - `:core:domain`: the text rule and `expect`/`actual` normalization, with `androidMain` and `iosMain` actuals.
  - `:core:presentation`: the name error strings and plural/relative-time strings.
  - `:composeApp`: the app shell, the start route, and the wiring for transfer and boards.
- **`:composeApp` placeholders replaced**: `AppStartRoute`, the no-op `onImported` / `onBoardListChanged`, and `BoardKeyOwnerLookup { null }`.
- **Dependencies**: none new. kotlinx-datetime is already in the stack table. Check whether a module needs it added to its build file.
- **Coordination**:
  - The board view (`mob-board-view-ui`) rebases on this change, reuses the Rename/Delete dialogs and adds its repository in `boards`.
  - Auth (add-auth-ui) provides `SessionManager` and `AppViewModel.onLogout()`. Its session gate holds a temporary `AppStartRoute` that the board list replaces. Whichever change lands second does the wiring (D20).
  - Board view and labels should reuse the core name rule and strings.
  - be-boards: the list response must include `createdAt` and `updatedAt`, sorted per `api-conventions` "Board list order" (D13). `cardCount` comes later from be-cards (design.md Open Questions).
- **Figma**: no changes.
- **Tests**: unit tests for the text rule, key prefill, relative time, the board store, and the ViewModels. `*UiTest`s for the list states, dialogs and shell, with fakes. Run on Android host and the iOS simulator.
