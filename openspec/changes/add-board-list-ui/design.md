# Design

## Context

See proposal.md for the motivation and decisions.md for every product decision (D1–D23).

Current state:
- `feature/boards/{domain,data,presentation}` contain only build files.
- `:composeApp` has an `AppNavHost` that starts on an empty `AppStartRoute` and registers `transferGraph` with no-op `onImported` / `onBoardListChanged`. `appModule` binds `BoardKeyOwnerLookup { null }` and `AppToastController`.
- `:core:domain` has `Board(id: Uuid, name, key)`, `BoardKeyRule` (normalize, validate) and `BoardKeyError`. `:core:presentation` has `UiText` (no plurals), the key error strings, `boardKeyAlreadyUsedText(key)` and `ToastController`.
- `:core:designsystem` has everything the screens need: `Sidebar`, `NavItem`, `PageHeader`, `BrandAppBar`, `TitleAppBar`, `BoardTile`, `BoardRow`, `EmptyState`, `Skeleton`, `RefreshBox`, `CardiganDialog`, `Field`, `Input`, `Menu`/`MenuItem`/`MenuHeader`/`MenuDivider`, `CardiganButton(isLoading)`, `LocalWindowClass` and the icons (`Plus`, `More`, `Upload`, `Label`, `LogOut`, `Rename`, `Trash`, `Board`, `Boards`, `CloudOff`).
- The backend has no board endpoints, so there is no generated board DTO. Nothing in this change touches `:core:data`'s HTTP code.
- No code normalizes text to NFC/NFD. Common Kotlin has no Unicode normalizer.

## Goals / Non-Goals

**Goals:**
- One board store that the list, the sidebar, the dialogs, transfer's key lookup and later the board view all read, so no screen holds its own copy.
- Board dialogs as navigation destinations that the board view can open as they are.
- A text rule in core that board, column and label names share.

**Non-Goals:**
- The HTTP data source, DTO mapping and ProblemDetail parsing (`mob-boards-connect`). The data source interface here returns already-typed errors.
- Labels page, logout implementation and the session email source (auth).
- Sorting boards on the client.

## Decisions

### Module layout

```
:core:domain        text/TextRules.kt                      normalizeText, codePointLength, validateName → NameError?
                    text/UnicodeNormalization.kt           expect fun String.toNfc(), expect fun String.toNfd()
                    (androidMain/iosMain actuals)          java.text.Normalizer / NSString precomposed/decomposed mapping
:core:presentation  util/UiText.kt                         + UiText.PluralResource(id, quantity, args)
                    text/NameErrorText.kt                  NameError.toUiText() + shared name strings
:feature:boards:domain        BoardSummary, BoardRepository, RemoteBoardDataSource, BoardErrors, BoardKeyPrefill, BoardOrder
:feature:boards:data          InMemoryBoardRepository, FixtureBoardDataSource (temporary), di/BoardsDataModule
:feature:boards:presentation  boardlist/ (Root, Screen, ViewModel, State, Action, Event)
                              newboard/, renameboard/, deleteboard/ (dialog Root/Screen/ViewModel each)
                              sidebar/BoardsSidebarSection + ViewModel
                              util/RelativeTimeFormatter, BoardUiText
                              navigation/BoardsRoutes, boardsGraph, BoardListReloader
                              di/BoardsPresentationModule
:composeApp         shell/AppShell.kt                      window-class switch, Sidebar on tablet/desktop
                    navigation/AppNavHost.kt               boardsGraph + transferGraph, start = BoardListRoute
                    di/AppModule.kt                        BoardKeyOwnerLookup over the store, fixture binding (temporary)
```

`boards:domain` gets `api(projects.core.domain)` and kotlinx-datetime like `transfer:domain`. `boards:presentation` gets `core:presentation` and `core:designsystem` if the `cmp.feature` plugin doesn't bring them already. `:composeApp` gets `feature.boards.*`. No new libraries.

### Domain model (D4)

```kotlin
data class BoardSummary(
    val id: Uuid, val name: String, val key: String,
    val cardCount: Int, val createdAt: Instant, val updatedAt: Instant,
) { fun toBoard() = Board(id, name, key) }
```

