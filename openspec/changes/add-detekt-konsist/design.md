# Design

## Context

See proposal.md for the motivation and decisions.md for every settled choice (D1–D23). Relevant current state:

- All Kotlin modules except `:androidApp` are KMP libraries on AGP 9's `com.android.kotlin.multiplatform.library` with Android and two iOS targets. Convention plugins live in the `build-logic` included build (`:convention`, plus the plain Java module `:pitest-filters`, which detekt can't analyze).
- The main build has no plain JVM module. On-demand tools (`coverageReport`, `mutationReport`) use a per-module opt-in plugin plus a root plugin applied in the root `build.gradle.kts`.
- detekt 2.0 on KMP gives type resolution only to JVM and Android compilations. Its plain `detekt` task and the `detekt<SourceSet>SourceSet` tasks run without type resolution.

## Goals / Non-Goals

**Goals:**
- `./gradlew detekt`, `./gradlew konsist` and `./gradlew staticAnalysis` work from the root and stay identical in name and behavior to the backend.
- `./gradlew <module>:detekt` analyzes exactly that module.
- A home for future custom rules and Konsist checks (`:tooling`) that is cheap to extend.

**Non-Goals:**
- Making any finding go away. Existing code may fail `detekt` after this change (D6).
- Tuning rules for Compose or KMP beyond the ktlint overlap list (D7, D8).
- Hooking anything into `check`, the gates, CI or git hooks (D4).

## Public surface

The sample rule is the only test-first part (Workflow line). Its tests are written against this surface:

- **Module** `:tooling`, Kotlin/JVM, package root `com.plcoding.cardiganapp.tooling`.
- **`com.plcoding.cardiganapp.tooling.detekt.NoMockingLibraries`**: `class NoMockingLibraries(config: dev.detekt.api.Config) : dev.detekt.api.Rule`. Rule id `NoMockingLibraries`. It visits import directives. For each import whose imported FQ name is `org.mockito` or `io.mockk`, or starts with `org.mockito.` or `io.mockk.` (case-sensitive, D28), it reports one finding at that import directive. Star imports (`import io.mockk.*`) and aliased imports (`import org.mockito.Mockito.mock as m`) count. Everything else is not reported: other imports, fully qualified references in code, prefix-only matches such as `io.mockkfake.X` or `org.mockitox.X`, and different letter case such as `Org.Mockito.X`.
  - Message, exact (D26, D27): `Import of '<path>' is not allowed. Use fakes instead of mocking libraries.` `<path>` is the import path as written without the alias, with `.*` for star imports: `io.mockk.every`, `io.mockk.*`, `org.mockito.Mockito.mock`.
  - Inactive by default (D29): the rule's default config is `active: false`, so it reports nothing unless a config activates it. No options besides detekt's standard `active`, `excludes` and `includes`. Standard detekt suppression (`@Suppress("NoMockingLibraries")`, `@file:Suppress(...)`) applies.
- **`com.plcoding.cardiganapp.tooling.detekt.CardiganRuleSetProvider`**: `class CardiganRuleSetProvider : dev.detekt.api.RuleSetProvider`. `ruleSetId` is `cardigan`. `instance()` returns a rule set containing `NoMockingLibraries`. Registered in `src/main/resources/META-INF/services/dev.detekt.api.RuleSetProvider`.
- **Test levels** (D24, D25), all JVM tests in `tooling/src/test/kotlin/com/plcoding/cardiganapp/tooling/detekt/`, with `kotlin-test` on JUnit 5 and AssertK:
  - Component: instantiate `NoMockingLibraries` with a config that activates it and call `lint` from `dev.detekt:detekt-test` on a code string. No type resolution needed; the rule only reads import text.
  - Acceptance: run real detekt in-process through `dev.detekt:detekt-core` (test-only dependency) with the repo's `config/detekt/detekt.yml` (absolute path passed by `tooling/build.gradle.kts` as the system property `cardigan.detekt.config`) and `CardiganRuleSetProvider` on the plugin path, over Kotlin files written to a temp directory laid out like source sets (for example `<tmp>/x/src/commonMain/kotlin/...` and `<tmp>/x/src/commonTest/kotlin/...`). Acceptance tests use only detekt's public API, the rule set id `cardigan`, the rule id `NoMockingLibraries` and the config file, never the rule class. They cover the test-sources-only scope from `includes` (D18) and suppression.

