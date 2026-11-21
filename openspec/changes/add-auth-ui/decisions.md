# Decisions: add-auth-ui

Confirmed with the user via the coordinator session (cardiganapp-18) on 2026-11-21. This session didn't ask the user directly. It sent its questions (MA-1 to MA-17) to the coordinator, which merged the shared ones with the web and board sessions and relayed the user's answers. The coordinator's cross-client IDs are given in brackets, e.g. [C-D25]. add-transfer-ui recorded its decisions the same way.

## Summary

Build the mobile auth UI for chunk `mob-auth-ui` in the `auth` feature: Log in and Register screens with ViewModels and client-side validation from the shared specs, the "Session expired" state on the login screen, a logout action, and the session interface the app shell uses. Everything runs against interfaces with test doubles. HTTP, token storage, refresh and the real session restore come in `mob-auth-connect`. The app start waits behind the platform splash until the session state is known. After that, the app shows the login screen or the board list.

## Decisions

### D1 — Scope [C-D2]
- **Question**: Which parts of login, register, session expired and log out does this change build?
- **Decision**: All four, as UI and interfaces:
  - the login and register screens
  - "session expired" as a parameter of the login screen
  - a session interface in `:core:domain` with logout and the session state, plus a fake
  - an auth repository interface with a fake
  No HTTP, no tokens, no refresh. Those come in `mob-auth-connect`.
- **Alternatives**: Login and register only; merge chunks 12 and 26 into one change.
- **Why**: Other features (board list menu, sidebar) need the logout action and the session email now. The backend auth endpoints aren't in the spec yet.
- **Depends on**: —

### D2 — Ownership of the app shell and Log out [C-D3]
- **Question**: Who builds the Log out menu item and the sidebar?
- **Decision**: The board list change owns the sidebar and the phone ⋯ menu (Import board, Labels, Log out), built in `:composeApp`. Auth only provides the logout action and the signed-in email.
- **Alternatives**: The auth change adds the menu item itself.
- **Why**: One owner for the shell. SHARED with web.
- **Depends on**: D1

### D3 — Temporary verification fixture [C-D6]
- **Question**: How is the UI checked before the endpoints exist?
- **Decision**: Interfaces plus test doubles. Mobile also gets a temporary in-memory fixture and a debug entry, removed in their own commit before archive.
- **Alternatives**: Test doubles only.
- **Why**: Same approach as add-transfer-ui.
- **Depends on**: D1

### D4 — When validation errors show [C-D25]
- **Question**: When are field errors shown?
- **Decision**:
  - The four password rules update live with every keystroke.
  - Other field errors show after the field loses focus for the first time, and then update live.
  - The submit button always stays enabled.
  - Submit with errors sends no request and shows every error.
