# Notes

## Baseline after the exclusions (task 4.1, 2026-11-26)

Taken after groups 1–3, before any new test.

**Coverage** (`./gradlew coverageReport`, 20 modules): line 82.0%, branch 80.8%. Before the change: line 66.0%, branch 77.5%.

**Mutation** (`./gradlew mutationReport`): total 75.1%, test strength 84.0% (1005 killed, 191 survived, 143 no coverage, 1339 mutants). Before the change: 68.4% of 1477.

| Module | Before | After exclusions | Killed / total |
|---|---|---|---|
| `:composeApp` | 22.2% | 66.7% | 8 / 12 |
| `:core:data` | 15.8% | 100.0% | 7 / 7 |
| `:core:designsystem` | 19.1% | 19.1% | 9 / 47 |
| `:core:domain` | 68.9% | 79.9% | 111 / 139 |
| `:core:presentation` | 42.9% | 100.0% | 3 / 3 |
| `:feature:auth:domain` | 97.3% | 97.3% | 72 / 74 |
| `:feature:auth:presentation` | 81.7% | 81.7% | 174 / 213 |
| `:feature:boards:data` | 63.4% | 65.2% | 45 / 69 |
| `:feature:boards:domain` | 75.0% | 75.0% | 27 / 36 |
| `:feature:boards:presentation` | 70.9% | 73.0% | 429 / 588 |
| `:feature:transfer:data` | 87.5% | 87.5% | 14 / 16 |
| `:feature:transfer:domain` | 85.0% | 85.0% | 34 / 40 |
| `:feature:transfer:presentation` | 75.8% | 75.8% | 72 / 95 |

Mutant counts in modules the new rules don't touch are unchanged against the run before the change.

### Biggest gaps per module (outer class, mutant count)

| Module | Survived | No coverage |
|---|---|---|
| `:composeApp` | `AppViewModel` 1 | `SessionGateKt` 2, `AppToastHostKt` 1 |
| `:core:data` | — | — |
| `:core:designsystem` | — | `AnchoredPositionProvider` 25, `ToastHostState` 5, `ToastVisuals` 4, `SpinnerSize` 2 |
| `:core:domain` | `LabelKt` 11, `BoardKeyRule` 4, `TextRulesKt` 3 | `BoardKeyRule` 2, `Card` 2, `Board` 2, `BoardContent` 1 |
| `:core:presentation` | — | — |
| `:feature:auth:domain` | — | `AuthError` 2 |
| `:feature:auth:presentation` | `LoginViewModel` 12, `RegisterState` 5, `RegisterViewModel` 4, `LoginState` 1 | `RegisterState` 6, `LoginState` 6, `LoginViewModel` 3, `AuthBanner` 1 |
| `:feature:boards:data` | `InMemoryBoardRepository` 9 | `NotConnectedBoardDetailRepository` 5, `NotConnectedRemoteBoardDataSource` 5, `InMemoryBoardRepository` 3, `NotConnectedCardCreateRepository` 1 |
| `:feature:boards:domain` | `BoardKeyPrefill` 2, `BoardOrder` 1 | `BoardSummary` 3, `BoardSaveError` 2, `NameFieldError` 1 |
| `:feature:boards:presentation` | `BoardViewModel` 76, `RenameBoardViewModel` 12, `NewBoardViewModel` 8, `DeleteBoardViewModel` 6 | `BoardViewModel` 15, `BoardListViewModel` 3, `SidebarBoardUi` 3, `BoardListEvent` 3 |
| `:feature:transfer:data` | `JsonExportFileChecker` 2 | — |
| `:feature:transfer:domain` | `FileSizeFormatter` 2, `ExportFileName` 1 | `FileSize` 1, `ExportedFile` 1, `CheckedFile` 1 |
| `:feature:transfer:presentation` | `ImportBoardViewModel` 12, `ExportBoardViewModel` 4 | `TransferUiTextKt` 2, `ImportPhase` 2, `ImportBoardViewModel` 1, `PickedFileInfo` 1 |

### Order for groups 5–9

By gap to 80% (mutants still to kill): `:feature:boards:presentation` (~42), `:core:designsystem` (29), `:feature:boards:data` (~11), `:feature:transfer:presentation` (~4), `:feature:boards:domain` (~2), `:composeApp` (2), `:core:domain` (~1). `:core:data` and `:core:presentation` are already at 100%. The tasks keep their group order; each group works from the numbers above.
