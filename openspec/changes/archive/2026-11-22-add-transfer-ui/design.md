# Design

## Context

- Motivation and scope: proposal.md. Behavior: `specs/board-transfer/spec.md`. Settled choices: decisions.md (D-numbers below).
- `feature/transfer/{domain,data,presentation}` exist with build files only.
  - `presentation` uses `cmp.feature`: Koin, navigation-compose, lifecycle and savedstate.
  - `data` already has Ktor and Koin. `kmp.library` brings kotlinx-serialization-json and the test stack to every module.
- `:core:designsystem` has `ProgressDialog`, `CardiganDialog`, `Banner`, `Toast`/`ToastHost`/`ToastHostState`, `FileRow`, `Modifier.dashedBorder`, and the icons `file`, `upload`, `alert`.
- `App()` is empty. No NavHost, no `AppTheme` wrapper, no screens.
- The boards UI chunks are planned in parallel. They own the screens the entry points live on (D25).
- No backend endpoint exists yet. The repository has no real implementation in this change.

## Goals / Non-Goals

**Goals:**
- Every state on Figma page 9:9 for phone, tablet and desktop window classes, plus the agreed undrawn states.
- Platform file picking and sharing behind interfaces with Android and iOS implementations.
- Logic that is fully unit-testable in `commonTest`, and both flows covered by `*UiTest`s with fakes.

**Non-Goals:**
- HTTP, DTOs, ProblemDetail mapping and timeouts (`mob-import-export-connect`, D22).
- The entry points and the board list or board view screens (D25).
- Drag-in from other apps (D4, D31).

## Decisions

### Module layout

```
:core:domain        board/Board.kt                          data class Board(id: Uuid, name: String, key: String)
:core:presentation  toast/ToastController.kt                 interface + ToastMessage(text: UiText, type: Success|Error)
:core:designsystem  components/transfer/DropZone.kt          tap-only Drop zone
:composeApp         toast/AppToastController.kt, App()       app-wide ToastHost; transfer graph + callbacks
:feature:transfer:domain
    BoardTransferRepository, ExportedFile, ImportError, ExportError
    ImportFileRules (size limit, size check)
    FileSizeFormatter, ExportFileName (fallback name)
    ExportFileChecker (interface), BoardKeyOwnerLookup (fun interface)
:feature:transfer:data
    JsonExportFileChecker (kotlinx-serialization-json), Koin module for it
:feature:transfer:presentation
    import/  ImportBoardViewModel, State, Action, Event, ImportBoardRoot, dialogs
    export/  ExportBoardViewModel, State, Action, Event, ExportBoardRoot
    platform/ FilePicker, FileSharer (expect/actual composables + interfaces)
    navigation/ ImportBoardRoute, ExportBoardRoute, transferGraph()
```

The JSON check sits behind an interface in `domain`, implemented in `data`. This keeps JSON parsing out of the domain module and lets ViewModel tests use a fake. The connect chunk adds `KtorBoardTransferRepository` to the same `data` module.

### Board in `:core:domain` (D27)

`Board(id: Uuid, name: String, key: String)`. Nothing else: card counts, update times and columns are for the boards chunks to add. `Uuid` follows CLAUDE.md's stack table.

Overlap risk: `mob-board-list-ui` / `mob-board-view-ui` may plan their own Board with more fields. Whoever applies second extends this class rather than adding a second one. This is called out in the summary to the orchestrator.

### Repository and errors

```kotlin
interface BoardTransferRepository {
    suspend fun exportBoard(boardId: Uuid): Result<ExportedFile, ExportError>
    suspend fun importBoard(file: ByteArray, key: String?): Result<Board, ImportError>
}
data class ExportedFile(val fileName: String, val bytes: ByteArray)
sealed interface ExportError : Error { BoardNotFound; Failed }
sealed interface ImportError : Error { FileTooLarge; InvalidFile; KeyAlreadyUsed; data class KeyInvalid(val rule: BoardKeyError); Failed }
enum class BoardKeyError { Required, TooShort, TooLong, InvalidFormat } // :core:domain
```