- **Alternatives**: Errors only on submit (this session's recommendation); a live mismatch check; disable the button until the form is valid.
- **Why**: User choice. SHARED with web.
- **Depends on**: —

### D5 — Email check and field copy [C-D26]
- **Question**: How strict is the device-side email check, and what is the copy?
- **Decision**:
  - Register checks the full domain-user email rule, the same as the backend.
  - Login checks only that email and password aren't empty.
  - Copy: "Enter your email." / "Enter a valid email address." / "Email can't be longer than 254 characters." / "Enter your password." / "Repeat your password."
  - Unmet password rules on submit: no message, a danger border on the password field and red (unmet) rules.
  - Register loading: "Creating account…".
- **Alternatives**: A loose "@" check; the full rule on login too.
- **Why**: Backend login treats a malformed email as wrong credentials, so a format check there adds nothing. SHARED with web.
- **Depends on**: D4

### D6 — Rate limit and connection errors [C-D27]
- **Question**: What do 429, network errors, 5xx and unexpected server validation errors show?
- **Decision**: An error Banner in the auth card.
  - 429: "Too many attempts. Try again in {time}.", with {time} from `Retry-After` (format in D19). No countdown. The button stays enabled.
  - Network and 5xx: "Couldn't reach Cardigan. Check your connection and try again."
  - 400 `VALIDATION_FAILED` maps known field codes to the field texts. Anything else shows the generic banner.
- **Alternatives**: Toasts; a countdown; disabling the button.
- **Why**: Same pattern as the designed "Email or password is incorrect." banner. SHARED with web.
- **Depends on**: D5

### D7 — Logout [C-D28]
- **Question**: Does logout ask for confirmation, and where does it land?
- **Decision**: No confirmation. Server errors are ignored. The user lands on the plain login screen without the banner.
- **Alternatives**: A confirmation dialog.
- **Why**: No design for a dialog. The shared spec says to discard tokens whatever the response. SHARED.
- **Depends on**: —

### D8 — Landing after login or register [C-D29]
- **Question**: Where does the user go after a successful login or register?
- **Decision**: Mobile always goes to the board list, and the auth screens are cleared from the back stack. Register logs the user in right away.
- **Alternatives**: Register leads to the login screen.
- **Why**: be-auth's register starts a session. Web differs (it returns to the previous page).
- **Depends on**: —

### D9 — Session expired [C-D30]
- **Question**: When does the "Session expired" banner show, and how does it behave?
- **Decision**:
  - The session layer sets an in-memory flag when a refresh fails.
  - No email prefill.
  - The banner stays until the next submit or until the user leaves the screen.
  - On mobile, a stored refresh token rejected at app start also shows the banner.
- **Alternatives**: Prefill the email (in memory or persisted); clear the banner on the first edit; plain login on a cold-start rejection.
- **Why**: User choice. The Figma frame shows a filled-in email, but the user chose no prefill.
- **Depends on**: D1

### D10 — Wrong credentials and email already registered [C-D31]
- **Question**: How do the two server-side form errors behave?
- **Decision**:
  - Wrong email or password: both values stay, the password field shows the error state, and the banner and error state clear on the next edit of either field.
  - Email already registered: error under the email field, cleared when the email is edited. No "Log in instead" link.
- **Alternatives**: Clear the password.
- **Why**: As drawn in Figma. SHARED.
- **Depends on**: —

### D11 — Splash during session restore [C-D32]
- **Question**: What shows while the session is restored on app start?
- **Decision**: The platform splash stays until the restore finishes: Android SplashScreen API, iOS launch screen. Adding `androidx.core:core-splashscreen` is approved.
- **Alternatives**: A spinner screen; login first, then a jump to the board list.
- **Why**: No design needed, and no flash of the login screen.
- **Depends on**: —

### D12 — Autofill and keyboard [C-D33]
- **Question**: How do the fields work with password managers and the keyboard?
- **Decision**:
  - Autofill hints for the email, password and new-password fields.
  - Email keyboard for the email field.
  - Next moves between fields, and Done on the last field submits.
  - No show-password toggle.
- **Alternatives**: A show-password toggle (needs a design); no hints.
- **Why**: Mobile only. Figma has no eye icon.
- **Depends on**: —

### D13 — Signed-in email [C-D34]
- **Question**: Where does the email in the phone menu ("Signed in as {email}") and the sidebar come from?
- **Decision**: From the local session. The login and register responses contain it, so no `GET /users/me` call is needed. The session interface exposes it for the shell. On mobile it is kept with the session, so it's available after a restore (D15).
- **Alternatives**: `GET /users/me`.
- **Why**: Faster, no extra request.
- **Depends on**: D1, D2

### D14 — Doc conflicts and the CLAUDE.md fix [C-D35]
- **Question**: The repo's CLAUDE.md says login and register are rate limited per client IP and per email, and that refresh is rate limited. The shared spec says per email only and never for refresh or logout. Which wins?
- **Decision**: The shared spec. This change also fixes the Authentication section of CLAUDE.md.
- **Alternatives**: —
- **Why**: Shared specs win for API behavior.
- **Depends on**: —

### D15 — Email after an app restart [MA-13]
- **Question**: The refresh response has no user. Where does the signed-in email come from after a restart?
- **Decision**: Persist the email in secure storage next to the refresh token. Save it on login and register, and clear it with the tokens on logout or when the session ends. Web calls `GET /users/me` instead.
- **Alternatives**: Call `GET /users/me` after the restore; change the backend's refresh response.
- **Why**: No extra request, and the email is there as soon as the restore succeeds. The storage itself is built in `mob-auth-connect`. This change only fixes the interface contract.
- **Depends on**: D13

### D16 — Splash look [MA-14]
- **Question**: iOS can't keep its launch screen up. What do the splash and the wait look like?
- **Decision**: Plain `background/app`, light and dark, with no logo. Android shows the system splash with the launcher icon on `background/app`. iOS sets its launch screen to `background/app`, and the app draws only that color until the restore finishes.
- **Alternatives**: The Brand on both platforms (needs a design); wait for a design.
- **Why**: Nothing new to design. The launch screen just appears to stay.
- **Depends on**: D11

### D17 — Register password limits [MA-15]
- **Question**: What shows for a register password over 128 characters, or one with a control character? The four rules can't show either case.
- **Decision**: A message under the password field, after the first blur and then live (D4): "Password can't be longer than 128 characters." / "Password can't contain tabs or line breaks.". The four rule items stay as they are.
- **Alternatives**: Mark "12+ characters" as unmet; other wording.
- **Why**: `text-length` "Clients validate with the same rule" forbids sending such a password, so the user needs to know why nothing is sent.
- **Depends on**: D4, D5

### D18 — Offline start [MA-16]
- **Question**: Where does the app start when the restore can't reach the backend (offline or 5xx)?
- **Decision**: Close the splash and open the board list as signed in. Its own load error state shows, and the next load or pull to refresh tries the refresh again.
- **Alternatives**: Keep the splash and retry; open login.
- **Why**: `api-auth-tokens` "Other refresh failures are temporary" keeps the session.
- **Depends on**: D11, D15

### D19 — Rate-limit time and a missing header [MA-17, shared with web-auth]
- **Question**: How is {time} written, and what shows when `Retry-After` is missing or unusable?
- **Decision**:
  - Under 60 seconds: "1 second" / "{n} seconds".
  - From 60 seconds on: whole minutes rounded up, "1 minute" / "{n} minutes". For example, 61 seconds is "2 minutes".
  - A 429 without a usable `Retry-After` shows the generic banner "Couldn't reach Cardigan. Check your connection and try again.".
- **Alternatives**: Seconds only; a separate "Try again later." text.
- **Why**: Long waits read better in minutes. No new copy for a case the backend never sends.
- **Depends on**: D6

### D20 — Accessibility labels
- **Question**: Which accessibility labels do the auth screens need? The accepted style is "Back", "Board actions", "Actions for column “{name}”".
- **Decision**: No new labels. The auth screens have no icon-only controls. Fields are named by their visible labels, links and buttons by their text, and the password rule items by their text plus the existing design-system "met" / "not met" state text. The banners are announced as a polite live region with their text.
- **Alternatives**: Extra labels such as a rules summary.
- **Why**: Every control already has visible text that names it.
- **Depends on**: —

## Facts

- be-auth isn't implemented. The backend's committed OpenAPI snapshot has only labels and `GET /users/me`, so there are no auth DTOs to generate yet. — source: `~/IdeaProjects/cardigan/app/src/test/resources/openapi/openapi.json`, `openspec/changes/be-auth/tasks.md` in the backend
- The planned response shapes: `AuthSessionDto(accessToken, accessTokenExpiresAt, refreshToken?, user: CurrentUserDto)` for login and register, and `AuthTokensDto(accessToken, accessTokenExpiresAt, refreshToken?)` for refresh, with no user. — source: backend `openspec/changes/be-auth/design.md`
- This repo has no `api/openapi.json` yet. — source: repo listing
- The password rule: 12–128 code points after NFC, with at least one Ll, one Lu and one Nd. No control characters. Not trimmed. — source: cardigan-shared-specs `domain-user` "Passwords follow one composition rule", `text-length` "Passwords are normalized without trimming", "Invalid characters are rejected"
- The email rule: 1–254 code points after text-length normalization, RFC 5322 addr-spec, ASCII only. The client sends it as entered. — source: `domain-user` "Email is stored lowercased and identifies one user", "Email follows the RFC 5322 address format"
- Clients must apply the same rules before sending. — source: `text-length` "Clients validate with the same rule"
- Login failures for an unknown email and a wrong password look the same: 401 `INVALID_CREDENTIALS`. — source: `api-errors` "Login failures look the same"
- A used email gives 409 `CONFLICT` with field error `email`/`ALREADY_USED`. — source: `api-errors` "Uniqueness clashes are conflicts"
- 429 `RATE_LIMITED` comes with `Retry-After` in whole seconds, at least 1. Limits count per email, separately for login and register. — source: `api-auth-tokens` "Auth endpoints are rate limited", "Rate limits count per email"
- Figma page 9:4 has these frames:
  - Log in and Register (phone, tablet, desktop)
  - Wrong email or password (desktop, phone)
  - Session expired (desktop, phone)
  - Live password check and mismatch (desktop, phone)
  - Email already registered (desktop)
  - Logging in (phone)
  — source: Figma `get_metadata` on 9:4
- Figma annotations: "Each rule shows a cross until it is met, then a check. It updates with every keystroke." / "Shown after a failed token refresh logs the user out." / "Fields are disabled while the request runs." — source: `use_figma` read of `node.annotations` on 9:4
- In Figma, "Create account" is drawn enabled with unmet rules. Unmet rules put the password Input in its Error state. Wrong credentials put the password Input in its Error state, not the email. — source: `use_figma` read of the instance properties on 9:4
- `:core:designsystem` already has Field, Input, PasswordRule, ErrorMessage, Banner (Error/Info), CardiganButton (`isLoading`), Brand and WindowClass. — source: `core/designsystem/.../components`
- `:feature:auth:{domain,data,presentation}` have build files only. `:composeApp` has an empty start route and the transfer graph. — source: repo listing
- `iosApp` uses the default launch screen. The Android activity theme is `Theme.Material.Light.NoActionBar`. — source: `iosApp/iosApp/Info.plist`, `androidApp/src/main/AndroidManifest.xml`

## Out of scope

- HTTP calls, generated auth DTOs, token storage, the bearer plugin, refresh and the real session restore (`mob-auth-connect`)
- The sidebar, the phone ⋯ menu and the Log out menu item (board list change, D2)
- Forgot password, email verification, change password, an account or settings screen
- A show-password toggle
- A logout confirmation dialog

## Deferred

- (none) MA-13 to MA-17 were answered on 2026-11-21 (D15–D19).
