# Spec Delta

## Purpose

Defines how the mobile app shows one board and lets the user add cards and manage its columns on Android and iOS phones and tablets: layouts, states, refresh, quick add, the column and board menus, and how writes and failures behave.

## ADDED Requirements

### Requirement: Board content area
The board view SHALL show the board's name, key and card count, and the board's columns in position order. Each column SHALL show its name, its card count, a ⋯ menu button, its cards in position order and an "Add card" button. Counts SHALL use plural forms. The board view SHALL render only its content area. The sidebar and phone menu belong to the app shell.

#### Scenario: Board with columns and cards
- **WHEN** the user opens a board with the key `CRD`, 4 columns and 12 cards
- **THEN** the header shows the board name, `CRD` and "12 cards", and the 4 columns show in position order with their cards in position order

#### Scenario: Single card
- **WHEN** the board has exactly one card
- **THEN** the header shows "1 card"

### Requirement: Header before the board has loaded
A board with 0 cards SHALL show only its name and key in the header, without a count. While the board loads and when the load fails, the header SHALL show the name and key the board list already has for this board, or no title if it has none. The board ⋯ button SHALL be hidden until the board has loaded.

#### Scenario: Empty board
- **WHEN** the user opens a board with columns but no cards
- **THEN** the header shows the name and key and no card count

#### Scenario: Opened from the board list
- **WHEN** the user opens a board from the board list and the load is still running
- **THEN** the header shows that board's name and key, and no board ⋯ button

#### Scenario: No board list data
- **WHEN** the board view opens without board list data for the board and the load fails
- **THEN** the header shows no title and no board ⋯ button

### Requirement: Card tiles
Each card tile SHALL show the card's display ID, its title and its labels as chips with the label's color and name, sorted as defined in cardigan-shared-specs `domain-card` "Card labels are a set sorted by name". Tapping a tile SHALL report the card to the host, which opens card detail.

#### Scenario: Tile content
- **WHEN** card 124 on the board `CRD` carries the labels `web`, `design` and `mobile`
- **THEN** its tile shows `CRD-124`, its title and the chips `design`, `mobile`, `web`

#### Scenario: Tap a card
- **WHEN** the user taps a card tile
- **THEN** the board view reports that card's ID to the host

### Requirement: Layout per window class
On the phone window class, columns SHALL be 300 dp wide in a free horizontal scroll without snapping, with the next column partly visible. Tablet and desktop window classes SHALL use the desktop layout, including the desktop variant of every state that has no tablet frame. The layout SHALL follow the window class, never the device type.

#### Scenario: Phone swipe
- **WHEN** the user swipes sideways on a phone and lifts the finger between two columns
- **THEN** the strip stops where the fling ends, without snapping to a column

#### Scenario: Tablet window on a phone-class width
- **WHEN** a tablet app window is resized to a width of the phone window class
- **THEN** the phone layout is used

### Requirement: Loading and load failure
While the board loads for the first time, the view SHALL show the loading skeleton. When the load fails for a reason other than a missing board, the view SHALL show "Couldn’t load this board" / "Check your connection and try again." with a "Try again" button that loads again.

#### Scenario: Load fails
- **WHEN** the first load fails with a network error
- **THEN** the error state shows, and "Try again" starts a new load with the loading skeleton

### Requirement: Empty board and empty column
A board without columns SHALL show "This board has no columns" / "Add a column to start adding cards." with an "Add column" button. A column without cards SHALL show "No cards yet" above its "Add card" button.

#### Scenario: All columns deleted
- **WHEN** the user deletes the last column of a board
- **THEN** the empty board state shows

#### Scenario: New board
- **WHEN** the user opens a new board with the default columns
- **THEN** each column shows "No cards yet"

### Requirement: Missing board
When a load or a write reports that the board no longer exists, the app SHALL leave the board view, show the board list and show the toast "This board no longer exists."

#### Scenario: Board deleted on another device
- **WHEN** the user opens a board that was deleted on another device
- **THEN** the board list shows with the toast "This board no longer exists."

### Requirement: Pull to refresh
Pulling down any column's card list SHALL reload the whole board, on every window class. There SHALL be no refresh button. When the reload fails, the board SHALL stay as it was and the toast "Couldn't refresh the board. Check your connection and try again." SHALL show.

#### Scenario: Refresh picks up changes
- **WHEN** a card was added on another device and the user pulls down a column
- **THEN** the board reloads and shows the new card

#### Scenario: Refresh fails
- **WHEN** the user pulls to refresh without a connection
- **THEN** the board stays as shown and the refresh error toast shows

#### Scenario: Open field keeps its text
- **WHEN** a quick add field in "To do" holds "Write docs" and the user pulls to refresh
- **THEN** after the reload the field is still open with "Write docs"

#### Scenario: Field's column is gone
- **WHEN** a quick add field is open in a column that was deleted on another device and the user pulls to refresh
- **THEN** after the reload the field is closed

### Requirement: Writes wait for the server
Adding, renaming, moving and deleting columns and adding cards SHALL change the shown board only after the server confirms the write. The confirmed result SHALL be applied to the shown board without reloading it. The app SHALL NOT repeat a create automatically after a failure.

