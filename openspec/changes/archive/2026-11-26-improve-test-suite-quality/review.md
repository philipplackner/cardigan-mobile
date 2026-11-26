# Artifact review (light): improve-test-suite-quality

**Verdict:** Ready
**Reviewed:** 2026-11-26, schema `careful-planning`, repo kind `code`

| Pass | Blocker | Major | Minor |
|---|---|---|---|
| Testability | 0 | 0 | 2 |
| Security | 0 | 0 | 0 |
| Hidden assumptions | 0 | 1 | 1 |
| Design contract | 1 | 1 | 2 |

## Findings

### D1: Inline rule misses the suspend-lambda classes where the inline mutants sit
**Severity:** blocker · **Status:** applied
**Location:** design.md > Decisions §2 "Inline functions (`FKOTLININLINE`)"; specs/mutation-testing/spec.md > Requirement: Inline functions are not mutated; tasks.md > 3.2
**Current text:** "Collect the JVM signatures of functions flagged `inline` in classes, file facades and multi-file parts. Drop mutants whose method matches one of these signatures."
**Failure scenario:** In the current `core/data` report, all 60 `HttpClientExt` mutants are in `invokeSuspend` of the classes `HttpClientExtKt$safeCall$2`, `$get$2`, `$post$2`, `$patch$2` and `$delete$2`, not in the inline methods. A filter that matches only the inline method signatures drops none of them. `:core:data` stays near 16% instead of reaching the ~100% that design §5 expects. Group 3 has to be redone, and its unit tests (inline vs non-inline overloads) pass without catching the gap.
**Fix:** design.md §2, replace the inline bullet's middle sentence with: "Drop mutants whose method matches one of these signatures, and every mutant in a class whose enclosing method (`EnclosingMethod` / `outerMethod`) is one of these functions, directly or through outer classes, as `ComposeUiFilter.isEnclosedByUi` does. The original body of a suspend lambda or anonymous object inside an inline function never runs: callers run their own `$$inlined$` copy." Spec requirement, first sentence: "Functions declared `inline`, and lambdas and classes declared in them, SHALL NOT be mutated, …". Spec scenario "Inline helpers", THEN: "it has no mutants in `safeCall`, `responseToResult`, the `get`/`post`/`patch`/`delete` helpers of `HttpClientExtKt`, or in any `HttpClientExtKt$<name>$<n>` class enclosed by them, nor in `forEachCodePoint` or `map`/`onSuccess`/`onFailure` of `ResultKt`". Task 3.2: add the unit test case "a `suspend inline` function whose body starts a suspend lambda: mutants in the enclosed `$<name>$<n>` class are dropped".
*Wrong implementation caught:* a method-signature-only filter, which leaves the 60 `HttpClientExtKt$…$2` mutants.

### D2: `UiText.asStringAsync` mutants sit in `DefaultImpls` and the subclasses, not in `UiText`
**Severity:** major · **Status:** applied
**Location:** design.md > Decisions §2 "Named members (`FNAMEDEXCLUDE`)"; tasks.md > 3.5; specs/mutation-testing/spec.md > Requirement: Runtime-bound members are not mutated
**Current text:** "fed by a `Class#methodGlob` list in `MutationExclusions` with `UiText#asStringAsync*` and `HttpClientFactory#create$lambda*`"
**Failure scenario:** `UiText` is an interface compiled with `DefaultImpls`. Today's `core/presentation` report has the four `asStringAsync` mutants in `UiText$DefaultImpls`, `UiText$DynamicString`, `UiText$Resource` and `UiText$PluralResource` (bridge methods). An exact `UiText` class match drops none of them, so `:core:presentation` ends at 3 of 7 (43%), not the "~100% (3 left)" in design §5.
**Fix:** tasks.md 3.5 and design §2: "… with `UiText*#asStringAsync*` (the interface, its `$DefaultImpls` class and the bridge methods in its nested implementations) and `HttpClientFactory#create$lambda*`. The class part is a glob on the binary name." Add to task 3.5's unit tests: "an `asStringAsync` bridge in a nested implementation class and in `$DefaultImpls` is dropped". Spec scenario "Named members", add: "**AND** `UiText$DynamicString.getValue` and `UiText$PluralResource.getQuantity` still have mutants".

