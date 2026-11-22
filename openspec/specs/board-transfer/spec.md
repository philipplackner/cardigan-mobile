# board-transfer Specification

## Purpose

Defines how the Android and iOS app exports a board to a file and imports a board file: the dialogs, the checks the device runs before uploading, the platform file picker and share sheet, and how results and errors are shown. The file format and server rules are in cardigan-shared-specs `board-export-format`.

## Requirements

### Requirement: Export shows a blocking progress dialog
When the user chooses to export a board, the app SHALL show the Exporting progress dialog titled with the board's name, with the share-sheet sentence as its body, until the export request finishes. The dialog SHALL follow the design system's Progress dialog rules.

#### Scenario: Export starts
- **WHEN** the user chooses "Export as JSON" for the board “Cardigan v1”
- **THEN** a progress dialog titled “Exporting “Cardigan v1”” shows with the text "When the file is ready, the share sheet opens so you can save or send it."

#### Scenario: Back during export
- **WHEN** the Exporting dialog shows and the user does the Android back gesture
- **THEN** the dialog stays and the export continues

### Requirement: Export opens the share sheet
When the export succeeds, the app SHALL close the progress dialog and open the system share sheet with the export file. The file SHALL carry the exported bytes unchanged and SHALL be named with the server's suggested file name. The app SHALL NOT show a toast after the share sheet closes, whether the user shared the file or not.

#### Scenario: Successful export
- **WHEN** the export returns the file name `CRD-2026-11-04.json` and its bytes
- **THEN** the share sheet opens with a file of exactly that name and those bytes, byte for byte

#### Scenario: User closes the share sheet
- **WHEN** the user closes the share sheet without choosing a target
- **THEN** the board view shows again and no toast appears

### Requirement: Save to device on Android
On Android, the export share sheet SHALL offer a "Save to device" entry. Choosing it SHALL open the system "Save to…" screen with the export's file name, for a file of the JSON type. Saving SHALL write the exported bytes unchanged and show the success toast "Saved “{fileName}”.". A failed save SHALL show the error toast "Couldn't save “{fileName}”. Try again.". Cancelling the "Save to…" screen SHALL show nothing.

#### Scenario: Save the export locally
- **WHEN** the user chooses "Save to device" in the share sheet for `CRD-2026-11-04.json` and saves it to Downloads
- **THEN** Downloads holds `CRD-2026-11-04.json` with the exported bytes and the toast "Saved “CRD-2026-11-04.json”." shows

#### Scenario: Save cancelled
- **WHEN** the user chooses "Save to device" and closes the "Save to…" screen without saving
- **THEN** no file is written and no toast appears

### Requirement: Export file name fallback
When the export response has no usable suggested file name, the app SHALL name the file with the file's `board.key`, a hyphen, the UTC date of `exportedAt` as `yyyy-MM-dd`, and `.json`. When the key or a valid `exportedAt` is missing too, the file SHALL be named `board-export.json`. A suggested name that contains a path separator or doesn't match the allowed characters SHALL be treated as missing.

#### Scenario: Missing suggested name
- **WHEN** an export response for the key `CRD` has no file name and `exportedAt` is `2026-11-04T23:30:00Z`
- **THEN** the shared file is named `CRD-2026-11-04.json`

#### Scenario: Suggested name with a path
- **WHEN** an export response for the key `CRD` suggests the file name `../x.json` and `exportedAt` is `2026-11-04T23:30:00Z`
- **THEN** the shared file is named `CRD-2026-11-04.json`

#### Scenario: Nothing to build a name from
- **WHEN** an export response has no file name and the file's `exportedAt` can't be read
- **THEN** the shared file is named `board-export.json`

### Requirement: Export failures show an error toast
When an export fails, the app SHALL close the progress dialog and show an error toast. A 404 SHALL show "“{name}” no longer exists, so it can't be exported." Every other failure SHALL show "Couldn't export “{name}”. Check your connection and try again." The user SHALL stay on the board view.

#### Scenario: Network failure
- **WHEN** the export of “Cardigan v1” fails because the device is offline
- **THEN** the error toast "Couldn't export “Cardigan v1”. Check your connection and try again." shows

#### Scenario: Board deleted meanwhile
- **WHEN** the export of “Cardigan v1” returns 404
- **THEN** the error toast "“Cardigan v1” no longer exists, so it can't be exported." shows

### Requirement: Import opens the file picker directly
Starting an import SHALL open the system file picker at once, without a dialog. Cancelling that first picker SHALL end the import with nothing shown.

#### Scenario: Start an import
- **WHEN** the user chooses "Import board" on the board list
- **THEN** the system file picker opens and no import dialog shows

#### Scenario: First picker cancelled
- **WHEN** the user closes the first picker without choosing a file
- **THEN** the board list shows as before and no dialog or toast appears

### Requirement: Import dialog shows only for errors
The Import dialog SHALL show only in its error state: after a picked file fails a device check or an import fails. It SHALL show the file row, the error banner and the drop zone. The drop zone SHALL open the system file picker when tapped, SHALL NOT accept files dragged in from other apps, and SHALL be an accessible button labeled from string resources.

