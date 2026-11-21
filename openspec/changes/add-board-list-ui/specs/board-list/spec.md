# Spec Delta

## Purpose

Defines how the Android and iOS app shows the user's boards and lets them create, rename and delete boards: the layouts per window class, the list states, refresh, the board dialogs, and the board-related parts of the app shell. Board, key, text and error rules are in cardigan-shared-specs `domain-board`, `text-length`, `api-conventions` and `api-errors`.

## ADDED Requirements

### Requirement: Board list is the start screen
The board list SHALL be the first screen after the app starts with a session. It SHALL load all boards at once and show them in the order the server returned them (cardigan-shared-specs `api-conventions` "Board list order"). The app SHALL NOT re-sort loaded boards.

#### Scenario: App start
- **WHEN** the app starts
- **THEN** the board list shows and loads the user's boards

#### Scenario: Server order kept
- **WHEN** the server returns boards B, A, C
- **THEN** the list shows B, A, C in that order

### Requirement: Layout follows the window class
In the Phone window class, boards SHALL show as a single-column list of rows. In the Tablet and Desktop window classes, they SHALL show as a grid of tiles next to the sidebar, with as many columns as fit at the Board tile's minimum width: 2 columns at a width of 834 dp and 4 columns at 1440 dp. The layout SHALL depend only on the window class and width, never on the device type or platform.

#### Scenario: Phone
- **WHEN** the window is 390 dp wide
- **THEN** boards show as rows below the large "Boards" title

#### Scenario: Tablet portrait
- **WHEN** the window is 834 dp wide
- **THEN** boards show as tiles in 2 columns next to the sidebar

#### Scenario: Wide window
- **WHEN** the window is 1440 dp wide
- **THEN** boards show as tiles in 4 columns next to the sidebar

### Requirement: Board item content
Each row and tile SHALL show the board's name as returned by the server, its key, its card count and its relative updated time. Card counts SHALL read "1 card" or "{n} cards". On tiles, the time SHALL be prefixed with "Updated". Tapping a row or tile SHALL open that board.

#### Scenario: Row content
- **WHEN** a board "Cardigan v1" with key `CRD` and 12 cards was updated 2 hours ago
- **THEN** its phone row shows "Cardigan v1", `CRD`, "12 cards" and "2 hours ago"

#### Scenario: Tile content with one card
- **WHEN** a board has 1 card and was updated yesterday
- **THEN** its tile shows "1 card" and "Updated Yesterday"

#### Scenario: Open a board
- **WHEN** the user taps the row of "Raytracer"
- **THEN** the app opens the board "Raytracer" and passes its id, name and key, so the board's name and key can show before it loads

### Requirement: Relative updated time
The updated time SHALL be shown in the device time zone, relative to the device clock when the list state was built: under 1 minute "Just now"; under 1 hour "1 minute ago" or "{n} minutes ago"; on the same calendar day "1 hour ago" or "{n} hours ago"; on the previous calendar day "Yesterday"; in the same year "MMM d"; otherwise "MMM d, yyyy". The text SHALL NOT update on its own while shown.

#### Scenario: Seconds ago
- **WHEN** a board was updated 40 seconds ago
- **THEN** its time reads "Just now"

#### Scenario: Minutes ago
- **WHEN** a board was updated 59 minutes ago
- **THEN** its time reads "59 minutes ago"

#### Scenario: Earlier today
- **WHEN** it is 18:00 and a board was updated at 09:30 the same day
- **THEN** its time reads "8 hours ago"

#### Scenario: Just after midnight
- **WHEN** it is 00:20 and a board was updated at 23:50 the day before
- **THEN** its time reads "30 minutes ago"

#### Scenario: Previous day
- **WHEN** it is 08:00 and a board was updated at 22:00 the day before
- **THEN** its time reads "Yesterday"

#### Scenario: Earlier this year
- **WHEN** it is 21 Nov 2026 and a board was updated on 9 Oct 2026
- **THEN** its time reads "Oct 9"

#### Scenario: Earlier year
- **WHEN** it is 21 Nov 2026 and a board was updated on 9 Oct 2025
- **THEN** its time reads "Oct 9, 2025"

#### Scenario: Time zone
- **WHEN** a board was updated at 2026-11-20T23:30:00.000Z, the device time zone is UTC+2 and it is 2026-11-21 10:00 local time
- **THEN** its time reads "10 hours ago", because both times fall on 21 Nov in the device time zone

### Requirement: Board count
The phone large title and the tablet/desktop page header SHALL show the board count as "1 board", "{n} boards" or "0 boards". On phones, the list SHALL end with the same count.

#### Scenario: Count at the end of the phone list
- **WHEN** the user scrolls to the end of a phone list of 14 boards
- **THEN** "14 boards" shows below the last row

#### Scenario: One board
- **WHEN** the user has 1 board
- **THEN** the header shows "1 board"

