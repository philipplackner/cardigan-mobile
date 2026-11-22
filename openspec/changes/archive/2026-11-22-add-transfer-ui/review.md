# Artifact review (light): add-transfer-ui

**Verdict:** Ready
**Reviewed:** 2026-11-15, schema `careful-planning`, repo kind `code`

All findings were applied after the walk-through. No blocker is open and none was rejected.

| Pass | Blocker | Major | Minor |
|---|---|---|---|
| Testability | 0 | 0 | 5 |
| Security | 0 | 1 | 0 |
| Hidden assumptions | 0 | 3 | 2 |
| Design contract | 0 | 3 | 3 |

## Findings

### S1: Export file name from the server is used as a file path
**Severity:** major · **Status:** applied
**Location:** design.md > Platform file handling (Android "Sharing", iOS "Sharing"); specs/board-transfer/spec.md > Export opens the share sheet
**Current text:** "The file is written to `cacheDir/exports/<fileName>`, replacing any earlier file in that folder." / "The file is written to `NSTemporaryDirectory()/exports/<fileName>`"
**Failure scenario:** `fileName` comes from `Content-Disposition`. A buggy server, or a man in the middle on the plain-HTTP dev setup, sends `filename="../shared_prefs/x.xml"` or a name with `/`. The app then writes outside `exports/` and overwrites its own files. A name with no `.json` also breaks the share target's type detection.
**Fix:** Add to design.md "File sizes and names": "`ExportFileName.sanitize(name)` keeps the last path segment only, and accepts it only if it matches `[A-Za-z0-9._-]{1,100}\.json` and is not `.`/`..`. Otherwise the fallback name is used. Both `actual` sharers write only sanitized names." Add to the spec requirement "Export file name fallback": "A suggested name that contains a path separator or doesn't match the allowed characters SHALL be treated as missing." Add a unit test task to 2.4 with `../x.json`, `a/b.json`, `x.txt` and an empty name.

### A1: The file's key isn't normalized before the clash checks
**Severity:** major · **Status:** applied
**Location:** design.md > Device-side checks, Import flow as one ViewModel
**Current text:** "On success it returns `CheckedFile(key: String?)`: the file's `board.key` when it is a string, for the clash prefill (D16)." / "`clashKey` comes from `CheckedFile.key` on the first clash"
**Failure scenario:** A hand-edited file has `"key": "crd"`. The server converts it to `CRD` and returns 409. The app stores `crd` in `usedKeys`, so the normalized input `CRD` doesn't match and "Import" is enabled for a used key. The lookup also searches for `crd`, finds no board and shows the no-name sentence, and the body says "already uses the key crd".
**Fix:** design.md: "`CheckedFile.key` is the file's `board.key` passed through `BoardKeyRule.normalize`. `clashKey`, `usedKeys` and the `BoardKeyOwnerLookup` argument always hold normalized keys." Add to spec "Key clash asks for another key" a scenario: "WHEN the file's key is `crd` and the upload returns 409 THEN the field holds `CRD`, the body names the board with the key `CRD`, and "Import" is disabled." This catches an implementation that stores the raw file key.

### A2: Process death while the system picker is open
**Severity:** major · **Status:** applied
**Location:** design.md > Import flow as one ViewModel
**Current text:** "The ViewModel emits `OpenPicker` once when the route opens" / "After process death in `Uploading` or `ChoosingKey`, the dialog returns to `ErrorShown`"
**Failure scenario:** On Android the picker is another app's activity, and the app process is often killed behind it. On restore the route is recreated, the new ViewModel emits `OpenPicker` again and opens a second picker. Or the picked result arrives at the re-registered launcher while the ViewModel is opening a new picker. The restore list leaves out `Picking`, so two implementers handle it differently.
**Fix:** design.md, after the process-death sentence: "`SavedStateHandle` also keeps `pickerOpened: Boolean`. `OpenPicker` is emitted on route open only when it is false. After restore in `Picking` or in `ErrorShown` with the picker open, the ViewModel waits for the launcher's result: a pick runs the normal checks, a cancel behaves like `PickerCancelled`." Add "restore in `Picking` with a delivered result" to the break vectors in task 5.2.

