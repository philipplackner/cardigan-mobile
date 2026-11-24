# Artifact review (light): mobile-pitest-mutation-testing

**Verdict:** Ready
**Reviewed:** 2026-11-23, schema `careful-planning`, repo kind `code`

| Pass | Blocker | Major | Minor |
|---|---|---|---|
| Testability | 0 | 1 | 3 |
| Security | 0 | 0 | 0 |
| Hidden assumptions | 0 | 4 | 1 |
| Design contract | 0 | 1 | 3 |

No blockers. All majors are applied. D4 (minor) stays open until the user decides on the commit override.

## Findings

### T1: Kotlin noise filters (D8) have no requirement
**Severity:** major · **Status:** applied
**Location:** specs/mutation-testing/spec.md > Requirement: Excluded code
**Current text:** "Generated code, Koin DI modules, debug-only fixtures, previews and androidMain `actual`s SHALL NOT be mutated."
**Failure scenario:** An implementation that leaves out `--avoidCallsTo`, `--excludedMethods` and `*$WhenMappings` passes every scenario, while reports fill with mutants in `Intrinsics.checkNotNull` calls and data class `equals`/`hashCode`. Only task 2.2 checks this, and tasks aren't specs.
**Fix:** Add to the spec:
> ### Requirement: Kotlin compiler noise is filtered
> Calls into `kotlin.jvm.internal` and `kotlin.ResultKt` SHALL NOT be mutated. Methods named `equals`, `hashCode`, `toString`, `copy`, `copy$default` and `component<N>`, and classes ending in `$WhenMappings`, SHALL NOT be mutated.
>
> #### Scenario: No mutants in compiler-generated code
> - **WHEN** a report is generated for `:feature:boards:domain`
> - **THEN** it has no mutant in a `kotlin.jvm.internal.Intrinsics` call, in a `$WhenMappings` class, or in an `equals`, `hashCode`, `toString`, `copy` or `component<N>` method

Catches: a build that forgets `--avoidCallsTo`.

### T2: "take as long as before" can't fail a test
**Severity:** minor · **Status:** applied
**Location:** specs/mutation-testing/spec.md > Requirement: Mutation testing stays out of the normal workflow
**Current text:** "`check`, `build`, the four gates and normal test runs SHALL behave and take as long as before."
**Failure scenario:** Timing varies per run, so no test can check it. A change that wires a PIT task into `check` is only caught by the scenario, not by this sentence.
**Fix:** "No PIT task SHALL be in the task graph of `check`, `build`, `testAndroidHostTest` or any of the four gates, and none of them SHALL resolve a PIT artifact."
Catches: `check` depending on `pitestAndroidHost`.

### T3: Threshold boundary and precision not defined
**Severity:** minor · **Status:** applied
**Location:** specs/mutation-testing/spec.md > Requirement: Optional minimum score
**Current text:** "When it is passed and the total mutation score is below it, the command SHALL fail"
**Failure scenario:** With score 79.6% and `minScore=80`, one implementation rounds to 80 and passes, another compares exactly and fails. The backend has the same property (D17), so the repos can drift.
**Fix:** Append: "The score is compared unrounded. A score equal to the threshold passes." Add a scenario: "**WHEN** the score is exactly the `mutation.minScore` value **THEN** the command succeeds."
Catches: a `<=` comparison, or comparing a rounded percentage.

### T4: Excluded-code scenario skips half the requirement
**Severity:** minor · **Status:** applied
**Location:** specs/mutation-testing/spec.md > Requirement: Excluded code > Scenario: Generated and wiring code
**Current text:** "it has no mutants in BuildKonfig, Compose resources (`Res`), `di` or `fixture` packages, or classes matching `*Debug*Overlay*`"
**Failure scenario:** Previews, `ComposableSingletons` and androidMain `actual`s (`*_androidKt`, `Android*`, `*Activity`) are in the requirement and D10 but not in the scenario, so a missing glob for them passes.
**Fix:** "it has no mutants in BuildKonfig, Compose resources (`Res`), `di` or `fixture` packages, `ComposableSingletons`, `*PreviewKt`/`*PreviewsKt` classes, or classes matching `*Debug*Overlay*`, `*_androidKt`, `*.Android*` or `*Activity`"
Catches: the `*_androidKt` pattern being dropped in translation to PIT globs.