- These are the domain errors the spec needs, and no more. `Failed` covers network, unknown outcome, 5xx and every unmapped code (D13, D14).
- How ProblemDetail codes reach these types is the connect chunk's open item (D29).
- `KeyAlreadyUsed` means 409 with `key`/`ALREADY_USED`. `KeyInvalid(rule)` means 400 with a `key` field error: `REQUIRED`, `TOO_SHORT` and `TOO_LONG` map to their own case, and `INVALID_FORMAT` or any other code maps to `InvalidFormat` (D37).
- `BoardKeyError` is also the result type of `BoardKeyRule`'s validation, so client and server key errors map to the same `UiText`.

### Device-side checks (D9, D11, D12)

In order, in the ViewModel, before any upload:
1. **Size.** The picked file arrives with its byte size. Size > 5,242,880 → `FileTooLarge`.
2. **Format.** `ExportFileChecker.check(bytes)`:
   - A leading UTF-8 BOM (`EF BB BF`) is removed before decoding.
   - Parses with `Json.parseToJsonElement` after UTF-8 decoding.
   - Requires a JSON object, `format` as the string `cardigan.board`, and `formatVersion` as the integer literal `1`. `1.0`, `"1"`, `2` and a missing value all fail.
   - On success it returns `CheckedFile(key: String?)`: the file's `board.key` passed through `BoardKeyRule.normalize` when it is a string, for the clash prefill (D16). A file without a usable key passes the check, and the server rejects it with 422.

The 5,242,880-byte limit and the format literals are the only rules duplicated from the shared spec. The server stays the full validator.

### File sizes and names (D10, D20)

`FileSizeFormatter.format(bytes, overLimit)` returns `FileSize(value: String, unit: KB|MB)`. The presentation layer turns it into a string resource, so the number format stays in one tested place.
- KB as a whole number, rounded half up. Once that value would be 1024 KB or more, MB with one decimal, rounded half up.
- Exception: a size over 5,242,880 bytes rounds up, so it never shows as "5.0 MB" (D10).
- The decimal separator is a dot. English only for now (CLAUDE.md).

`ExportFileName.fallback(key, exportedAt)` gives `KEY-yyyy-MM-dd.json` from the UTC date, or `board-export.json` when the key is missing or `exportedAt` can't be parsed. The connect chunk uses it when `Content-Disposition` is missing. It is built and tested here because it is pure logic the user decided on.

`ExportFileName.sanitize(name)` keeps the last path segment only, and accepts it only if it matches `[A-Za-z0-9._-]{1,100}\.json` and is not `.`/`..`. Otherwise the fallback name is used. Both `actual` sharers write only sanitized names, because the name comes from a server header and becomes a file path.

### Key rule (D18)

`BoardKeyRule.normalize(input)`: maps `a`–`z` to uppercase and leaves everything else as is. `BoardKeyRule.validate(key)` checks in this order: empty → `Required`, fewer than 2 chars → `TooShort`, more than 5 → `TooLong`, any char outside `A`–`Z` → `InvalidFormat`. No trimming, because `domain-board` treats any other character as invalid. Both live in `:core:domain` (`core.domain.board`) so the board list reuses them (D37).

This duplicates what the New board field needs. When `mob-board-list-ui` lands, the rule moves to `:core:domain` if both features use it (CLAUDE.md: code moves to core when a second feature needs it).

The key field error strings (`REQUIRED`, `TOO_SHORT`, `TOO_LONG`, `INVALID_FORMAT`, and "You already have a board with the key {key}.") are defined by this change and shared with the New board field (D37). They live in `:core:presentation` string resources with `BoardKeyError.toUiText()` and `boardKeyAlreadyUsedText(key)`. The board list chunks reuse them as they are.