### A3: Reading the picked file: thread, waiting UI and unknown sizes
**Severity:** major · **Status:** applied with edits
**Location:** design.md > Platform file handling (Android)
**Current text:** "If `SIZE` is unknown, the stream is counted to the end without keeping more than the cap, so the error can show the real size."
**Failure scenario:** A file from a cloud provider (Drive, OneDrive) is downloaded while it is read. A large one takes many seconds, or minutes when counted to the end. The route is in `Picking` and draws nothing, but its dialog window still blocks touches, so the board list looks frozen. Some providers report `SIZE` as 0 or wrong, so the size check and the displayed size can disagree with the bytes actually read.
**Fix:** design.md: "Reading runs on `Dispatchers.IO`. The size used for the check and the file row is the number of bytes read, capped at 5,242,881 for the check. `SIZE` is used only when it is larger than that." The UI during a slow read has no design. Per CLAUDE.md, add to decisions.md Deferred: "What shows while a picked file is read, and whether counting an unknown-size file stops at a bound." Ask the user before task 4.1.

### A4: A UTF-8 BOM is a test vector without an expected result
**Severity:** minor · **Status:** applied
**Location:** design.md > Testing (`JsonExportFileChecker`), spec > Non-Cardigan files are rejected on the device
**Current text:** "`JsonExportFileChecker`: not JSON, BOM, array root, …"
**Failure scenario:** A valid export saved by a Windows editor starts with a BOM. `parseToJsonElement` fails, so one implementer rejects the file and another strips the BOM. The server likely accepts it, because Jackson detects BOMs.
**Fix:** design.md "Device-side checks": "A leading UTF-8 BOM (`EF BB BF`) is removed before decoding."

### A5: Double tap on an entry point starts two flows
**Severity:** minor · **Status:** applied
**Location:** design.md > Navigation and hosting
**Current text:** "`:composeApp` navigates to the routes"
**Failure scenario:** A fast double tap on "Export as JSON" pushes two `ExportBoardRoute`s. That sends two export requests and opens two share sheets. The same happens with two pickers for import.
**Fix:** "`:composeApp` navigates to both routes with `launchSingleTop = true`."

### D1: The dialog destination's properties are fixed, and its window sits behind a second dialog window
**Severity:** major · **Status:** applied
**Location:** design.md > Navigation and hosting
**Current text:** "registers both routes as `dialog<...>` destinations with `DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)` while a Progress dialog shows. The Import dialog itself keeps the normal dismiss behavior"
**Failure scenario:** A `dialog<>` destination's `DialogProperties` are set at registration and can't change per state. Inside it, `CardiganDialog` and `ProgressDialog` (`BasicAlertDialog`) each open their own window, so there are two dialog windows, with a possible double scrim. Back is handled by whichever window gets it. In `Picking` the outer window draws nothing but still blocks touches. Implementers will end up with different back behavior.
**Fix:** "Both routes are `dialog<...>` destinations with fixed `DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false, usePlatformDefaultWidth = false)` and no window dim. Dismiss behavior per state comes only from the design-system dialog drawn inside it: `CardiganDialog`'s `onDismissRequest` sends `Cancel`/`CancelImport`, and `ProgressDialog` ignores dismiss. Task 7.1 adds a UiTest: back in `ErrorShown` ends the import, back in `Uploading` does nothing, and only one scrim is drawn." If two windows still can't be avoided, register the routes differently and record that as a design question for the user, since D24 names dialog destinations.

### D2: `ImportError.KeyInvalid` doesn't say which rule failed
**Severity:** major · **Status:** applied
**Location:** design.md > Repository and errors; spec > Key field errors from the server
**Current text:** "`sealed interface ImportError : Error { FileTooLarge; InvalidFile; KeyAlreadyUsed; KeyInvalid; Failed }`" vs spec "the New board key field's error for that code under it"
**Failure scenario:** The spec needs the 400's field error code (`TOO_SHORT`, `TOO_LONG`, `INVALID_FORMAT`) to pick the message. `KeyInvalid` has no payload, so the scenario "Server rejects the format" can't be met. The connect chunk would have to change the domain type.
**Fix:** "`data class KeyInvalid(val rule: BoardKeyError) : ImportError` with `enum class BoardKeyError { TooShort, TooLong, InvalidFormat }`, shared with `BoardKeyRule`'s validation result. A 400 `key` error with any other code maps to `Failed`." Ask the user to confirm the last sentence, since it decides which copy shows.

