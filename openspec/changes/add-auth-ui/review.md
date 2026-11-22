# Artifact review (light): add-auth-ui

**Verdict:** Ready
**Reviewed:** 2026-11-21, schema `careful-planning`, repo kind `code`

| Pass | Blocker | Major | Minor |
|---|---|---|---|
| Testability | 0 | 2 | 2 |
| Security | 1 | 0 | 0 |
| Hidden assumptions | 0 | 1 | 2 |
| Design contract | 0 | 1 | 2 |

## Findings

### S1: Signing out doesn't clear in-memory user data
**Severity:** blocker · **Status:** applied with edits
**Location:** specs/session-lifecycle/spec.md > "Logout needs no confirmation", "Session ending on its own"; design.md > "Session interface in `:core:domain`"
**Current text:** "When the user chooses Log out, the app SHALL end the session at once, with no confirmation. It SHALL show the login screen without a banner, whatever the server answers, and SHALL clear the back stack."
**Failure scenario:** CLAUDE.md requires "clear in-memory feature data" on logout or a failed refresh, but neither this change nor add-board-list-ui specifies it. add-board-list-ui's `InMemoryBoardRepository` is a Koin `single` whose `boards` stays non-null. User A logs out, user B logs in on the same device, and B's board list shows A's boards (and the sidebar lists them) until a refresh replaces them, or keeps them if that refresh fails offline.
**Fix:** Add to specs/session-lifecycle/spec.md:
```
### Requirement: Signing out clears user data in memory
When the session state becomes signed out for any reason, the app SHALL clear every in-memory store that holds data loaded for the signed-in user before the login screen shows. The next signed-in user SHALL NOT see any of that data.

#### Scenario: Another user logs in
- **WHEN** user A has loaded boards, logs out, and user B logs in while the board request is still running or fails
- **THEN** none of A's boards are shown to B
```
And in design.md, under "Session interface": "`:core:domain` gets `interface SessionScopedStore { fun clear() }`. Every in-memory store that holds user data binds it in Koin. `AppViewModel` calls `clear()` on all of them when the state changes to `SignedOut`, before navigating. The board list change's `InMemoryBoardRepository` implements it (send with the Workflow follow-up)." Add a matching `AppViewModel` unit test to task 5.2.

### T1: Server field errors have no code-to-message table
**Severity:** major · **Status:** applied with edits
**Location:** specs/auth-screens/spec.md > "Connection and unexpected errors"
**Current text:** "A 400 `VALIDATION_FAILED` SHALL show the matching field messages for known fields and codes, and the generic banner when none match."
**Failure scenario:** "Known" and "matching" are undefined. One implementer maps `password`/`PASSWORD_TOO_WEAK` to the error state with no message (as for unmet rules); another sends it to the banner. If the client's rules drift from the backend's, the first leaves the user stuck on a form with a red border and all rules shown as met. `email`/`INVALID_CHARACTER` and login-screen field errors are also unassigned.
**Fix:** Replace the sentence with: "A 400 `VALIDATION_FAILED` SHALL map field errors with this table and SHALL show the generic banner for any field error not in it, and when no field error matches:
| Screen | Field / code | Shown |
|---|---|---|
| Register | `email`/`REQUIRED` | "Enter your email." |
| Register | `email`/`TOO_LONG` | "Email can't be longer than 254 characters." |
| Register | `email`/`INVALID_FORMAT`, `email`/`INVALID_CHARACTER` | "Enter a valid email address." |
| Register | `password`/`REQUIRED` | "Enter your password." |
| Register | `password`/`TOO_LONG` | "Password can't be longer than 128 characters." |
| Register | `password`/`INVALID_CHARACTER` | "Password can't contain tabs or line breaks." |
| Login | `email`/`REQUIRED` | "Enter your email." |
| Login | `password`/`REQUIRED` | "Enter your password." |
Any other code, including `password`/`PASSWORD_TOO_WEAK`, shows the generic banner." (Use the exact code names from `api-errors` "Field error codes".) Add the scenario: "**WHEN** register fails with 400 and `password`/`PASSWORD_TOO_WEAK` **THEN** the generic banner shows". Catches: an implementation that silently shows only a red border for a server-side weak password.

### T2: The typing scenario gets the uppercase rule wrong
**Severity:** major · **Status:** applied
**Location:** specs/auth-screens/spec.md > "Password rules update live" > "Typing completes the rules"
**Current text:** "\"Lowercase letter\" and \"Uppercase letter\" are met from the second character on"
**Failure scenario:** The first character of `Correct horse 7` is `C`, so "Uppercase letter" is met from the first character. A test written from the scenario (unmet after one character) fails against a correct implementation, or gets "fixed" by breaking the rule.
**Fix:** "**THEN** \"Uppercase letter\" is met from the first character on, \"Lowercase letter\" from the second, \"12+ characters\" from the twelfth, and \"Digit\" only after the last character". Catches: a rule check that ignores the first code point.