Core `Board` stays as it is. `kotlin.time.Instant` follows CLAUDE.md. There is no column count.

### Errors

```kotlin
sealed interface BoardSaveError : Error {
    data class Network(val error: DataError.Network) : BoardSaveError   // no network, 5xx, unknown codes
    data class InvalidFields(val name: NameFieldError?, val key: KeyFieldError?) : BoardSaveError
    data object NotFound : BoardSaveError
}
sealed interface NameFieldError { data object Required; data class TooLong(val max: Int); data object Invalid }
sealed interface KeyFieldError  { data class Rule(val error: BoardKeyError); data object AlreadyUsed; data object Invalid }
```

`NameFieldError.Invalid` covers `INVALID_CHARACTER` and any unknown name code. `KeyFieldError.Invalid` covers any key code other than the ones with their own text. Both follow `api-errors` "Clients handle unknown error codes by status". How the ProblemDetail `code` and `fieldErrors` become these types is the connect chunk's job (CLAUDE.md open item). The fixture returns them directly. Delete returns `EmptyResult<BoardSaveError>`, and its `NotFound` is turned into success by the repository (D11).

### Text rule in core (D9)

`normalizeText(input, multiLine = false)`: CRLF and lone CR → LF, then NFC via `toNfc()`, then trim the code points with the Unicode White_Space property. The list is fixed: U+0009–000D, 0020, 0085, 00A0, 1680, 2000–200A, 2028, 2029, 202F, 205F, 3000. Kotlin's `isWhitespace()` differs from this set, so it isn't used.

`validateName(normalized, max)` returns the first broken rule in `api-errors` order: empty → `Required`, a control char U+0000–001F or an unpaired surrogate → `InvalidCharacter`, code point length > max → `TooLong(max)`. Code points are counted by walking UTF-16 and counting a valid surrogate pair as one.

`toNfc`/`toNfd` are `expect` functions in `:core:domain`. The Android actual uses `java.text.Normalizer`, the iOS actual `NSString.precomposedStringWithCanonicalMapping` / `decomposedStringWithCanonicalMapping`. This is platform code behind `expect`/`actual` as CLAUDE.md asks, and needs no library. Board view and labels reuse the rule with their own max.

The submitted name is the normalized text, so the client sends what it validated.

### Key prefill (D7)

`BoardKeyPrefill.fromName(name)`: `toNfd()`, drop chars whose category is `NON_SPACING_MARK`, `uppercase()`, keep `A`–`Z`, `take(4)`, and return "" when fewer than 2 remain. "ß" becomes "SS" through `uppercase()`. Letters with no decomposition (Ø, Æ, Ł) are dropped. That matches "A–Z only" and is listed in the tests so web can compare.

The New board ViewModel holds `keyEdited: Boolean`. Any key `onValueChange` sets it. While it's false, every name change rewrites the key.

### Board store (D2, D13)

```kotlin
interface BoardRepository {
    val boards: StateFlow<List<BoardSummary>?>          // null = never loaded
    suspend fun refresh(): EmptyResult<DataError.Network>
    suspend fun ensureLoaded(): EmptyResult<DataError.Network>  // joins or starts the first load
    suspend fun createBoard(name: String, key: String): Result<BoardSummary, BoardSaveError>
    suspend fun renameBoard(id: Uuid, name: String): Result<BoardSummary, BoardSaveError>
    suspend fun deleteBoard(id: Uuid): EmptyResult<BoardSaveError>
}
interface RemoteBoardDataSource { getBoards(); createBoard(name, key); renameBoard(id, name); deleteBoard(id) }
```