### D3: Cross-module test fixtures don't exist in this KMP setup
**Severity:** major · **Status:** applied
**Location:** tasks.md > 1.2
**Current text:** "with a `FakeToastController` in that module's test fixtures for feature tests"
**Failure scenario:** KMP `commonTest` source sets aren't visible to other modules, and `build-logic` has no `testFixtures`. The transfer tests can't import the fake, and the task fails or someone creates a new module without asking.
**Fix:** "1.2 … The `FakeToastController` lives in the `commonTest` of each module that needs it (`:feature:transfer:presentation`, `:composeApp`). Ask the user before adding a shared test module." Change "the fake records messages in order" to a test in `:feature:transfer:presentation`.

### D4: `BuildConfig.DEBUG` isn't generated in `:androidApp`
**Severity:** minor · **Status:** applied
**Location:** design.md > How A is built; tasks.md > 9.1
**Current text:** "`initKoin` takes `isDebug` (`BuildConfig.DEBUG` from `:androidApp`, …)"
**Failure scenario:** AGP 9 turns off `buildConfig` by default, and `androidApp/build.gradle.kts` doesn't turn it on, so the code doesn't compile.
**Fix:** "`isDebug` on Android is `(applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0`, read in `CardiganApplication`."

### D5: The debug code does reach the remote, in history
**Severity:** minor · **Status:** applied
**Location:** design.md > Risks ("Temporary debug code gets pushed"); tasks.md > 10.2
**Current text:** "The fixture repository and debug screen must not reach the remote branch."
**Failure scenario:** 9.2 commits the code and CLAUDE.md forbids rewriting commits, so the pushed branch contains it in history. A reviewer of the risk line expects otherwise.
**Fix:** "The fixture repository and debug screen must not be in the branch tip when it's pushed. Their commits stay in history (CLAUDE.md forbids rewriting)."

### D6: D29 says nothing is committed, but the tasks commit after each group
**Severity:** minor · **Status:** applied with edits
**Location:** tasks.md > 1.4, 2.5, …; decisions.md D29
**Current text:** decisions.md: "The change is named `add-transfer-ui`, and nothing is committed." vs tasks.md: "Commit only when all gates pass (CLAUDE.md Git Workflow)."
**Failure scenario:** The implementer either skips every commit the tasks call for or breaks D29.
**Fix:** If D29 meant the planning session, add to tasks.md under Gates: "D29's 'nothing is committed' applies to the planning session. Implementation commits follow CLAUDE.md." Otherwise remove the commit steps. The user decides. decisions.md isn't edited.

### T1: "Successful export" tests a name this change doesn't produce
**Severity:** minor · **Status:** applied
**Location:** spec > Export opens the share sheet > Successful export
**Current text:** "**WHEN** the export of the board with the key `CRD` succeeds on 4 November 2026 UTC **THEN** the share sheet opens with a file named `CRD-2026-11-04.json`"
**Failure scenario:** The name comes from the repository's `ExportedFile.fileName`. A test passes as long as the fake returns that name, even if the sharer renames the file or changes the bytes.
**Fix:** "**WHEN** the export returns the file name `CRD-2026-11-04.json` and its bytes **THEN** the share sheet opens with a file of exactly that name and those bytes, byte for byte." Catches a sharer that re-encodes the content or appends an extension.

### T2: Half-up rounding has no case at exactly one half
**Severity:** minor · **Status:** applied
**Location:** spec > File sizes use base 1024
**Current text:** "#### Scenario: Half a kilobyte — 1,535 bytes … "1 KB""
**Failure scenario:** 1,535 bytes is 1.499 KB, so it doesn't test the half. Half-even rounding (`roundToInt` on some paths, `BigDecimal.HALF_EVEN`) passes every scenario and shows 2,560 bytes as "2 KB".
**Fix:** Add "#### Scenario: Exactly half a kilobyte — **WHEN** a picked file has 2,560 bytes **THEN** its size is shown as "3 KB"". Catches half-even rounding.

