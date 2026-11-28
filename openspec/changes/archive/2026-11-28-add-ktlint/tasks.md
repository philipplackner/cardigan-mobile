# Tasks

## 1. Rule settings and dependencies

- [x] 1.1 Add versions `ktlint = "1.8.0"` and `ktlintGradle = "14.2.0"`, plugin alias `ktlint` (`org.jlleitschuh.gradle.ktlint`), library `ktlint-gradlePlugin` and plugin alias `convention-ktlint` to `gradle/libs.versions.toml`; verify `./gradlew help` succeeds
- [x] 1.2 Create the root `.editorconfig` with one `[*.{kt,kts}]` section: `ktlint_code_style = ktlint_official`, `max_line_length = 120`, `ktlint_function_naming_ignore_when_annotated_with = Composable` and the `ij_kotlin_*` keys from design.md decision 4. Compare with the backend's `.editorconfig` (`cardigan.backend.dir`, read only) once it exists; the files must match apart from the Composable line. Verify by diffing the two files. If the backend's `.editorconfig` doesn't exist yet, use the keys from design.md decision 4, tell the user that the comparison is still open, and leave this task unchecked until the diff has been done

## 2. Convention plugin and wiring

- [x] 2.1 Add `compileOnly(libs.ktlint.gradlePlugin)` to `build-logic/convention`, create `KtlintConventionPlugin` (applies ktlint-gradle; `version` from the catalog; SARIF + Checkstyle reporters; console output; `ignoreFailures = false`; exclude files inside the project's build directory, as in design.md decision 1) and register it as `com.plcoding.cardiganapp.convention.ktlint`; verify `./gradlew -p build-logic :convention:validatePlugins` passes
- [x] 2.2 In the convention plugin, remove the ktlint tasks from `check`'s dependencies; verify `./gradlew :core:domain:check --dry-run` and `./gradlew :androidApp:check --dry-run` list no `ktlint` task
- [x] 2.3 Load `alias(libs.plugins.ktlint) apply false` and apply `alias(libs.plugins.convention.ktlint)` in the root `build.gradle.kts`; apply `alias(libs.plugins.convention.ktlint)` in all 20 KMP module build files and `:androidApp`; verify `./gradlew tasks --all | grep ktlintCheck` lists the task for every module and the root
- [x] 2.4 Apply `alias(libs.plugins.ktlint)` with the same extension settings in `build-logic/convention/build.gradle.kts`, `build-logic/pitest-filters/build.gradle.kts` and a new minimal `build-logic/build.gradle.kts`; verify `./gradlew -p build-logic ktlintCheck --dry-run` lists the script and source tasks of all three projects
- [x] 2.5 Make the root project's `ktlintCheck` and `ktlintFormat` depend on build-logic's tasks; verify `./gradlew ktlintCheck --dry-run` lists build-logic's tasks next to the module tasks
- [x] 2.6 Run `./gradlew ktlintCheck --continue` once and check coverage against the spec: violations are reported for commonMain, androidMain, iosMain, a test source set, a `*.gradle.kts` script and a build-logic file; nothing under `build/` is reported; SARIF and Checkstyle files exist under `build/reports/ktlint/`; PascalCase `@Composable` functions are not reported by function-naming. If ktlint fails to parse Kotlin 2.4 syntax, stop and tell the user
- [x] 2.7 Update CLAUDE.md: the "Lint / format" tech stack row (ktlint 1.8.0 through ktlint-gradle, rule settings in `.editorconfig`) and two rows in the Commands table for `./gradlew ktlintCheck` and `./gradlew ktlintFormat` (scope, report location, "Not a gate", and that `--continue` lists the violations of all modules in one run). The gates paragraph stays unchanged; verify by reading the diff
- [x] 2.8 Run all four gates (`:androidApp:assembleDebug`, `testAndroidHostTest`, `iosSimulatorArm64Test`, `:composeApp:linkDebugFrameworkIosSimulatorArm64`) and `./gradlew -p build-logic :convention:test`, since this commit touches build-logic, and verify none of them runs a ktlint task; commit as `build: add ktlint with check and format tasks`

## 3. One-time reformat

- [x] 3.1 Run `./gradlew ktlintFormat --continue` and review the diff contains formatting changes only
- [x] 3.2 Fix the remaining violations by hand. For a rule that clashes with a project convention, stop and ask the user whether to disable it in `.editorconfig`. For a violation whose only fix changes behavior or a client-visible name, add `@Suppress("ktlint:standard:<rule>")` at that declaration. Verify `./gradlew ktlintCheck --continue` passes with zero violations
- [x] 3.3 Run all four gates and `./gradlew -p build-logic :convention:test`, since the reformat touches build-logic, then commit the reformat on its own as `style: reformat code with ktlint`; verify `./gradlew ktlintCheck` still passes on the committed tree

## Workflow follow-up

- If `add-detekt-konsist` lands first, apply `convention.ktlint` to its `:tooling` module in group 2. If this change lands first, `add-detekt-konsist` does it.
- If `add-detekt-konsist` landed first, `build-logic/build.gradle.kts` and the CLAUDE.md "Lint / format" row may already exist: extend them, keeping the detekt content, instead of creating or replacing them.
- Archive the change after review.