### A1: A metadata read failure would silently bring the inline mutants back
**Severity:** major · **Status:** applied
**Location:** design.md > Risks / Trade-offs, first bullet; Decisions §4 "Filter tests"
**Current text:** "pin it to the `kotlin` version ref, so a Kotlin bump moves both together. The inline unit test fails loudly if reading breaks."
**Failure scenario:** `build-logic/convention` uses `kotlin-dsl`, so the "tiny Kotlin fixtures" in its tests are compiled by Gradle's embedded Kotlin, not 2.4.20. If a later project Kotlin version writes metadata that the pinned reader can't parse, the unit tests stay green. If the interceptor catches the error and treats the class as having no inline functions, about 82 mutants come back without a clear cause. The per-module scores drop and nobody knows why.
**Fix:** design.md risk bullet: "pin it to the `kotlin` version ref, so a Kotlin bump moves both together. If `KotlinClassMetadata.readLenient` throws or returns unknown metadata for a class from a tested module, the interceptor fails the PIT run with the class name and the metadata version. It never treats the class as having no inline functions. Unit-test fixtures are compiled by Gradle's embedded Kotlin, so only the real run in task 3.6 checks the project's Kotlin version." Task 3.2: add the unit test "unreadable metadata fails with the class name".
*Wrong implementation caught:* `try { read } catch { emptySet() }`.

### D3: Task 7.2 asks to kill survivors in a function the new inline rule removes
**Severity:** minor · **Status:** applied
**Location:** tasks.md > 7.2; design.md > §5 table, `:feature:boards:data` row
**Current text:** "the `InMemoryBoardRepository` survivors (`applySave`, `refresh`, `setIfSession`, `collect`, `clear`, `createBoard`)" / "63.4% (71)"
**Failure scenario:** `setIfSession` is `private inline`, so `FKOTLININLINE` drops its 2 mutants. The task points the implementer at mutants that won't exist, and the "after exclusions" count is off.
**Fix:** tasks.md 7.2: "… the `InMemoryBoardRepository` survivors (`applySave`, `refresh`, `collect`, `clear`, `createBoard`; `setIfSession` is inline and leaves the report after group 3) …". Design table row: "63.4% | ~63% (69) |".

### D4: The navigation rule's scope differs across decisions, proposal, spec and design
**Severity:** minor · **Status:** applied
**Location:** proposal.md > What Changes "Mutation exclusions"; specs/mutation-testing/spec.md > Requirement: NavController helpers are not mutated; design.md §2 "Navigation receivers"
**Current text:** proposal: "helpers on a NavController/NavHostController receiver"; spec: "Functions whose receiver is a `NavController`, `NavHostController` or `NavDestination`"; design: "static on a file facade and its first parameter is …"
**Failure scenario:** The spec says any function with that receiver. The design matches only file-facade statics, but also catches a non-extension function whose first parameter is a `NavController`. A helper declared as an extension inside an `object`, or a plain `fun go(nav: NavController, …)`, gets different treatment depending on which artifact the implementer follows. `isAuthScreen` (a `NavDestination` extension) depends on the spec's wider list, which D4/D5 and the proposal don't mention.
**Fix:** spec requirement, first sentence: "Top-level functions whose first JVM parameter is a `NavController`, `NavHostController` or `NavDestination` (this includes extension receivers), and lambdas and classes declared in them, SHALL NOT be mutated, …". Proposal: "helpers whose receiver or first parameter is a NavController, NavHostController or NavDestination". Task 3.3: add "a member extension in an `object` stays in (only top-level functions are matched)".

### T1: "Runtime-bound members" says other members stay in, but no scenario checks it
**Severity:** minor · **Status:** applied
**Location:** specs/mutation-testing/spec.md > Requirement: Runtime-bound members are not mutated
**Current text:** "Other members of both classes SHALL stay in." (no scenario)
**Failure scenario:** A broad match such as `UiText*#*` drops the `DynamicString`/`PluralResource` getters too, and the only scenario still passes.
**Fix:** covered by the AND line in D2's fix.
*Wrong implementation caught:* a class-wide `UiText*` exclusion.

### T2: "Design tokens are excluded" doesn't say token-building logic goes out too
**Severity:** minor · **Status:** applied
**Location:** specs/test-coverage/spec.md > Requirement: Excluded code, scenario "Design tokens are excluded"
**Current text:** "it lists no color, typography, text style, shadow or `App*` dimension token class from the theme package **AND** it still lists `WindowClassKt`"
**Failure scenario:** The globs also remove logic that isn't a token: `appTypography` in `TypographyKt` picks styles by window class, and `AppThemeKt` holds `toColorScheme`. The requirement says "Window size logic in the theme package SHALL stay in". One implementer reads that as keeping `appTypography` measured, and another follows the globs. Both can pass the scenario.
**Fix:** requirement sentence: "Window size logic in the theme package SHALL stay in. Here that means `WindowClassKt` only. The whole `AppThemeKt`, `TypographyKt`, `ShadowsKt` and `TextStylesKt` classes, including the functions that build tokens from the window class (`appTypography`) or the theme (`toColorScheme`, `appShadows`), are left out." Scenario, add: "**AND** it does not list `TypographyKt` or `AppThemeKt`".

