# Artifact review (light): add-board-view-ui

**Verdict:** Ready
**Reviewed:** 2026-11-21, fixes applied 2026-11-22, schema `careful-planning`, repo kind `code`

| Pass | Blocker | Major | Minor |
|---|---|---|---|
| Testability | 0 | 0 | 3 |
| Security | 0 | 0 | 0 |
| Hidden assumptions | 0 | 2 | 1 |
| Design contract | 1 | 1 | 3 |

## Findings

### D1: Failed POSTs don't reload before the user tries again
**Severity:** blocker · **Status:** applied with edits
**Location:** specs/board-view/spec.md > Requirement: Write failures; design.md > ViewModel and state > Failures
**Current text:** "When a write fails with a network error, a server error or an error code the app doesn't know, the dialog or inline field SHALL stay open with the user's input and an error toast SHALL show" / design: "`Failed`: the matching D15 toast through `ToastController`. Inputs stay."
**Failure scenario:** The proposal lists cardigan-shared-specs `api-conventions` "Retries after a lost response" as implemented. That requirement says clients "SHALL report the failure and reload the affected data before the user tries again". Here, quick add on a flaky connection loses the response of a card create that the server applied. The text stays, the user presses Enter again, and the board gets a duplicate card (same for add column). The spec only forbids *automatic* repeats.
**Fix:** Add to the spec requirement "Write failures": "When adding a column, adding a card or moving a column fails with a network error or an unknown outcome, the app SHALL also reload the board in the background, keeping the open field or dialog and its text. A failed reload keeps the board as shown and shows no second toast." Add the scenario: "**WHEN** adding the card "Write docs" fails with a network error after the server created it **THEN** the field still holds "Write docs", the error toast shows, and the board reloads and shows the created card." In design.md "Failures", change the `Failed` line to: "`Failed`: the matching D15 toast through `ToastController`. Inputs stay. For POSTs (add column, add card, move) the board also reloads in the background, as `api-conventions` "Retries after a lost response" requires." Add the break vector "Card create fails with an unknown outcome → board reloads, text kept" to design "Testing" and task 3.2/3.3.

### A1: Target index of a queued column move is undefined
**Severity:** major · **Status:** applied
**Location:** specs/board-view/spec.md > Requirement: Column menu; design.md > ViewModel and state > One write at a time
**Current text:** "A move SHALL place the column one position to the left or right." / "All writes and refreshes go through one `Mutex` in the order they arrive."
**Failure scenario:** A move shows no progress UI, so the user can pick "Move right" on the same column twice before the first response arrives. If the ViewModel computes `position` when the action arrives, both requests send index 2, and the column moves one step instead of two (or a queued move sends an index that a queued delete has made `OUT_OF_RANGE`, showing a false "changed on another device" toast). The menu's enabled state has the same problem.
**Fix:** Add to the spec requirement "Column menu": "The target position SHALL be computed from the board as shown when the request is sent, after earlier writes have been applied. Move left and Move right SHALL be enabled from that same board." Add the scenario: "**WHEN** the user picks "Move right" twice on the first of four columns before the first move is confirmed **THEN** after both are confirmed the column shows third." In design.md "One write at a time", add: "A move computes its target index inside the mutex from the current state, not when the action arrives."

### A2: Labels failing makes the whole board fail to load
**Severity:** major · **Status:** applied
**Location:** design.md > ViewModel and state
**Current text:** "Load and refresh fetch the board and `LabelSource.labels()` in parallel. If either fails, the load fails"
**Failure scenario:** This is user-visible behavior (the "Couldn’t load this board" state when only labels fail) that is in no decision and no spec requirement. Two implementers could reasonably decide differently (show the board without chips), and no test asserts it from the spec. CLAUDE.md says not to make product decisions.
**Fix:** Confirm with the user, then add to the spec requirement "Loading and load failure": "The board and the user's labels SHALL load together. When the labels fail to load, the load SHALL count as failed." with the scenario "**WHEN** the board loads but the labels fail with a network error **THEN** the error state shows." If the user decides otherwise, change the design line instead.