The "test source sets only" scope (D18) is not part of the rule. It comes from `includes` in `detekt.yml` (see D-config below) and is covered by the acceptance tests.

## Decisions

### Convention plugins

- **`com.plcoding.cardiganapp.convention.jvm.library`** (`JvmLibraryConventionPlugin`): applies `org.jetbrains.kotlin.jvm`, sets the JVM toolchain to match the other modules, uses JUnit Platform for `Test` tasks. Used only by `:tooling` for now. Alternative: configure `:tooling` inline in its build file; rejected because CLAUDE.md and `kmp-module-structure` keep build config in convention plugins.
- **`com.plcoding.cardiganapp.convention.detekt`** (`DetektConventionPlugin`), applied by each opted-in module (D12):
  - Applies `dev.detekt`, sets `config.from(rootProject.file("config/detekt/detekt.yml"))`, `buildUponDefaultConfig = true`, `parallel = true`, no baseline.
  - Adds `detektPlugins(project(":tooling"))` so every opted-in module, `:tooling` included, loads the `cardigan` rule set.
  - Reports: HTML and SARIF on every detekt task; all others off (D16).
  - Removes every detekt task from `check`'s dependencies after `dev.detekt` is applied (same approach as `add-ktlint` decision 2; use a supported switch if detekt 2.0 offers one), so `check` never runs detekt (D4).
  - Source exclusion: `exclude("**/build/**")` on every detekt task, so generated compose resources, BuildKonfig and OpenAPI DTOs are never analyzed (D11).
  - Wires the module's plain `detekt` task to cover the right code (D13, D14):
    - KMP modules: the plain `detekt` task is limited to the iOS source sets (`iosMain`, `iosArm64Main`, `iosSimulatorArm64Main`, `iosTest` and the per-target test sets), which run in light mode. It depends on the type-resolved detekt tasks of the Android compilations (main, host test and device test), which cover `commonMain`, `androidMain`, `commonTest`, `androidHostTest` and `androidDeviceTest`.
    - `:androidApp`: `detekt` depends on the type-resolved tasks for the debug variant (main and unit test). Its own plain sources are cleared so files are not analyzed twice.
    - `:tooling`: `detekt` depends on `detektMain` and `detektTest` (type-resolved), plain sources cleared.
  - The exact names of detekt 2.0's per-compilation tasks for the AGP 9 KMP library plugin are read from `./gradlew :core:domain:tasks --all` during implementation, not guessed here. If the Android compilation of a KMP library gets no type-resolved task at all, stop and raise it, because D13 can't be met as planned.
- **`com.plcoding.cardiganapp.convention.static.analysis.root`** (`StaticAnalysisRootConventionPlugin`), applied in the root `build.gradle.kts` next to `coverage` and `mutation.root`:
  - Registers a root `detekt` task (group `verification`) that depends on build-logic's detekt task through `gradle.includedBuild("build-logic").task(":convention:detekt")`, and finalizes with a `ReportMergeTask` that merges the SARIF reports into `build/reports/detekt/merged.sarif`. In `gradle.projectsEvaluated`, the root `detekt` task also depends on the `detekt` task of every subproject that has `convention.detekt` applied (same discovery as `coverageReport` and `mutationReport`), and the merge task takes those tasks' SARIF report files as inputs. Gradle's task-name matching only applies to names typed on the command line, so this explicit wiring is what makes `./gradlew detekt` and `staticAnalysis` run the same set of tasks. `./gradlew :feature:boards:data:detekt` runs only that module (D10).
  - Registers `konsist`, depending on `:tooling:konsistTest`.
  - Registers `staticAnalysis`, depending on `detekt` and `konsist`. When `staticAnalysis` is requested, every detekt task registered by the convention plugins and by build-logic sets `ignoreFailures = true`, and `konsistTest` sets `ignoreFailures = true`. A final task `staticAnalysisVerify`, finalizing `staticAnalysis`, fails when any SARIF report has a result or `konsistTest` had a failed test, naming which tool failed. So the Konsist checks run after a detekt failure and the build fails at the end (D22). Without `staticAnalysis`, detekt tasks keep failing on findings (D5) and `konsist` keeps failing on a failed check. Rejected: setting `gradle.startParameter.isContinueOnFailure` at configuration time, because `org.gradle.configuration-cache=true` skips configuration on a cache hit and the setting is lost.

