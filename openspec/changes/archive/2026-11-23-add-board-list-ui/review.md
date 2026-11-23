# Artifact review (light): add-board-list-ui

**Verdict:** Ready
**Reviewed:** 2026-11-21, applied 2026-11-22, schema `careful-planning`, repo kind `code`

| Pass | Blocker | Major | Minor |
|---|---|---|---|
| Testability | 0 | 1 | 3 |
| Security | 1 | 0 | 0 |
| Hidden assumptions | 0 | 3 | 0 |
| Design contract | 0 | 0 | 3 |

## Findings

### S1: Board store isn't cleared when the session ends
**Severity:** blocker · **Status:** applied with edits
**Location:** design.md > Board store (D2, D13); specs/board-list/spec.md > (no requirement)
**Current text:** "`InMemoryBoardRepository(remote)` is a Koin `single`" / "`refresh()` replaces the list with the server's list as it is."
**Failure scenario:** User A logs out (or the session expires) and user B logs in without the process dying. The Koin `single` still holds A's boards, so B's sidebar shows them, B's key clash check and transfer's `BoardKeyOwnerLookup` return A's board names, and if B's first refresh fails the D12 path keeps A's list on screen. A refresh started for A that completes after logout also writes A's boards back into the cleared store. CLAUDE.md requires in-memory feature data to be cleared on logout or failed refresh, and neither this change nor add-auth-ui says who clears the board store.
**Fix:** Add to spec.md:
> ### Requirement: Board data is cleared when the session ends
> When the session leaves the signed-in state (log out, expired session, failed restore), the board store SHALL return to the never-loaded state, and any refresh or save that was running SHALL NOT write its result into the store afterwards. The next signed-in user SHALL see only boards loaded for their session.
>
> #### Scenario: Log out and sign in as someone else
> - **WHEN** user A has 6 boards loaded, logs out, and user B signs in and B's first load fails
> - **THEN** the load error state shows, the sidebar shows no boards, and the key lookup finds none of A's keys
>
> #### Scenario: Refresh finishes after logout
> - **WHEN** a refresh is running and the user logs out before it returns
> - **THEN** the store stays in the never-loaded state

Add to design.md > Board store: "`BoardRepository.clear()` resets `boards` to null and bumps a session generation counter. `refresh` and mutations apply their result only if the generation is unchanged. `:composeApp` calls `clear()` when `SessionManager.state` leaves `SignedIn` (D20), next to the existing back stack reset." Add a task under 8.2 with a unit test for both scenarios.

### T2: Failed pull to refresh from the empty or error state is undefined
**Severity:** major · **Status:** applied
**Location:** specs/board-list/spec.md > Pull to refresh; Failed refresh keeps the list
**Current text:** "Pull to refresh SHALL also work in the empty and load error states." / "When a refresh fails while boards are shown, …"
**Failure scenario:** In the load error state the user pulls down and it fails again. The design routes every pull through `BoardListReloader`, which shows the refresh toast, while one implementer keeps the error state silently and another shows the toast on top of "Couldn't load your boards". No test can tell which is right. The same gap applies to a failed pull from the empty state, which does have a list (an empty one).
**Fix:** Confirm with the user, then add to "Failed refresh keeps the list": "When a refresh started from the empty or load error state fails, that state SHALL stay as it is, the indicator SHALL stop, and the refresh toast SHALL show." with a scenario: "**WHEN** the load error state shows and a pull to refresh fails **THEN** the error state stays and the toast 'Couldn't refresh your boards. Check your connection and try again.' shows." A wrong implementation this catches: one that swaps the error state for an empty list or skips the toast.

### A1: The store may never be loaded on screens that aren't the board list
**Severity:** major · **Status:** applied
**Location:** design.md > Dialogs as destinations; Board list screen; specs/board-list/spec.md > Sidebar board section
**Current text:** "Each dialog ViewModel reads the board by id from `repository.boards`. If it is missing (deleted elsewhere, or process death before a load), the dialog closes at once." / "The first load and 'Try again' call `repository.refresh()` from the ViewModel"
**Failure scenario:** Only the board list ViewModel triggers the first load. After process death on the board view, the back stack is restored but the list's ViewModel isn't recreated until the user goes back. The tablet sidebar is empty, transfer's key lookup finds nothing, and "Rename board" / "Delete board" from the board view ⋯ menu close immediately with no feedback.
**Fix:** design.md > Board store: "The shell starts the first load once the session is `SignedIn` (`:composeApp`, through `BoardListReloader`), not the board list ViewModel. The list ViewModel shows Loading/Error from the store's load state." Dialogs: "If `boards` is null, the dialog waits for the running load. It closes at once only when the loaded list has no board with this id." Add to spec.md > Sidebar board section: "The section SHALL load the boards if they aren't loaded yet, whichever screen is shown."