### A2: The other three timing assertions in `EmailRuleTest` stay
**Severity:** minor · **Status:** applied
**Location:** tasks.md > 1.1; design.md §8
**Current text:** "remove the `measureTime` block, the duration assertion and the `println` from `specNearMatchThroughTheRuleIsTooLongFast`"
**Failure scenario:** `assertPatternRejectsFast` keeps a 100 ms wall-clock limit for three more tests, and they now also run under Kover and PIT. Their inputs are at most 500 characters, so a timeout is unlikely, but `import kotlin.time.measureTime` stays and a slow CI machine could still stop `coverageReport`. D8 covers only the one test, so this is a note, not a change of scope.
**Fix:** tasks.md 1.1, append: "The three `*RejectedFast` tests that use `assertPatternRejectsFast` keep their limit (D8 names only one test). If one of them fails a `coverageReport` or `mutationReport` run, stop and ask."

## Trust boundaries
- Build dependency `kotlin-metadata-jvm` from Maven Central: a compromised or swapped artifact could run code during `mutationReport`. It's pinned in the catalog and stays on PIT's classpath only, the same as the existing PIT artifacts. Not a new kind of risk.
- Excluding code from the reports: a security check could be hidden from coverage or mutation testing. The new rules drop inline bodies, nav helpers, composable-local code and two named members. None of them hold auth or session logic. `safeCall`'s inlined copies in feature data modules are still mutated in the caller methods. Not a gap.
- `EmailRuleTest` timing change: dropping the wall-clock check could hide a ReDoS regression. `EmailRule.check` returns `TooLong` before running the pattern, and the kept `TooLong` assertion fails if the pattern runs first (it would return `InvalidFormat`). The three pattern-level timing tests stay. Covered.

## Coverage
Read all artifacts in full (decisions, proposal, both delta specs, design, tasks), the two main specs, `openspec/config.yaml` and CLAUDE.md. Checked against `CodeExclusions`, `MutationExclusions`, `Coverage.kt`, `ComposeUiFilter.java`, `pitest-filters/build.gradle.kts`, `AppShell.kt`, `SessionGate.kt`, `HttpClientExt.kt`, `Result.kt`, `TextRules.kt`, `UiText.kt`, `HttpClientFactory.kt`, `InMemoryBoardRepository.kt`, the theme package, `EmailRuleTest.kt` and `EmailRule.kt`. Used the existing `mutations.xml` of `:core:data`, `:core:presentation`, `:composeApp` and `:feature:boards:data` to confirm where mutants sit. Shared spec `domain-user` was read for the email check order. Every requirement and scenario in both deltas was checked. No other active change exists.

Not read: the test plans in groups 5–10 against the production classes they name (`BoardViewModel`, `AnchoredPositionProvider` and others), the Kover HTML/XML report, `MutationConventionPlugin`/`MutationRootConventionPlugin` wiring, and the shared specs `text-length`, `domain-board` and `domain-label`.

## Applied changes
All eight findings applied as recommended (2026-11-26).

- `design.md`: §2 inline rule also drops classes enclosed by inline functions; nav rule limited to top-level functions by first JVM parameter; named-member globs `UiText*#asStringAsync*` and `HttpClientFactory#create$lambda*`; §4 test list extended; §5 `:feature:boards:data` row; metadata-read risk bullet; §8 note on the three `*RejectedFast` tests.
- `tasks.md`: 1.1, 3.2, 3.3, 3.5, 3.6, 3.7 and 7.2 updated to match. No checkbox changed.
- `proposal.md`: mutation exclusion wording for inline-enclosed classes and NavDestination.
- `specs/mutation-testing/spec.md`: inline, navigation and runtime-bound requirements and scenarios.
- `specs/test-coverage/spec.md`: theme package wording and an extra AND line in "Design tokens are excluded".
- `openspec validate improve-test-suite-quality --strict` passes.

Contradiction with `decisions.md` (not edited): D4 and D5 name only NavController/NavHostController helpers. The spec, proposal and design now also cover `NavDestination` (needed for `isAuthScreen`, which D4 lists through the spec scenario). Update D4/D5 if you want the decision record to match.

No non-standard artifacts in the change folder.