### Import flow as one ViewModel

`ImportBoardViewModel` drives a single `ImportState`. The import starts with the system picker and no dialog (D31):

```
Picking                            no UI; the picker is open (initial state)
ErrorShown(fileRow?, error)         Import dialog error state: file row + error banner + drop zone
Uploading(fileName)                Importing progress dialog
ChoosingKey(key, boardName?, clashKey, usedKeys, fieldError?)
Done                               route pops (the success toast is sent before)
```

The ViewModel emits `OpenPicker` once when the route opens, so "Import" / "Import board" goes straight to the picker. The route is a dialog destination that draws nothing while in `Picking`.

Actions:
- `DropZoneClick` (error state only): emits `OpenPicker` again and keeps `ErrorShown` showing behind the picker.
- `FilePicked(PickedFile)`.
- `PickerCancelled`: in `Picking` the route pops (`onDone`) with nothing shown. In `ErrorShown` the dialog stays as it was.
- `KeyChange`, `ImportWithKey`, `CancelImport`, `Cancel`.

The ViewModel keeps the picked bytes in memory to re-upload with a key (at most 5 MB). On success, it calls `ToastController.show(...)`, then emits `Imported(board)`. The Root forwards that to the `onImported` callback from `:composeApp`.

On `Failed` it also emits `BoardListChanged`, so the list reloads (D13). One `onBoardListChanged` callback serves both cases. `:composeApp` maps it to the boards reload hook.

`clashKey` comes from `CheckedFile.key` on the first clash and from the entered key on a retry. `clashKey`, `usedKeys` and the `BoardKeyOwnerLookup` argument always hold normalized keys. Every key that comes back as used goes into `usedKeys`. The dialog opens with the "already used" field error, and "Import" is disabled while the normalized entered key is in `usedKeys` (D30). The "already used" error stays under the field until the key changes. `boardName` comes from `BoardKeyOwnerLookup(key)`. `:composeApp` implements it over the board list data the boards chunk has already loaded (D17), and it returns `null` when unknown.