`InMemoryBoardRepository(remote)` is a Koin `single`:
- `refresh()` replaces the list with the server's list as it is. Concurrent calls share one request: a `Mutex` plus the running `Deferred`. A failed refresh leaves the list unchanged.
- `ensureLoaded()` returns at once when `boards` is non-null. Otherwise it joins the running refresh or starts one, and returns its result.
- Saves that complete while a refresh is running are recorded. Before the refresh result is published, the store re-applies them by id (insert or replace by `BoardOrder`, or remove). Saves that complete while no refresh runs update the list directly.
- Session end (add-auth-ui "Clearing user data on sign-out"): `InMemoryBoardRepository` also binds add-auth-ui's `SessionScopedStore`. Its `clear()` resets `boards` to `null` and bumps a session counter. A refresh or save applies its result only if the counter hasn't changed since it started, so a late result can't bring back the previous user's boards. add-auth-ui's `AppViewModel` calls `clear()`. If this change lands before add-auth-ui, it adds the `clear()` method now and add-auth-ui binds it.
- `createBoard` inserts the response by `BoardOrder`. `renameBoard` removes the old entry and inserts the response by `BoardOrder`. `deleteBoard` removes it, also on `NotFound`. A rename `NotFound` removes the board and returns `NotFound`.
- `BoardOrder` is the shared rule from `api-conventions` "Board list order": updatedAt desc, createdAt desc, id string asc. It's used only to place one returned board into the loaded list. The loaded list itself is never re-sorted.
- D13: ties go by createdAt desc, then id asc, as the shared spec says (the coordinator withdrew "by name").

This keeps the screens consistent without a refetch after every mutation. Whether connect refetches after mutations is the connect chunk's question (chunk 33). The store's interface allows either.

### Reload hook

`BoardListReloader` (boards:presentation, Koin `single`) owns an app-scoped `CoroutineScope`. Its `reload()` calls `repository.refresh()` and shows the D12 refresh toast on failure. It exposes `isReloading: StateFlow<Boolean>`. Pull to refresh in the list goes through it too, so the import reload and a pull share one indicator and one request. `:composeApp` maps transfer's `onImported` and `onBoardListChanged` to `reload()`.