#### Scenario: Pick another file after an error
- **WHEN** the Import dialog shows a size error and the user taps the drop zone
- **THEN** the system file picker opens

#### Scenario: Screen reader
- **WHEN** TalkBack or VoiceOver focuses the drop zone
- **THEN** it is announced as a button labeled "Choose a board export file"

### Requirement: Picker offers JSON files only
The file picker SHALL offer only files of the JSON type: MIME type `application/json` on Android and the JSON uniform type on iOS. Cancelling the picker SHALL leave the Import dialog unchanged.

#### Scenario: Picker opens
- **WHEN** the file picker opens from the Import dialog
- **THEN** only files of the JSON type can be selected

#### Scenario: Picker cancelled from the error dialog
- **WHEN** the user opens the picker from the Import dialog's drop zone and closes it without choosing a file
- **THEN** the Import dialog shows as before, with its error

### Requirement: Oversized files are rejected on the device
A picked file larger than 5,242,880 bytes SHALL be rejected without uploading. The Import dialog SHALL open in its error state with the file row (the file's name and size) and the error "This file is {size}. Board exports can be up to 5 MB." A server 413 SHALL show the same error.

#### Scenario: File over the limit
- **WHEN** the user picks `board-backup.json` of 7,549,747 bytes
- **THEN** nothing is uploaded and the dialog shows `board-backup.json`, "7.2 MB" and "This file is 7.2 MB. Board exports can be up to 5 MB."

#### Scenario: Exactly at the limit
- **WHEN** the user picks a valid export of exactly 5,242,880 bytes
- **THEN** it is not rejected for its size and the import starts

#### Scenario: Server rejects the size
- **WHEN** an upload returns 413
- **THEN** the dialog shows the size error with the picked file's size

### Requirement: File sizes use base 1024
File sizes SHALL be shown in base 1024: in KB as a whole number, or in MB with one decimal once the value rounds to 1024 KB or more. Sizes SHALL round half up, except that a size over the 5,242,880-byte limit SHALL round up, so it never shows as "5.0 MB".

#### Scenario: Small file
- **WHEN** a picked file has 14,000 bytes
- **THEN** its size is shown as "14 KB"

#### Scenario: Half a kilobyte
- **WHEN** a picked file has 1,535 bytes
- **THEN** its size is shown as "1 KB"

#### Scenario: Exactly half a kilobyte
- **WHEN** a picked file has 2,560 bytes
- **THEN** its size is shown as "3 KB"

#### Scenario: One byte over the limit
- **WHEN** a picked file has 5,242,881 bytes
- **THEN** its size is shown as "5.1 MB"

#### Scenario: At the limit
- **WHEN** a picked file has 5,242,880 bytes
- **THEN** its size is shown as "5.0 MB"

### Requirement: Non-Cardigan files are rejected on the device
A picked file within the size limit SHALL be parsed as JSON before uploading. It SHALL be rejected without uploading when it isn't a JSON object, its `format` isn't the string `cardigan.board`, or its `formatVersion` isn't the JSON number literal `1` (`1.0`, `1e0` and `"1"` are rejected). The Import dialog SHALL open in its error state with the file row and "This file isn't a Cardigan board export. Choose a .json file that was exported from Cardigan."

#### Scenario: Plain JSON file
- **WHEN** the user picks `notes.json` holding `{"todo": []}`
- **THEN** nothing is uploaded and the dialog shows `notes.json`, its size and the invalid-file error

#### Scenario: Future version
- **WHEN** the user picks an export with `"formatVersion": 2`
- **THEN** nothing is uploaded and the dialog shows the invalid-file error

#### Scenario: Version as a decimal
- **WHEN** the user picks a file with `"formatVersion": 1.0`
- **THEN** nothing is uploaded and the dialog shows the invalid-file error

#### Scenario: Version as a string
- **WHEN** the user picks a file with `"formatVersion": "1"`
- **THEN** nothing is uploaded and the dialog shows the invalid-file error

### Requirement: Server-rejected files show the invalid-file error
An upload that returns 422 SHALL show the same file row and invalid-file error as a file rejected on the device.

#### Scenario: Invalid content
- **WHEN** a file passes the device checks and the upload returns 422 `INVALID_EXPORT_FILE`
- **THEN** the Import dialog shows the file row and "This file isn't a Cardigan board export. Choose a .json file that was exported from Cardigan."

### Requirement: Import starts right after a valid pick
A picked file that passes the device checks SHALL be uploaded at once, without a confirm step. While the upload runs, the app SHALL show the progress dialog "Importing {filename}" with "This creates the board with all its columns, cards and labels." The app SHALL NOT repeat an upload on its own.

#### Scenario: Valid file
- **WHEN** the user picks a valid export named `board-backup.json`
- **THEN** the progress dialog "Importing board-backup.json" shows, replacing the Import dialog if it was open

### Requirement: Successful import
When the import succeeds, the app SHALL close all import dialogs, reload the board list, and show the success toast "Imported “{name}” with the key {key}." with the imported board's name and key. The user SHALL stay on the board list.

#### Scenario: Import without a clash
- **WHEN** the import of “Cardigan v1” with the key `CRD` succeeds
- **THEN** the board list reloads and the toast "Imported “Cardigan v1” with the key CRD." shows

### Requirement: Other import failures
Any other import failure, including a network failure with an unknown outcome, SHALL show the Import dialog in its error state with the file row and the error "Couldn't import the board. Check your connection and try again." The board list SHALL reload.

#### Scenario: Connection lost during upload
- **WHEN** the connection drops while the app waits for the import response
- **THEN** the upload is not repeated, the Import dialog shows the import-failed error, and the board list reloads

### Requirement: Key clash asks for another key
When an upload returns 409 with the field error `key`/`ALREADY_USED`, the app SHALL show "Choose a different key". The Key field SHALL start with the clashing key and its "already used" error. The body SHALL name the existing board when a loaded board has that key, and use the sentence without the name otherwise.

#### Scenario: Known board
- **WHEN** the file's key is `CRD` and the loaded board list has “Cardigan v1” with the key `CRD`
- **THEN** the dialog says "Your board “Cardigan v1” already uses the key CRD. Enter another key for the imported board.", the field holds `CRD`, and "You already have a board with the key CRD." shows under it

#### Scenario: Board not in the loaded list
- **WHEN** the file's key is `CRD` and no loaded board has that key
- **THEN** the dialog says "One of your boards already uses the key CRD. Enter another key for the imported board."

#### Scenario: Lowercase key in the file
- **WHEN** the file's key is `crd` and the upload returns 409 with `key`/`ALREADY_USED`
- **THEN** the field holds `CRD`, the body names the loaded board with the key `CRD`, and "Import" is disabled

### Requirement: Import is disabled for used keys
In the key dialog, "Import" SHALL be disabled while the entered key, with `a`–`z` converted to uppercase, equals a key that came back as already used during this import.

#### Scenario: Key unchanged
- **WHEN** the key dialog opens after `CRD` clashed
- **THEN** "Import" is disabled

#### Scenario: Back to a used key
- **WHEN** `CRD` and then `CRDB` came back as used and the user types `crd`
- **THEN** "Import" is disabled

### Requirement: Retry with another key
Choosing "Import" in the key dialog SHALL convert `a`–`z` to uppercase and check the key in this order, showing the first failure under the field: empty, fewer than 2 characters, more than 5, any character outside `A`–`Z`. The New board key field uses the same check and strings. An invalid key SHALL NOT upload. A valid key SHALL upload the same file again with that key and show the Importing progress dialog.

#### Scenario: Valid new key
- **WHEN** the user enters `crdb` and chooses "Import"
- **THEN** the file is uploaded with the key `CRDB` and the Importing dialog shows

#### Scenario: Too short
- **WHEN** the user enters `C` and chooses "Import"
- **THEN** nothing is uploaded and the field shows "Use at least 2 letters."

#### Scenario: Empty
- **WHEN** the user clears the field and chooses "Import"
- **THEN** nothing is uploaded and the field shows "Enter a key."

#### Scenario: Too long
- **WHEN** the user enters `CRD123` and chooses "Import"
- **THEN** nothing is uploaded and the field shows "Use at most 5 letters."

#### Scenario: Not letters
- **WHEN** the user enters `CR1` and chooses "Import"
- **THEN** nothing is uploaded and the field shows "Use only the letters A–Z."

### Requirement: Key field errors from the server
A retry that returns 409 `ALREADY_USED` or 400 for the `key` field SHALL return to the key dialog with the user's key in the field and the error for that code under it: `REQUIRED`, `TOO_SHORT` and `TOO_LONG` show their key check message, and `INVALID_FORMAT` or any other code shows "Use only the letters A–Z." The dialog SHALL stay open. Other failures of a retry SHALL behave as for the first upload.

#### Scenario: New key also used
- **WHEN** the retry with `CRDB` returns 409 with `key`/`ALREADY_USED`
- **THEN** the key dialog shows `CRDB` with "You already have a board with the key CRDB." under the field

#### Scenario: Server rejects the format
- **WHEN** the retry returns 400 with the field error `key`/`INVALID_FORMAT`
- **THEN** the key dialog shows "Use only the letters A–Z." under the field

#### Scenario: Server sends an unknown key code
- **WHEN** the retry returns 400 with a `key` field error code the app doesn't know
- **THEN** the key dialog shows "Use only the letters A–Z." under the field

### Requirement: Cancel import closes everything
"Cancel import" in the key dialog SHALL close all import dialogs without uploading again. "Cancel" in the Import dialog SHALL close it and end the import. Back, Escape and a scrim tap on the Import dialog SHALL do the same as "Cancel". On the key dialog they SHALL do the same as "Cancel import".

#### Scenario: Cancel after a clash
- **WHEN** the key dialog shows and the user chooses "Cancel import"
- **THEN** all import dialogs close, no board is created, and no toast shows

#### Scenario: Back on the error dialog
- **WHEN** the Import dialog shows an error and the user does the Android back gesture
- **THEN** it closes and nothing is uploaded
