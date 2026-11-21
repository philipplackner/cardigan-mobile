# Decisions: add-board-list-ui

Confirmed with the user via the coordinator session (cardiganapp-18) on 2026-11-21. The questions (MBL-1 to MBL-16) were sent to that session. It merged them with the questions from the other five sessions (mobile and web × auth, board list, board view) and relayed the user's answers. The coordinator's decision IDs are given in brackets, e.g. [C-D3], so the other changes can be cross-checked. This is the same way add-transfer-ui and add-design-system recorded their decisions.

## Summary

Build the mobile board list for chunk `mob-board-list-ui` in the `boards` feature: the phone list and the tablet/desktop tile grid, loading, empty and error states, pull to refresh, and the New board, Rename board and Delete board dialogs. It also builds the app shell in `:composeApp` (tablet/desktop sidebar, phone ⋯ menu), the board repository interface with one in-memory board store shared by the list, the sidebar and transfer's key lookup, and the shared text rule for names. Everything runs against a repository interface tested with test doubles. A temporary in-memory fixture and a debug entry make it usable by hand until `mob-boards-connect`.

## Decisions

### D1 — App shell ownership [C-D3]
- **Question**: Who owns the tablet/desktop sidebar and the phone ⋯ menu?
- **Decision**: The app shell lives in `:composeApp`, built from the design-system `Sidebar`. The board list owns the sidebar's board section and the phone ⋯ menu (Import board, Labels, Log out). Auth only provides the logout action and the account email. Board view and labels render only their content area. Targets that aren't built yet (board view, labels, logout) are no-op callbacks with TODOs.
- **Alternatives**: The boards feature owns the sidebar and other screens embed it; each feature draws its own sidebar.
- **Why**: Features can't depend on each other, and the sidebar shows on every top-level screen. SHARED with web (same structure).
- **Depends on**: —

### D2 — One in-memory board store [C-D5]
- **Question**: How is board data shared between the list, the sidebar, the board view and transfer's key lookup?
- **Decision**: The board list owns one board repository: a Koin `single` in-memory store that exposes the board list as a `StateFlow`. Mutations update it, and `refresh()` reloads it. The board list also owns reusable Rename board and Delete board dialogs, which the board view reuses from its ⋯ menu. The board view adds its own repository in the same module and rebases onto this change.
- **Alternatives**: Each screen loads on its own and keeps the result in its ViewModel.
- **Why**: The sidebar and the transfer lookup need the same list without extra requests.
- **Depends on**: D1

### D3 — Test doubles and a temporary fixture [C-D6]
- **Question**: What stands in for the backend in this chunk?
- **Decision**: Interfaces plus test doubles in tests. Also a temporary in-memory fixture data source and a debug entry for manual checks. Both are removed in their own commit before the change is archived.
- **Alternatives**: Test doubles only.
- **Why**: The backend has no board endpoints yet, and the screens should be checked by hand on both platforms.
- **Depends on**: —

### D4 — List item model and no column count [C-D8]
- **Question**: What does a board list item carry? The delete dialog in Figma names the column count.
- **Decision**: Keep core `Board(id, name, key)` as it is. Add `BoardSummary` in `boards:domain` with id, name, key, cardCount, createdAt and updatedAt. No column count. The delete dialog drops the column count: "Its 12 cards will be deleted too. This can't be undone."
- **Alternatives**: Extend core `Board`; add columnCount to the summary (my recommendation, not chosen).
- **Why**: User choice. It keeps the list response small, and the backend needs no column count.
- **Depends on**: —

### D5 — Count plurals and delete body [C-D9]
- **Question**: How are counts written, and what does the delete body say with 0 cards?
- **Decision**: "1 card" / "N cards", "1 board" / "N boards", "0 boards". With 0 cards, the delete body is only "This can't be undone."
- **Alternatives**: Always show "0 cards".
- **Why**: SHARED with web.
- **Depends on**: D4

### D6 — Relative updated time [C-D10]
- **Question**: How is the updated time written on rows and tiles?
- **Decision**: In the device time zone: under 1 minute "Just now", under 1 hour "N minutes ago", same calendar day "N hours ago", the previous day "Yesterday", the same year "Oct 9", older "Oct 9, 2025". Tiles prefix it with "Updated" as in Figma. The text doesn't tick live. It is computed when the list state is built.
- **Alternatives**: Another scale, such as "N days ago" up to a week.
- **Why**: SHARED with web, which uses the same thresholds.
- **Depends on**: —