### T3: Password manager scenario can't be tested
**Severity:** minor · **Status:** open
**Location:** specs/auth-screens/spec.md > "Keyboard and autofill" > "Password manager"
**Current text:** "**THEN** the system offers saved credentials for the app"
**Failure scenario:** Whether the OS offers credentials depends on device setup, so no `*UiTest` can fail on it. A missing hint is only found in manual check 8.2, if at all.
**Fix:** Replace the scenario with: "#### Scenario: Autofill hints — **WHEN** the Log in and Register screens are shown **THEN** the email fields carry the email-address content type, the login password the password content type, and both register password fields the new-password content type". Keep the device offer in task 8.2. Catches: a register password field that uses the plain password hint.

### T4: Unclear whether a blocked submit clears the banners
**Severity:** minor · **Status:** open
**Location:** specs/auth-screens/spec.md > "Session expired banner" > "Submit removes it"; "Rate limit banner"
**Current text:** "the banner goes away, whatever the result" / "The banner SHALL go away on the next submit."
**Failure scenario:** Tapping "Log in" with an empty password sends nothing. One implementation clears the session-expired and rate-limit banners on that tap, another only when a request starts. The tests disagree.
**Fix:** Append to both requirements: "A submit counts even when local validation blocks it and no request is sent." Add a scenario: "**WHEN** the session expired banner shows and the user submits with an empty password **THEN** the banner goes away and the password field shows \"Enter your password.\"". Catches: clearing the banner only inside the request branch.

### A1: Logout doesn't say local state clears before the server call
**Severity:** major · **Status:** applied
**Location:** design.md > "Session interface in `:core:domain`"; specs/session-lifecycle/spec.md > "Logout needs no confirmation"
**Current text:** "/** Ends the session. Never fails; server errors are ignored. */ suspend fun logout()"
**Failure scenario:** `mob-auth-connect` implements `logout()` as "call the server, then clear". Offline, the call waits for the HTTP timeout while the state is still `SignedIn`, so the user stays on the board list, taps again or puts the phone away believing they're signed out. The fake in this change flips at once, so every test here passes.
**Fix:** Design KDoc: "Reads the refresh token, then clears the tokens and email from storage and memory and sets `SignedOut(LoggedOut)` before sending the server logout. The server call is best-effort, runs after the state change, and its result is ignored." Spec, add to "Logout needs no confirmation": "The app SHALL show the login screen without waiting for the server's answer." and scenario "**WHEN** the user logs out and the logout request hangs **THEN** the login screen shows at once".

### A2: "Usable" `Retry-After` isn't defined
**Severity:** minor · **Status:** open
**Location:** specs/auth-screens/spec.md > "Rate limit banner" > "Missing header"; design.md > "`RateLimited.retryAfterSeconds` is nullable"
**Current text:** "**WHEN** a 429 response has no usable `Retry-After`"
**Failure scenario:** `Retry-After: 0`, `-5`, `1.5`, an HTTP-date, or `99999999999` (overflows `Int`): one implementation crashes or shows "Try again in 0 seconds", another shows the generic banner.
**Fix:** Add to the requirement: "`Retry-After` is usable when it is a decimal integer from 1 to 2,147,483,647 with no other characters. Anything else counts as missing." Add the scenario "**WHEN** a 429 has `Retry-After: 0` **THEN** the generic connection banner shows".

### A3: No order for password messages
**Severity:** minor · **Status:** open
**Location:** design.md > "Text rules ..." (`PasswordRules`); tasks.md > 2.4 ("the error order from design")
**Current text:** "`PasswordRules.check(raw): PasswordCheck` gives the four live flags, `tooLong`, `invalidCharacter` and `isEmpty`."
**Failure scenario:** A 129-code-point password with a tab: one validator shows the length message, another the tabs message. Task 2.4 points at an order the design only gives for email.
**Fix:** Add after the `PasswordRules` line: "The register password shows one message, in this order: empty → \"Enter your password.\", control character → \"Password can't contain tabs or line breaks.\", over 128 code points → \"Password can't be longer than 128 characters.\". Unmet rules alone show no message."

### D1: Text helpers clash with add-board-list-ui
**Severity:** major · **Status:** applied
**Location:** design.md > "Text rules in `:core:domain`"; tasks.md > 1.1
**Current text:** "`normalizeText(String)`: CRLF and CR to LF, NFC, trim Unicode White_Space. ... `codePointCount(String)`. ... **NFC uses `expect`/`actual`**"
**Failure scenario:** add-board-list-ui defines `toNfc()`/`toNfd()`, `normalizeText(input, multiLine = false)` with a fixed White_Space list, `codePointLength` and `validateName`. Both changes are planned in parallel on this branch. Whichever lands second either duplicates the helpers under other names or renames the other's API and breaks its tests. "Check and extend" doesn't say which names win.
**Fix:** Replace the `:core:domain` list with: "Use add-board-list-ui's `toNfc()`, `normalizeText(input, multiLine = false)` and `codePointLength`. If that change hasn't landed, add them with exactly those names and signatures from its design. This change adds only `normalizePassword(String)` (NFC only, via `toNfc()`), `hasInvalidCharacter(String, multiLine)` and `expect fun codePointCategory(cp: Int)`." Update task 1.1 and the test list (`codePointLength` instead of `codePointCount`).

