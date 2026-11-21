# Spec Delta

## Purpose

Defines how the Android and iOS app moves between restoring, signed in and signed out: what shows at app start, where each state lands, logout, a session that ends on its own, and the signed-in email other screens show.

## ADDED Requirements

### Requirement: Splash stays while the session restores
At app start, the app SHALL keep showing its launch splash until it knows whether a session exists. It SHALL NOT show the login screen or the board list before then.

#### Scenario: Restore succeeds
- **WHEN** the app starts with a session that restores successfully
- **THEN** the splash stays until the restore finishes and the board list is the first screen shown

#### Scenario: No stored session
- **WHEN** the app starts with no stored session
- **THEN** the login screen is the first screen shown, without a banner

### Requirement: Rejected session at start shows the expired banner
When the stored session is rejected at app start (`api-auth-tokens` "Clients end the session on a rejected refresh"), the app SHALL open the login screen with the session expired banner.

#### Scenario: Idle for 31 days
- **WHEN** the app starts and the stored refresh token is rejected with 401 `INVALID_REFRESH_TOKEN`
- **THEN** the login screen shows "Your session has expired. Log in again to continue."

### Requirement: Restore failure that isn't a rejection
When the restore at app start fails for another reason, such as no connection or a 5xx, the app SHALL keep the stored session, as `api-auth-tokens` "Other refresh failures are temporary" requires, and SHALL open the board list.

#### Scenario: Offline start
- **WHEN** the app starts offline with a stored session
- **THEN** the session is kept and the board list opens, where its own load error applies

### Requirement: Successful login or register lands on the board list
After a successful login or register, the app SHALL show the board list. The Log in and Register screens SHALL be removed from the back stack, so the back action on the board list doesn't return to them. Register SHALL sign the user in without a separate login.

#### Scenario: After register
- **WHEN** register succeeds
- **THEN** the board list shows and the user is signed in

#### Scenario: Back after login
- **WHEN** the user logs in and then presses back on the board list
- **THEN** the app is left as the platform does, and the login screen doesn't show

### Requirement: Logout needs no confirmation
When the user chooses Log out, the app SHALL end the session at once, with no confirmation. It SHALL show the login screen without a banner, whatever the server answers, and SHALL clear the back stack.

#### Scenario: Logout offline
- **WHEN** the user logs out without a connection
- **THEN** the login screen shows without a banner, and the app no longer has the session

#### Scenario: Back after logout
- **WHEN** the user logs out and presses back on the login screen
- **THEN** no signed-in screen shows

### Requirement: Session ending on its own
When the session ends without a logout while the app is open (`api-auth-tokens` "Clients end the session on a rejected refresh"), the app SHALL leave the current screen, including any open dialog, clear the back stack and show the login screen with the session expired banner.

#### Scenario: Expired while on the board view
- **WHEN** the session ends without a logout while a board is open
- **THEN** the login screen shows with the session expired banner and back doesn't return to the board

### Requirement: Signed-in email for other screens
While signed in, the app SHALL make the signed-in email available to other screens, taken from the session and not from a separate request. It SHALL be the email the backend returned, which is lowercased. After a logout or an expired session, no email SHALL be available.

#### Scenario: Email after login
- **WHEN** the user logs in as `Ann@Example.com`
- **THEN** the email available to other screens is `ann@example.com`

#### Scenario: Email after a restart
- **WHEN** the app restarts and the stored session restores, or the restore can't reach the backend
- **THEN** the same email is available without another request

#### Scenario: Email after logout
- **WHEN** the user logs out
- **THEN** no signed-in email is available
