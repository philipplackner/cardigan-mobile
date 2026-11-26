# Artifact review (light): add-detekt-konsist

**Verdict:** Ready
**Reviewed:** 2026-11-26, schema `careful-planning-tdd`, repo kind `code`

The review was requested for `mobile-detekt-konsist`. No change has that name; `add-detekt-konsist` is the matching change.

| Pass | Blocker | Major | Minor |
|---|---|---|---|
| Testability | 0 | 0 | 1 |
| Security | 0 | 0 | 0 |
| Hidden assumptions | 0 | 3 | 2 |
| Design contract | 0 | 3 | 1 |

## Findings

### D1: detekt is not removed from `check`
**Severity:** major · **Status:** applied
**Location:** design.md > Decisions > Convention plugins > `convention.detekt`; specs/static-analysis/spec.md > Static analysis stays on demand
**Current text:** "detekt and Konsist SHALL run only when one of their tasks is requested. The gate commands, `testAndroidHostTest`, `check`, `coverageReport` and `mutationReport` SHALL NOT run them"
**Failure scenario:** The detekt Gradle plugin makes `check` depend on the detekt tasks by default. The design never undoes that, and task 6.3 only dry-runs the gates, coverage and mutation, so `./gradlew check` runs detekt (and fails on existing findings, D6) without anyone noticing. `add-ktlint` handles the same problem explicitly (its design decision 2).
**Fix:** Add to design.md > `convention.detekt`: "- Removes every detekt task from `check`'s dependencies after `dev.detekt` is applied (same approach as `add-ktlint` decision 2; use a supported switch if detekt 2.0 offers one). build-logic's build files do the same." Add to the spec requirement a scenario: "#### Scenario: check run / - **WHEN** a developer runs `./gradlew check` / - **THEN** no detekt task and no Konsist check runs". Extend task 6.3: "Run `./gradlew check --dry-run` and `./gradlew -p build-logic check --dry-run` and confirm no detekt task is scheduled."

### D2: `androidDeviceTest` source sets are never analyzed
**Severity:** major · **Status:** applied
**Location:** design.md > Convention plugins > "Wires the module's plain `detekt` task" (KMP modules); specs/static-analysis/spec.md > Production and test code are analyzed
**Current text:** "It depends on the type-resolved detekt tasks of the Android compilations (main and host test), which cover `commonMain`, `androidMain`, `commonTest` and `androidHostTest`."
**Failure scenario:** `feature/boards/presentation/src/androidDeviceTest` and `feature/transfer/presentation/src/androidDeviceTest` exist. The KMP wiring covers the main and host-test compilations plus iOS only, so device tests are never analyzed. That breaks D11 ("production and test source sets of every opted-in module"), and the `**/androidDeviceTest/**` entry in the `NoMockingLibraries` `includes` never takes effect. Device tests are where Mockito is most likely to appear.
**Fix:** In design.md replace the sentence with: "It depends on the type-resolved detekt tasks of the Android compilations (main, host test and device test), which cover `commonMain`, `androidMain`, `commonTest`, `androidHostTest` and `androidDeviceTest`." Add `androidDeviceTest` to the spec scenario "Test code" (`WHEN a file in a module's commonTest, androidHostTest, androidDeviceTest, iosTest or src/test has a finding`). In task 2.2 also confirm the task name for the device-test compilation.

### D3: root `detekt` and `staticAnalysis` don't reach subproject detekt tasks through `dependsOn`
**Severity:** major · **Status:** applied
**Location:** design.md > Convention plugins > `convention.static.analysis.root`
**Current text:** "Running `./gradlew detekt` from the root then executes the root task plus every subproject's `detekt` task through Gradle's task-name matching." … "Registers `staticAnalysis`, depending on `detekt` and `konsist`."
**Failure scenario:** Task-name matching only applies to names typed on the command line. `dependsOn("detekt")` inside the root project resolves to the root task alone, so `./gradlew staticAnalysis` runs build-logic's detekt and Konsist but no module's detekt. In addition, the SARIF merge has no defined inputs from the subprojects.
**Fix:** Replace the first sentence with: "In `gradle.projectsEvaluated`, the root `detekt` task depends on the `detekt` task of every subproject that has `convention.detekt` applied (same discovery as `coverageReport` and `mutationReport`), and the merge task takes those tasks' SARIF report files as inputs. `./gradlew detekt` and `staticAnalysis` therefore run the same set of tasks." Task 2.9's dry-run check already catches it once the design is fixed.