The first load starts in `:composeApp` once the session is `SignedIn` (or at start while add-auth-ui hasn't landed), through `repository.ensureLoaded()`, so the sidebar, the key lookup and the board dialogs have data whichever screen is restored. The list ViewModel calls `ensureLoaded()` on start, which joins that request, and shows the error state if it fails. "Try again" calls `repository.refresh()` from the ViewModel. Both failures show the error state, not a toast. A pull to refresh from the empty or error state goes through the reloader and shows its toast on failure, and the state stays as it is.

### Board list screen

State:

```kotlin
data class BoardListState(
    val content: Content,                // Loading | Error | Loaded(boards: List<BoardItemUi>)
    val isRefreshing: Boolean,
    val boardCount: Int,
    val accountEmail: String?,           // from the Root parameter (D18)
    val isMenuOpen: Boolean,
)
data class BoardItemUi(val id: String, val name: String, val key: String, val cardCount: UiText, val updated: UiText)
```

- The ViewModel combines `repository.boards` with the reloader's flag. It maps items with `RelativeTimeFormatter(clock, timeZone)` each time the list changes. There is no ticking (D6).
- `Clock` and `TimeZone` come from Koin, so tests pin them.
- `Root` reads `LocalWindowClass`:
  - Phone: `LazyColumn` with a large-title header item, `BoardRow`s and a footer count. The bar switches from `BrandAppBar` to `TitleAppBar("Boards", raised = true)` once the header item has left the viewport (`firstVisibleItemIndex > 0`).
  - Tablet/Desktop: `PageHeader("Boards", meta = count, actions = Import)` above a `LazyVerticalGrid(GridCells.Adaptive(BoardTileMinWidth))`. `BoardTileMinWidth` is about 260 dp, taken from the Figma Board tile, and defined once (D19).
  - Both sit in a `RefreshBox`.
- Actions are `OnBoardClick(id)` (the ViewModel looks up the name and key and emits `OpenBoard(id, name, key)`), `OnRenameClick(id)`, `OnDeleteClick(id)`, `OnNewBoardClick`, `OnImportClick`, `OnLabelsClick`, `OnLogOutClick`, `OnRefresh` and `OnRetry`. Navigation actions are forwarded by the Root to callbacks.

Relative time goes through `UiText`. "N minutes ago" and "N hours ago" use plural resources. "Oct 9" is built from a short-month string array resource plus the day. Strings live in boards:presentation and move to core when card detail needs them.

`UiText.PluralResource(id: PluralStringResource, quantity: Int, args)` is added to `:core:presentation`, using `pluralStringResource` / `getPluralString`.

### Dialogs as destinations (D2, D16)

`NewBoardRoute`, `RenameBoardRoute(boardId: String)` and `DeleteBoardRoute(boardId: String)` are `dialog<>` destinations in `boardsGraph`. They use the transfer pattern: fixed `DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false, usePlatformDefaultWidth = false)` and no window dim. `CardiganDialog.onDismissRequest` decides by state: it is ignored while saving and sends Cancel otherwise. `CardiganDialog` already sizes itself by window class, which covers D16.

The board view registers nothing new. It calls `navigateToRenameBoard(id)` / `navigateToDeleteBoard(id)`, which the boards graph exposes. All three destinations navigate with `launchSingleTop = true`.

Each dialog ViewModel reads the board by id from `repository.boards`. If `boards` is null (for example after process death), it calls `ensureLoaded()` and waits. It closes at once only when the loaded list has no board with this id, or the load fails. The ViewModels:
- `NewBoardViewModel`: name, key, keyEdited, submitted, nameError, keyError, isSaving. Submit runs `normalizeText` + `validateName(…, 100)`, `BoardKeyRule.normalize` + `validate`, and the local clash against `repository.boards` (D14). After the first submit, every change re-validates (D8). The button is enabled when `!isSaving && (!submitted || no errors)`.
- `RenameBoardViewModel`: the same name handling. If the normalized name equals the current name, it emits `Done` (D15).
- `DeleteBoardViewModel`: builds the body from `cardCount` (D5).
- Results (D21):
  - Create success emits `Created(id, name, key)` from the server's response. The Root pops and calls `onOpenBoard(id, name, key)` (D10, D23).
  - Rename success emits `Renamed(newName)`, delete success (including 404) emits `Deleted`. The Root pops and calls `onRenamed(newName)` / `onDeleted()`, which `boardsGraph` takes per caller. The board list passes no-ops, since the store already updated it. The board view passes its own handling (back to the list after a delete, no toast).
  - Cancel and an unchanged rename pop with no result.
  - `Network` keeps the dialog open and shows the toast through `ToastController` (D11).
  - `InvalidFields` sets the field errors.
  - `NotFound` on rename shows the toast and emits `Done`.
- `SavedStateHandle` keeps the name, key, keyEdited and submitted fields. A save that was running at process death is not resumed. Its outcome is unknown, and the list reloads on the next start anyway.

### App shell (D1, D17, D18)

`AppShell(navController, accountEmail: String?, onLogOut: () -> Unit)` in `:composeApp`:
- Phone: only the `AppNavHost`. The bar and its ⋯ menu belong to the board list screen, which gets `accountEmail`, `onImportClick`, `onLabelsClick` and `onLogOutClick` from `boardsGraph` parameters.
- Tablet/Desktop: `Row { Sidebar(...); AppNavHost(...) }`. `mainNav` holds "All boards" (selected when the current destination is `BoardListRoute`) and "Labels" (no-op). `boards` holds `BoardsSidebarSection(onOpenBoard, onNewBoardClick)` from boards:presentation, which reads `repository.boards`. `Sidebar` takes a non-null email. When `accountEmail` is null, the shell passes "" and the avatar initial is left out. If `Sidebar` can't hide them, a small `Sidebar` change to accept null is part of the shell task.
- `:composeApp` feeds `accountEmail` from `SessionManager.state` (`SignedIn(email)` → email, anything else → null) and `onLogOut` from `AppViewModel.onLogout()`, both from add-auth-ui (D20). If this change lands before add-auth-ui, it passes null and a no-op with a TODO, and add-auth-ui does the wiring. If it lands after, this change does it and replaces add-auth-ui's temporary `AppStartRoute` inside the session gate with `BoardListRoute`.
- Opening a board (list tap, sidebar tap, after create) goes through one callback, `onOpenBoard(boardId: String, name: String, key: String)` (D23). `:composeApp` maps it to `navigate(BoardRoute(boardId, name, key))`, the route add-board-view-ui defines in `boards:presentation`. If the board view hasn't landed, it is a no-op with a TODO. Labels is a no-op with a TODO for `mob-labels-ui`.

### Wiring and placeholders

- `AppStartRoute` is removed, and `BoardListRoute` becomes the start destination (inside add-auth-ui's session gate if that landed first, D20).
- `BoardKeyOwnerLookup { key -> repository.boards.value?.firstOrNull { it.key == key }?.name }`.
- `onImported` / `onBoardListChanged` → `BoardListReloader.reload()`.
- `{ navController.navigate(ImportBoardRoute) { launchSingleTop = true } }` is passed as `onImportClick`. transfer has no navigate helper.

### Accessible names (D22)

Accessible names come from string resources in the shared style: "Actions for board “{name}”" on row and tile ⋯ buttons, "Menu" on the phone bar ⋯, "New board", "Import board", "Labels", "All boards" and "Log out". Rows and tiles merge their semantics into "{name}, {key}, {cards}, updated {time}".

### Temporary fixture (D3)

`FixtureBoardDataSource` in `boards:data/fixture`, bound in a `fixtureBoardsModule` that `:composeApp` loads instead of a real data source. This binding is the D3 debug entry. No extra UI is added:
- It holds the six boards from the Figma frames and answers after 600 ms.
- Trigger names make every error path checkable by hand: a create or rename with a name starting with "fail" → `Network`; key `USED` → `AlreadyUsed`; renaming or deleting a board named "Gone" → `NotFound`; a refresh within 2 s of a "fail" save → `Network`.
- The whole fixture package and module are removed in one commit before archive. `mob-boards-connect` binds the Ktor source.

### Tests

- Unit:
  - `TextRules`: CRLF, lone CR, NFC (decomposed é), whitespace-only, NBSP/U+3000 trim, tab/LF/NUL in single-line, unpaired surrogate, emoji = 1, ZWJ family = 5, 100 vs 101, a leading BOM (not White_Space, kept).
  - `BoardKeyPrefill`: Garden irrigation, Ärger, ß, Ø, digits, "X 1", emoji, empty.
  - `RelativeTimeFormatter` with a fake clock in a pinned non-UTC zone: every boundary in the spec, DST day, New Year.
  - `BoardOrder` and `InMemoryBoardRepository`: insert, rename move, delete, 404 handling, single-flight refresh, failed refresh keeps the list.
  - All ViewModels with a fake repository and a recording `ToastController`.
- `*UiTest`: list states per window class, collapse, menus, each dialog's states, sidebar section, and the shell with fakes.
- Android host and iOS simulator, as the gates require.

## Risks / Trade-offs

- [The board view rebases onto this change and adds to the same module] → Keep `BoardRepository` focused on the list. The board view adds its own repository rather than widening this one. The dialog routes are public so it doesn't need internals.
- [add-auth-ui and this change both edit `AppNavHost` and the start route] → The shell takes plain parameters, and whichever lands second does the wiring (D20). Expect a small merge in `:composeApp`.
- [iOS and Android NFC could differ on new Unicode versions] → Both follow the OS's Unicode tables, and the backend stays authoritative (`text-length`). Server field errors still show under the field.
- [A local insert by `BoardOrder` could disagree with the next server list] → The next refresh shows the server's order. Both use the same rule, so this only happens with clock skew between create and list.
- [Two dialog windows on top of each other] → Same dialog-destination approach as transfer, which already proved one scrim works on both platforms.

## Migration Plan

None. Nothing is released. The fixture binding is removed before archive and replaced by the Ktor data source in `mob-boards-connect`.


## Open Questions

- `cardCount` comes from be-cards, not be-boards. If boards-connect runs before be-cards, the connect change decides the placeholder (e.g. hide the card count). This change stays as it is, because the fixture provides `cardCount`.