### A1: Runs with no results are undefined
**Severity:** major · **Status:** applied
**Location:** specs/mutation-testing/spec.md > Requirement: Modules opt in with one plugin id > Scenario: Module without tests; design.md > Decision 2
**Current text:** "**THEN** the run succeeds and says the module has no host tests"
**Failure scenario:** With `-Pmutation.modules=:feature:labels:domain`, no module produces `mutations.xml`. The aggregator has no input, the score is 0/0, and with `minScore` the comparison divides by zero. Requirement 1 still demands an `index.html`. Also, PIT fails a module with "No mutations found" when every class is excluded, unless `--failWhenNoMutations=false` is set.
**Fix:** Add to the requirement: "When no selected module produces mutants, the command SHALL succeed, write no combined report, print that there were no mutants, and ignore `mutation.minScore`. A module whose classes are all excluded SHALL be treated like a module without host tests." Add `--failWhenNoMutations=false` to design.md decision 1.

### A2: Threads multiply across parallel module runs
**Severity:** major · **Status:** applied
**Location:** design.md > Decision 1 (`--threads`) and specs > Requirement: Thread count
**Current text:** "`--threads` from `mutation.threads`, default `Runtime.availableProcessors()` (D13)"
**Failure scenario:** `org.gradle.configuration-cache=true` makes Gradle run tasks of different projects in parallel. Twenty `pitestAndroidHost` tasks each start N minion JVMs on an N-core machine. CPU starvation causes timeouts, and timed-out mutants count as killed (design decision 2), so the score goes up because the machine is overloaded. Memory can also run out.
**Fix:** In design.md decision 1: "The module PIT tasks share one Gradle `BuildService` with `maxParallelUsages = 1`, so only one module runs PIT at a time. Each run uses `mutation.threads` threads." In the spec requirement: "Module runs SHALL NOT overlap, so the thread count is the total for the machine."

### A3: Up-to-date checks and the build cache aren't ruled out
**Severity:** major · **Status:** applied with edits
**Location:** design.md > Decision 1; specs > Requirement: Incremental analysis is opt-in > Scenario: Default run
**Current text:** "**THEN** both runs analyze every mutant"
**Failure scenario:** `org.gradle.caching=true` is on. An implementer who declares inputs and outputs on `pitestAndroidHost` (natural for configuration cache) gets an UP-TO-DATE or FROM-CACHE second run, which analyzes nothing. Another implementer declares no outputs and always reruns. The default-run scenario passes for one and fails for the other.
**Fix:** In design.md decision 1: "`pitestAndroidHost` and `mutationReport` are never up to date and never cached (`outputs.upToDateWhen { false }`, `doNotCacheIf`). Reuse of results only happens through `mutation.incremental`." Extend the scenario: "**THEN** both runs execute PIT and analyze every mutant, and no `history.bin` is read or written."

### A4: Property validation depends on how the task is named
**Severity:** major · **Status:** applied
**Location:** design.md > Decision 2
**Current text:** "at configuration time, only when `mutationReport` is requested, so invalid values fail before any test runs"
**Failure scenario:** The same design says module tasks can be run by name, and task 3.4 checks incremental on `:core:domain`. `./gradlew :core:domain:pitestAndroidHost -Pmutation.threads=abc` or `-Pmutation.incremental=yes` then isn't validated, so one implementer ignores the value and another fails. An abbreviation like `./gradlew mR` slips past the `startParameter.taskNames` check entirely, as `coverageReport` does.
**Fix:** "The properties are parsed when `mutationReport` or any `pitestAndroidHost` task is requested by full name, and module tasks read the same parsed values. `mutation.modules` applies only to `mutationReport`. An abbreviated task name that resolves to a mutation task fails with a message to use the full name, as the coverage tasks do."

### A5: Stale combined report after a red run
**Severity:** minor · **Status:** applied
**Location:** specs/mutation-testing/spec.md > Requirement: Failing tests stop the run
**Current text:** "SHALL NOT write or update the combined report"
**Failure scenario:** After a red run, the previous run's `build/reports/pitest/index.html` is still there. A developer opens it and reads old numbers as current.
**Fix:** "SHALL delete any existing combined report before running PIT, and SHALL NOT write a new one."

