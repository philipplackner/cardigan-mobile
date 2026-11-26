# Design

## Context

See proposal.md (Why) and decisions.md for the settled choices (D1–D12). The current state that shapes the approach:

- Kover reads `CodeExclusions` (class and package globs) in `build-logic/convention`. PIT reads `CodeExclusions.pitClassGlobs` plus `MutationExclusions.classGlobs`, the FANN annotation filter and the custom `FCOMPOSEUI` interceptor from `build-logic:pitest-filters` (`ComposeUiFilter.java`).
- `pitest-filters` reaches PIT through a detached configuration on `com.plcoding.cardiganapp.buildlogic:pitest-filters` in `MutationConventionPlugin` / `MutationRootConventionPlugin`. Its runtime dependencies resolve transitively.
- Filter and exclusion unit tests live in `build-logic/convention/src/test` (`ComposeUiFilterTest`, `MutationExclusionsTest`, `MutationCountsTest`), on kotlin.test.
- Baselines from 2026-11-26: line coverage 66.0%, or 82.0% once the token and nav globs apply to Kover. Mutation score 68.4% in total; the per-module scores are in decisions.md (Facts).

## Goals / Non-Goals

**Goals:**
- Reach the one-time targets: line coverage ≥ 80% in total, and a mutation score ≥ 80% in every module with mutants.
- Make every exclusion match by rule, so that new code following the layout needs no build change. The two named runtime-bound members are the only exception.

**Non-Goals:**
- No threshold defaults in the build. `coverage.minLine` and `mutation.minScore` stay opt-in flags. There is no per-module threshold flag: the per-module check is read from the existing summary.
- No Compose UI tests written to move the numbers.
- No changes to how the tools run (selection, threads, caching, outputs).

## Decisions

### 1. Token and nav globs move into the shared list
The token globs (`*.designsystem.theme.*Colors*`, `App*`, `ShadowsKt`, `TextStylesKt`, `TypographyKt`, with their `$*` variants) and the nav globs (`*.navigation.*Route`, `*GraphKt`, `*NavigationKt`, `*NavHostKt`, with their `$*` variants) move from `MutationExclusions.classGlobs` to `CodeExclusions.classes`. Kover and PIT then both read them from one place, so the two lists can't drift apart. Compiler-noise globs (`$WhenMappings`, `$$inlined$`, `$$serializer`) stay PIT-only.
*Alternative:* have Kover also read `MutationExclusions`. Rejected because it would pull compiler-noise globs into coverage, and D6 keeps coverage narrower than mutation testing.

### 2. One interceptor per new rule, in `pitest-filters`
Each rule is its own `MutationInterceptor` with its own factory and feature name. All are enabled in `MutationExclusions.features` next to `FCOMPOSEUI`. Separate features keep each rule small and easy to test on its own, and a rule can be switched off if PIT changes.
- **Inline functions (`FKOTLININLINE`)**: read the class's `kotlin.Metadata` annotation with `kotlin-metadata-jvm` (`KotlinClassMetadata.readLenient`). Collect the JVM signatures of functions flagged `inline` in classes, file facades and multi-file parts. Drop mutants whose method matches one of these signatures, and every mutant in a class whose enclosing method (`EnclosingMethod` / `outerMethod`) is one of these functions, directly or through outer classes, as `ComposeUiFilter.isEnclosedByUi` does. The original body of a suspend lambda or anonymous object inside an inline function never runs: callers run their own `$$inlined$` copy. Today all 60 `HttpClientExt` mutants sit in such classes (`HttpClientExtKt$safeCall$2`, `$get$2`, ...). Matching by JVM signature, not by name, keeps a non-inline overload with the same name in (spec scenario "Non-inline function in the same file").
- **Navigation receivers (`FNAVRECEIVER`)**: a method qualifies when it is a top-level function (static on a file facade) and its first JVM parameter is `androidx/navigation/NavController`, `NavHostController` or `NavDestination`. Its lambdas (`<name>$lambda…`) and the classes it encloses are dropped the same way `ComposeUiFilter` handles them. The receiver check reads the descriptor, so no metadata is needed. Kotlin compiles an extension receiver to the first parameter, so a plain function whose first parameter is one of these types is matched too. Member extensions inside an `object` or class are not matched.
- **Composable-local code**: extend `ComposeUiFilter` rather than add a new feature. Kotlin compiles a local function to `<outer>$<name>` (for example `BoardRoot$scrollToItem`), as a static method on the same class or a synthetic class. The filter already handles `$lambda` names, so treating `<uiMethod>$<anything>` as enclosed covers local functions too. The interceptor's unit test pins that a top-level function whose name merely shares the prefix stays in, as it already does for lambdas.
- **Named members (`FNAMEDEXCLUDE`)**: `UiText.asStringAsync` and the lambdas of `HttpClientFactory.create` are dropped by a small interceptor that takes `Class#methodGlob` pairs from one list in `MutationExclusions`: `UiText*#asStringAsync*` and `HttpClientFactory#create$lambda*`. The class part is a glob on the binary name. `UiText` is an interface compiled with `DefaultImpls`, so its `asStringAsync` mutants sit in `UiText$DefaultImpls` and in the bridge methods of the nested implementations, not in `UiText` itself. PIT's own `excludedMethods` was not used, because it matches method names in every class, and it would also drop same-named methods elsewhere.
*Alternative for inline functions:* scan callers for `$i$f$<name>` markers. Rejected: callers often live in other modules (feature data modules call `HttpClientExt` helpers), and the marker carries only the name.

### 3. `kotlin-metadata-jvm` on the filter's runtime classpath only
Added as `implementation` of `pitest-filters`, with the version ref `kotlin` (2.4.20) in the catalog. The detached configuration resolves it into PIT's classpath during `mutationReport` only. The gates never resolve it, which the existing requirement "Mutation testing stays out of the normal workflow" already covers.