### build-logic

build-logic can't apply the main build's convention plugins to itself and can't load `:tooling` (D15). Its `settings.gradle.kts` already reads the shared version catalog. Only `build-logic/convention/build.gradle.kts` applies `alias(libs.plugins.detekt)` directly with the same config file (`../config/detekt/detekt.yml` relative to build-logic), `buildUponDefaultConfig = true`, HTML + SARIF, and no `detektPlugins`. Its `detekt` task depends on the type-resolved `detektMain` and `detektTest`, and the detekt tasks are removed from `check` as in the convention plugin. `:pitest-filters` has Java sources only and is not analyzed, since detekt analyzes Kotlin only.

### D-config: `config/detekt/detekt.yml`

Holds only overrides of detekt's default config (D7):

- **ktlint overlaps off** (D8): the default-active detekt rules whose subject ktlint's `ktlint_official` style also checks. Candidates: `style>MaxLineLength`, `style>NewLineAtEndOfFile`, `style>TrailingWhitespace`, `style>NoTabs`, `style>WildcardImport`, `naming>FunctionNaming`, `naming>ClassNaming`, `naming>PackageNaming`, `naming>EnumNaming`, plus any other default-active rule found to duplicate a ktlint_official rule. The final list is made during implementation by comparing detekt 2.0.0-alpha.6's default config with the ktlint rules; each entry gets a one-line comment naming the ktlint rule it duplicates.
- **`cardigan` rule set**: `NoMockingLibraries: active: true` with `includes` set to test source set paths: `**/commonTest/**`, `**/androidHostTest/**`, `**/androidDeviceTest/**`, `**/iosTest/**`, `**/iosArm64Test/**`, `**/iosSimulatorArm64Test/**`, `**/src/test/**` (D18).
- No ktlint wrapper rule set is added to `detektPlugins` (D8).

Mobile and backend keep the same overrides. Path patterns for test source sets are the only expected difference.

### `:tooling` module and Konsist