### D1: Summary reads a merged `mutations.xml` that doesn't exist
**Severity:** major · **Status:** applied
**Location:** design.md > Decision 2
**Current text:** "It writes `build/reports/pitest/index.html`, then prints the summary from the merged `mutations.xml`."
**Failure scenario:** `pitest-aggregator`'s `ReportAggregator` reads the per-module XML files and writes HTML only. No merged `mutations.xml` is produced, so the implementer either has to write one or invent another source, and two people will do it differently.
**Fix:** "…then prints the summary by summing the per-module `mutations.xml` files of the selected modules that ran in this invocation."

### D2: Method exclusions by name hit every class, not only data classes
**Severity:** minor · **Status:** applied
**Location:** design.md > Decision 1 vs decisions.md > D8
**Current text:** "`--excludedMethods equals,hashCode,toString,copy,copy$default,component*`"
**Failure scenario:** D8 says "exclude data class `equals`, `hashCode`…". PIT matches by method name only, so hand-written `equals`, `toString` or `copy` in normal classes are excluded too. Nobody would notice the gap, because the reports just look clean.
**Fix:** Add to design.md decision 1: "PIT can't tell data classes apart, so these names are excluded on all classes. Hand-written `equals`, `hashCode`, `toString` and `copy` are not mutated (accepted, D8)." If the user doesn't accept this, D8 needs revisiting. That's a decision for the user, not an edit here.

### D3: Proposal Impact misses the root plugin and its alias
**Severity:** minor · **Status:** applied
**Location:** proposal.md > Impact
**Current text:** "`gradle/libs.versions.toml`: PIT version and libraries, the `convention-mutation` plugin alias."
**Failure scenario:** Design decision 2 and task 1.1/3.1 add `convention.mutation.root` and a `convention-mutation-root` alias. The proposal doesn't mention them.
**Fix:** "`gradle/libs.versions.toml`: PIT version and libraries, the `convention-mutation` and `convention-mutation-root` plugin aliases." and "Root build: applies `convention.mutation.root` next to `convention.coverage`."

### D4: Commit rule override isn't a recorded decision
**Severity:** minor · **Status:** open
**Location:** tasks.md > Workflow follow-up
**Current text:** "Commit only after the user's OK is relayed through the orchestrator (overrides CLAUDE.md's automatic commits for this change only)."
**Failure scenario:** CLAUDE.md requires commits as soon as a chunk passes the gates. decisions.md doesn't record this override, so an implementer following CLAUDE.md commits anyway, or one following tasks.md holds work back without the user having decided it.
**Fix:** Ask the user to confirm the override and record it in decisions.md (by the user), or remove the line from tasks.md.

## Trust boundaries
- Gradle properties (`mutation.*`): malformed input; the spec validates all four properties before tests run, except for A4's direct module-task path. No security impact.
- PIT artifacts from Maven Central: same supply chain exposure as every other catalog dependency; pinned version, resolved only on demand. No new gap.
- History file under `build/`: local, regenerated by `clean`, read only with the opt-in flag. Not a realistic attack surface.
- Mutated code under test: host tests use fakes and `MockEngine`, no network or keystore access, so mutants can't reach outside the JVM.

## Coverage
Checked all 10 requirements and 20 scenarios in `specs/mutation-testing/spec.md`, plus proposal, design, tasks and decisions. Read `KotlinMultiplatform.kt`, `Coverage.kt`, `CoverageConventionPlugin.kt` (property parsing pattern), `settings.gradle.kts`, root `build.gradle.kts`, `gradle.properties`, and confirmed `normalizePassword`, `BoardListScreen` and the absence of tests in `:feature:labels:domain`. Not read: the PIT and pitest-aggregator source (D1 relies on knowing the aggregator writes HTML only, worth a quick check during task 3.2), the backend's matching change, and the main `test-coverage` spec beyond its requirement names. No shared-spec dependency.