### 4. Filter tests
Each rule gets unit tests in `build-logic/convention/src/test/.../pitest/`, next to `ComposeUiFilterTest`. They build synthetic `ClassNode`s, or compile tiny Kotlin fixtures for metadata, where a hand-built `@Metadata` is impractical. They cover:
- inline vs non-inline overloads, inline in an `object`, `suspend inline`, a class enclosed by an inline function, and unreadable metadata
- the three receiver types, a non-nav first parameter, nested lambdas, and a member extension in an `object`
- a local function in a composable, and a same-prefix sibling
- named members, the `asStringAsync` bridges in `$DefaultImpls` and nested implementations, and a same-named method in another class

The real-report scenarios from the spec deltas are checked afterwards on full runs.

### 5. Test work is ordered by module score gap
After the exclusions land, rerun `mutationReport` and work module by module. Approximate post-exclusion needs, from the baseline:

| Module | Now | After exclusions | Work |
|---|---|---|---|
| `:core:data` | 15.8% | ~100% (16 mutants left) | Confirm only |
| `:core:presentation` | 42.9% | ~100% (3 left) | Confirm only |
| `:composeApp` | 22.2% | ~67% (12 left) | `destinationFor`, remaining survivor |
| `:core:designsystem` | 19.1% | ~19% (47) | `AnchoredPositionProvider`, `ToastHostState`, `ToastVisuals` |
| `:core:domain` | 68.9% | ~80% (139) | `LabelOrder` code points, `BoardKeyRule`, text-rule survivors |
| `:feature:boards:data` | 63.4% | ~63% (69) | NotConnected sources, `InMemoryBoardRepository` |
| `:feature:boards:domain` | 75.0% | 75.0% (36) | `BoardKeyPrefill`, getters through behavior |
| `:feature:boards:presentation` | 70.9% | ~73% (589) | `BoardViewModel` first (76 survivors), then Rename/New/Delete/BoardList ViewModels, `BoardDialog`, `BoardState` |
| `:feature:transfer:presentation` | 75.8% | 75.8% (95) | `ImportBoardViewModel`, `ExportBoardViewModel` |
| `:feature:auth:presentation` | 81.7% | 81.7% | `LoginViewModel` `withError` survivors (margin) |

Coverage is already above 80% after decision 1. The small logic gaps (`TransferUiText`, `BoardKeyErrorText`, `BoardFieldErrors`, `UiText`, `RelativeTimeFormatter`) are covered along the way by the module work, because the same tests kill their mutants.

### 6. Test style for killing survivors
Survivors in `invokeSuspend` of ViewModels mean that state transitions during a coroutine aren't asserted: loading flags, error mapping, and the order of state emissions. Tests assert each intermediate state with Turbine on `state`. Delays use `StandardTestDispatcher` with explicit advancing where the ViewModel uses timers. Data class getters and no-coverage mutants are killed through behavior that reads them, not through getter-only tests.

### 7. Equivalent mutants and product-code changes
When a survivor can't be killed because the mutant is equivalent, refactor the code so the mutant no longer exists, with behavior unchanged and its own commit (D7). Typical cases are a redundant bound check and a defensive `coerceAtLeast` that can't trigger. When a refactor would change anything users can see, stop and ask. Preview data lists in `RegisterRoot`, `LoginRoot`, `NewBoardRoot`, `RenameBoardRoot`, `BoardListScreen` and `ImportBoardScreen` move into `*Previews.kt` files in the same package, which the existing preview globs exclude.

### 8. Flaky timing test
Remove the `measureTime` block, the duration assertion and the `println` from `specNearMatchThroughTheRuleIsTooLongFast`. Keep the `TooLong` assertion and rename the test to describe what it now checks. The three `*RejectedFast` tests that use `assertPatternRejectsFast` keep their limit (D8 names only one test). If one of them fails a `coverageReport` or `mutationReport` run, stop and ask.

## Risks / Trade-offs

- [`kotlin-metadata-jvm` version must read the metadata of the project's Kotlin version] → pin it to the `kotlin` version ref, so a Kotlin bump moves both together. If `KotlinClassMetadata.readLenient` throws or returns unknown metadata for a class from a tested module, the interceptor fails the PIT run with the class name and the metadata version. It never treats the class as having no inline functions. Unit-test fixtures are compiled by Gradle's embedded Kotlin (`kotlin-dsl`), so only the real run in task 3.6 checks the project's Kotlin version.
- [kotlin-stdlib from `kotlin-metadata-jvm` on PIT's classpath could clash with the tested module's stdlib] → PIT loads plugins in its own process classpath, separate from the minion's test classpath. Check on the first full `mutationReport` run that mutant counts in untouched modules don't change.
- [Moving token and nav globs changes coverage numbers for everyone] → intended (D2). CLAUDE.md is updated in the same commit.
- [Per-module 80% for small modules swings on single mutants (`:composeApp` has 12 after exclusions)] → accepted (D3). Kill all reachable mutants there rather than aiming for exactly 80%.
- [The `$<name>` rule for composable-local code could also catch a non-local function that shares the prefix] → the existing `ComposeUiFilter` logic only treats methods in the same class or enclosed classes as children. The unit test pins a same-prefix sibling that stays in.
- [Equivalent-mutant refactors touch tested product code] → each in its own commit, with all four gates green. Changes users can see go back to the user.
- [Baselines move while the work runs] → rerun both reports at the start of each module group and work from the fresh numbers, not the table above.

## Migration Plan

Nothing to deploy. The build changes apply on the next run. Rollback is a revert of the build-logic commits.