### T3: Back and scrim tap on the Import error dialog aren't specified
**Severity:** minor · **Status:** applied
**Location:** spec > Cancel import closes everything
**Current text:** ""Cancel" in the Import dialog SHALL close it and end the import."
**Failure scenario:** `CardiganDialog` dismisses on back and scrim taps. One implementer ends the import, another ignores the dismiss.
**Fix:** Append: "Back, Escape and a scrim tap on the Import dialog SHALL do the same as "Cancel". On the key dialog they SHALL do the same as "Cancel import"." Add a scenario: "WHEN the Import dialog shows an error and the user does the Android back gesture THEN it closes and nothing is uploaded."

### T4: "The integer `1`" is open for `1.0` and `1e0`
**Severity:** minor · **Status:** applied
**Location:** spec > Non-Cardigan files are rejected on the device
**Current text:** "its `formatVersion` isn't the integer `1`"
**Failure scenario:** The design rejects `1.0`, but the spec allows reading `1.0` as the integer 1. A test for `1.0` can be written either way.
**Fix:** "its `formatVersion` isn't the JSON number literal `1` (`1.0`, `1e0` and `"1"` are rejected)". Add the scenario "WHEN a file has `"formatVersion": 1.0` THEN nothing is uploaded and the invalid-file error shows".

### T5: Which error shows when a key breaks two rules
**Severity:** minor · **Status:** applied
**Location:** spec > Retry with another key
**Current text:** "An invalid key SHALL show the New board key field's error for that rule"
**Failure scenario:** `1` is both too short and has the wrong format, and `ABCDE1` is too long and has the wrong format. Without an order, the transfer tests and the New board tests can expect different messages.
**Fix:** "When a key breaks more than one rule, the field shows the error the New board key field shows for that key. Until that order exists, length rules come first." Confirm the order with `mob-board-list-ui`.

## Trust boundaries

- **Imported file (user-picked, untrusted):** size cap before upload, parse with kotlinx (deep nesting handled without stack overflow), server validates. Stopped. Slow or unbounded reads: see A3.
- **Server responses → local file system (export name):** a path in the name can write outside `exports/`. Not stopped. See S1.
- **Shared export file → other apps:** `FileProvider` limited to `cache/exports/`, read grant only, file overwritten next time. Stopped. The full board stays in the cache until the next export (accepted in D29).
- **iOS picker temp copy:** deleted after reading. Stopped.
- **Debug fixture binding:** bound only when `isDebug`, removed before push. Stopped. The commits stay in history (D5).
- **Network (key part, upload):** belongs to `mob-import-export-connect`. Out of scope here.

## Coverage

- Read all of decisions.md (D1–D31), proposal.md, design.md, tasks.md and `specs/board-transfer/spec.md`: 17 requirements and 31 scenarios, all checked.
- Read the main spec `design-system` (Progress dialog, Toast timing), `CardiganDialog.kt`, `FileRow.kt`, `InitKoin.kt` and the module file list.
- Read shared specs `board-export-format` (in full) and `domain-board` "Board key format". Not read: `api-errors` and `api-conventions`. Both are used only through decisions.md facts.
- No other active changes, so there are no cross-change conflicts.
- Not checked: Figma frames and annotations (decisions.md records them), and the Android/iOS platform API details beyond what the findings name.

## Applied changes

The user accepted every recommendation (2026-11-15).

- **Files changed:** `specs/board-transfer/spec.md`, `design.md`, `tasks.md`. `openspec validate add-transfer-ui --strict` passes.
- **A3, applied with edits:** the fix asked for an entry in decisions.md Deferred, but decisions.md is never edited by this review. The open question went into design.md "Open Questions" instead, and task 4.1 now stops and asks before starting. Consider moving it to decisions.md Deferred yourself.
- **D6, applied with edits:** applied the option that reads D29's "nothing is committed" as the planning session. tasks.md says so under Gates. If D29 meant the implementation, revert that line and remove the commit steps.
- **D2:** a 400 `key` error with an unknown code now maps to `Failed`. This was the recommendation the user accepted.
- **Contradictions with decisions.md:** none. D24 ("dialog destinations") is kept. D1's fix adds a stop-and-ask if two dialog windows can't be avoided.
- **proposal.md:** no changes needed. None of the fixes change its wording.
- **Non-standard artifacts:** none in the change folder.
- **Accepted risks:** none.