- `settings.gradle.kts` gets `include(":tooling")`. `tooling/build.gradle.kts` applies `convention.jvm.library` and `convention.detekt`. It does not apply `kover` or `mutation` (D23).
- Dependencies: `compileOnly(libs.detekt.api)` for the rules (detekt supplies it at runtime), `testImplementation` of `detekt-test`, `detekt-api`, `detekt-core` (D25), `kotlin-test-junit5` and AssertK, and `testCompileOnly(konsist)`. Konsist is also a dependency of a separate `konsistTestRuntimeClasspath` configuration (extending the JUnit and AssertK test dependencies, without detekt-core and detekt-test) that only `konsistTest` uses. detekt-core and detekt-test are on the `test` task's runtime classpath only. The two tools never share a runtime classpath, because Konsist bundles `kotlin-compiler-embeddable` 2.4.21 and detekt alpha.6 is built on Kotlin 2.4.0. If the acceptance tests still fail on the resolved compiler version, stop and raise it. The `test` task gets the absolute path of `config/detekt/detekt.yml` as the system property `cardigan.detekt.config` and declares the file with `inputs.file(...).withPathSensitivity(PathSensitivity.NONE)`, so a config change re-runs the tests instead of restoring them from the build cache. Tests that need it fail with a message naming the property when it is missing.
- The Konsist check is written by the test-writer agent as a `[plain]` task, because `tooling/src/test` is a test path under the TDD path guard (D30).
- Test packages split the two kinds of tests: rule tests in `com.plcoding.cardiganapp.tooling.detekt`, Konsist checks in `com.plcoding.cardiganapp.tooling.konsist`.
- The default `test` task excludes `com/plcoding/cardiganapp/tooling/konsist/**` (D21). A second `Test` task `konsistTest` uses the same test classes with the `konsistTestRuntimeClasspath` runtime classpath, includes only that package, and uses `doNotTrackState("Konsist reads source files that are not task inputs.")`, like `mutationReport`, so it never comes UP-TO-DATE or FROM-CACHE (`org.gradle.caching=true` is set).
- `DomainLayerKonsistTest` (D20): `Konsist.scopeFromProject()`, filtered to files whose path relative to the project root starts with `core/domain/src/` or matches `feature/<name>/domain/src/`, and whose package contains `.domain`, all source sets. It asserts that no import of those files matches `..data..`, `..presentation..`, `..designsystem..`, `io.ktor..`, `org.koin..`, `androidx..`, `org.jetbrains.compose..`. A failure names file and import (Konsist's default assertion message does).

### Version catalog

New entries: version `detekt = "2.0.0-alpha.6"`, `konsist = "1.0.0-beta.1"`; libraries `detekt-api`, `detekt-test`, `detekt-core`, `detekt-gradlePlugin` (build-logic compileOnly), `konsist`, `kotlin-test-junit5`; plugins `detekt` (`dev.detekt`), `kotlinJvm`, and the three new convention plugin ids. Root `build.gradle.kts` declares `alias(libs.plugins.detekt) apply false` and `alias(libs.plugins.kotlinJvm) apply false` next to the others.

### Documentation

- CLAUDE.md: tech stack row "Lint / format" names detekt (with `:tooling` custom rules) and Konsist, on demand. Commands table gets `detekt`, `<module>:detekt`, `konsist`, `staticAnalysis` and `:tooling:test`, each marked "Not a gate".
- `.claude/tdd/project.md`: acceptance level for `:tooling` (D24: JVM tests running real detekt in-process with the repo config, through the rule set id and rule id only), area `tooling/...` → `:tooling`, run with `./gradlew :tooling:test`; full-check note "when a change touches `tooling/`, also run `./gradlew :tooling:test`".

## Risks / Trade-offs

- [detekt 2.0 is an alpha: config keys, task names and the rule API can change] → Version pinned in the catalog; upgrades are deliberate. Backend uses the same version, so both repos move together.
- [detekt alpha.6 is built on Kotlin 2.4.0, the project uses 2.4.20] → Type-resolved tasks may fail on newer metadata. Check on the first run; if it fails, raise it with the user rather than switching off type resolution.
- [AGP 9 KMP library plugin may not expose a type-resolved Android compilation task] → Checked first in the setup group; stop and ask if missing (D13).
- [`./gradlew detekt` fails on the current code] → Accepted (D6). The command still writes all reports, because findings fail the task after reports are written.
- [Konsist 1.0 is a beta] → Only one simple check depends on its API.
- [`ignoreFailures` under `staticAnalysis` hides a detekt failure until `staticAnalysisVerify` runs] → `staticAnalysisVerify` finalizes `staticAnalysis` and names the failing tool; documented in CLAUDE.md.
- [`detektPlugins(project(":tooling"))` inside `:tooling` makes the module load its own rules] → If Gradle rejects the self-reference, `:tooling` is analyzed without the `cardigan` set and the reason is noted in its build file.
- [Overlap list depends on final ktlint settings from `add-ktlint`] → Whichever change lands second re-checks the list and applies the ktlint convention plugin to `:tooling`.

## Migration Plan

Additive. No existing behavior or gate changes. Rollback is removing the plugin ids, the `:tooling` include and the catalog entries.