## Applied changes
- `specs/mutation-testing/spec.md`: T1 (new requirement "Kotlin compiler noise is filtered"), T2, T3 (unrounded comparison, equal passes, new scenario), T4, A1 (no-mutants rule and scenario), A2 (module runs don't overlap, new scenario), A3, A5.
- A3 applied with edits: the caching rule went into its own requirement, "Gradle never reuses mutation results", because adding it to "Incremental analysis is opt-in" pushed that requirement over the 500-character limit of `openspec validate --strict`.
- `design.md`: A1 (`--failWhenNoMutations=false`, no-mutants path), A2 (shared `BuildService`, `maxParallelUsages = 1`), A3 (never up to date or cached), A4 (validation for module tasks and abbreviations), A5 (delete old combined report), D1 (sum per-module XML), D2 (name-based exclusion noted as accepted), T3.
- `proposal.md`: D3.
- `tasks.md`: verify steps for 2.1, 3.2, 3.3 and 3.4 carried over. No checkbox changed.
- Not applied: D4. Its fix means either editing `decisions.md`, which this review must not touch, or removing the line from `tasks.md`. Left for the user.
- D2 records name-based method exclusion as accepted under D8. If the user doesn't accept it, D8 needs revisiting.
- `openspec validate mobile-pitest-mutation-testing --strict` passes.

## Implementation notes (task 2.2)

Checked on the reports of all 20 modules after the first full run (20,469 mutants):
- No mutants in BuildKonfig, `Res`, `di`, `fixture`, `*Debug*Overlay*`, `*PreviewKt`/`*PreviewsKt`, `ComposableSingletons`, `Android*`, `*_androidKt`, `*Activity` or `$WhenMappings` classes.
- No mutants in `equals`, `hashCode`, `toString`, `copy`, `copy$default` or `component<N>` methods, and none in `kotlin.jvm.internal` or `kotlin.ResultKt` calls.
- `:feature:auth:data` contains only fixture code, so PIT finds no mutants and the module is skipped with a note.

The remaining-noise notes from this first run were superseded by the amendment below.

## Implementation notes (amendment, task 5.3)

The first run kept Compose UI code in (old D11). It had 20,469 mutants, 88% of them in `@Composable` methods, so the total score was about 5%. After the amendment (D8, D10, D11, D18–D20), a full `./gradlew mutationReport` gives:

| Module | Mutants | Score | Test strength |
|---|---|---|---|
| `:composeApp` | 36 | 22.2% | 88.9% |
| `:core:data` | 76 | 15.8% | 75.0% |
| `:core:designsystem` | 47 | 19.1% | 100.0% |
| `:core:domain` | 161 | 68.9% | 86.0% |
| `:core:presentation` | 7 | 42.9% | 100.0% |
| `:feature:auth:domain` | 74 | 97.3% | 100.0% |
| `:feature:auth:presentation` | 213 | 81.7% | 88.3% |
| `:feature:boards:data` | 71 | 63.4% | 83.3% |
| `:feature:boards:domain` | 36 | 75.0% | 90.0% |
| `:feature:boards:presentation` | 605 | 70.9% | 78.7% |
| `:feature:transfer:data` | 16 | 87.5% | 87.5% |
| `:feature:transfer:domain` | 40 | 85.0% | 91.9% |
| `:feature:transfer:presentation` | 95 | 75.8% | 81.8% |
| **Total** | **1,477** | **68.4%** | **83.8%** |

Checked against the first run's reports:
- No mutant is left in a `@Composable` method, in a lambda declared in one, or in a class enclosed by one.
- No mutant in `$$serializer`, `$$inlined$`, theme token, `*Route`, `*GraphKt`, `*NavigationKt` or `*NavHostKt` classes.
- Still mutated: `destinationFor` (`SessionGateKt`), `openBoardId` (`AppShellKt`), `WindowClassKt`, `WindowBreakpoints`, `BoardListReloader`, and 137 `invokeSuspend` mutants in `BoardViewModel`.
- The only killed mutants that left the report are 31 in `$$serializer` classes of `:feature:transfer:presentation`, which the tests reach through `SavedStateHandle`. They are compiler-generated, so dropping them is intended.
- The `Modifier` rule also drops `inlineFieldKeys` in `BoardColumnParts.kt` (7 mutants, none covered). It maps Enter and Escape to `BoardAction.InlineSubmit` and `InlineCancel`, so it is real logic that only UI tests reach. Moving the key mapping into a plain function would bring it back into the report.
- Remaining known effects: about 60 uncovered mutants in `:core:data` are lambda classes declared inside `suspend inline` functions (`safeCall`, `get`, `post`, ...), which no test can kill because tests run the inlined copies. Timeouts in inlined `MutableStateFlow.update {}` loops count as killed (D8).
