# Design

## Context

- `:feature:auth:{domain,data,presentation}` have only build files. `:composeApp` has `App()` with `AppTheme`, the toast host, and a NavHost whose start is the empty `AppStartRoute` plus the transfer dialogs.
- `:core:designsystem` already has every component the Figma frames use: `Brand`, `Field`, `Input` (with `keyboardOptions`, `keyboardActions`, `visualTransformation`, `isError`, `enabled`), `PasswordRule`, `ErrorMessage`, `Banner` (Error, Info), `CardiganButton` (`isLoading`) and `WindowClass`. No new design-system component is needed.
- No auth endpoints exist in the API spec yet (decisions.md Facts). The repository and session are interfaces with fakes. `mob-auth-connect` implements them.
- The text rules from `text-length` and `domain-user` need NFC normalization, code point counting and Unicode categories for code points. Kotlin common code has `Char.category` for UTF-16 units only, and no NFC.
- CLAUDE.md requires the logout call and session clearing to sit behind an interface in `:core:domain`, implemented in `:core:data`.

## Goals / Non-Goals

**Goals:**
- One place that decides what the app shows for each session state (start, login, logout, expiry), so `mob-auth-connect` only has to drive the state.
- Validation logic as pure, platform-independent functions that the ViewModels and tests share.
- Interfaces shaped so `mob-auth-connect` can implement them without changing the presentation layer.

**Non-Goals:**
- Mapping HTTP responses to the error types. Deciding how ProblemDetail `code` values reach the features is still a CLAUDE.md Open Item, settled in `mob-auth-connect`.
- Storing anything across app restarts.

## Decisions

### Session interface in `:core:domain`

```kotlin
interface SessionManager {
    val state: StateFlow<SessionState>
    /** Restores the stored session once per process. Later calls do nothing. */
    suspend fun restore()
    /**
     * Ends the session. Reads the refresh token, then clears the tokens and email from storage
     * and memory and sets `SignedOut(LoggedOut)` before sending the server logout. The server
     * call is best-effort, runs after the state change, and its result is ignored. Never fails.
     */
    suspend fun logout()
}

/** An in-memory store of data loaded for the signed-in user. */
interface SessionScopedStore {
    fun clear()
}

sealed interface SessionState {
    data object Restoring : SessionState
    data class SignedIn(val email: String) : SessionState
    data class SignedOut(val reason: SignOutReason) : SessionState
}

enum class SignOutReason { NoSession, LoggedOut, Expired }
```

- `Expired` covers both the rejected token at start and a session ending while the app is open (D9). `mob-auth-connect` sets it when a refresh fails with `INVALID_REFRESH_TOKEN`, or when the retry after a refresh fails with 401 again.
- `SignedIn.email` is the signed-in email the shell shows (D13). It is the backend's lowercased email. After a restart it comes from secure storage, where `mob-auth-connect` keeps it next to the refresh token and clears it with the tokens (D15). A restore that can't reach the backend still reports `SignedIn` with the stored email (D18).
- The board list change uses `state` for "Signed in as {email}" and `logout()` for the menu item (D2).
- **Clearing user data on sign-out** (spec "Signing out clears user data in memory", CLAUDE.md Authentication): every in-memory store that holds user data binds `SessionScopedStore` in Koin. `AppViewModel` gets them all with `getAll<SessionScopedStore>()` and calls `clear()` on each when the state leaves `SignedIn`, before the gate navigates. A store SHALL drop the result of any request that started before its last `clear()` (for example with a session counter), so a late refresh or save can't write the old user's data back. This change has no such store yet. The board list change's `InMemoryBoardRepository` implements it (Workflow follow-up in tasks.md).
- Alternatives:
  - A separate `LogoutUseCase` and `CurrentUserProvider`: more types, no gain.
  - Putting the email on the auth repository: the shell would depend on `:feature:auth`, which breaks the no-feature-to-feature rule.

