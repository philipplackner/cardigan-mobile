# Tasks

**Gates:** `./gradlew :androidApp:assembleDebug testAndroidHostTest iosSimulatorArm64Test :composeApp:linkDebugFrameworkIosSimulatorArm64`. This change doesn't touch `iosApp/`. Commit only when all gates pass (CLAUDE.md Git Workflow). D29's "nothing is committed" applies to the planning session. Implementation commits follow CLAUDE.md.

**Figma workflow for UI tasks:**
1. `create_session` on `MALwbOph7Uj2QtiTy21JaQ`.
2. Load `figma:figma-design-to-code`.
3. Run `get_design_context` on the frame or component, then check with `get_screenshot`.
4. Read the frames' annotations with a read-only `use_figma` call on `node.annotations`. The MCP read tools don't return them, and app-only notes decide behavior.
5. If a Figma call fails, stop and report the call and node.

Frame IDs come from `get_metadata` on page `9:9` (Import & export) and `9:3` (Design System). Don't copy them from this file.

**"Figma check"** means the composable is compared in code with `get_design_context` values (tokens, sizes, spacing, copy, icons, states), with no high-severity difference left. Previews stay in `commonMain` and are never rendered with the Android CLI.

## 1. Core additions

- [x] 1.1 Add `Board(id: Uuid, name: String, key: String)` to `:core:domain` (design "Board in `:core:domain`"). Verify: `:core:domain` compiles for Android and iOS. Before adding it, check whether a boards change has already added a Board. If it has, extend that one instead of adding a second.
- [x] 1.2 Add `ToastController`, `ToastMessage` and the presentation `ToastType` to `:core:presentation`, Verify: compiles. The `FakeToastController` lives in the `commonTest` of each module that needs it (`:feature:transfer:presentation`, `:composeApp`), because KMP `commonTest` isn't visible across modules. Ask the user before adding a shared test module. A unit test in `:feature:transfer:presentation` confirms the fake records messages in order.
- [x] 1.3 Add the tap-only `DropZone` (used only in the Import dialog's error state) to `:core:designsystem/components/transfer` from the Figma Drop zone Default variant. It takes title, hint (accent part), `onClick` and `contentDescription`, and has `Role.Button`, the theme ripple, hover/focus highlight and a touch area of at least 48dp. No Over variant. Verify: previews in light and dark at phone and desktop window classes, at 2× font scale, then the Figma check.
- [x] 1.4 Run all gates. Verify they pass, then commit `feat(designsystem): add tap-only drop zone` (1.3) and `feat(core): add board model and toast controller` (1.1, 1.2) as two commits.

## 2. Transfer domain

- [x] 2.1 Add `BoardTransferRepository`, `ExportedFile`, `ExportError`, `ImportError` (with `KeyInvalid(rule: BoardKeyError)`), `BoardKeyError`, `ExportFileChecker`/`CheckedFile` and `BoardKeyOwnerLookup` to `:feature:transfer:domain` (design "Repository and errors"). Verify: compiles for Android and iOS.
- [x] 2.2 Add `ImportFileRules` (limit 5,242,880 bytes) and `FileSizeFormatter` (design "File sizes and names"). List the break vectors from design "Testing" first, then one unit test per angle. Verify: the spec scenarios "Small file", "Half a kilobyte", "One byte over the limit" and "At the limit" are among the passing tests on Android host and the iOS simulator.
- [x] 2.3 Add `BoardKeyRule` (normalize, isValid) with adversarial unit tests from design "Testing". Verify: the tests pass, including `crdb` → `CRDB` valid and `C` invalid.
- [x] 2.4 Add `ExportFileName.fallback(key, exportedAt)` and `ExportFileName.sanitize(name)` (design "File sizes and names") with unit tests, including `../x.json`, `a/b.json`, `x.txt` and an empty name for `sanitize`, in a pinned non-UTC time zone and a fake `Clock` where needed. Verify: `2026-11-04T23:30:00Z` with `CRD` gives `CRD-2026-11-04.json`, and a missing key or unparseable `exportedAt` gives `board-export.json`.
- [x] 2.5 Run all gates. Verify they pass, then commit `feat(transfer): add transfer domain rules`.

## 3. Transfer data

- [x] 3.1 Add `JsonExportFileChecker` in `:feature:transfer:data` with a Koin module binding it to `ExportFileChecker`. Verify: unit tests for every vector in design "Testing" pass, and a fixture shaped like the shared export format (`board-export-format` "Exact fields") passes and returns its key.
- [x] 3.2 Run all gates. Verify they pass, then commit `feat(transfer): add device-side export file check`.

## 4. Platform file handling

- [x] 4.1 Before starting, ask the user the open question in design "Open Questions" about what shows while a picked file is read. Add `FilePicker`, `PickedFile` and `rememberFilePicker` with the Android `actual` (reading on `Dispatchers.IO`, size from the bytes read) (OpenDocument, `application/json`, name and size query, capped read, counting when the size is unknown). Verify: `:androidApp:assembleDebug` passes.
- [x] 4.2 Add the iOS `actual` for `rememberFilePicker` (`UIDocumentPickerViewController`, `UTTypeJSON`, `asCopy`, retained delegate, capped read, temp copy deleted). Verify: `:composeApp:linkDebugFrameworkIosSimulatorArm64` passes.
- [x] 4.3 Add `FileSharer` and `rememberFileSharer`:
  - Android: cache `exports/` folder, `FileProvider` and `ACTION_SEND` chooser. Add the `<provider>` and `res/xml` paths file to `:androidApp`.
  - iOS: temp `exports/` folder and `UIActivityViewController` with the iPad popover anchor.

  Verify: both builds pass, and the merged Android manifest contains the provider with authority `${applicationId}.fileprovider`.
- [x] 4.4 Run all gates. Verify they pass, then commit `feat(transfer): add platform file picker and share sheet`.

## 5. Import UI

- [x] 5.1 Add the string resources for import from proposal "Copy", in `:feature:transfer:presentation`. For the key field errors, reuse the `:core:presentation` strings from `mob-board-list-ui` if they exist (design "Key rule"). If they don't, stop and ask the user for the `TOO_SHORT` and `TOO_LONG` copy, then add all key error strings and the "already used" string to `:core:presentation` with a key-error → `UiText` mapper. Verify: every import string in the proposal exists once, no import text is hard-coded in composables, and the key strings exist only in `:core:presentation`.
- [x] 5.2 Add `ImportBoardViewModel` with State/Action/Event per design "Import flow as one ViewModel", including retry, cancel paths and the `SavedStateHandle` restore. Unit test every transition with a fake repository, checker, lookup and `FakeToastController`. List the break vectors first: first picker cancelled, picker cancelled from the error dialog, double pick, pick during upload, cancel during clash, retry clash, 400 on retry, `Failed` on retry, 413 and 422 from the server, Import disabled for every used key (including lowercase input of a used key), restore in each phase, and restore in `Picking` with a delivered picker result (no second picker opens), and a lowercase file key that clashes. Verify: the tests pass on Android host and the iOS simulator.
- [x] 5.3 Add the import composables (Import dialog error state with FileRow, error Banner and DropZone; Importing progress dialog; Choose a different key dialog with field error) and `ImportBoardRoot`, which opens the picker when the route opens and draws nothing while in `Picking`. Verify: previews for every state in light and dark at phone and desktop classes, then the Figma check against the phone and desktop frames on `9:9`.
- [x] 5.4 Add `ImportBoardUiTest` (Root + real ViewModel, fake picker, repository, lookup and toast controller). Cover:
  - The picker opens first with no dialog, and cancelling it ends the import with nothing shown.
  - Drop zone semantics, and re-picking from the error dialog (cancel keeps the error).
  - Size and invalid-file errors.
  - Progress dialog not dismissable by back.
  - Success sends the toast and calls `onImported`.
  - Clash with and without a known name, opening with the "already used" error and Import disabled until the key changes.
  - Invalid key field error.
  - "Cancel import".
  - Failed calls `onBoardListChanged`.

  Verify: `iosSimulatorArm64Test` passes.
- [x] 5.5 Run all gates. Verify they pass, then commit `feat(transfer): add import dialogs`.

## 6. Export UI

- [x] 6.1 Add the export string resources from proposal "Copy". Verify: every export string exists once.
- [x] 6.2 Add `ExportBoardViewModel` (design "Export flow") with unit tests: success emits `Share`, 404 and `Failed` send the matching error toast and finish, and nothing is shown after sharing. Verify: the tests pass on both targets.
- [x] 6.3 Add the Exporting progress dialog and `ExportBoardRoot`. Verify: previews in light and dark at phone and desktop classes, then the Figma check against the Exporting frames.
- [x] 6.4 Add `ExportBoardUiTest` with a fake sharer and repository: the dialog title and body, the sharer called with the file name and bytes unchanged, error toasts, and no toast after a successful share. Verify: `iosSimulatorArm64Test` passes.
- [x] 6.5 Run all gates. Verify they pass, then commit `feat(transfer): add export flow`.

## 7. Navigation and DI in the feature

- [x] 7.1 Add `ImportBoardRoute`, `ExportBoardRoute(boardId, boardName)` and `NavGraphBuilder.transferGraph(onImported, onBoardListChanged, onDone)` as dialog destinations with the Progress dialog dismiss rules. Add the transfer presentation Koin module. Add a UiTest: back in `ErrorShown` ends the import, back in `Uploading` does nothing, and only one scrim is drawn. Verify: compiles, the UiTest passes, and the Koin module's definitions resolve in a `verify()` test with the repository supplied by a test module.
- [x] 7.2 Run all gates. Verify they pass, then commit `feat(transfer): add transfer routes and koin module`.

## 8. App wiring

- [x] 8.1 Check `:composeApp` for a NavHost and `AppTheme` wrapper in `App()`. If either is missing, stop and ask the user how to proceed (decisions.md Deferred). Verify: the user's answer is recorded in decisions.md.
- [x] 8.2 Add `AppToastController` (Koin `single` for `ToastController`) and the app-wide `ToastHost` in `App()` using `ObserveAsEvents`. Add a unit test that a shown message reaches the collector and a second replaces the first. Verify: the test passes on both targets.
- [x] 8.3 Register `transferGraph` in the NavHost. Add the `BoardKeyOwnerLookup` implementation and the reload wiring from the boards hooks if they exist. If they don't, use a lookup that returns `null` and a no-op reload, and note in decisions.md which hook is still pending. Add the transfer Koin modules to `initKoin`. Verify: all gates pass.
- [x] 8.4 Commit `feat(transfer): wire import and export into the app`.

## 9. Manual verification (option A, temporary)

- [x] 9.1 Add `FixtureBoardTransferRepository` in `:feature:transfer:data` per design "How A is built", with a unit test that its fixture export passes `JsonExportFileChecker`. Add the `isDebug` parameter to `initKoin` (`ApplicationInfo.FLAG_DEBUGGABLE` read in `CardiganApplication` on Android, `Platform.isDebugBinary` on iOS) and bind the fixture only when it is true. Verify: the unit test passes, and the Koin `verify()` test passes for both values of `isDebug` (release with a test module for the repository).
- [x] 9.2 Add the temporary `TransferDebugScreen` in `:composeApp`, shown only when `isDebug` is true. Its "Import board" button opens `ImportBoardRoute`. Its "Export fixture board" button opens `ExportBoardRoute` with a fixed id and name. Verify: all gates pass, then commit `chore(transfer): add temporary debug entry and fixture repository` with a body saying both are removed before push (group 10).
- [x] 9.3 On the Android emulator and the iOS simulator, pick each of these through the system picker and check the matching state against the spec:
  - A real valid export file.
  - A valid export whose key is `CRD` (clash flow, retry with `CRD` again and with a new key).
  - A >5 MB JSON file.
  - A non-Cardigan JSON file.
  - A non-JSON file, which must be greyed out.

  Also run export to the share sheet on both platforms, including an iPad simulator for the popover. Verify: a short checklist with each result is added under this task, and every check passes.

  Results (2026-11-15, checked by the user on both platforms):
  - [x] `valid-export.json`: imported, success toast — Android, iOS
  - [x] `clash-crd.json`: key dialog, retry with `CRD` clashes again, a new key imports — Android, iOS
  - [x] `board-backup.json` (7.2 MB): size error with "7.2 MB" — Android, iOS
  - [x] `notes.json`: invalid-file error — Android, iOS
  - [x] `not-json.txt`: greyed out in the picker — Android, iOS
  - [x] Export: share sheet on iOS (iPhone and iPad popover) and Android
  - [x] Android share sheet had no local save target. Fixed by D36 ("Save to device"); save to Downloads, the toast and cancel were checked on the emulator.

## 10. Remove temporary verification code (before pushing)

- [x] 10.1 Delete `FixtureBoardTransferRepository` and its test, `TransferDebugScreen`, its route and NavHost entry, the `isDebug` fixture binding, and the `isDebug` parameter if nothing else uses it. Verify: `grep -rn "Fixture\|TransferDebug" --include=*.kt .` finds nothing outside `commonTest` fixtures for the JSON checker.
- [x] 10.2 Run all gates. Verify they pass, then commit `chore(transfer): remove temporary debug entry and fixture repository`. Verify: `git log` shows this commit after the one from 9.2, so the branch can be pushed.

## 11. Shared board key errors (D37)

- [x] 11.1 Move `BoardKeyRule` and `BoardKeyError` to `:core:domain` (`core.domain.board`), add `Required`, and check in the order empty, too short, too long, format. Move `BoardKeyRuleTest` with them and add the empty and lone-space cases. Verify: the tests pass.
- [x] 11.2 Replace the `:core:presentation` key strings with the four approved strings, add `BoardKeyError.toUiText()` and `boardKeyAlreadyUsedText(key)` there, and drop the transfer copy of the mapper. Verify: the key strings exist only in `:core:presentation`, and the import ViewModel and UI tests show "Enter a key." and "Use only the letters A–Z.".
- [ ] 11.3 Hand-off for `mob-import-export-connect`: map a 400 `key` field error `REQUIRED`/`TOO_SHORT`/`TOO_LONG` to its own `BoardKeyError` and `INVALID_FORMAT` or any other code to `InvalidFormat`. Verify: a `MockEngine` test per code, including an unknown one.

## Workflow follow-up

- `mob-import-export-connect` adds `KtorBoardTransferRepository`, the ProblemDetail mapping, `Content-Disposition` parsing with `ExportFileName.fallback`, and timeouts.
- Archive the change after review.