### D2: Proposal and design put the email and password rules in different modules
**Severity:** minor · **Status:** open
**Location:** proposal.md > What Changes > "Shared text rules"
**Current text:** "**Shared text rules** in `:core:domain`: text normalization (NFC, trim), code point length, the email rule and the password rule from the shared specs."
**Failure scenario:** design.md and task 2.2/2.3 put `EmailRule` and `PasswordRules` in `:feature:auth:domain`. An implementer following the proposal puts them in core.
**Fix:** "**Shared text rules** in `:core:domain`: text normalization (NFC, trim), code point length and character checks. The email rule and the password rule go in `:feature:auth:domain`."

### D3: Spec says the iOS launch screen stays, design says it can't
**Severity:** minor · **Status:** open
**Location:** specs/session-lifecycle/spec.md > "Splash stays while the session restores"
**Current text:** "At app start, the app SHALL keep showing its launch splash until it knows whether a session exists."
**Failure scenario:** iOS removes the launch screen on the first frame (D16). An implementer reads the SHALL literally and tries to hold the iOS launch screen with a workaround.
**Fix:** "At app start, the app SHALL show only its launch splash, or on iOS a plain `background/app` screen that matches the launch screen, until it knows whether a session exists."

## Trust boundaries
- In-memory user data across accounts: a second user sees the first user's boards after logout. Not stopped (S1).
- Saved state across process death: passwords could be written to disk. Stopped (design keeps passwords out of `SavedStateHandle`).
- Debug fixture and overlay in release: a fixture that accepts any password would bypass login. Stopped (debug-only binding via the `isDebug` mechanism from commit 91fa210, Koin verify for release in 8.1, removal in 9.1).
- Logout without network: session stays active while the UI waits. Not stated (A1).
- Server error text: showing `title`/`detail` to users. Stopped ("Connection and unexpected errors").
- `Retry-After` header value: odd values crash or mislead the formatter. Partly stopped (A2).
- Email regex on user input: catastrophic backtracking. Stopped (no nested quantifiers, 250 KB test in 2.2).
- Secure storage of tokens and email, HTTP logging of credentials: out of scope here, belongs to `mob-auth-connect`.

## Coverage
Checked every requirement and scenario in `specs/auth-screens` (15 requirements) and `specs/session-lifecycle` (7 requirements), plus decisions.md, proposal.md, design.md and tasks.md. Read CLAUDE.md, `openspec/config.yaml`, `api-auth-tokens` (rate limits, logout, refresh rules) and `text-length` (password normalization, invalid characters, client validation) from the shared store, `App.kt`, `AppNavHost.kt`, `AppRoutes.kt`, the Koin setup, `Input.kt`, and the overlapping parts of add-board-list-ui (text helpers, board store, session wiring) and add-board-view-ui. Not read: `domain-user` and `api-errors` in full, Figma, the other design-system components, add-transfer-ui. No artifacts are missing.

## Applied changes
Applied: S1, T1, T2, A1, D1. The minors (T3, T4, A2, A3, D2, D3) stay open.

- `specs/session-lifecycle/spec.md`: new requirement "Signing out clears user data in memory" (S1). "Logout needs no confirmation" no longer waits for the server, with a new "Logout request hangs" scenario (A1).
- `specs/auth-screens/spec.md`: "Typing completes the rules" corrected (T2). The field-error mapping moved into a new requirement "Server field errors" with four scenarios, because the table made "Connection and unexpected errors" too long for strict validation. The mapping is written as prose instead of a table (T1, applied with edits).
- `design.md`: `logout()` KDoc ordering (A1). `SessionScopedStore` and the clearing rule, extended with the board list review's late-result guard: stores drop results of requests started before the last `clear()` (S1, applied with edits). Text helpers now reuse add-board-list-ui's `toNfc()`, `normalizeText(input, multiLine)` and `codePointLength` (D1).
- `tasks.md`: 1.1 (D1), 1.2 (`SessionScopedStore`, logout KDoc), 4.2 (field-error mapping), 5.2 (`clear()` on sign-out), Workflow follow-up (board store, mob-auth-connect logout ordering).
- `proposal.md`: session interface bullet lists the logout ordering and `SessionScopedStore`.
- No contradictions with decisions.md. No non-standard artifacts.
- `openspec validate add-auth-ui --strict`: valid.
- Cross-change: add-board-list-ui's review has a matching S1 (`BoardRepository.clear()` plus a session counter). Both fixes should be applied with the same `clear()`.