### D7 — Key prefill rule [C-D11]
- **Question**: How is the key prefilled from the name?
- **Decision**: Take the name, remove diacritics, uppercase it, keep only `A`–`Z`, and take the first 4 letters ("Garden irrigation" → `GARD`, "Ärger" → `ARGE`). The prefill is empty if fewer than 2 letters remain. Prefill stops once the user edits the key and doesn't come back.
- **Alternatives**: Initials of the words; the first 2–5 letters without removing diacritics.
- **Why**: SHARED with web, which must give the same result.
- **Depends on**: —

### D8 — When field errors show [C-D12]
- **Question**: When do the name and key errors show, and when is the primary button disabled?
- **Decision**: Errors show on the first submit and update live after that. The primary button is disabled only while an error is shown or a save is running.
- **Alternatives**: Button disabled until valid and errors live from the first edit (my recommendation, not chosen); errors only on submit.
- **Why**: User choice. SHARED with web.
- **Depends on**: —

### D9 — Name error copy [C-D13]
- **Question**: What do the name errors say? Figma has no copy.
- **Decision**: `REQUIRED`: "Enter a name." `TOO_LONG`: "Use at most {n} characters." `INVALID_CHARACTER`: "Use a single line without control characters." The same strings serve board, column and label names, so they live in core.
- **Alternatives**: —
- **Why**: SHARED with web and with the board view and labels changes.
- **Depends on**: —

### D10 — After Create board [C-D14]
- **Question**: What happens after a board is created?
- **Decision**: The dialog closes and the new board opens. Until the board view exists, this is a callback wired as a no-op with a TODO in `:composeApp`.
- **Alternatives**: Stay on the list, with or without a toast.
- **Why**: SHARED with web.
- **Depends on**: D1

### D11 — Saving, failures and missing boards [C-D15]
- **Question**: What do the dialogs do while saving and on failure?
- **Decision**:
  - While saving: the primary button shows its spinner, the fields and Cancel are disabled, and the dialog can't be dismissed.
  - Network, 5xx or unknown failure: the dialog stays open with its input and an error toast shows: "Couldn't create the board. Check your connection and try again." / "Couldn't rename “X”. Check your connection and try again." / "Couldn't delete “X”. Check your connection and try again."
  - Server field errors show under their field.
  - No success toast.
  - Rename 404: the dialog closes, the board is removed from the list, and the toast "“X” no longer exists." shows.
  - Delete 404: treated as deleted.
- **Alternatives**: An inline Banner in the dialog; success toasts.
- **Why**: Reuses the app-wide toast host. Delete 404 follows `api-conventions` "Retries after a lost response". SHARED with web and the board view.
- **Depends on**: D2

### D12 — Refresh control and failed refresh [C-D16]
- **Question**: How does the list refresh in the app, and what happens when a refresh fails while a list is shown?
- **Decision**: Pull to refresh on every size, with no refresh button anywhere in the app. A failed refresh keeps the shown list, stops the indicator and shows the toast "Couldn't refresh your boards. Check your connection and try again."
- **Alternatives**: The refresh button on tablet/desktop; the full error state on a failed refresh.
- **Why**: DECISIONS.md: "pull to refresh instead of the refresh button". SHARED with the board view and labels.
- **Depends on**: —