#### Scenario: Column moved
- **WHEN** the user picks "Move right" on the second of four columns
- **THEN** the columns keep their order until the server confirms, then the column shows third, with no full reload

#### Scenario: No automatic repeat
- **WHEN** adding a card fails with a network error
- **THEN** the app sends no second create request on its own

### Requirement: Saving state
While a write runs from a dialog or an inline field, its confirm button SHALL show a spinner, its fields and Cancel SHALL be disabled, and it SHALL NOT be dismissible. Enter SHALL NOT submit again.

#### Scenario: Rename in progress
- **WHEN** the user saves a column rename and the request is running
- **THEN** "Save" shows a spinner, the field and "Cancel" are disabled, and back or a tap outside does not close the dialog

### Requirement: Write failures
When a write fails with a network error, a server error or an error code the app doesn't know, the dialog or inline field SHALL stay open with the user's input and an error toast SHALL show: "Couldn't add the column. …", "Couldn't rename “{name}”. …", "Couldn't move “{name}”. …", "Couldn't delete “{name}”. …" or "Couldn't add the card. …", each ending in "Check your connection and try again." Field errors from the server SHALL show under the field. No success toast SHALL show.

#### Scenario: Add column fails
- **WHEN** adding the column "Blocked" fails with a server error
- **THEN** the field still holds "Blocked" and the toast "Couldn't add the column. Check your connection and try again." shows

#### Scenario: Server field error
- **WHEN** a rename is rejected with a field error for the name
- **THEN** the matching name error shows under the field and the dialog stays open

### Requirement: Stale column
When a column move is rejected as out of range, the app SHALL reload the board and show the toast "Couldn't move “{name}”. The board changed on another device, so it was reloaded." When a write reports that its column no longer exists, the app SHALL reload the board and show the toast "“{name}” no longer exists. The board was reloaded." A delete that reports the column as missing SHALL count as done.

#### Scenario: Stale move
- **WHEN** the user moves "Review" right after another device has changed the board's columns, and the move is rejected as out of range
- **THEN** the board reloads and the toast "Couldn't move “Review”. The board changed on another device, so it was reloaded." shows

#### Scenario: Column deleted elsewhere
- **WHEN** the user renames a column that was deleted on another device
- **THEN** the dialog closes, the board reloads and the stale column toast shows

#### Scenario: Delete of a missing column
- **WHEN** the user deletes a column that was already deleted on another device
- **THEN** the column disappears from the board and no error shows

### Requirement: Name validation
Column names and card titles, including the quick add field, SHALL be checked on the device with the rules of cardigan-shared-specs `text-length` and the field limits (column name 1–30, card title 1–200) before a request is sent. Errors SHALL show under the field after the first submit and then update while typing: "Enter a name.", "Use at most {n} characters.", "Use a single line without control characters." The confirm button SHALL be disabled only while an error shows or a save runs.

#### Scenario: Too long
- **WHEN** the user submits a column name with 31 code points after trimming
- **THEN** "Use at most 30 characters." shows, no request is sent, and the error clears once the name is short enough

#### Scenario: Pasted line break in quick add
- **WHEN** the user pastes "Fix\nlogin" into a quick add field and presses "Add card"
- **THEN** "Use a single line without control characters." shows under the field and no request is sent

#### Scenario: Error before first submit
- **WHEN** the user types an over-long name but hasn't submitted yet
- **THEN** no error shows and the confirm button is enabled

### Requirement: Quick add card
"Add card" SHALL open a title field at the bottom of its column. Enter or "Add card" SHALL create the card at the bottom of the column. After success the field SHALL stay open, empty and focused. Empty or whitespace-only input SHALL do nothing. Cancel or back SHALL close the field and drop its text without asking. A tap outside SHALL keep the field open if it has text and close it if it is empty.

#### Scenario: Add two cards in a row
- **WHEN** the user types "Write docs", presses Enter, then types "Ship it" and presses Enter
- **THEN** both cards appear at the bottom of the column in that order, each after the server confirms it, and the field is open and empty

#### Scenario: Whitespace only
- **WHEN** the user presses Enter in a field that holds only spaces
- **THEN** nothing is sent and the field stays open

### Requirement: One inline field at a time
Only one inline field (a quick add field or the add column field) SHALL be open at a time. Opening another one SHALL close an empty field. If the open field holds text, the "Discard changes?" dialog SHALL ask first. Cancel or back in the field SHALL still drop the text without asking. While the open field is saving, opening another inline field SHALL do nothing. Opening a menu or a dialog SHALL NOT close an open field.

#### Scenario: Second column, empty field
- **WHEN** an empty quick add field is open in "To do" and the user taps "Add card" in "Done"
- **THEN** the field in "To do" closes and the field in "Done" opens

#### Scenario: Second column, field with text
- **WHEN** the quick add field in "To do" holds "Write docs" and the user taps "Add card" in "Done"
- **THEN** the dialog "Discard changes?" / "The card you started in “To do” hasn’t been added." with "Keep editing" and "Discard" shows

