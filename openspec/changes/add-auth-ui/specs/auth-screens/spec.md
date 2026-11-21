# Spec Delta

## Purpose

Defines how the Log in and Register screens behave on Android and iOS: which checks run on the device and when, how server errors and rate limits appear, loading, keyboard and autofill, and the session expired state.

## ADDED Requirements

### Requirement: Login checks only for empty fields
Before sending a login, the app SHALL check only that the email is not empty after `text-length` normalization and that the password is not empty. It SHALL send the email as entered and the password unchanged. The app SHALL NOT check the email format or the password rules on login.

#### Scenario: Malformed email on login
- **WHEN** the user logs in with the email `ann@` and a non-empty password
- **THEN** the login request is sent

#### Scenario: Whitespace-only email on login
- **WHEN** the user submits the login form with the email `"   "`
- **THEN** no request is sent and the email field shows "Enter your email."

#### Scenario: Empty password on login
- **WHEN** the user submits the login form with an empty password
- **THEN** no request is sent and the password field shows "Enter your password."

### Requirement: Register checks the shared email rule
On register, the app SHALL check the email with the `domain-user` rules ("Email is stored lowercased and identifies one user", "Email follows the RFC 5322 address format") after `text-length` normalization. An empty email SHALL show "Enter your email.". An email over 254 code points SHALL show "Email can't be longer than 254 characters.". Any other broken email SHALL show "Enter a valid email address.".

#### Scenario: Empty register email
- **WHEN** the register email is empty or only whitespace and the field has lost focus
- **THEN** it shows "Enter your email."

#### Scenario: Missing domain
- **WHEN** the register email is `ann@` and the field has lost focus
- **THEN** it shows "Enter a valid email address."

#### Scenario: Non-ASCII email
- **WHEN** the register email is `jürgen@münchen.de` and the field has lost focus
- **THEN** it shows "Enter a valid email address."

#### Scenario: 255 code points
- **WHEN** the register email has a normalized length of 255
- **THEN** it shows "Email can't be longer than 254 characters." and not the format message

#### Scenario: Surrounding spaces
- **WHEN** the register email is `  Ann@Example.com `
- **THEN** it is valid, and the request carries it as entered

### Requirement: Password rules update live
The Register screen SHALL show the four rules "12+ characters", "Lowercase letter", "Uppercase letter" and "Digit". Each SHALL show as met or unmet, and SHALL update with every change to the password. The rules SHALL follow `domain-user` "Passwords follow one composition rule": 12 or more code points after NFC, a Unicode Ll, a Unicode Lu and a Unicode Nd character.

#### Scenario: Typing completes the rules
- **WHEN** the user types `Correct horse 7` one character at a time
- **THEN** "Lowercase letter" and "Uppercase letter" are met from the second character on, "Digit" is met after the last character, and "12+ characters" is met from the twelfth character on

#### Scenario: Non-ASCII letters and digits
- **WHEN** the password is `äöüÄÖÜ١٢٣ßßßß`
- **THEN** all four rules are met

#### Scenario: Emoji counts once
- **WHEN** the password is `Abcdefghij1` followed by one emoji
- **THEN** "12+ characters" is met

#### Scenario: Spaces are kept
- **WHEN** the password is ` Secret Pass1 ` with a leading and trailing space
- **THEN** its length is 14 and "12+ characters" is met

### Requirement: Empty register password
An empty register password SHALL show "Enter your password." under the password field, in addition to the unmet rules.

#### Scenario: Password left empty
- **WHEN** the user focuses the register password field and leaves it empty
- **THEN** the field shows "Enter your password." and all four rules show as unmet

### Requirement: Register blocks passwords the rules can't show
The app SHALL NOT send a register request whose password has more than 128 code points after NFC or contains a control character (U+0000–U+001F). It SHALL show "Password can't be longer than 128 characters." or "Password can't contain tabs or line breaks." under the password field, following the timing in "Field errors appear after the first blur".

#### Scenario: Password over the limit
- **WHEN** the register password has 129 code points and every other field is valid
- **THEN** no request is sent and the password field shows "Password can't be longer than 128 characters."