`SavedStateHandle` keeps the dialog phase and the entered key. The picked bytes are not saved. After process death in `Uploading` or `ChoosingKey`, the dialog returns to `ErrorShown` with the import-failed error (without a file row, since the name and size aren't saved) and the list reloads, the same as an unknown outcome. That error is the only safe statement after the request's result is lost.

`SavedStateHandle` also keeps `pickerOpened: Boolean`. `OpenPicker` is emitted on route open only when it is false. After restore in `Picking`, or in `ErrorShown` with the picker open, the ViewModel waits for the launcher's result: a pick runs the normal checks, and a cancel behaves like `PickerCancelled`.

### Export flow

`ExportBoardRoute(boardId: String, boardName: String)`. The name is needed for the dialog title and the error toasts without another load.

`ExportBoardViewModel` starts the export in `init`:
- **Success:** it emits `Share(ExportedFile)`. The Root calls `FileSharer.share(file)` and, once the sheet is presented, pops the route.
- **Failure:** it shows the matching error toast through `ToastController` and pops.

There is no state beyond "exporting", because the progress dialog is the whole UI.

### Navigation and hosting (D24, D25)

`NavGraphBuilder.transferGraph(onImported: (Board) -> Unit, onBoardListChanged: () -> Unit, onDone: () -> Unit)` registers both routes as `dialog<...>` destinations with fixed `DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false, usePlatformDefaultWidth = false)` and no window dim. Dismiss behavior per state comes only from the design-system dialog drawn inside it: `CardiganDialog`'s `onDismissRequest` sends `Cancel` (Import dialog) or `CancelImport` (key dialog), and `ProgressDialog` ignores dismiss (design-system "Progress dialog can't be dismissed"). Only one scrim is drawn. If two dialog windows can't be avoided this way, the apply stops and asks, since D24 names dialog destinations.

The boards chunks expose callbacks such as `onImportClick()` and `onExportClick(board)`. `:composeApp` navigates to both routes with `launchSingleTop = true`, so a double tap starts one flow, and maps `onImported` / `onBoardListChanged` to the board list's reload hook. The exact names of the boards-side hooks are agreed with those chunks. This change only adds what `transfer` exposes, plus the `:composeApp` wiring once both sides exist.

### Toasts (D26)

`ToastController` in `:core:presentation`:

```kotlin
interface ToastController { fun show(message: ToastMessage) }
data class ToastMessage(val text: UiText, val type: ToastType)
```

`ToastType` there is the presentation type, mapped to the design-system `ToastType` in `:composeApp`.
- `AppToastController` in `:composeApp` is a Koin `single` backed by a `Channel`.
- `App()` collects it with `ObserveAsEvents` and calls `ToastHostState.show`.
- The design system already handles timing and "one toast at a time".
- ViewModels get `ToastController` injected, so tests use a fake that records messages.

### Platform file handling (D6, D8, CLAUDE.md "Files")

Interfaces in `presentation/platform`, obtained in composition through `expect` composables:

```kotlin
interface FilePicker { fun pickJson() }                    // result delivered to onPicked / onCancelled
@Composable expect fun rememberFilePicker(onPicked: (PickedFile) -> Unit, onCancelled: () -> Unit): FilePicker
interface FileSharer { fun share(file: ExportedFile) }
@Composable expect fun rememberFileSharer(): FileSharer
data class PickedFile(val name: String, val sizeBytes: Long, val bytes: ByteArray?)   // bytes null when over the limit
```

**Android:**
- Picking uses `rememberLauncherForActivityResult(OpenDocument())` with `arrayOf("application/json")`.
- `OpenableColumns.DISPLAY_NAME` and `SIZE` come from `ContentResolver.query`. The stream is read with a cap of 5,242,880 + 1 bytes.
- If `SIZE` is unknown, the stream is counted to the end without keeping more than the cap, so the error can show the real size.
- Reading runs on `Dispatchers.IO`. The size used for the check and the file row is the number of bytes read, capped at 5,242,881 for the check. `SIZE` is used only when it is larger than that, since some providers report 0 or a wrong value.
- Sharing:
  - The file is written to `cacheDir/exports/<fileName>`, replacing any earlier file in that folder.
  - It is shared through `FileProvider` (authority `${applicationId}.fileprovider`, declared in `:androidApp`'s manifest with an `exports/` cache path).
  - It goes through `Intent.createChooser(ACTION_SEND, type application/json, FLAG_GRANT_READ_URI_PERMISSION)`.
  - No app accepts a shared file to save it locally, so the chooser gets the app's own "Save to device" entry through `EXTRA_INITIAL_INTENTS` (D36). It starts the non-exported `SaveExportActivity` (translucent, no UI of its own), which opens `CreateDocument("application/json")` with the file name, copies the cached file to the chosen URI on `Dispatchers.IO`, sends the result toast through `ToastController` and finishes. The activity only reads sanitized names from the `exports/` cache folder.

**iOS:**
- Picking uses `UIDocumentPickerViewController(forOpeningContentTypes: [UTTypeJSON], asCopy: true)`, presented from the top view controller of the key window, with a retained delegate.
- The size comes from `NSFileManager` attributes, and the bytes from `NSData` with the same cap, read off the main thread. The copied temp file is deleted after reading.
- Sharing:
  - The file is written to `NSTemporaryDirectory()/exports/<fileName>` and shared with `UIActivityViewController`.
  - On iPad, `popoverPresentationController` is anchored to the center of the presenting view, with no arrow, since there is no anchor element in the design.

Picker and sharer code lives in `androidMain` / `iosMain` of `:feature:transfer:presentation`, so it never leaks into screens (CLAUDE.md: screens never branch on the platform).

### Drop zone component (D5, D31)

Used only in the Import dialog's error state.

`DropZone(title, hint, onClick, contentDescription, modifier)` in `:core:designsystem/components/transfer`.
- It uses the Figma Drop zone Default variant (12:1108): dashed `line-strong` border via `dashedBorder`, `background`, the file icon, title and hint, with the "browse files" part in `accent`.
- It is clickable with `Role.Button`, the theme ripple and a hover/focus highlight, at full dialog width with a height well above 48dp.
- The Over variant is web-only and is not built.
- Copy comes from the caller, so the app-only strings stay in `transfer`.

### Testing

**Unit (`commonTest`):**
- `FileSizeFormatter`. Break vectors: 0, 1, 1023, 1024, 1,535, 1,536, 2,560, 1,048,575, 1,048,576, 5,242,880, 5,242,881, 7,549,747, `Long.MAX_VALUE`.
- `BoardKeyRule` (in `:core:domain`): `crd`, `CRD`, `C`, `CRDBXY`, `CR1`, ` CRD`, `ÄBC`, `ǅAB`, an emoji, empty (`Required`), a lone space (`TooShort`).
- `ExportFileName.fallback`: a UTC date across midnight in a pinned non-UTC zone.
- `ExportFileName.sanitize`: `../x.json`, `a/b.json`, `a\\b.json`, `x.txt`, `.json`, an empty name, a 101-character name, `CRD-2026-11-04.json`.
- `JsonExportFileChecker`: not JSON, BOM, array root, `null` root, wrong `format` case, `formatVersion` `"1"`/`1.0`/`1e0`/`2`/missing/huge, lowercase `board.key` (normalized), missing `board`, non-string key, unknown fields, invalid UTF-8.
- Both ViewModels with a fake repository, fake checker, fake lookup and fake toast controller. Every transition in the state machine above, including retry errors, cancel paths and process-death restore.
- `AppToastController`.

**UI (`*UiTest`, `runComposeUiTest`):**
- The picker opening first with no dialog, the Import dialog error states, key dialog, progress dialogs not dismissable, and Drop zone semantics.
- The Import and Export Roots with the real ViewModels and fakes: fake picker and sharer injected through a test-only composable parameter.

**Platform code** (`actual` picker and sharer, FileProvider) has no automated test. It is covered by the manual check below.

### Manual verification (D28)

What works with real files and no server:
- Picking a real `.json` on the Android emulator and iOS simulator.
- The JSON-only filter.
- Reading name and size.
- The size error with a >5 MB file.
- The invalid-file error with a non-Cardigan JSON file.

All of this runs before any request.

What can't run without something standing in for the server:
- **Upload step:** the Importing dialog, success toast, key clash and import-failed states.
- **Export:** export needs bytes to share, so the share sheet can't open without them.

No endpoint can be used, because none exists yet and CLAUDE.md forbids inventing one.

Also, nothing in `App()` can open the dialogs until the boards chunks add their entry points.

The user chose option A (D28). The other options are kept for the record.
- **A — Debug-only test double (chosen).** A `FixtureBoardTransferRepository` bound by Koin only in debug builds until the connect chunk replaces it. It returns a fixed export file built from a fixture shaped like the shared format. For import it returns success, or 409 when the file's key is a chosen value. A temporary debug-only entry screen in `:composeApp` opens the two dialogs and is removed when the boards entry points land. This covers every state by hand, but it is a fake data source and a screen without a design.
- **B — "Not connected" binding.** A repository that returns `Failed` for every call. Picking and all device checks run for real, and a valid file shows the designed import-failed state. The share sheet and the success and clash states are checked only in `*UiTest`s until `mob-import-export-connect`. It still needs a temporary entry point, or a wait until a boards chunk provides one.
- **C — No running-app check in this change.** Platform code is checked by hand in `mob-import-export-connect`. Done-when here is the automated tests only.

How A is built:
- `FixtureBoardTransferRepository` lives in `:feature:transfer:data` `commonMain`, with the fixture export built in code from the shared format's exact fields.
  - Export returns that file under `FIX-2026-11-04.json`, or `BoardNotFound` when the board id is a fixed "missing" UUID.
  - Import returns a Board built from the file, or `KeyAlreadyUsed` when the key is `CRD` or any key in a small fixed set, so the clash flow and the retry error can be driven from a real file.
- "Debug-only" binding: `initKoin` takes `isDebug` (on Android `(applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0`, read in `CardiganApplication`, because AGP 9 doesn't generate `BuildConfig` by default; `Platform.isDebugBinary` on iOS). It binds the fixture only when `isDebug` is true. Release builds bind nothing for `BoardTransferRepository`, and the routes are unreachable there because the debug screen is the only entry point.
- `TransferDebugScreen` in `:composeApp` has two buttons: "Import board" and "Export fixture board". The labels are hard-coded on purpose and the screen is temporary, with no design. It is shown only when `isDebug` is true, as the start of the NavHost when no other start destination exists yet (task 8.1 still asks first).
- Both are removed by the last task group before the branch is pushed. Then nothing binds the repository until `mob-import-export-connect`. The Koin `verify()` test supplies it from a test module, so the gates stay green.

## Risks / Trade-offs

- [JSON-only picker (D8)] A valid export saved by a messenger or cloud app as `application/octet-stream` or `text/plain` is greyed out and can't be picked. → User decision. If it shows up in testing, widening the filter is a one-line change in each `actual`, since the device and server checks already handle non-JSON content.
- [Board in `:core:domain` (D27)] Parallel boards planning may define another Board. → Kept minimal (id, name, key). The summary to the orchestrator asks the boards sessions to extend it rather than add their own.
- [Duplicated rules] The size limit, format literals and key rule exist on the client and the server. → Each is a small, tested constant or function referencing its shared-spec requirement. The server stays authoritative and its errors map to the same messages.
- [App() and NavHost ownership] Several wave-3 chunks need a NavHost, `AppTheme` and now the toast host in `App()`. → This change adds only the toast host and `transferGraph` registration. If no NavHost exists at apply time, the apply stops and asks (decisions.md Deferred).
- [Lookup depends on boards data] The key clash name needs the board list's loaded data, which `transfer` can't read. → `BoardKeyOwnerLookup` is a `fun interface` implemented in `:composeApp`. Until boards provides data, it returns `null` and the no-name sentence shows.
- [Memory] The picked file (≤ 5 MB) and the export bytes are held in memory. → Bounded by the import cap. Export has no size limit, so a very large board is held in memory once while writing the temp file. Acceptable for v1.
- [iPad share sheet anchor] No anchor element exists. → A centered popover with no arrow. Check on an iPad simulator during the manual check.
- [Process death during upload] The outcome is unknown after restore. → Shows import-failed and reloads the list, the same as a lost response (shared "Retries after a lost response").
- [Annotations not returned by the MCP read tools] `get_metadata`/`get_design_context` don't return frame annotations, which hid the app-only note behind D31 in the first round. → Read `node.annotations` with a read-only `use_figma` call on each page before building its UI (Figma workflow in tasks.md).
- [Temporary debug code gets pushed] The fixture repository and debug screen must not be in the branch tip when it's pushed. Their commits stay in history (CLAUDE.md forbids rewriting). → The last task group removes them, and the user pushes only after it is done. The fixture binding is debug-only even before removal.

## Open Questions

- What shows while a picked file is read (a slow cloud file can take seconds behind the empty dialog window), and whether counting an unknown-size file stops at a bound. No design exists. Ask the user before task 4.1.

- The names of the boards chunks' reload hook and loaded-board accessor. Settled with those chunks at wiring time. It doesn't change this change's specs or tasks.