### Auth repository and errors in `:feature:auth:domain`

```kotlin
interface AuthRepository {
    suspend fun login(email: String, password: String): EmptyResult<AuthError>
    suspend fun register(email: String, password: String): EmptyResult<AuthError>
}

sealed interface AuthError : Error {
    data object InvalidCredentials : AuthError
    data object EmailAlreadyUsed : AuthError
    data class RateLimited(val retryAfterSeconds: Int?) : AuthError
    data class InvalidFields(val fields: List<AuthFieldError>) : AuthError
    data object Unavailable : AuthError
}

data class AuthFieldError(val field: AuthField, val code: FieldErrorCode)
enum class AuthField { Email, Password, Other }
enum class FieldErrorCode { Required, TooShort, TooLong, InvalidCharacter, InvalidFormat, PasswordTooWeak, Other }
```

- **Contract:** when `login` or `register` succeeds, `SessionManager.state` is already `SignedIn` before the call returns. The app's session gate then navigates (see below). The ViewModels don't navigate on success. They stay in their loading state until the screen leaves.
- **One error type for both calls.** Each ViewModel handles the cases that apply and treats the rest as `Unavailable`. Two types would duplicate four of the five cases.
- **`RateLimited.retryAfterSeconds` is nullable.** `null` means the header was missing or unusable, and the generic banner shows (D19).
- The email and password go to the repository exactly as typed (`domain-user`: the client sends the email as entered). The password isn't normalized on the device before sending, because the backend normalizes it.

### Text rules in `:core:domain`, auth rules in `:feature:auth:domain`

- **`:core:domain` `text` package:** use add-board-list-ui's `expect fun String.toNfc()`, `normalizeText(input, multiLine = false)` and `codePointLength`. If that change hasn't landed, add them with exactly the names, signatures and behavior from its design ("Module layout" and the `normalizeText` rules, including its fixed White_Space list). This change adds only:
  - `normalizePassword(String)`: NFC only, via `toNfc()`.
  - `forEachCodePoint`.
  - `hasInvalidCharacter(String, multiLine)`: unpaired surrogate or U+0000–U+001F, with tab, LF and CR allowed only when `multiLine`.
  If the board list's `validateName` already has an equivalent character check, reuse it instead of adding `hasInvalidCharacter`.
- **NFC** comes from the board list's `toNfc()` (`java.text.Normalizer` on Android, `NSString.precomposedStringWithCanonicalMapping` on iOS). No library.
- **Code point categories:** `expect fun codePointCategory(cp: Int): CodePointCategory` with `Lower`, `Upper`, `Digit` and `Other`.
  - Android: `Character.getType(Int)`, mapped to `LOWERCASE_LETTER`, `UPPERCASE_LETTER` and `DECIMAL_DIGIT_NUMBER`.
  - iOS: `Char.category` for BMP code points. For supplementary code points, `NSCharacterSet.lowercaseLetterCharacterSet`, `uppercaseLetterCharacterSet` and `decimalDigitCharacterSet` with `longCharacterIsMember`. Apple's uppercase set also contains Lt, but every Lt character is in the BMP, so the BMP path gives the exact Ll/Lu/Nd answer.
  - Alternative: BMP only in common code. Rejected, because `𝟙` (U+1D7D9, Nd) and other supplementary letters would disagree with the backend.
