# Decisions: add-transfer-ui

Confirmed with the user via the orchestrator session (cardiganapp-be) on 2026-11-15. The questions were sent to that session, which merged the SHARED ones with the web client's questions (web-import-export-ui) and relayed the user's answers. This is the same way add-design-system recorded its decisions. Shared strings were aligned with the web session directly.

## Summary

Build the mobile import and export UI for chunk `mob-import-export-ui` in the `transfer` feature. Export starts from the board view menu, shows a progress dialog and opens the share sheet. Import starts from the board list and opens the system file picker directly. It checks the picked file's size and format on the device and shows the Import dialog only in its error state, where a tap-only drop zone picks another file. Otherwise it uploads it in one blocking request, handles a key clash, and leaves the user on a reloaded board list with a success toast. The feature runs against a repository interface tested with test doubles. The real HTTP calls come in `mob-import-export-connect`.

## Decisions

### D1 — Where export lives
- **Question**: Where is the export action?
- **Decision**: Only in the board view menu ("Export as JSON"), as in Figma. Not in the board list menus.
- **Alternatives**: Board list menus (chunk 38's wording).
- **Why**: Figma. The orchestrator is updating the shared breakdown (chunks 16/38).
- **Depends on**: —

### D2 — After a successful import
- **Question**: What happens after a successful import?
- **Decision**: The user stays on the board list, the list reloads, and the success toast shows.
- **Alternatives**: Open the imported board, with or without a toast.
- **Why**: Figma "Imported" frames. SHARED.
- **Depends on**: —

### D3 — Import starts on pick
- **Question**: Does the user confirm after picking a file?
- **Decision**: No. The import starts right after a file is picked and passes the client checks.
- **Alternatives**: An extra confirm step.
- **Why**: Figma. The dialog has only Cancel. SHARED.
- **Depends on**: D11

### D4 — Import entry on mobile (superseded by D31)
- **Question**: How does import start on mobile, given the design system left out the Drop zone as web-only?
- **Decision**: Show the Import dialog as drawn. The drop zone is tap-only and opens the system file picker. A tap-only Drop zone is added to `:core:designsystem`.
- **Alternatives**: Skip the dialog and open the picker directly; build a full drop zone that accepts files dragged in from other apps.
- **Why**: Matches the phone frame, and the designed error states live in this dialog.
- **Depends on**: —

### D5 — Drop zone copy
- **Question**: What does the drop zone say on mobile, where nothing can be dropped?
- **Decision**: App-only copy, approved by the user: title "Choose a board export", hint "Tap to browse files" ("browse files" in accent), accessibility label "Choose a board export file". It is added to Figma page 9:9 as an app-only note.
- **Alternatives**: Keep "Drop a board export here"; wait for a design.
- **Why**: The drawn copy is wrong on touch devices. Per D31 the drop zone and this copy show only in the Import dialog's error state, and the Figma note says so.
- **Depends on**: D4, D31

### D6 — Export target
- **Question**: Where does an exported file go?
- **Decision**: The system share sheet on both Android and iOS.
- **Alternatives**: Android "Save to…" (create document) and iOS share sheet; offer both.
- **Why**: Matches the Figma copy and works the same on both platforms.
- **Depends on**: —

### D7 — No toast after the share sheet
- **Question**: Does the app confirm the export after the share sheet?
- **Decision**: No. The share sheet is the confirmation. The web-only "“X” exported." toast is not used in the app.
- **Alternatives**: Toast after the sheet closes; toast only when the platform reports completion.
- **Why**: Android can't tell whether the user shared or cancelled.
- **Depends on**: D6

### D8 — Picker filter
- **Question**: Which files can be picked?
- **Decision**: JSON only: MIME type `application/json` on Android, `UTType.json` on iOS.
- **Alternatives**: Any file, with the client and backend judging the content (recommended in the review).
- **Why**: User choice. The trade-off (a valid export labeled with another type by a messenger or cloud app is greyed out) is recorded in design.md.
- **Depends on**: —

### D9 — Size check on the client
- **Question**: Is the 5 MB limit checked before upload?
- **Decision**: Yes. A file over 5,242,880 bytes is rejected on the device with "This file is {size}. Board exports can be up to 5 MB." without uploading. A server 413 maps to the same message.
- **Alternatives**: Server 413 only.
- **Why**: Matches the design, which shows the real size, and saves mobile data. SHARED.
- **Depends on**: —

### D10 — Size format
- **Question**: How are file sizes shown?
- **Decision**: Base 1024. Under 1024 KB: KB as a whole number. From 1024 KB: MB with one decimal. Both round half up, except that a size over the limit rounds up, so 5,242,881 bytes shows "5.1 MB", never "5.0 MB". A value that rounds to 1024 KB is shown in MB.
- **Alternatives**: Base 1024 with normal rounding everywhere; base 1000; KB always rounded up.
- **Why**: "5 MB" matches the 5,242,880-byte limit, and an over-limit file never looks like it is within it. The half-up rule was confirmed in the follow-up round. SHARED.
- **Depends on**: D9

### D11 — Format check on the client
- **Question**: Is "Not a Cardigan export" detected on the device?
- **Decision**: Yes. After the size check, the client parses the JSON and checks that `format` is `cardigan.board` and `formatVersion` is `1`. A server 422 maps to the same message.
- **Alternatives**: Server 422 only (recommended in the review).
- **Why**: User choice. Overrides the review's recommendation. SHARED.
- **Depends on**: D9

### D12 — One message for every invalid file
- **Question**: What does an invalid file show?
- **Decision**: Every invalid file, including any 422 and an unsupported `formatVersion`, shows "This file isn't a Cardigan board export. Choose a .json file that was exported from Cardigan."
- **Alternatives**: Separate copy for unsupported versions.
- **Why**: The shared spec says the response doesn't say what failed. SHARED.
- **Depends on**: D11

### D13 — Undrawn import errors
- **Question**: How are import errors without a design shown (network failure, unknown outcome, 5xx, anything not mapped above)?
- **Decision**: In the Import dialog's error banner with the generic import message, and the board list reloads.
- **Alternatives**: Close the dialog and show an error toast; wait for a design.
- **Why**: The same place as the designed errors. The reload follows the shared rule for a lost import response. SHARED.
- **Depends on**: —

### D14 — Undrawn export errors
- **Question**: How are export errors shown?
- **Decision**: The progress dialog closes and an error toast shows: a generic message, or a "no longer exists" message for a 404.
- **Alternatives**: Wait for a design.
- **Why**: The design system has an error toast. SHARED.
- **Depends on**: —

### D15 — Shared strings
- **Question**: What copy do the undrawn states use?
- **Decision**: The strings drafted by web-import-export-ui and accepted by this session:
  - Export failed: "Couldn't export “{boardName}”. Check your connection and try again."
  - Export 404: "“{boardName}” no longer exists, so it can't be exported."
  - Import failed: "Couldn't import the board. Check your connection and try again."
  - Key clash without a known board name: "One of your boards already uses the key {key}. Enter another key for the imported board."
  - App Exporting dialog body: "When the file is ready, the share sheet opens so you can save or send it."
- **Alternatives**: —
- **Why**: Both clients use the same strings. Approved by the user in the follow-up round.
- **Depends on**: D13, D14, D17, D23

### D16 — Key prefill
- **Question**: What's in the Key field when "Choose a different key" opens?
- **Decision**: The clashing key (e.g. `CRD`).
- **Alternatives**: Empty; a suggested free key.
- **Why**: User choice. SHARED.
- **Depends on**: —

### D17 — Board name in the key clash dialog
- **Question**: Where does the name in "Your board “{boardName}” already uses the key {key}." come from?
- **Decision**: From the board list data already loaded. If no loaded board has that key, the dialog uses the variant sentence without the name (D15).
- **Alternatives**: An extra request.
- **Why**: User choice. SHARED.
- **Depends on**: —

### D18 — Key checks
- **Question**: How is the new key checked?
- **Decision**: The same as the New board key field: client-side 2–5 letters A–Z, server field errors shown under the field, and the dialog stays open. The field error strings for `TOO_SHORT`, `TOO_LONG` and `INVALID_FORMAT` are defined by the board list chunks and reused here. "Use 2–5 letters from A to Z." is approved and goes to the board list session as the `INVALID_FORMAT` candidate.
- **Alternatives**: Wait for designs; own strings in `transfer`.
- **Why**: User choice; one set of key strings across the app. SHARED.
- **Depends on**: —

### D19 — Cancel import
- **Question**: What does "Cancel import" do?
- **Decision**: It closes everything and imports nothing.
- **Alternatives**: Return to the Import dialog.
- **Why**: User choice. SHARED.
- **Depends on**: —

### D20 — Export file name
- **Question**: What is the exported file called?
- **Decision**: The `Content-Disposition` file name. Fallback: the file's `board.key`, a hyphen, and the UTC date of `exportedAt`, plus `.json`. Last resort, when that can't be built either: `board-export.json`.
- **Alternatives**: —
- **Why**: The shared export format defines the name. The fallback uses the same pattern. The last-resort name came from the web follow-ups. SHARED.
- **Depends on**: —

### D21 — Exporting dialog copy
- **Question**: What does the Exporting dialog say in the app?
- **Decision**: Only the share-sheet sentence (D15). The browser sentence is web-only.
- **Alternatives**: The combined Figma sentence.
- **Why**: User choice. SHARED.
- **Depends on**: D6

### D22 — Hung requests
- **Question**: What happens when an import or export request hangs?
- **Decision**: Decided in `mob-import-export-connect`.
- **Alternatives**: —
- **Why**: User choice. Timeouts belong to the HTTP calls.
- **Depends on**: —

### D23 — Success toast
- **Question**: What does the success toast say?
- **Decision**: Always "Imported “{name}” with the key {key}.", with or without a key clash.
- **Alternatives**: Drop the key when it didn't change.
- **Why**: User choice. SHARED.
- **Depends on**: D2

### D24 — Hosting
- **Question**: How does `transfer` plug into the board list and board view?
- **Decision**: `transfer` owns the Import and Export(boardId) dialog destinations. Callbacks are wired in `:composeApp`, and the import result makes the board list reload.
- **Alternatives**: Host composables passed into the boards screens as slots; move the transfer UI into `boards`.
- **Why**: Features don't depend on each other (CLAUDE.md).
- **Depends on**: —

### D25 — Entry points
- **Question**: Who adds the Import button, the "Import board" menu item and "Export as JSON"?
- **Decision**: The boards UI chunks (`mob-board-list-ui`, `mob-board-view-ui`). This change handles everything after the tap.
- **Alternatives**: This change adds them to the boards screens.
- **Why**: Those chunks own the screens.
- **Depends on**: D24

### D26 — Toast host
- **Question**: Who shows a toast that comes from `transfer` but appears over the board list or board view?
- **Decision**: One app-wide toast host in `:composeApp`, plus a controller interface in `:core:presentation`.
- **Alternatives**: Each screen hosts its own toast.
- **Why**: Every feature will need toasts.
- **Depends on**: —

### D27 — Board model
- **Question**: Where does the Board model live?
- **Decision**: Add a minimal Board to `:core:domain` in this change.
- **Alternatives**: A transfer-only `ImportedBoard` model (recommended in the review).
- **Why**: User choice. The overlap risk with the parallel boards chunks is recorded in design.md.
- **Depends on**: —

### D28 — Manual verification
- **Question**: How is the file handling checked without a backend?
- **Decision**: Pick a real JSON file through the system file picker on Android and iOS and run it through the whole flow. A debug-only fixture repository stands in for the server, and a temporary debug entry screen opens the dialogs (design "Manual verification", option A). A last task group in this change removes both before the branch is pushed.
- **Alternatives**: B, a "not connected" repository that fails every call; C, no running-app check in this change.
- **Why**: The user first asked: "Can't we just use the file open dialog in order to pick a JSON file and test it that way?" The upload step and the share sheet still need something in place of the server, so the user chose option A in the follow-up round and asked for the removal task.
- **Depends on**: D8, D9, D11

### D29 — Defaults accepted
- **Question**: Accept the session's listed defaults?
- **Decision**: Yes:
  - Tablet uses the same dialogs, sized by window class.
  - Phone versions of the desktop-only error frames use the Touch dialog.
  - Progress dialogs block back and scrim taps.
  - No automatic retry.
  - Picked files are read with a 5 MB + 1 byte cap.
  - The export temp file lives in the cache directory and is overwritten on the next export.
  - Export bytes are passed through unchanged.
  - Mapping ProblemDetail codes to transfer errors is left to the connect chunk.
  - The change is named `add-transfer-ui`, and nothing is committed.
- **Alternatives**: —
- **Why**: User choice.
- **Depends on**: —

### D30 — Key clash dialog start state
- **Question**: How does "Choose a different key" look when it opens?
- **Decision**: It opens with the "already used" error showing under the field. "Import" stays disabled until the key differs from every key that came back as used in this flow.
- **Alternatives**: Open without an error; Import always enabled.
- **Why**: From the web follow-ups, confirmed by the user. SHARED.
- **Depends on**: D16, D18

### D31 — App import flow follows the Figma note
- **Question**: The app-only annotation on "Import from the phone menu · Phone 390" says the app opens the system file picker directly. Follow it, or keep D4's dialog up front?
- **Decision**: Follow the Figma note. "Import" / "Import board" opens the system file picker directly, with no import dialog up front. When a picked file fails the size or format check, or the import fails, the Import dialog shows in its drawn error state (file row + banner). Its tap-only drop zone lets the user pick another file. The tap-only DropZone and its approved copy stay, used only in that error state. Progress and key clash are unchanged.
- **Alternatives**: Keep D4 (dialog up front) and change the Figma note.
- **Why**: Figma is the source of truth for UI. The annotation was missed in the first round because the MCP read tools don't return annotations. Supersedes D4.
- **Depends on**: D3, D4, D5

## Facts

- Figma 9:9 has 17 frames: Export (menu D/P, Exporting D/P, Export done D) and Import (dialog D/P, dragging D, phone menu P, Importing D/P, Not a Cardigan export D, File larger than 5 MB D, Key already used D/P, Imported D/P). There are no tablet frames — source: `get_metadata` 9:9.
- `get_metadata`/`get_design_context` returned no frame annotations for 9:9, and Overview (0:1) has no UI decisions list. A later Plugin API read found 3 annotations on 9:9:
  - "Import from the phone menu · Phone 390": "Web opens the import dialog. The app opens the system file picker (not drawn), then shows the same progress and error dialogs."
  - "Imported · Desktop 1440" and "Imported · Phone 390": "User stays on the board list; the toast confirms the import."

  The first conflicted with D4 and is resolved by D31 — source: `use_figma` read of `node.annotations` on 9:9.
- The board list tablet header has an "Import" button. The phone board row menu has only Rename and Delete board — source: Figma 28:1941, 30:4580.
- The New board key-used error is "You already have a board with the key CRD.", and the key help text is "2–5 uppercase letters. Can’t be changed later." — source: Figma 30:5738, 56:18965.
- The key clash dialog: title "Choose a different key", body "Your board “Cardigan v1” already uses the key CRD. Enter another key for the imported board.", buttons "Cancel import" / "Import" — source: Figma 56:18965.
- Import is one multipart request with a required `file` part and an optional `key` part. 413 is returned over 5,242,880 bytes, 422 `INVALID_EXPORT_FILE` for any invalid file, and 409 `CONFLICT` with field error `key`/`ALREADY_USED` on a clash — source: cardigan-shared-specs `board-export-format`.
- Lowercase key letters are converted to uppercase before validation — source: cardigan-shared-specs `domain-board` "Board key format".
- After a lost import response, the client doesn't repeat the upload, reports the failure and reloads the board list — source: cardigan-shared-specs `api-conventions` "Retries after a lost response".
- `:core:domain` has only `Result`, `Error` and `DataError`. `App()` is empty, with no NavHost and no theme wrapper. `feature/transfer/*` has build files only — source: codebase.
- `:core:designsystem` already has `ProgressDialog`, `CardiganDialog`, `Banner`, `Toast`/`ToastHost`/`ToastHostState`, `FileRow`, `dashedBorder`, and the `file`, `upload` and `alert` icons — source: codebase.

## Out of scope

- Real HTTP calls, DTOs, ProblemDetail mapping and timeouts (`mob-import-export-connect`).
- The Import button, the "Import board" and "Export as JSON" menu items, and the board list itself (`mob-board-list-ui`, `mob-board-view-ui`).
- Dropping files into the app from other apps.
- Navigating to the imported board.
- A toast after the share sheet.
- Async import jobs.

## Deferred


- The `TOO_SHORT` and `TOO_LONG` key error strings, if `mob-board-list-ui` hasn't defined them when task 5.1 runs. — settle before: implementation of task 5.1.
- Whether a NavHost and theme wrapper exist in `:composeApp` when this change is applied. They may come from a parallel chunk. — settle before: implementation of task group 8.