### A3: Restore after process death doesn't close a field whose column is gone
**Severity:** minor · **Status:** applied
**Location:** design.md > ViewModel and state > Process death / Pull to refresh with an open field
**Current text:** "the open field or dialog (column ID, kind) and its text are saved in `SavedStateHandle`" / "A quick add field whose column is gone closes."
**Failure scenario:** The close rule is only stated for refresh. After a restore, the first load can return a board without the saved field's column (or a rename/delete dialog for it), leaving a field or dialog bound to a missing column.
**Fix:** In design.md "Pull to refresh with an open field", replace the sentence with: "After any load (first load, restore, refresh), a quick add field, rename dialog or delete dialog whose column is gone closes. Other fields and dialogs keep their text."

### D2: Board list store goes stale after the board view finds the board missing
**Severity:** major · **Status:** applied
**Location:** specs/board-view/spec.md > Requirement: Missing board; design.md > Navigation and wiring
**Current text:** "When a load or a write reports that the board no longer exists, the app SHALL leave the board view, show the board list and show the toast "This board no longer exists.""
**Failure scenario:** add-board-list-ui keeps boards in a Koin `single` store and only updates it through its own writes, `refresh()` and `BoardListReloader`. After `onBoardMissing`, the board list still shows the deleted board right below the "This board no longer exists." toast, and tapping it opens the board view and fails again. The board list's own rename-404 removes the board, so behavior differs by entry point.
**Fix:** Add to the spec requirement "Missing board": "The board SHALL also disappear from the board list and the sidebar." Add to the scenario's THEN: "and the board is not in the list". In design.md "Navigation and wiring", change the `onBoardMissing` wiring to: "`onBoardMissing` → `BoardListReloader.reload()` (or remove the board from the board list store, whichever API add-board-list-ui exposes) and pop back to the board list route." Note for the user: card counts and order on the board list also go stale after quick adds; whether returning from the board view reloads the list is a separate product call.

### D3: A 404 on a column call can't tell a missing board from a missing column
**Severity:** minor · **Status:** applied
**Location:** design.md > Repositories and errors; tasks.md > Workflow follow-up
**Current text:** "`deleteColumn` maps a 404 for the column to success" / "Hand-off for `mob-boards-connect`: map ... 404 on board and column"
**Failure scenario:** `api-errors` returns 404 for anything not found, so `/boards/{b}/columns/{c}` can't say which one is gone. If the board was deleted, a column delete counts as done and the user stays on a dead board. A rename shows the column toast, reloads, then the board toast.
**Fix:** Add to the `mob-boards-connect` hand-off: "A 404 on a column call maps to `ColumnNotFound` (or success for delete). The reload that follows detects a missing board through `loadBoard`'s `BoardNotFound`. After a column delete answered with 404, reload the board too."

### D4: Design and decisions disagree on the backend state
**Severity:** minor · **Status:** applied with edits
**Location:** design.md > Context; decisions.md > Facts
**Current text:** design: "`be-boards` is archived and implemented: the board and column endpoints exist" / decisions: "The backend snapshot has only labels and `users/me`. Boards and columns are planned in `be-boards`"
**Failure scenario:** `api/openapi.json` doesn't exist in this repo yet, so the design claim can't be checked here. A reader of the hand-off can't tell whether `mob-boards-connect` is unblocked.
**Fix:** In design.md "Context", replace the sentence with the state as of the latest backend check, and name the source (for example "`be-boards` is archived in the backend repo; `api/openapi.json` hasn't been synced yet."). The Facts line in decisions.md stays as recorded.

### D5: Task 5.1 drops the route's name and key
**Severity:** minor · **Status:** applied
**Location:** tasks.md > 5.1
**Current text:** "Add `BoardRoute(boardId)` and `boardViewDestination(...)`"
**Failure scenario:** design.md and add-board-list-ui both use `BoardRoute(boardId, name?, key?)` for the D39 header hint. Building the task as written breaks the board list's `onOpenBoard` mapping.
**Fix:** "Add `BoardRoute(boardId, name = null, key = null)` and `boardViewDestination(...)`"

### T1: Phone layout scenarios can't fail
**Severity:** minor · **Status:** applied
**Location:** specs/board-view/spec.md > Requirement: Layout per window class > Scenario: Phone swipe
**Current text:** "the strip stops where the fling ends, without snapping to a column" / "with the next column partly visible"
**Failure scenario:** "Where the fling ends" and "partly visible" have no observable bound. A snapping `LazyRow` with a short fling passes.
**Fix:** Scenario: "**WHEN** the phone layout shows a board with 4 columns at 390 dp width **THEN** the second column's left edge is inside the window. **WHEN** the user swipes sideways by half a column width without a fling **THEN** the strip stays at that offset (the first visible column's scroll offset is not 0)." Catches: a snap fling behavior added to the `LazyRow`.