### D13 — List order [C-D17, MBL-17]
- **Question**: In what order are boards listed?
- **Decision**: The server sorts per cardigan-shared-specs `api-conventions` "Board list order": updatedAt desc, then createdAt desc, then id asc. The client keeps the server's order, and the sidebar uses the same order. After a local mutation, the store places the board by the same rule, using the times from the response.
- **Alternatives**: Ties by name (the coordinator's first wording, withdrawn after MBL-17); sort on the client.
- **Why**: The shared spec wins. One order everywhere, set by the backend.
- **Depends on**: D2

### D14 — Local key clash check [C-D18]
- **Question**: Does the New board dialog check for a used key before sending?
- **Decision**: Yes, against the loaded boards: "You already have a board with the key X." The server's 409 `ALREADY_USED` is the fallback and shows the same text under the field.
- **Alternatives**: Server check only.
- **Why**: Instant feedback. Uses the store from D2.
- **Depends on**: D2

### D15 — Unchanged rename [C-D19]
- **Question**: What does Save do when the normalized name hasn't changed?
- **Decision**: The dialog closes without a request.
- **Alternatives**: Send it anyway; disable Save.
- **Why**: An unchanged name would only bump the updated time.
- **Depends on**: —

### D16 — Undesigned dialog sizes [C-D20]
- **Question**: Rename and "key already used" are only drawn at Desktop 1440. How do they look on phones and tablets?
- **Decision**: On phones they reuse the New board · Phone 390 dialog layout. On tablets, the desktop variant is used wherever no tablet frame exists.
- **Alternatives**: Wait for designs.
- **Why**: Low risk. Same components.
- **Depends on**: —

### D17 — Log out from the menu [C-D28]
- **Question**: Does Log out in the menu or sidebar ask for confirmation?
- **Decision**: No. It calls the logout action right away (a no-op callback until auth lands).
- **Alternatives**: A confirm dialog.
- **Why**: SHARED with web and auth.
- **Depends on**: D1

### D18 — Account email in menu and sidebar [C-D34]
- **Question**: Show "Signed in as {email}" at the top of the phone menu (Figma: "Proposal") and the email in the sidebar?
- **Decision**: Yes, both. The email comes from the session that auth exposes, not from `GET /users/me`.
- **Alternatives**: Leave it out; load it from `/users/me`.
- **Why**: There is no settings screen, so this is the only place that shows the account. AFFECTS auth.
- **Depends on**: D1

### D19 — Grid columns between drawn sizes [MBL-18]
- **Question**: How many tile columns show between the drawn widths (834 and 1440 dp)?
- **Decision**: An adaptive grid with the Board tile's minimum width (about 260 dp, one constant). It gives 2 columns at 834 and 4 at 1440. Web does the same.
- **Alternatives**: Fixed columns per window class.
- **Why**: Tablet landscape uses the desktop layout at widths that aren't drawn.
- **Depends on**: —

### D20 — Session wiring [MBL-19]
- **Question**: Where do the account email and the logout action come from?
- **Decision**: From mobile-auth's add-auth-ui: `SessionManager` in `:core:domain` (`state: StateFlow<SessionState>` with Restoring / SignedIn(email) / SignedOut(NoSession | LoggedOut | Expired), `restore()`, `logout()`) and `AppViewModel.onLogout()` in `:composeApp`. The shell keeps plain parameters (`accountEmail`, `onLogOut`), and `:composeApp` feeds them from `SessionState.SignedIn.email` and `AppViewModel.onLogout()`. The email is persisted next to the refresh token, so it is always known while signed in. add-auth-ui's session gate has a temporary `AppStartRoute`, which the board list replaces. Whichever of the two changes lands second does the wiring.
- **Alternatives**: Load the email from `GET /users/me`.
- **Why**: Auth owns the session (D1, D18).
- **Depends on**: D1, D18

### D21 — Dialog results for the caller [coordinator, board view]
- **Question**: What do the Rename and Delete dialogs tell the screen that opened them?
- **Decision**: Rename returns `Renamed(newName)`, Delete returns `Deleted`. Cancel and the rename-404 close return no result (the board is gone from the store either way). After a delete from the board view ⋯ menu, the board view goes back to the board list with no toast. That is the board view's handling, and this change only provides the results.
- **Alternatives**: The caller watches the store.
- **Why**: mobile-boardview depends on it.
- **Depends on**: D2

### D22 — Accessibility label style [coordinator]
- **Question**: How are accessible names worded?
- **Decision**: The shared style: "Back", "Board actions", "Actions for column “{name}”", "Add card to “{name}”", "Add column". This change uses: "Actions for board “{name}”" (row/tile ⋯), "Menu" (phone bar ⋯), "New board", "Import board", "Log out", "Labels", "All boards". A row or tile reads "{name}, {key}, {cards}, updated {time}".
- **Alternatives**: —
- **Why**: Same style across features and clients.
- **Depends on**: —

## Facts

- The backend spec snapshot has only `/api/v1/labels`, `/api/v1/labels/{labelId}` and `/api/v1/users/me`. There are no board endpoints and no `api/openapi.json` in this repo yet — source: `cardigan.backend.dir`/app/src/test/resources/openapi/openapi.json.
- Board name: required, single line, 1–100 code points after normalization. Not unique — source: cardigan-shared-specs `domain-board` "Board name", `text-length`.
- Board key: 2–5 letters `A`–`Z`, lowercase converted to uppercase, unique per user, never changes — source: `domain-board`.
- Text normalization: CRLF/CR → LF, NFC, trim Unicode White_Space. Length counts code points. Control characters U+0000–U+001F and unpaired surrogates are invalid. Clients must apply the same rule before sending — source: `text-length`.
- Field error codes and order: `REQUIRED`, `INVALID_CHARACTER`, `TOO_SHORT`, `TOO_LONG`, `INVALID_FORMAT`, …, `ALREADY_USED`. A used key is 409 `CONFLICT` with field `key` `ALREADY_USED`. An unknown field code shows the general invalid-value message — source: `api-errors`.
- Board list order: updated time desc, then created time desc, then id ascending. All boards in one response, no paging — source: `api-conventions` "Board list order", "Lists are complete and unpaginated".
- Create returns 201 with the full resource, rename (PATCH) 200, delete 204. A repeated delete that gets 404 counts as deleted. No automatic repeat of a POST after a lost response — source: `api-conventions`.
- Deleting a board deletes its columns and cards, not labels — source: `domain-board`.
- Figma 9:5 frames: phone list, tablet grid (2 columns at 834), desktop grid (4 columns at 1440), loading skeletons, "No boards yet", "Couldn't load your boards", pull to refresh ("Refreshing"), phone large title that collapses into the bar, list end with the board count, phone ⋯ menu, tile and row menus (Rename, Delete board), New board (desktop, phone), key already used (desktop), Rename (desktop), Delete (desktop, phone) — source: Figma 9:5 via `use_figma`.
- Figma annotations: "The menu holds Import board, Labels and Log out. In the app, pull down to refresh instead of the refresh button." / "Key is prefilled from the name and can be edited before creating." / "The list ends with the board count; no paging, all boards load at once." / "Once the large title scrolls under the bar, “Boards” moves into the bar and a divider appears. The actions stay in place." — source: Figma 9:5 node annotations.
- Copy from Figma: "Boards", "No boards yet", "Create a board to start adding cards, or import a board from a JSON export.", "New board", "Import", "Import board", "Labels", "Log out", "All boards", "Couldn’t load your boards", "Check your connection and try again.", "Try again", "Refreshing", "Rename", "Delete board", "Name", "Key", "2–5 uppercase letters. Card IDs will look like {KEY}-1. The key can’t be changed later.", "New boards start with the columns To do, In progress and Done.", "Cancel", "Create board", "Rename board", "The key can’t be changed.", "Save", "Delete board “{name}”?", "This can’t be undone." — source: Figma 9:5.
- Already in the code: `Board`, `BoardKeyRule`, `BoardKeyError` in `:core:domain`. The key error strings and `boardKeyAlreadyUsedText` in `:core:presentation`. `ToastController` and the app toast host. AppNavHost with an empty `AppStartRoute` and no-op `onImported` / `onBoardListChanged`. `BoardKeyOwnerLookup { null }` in `appModule`. Design-system `Sidebar`, `NavItem`, `PageHeader`, `BrandAppBar`, `TitleAppBar`, `BoardTile`, `BoardRow`, `EmptyState`, `Skeleton`, `RefreshBox`, `CardiganDialog`, `Field`, `Input`, `Menu`, `CardiganButton(isLoading)` — source: codebase.
- No NFC/NFD normalization exists in the codebase. Common Kotlin has none, so it needs `expect`/`actual` on `java.text.Normalizer` and `NSString` — source: codebase search.

## Out of scope

- Board view content, column and card work (`mob-board-view-ui`).
- Import/export flows themselves (`add-transfer-ui`). This change only adds the entry points and the reload hook.
- Real HTTP data sources and DTO mapping (`mob-boards-connect`).
- The labels page and the logout implementation. They are no-op callbacks here.
- Rate limit (429), offline and network-error designs beyond the toasts in D11 and D12.

## Deferred

None. The grid width (D19) and the session wiring (D20) were settled after the first round.