#### Scenario: Pasted tab
- **WHEN** the register password contains a tab
- **THEN** no request is sent and the password field shows "Password can't contain tabs or line breaks."

### Requirement: Repeat password must match
On register, an empty repeat field SHALL show "Repeat your password.". A repeat that differs from the password SHALL show "Passwords don’t match.". The comparison SHALL be on the exact text the user typed.

#### Scenario: Mismatch
- **WHEN** the password is `Correct horse 7`, the repeat is `Correct horse 8`, and the repeat field has lost focus
- **THEN** the repeat field shows "Passwords don’t match." in its error state

#### Scenario: Password changed after repeat
- **WHEN** the repeat field already showed its error once and the user then changes the password so it equals the repeat
- **THEN** the mismatch message goes away

### Requirement: Field errors appear after the first blur
A field's error SHALL NOT show while the user types in it for the first time. The error SHALL show when the field loses focus for the first time, and from then on SHALL update with every change. The four password rules are excepted: they always update live.

#### Scenario: Typing an email for the first time
- **WHEN** the user types `ann@` into the register email field and focus is still in it
- **THEN** no email error is shown

#### Scenario: Leaving the field
- **WHEN** the user moves focus from the register email field holding `ann@` to the password field
- **THEN** the email field shows "Enter a valid email address."

#### Scenario: Fixing after the blur
- **WHEN** the email field shows its error and the user types until it reads `ann@example.com`
- **THEN** the error goes away while the user is still typing

### Requirement: Submit with errors shows every error
The submit button SHALL stay enabled whatever the form contains. When the user submits a form that breaks any check, the app SHALL send no request and SHALL show the error of every broken field, including fields that never lost focus. When the password rules are unmet, the password field SHALL show its error state and the unmet rules SHALL show as unmet, with no extra message.

#### Scenario: Submit an empty register form
- **WHEN** the user taps "Create account" on an empty Register screen
- **THEN** no request is sent, the email field shows "Enter your email.", the password field shows "Enter your password." in its error state with all four rules unmet, and the repeat field shows "Repeat your password."

#### Scenario: Unmet rule on submit
- **WHEN** the password meets every rule but "Digit" and the user submits
- **THEN** no request is sent, the password field is in its error state, and no message is shown under the rules

### Requirement: Fields are disabled while a request runs
While a login or register request runs, every field and link SHALL be disabled. The button SHALL show its loading state with "Logging in…" or "Creating account…". A second submit SHALL NOT send a second request.

#### Scenario: Double tap
- **WHEN** the user taps "Log in" twice quickly with a valid form
- **THEN** exactly one login request is sent

#### Scenario: Loading state
- **WHEN** a register request is running
- **THEN** the button reads "Creating account…" with its loading indicator and no field accepts input

### Requirement: Wrong credentials keep the entered values
When login fails with wrong credentials, the screen SHALL show the error banner "Email or password is incorrect." and put the password field in its error state. Both values SHALL stay. The banner and the error state SHALL go away on the next edit of either field.

#### Scenario: Wrong password
- **WHEN** login fails with 401 `INVALID_CREDENTIALS`
- **THEN** the banner shows, the email and password stay filled in, and the password field is in its error state

#### Scenario: Editing clears the banner
- **WHEN** the wrong-credentials banner is shown and the user types one character in the email field
- **THEN** the banner and the password error state go away

### Requirement: Email already registered
When register fails because the email is used, the email field SHALL show "An account with this email already exists." in its error state. The other fields SHALL keep their values. The message SHALL go away when the email is edited. The screen SHALL NOT offer a separate "Log in instead" action.

#### Scenario: Used email
- **WHEN** register fails with 409 `CONFLICT` and a field error `email`/`ALREADY_USED`
- **THEN** the email field shows "An account with this email already exists." and the passwords stay filled in

#### Scenario: Different email
- **WHEN** the used-email message is shown and the user changes the email
- **THEN** the message goes away