### A1: `konsistTest` can be restored from the build cache and skip the check
**Severity:** major · **Status:** applied
**Location:** design.md > `:tooling` module and Konsist
**Current text:** "sets `outputs.upToDateWhen { false }` so it re-reads the source tree every run, since Konsist reads files that are not task inputs."
**Failure scenario:** `org.gradle.caching=true` is set in `gradle.properties`. `upToDateWhen { false }` stops UP-TO-DATE but not FROM-CACHE. The task's inputs (classes, classpath) don't change when a domain file gains `import io.ktor...`, so Gradle restores the previous passing result and `./gradlew konsist` passes on a violation.
**Fix:** "uses `doNotTrackState("Konsist reads source files that are not task inputs.")`, like `mutationReport`, so it never comes UP-TO-DATE or FROM-CACHE." Add to task 4.1's verify step: run `./gradlew konsist` once clean, then add the temporary Ktor import and run again without `--rerun-tasks`.

### A2: setting `continueOnFailure` at configuration time is not kept by the configuration cache
**Severity:** major · **Status:** applied
**Location:** design.md > Convention plugins > `convention.static.analysis.root` (`staticAnalysis`)
**Current text:** "When `staticAnalysis` is among the requested tasks, the plugin sets `gradle.startParameter.isContinueOnFailure = true` so the Konsist checks run after a detekt failure"
**Failure scenario:** `org.gradle.configuration-cache=true` is set. On a cache hit the plugin's configuration code does not run, and `--continue` is not an option that is changed from inside the build, so the second `./gradlew staticAnalysis` stops at the first detekt failure and never runs Konsist (breaks D22). It may also not work on the first run.
**Fix:** Make the design's fallback the plan: "Every detekt task registered by the convention plugins and build-logic sets `ignoreFailures = true` only when `staticAnalysis` is requested; a final `staticAnalysisVerify` task, finalizing `staticAnalysis`, fails when any SARIF report has a result or `konsistTest` failed. Without `staticAnalysis`, detekt tasks keep failing on findings (D5)." Add to task 6.3: run `./gradlew staticAnalysis` twice in a row with detekt findings present and confirm Konsist runs both times.

### A3: detekt-core and Konsist share one test runtime classpath with different Kotlin compiler versions
**Severity:** major · **Status:** applied
**Location:** design.md > `:tooling` module and Konsist ("Dependencies" and "A second `Test` task `konsistTest` uses the same classes and classpath")
**Current text:** "`testImplementation` of `detekt-test`, `detekt-api`, `detekt-core` (D25), `kotlin-test-junit5`, AssertK and `konsist`."
**Failure scenario:** decisions.md Facts: Konsist 1.0.0-beta.1 bundles `kotlin-compiler-embeddable` 2.4.21, and detekt alpha.6 is built on Kotlin 2.4.0. Gradle resolves one version (2.4.21) for both, so the acceptance tests run detekt-core on a compiler it wasn't built against. The compiler has no binary-compatibility promise, so A01–A09 may fail or behave differently from the real Gradle run, which uses detekt's own isolated classpath.
**Fix:** Add to design.md: "`konsist` is `testCompileOnly` plus a dependency of a separate `konsistTestRuntimeClasspath` configuration used only by `konsistTest`. `detekt-core` and `detekt-test` are on the `test` task's runtime classpath only. The two tools never share a runtime classpath. If the acceptance tests still fail on the resolved compiler version, stop and raise it." Update task 2.4 to match.

### T1: the ktlint-overlap scenario can't fail
**Severity:** minor · **Status:** applied
**Location:** specs/static-analysis/spec.md > Default rules with project overrides > Formatting is left to ktlint
**Current text:** "**WHEN** a file breaks only a formatting rule that ktlint covers / **THEN** `./gradlew detekt` does not report it"
**Failure scenario:** "A formatting rule that ktlint covers" is defined by the list made during implementation, so any list passes, including one that leaves `MaxLineLength` active. A wrong implementation that keeps `style>MaxLineLength` on would not be caught.
**Fix:** "**WHEN** a file's only problems are a 130-character line and trailing whitespace / **THEN** `./gradlew detekt` reports no `MaxLineLength` and no `TrailingWhitespace` finding"

### A4: Konsist file filter "under `core/domain/`" can be read two ways
**Severity:** minor · **Status:** applied
**Location:** design.md > `:tooling` module and Konsist > `DomainLayerKonsistTest`
**Current text:** "filtered to files under `core/domain/` and `feature/*/domain/` whose package contains `.domain`"
**Failure scenario:** A substring match on the absolute path also matches a source directory like `core/data/src/commonTest/kotlin/com/plcoding/cardiganapp/core/domain/` (package dirs mirror `core/domain`), so a fake in a data module's test set would be held to the domain rules. A prefix match relative to the project root does not.
**Fix:** "filtered to files whose path relative to the project root starts with `core/domain/src/` or matches `feature/<name>/domain/src/`, all source sets"