- **`:feature:auth:domain`:**
  - `EmailRule.check(raw): EmailProblem?` returns `Required`, `TooLong`, `InvalidFormat` or `null`. `InvalidFormat` also covers invalid characters, which show the same message. The format check is a full match of the reference pattern from `domain-user` "Email format has one reference pattern", copied byte for byte into one constant (`EMAIL_PATTERN`): a dot-atom local part of 1–64 characters, a domain of two or more labels, a last label of 2–63 letters, ASCII only (also "Email local part is a dot-atom" and "Email domain has at least two labels"). The order follows `domain-user` "Register enforces the email format": empty, invalid character, over 254 code points, format. Log in doesn't use it ("Login does not check the email format").
  - **Email field cap (D22):** the Log in and Register ViewModels cut the email value to its first 500 UTF-16 characters on every change, without splitting a surrogate pair. No counter or message. Passwords aren't cut.
  - `PasswordRules.check(raw): PasswordCheck` gives the four live flags, `tooLong`, `invalidCharacter` and `isEmpty`.
  - `LoginForm` and `RegisterForm` validators turn the field values into one error per field. These are pure functions, so the ViewModels and unit tests share them.

### Presentation: one ViewModel per screen

- `LoginViewModel` and `RegisterViewModel` with State, Action and Event, following `kmp-presentation-mvi`.
- **State** holds the field values, one `blurred` flag per field, `submitAttempted`, `isLoading` and a `banner`. The banner is one of `SessionExpired`, `WrongCredentials`, `RateLimited(seconds)` and `Unavailable`. Register also has `emailAlreadyUsed` and server field errors.
- **Visible errors** are derived in the ViewModel: a field's error shows when `blurred || submitAttempted` (D4). The four rule flags are always shown live. The password field's error state shows when its error is visible, or when a rule is unmet and the field is blurred or submitted.
- **Blur tracking:** the composables send `OnFocusChanged(field, focused)`. The ViewModel sets `blurred` the first time a focused field loses focus. A field that never had focus isn't blurred, so the first focus at screen start doesn't count.
- **Submit** sets `submitAttempted`. If any check fails, it stops there and sends no request. If a request is already running, a second submit is ignored (spec "Fields are disabled while a request runs").
- **Clearing rules:**
  - `WrongCredentials` clears on any edit (D10).
  - `emailAlreadyUsed` clears when the email is edited (D10).
  - Server field errors clear when their field is edited.
  - `SessionExpired` and `RateLimited` clear on the next submit (D9, D6).
  - Tapping "Create one" clears `SessionExpired` as well. Log in stays on the back stack under Register, so without this the banner would still show on return. The spec says leaving the screen removes it.