### A2: A refresh running during a save can undo the save in the store
**Severity:** major · **Status:** applied
**Location:** design.md > Board store (D2, D13)
**Current text:** "`refresh()` replaces the list with the server's list as it is. … `createBoard` inserts the response by `BoardOrder`."
**Failure scenario:** An import reload (or a pull) starts a refresh. While it waits, the user creates "GARD", and the store inserts it. The refresh then returns the server list from before the create and replaces the store, so "GARD" disappears from the list and the sidebar and the key clash check misses it, until the next refresh. The same happens to a rename (old name comes back) and a delete (the board comes back).
**Fix:** design.md > Board store: "Mutations that complete while a refresh is running are recorded. Before the refresh result is published, the store re-applies them by id (insert or replace by `BoardOrder`, or remove). Mutations that complete while no refresh runs update the list directly." Add to task 3.1: "a create, rename and delete that each complete during a running refresh are still in the store after it returns."

### A3: Updated times in the future aren't covered
**Severity:** major · **Status:** applied
**Location:** specs/board-list/spec.md > Relative updated time
**Current text:** "under 1 minute 'Just now'; under 1 hour '1 minute ago' or '{n} minutes ago'"
**Failure scenario:** The device clock runs behind the server, so `updatedAt` is after "now". This happens right after every create and rename, when the response time is set by the server. Implementations will show "-3 minutes ago", "Just now", or a date, depending on how they compare.
**Fix:** Add to the requirement: "An updated time after the device clock SHALL read 'Just now'." with a scenario: "**WHEN** a board's updated time is 5 minutes after the device clock **THEN** its time reads 'Just now'." It catches an implementation that formats the negative difference.

### T1: Rounding of minutes and hours isn't stated
**Severity:** minor · **Status:** applied
**Location:** specs/board-list/spec.md > Relative updated time
**Current text:** "under 1 hour '1 minute ago' or '{n} minutes ago'; on the same calendar day '1 hour ago' or '{n} hours ago'"
**Failure scenario:** The "Earlier today" scenario (09:30 → 18:00 = "8 hours ago") only passes with rounding down. An implementation that rounds to nearest gives "9 hours ago" for 8 h 30 min and fails no other scenario.
**Fix:** Append: "`{n}` is the elapsed time in whole minutes or whole hours, rounded down, measured between the two instants (so a DST change doesn't shift it)."

### T3: App start scenario doesn't state the session or a falsifiable outcome
**Severity:** minor · **Status:** applied
**Location:** specs/board-list/spec.md > Board list is the start screen > Scenario: App start
**Current text:** "**WHEN** the app starts **THEN** the board list shows and loads the user's boards"
**Failure scenario:** The requirement applies only with a session, but the scenario has no precondition, so a test can't tell it apart from add-auth-ui's login start. It also passes if the list shows a stale cache without a request.
**Fix:** "**WHEN** the app starts with a signed-in session **THEN** the first screen is the board list in its loading state and one board list request is made."

### T4: Outside tap while saving has no scenario
**Severity:** minor · **Status:** applied
**Location:** specs/board-list/spec.md > Dialogs while saving
**Current text:** "the dialog SHALL NOT close by back, tapping outside or any other dismiss." (one scenario, for back only)
**Failure scenario:** `CardiganDialog.onDismissRequest` is the one path for outside taps. If only the back handler checks `isSaving`, the only scenario passes and an outside tap still closes the dialog mid-save.
**Fix:** Add "#### Scenario: Tap outside while saving — **WHEN** 'Save' in Rename board is saving and the user taps outside the dialog **THEN** the dialog stays open with Cancel and the field disabled."

