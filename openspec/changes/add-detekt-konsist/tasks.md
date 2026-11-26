# Tasks

## 1. Acceptance tests

- [x] 1.1 [acceptance-red] Write A01-A09 from tests.md as `NoMockingLibrariesAcceptanceTest` in `tooling/src/test/kotlin/com/plcoding/cardiganapp/tooling/detekt/` (area `:tooling`). Real detekt in-process via `detekt-core`, config from the system property `cardigan.detekt.config`.

## 2. Build setup

- [x] 2.1 [plain] Add to `gradle/libs.versions.toml`: versions `detekt = "2.0.0-alpha.6"` and `konsist = "1.0.0-beta.1"`; libraries `detekt-api`, `detekt-core`, `detekt-test`, `detekt-gradlePlugin`, `konsist`, `kotlin-test-junit5`; plugins `detekt` (`dev.detekt`), `kotlinJvm`, and convention ids `convention-jvm-library`, `convention-detekt`, `convention-static-analysis-root`. Add `alias(...) apply false` for `detekt` and `kotlinJvm` in the root `build.gradle.kts`. Verify with `./gradlew help`.
- [x] 2.2 [plain] First check the open risk from design.md: apply `dev.detekt` to `:core:domain` temporarily and list its tasks (`./gradlew :core:domain:tasks --all`). Confirm there is a type-resolved detekt task for the Android main, host test and device test compilations, and note the exact task names for 2.4. If there is none, stop and ask the user (D13 can't be met as planned). Revert the temporary change. Verify by writing the task names into task 2.5 of this file.
- [x] 2.3 [plain] Add `JvmLibraryConventionPlugin` (`convention.jvm.library`) to `build-logic/convention`: Kotlin/JVM, same toolchain as the other modules, JUnit Platform for `Test` tasks. Register it in `build-logic/convention/build.gradle.kts`. Verify with `./gradlew -p build-logic :convention:validatePlugins`.
- [x] 2.4 [plain] Create the `:tooling` module: `include(":tooling")` in `settings.gradle.kts`, `tooling/build.gradle.kts` applying `convention.jvm.library` (not `kover`, not `mutation`), dependencies as in design.md (`compileOnly` detekt-api; test: detekt-api, detekt-core, detekt-test, kotlin-test-junit5, AssertK; `testCompileOnly` konsist plus a separate `konsistTestRuntimeClasspath` with konsist and without detekt-core/detekt-test). The `test` task sets `cardigan.detekt.config` to the absolute path of `config/detekt/detekt.yml`, declares that file as an input, and excludes `com/plcoding/cardiganapp/tooling/konsist/**`. Register a `konsistTest` `Test` task with the same test classes and the `konsistTestRuntimeClasspath` runtime classpath that includes only that package and uses `doNotTrackState(...)`. Verify with `./gradlew :tooling:compileKotlin :tooling:tasks --all` showing `konsistTest`.
- [x] 2.5 [plain] Add `DetektConventionPlugin` (`convention.detekt`) as design.md > Convention plugins describes: config file, `buildUponDefaultConfig`, no baseline, `detektPlugins(project(":tooling"))`, HTML + SARIF only, `**/build/**` excluded, detekt tasks removed from `check`, and the `detekt` task wiring per module kind (KMP: type-resolved Android main, host test and device test compilation tasks from 2.2 (`detektMainAndroid`, `detektHostTestAndroid`, `detektDeviceTestAndroid`) plus light-mode iOS source sets; `:androidApp`: debug variant tasks; JVM: `detektMain` + `detektTest`). Verify with `./gradlew -p build-logic :convention:validatePlugins`.
- [x] 2.6 [plain] Add `config/detekt/detekt.yml` with overrides only: the ktlint overlap rules turned off, each with a one-line comment naming the duplicated ktlint rule (final list from comparing detekt 2.0.0-alpha.6's default config with `ktlint_official`), and the `cardigan` rule set with `NoMockingLibraries: active: true` and the test source set `includes` from design.md. No ktlint wrapper. Verify the file parses by running `./gradlew :core:domain:detekt` after 2.7 (findings are allowed; a config error is not).
- [x] 2.7 [plain] Apply `convention.detekt` in every KMP module, `:androidApp` and `:tooling`. Verify with `./gradlew :core:domain:detekt :androidApp:detekt :tooling:detekt --dry-run` listing the expected type-resolved and light tasks.
- [x] 2.8 [plain] Wire detekt into build-logic: apply `alias(libs.plugins.detekt)` in `build-logic/convention/build.gradle.kts` only (`:pitest-filters` is Java-only and not analyzed) with `../config/detekt/detekt.yml`, `buildUponDefaultConfig`, HTML + SARIF, no `detektPlugins`; `detekt` depends on `detektMain` and `detektTest`; detekt tasks removed from `check`. Verify with `./gradlew -p build-logic detekt` running (findings are allowed).
- [x] 2.9 [plain] Add `StaticAnalysisRootConventionPlugin` (`convention.static.analysis.root`) and apply it in the root `build.gradle.kts`: root `detekt` (build-logic's `:convention:detekt` + every opted-in subproject's `detekt`, wired in `projectsEvaluated`, + SARIF merge of those tasks' reports into `build/reports/detekt/merged.sarif`), `konsist` (→ `:tooling:konsistTest`), `staticAnalysis` (→ both; when requested, detekt tasks and `konsistTest` set `ignoreFailures` and the finalizer `staticAnalysisVerify` fails on any SARIF result or failed Konsist test). Verify with `./gradlew staticAnalysis --dry-run` listing all module detekt tasks, build-logic's detekt tasks and `:tooling:konsistTest`.

## 3. Sample custom rule

- [x] 3.1 [scaffold] Add `NoMockingLibraries(config: Config) : Rule` and `CardiganRuleSetProvider : RuleSetProvider` (`ruleSetId = "cardigan"`) in `tooling/src/main/kotlin/com/plcoding/cardiganapp/tooling/detekt/`, with stub bodies per project.md, and the service file `tooling/src/main/resources/META-INF/services/dev.detekt.api.RuleSetProvider`. Verify with `./gradlew :tooling:compileKotlin`.
- [x] 3.2 [red] Write C01-C06 from tests.md as `NoMockingLibrariesTest` in `tooling/src/test/kotlin/com/plcoding/cardiganapp/tooling/detekt/` (area `:tooling`).
- [x] 3.3 [green] Implement `NoMockingLibraries` and `CardiganRuleSetProvider` as design.md > Public surface describes: import matching (case-sensitive, prefix-safe, star and aliased imports), the exact message, inactive by default. Verify with the tests of 3.2 passing (`./gradlew :tooling:test`).
- [x] 3.4 [acceptance-green] Make the acceptance tests of 1.1 pass. Verify with `./gradlew :tooling:test` passing and `./gradlew konsist` not running them.

## 4. Konsist check

- [x] 4.1 [plain] Written by the test-writer, not the implementer (D30: `tooling/src/test` is a test path). Add `DomainLayerKonsistTest` in `tooling/src/test/kotlin/com/plcoding/cardiganapp/tooling/konsist/` as design.md > `:tooling` module and Konsist describes: files in `*.domain` packages whose path relative to the project root starts with `core/domain/src/` or matches `feature/<name>/domain/src/` import nothing from `..data..`, `..presentation..`, `..designsystem..`, `io.ktor..`, `org.koin..`, `androidx..`, `org.jetbrains.compose..`. Verify with `./gradlew konsist` passing on the current code, then, without `--rerun-tasks`, failing with file and import named after temporarily adding `import io.ktor.client.HttpClient` to a file in `:feature:boards:domain` (revert afterwards).

## 5. Documentation

- [ ] 5.1 [plain] Update CLAUDE.md: tech stack row "Lint / format" (detekt 2.0 alpha with `:tooling` custom rules, Konsist; on demand; ktlint row left to `add-ktlint`), Commands table rows for `detekt`, `<module>:detekt`, `konsist`, `staticAnalysis` (runs both, continues after a failure) and `:tooling:test`, each "Not a gate". Add `:tooling` to the Architecture module list. Verify by reading the diff against decisions.md D9-D10, D21-D22.
- [ ] 5.2 [plain] Update `.claude/tdd/project.md`: area `tooling/...` → `:tooling` (`./gradlew :tooling:test`), acceptance level for `:tooling` (D24: JVM tests running real detekt in-process with the repo config, through rule set id and rule id only), full-check note "when a change touches `tooling/`, also run `./gradlew :tooling:test`". Verify by reading the diff.

## 6. Manual checks

- [ ] 6.1 [plain] Run `./gradlew detekt`. It may fail on existing findings (D6); confirm that every opted-in module and build-logic were analyzed, iOS source sets ran in light mode, no file under a `build/` directory was reported, each module has HTML and SARIF reports, and `build/reports/detekt/merged.sarif` exists. Verify by listing the report files.
- [ ] 6.2 [plain] Put `import io.mockk.every` in a temporary file in `feature/boards/data/src/commonTest/` and in `feature/boards/data/src/commonMain/`. Run `./gradlew :feature:boards:data:detekt`: only the test file is reported as `cardigan:NoMockingLibraries` with the exact message. Remove both files. Verify by the command output.
- [ ] 6.3 [plain] Run `./gradlew staticAnalysis` twice in a row while detekt has findings: confirm both times that `:tooling:konsistTest` still runs and the build fails at the end. Run the gate commands, `./gradlew check` and `./gradlew -p build-logic check` with `--dry-run` and confirm no detekt task or `konsistTest` is scheduled, and `coverageReport --dry-run` / `mutationReport --dry-run` don't include `:tooling`. Verify by the command output.

## 7. Integration

- [ ] 7.1 [plain] Run `openspec validate add-detekt-konsist --strict`. Verify it passes.
- [ ] 7.2 [plain] Run the full check from project.md, plus `./gradlew -p build-logic :convention:test` (build-logic changed) and `./gradlew :tooling:test`. Verify all pass.

## Workflow follow-up

- Whichever of `add-detekt-konsist` and `add-ktlint` lands second applies the ktlint convention plugin to `:tooling` and re-checks the detekt overlap list against the final ktlint settings.
- Archive the change after review.