#### Scenario: Keep editing
- **WHEN** the discard dialog shows and the user taps "Keep editing", goes back, or taps outside the dialog
- **THEN** the dialog closes, the field in "Done" doesn't open, and the field in "To do" keeps "Write docs", scrolls into view and has focus

#### Scenario: Discard
- **WHEN** the discard dialog shows and the user taps "Discard"
- **THEN** "Write docs" is dropped, the field in "To do" closes, and the field in "Done" opens with focus

#### Scenario: Add column field with text
- **WHEN** the add column field holds "Blocked" and the user taps "Add card" in a column
- **THEN** the dialog shows with the body "The column you started hasn’t been added."

#### Scenario: Quick add to add column
- **WHEN** a quick add field holds text and the user taps "Add column"
- **THEN** the discard dialog for that card shows

#### Scenario: Switch while saving
- **WHEN** the quick add field in "To do" is saving and the user taps "Add card" in "Done"
- **THEN** nothing happens

#### Scenario: Menu with an open field
- **WHEN** a quick add field holds text and the user opens a column's ⋯ menu
- **THEN** the field stays open with its text and no dialog shows

#### Scenario: Explicit cancel
- **WHEN** the quick add field holds "Write docs" and the user taps "Cancel" or goes back
- **THEN** the field closes and the text is dropped without a question

### Requirement: Add column
"Add column" SHALL open an inline name field at the end of the column strip with "Add column" and "Cancel", on every window class. On a board without columns, the field SHALL open in place of the empty state. The new column SHALL be added as the last column after the server confirms it. Then the field SHALL close and the strip SHALL scroll to the new column.

#### Scenario: Add a column
- **WHEN** the user enters "Blocked" and confirms
- **THEN** once the server confirms it, "Blocked" shows as the last column, the field closes and the strip scrolls to "Blocked"

#### Scenario: Board without columns
- **WHEN** the user taps "Add column" in the "This board has no columns" state
- **THEN** the name field shows in place of the empty state

### Requirement: Column menu
Each column's ⋯ menu SHALL offer "Move left", "Move right", "Rename" and "Delete column". "Move left" SHALL be disabled on the first column and "Move right" on the last. A move SHALL place the column one position to the left or right.

#### Scenario: First column
- **WHEN** the user opens the menu of the first column
- **THEN** "Move left" is disabled and the other items are enabled

#### Scenario: Only column
- **WHEN** the board has one column and the user opens its menu
- **THEN** both "Move left" and "Move right" are disabled

### Requirement: Rename column
"Rename" SHALL open the "Rename column" dialog with the current name in the "Name" field. Saving an unchanged name, compared after normalization, SHALL close the dialog without a request.

#### Scenario: Unchanged name
- **WHEN** the user saves the dialog without changing the name, or only adds surrounding spaces
- **THEN** the dialog closes and no request is sent

### Requirement: Delete column
"Delete column" SHALL open a confirm dialog. For a column with cards it SHALL read "Delete column “{name}”?" / "Its {count} cards will be deleted too. This can’t be undone." with the button "Delete column and {count} cards". For an empty column it SHALL read "Delete column “{name}”?" / "This can't be undone." with the button "Delete column".

#### Scenario: Column with cards
- **WHEN** the user deletes "Review", which holds 2 cards
- **THEN** the dialog names the 2 cards, and after confirming, the column and its cards disappear once the server confirms

#### Scenario: Empty column
- **WHEN** the user deletes an empty column
- **THEN** the empty-column dialog shows

### Requirement: Board menu
The board's ⋯ menu SHALL offer "Rename board", "Export as JSON" and "Delete board". "Rename board" and "Delete board" SHALL open the same dialogs as the board list. "Export as JSON" SHALL start the export flow for this board. After a rename, the header SHALL show the new name. After the board is deleted, the app SHALL show the board list without a toast.

#### Scenario: Rename board
- **WHEN** the user renames the board to "Cardigan v2" from the board menu
- **THEN** the header shows "Cardigan v2"

#### Scenario: Export
- **WHEN** the user picks "Export as JSON"
- **THEN** the export flow for this board starts

#### Scenario: Delete board
- **WHEN** the user deletes the board from the board menu and the server confirms it
- **THEN** the board list shows and no toast shows

### Requirement: Accessibility labels
Controls without visible text SHALL have accessibility labels from string resources: "Back", "Board actions", "Actions for column “{name}”", "Add card to “{name}”" and "Add column". A card tile SHALL read as "{displayId}, {title}, labels {a}, {b}", with labels in tile order. Every interactive element SHALL have a touch target of at least 48 dp.

#### Scenario: Column menu button
- **WHEN** a screen reader focuses the ⋯ button of the column "Review"
- **THEN** it reads "Actions for column “Review”"

#### Scenario: Card tile
- **WHEN** a screen reader focuses the tile of `CRD-121` "Define export file format v1" with the labels `api` and `infra`
- **THEN** it reads "CRD-121, Define export file format v1, labels api, infra"