### D1: `navigateToImport()` doesn't exist
**Severity:** minor · **Status:** applied
**Location:** design.md > Wiring and placeholders; tasks.md > 8.2
**Current text:** "`navigateToImport()` is passed as `onImportClick`." / "Import entries → `navigateToImport()`"
**Failure scenario:** transfer:presentation exposes only `ImportBoardRoute` and `transferGraph(...)`. There is no `navigateToImport` helper, so the implementer either adds one to transfer (outside this change's scope) or improvises.
**Fix:** "`:composeApp` passes `{ navController.navigate(ImportBoardRoute) { launchSingleTop = true } }` as `onImportClick`."

### D2: Spec and design disagree on server key error codes
**Severity:** minor · **Status:** applied
**Location:** specs/board-list/spec.md > Key field errors vs design.md > Errors
**Current text:** Spec: "Any other key field error code SHALL show 'Use only the letters A–Z.'" Design: "`KeyFieldError { data class Rule(val error: BoardKeyError); data object AlreadyUsed; data object Invalid }` … `KeyFieldError.Invalid` covers any key code other than the ones with their own text."
**Failure scenario:** A server `REQUIRED` or `TOO_LONG` for `key` shows "Use only the letters A–Z." per the spec, but "Enter a key." / "Use at most 5 letters." per the design. Tests written from each disagree.
**Fix:** Spec: "A server key field error `REQUIRED`, `TOO_SHORT`, `TOO_LONG` or `INVALID_FORMAT` SHALL show the matching shared key string. Any other key code except `ALREADY_USED` SHALL show 'Use only the letters A–Z.'"

### D3: The "debug entry" from D3 has no design or task
**Severity:** minor · **Status:** applied
**Location:** decisions.md > D3; design.md > Temporary fixture; tasks.md > 10.1
**Current text:** decisions D3: "a temporary in-memory fixture data source and a debug entry for manual checks. Both are removed in their own commit". Design and tasks describe only the fixture.
**Failure scenario:** One implementer reads "debug entry" as the fixture binding, another adds a debug-only screen or menu item. Task 10.1 removes only the fixture, so an added entry could survive the archive.
**Fix:** design.md > Temporary fixture: "The debug entry is the `fixtureBoardsModule` binding in `:composeApp`. No extra UI is added." (or describe the entry if one is meant). Task 10.1: "Delete `boards:data/fixture`, `fixtureBoardsModule` and the debug entry …".

## Trust boundaries

- Network (backend, fixture for now): typed errors only, no parsing here. Nothing to attack in this change; ProblemDetail parsing is deferred to connect.
- Session / logout: an earlier user's boards survive in the `single` store and an in-flight refresh can repopulate it. Stopped by add-auth-ui's `SessionScopedStore` plus this change's `clear()` and session counter (S1).
- User input (name, key): normalized and validated per `text-length` before sending, server stays authoritative. Stopped.
- Imported files: handled by add-transfer-ui; this change only reloads afterwards. Out of scope.
- Process restore (SavedStateHandle): holds only name, key and flags of the user's own draft. No sensitive data. Stopped.
- Local storage: none. Logging: Kermit, debug only. No issue.

## Coverage

Read every artifact (decisions, proposal, the `board-list` delta spec, design, tasks), `openspec/config.yaml`, CLAUDE.md, and the code the design names: `Sidebar`, `BoardKeyRule`, `BoardKeyOwnerLookup`, `transferGraph`/`TransferRoutes`, `AppModule`, `AppNavHost`. Cross-checked add-board-view-ui (dialog results, `BoardRoute`) and add-auth-ui (`AppStartRoute`, logout clearing). All 30 requirements and their scenarios were checked. Not read: the shared-spec texts themselves (relied on the facts in decisions.md), `CardiganDialog` and `RefreshBox` internals, Figma.

## Applied changes

- **S1** (applied with edits): no new requirement here. add-auth-ui owns the rule (spec "Signing out clears user data in memory", design "Clearing user data on sign-out") and hands the store part to this change. design.md > Board store now has `InMemoryBoardRepository` bind `SessionScopedStore`, with `clear()` resetting `boards` to null and a session counter that drops late results. Task 3.1 tests it.
- **specs/board-list/spec.md**: App start scenario (T3). New requirement "Relative time edge cases" with rounding down and a future time as "Just now" (T1, A3). Failed refresh from the empty or error state keeps the state and shows the toast, plus a scenario (T2). New requirement "Server key field errors" (D2). Tap outside while saving scenario (T4). Sidebar loads boards if not loaded (A1).
- **design.md**: D1–D23 reference. `ensureLoaded()`, first load started in `:composeApp`, dialogs wait for the load instead of closing (A1). Saves during a refresh are re-applied (A2). `clear()` and session counter (S1). Import via `navController.navigate(ImportBoardRoute)` (D1). Fixture binding is the D3 debug entry (D3). The design's `KeyFieldError.Rule` already matched the D2 fix.
- **tasks.md**: 3.1 tests for `ensureLoaded`, saves during a refresh and `clear()`. 8.2 import navigation and first load. 10.1 names the debug entry. No checkbox changed.
- **proposal.md**: no change needed.
- T2 extends D12 (refresh failure toast) to the empty and error states. Confirmed by the user. decisions.md was not edited.
- `openspec validate add-board-list-ui --strict`: valid. Two long-requirement warnings from the first edit were fixed by splitting into the two new requirements above.