- **Session expired input:** `LoginRoute(sessionExpired: Boolean = false)`. The auth graph reads it with `NavBackStackEntry.toRoute()` and passes it to the ViewModel as a Koin parameter, as the board list does for `RenameBoardRoute`. The ViewModel reads it once. (`SavedStateHandle.toRoute()` needs `android.os.Bundle`, which isn't available in Android host tests.)
- **Saved state:** only the email values, the `blurred` flags and the session-expired banner go into `SavedStateHandle`. Passwords are never written there, because the platform could persist the saved state.
- **Copy:** all strings are resources in `:feature:auth:presentation`. The rate-limit time uses two plurals resources: seconds under 60, and whole minutes rounded up from 60 (D19). A small pure formatter picks the unit and value and is unit tested at 1, 59, 60, 61 and 3600.
- **Field setup** (D12): `KeyboardOptions(keyboardType = Email, imeAction = Next)` on the email field, `Password` + `Next`/`Done` on the password fields, and `PasswordVisualTransformation` with no toggle. Autofill uses Compose `Modifier.semantics { contentType = ContentType.EmailAddress / Password / NewPassword }`. The task checks the exact API in the Compose Multiplatform version in the catalog. If iOS doesn't support it in that version, the task reports this as a platform gap instead of building a workaround. Done calls the submit action.
- **Accessibility** (D20): no new labels. Banners are a polite live region, so their text is read when they appear.
- **Layout:**
  - Phone: the Figma phone frame, top-aligned in a vertical scroll with `imePadding`.
  - Tablet and desktop: the 380dp card, centered in a scroll container.
  - The class comes from `WindowClass`. Input and button sizes follow the existing design-system rule.

### Navigation and the session gate in `:composeApp`

- **Feature navigation:** `:feature:auth:presentation` exposes `LoginRoute`, `RegisterRoute` and `NavGraphBuilder.authGraph(navController)`.
  - "Create one" navigates to `RegisterRoute`.
  - "Log in" on Register pops back to `LoginRoute` if it is on the back stack, and navigates to it otherwise.
  - Nothing in the feature knows about the board list.
- **`AppViewModel`** in `:composeApp` collects `SessionManager.state` and calls `restore()` once in `init`.
- **The NavHost isn't composed while the state is `Restoring`.** `App()` draws only the `background/app` color, and Android keeps the system splash. When the first non-`Restoring` state arrives, the start destination is fixed:
  - `SignedIn` → the board list route (`AppStartRoute` until the board list change replaces it)
  - `SignedOut(NoSession)` → `LoginRoute(false)`
  - `SignedOut(Expired)` → `LoginRoute(true)`
  - A restore that can't reach the backend reports `SignedIn` with the stored email, so it lands on the board list (D18).
- **Later transitions** go through one `LaunchedEffect` on the state:
  - `SignedOut → SignedIn`: navigate to the board list with `popUpTo(graph) { inclusive = true }` (D8).
  - `SignedIn → SignedOut(LoggedOut)`: `LoginRoute(false)`, with the back stack cleared (D7).
  - `SignedIn → SignedOut(Expired)`: `LoginRoute(true)`, with the back stack cleared. Dialog destinations are part of the back stack, so they close too.
- **Logout entry point:** `logout()` is a suspend call on the session interface. The shell calls it from a coroutine scope that outlives the screen (`AppViewModel` exposes `onLogout()`), so leaving the screen can't cancel it.
- **Process death:** the session is in memory, so after a process restart the state is `Restoring` again. The NavHost waits, and the saved back stack is restored only if the restored state is `SignedIn`. Otherwise the transition clears it.
- **Alternative:** each ViewModel emits a "logged in" event and the Root navigates. Rejected, because expiry and logout start outside the auth screens and need the same central handling anyway.

### Splash

- **Android:**
  - `androidx.core:core-splashscreen` goes in the catalog. Look up the latest stable version during the task.
  - `:androidApp` gets a `Theme.Cardigan.Starting` splash theme with `postSplashScreenTheme`, and calls `installSplashScreen()` before `super.onCreate`.
  - `setKeepOnScreenCondition` reads a `isRestoringSession()` function that `:composeApp` exposes, backed by the Koin `SessionManager`.
  - The splash uses the launcher icon on `background/app`, with a night variant (D16).
- **iOS:** the launch screen goes away on the first frame. While `Restoring`, `App()` draws only the background color, and the launch screen's background color matches it (D16: a color asset with a dark variant, used by `UILaunchScreen`, no logo). This changes `iosApp/`, so the `xcodebuild` gate applies.
- **Restore time:** the fake restores at once. With `mob-auth-connect`, the restore lasts as long as one refresh call. Its timeout comes from the HTTP client.

### Temporary fixture and debug entry (D3)

- **`:feature:auth:data`** gets `fixture/InMemorySessionManager` and `fixture/FixtureAuthRepository`, bound in Koin only for debug builds, with the same `isDebug` mechanism add-transfer-ui used. Re-add that mechanism if it was removed. The fixture repository picks its result from the email:
  - `wrong@…` → `InvalidCredentials`
  - `taken@…` → `EmailAlreadyUsed`
  - `limited@…` → `RateLimited(12)`
  - `offline@…` → `Unavailable`
  - anything else → success, after an 800 ms delay so the loading state can be seen
- **`:composeApp`** gets a small debug overlay, shown only when `isDebug` is true and the user is signed in, with "Expire session" and "Log out".
- **Release builds:** there is no `SessionManager` binding in release until `mob-auth-connect`. The Koin `verify()` test supplies one through a test module.
- **Removal:** the fixture and overlay are removed in their own commit before archive. After that, debug builds can't start until `mob-auth-connect` binds the real implementation. So this change is archived only after `mob-auth-connect` has landed (Workflow follow-up).

### Testing

- **Unit tests in `commonTest`, adversarial (break vectors first):**
  - **Text helpers:** CRLF and lone CR, decomposed `é`, whitespace-only text including U+3000, emoji, ZWJ sequences (5 code points), a lone high surrogate, a lone low surrogate, U+0000, a tab in single-line and multi-line mode, and a supplementary Nd/Lu/Ll character.
  - **`EmailRule`:**
    - the scenarios of the `domain-user` email requirements: accepted shapes (`a@b.co`, subdomains, punycode below the top level, a 64-character local part) and rejected ones (single-label domain, domain literal, trailing dot, hyphen at a label edge, bad top-level label, a 65-character local part, misplaced dots, quoted local parts, a comment, two `@`, non-ASCII)
    - 254 and 255 code points, 255 characters without `@` giving `TooLong`
    - a tab, NUL and a lone surrogate inside the address
    - the spec's 250 KB near-match through `EmailRule.check()`, which gives `TooLong` within 100 ms; the pattern itself on 254- and 500-character near-matches, rejected quickly
  - **Email field cap:** a 600-character paste keeps 500, a surrogate pair at the cut isn't split, edits up to 500 are kept, passwords are never cut.
  - **`PasswordRules`:** lengths 11, 12, 128 and 129 in code points, each missing class, non-ASCII classes, astral characters counted once, only spaces, leading and trailing spaces kept, control characters.
  - **ViewModels:** blur timing, first-focus-only, submit with every combination of broken fields, double submit, every `AuthError` case, each clearing rule, the session expired banner kept while typing and removed on submit and on "Create one", and saved-state restore without passwords.
  - **`AppViewModel` and session-gate logic:** each start state, each transition, `restore()` called once.
- **UI tests (`*UiTest`, `runComposeUiTest`)** with Root + real ViewModel and a fake repository and session:
  - `LoginUiTest`: the loading state disables fields; the banners; Done submits; the session expired banner.
  - `RegisterUiTest`: errors after blur; mismatch; the email-used error; the rules update while typing.
  - `SessionGateUiTest` in `:composeApp`: login → board list with back not returning; logout; expiry from a screen with a dialog open; the start state for each restore result.
- **Fakes:** `FakeAuthRepository` and `FakeSessionManager` live in the `commonTest` of each module that needs them, as with the toast controller in add-transfer-ui.

## Risks / Trade-offs

- [The client email check can drift from the backend's] → Both use the one reference pattern from `domain-user` "Email format has one reference pattern", copied byte for byte. The backend stays authoritative, and `INVALID_FORMAT` from the server maps to the same message.
- [The reference email pattern overflows the stack on very long input (found 2026-11-23)] → `java.util.regex` (Android) throws `StackOverflowError` and Kotlin/Native's engine crashes the process (signal 11) on a domain-shaped near-match of about 10,000 characters, because each repetition of the domain-label group adds a recursion level. Up to about 2,000 characters both are fast. So the pattern never sees more than 254 code points: the length check runs first, and the email fields keep at most 500 characters as defense in depth against huge pastes (D22). The spec's 250 KB scenario is tested through `EmailRule.check()`, which gives `TooLong`.
- [Compose autofill semantics may be incomplete on iOS in the catalog's Compose version] → The task checks it and reports a gap instead of adding a native workaround.
- [Debug builds can't start between removing the fixture and `mob-auth-connect`] → The removal commit is made only right before archive, and archive waits for `mob-auth-connect`.
- [The email sits in secure storage next to the refresh token (D15)] → It is cleared with the tokens, and the storage is the same encrypted store CLAUDE.md requires for the token.
- [Passwords aren't in saved state, so they are lost after process death] → Intended. The user types them again.