### Requirement: Phone large title collapses
On phones, the top bar SHALL stay pinned. The large "Boards" title SHALL scroll under it. Once the title has scrolled under the bar, "Boards" SHALL show in the bar with a divider. The bar's actions SHALL stay in place.

#### Scenario: Scrolled
- **WHEN** the user scrolls the phone list past the large title
- **THEN** the bar shows "Boards" with a divider below it, and the New board and ⋯ buttons are unchanged

### Requirement: Loading state
While the first load runs, the list SHALL show skeleton rows (phone) or skeleton tiles (tablet/desktop) in place of boards.

#### Scenario: First load
- **WHEN** the board list opens and the boards are still loading
- **THEN** skeletons show where the boards will be

### Requirement: Empty state
When the user has no boards, the list SHALL show the "No boards yet" empty state with the text "Create a board to start adding cards, or import a board from a JSON export.". On phones, the empty state SHALL have a "New board" and an "Import" button.

#### Scenario: No boards on a phone
- **WHEN** the load returns no boards in the Phone window class
- **THEN** "No boards yet" shows with the "New board" and "Import" buttons

#### Scenario: No boards on a tablet
- **WHEN** the load returns no boards in the Tablet window class
- **THEN** "No boards yet" shows, and New board and Import stay in the sidebar and the page header

### Requirement: Load error state
When the first load fails, the list SHALL show "Couldn’t load your boards" with "Check your connection and try again." and a "Try again" button. "Try again" SHALL start a new load and show the loading state.

#### Scenario: Load fails
- **WHEN** the first load fails because there is no network
- **THEN** the error state shows with "Try again"

#### Scenario: Try again
- **WHEN** the user taps "Try again" and the load succeeds
- **THEN** the loading state shows, then the boards

### Requirement: Pull to refresh
On every window class, the user SHALL refresh the list by pulling it down. The app SHALL NOT show a refresh button. While a refresh runs, the indicator with "Refreshing" SHALL show above the list. Pull to refresh SHALL also work in the empty and load error states.

#### Scenario: Refresh on a tablet
- **WHEN** the user pulls down the tile grid in the Tablet window class
- **THEN** the boards reload and the grid shows the new result

#### Scenario: Refresh from the error state
- **WHEN** the load error state shows and the user pulls down
- **THEN** the boards load again

### Requirement: Failed refresh keeps the list
When a refresh fails while boards are shown, the app SHALL keep the shown boards, stop the indicator and show the error toast "Couldn't refresh your boards. Check your connection and try again.".

#### Scenario: Refresh without network
- **WHEN** 6 boards show and a refresh fails
- **THEN** the 6 boards stay and the error toast shows

### Requirement: Board menus
Each row and tile SHALL have a ⋯ button that opens a menu with "Rename" and "Delete board". The ⋯ button's accessible name SHALL be "Actions for board “{name}”".

#### Scenario: Menu button name
- **WHEN** TalkBack or VoiceOver focuses the ⋯ button on the row of "Cardigan v1"
- **THEN** it reads "Actions for board “Cardigan v1”"

#### Scenario: Open the row menu
- **WHEN** the user taps ⋯ on the row of "Cardigan v1"
- **THEN** a menu with "Rename" and "Delete board" opens for that board

### Requirement: New board dialog fields
"New board" (phone bar button, phone empty state, sidebar) SHALL open the New board dialog. It has a Name field and a Key field with the hint "2–5 uppercase letters. Card IDs will look like {KEY}-1. The key can’t be changed later.", the note "New boards start with the columns To do, In progress and Done.", and "Cancel" and "Create board". Lowercase letters typed in the key SHALL show as uppercase.

#### Scenario: Open
- **WHEN** the user taps "New board"
- **THEN** the New board dialog opens with empty fields and "Create board" enabled

#### Scenario: Lowercase key
- **WHEN** the user types `gard` in the Key field
- **THEN** the field shows `GARD`

### Requirement: Key prefill from the name
While the user hasn't edited the Key field, the key SHALL follow the name: diacritics removed, uppercased, only `A`–`Z` kept, first 4 letters. If fewer than 2 letters remain, the key SHALL be empty. After the user edits the Key field, the name SHALL no longer change it.

#### Scenario: Prefill
- **WHEN** the user types "Garden irrigation" as the name
- **THEN** the key is `GARD`

#### Scenario: Diacritics
- **WHEN** the user types "Ärger" as the name
- **THEN** the key is `ARGE`

#### Scenario: Too few letters
- **WHEN** the user types "X 1" as the name
- **THEN** the key is empty

#### Scenario: Edited key is kept
- **WHEN** the user changes the prefilled key to `GI` and then edits the name
- **THEN** the key stays `GI`

