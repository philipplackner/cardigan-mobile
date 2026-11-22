# Proposal

## Why

The app has no way to sign in yet, and every other screen needs a signed-in user. Chunk `mob-auth-ui` builds the auth screens, their validation, and the session interface the app shell and other features use. It builds them now, against interfaces. Then `mob-auth-connect` only has to add HTTP, token storage and refresh once be-auth is in the API spec.

## What Changes

- **Log in screen** (Figma 9:4) in phone, tablet and desktop layouts:
  - email and password, checked only for being empty (D5)
  - "Logging in…" with disabled fields while the request runs
  - "Email or password is incorrect." banner, with the password field in its error state (D10)
  - an info banner "Your session has expired. Log in again to continue." when the screen is opened after an expired session (D9)
  - the link to Register
- **Register screen**:
  - email, password and repeat password
  - live password rules
  - field errors after the first blur, then live (D4)
  - full email check (D5)
  - "Creating account…" loading state
  - "An account with this email already exists." under the email field (D10)
  - the link to Log in
- **Banners for both screens**: rate limit (429) and connection or server errors (D6).
- **Autofill hints, keyboard types and IME actions** on every field (D12).
- **Session interface** in `:core:domain`:
  - the session state: restoring, signed in with the email, signed out with a reason
  - `logout()`, which ends the session locally before the server call
  - `SessionScopedStore`, so in-memory user data is cleared on every sign-out
  The app shell and other features use it. The board list change adds the Log out item and the "Signed in as {email}" text (D2, D13).
- **Auth repository interface** with login and register and typed errors, in `:feature:auth:domain`.
- **Session gate in `:composeApp`**: the start route follows the session state.
  - The platform splash stays while the session restores (D11).
  - A signed-in user goes to the board list.
  - A signed-out user goes to Log in, with the banner if the session expired.
  - The auth screens are cleared from the back stack after a login or register (D8).
  - Logout goes to the plain login screen with no confirmation (D7).
- **Shared text rules** in `:core:domain`: text normalization (NFC, trim), code point length, the email rule and the password rule from the shared specs. Other features can reuse them.
- **Temporary verification**: an in-memory fixture repository and session, plus a debug entry, removed in their own commit before archive (D3).
- **Dependency**: `androidx.core:core-splashscreen`, approved (D11).
- **Docs**: fix the rate-limit lines in the repo's `CLAUDE.md` Authentication section and Open Items, so they match the shared spec (D14).

### Copy

From Figma 9:4, unchanged:
- "Cardigan", "Log in to Cardigan", "Create your account"
- "Email" / "you@example.com", "Password" / "Your password" (log in) / "Create a password" (register), "Repeat password" / "Type it again"
- "Log in", "Logging in…", "Create account"
- "No account yet? Create one" ("Create one" is the link), "Already have an account? Log in" ("Log in" is the link)
- Rules: "12+ characters", "Lowercase letter", "Uppercase letter", "Digit"
- "Email or password is incorrect.", "Your session has expired. Log in again to continue.", "Passwords don’t match.", "An account with this email already exists."

Approved by the user (D5, D6, D19):
- "Enter your email." / "Enter a valid email address." / "Email can't be longer than 254 characters."
- "Enter your password." / "Repeat your password."
- "Creating account…"
- "Too many attempts. Try again in {time}." ({time}: "1 second" / "{n} seconds" under 60 seconds, then "1 minute" / "{n} minutes" rounded up, D19)
- "Couldn't reach Cardigan. Check your connection and try again."

Approved by the user (D17):
- "Password can't be longer than 128 characters."
- "Password can't contain tabs or line breaks."

## Capabilities

### New Capabilities
- `auth-screens`: what the Log in and Register screens do on Android and iOS: fields, validation timing, errors and banners, loading, autofill and keyboard, and the session expired state.
- `session-lifecycle`: how the app moves between restoring, signed in and signed out: the splash at start, where each state lands, logout, a session ending on its own, and the signed-in email for the shell.

### Modified Capabilities
- (none) The screens use the existing design-system components without changing their requirements.

### Shared-spec requirements

This change implements or depends on:
- cardigan-shared-specs `domain-user`:
  - "Email is stored lowercased and identifies one user": the email length limit and sending the email as entered.
  - "Email follows the RFC 5322 address format": the register email check.
  - "Passwords follow one composition rule": the live rules and the register check.
- cardigan-shared-specs `text-length`:
  - "Text is normalized before validation", "Passwords are normalized without trimming", "Length is counted in Unicode code points", "Invalid characters are rejected": the client checks.
  - "Clients validate with the same rule": no request is sent with broken fields.
- cardigan-shared-specs `api-errors`:
  - "Login failures look the same": one wrong-credentials message.
  - "Uniqueness clashes are conflicts": email already registered.
  - "Field error codes", "Validation errors report all broken fields": mapping server field errors.
  - "Clients handle unknown error codes by status", "Error text is for developers only": the generic banner, and never showing server text.
- cardigan-shared-specs `api-auth-tokens`:
  - "Auth endpoints are rate limited", "Rate limits count per email": the 429 banner with `Retry-After`.
  - "Logout ends only the current session": logout discards the session whatever the response.
  - "Clients end the session on a rejected refresh": the session expired state.
  - "Mobile uses the body transport": the session interface's contract for `mob-auth-connect`. Not implemented here.

## Impact

- New code in `:feature:auth:domain`, `:feature:auth:data` (the temporary fixture only), `:feature:auth:presentation`, `:core:domain` (session interface, text rules), `:core:presentation` (auth-independent text mappings if needed) and `:composeApp` (session gate, routes, splash hook).
- `:androidApp`: splash theme and `installSplashScreen`. `iosApp/`: launch screen background color (D16), so the iOS app build is part of the gates.
- New dependency: `androidx.core:core-splashscreen` in the version catalog, used only by `:androidApp`.
- No API, DTO or spec file changes. `api/openapi.json` stays absent until be-auth lands.
- Board list change: it builds the Log out item and the "Signed in as {email}" text on the session interface from this change.
- `CLAUDE.md`: rate-limit wording in Authentication and Open Items.
