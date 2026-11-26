## MODIFIED Requirements

### Requirement: Compose UI code is not mutated
Methods annotated `@Composable`, methods that return `Modifier`, lambdas and local functions declared in either, and classes they enclose SHALL NOT be mutated. Other code in the same files SHALL stay in. The mutation report SHALL exclude this code although the coverage report only excludes `@Composable` methods.

#### Scenario: Screen file
- **WHEN** a report is generated for `:feature:boards:presentation`
- **THEN** it has no mutants in `BoardListScreen`, in its `onClick` lambdas, in `drawDividerAbove`, or in the `LaunchedEffect` lambdas of `BoardScreen`
- **AND** `BoardViewModel` still has mutants

#### Scenario: Local function inside a composable
- **WHEN** a report is generated for `:feature:boards:presentation`
- **THEN** it has no mutants in `BoardRootKt`, including its local `scrollToItem` function and the suspend lambdas `BoardRoot` starts

#### Scenario: Non-UI function next to composables
- **WHEN** a report is generated for `:composeApp`
- **THEN** `destinationFor` in `SessionGateKt` has mutants

## ADDED Requirements

### Requirement: Inline functions are not mutated
Functions declared `inline`, and lambdas and classes declared in them, SHALL NOT be mutated, because host tests run the copy inlined into the caller and can never kill a mutant in the original body. They SHALL be detected without listing them by name, so new inline functions are excluded without a build change. Non-inline functions in the same file SHALL stay in.

#### Scenario: Inline helpers
- **WHEN** a report is generated for `:core:data` and `:core:domain`
- **THEN** it has no mutants in `safeCall`, `responseToResult`, the `get`/`post`/`patch`/`delete` helpers of `HttpClientExtKt`, or in any `HttpClientExtKt$<name>$<n>` class enclosed by them, nor in `forEachCodePoint` or `map`/`onSuccess`/`onFailure` of `ResultKt`

#### Scenario: Non-inline function in the same file
- **WHEN** a report is generated for `:core:data` and `:core:domain`
- **THEN** `constructRoute` and `toCommonNetworkError` in `HttpClientExtKt`, and `asEmptyResult` in `ResultKt`, still have mutants

### Requirement: NavController helpers are not mutated
Top-level functions whose first JVM parameter is a `NavController`, `NavHostController` or `NavDestination` (this includes extension receivers), and lambdas and classes declared in them, SHALL NOT be mutated, because only UI tests can run them. They SHALL be detected by receiver type, so new helpers are excluded without a build change. Other functions in the same file SHALL stay in.

#### Scenario: Back-stack helpers
- **WHEN** a report is generated for `:composeApp`
- **THEN** it has no mutants in `openBoardId` or `previousNonDialogDestinationIsBoardList` in `AppShellKt`, or in `replaceBackStackWith` and `isAuthScreen` in `SessionGateKt`, or their lambdas

#### Scenario: Pure function next to helpers
- **WHEN** a report is generated for `:composeApp`
- **THEN** `destinationFor` in `SessionGateKt` still has mutants

### Requirement: Runtime-bound members are not mutated
`UiText.asStringAsync`, including its `$DefaultImpls` body and the bridge methods in the nested implementations, and the plugin-config lambdas of `HttpClientFactory.create` SHALL NOT be mutated, because host tests can't observe them without the Compose resources runtime or a real client configuration. They SHALL be matched by name. Other members of both classes SHALL stay in.

#### Scenario: Named members
- **WHEN** a report is generated for `:core:presentation` and `:core:data`
- **THEN** it has no mutants in `asStringAsync` of `UiText`, `UiText$DefaultImpls` or any nested `UiText` implementation, or in the lambdas of `HttpClientFactory.create`
- **AND** `UiText$DynamicString.getValue` and `UiText$PluralResource.getQuantity` still have mutants