### Requirement: Field validation timing
Before the first submit, the dialogs SHALL show no field errors. On submit, the app SHALL check the name per cardigan-shared-specs `text-length` and the key per `domain-board`, in the order of `api-errors` "One field error per field", and SHALL NOT send the request if a check fails. After the first submit, errors SHALL update live while the user types. The primary button SHALL be disabled only while an error is shown or a save runs.

#### Scenario: Empty name on submit
- **WHEN** the user taps "Create board" with an empty name
- **THEN** "Enter a name." shows under Name, no request is sent and "Create board" is disabled

#### Scenario: Live after submit
- **WHEN** "Enter a name." shows and the user types a name
- **THEN** the error disappears and "Create board" is enabled again

#### Scenario: No errors before submit
- **WHEN** the user clears the prefilled key without submitting
- **THEN** no error shows and "Create board" stays enabled

### Requirement: Name field errors
Name errors SHALL read: required "Enter a name."; over the limit "Use at most {n} characters." with `{n}` = 100; a forbidden character "Use a single line without control characters.". The same text SHALL show for the matching server field error, using the server's `max` for `{n}`. An unknown name field error code SHALL show "Use a single line without control characters.".

#### Scenario: Whitespace only
- **WHEN** the name is only spaces and the user submits
- **THEN** "Enter a name." shows

#### Scenario: Too long
- **WHEN** the normalized name has 101 code points and the user submits
- **THEN** "Use at most 100 characters." shows

#### Scenario: Line break
- **WHEN** the pasted name contains a line break and the user submits
- **THEN** "Use a single line without control characters." shows

#### Scenario: Emoji counts as one
- **WHEN** the name is 99 letters plus "👍" and the user submits
- **THEN** no name error shows

### Requirement: Key field errors
Key errors SHALL use the shared key strings: "Enter a key.", "Use at least 2 letters.", "Use at most 5 letters.", "Use only the letters A–Z.". A key that matches a loaded board's key SHALL show "You already have a board with the key {key}." without a request. A server `ALREADY_USED` for `key` SHALL show the same text. Any other key field error code SHALL show "Use only the letters A–Z.".

#### Scenario: Local clash
- **WHEN** a board with key `CRD` is loaded and the user submits a new board with key `CRD`
- **THEN** "You already have a board with the key CRD." shows and no request is sent

#### Scenario: Server clash
- **WHEN** the key isn't in the loaded list but the server answers 409 with `key` `ALREADY_USED`
- **THEN** "You already have a board with the key {key}." shows under Key and the dialog stays open

#### Scenario: Digit in key
- **WHEN** the user submits the key `CR1`
- **THEN** "Use only the letters A–Z." shows

### Requirement: Create board result
After a successful create, the dialog SHALL close, the new board SHALL appear first in the list and the sidebar, and the app SHALL open the new board. No toast SHALL show.

#### Scenario: Created
- **WHEN** the user creates "Garden irrigation" with key `GARD`
- **THEN** the dialog closes, `GARD` is first in the list and the app asks to open it

### Requirement: Rename board dialog
"Rename" SHALL open the Rename board dialog with the current name in the Name field, the key shown read-only with "The key can’t be changed.", and "Cancel" and "Save". If the normalized name equals the current name, Save SHALL close the dialog without a request. A successful rename SHALL update the board in the list and the sidebar, place it by the server's order rule, and show no toast.

#### Scenario: Unchanged name
- **WHEN** the user adds a trailing space to "Raytracer" and taps Save
- **THEN** the dialog closes and no request is sent

#### Scenario: Renamed
- **WHEN** the user renames "Raytracer" to "Path tracer" and the server accepts it
- **THEN** the dialog closes and the list shows "Path tracer" as returned by the server, first in the list

### Requirement: Delete board dialog
"Delete board" SHALL open a confirmation titled "Delete board “{name}”?". Its body SHALL be "Its {n} cards will be deleted too. This can’t be undone.", "Its 1 card will be deleted too. This can’t be undone." for one card, or only "This can’t be undone." for no cards. A successful delete SHALL remove the board from the list and the sidebar and show no toast.

#### Scenario: Board with cards
- **WHEN** the user chooses "Delete board" for "Cardigan v1" with 12 cards
- **THEN** the dialog reads "Delete board “Cardigan v1”?" and "Its 12 cards will be deleted too. This can’t be undone."

#### Scenario: Board without cards
- **WHEN** the board has 0 cards
- **THEN** the body reads "This can’t be undone."

#### Scenario: Deleted
- **WHEN** the user confirms and the server answers 204
- **THEN** the dialog closes and the board is gone from the list and the sidebar

### Requirement: Board dialogs report their result
The Rename board and Delete board dialogs SHALL report their result to the screen that opened them: a successful rename reports the new name as returned by the server, a successful delete (including a delete that got 404) reports that the board was deleted. Cancel, an unchanged rename and a rename that got 404 SHALL report no result.