### A5: the acceptance tests don't declare the config file as a `test` input
**Severity:** minor · **Status:** applied
**Location:** design.md > `:tooling` module and Konsist ("The `test` task gets the absolute path of `config/detekt/detekt.yml` as the system property")
**Current text:** "The `test` task gets the absolute path of `config/detekt/detekt.yml` as the system property `cardigan.detekt.config`."
**Failure scenario:** With the build cache on, editing `detekt.yml` (for example dropping a test path from `includes`) doesn't change any `test` input, so `:tooling:test` comes FROM-CACHE and A01/A03 never re-run against the new config.
**Fix:** "The `test` task gets the absolute path of `config/detekt/detekt.yml` as the system property `cardigan.detekt.config` and declares the file with `inputs.file(...).withPathSensitivity(PathSensitivity.NONE)`."

### D4: `:pitest-filters` has no Kotlin, so `detektMain`/`detektTest` don't apply
**Severity:** minor · **Status:** applied
**Location:** design.md > build-logic; tasks.md > 2.8
**Current text:** "`build-logic/convention/build.gradle.kts` and `build-logic/pitest-filters/build.gradle.kts` apply `alias(libs.plugins.detekt)` … Their `detekt` tasks depend on the type-resolved `detektMain` and `detektTest`."
**Failure scenario:** `pitest-filters` is a `java-library` module with only `.java` sources. detekt analyzes Kotlin only, and its per-source-set tasks need the Kotlin plugin, so task 2.8 either fails to configure or analyzes nothing. The root task's dependency on `:pitest-filters:detekt` then fails.
**Fix:** "Only `build-logic/convention/build.gradle.kts` applies `alias(libs.plugins.detekt)` … `:pitest-filters` has Java sources only and is not analyzed." Remove `task(":pitest-filters:detekt")` from the root plugin description and `pitest-filters` from task 2.8.

## Trust boundaries

- Third-party build dependencies (detekt alpha, Konsist beta, Gradle plugins from Maven Central and the Plugin Portal): a compromised artifact would run in the build. Versions are pinned in the catalog and the user approved them. The repo has no dependency verification, but this change doesn't make that worse. No finding.
- detekt plugin loading: only `project(":tooling")` is added to `detektPlugins`. No external rule sets. No finding.
- Konsist and detekt read local source files and write local reports. No network, no user input, no secrets. No finding.

## Coverage

Checked all 17 requirements and 27 scenarios in `specs/static-analysis/spec.md`, plus decisions.md (D1–D30), proposal.md, design.md, tests.md (A01–A09, C01–C06) and tasks.md. Read `openspec/config.yaml`, CLAUDE.md, `.claude/tdd/project.md` and `tdd.conf`, `settings.gradle.kts`, the root and build-logic build files, `MutationRootConventionPlugin` and `CoverageConventionPlugin` (opt-in discovery pattern), `gradle.properties`, domain module imports, and the `add-ktlint` proposal and design for overlap. No main spec is touched. No shared spec applies (proposal and decisions say so).

Not checked in a light review: detekt 2.0.0-alpha.6's actual task names, path-filter semantics for `includes`, and how it loads a plugin's default config; Konsist's project-root detection. The design already defers the task names to task 2.2.

## Applied changes
All ten findings applied on 2026-11-26. `openspec validate add-detekt-konsist --strict` passes.

- `design.md`: detekt tasks removed from `check` in the convention plugin and in build-logic (D1); KMP wiring adds the Android device-test compilation (D2); the root `detekt` depends on every opted-in subproject's `detekt` in `projectsEvaluated`, and the SARIF merge takes their reports as inputs (D3); `:pitest-filters` is no longer analyzed (D4); `konsistTest` uses `doNotTrackState` (A1); `staticAnalysis` uses `ignoreFailures` plus a `staticAnalysisVerify` finalizer instead of `isContinueOnFailure`, and the risk line was updated to match (A2); Konsist is `testCompileOnly` with its own `konsistTestRuntimeClasspath` (A3); the Konsist file filter uses project-relative path prefixes (A4); the `test` task declares `detekt.yml` as an input (A5).
- `specs/static-analysis/spec.md`: `androidDeviceTest` in the "Test code" scenario (D2), a concrete "Formatting is left to ktlint" scenario (T1), a new "check run" scenario (D1).
- `tasks.md`: 2.2, 2.4, 2.5, 2.8, 2.9, 4.1 and 6.3 updated to match. No checkbox changed.
- `proposal.md`: device tests are type-resolved; `:pitest-filters` isn't analyzed.
- `tests.md`: unchanged. None of its tests depend on the changed wording.
- `decisions.md`: not edited, and no contradiction. D14 lists only host tests among the type-resolved Android sets, but D11 requires every test source set, so adding device tests follows D11. Consider adding device tests to D14's wording when you next touch it.
- No accepted risks.
