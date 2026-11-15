# Proposal

## Why

Users can't yet move a board out of the app or bring one back in. Chunk `mob-import-export-ui` builds the import and export UI and the platform file handling now, against a repository interface. Then `mob-import-export-connect` only has to add the HTTP calls once the backend endpoints exist.

## What Changes

- **Export** (board view menu → "Export as JSON"; the menu item comes from the boards chunk):
  - A blocking "Exporting “{name}”" progress dialog.
  - Then the system share sheet with the file under its export name.
  - Failures show an error toast. No toast after the share sheet.
- **Import** (board list "Import" button or phone "Import board"; the entry points come from the boards chunk):
  - The system file picker opens directly, filtered to JSON. There's no dialog up front (Figma app-only note).
  - The picked file is checked on the device for size (over 5 MB) and format (`cardigan.board`, version 1).
  - Then it is uploaded in one blocking "Importing {filename}" progress dialog.
  - On success, the board list reloads and a toast shows.
  - Invalid and oversized files and other failures open the Import dialog in its error state: file row, error banner, and a tap-only drop zone to pick another file.
- **Key clash**: a "Choose a different key" dialog. It opens with the clashing key and the "already used" error. "Import" stays disabled until the key differs from every key that came back as used. The key is checked like the New board key field. "Cancel import" closes everything.
- **`:core:designsystem`**: a tap-only **Drop zone** component, used only in the Import dialog's error state.
- **`:core:presentation`**: a `ToastController` interface. **`:composeApp`**: one app-wide toast host that every feature can use.
- **`:core:domain`**: a minimal `Board` model (id, name, key).
- **`transfer` feature**:
  - `domain`: repository interface, error types, file checks, key rule, size and file name formatting.
  - `data`: the JSON format check.
  - `presentation`: dialogs, ViewModels, dialog routes and the platform file picker and share sheet (`expect`/`actual`).
- **Wiring in `:composeApp`**: transfer's nav graph, entry callbacks, the board list reload after an import, and the loaded-board lookup for the key clash dialog.

### Copy

All copy below is approved by the user.

From Figma, unchanged:
- "Import board" / "Choose a .json file exported from Cardigan. It’s added as a new board; your other boards don’t change." / "JSON exports up to 5 MB" / "Cancel" (Import dialog, error state only)
- "Importing {filename}" / "This creates the board with all its columns, cards and labels."
- "Exporting “{name}”"
- "This file isn't a Cardigan board export. Choose a .json file that was exported from Cardigan."
- "This file is {size}. Board exports can be up to 5 MB."
- "Choose a different key" / "Your board “{name}” already uses the key {key}. Enter another key for the imported board." / "Key" / "2–5 uppercase letters. Can’t be changed later." / "Cancel import" / "Import"
- "Imported “{name}” with the key {key}."
- Key already used under the field: "You already have a board with the key {key}." (New board dialog)

Shared, agreed with web-import-export-ui:
- Exporting dialog body (app): "When the file is ready, the share sheet opens so you can save or send it."
- Export failed: "Couldn't export “{name}”. Check your connection and try again."
- Export 404: "“{name}” no longer exists, so it can't be exported."
- Import failed: "Couldn't import the board. Check your connection and try again."
- Key clash, board name unknown: "One of your boards already uses the key {key}. Enter another key for the imported board."

App-only, Android share sheet (D36):
- Entry: "Save to device"
- Saved: "Saved “{fileName}”."
- Save failed: "Couldn't save “{fileName}”. Try again."

App-only, shown only in the Import dialog's error state (added to Figma page 9:9 as an app-only note):
- Drop zone title: "Choose a board export"
- Drop zone hint: "Tap to browse files" (the "browse files" part in the accent color, as in Figma)
- Drop zone accessibility label: "Choose a board export file"
Key field errors (`TOO_SHORT`, `TOO_LONG`, `INVALID_FORMAT`) reuse the New board key field strings that `mob-board-list-ui` defines. "Use 2–5 letters from A to Z." is the approved `INVALID_FORMAT` candidate handed to that chunk.

## Capabilities

### New Capabilities
- `board-transfer`: how the app exports a board to a file and imports a board file on Android and iOS: entry flow, device-side file checks, progress, key clash, results and errors, and the platform file picker and share sheet.

### Modified Capabilities
- (none) The tap-only Drop zone and the toast host are components and wiring. Their behavior is specified in `board-transfer`. `design-system` keeps its existing requirements.

### Shared-spec requirements

This change implements or depends on:
- cardigan-shared-specs `board-export-format`:
  - "Export file identifies its format and version": the device-side format check.
  - "Export download": the export file name.
  - "Import upload": one request, file plus optional key.
  - "Import size limits": the 5,242,880-byte check and the 413 mapping.
  - "Unreadable or unsupported files": the 422 mapping and unsupported versions.
  - "Import validates every rule of the domain": any 422 shows the one invalid-file message.
  - "Imported board key and clashes": the key part, 409 `ALREADY_USED`, 400 on the `key` field.
- cardigan-shared-specs `domain-board`:
  - "Board key format": 2–5 letters A–Z, lowercase converted to uppercase.
  - "Board key is unique among a user's existing boards".
- cardigan-shared-specs `api-conventions`, "Retries after a lost response": no repeated upload, report the failure, reload the board list.
- cardigan-shared-specs `api-errors`:
  - "Error precedence": an invalid file beats a used key.
  - "Field errors identify the field and rule".
  - "Uniqueness clashes are conflicts".

## Impact

- **New code** in `feature/transfer/{domain,data,presentation}`, `:core:domain` (Board), `:core:presentation` (ToastController), `:core:designsystem` (Drop zone) and `:composeApp` (toast host, transfer graph and callbacks).
- **`:androidApp`**: a `FileProvider` entry in the manifest and a paths XML for sharing the export file from the cache directory.
- **`iosApp/`**: no changes. The share sheet and picker are presented from Kotlin through UIKit.
- **Dependencies**: none new. JSON parsing uses kotlinx-serialization-json, which `kmp.library` already brings. The file picker and share sheet use platform APIs only (CLAUDE.md).
- **Coordination**:
  - The boards UI chunks add the entry points and expose a board list reload hook and a key → name lookup over the loaded list.
  - Board in `:core:domain` may overlap with their planning. See design.md.
  - A NavHost and theme wrapper in `:composeApp` may also come from a parallel chunk.
- **Manual verification**: real files are picked through the system picker on both platforms and run through the whole flow. A debug-only fixture repository stands in for the server, and a temporary debug entry screen opens the dialogs. The last task group removes both before the branch is pushed.
- **Figma**: an app-only note on "Import dialog · Phone 390" with the drop zone copy, saying the dialog shows only in its error state in the app.
- **Tests**: unit tests for checks, formatting, key rule, ViewModels and the toast controller. `*UiTest`s for the dialogs and both flows with fakes. Run on Android host and the iOS simulator.