#### Scenario: Rename from another screen
- **WHEN** the board view opens Rename board and the user renames "Raytracer" to "Path tracer"
- **THEN** the board view receives the new name "Path tracer"

#### Scenario: Delete from another screen
- **WHEN** the board view opens Delete board and the delete succeeds
- **THEN** the board view receives that the board was deleted, and no toast shows

### Requirement: Dialogs while saving
While a create, rename or delete request runs, the primary button SHALL show its spinner, the fields and "Cancel" SHALL be disabled, and the dialog SHALL NOT close by back, tapping outside or any other dismiss.

#### Scenario: Back while saving
- **WHEN** "Create board" is saving and the user does the Android back gesture
- **THEN** the dialog stays open and the request continues

### Requirement: Failed saves keep the dialog open
When a create, rename or delete fails because of the network, a 5xx response or an error code without a specific message, the dialog SHALL stay open with its input and show the error toast: "Couldn't create the board. Check your connection and try again.", "Couldn't rename “{name}”. Check your connection and try again." or "Couldn't delete “{name}”. Check your connection and try again.". A failed create SHALL NOT be repeated automatically.

#### Scenario: Create without network
- **WHEN** creating "Garden irrigation" fails because there is no network
- **THEN** the dialog stays open with the name and key, and the create error toast shows

### Requirement: Board no longer exists
A rename that gets 404 `NOT_FOUND` SHALL close the dialog, remove the board from the list and the sidebar, and show the error toast "“{name}” no longer exists.". A delete that gets 404 `NOT_FOUND` SHALL be handled as a successful delete.

#### Scenario: Rename a deleted board
- **WHEN** the user renames "Raytracer", which was deleted on another device
- **THEN** the dialog closes, "Raytracer" disappears and the toast "“Raytracer” no longer exists." shows

#### Scenario: Delete a deleted board
- **WHEN** the user deletes "Raytracer" and the server answers 404
- **THEN** the dialog closes and "Raytracer" disappears without an error

### Requirement: Undrawn dialog sizes
In the Phone window class, Rename board and the New board key error SHALL use the phone New board dialog layout. In the Tablet window class, all board dialogs SHALL use the desktop dialog layout.

#### Scenario: Phone rename
- **WHEN** the user opens Rename board on a phone
- **THEN** it uses the same dialog layout as New board on a phone

### Requirement: Sidebar board section
In the Tablet and Desktop window classes, the sidebar SHALL list the loaded boards by name and key in the list's order, with "All boards" selected on the board list, and a "New board" entry that opens the New board dialog. Tapping a board SHALL open it. The section SHALL reflect every create, rename, delete, refresh and import.

#### Scenario: Board created
- **WHEN** the user creates a board on a tablet
- **THEN** the sidebar shows it first in its board section

### Requirement: Phone menu
On phones, the bar's ⋯ button SHALL open a menu with "Signed in as {email}" at the top, then "Import board" and "Labels", a divider, and "Log out". "Import board" SHALL start the import flow. "Log out" SHALL log out without a confirmation. When no email is known, the "Signed in as" line SHALL be left out.

#### Scenario: Menu content
- **WHEN** the user is signed in as `ada@example.com` and taps ⋯ in the phone bar
- **THEN** the menu shows "Signed in as ada@example.com", "Import board", "Labels" and "Log out"

#### Scenario: Import from the menu
- **WHEN** the user chooses "Import board"
- **THEN** the system file picker opens (add-transfer-ui)

### Requirement: Sidebar account
In the Tablet and Desktop window classes, the sidebar SHALL show the account email with its avatar initial and a Log out button. Log out SHALL log out without a confirmation. When no email is known, the email and avatar SHALL be left out.

#### Scenario: Log out on a tablet
- **WHEN** the user taps Log out in the sidebar
- **THEN** the app logs out without asking first

### Requirement: Import entry and reload
In the Tablet and Desktop window classes, the page header SHALL have an "Import" button that starts the import flow. After an import ends successfully or with an unknown outcome, the board list and the sidebar SHALL reload.

#### Scenario: Import from the header
- **WHEN** the user taps "Import" in the page header
- **THEN** the system file picker opens

#### Scenario: After import
- **WHEN** an import succeeds
- **THEN** the board list reloads and shows the imported board

### Requirement: Accessible board items and actions
Every row, tile, ⋯ button, menu item and bar button SHALL have an accessible name from string resources and a touch area of at least 48 dp. A row or tile SHALL read as "{name}, {key}, {cards}, updated {time}". The phone bar ⋯ button SHALL read "Menu".

#### Scenario: Screen reader on a row
- **WHEN** TalkBack or VoiceOver focuses the row of "Cardigan v1"
- **THEN** it reads "Cardigan v1, CRD, 12 cards, updated 2 hours ago"