### Requirement: Rate limit banner
When login or register fails with 429 `RATE_LIMITED`, the screen SHALL show the error banner "Too many attempts. Try again in {time}.". {time} SHALL come from `Retry-After`: under 60 seconds as "1 second" or "{n} seconds", from 60 seconds as whole minutes rounded up, "1 minute" or "{n} minutes". The banner SHALL NOT count down. The button SHALL stay enabled. The banner SHALL go away on the next submit.

#### Scenario: Twelve seconds
- **WHEN** login fails with 429 and `Retry-After: 12`
- **THEN** the banner reads "Too many attempts. Try again in 12 seconds."

#### Scenario: One second
- **WHEN** register fails with 429 and `Retry-After: 1`
- **THEN** the banner reads "Too many attempts. Try again in 1 second."

#### Scenario: Exactly one minute
- **WHEN** login fails with 429 and `Retry-After: 60`
- **THEN** the banner reads "Too many attempts. Try again in 1 minute."

#### Scenario: Rounded up
- **WHEN** login fails with 429 and `Retry-After: 61`
- **THEN** the banner reads "Too many attempts. Try again in 2 minutes."

#### Scenario: Missing header
- **WHEN** a 429 response has no usable `Retry-After`
- **THEN** the generic connection banner shows instead

### Requirement: Connection and unexpected errors
When login or register fails with no response, a 5xx, or any error not covered by another requirement, the screen SHALL show the error banner "Couldn't reach Cardigan. Check your connection and try again.". The app SHALL NOT show the server's `title` or `detail`. A 400 `VALIDATION_FAILED` SHALL show the matching field messages for known fields and codes, and the generic banner when none match.

#### Scenario: Offline
- **WHEN** the device has no connection and the user logs in
- **THEN** the generic banner shows and the entered values stay

#### Scenario: Server validation for a known field
- **WHEN** register fails with 400 `VALIDATION_FAILED` and a field error `email`/`INVALID_FORMAT`
- **THEN** the email field shows "Enter a valid email address." and no banner shows

#### Scenario: Unknown code
- **WHEN** login fails with a 409 whose code the app doesn't know
- **THEN** the generic banner shows

### Requirement: Session expired banner
When the login screen opens because the session ended without a logout, it SHALL show the info banner "Your session has expired. Log in again to continue." with empty fields. The banner SHALL stay until the user submits or leaves the screen.

#### Scenario: Expired session
- **WHEN** the login screen opens after the session expired
- **THEN** the info banner shows and both fields are empty

#### Scenario: Typing keeps the banner
- **WHEN** the session expired banner shows and the user types an email
- **THEN** the banner stays

#### Scenario: Submit removes it
- **WHEN** the session expired banner shows and the user submits
- **THEN** the banner goes away, whatever the result

### Requirement: Keyboard and autofill
The email field SHALL use an email keyboard and the email autofill hint. The login password SHALL use the password hint. The register password fields SHALL use the new-password hint. Every field except the last SHALL move focus to the next field with the keyboard's Next action. The last field's Done action SHALL submit the form. Passwords SHALL always be masked, with no option to show them.

#### Scenario: Done on login
- **WHEN** focus is in the login password field and the user presses Done
- **THEN** the form is submitted as if "Log in" was tapped

#### Scenario: Password manager
- **WHEN** a password manager is set up on the device and the user focuses the login email field
- **THEN** the system offers saved credentials for the app

### Requirement: Switching between Log in and Register
"Create one" on the Log in screen SHALL open Register. "Log in" on the Register screen SHALL open Log in. The back action on Register SHALL return to Log in. On Log in, the back action SHALL leave the app as the platform does.

#### Scenario: To register and back
- **WHEN** the user taps "Create one" and then goes back
- **THEN** the Log in screen shows again with the values it had

### Requirement: Layout follows the window class
The screens SHALL use the phone layout from Figma in the phone window class, and the centered card layout in the tablet and desktop classes. The content SHALL scroll when it doesn't fit, so the focused field and the button stay reachable above the keyboard.

#### Scenario: Landscape phone with keyboard
- **WHEN** the Register screen is open on a phone in landscape and the keyboard covers half the window
- **THEN** the user can scroll to every field and to "Create account"
