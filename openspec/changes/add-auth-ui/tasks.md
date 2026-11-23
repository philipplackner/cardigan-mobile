# Tasks

**Gates:** `./gradlew :androidApp:assembleDebug testAndroidHostTest iosSimulatorArm64Test :composeApp:linkDebugFrameworkIosSimulatorArm64`. Group 6 changes `iosApp/`, so from then on also build the iOS app with `xcodebuild` for the simulator. Commit only when all gates pass (CLAUDE.md Git Workflow).

**Figma workflow for UI tasks:**
1. `create_session` on `MALwbOph7Uj2QtiTy21JaQ`.
2. Load `figma:figma-design-to-code`.
3. Run `get_design_context` on each frame, then check with `get_screenshot`.
4. Read the annotations with a read-only `use_figma` call on `node.annotations`.
5. If a Figma call fails, stop and report the call and node.

Frame IDs come from `get_metadata` on page `9:4`. Don't copy them from this file.

**"Figma check"** means the composable is compared in code with `get_design_context` values (tokens, sizes, spacing, copy, states), with no high-severity difference left. Previews stay in `commonMain` and are never rendered with the Android CLI.

## 1. Core: text helpers and session interface

- [x] 1.1 Check whether add-board-list-ui already added `toNfc()`, `normalizeText(input, multiLine)` and `codePointLength` to `:core:domain`. If not, add them with exactly the names and signatures from its design. Then add (design "Text rules"):
  - `normalizePassword`, `forEachCodePoint`, `hasInvalidCharacter` (or reuse the board list's character check)
  - the `expect`/`actual` `codePointCategory` for Android and iOS

  List the break vectors from design "Testing" first, then write one test per angle. Verify: the tests pass on Android host and the iOS simulator, including `"👨‍👩‍👧"` = 5, decomposed `é` composed, U+1D7D9 as `Digit` on both platforms, and a lone surrogate rejected.
- [x] 1.2 Add `SessionManager` (with the `logout()` ordering in its KDoc), `SessionState`, `SignOutReason` and `SessionScopedStore` to `:core:domain` (design "Session interface"). Verify: compiles for Android and iOS.
- [x] 1.3 Run all gates. Verify they pass, then commit `feat(core): add text rules` (1.1) and `feat(core): add session interface` (1.2) as two commits.

## 2. Auth domain

- [x] 2.1 Add `AuthRepository`, `AuthError`, `AuthFieldError`, `AuthField` and `FieldErrorCode` to `:feature:auth:domain` (design "Auth repository and errors"). Write the success contract (session is `SignedIn` before the call returns) as KDoc on the interface. Verify: compiles.
- [x] 2.2 Add `EmailRule` with adversarial unit tests (design "Testing"). Verify: the tests pass on both targets, including the `domain-user` scenarios "Valid address", "Missing domain", "Non-ASCII address", "Comment in the address" and "Email over the limit". A 255-code-point email gives `TooLong` and not `InvalidFormat`. The 250 KB input finishes in under 100 ms.
- [x] 2.3 Add `PasswordRules` with adversarial unit tests. Verify: the tests pass, including the spec scenarios "Non-ASCII letters and digits", "Emoji counts once" and "Spaces are kept", and lengths 11, 12, 128 and 129.
- [x] 2.4 Add the `LoginForm` and `RegisterForm` validators (one error per field, the error order from design) with unit tests for every field combination in the spec requirement "Submit with errors shows every error". Verify: the tests pass on both targets.
- [x] 2.5 Run all gates. Verify they pass, then commit `feat(auth): add auth domain and form rules`.

## 3. Log in screen

- [x] 3.1 Add the Log in and shared auth strings from proposal "Copy" to `:feature:auth:presentation`, including the seconds and minutes plurals. Add the rate-limit time formatter (D19). Verify: unit tests for 1, 59, 60, 61 and 3600 seconds pass, every Log in string exists once, and no auth text is hard-coded in composables.
- [x] 3.2 Add `LoginRoute(sessionExpired)`, and `LoginViewModel` with State, Action and Event (design "Presentation"). Unit test it with a `FakeAuthRepository`. List the break vectors first:
  - blur timing and the first focus
  - an empty and a whitespace-only email
  - double submit
  - each `AuthError` and `RateLimited(null)`
  - each clearing rule
  - the session expired banner kept while typing and cleared by submit and by "Create one"
  - saved-state restore without the password

  Verify: the tests pass on both targets.
- [x] 3.3 Add the Log in composables and `LoginRoot`:
  - phone and card layouts by `WindowClass`, scroll and `imePadding`
  - Banner variants and the loading button
  - disabled fields and link while loading
  - keyboard options, IME actions and autofill content types (D12)
  - banners announced as a polite live region with their text, and no extra labels (D20)

  Check the autofill API in the catalog's Compose version. If iOS lacks it, write the gap in decisions.md and tell the user. Verify: previews for default, wrong credentials, session expired, rate limited, unavailable and loading, in light and dark at phone and desktop classes, then the Figma check against the Log in, Wrong email or password, Session expired and Logging in frames.
- [x] 3.4 Add `LoginUiTest` (Root + real ViewModel, fake repository). Cover: Done submits, double tap sends one request, fields disabled while loading, the wrong-credentials banner clears on edit, the session expired banner stays while typing and goes on submit, the 429 text for 1 and 12 seconds, and the "Create one" callback. Verify: `iosSimulatorArm64Test` passes.
- [x] 3.5 Run all gates. Verify they pass, then commit `feat(auth): add log in screen`.

## 4. Register screen

- [x] 4.1 Add the Register strings, including the two password messages from D17. Verify: every Register string in proposal "Copy" exists once.
- [x] 4.2 Add `RegisterRoute` and `RegisterViewModel`, with unit tests for:
  - live rule flags on every change
  - errors only after blur
  - mismatch updating when either password changes
  - an empty form on submit showing every error
  - unmet rules on submit with no message
  - over 128 code points and a tab
  - `EmailAlreadyUsed` cleared by an email edit
  - `InvalidFields` mapping for every row of the table in spec "Connection and unexpected errors", `PASSWORD_TOO_WEAK` and other unlisted codes going to the banner, and a mix of mapped and unmapped errors showing both
  - double submit

  Verify: the tests pass on both targets.
- [x] 4.3 Add the Register composables and `RegisterRoot` with the same layout, keyboard and autofill rules as 3.3 (new-password hints). Verify: previews for default, live rules with mismatch, email already registered, an empty submit and loading, in light and dark at phone and desktop classes, then the Figma check against the Register, Live password check and mismatch, and Email already registered frames.
- [x] 4.4 Add `RegisterUiTest`. Cover: rules change while typing, no error before blur, the error after blur, the empty-submit errors, the used-email error clearing on edit, the loading text "Creating account…", and the "Log in" link callback. Verify: `iosSimulatorArm64Test` passes.
- [x] 4.5 Run all gates. Verify they pass, then commit `feat(auth): add register screen`.

## 5. Navigation and session gate

- [ ] 5.1 Add `authGraph(navController)` with the Log in ↔ Register navigation (design "Navigation"), and the auth presentation Koin module. Verify: a UI test that "Create one" opens Register and back returns to Log in with its values kept.
- [ ] 5.2 In `:composeApp`, add `AppViewModel`, which calls `restore()` once and exposes `onLogout()`. Add the start destination and transition handling from design "Navigation and the session gate", with `AppStartRoute` standing in for the board list until the board list change replaces it. On every change to `SignedOut`, `AppViewModel` calls `clear()` on all Koin-bound `SessionScopedStore`s before the gate navigates (design "Clearing user data on sign-out"). Unit test `AppViewModel` with a `FakeSessionManager` and fake stores, including that `clear()` runs for `LoggedOut` and `Expired` and before navigation. Verify: the tests pass.
- [ ] 5.3 Add `SessionGateUiTest` in `:composeApp` with a `FakeSessionManager` and `FakeAuthRepository`. Cover:
  - each start state (`SignedIn`, `NoSession`, `Expired`, and `SignedIn` reported after a restore that couldn't reach the backend, D18)
  - nothing drawn while `Restoring`
  - login → board list, where back leaves the NavHost empty and no auth route remains
  - logout → plain login with a cleared back stack
  - expiry with a transfer dialog open → login with the banner and no dialog

  Verify: `iosSimulatorArm64Test` passes.
- [ ] 5.4 Register the auth modules in `appModules()`. Add a Koin `verify()` test with the session and repository supplied by a test module. Verify: all gates pass, then commit `feat(auth): add session gate and auth navigation`.

## 6. Splash

- [ ] 6.1 Look up the latest stable `androidx.core:core-splashscreen` and add it to the version catalog and `:androidApp`. Add the starting theme with `postSplashScreenTheme`, and `installSplashScreen()` with `setKeepOnScreenCondition` on `isRestoringSession()` exposed by `:composeApp`. Use the launcher icon on `background/app`, with a night variant (D16). Verify: `:androidApp:assembleDebug` passes and the merged manifest uses the starting theme.
- [ ] 6.2 iOS: add the launch background color asset (light and dark) and set it in `UILaunchScreen` with no image (D16). Verify: `xcodebuild` for the simulator passes.
- [ ] 6.3 Run all gates plus `xcodebuild`. Verify they pass, then commit `feat(auth): keep the splash while the session restores`.

## 7. Docs

- [ ] 7.1 Update the repo's `CLAUDE.md` (D14):
  - Authentication: login and register are rate limited per email only, with no IP limit. Refresh and logout are never limited.
  - Open Items: drop "Whether logout gets a rate limit".

  Verify: the text matches `api-auth-tokens` "Auth endpoints are rate limited" and "Rate limits count per email" when read side by side. Then commit `docs: align auth rate limits with the shared spec`.

## 8. Manual verification (temporary)

- [ ] 8.1 Add `InMemorySessionManager` and `FixtureAuthRepository` in `:feature:auth:data/fixture` (design "Temporary fixture"), bound only for debug builds. Add the debug overlay ("Expire session", "Log out") in `:composeApp`. Verify: a unit test for each fixture email, and the Koin `verify()` test for debug and release. Then commit `chore(auth): add temporary fixture session and debug overlay`, with a body saying both are removed before archive (group 9).
- [ ] 8.2 On the Android emulator and the iOS simulator, on a phone and a tablet, in portrait and landscape, and in light and dark, check each spec scenario that the tests can't fully show:
  - the splash at start
  - password manager offers (Android autofill service, iOS Passwords)
  - the keyboard types and Next/Done
  - scrolling above the keyboard in landscape
  - logout and expiry from the debug overlay

  Verify: a short result checklist is added under this task, and every check passes.

## 9. Remove temporary verification code (right before archive)

- [ ] 9.1 Only once `mob-auth-connect` binds the real `SessionManager` and `AuthRepository`: delete the fixture, the overlay and their bindings and tests. Verify: `grep -rn "Fixture\|DebugOverlay" feature/auth composeApp` finds nothing, and all gates pass. Then commit `chore(auth): remove temporary fixture session and debug overlay`.

## Workflow follow-up

- Send the board list change the `SessionManager` contract (state with email, `logout()`, `AppViewModel.onLogout()`), so its menu and sidebar use it (D2, D13). Also send `SessionScopedStore`: its `InMemoryBoardRepository` must bind it, reset `boards` to `null` in `clear()`, and drop results of requests started before the last `clear()`. This matches the board list review's S1 (`BoardRepository.clear()` plus a session counter), so that change can implement both with one `clear()`.
- `mob-auth-connect` implements `SessionManager` in `:core:data` and `AuthRepository` in `:feature:auth:data`, maps HTTP errors to `AuthError`, and stores the email next to the refresh token (D15). Its `logout()` clears local state before the server call (design "Session interface").
- Archive only after `mob-auth-connect` has landed and task 9.1 is done, and only when the user says so.