### T2: Column delete copy has no singular form
**Severity:** minor · **Status:** applied
**Location:** specs/board-view/spec.md > Requirement: Delete column
**Current text:** ""Its {count} cards will be deleted too. This can’t be undone." with the button "Delete column and {count} cards""
**Failure scenario:** D9 requires plurals, but the requirement and scenario only cover 2 cards. "Its 1 cards" / "Delete column and 1 cards" would pass.
**Fix:** Add: "Both strings SHALL use plural forms (D9)." and the scenario "**WHEN** the user deletes a column with 1 card **THEN** the dialog reads "Its 1 card will be deleted too. This can’t be undone." and the button "Delete column and 1 card"." Catches: a plain string resource with "cards".

### T3: Tile accessibility text for a card without labels is undefined
**Severity:** minor · **Status:** applied
**Location:** specs/board-view/spec.md > Requirement: Accessibility labels
**Current text:** "A card tile SHALL read as "{displayId}, {title}, labels {a}, {b}", with labels in tile order."
**Failure scenario:** A card with no labels could read "CRD-1, Title, labels" and still match the template.
**Fix:** Append: "Without labels, the tile SHALL read "{displayId}, {title}"." Scenario: "**WHEN** a screen reader focuses `CRD-1` "Setup" without labels **THEN** it reads "CRD-1, Setup"." Catches: the template string used with an empty join. (Follows D9's "zero parts are dropped"; confirm the copy with the user if unsure.)

## Trust boundaries

- Network responses: only fixtures in this change; no parsing to attack yet. Names returned by the server are shown as plain Compose text. Not a gap.
- User text input: validated with the shared text rule before sending; rendered as text. Not a gap.
- Route arguments: `BoardRoute` is reached only through internal navigation (no deep links per CLAUDE.md). Not a gap.
- `SavedStateHandle`: holds unsent field text in OS-protected app state. Not a gap.
- Fixture data: bound only when `isDebug` is true and removed before archive (D6). Not a gap.
- Logging: Kermit debug-only per CLAUDE.md; the change adds no logging. Not a gap.

## Coverage

Checked all 22 requirements and their scenarios in `specs/board-view/spec.md`, plus decisions.md, proposal.md, design.md and tasks.md. Read the add-board-list-ui design/decisions/spec for the shared store, dialogs and route, and cardigan-shared-specs `api-conventions` (move, stale move, retries). Confirmed in code: `ToastController`, `RefreshBox`, `BoardColumn` (ColumnScope slot), `WindowClass`, `Board`, `ExportBoardRoute`. `api/openapi.json` doesn't exist. Not read: the other shared specs (`domain-*`, `text-length`, `api-errors`) beyond what the artifacts quote, Figma, and the backend repo.

## Applied changes

- `specs/board-view/spec.md`: new requirement "Reload after a lost response" with its scenario (D1, split out of "Write failures" because strict validation flagged that requirement as too long). Labels load together with the board, plus the "Labels fail" scenario (A2, confirmed by the user: the whole load fails). Move target computed from the board when sent, plus the "Two moves before the first is confirmed" scenario (A1). A missing board also disappears from the board list (D2). New phone layout scenarios (T1). Plurals and a one-card scenario for delete column (T2). Tile text without labels (T3).
- `design.md`: background reload for failed POSTs (D1), move index inside the mutex (A1), A2 recorded as confirmed, close rule after any load (A3), `onBoardMissing` wiring (D2), backend state in Context (D4), new break vectors.
- `tasks.md`: 3.2 and 3.3 break vectors (D1, A1, A3), 5.1 route signature (D5), 5.2 `onBoardMissing` wiring (D2), connect hand-off for column 404s (D3).
- `proposal.md`: the failures bullet mentions the background reload and the missing board leaving the list.
- `decisions.md` (edited at the user's request on 2026-11-22): the Facts line about the backend snapshot now says `be-boards` is archived and the snapshot has the board and column paths.
- Still open, outside this review: whether the board list reloads when the user comes back from the board view, so card counts and order stay current (noted under D2).
- `openspec validate add-board-view-ui --strict` passes.
