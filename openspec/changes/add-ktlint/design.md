# Design

## Context

See proposal.md for the motivation and decisions.md for every settled choice. Relevant current state:

- All Gradle wiring lives in `build-logic/convention` convention plugins. Plugins that modules apply are on build-logic's classpath as `compileOnly` and are loaded once at the root with `alias(...) apply false`.
- Modules opt into tooling one by one (`convention.kover`, `convention.mutation` in each `build.gradle.kts`).
- Module types: 20 KMP modules (`org.jetbrains.kotlin.multiplatform` + `com.android.kotlin.multiplatform.library`, source sets commonMain, androidMain, iosMain and tests) and `:androidApp` (`com.android.application` with AGP 9 built-in Kotlin, no `kotlin-android` plugin).
- build-logic is an included build with two projects: `:convention` (Kotlin, `kotlin-dsl`) and `:pitest-filters` (`java-library`, Java sources only). It has a `settings.gradle.kts` but no root build file.
- There is no `.editorconfig`.

## Goals / Non-Goals

**Goals:**
- `./gradlew ktlintCheck` and `./gradlew ktlintFormat` from the repo root cover modules, scripts and build-logic in one call.
- The same plugin, versions, task names, `.editorconfig` content (apart from the Composable line) and report formats as the backend.

**Non-Goals:**
- Any task that wraps or renames the plugin's tasks.
- Hooking ktlint into `check`, the gates, git hooks or CI.

## Decisions

### 1. ktlint-gradle applied through a convention plugin
New `KtlintConventionPlugin`, id `com.plcoding.cardiganapp.convention.ktlint`, registered in `build-logic/convention/build.gradle.kts` like the others. It applies `org.jlleitschuh.gradle.ktlint` and configures the `ktlint` extension:
- `version` = the catalog's `ktlint` version (1.8.0), so the ktlint engine is pinned separately from the plugin.
- `reporters`: `SARIF` and `CHECKSTYLE`. ktlint-gradle writes them to `build/reports/ktlint/`. `outputToConsole = true` keeps the console output.
- `ignoreFailures = false`.
- `filter { exclude { it.file.toPath().startsWith(project.layout.buildDirectory.get().asFile.toPath()) } }`, so generated sources registered on a source set (for example the future OpenAPI DTOs and compose resource accessors) are never checked. Only files inside the project's own build directory are excluded, whatever the checkout path. build-logic uses the same expression.

Catalog: versions `ktlint = "1.8.0"` and `ktlintGradle = "14.2.0"`, plugin alias `ktlint`, library `ktlint-gradlePlugin` (`org.jlleitschuh.gradle:ktlint-gradle`) added as `compileOnly` to `:convention`, plugin alias `convention-ktlint`. The root build file loads `alias(libs.plugins.ktlint) apply false`, following the existing pattern.

The plugin detects KMP modules through the Kotlin multiplatform plugin and creates tasks per source set, including iosMain and the test source sets. `:androidApp` is detected through AGP 9 built-in Kotlin, which needs ktlint-gradle ≥ 13.1.0. Each project also gets `*.gradle.kts` script tasks for the scripts in its own directory.

Alternatives (Spotless, an own JavaExec task) were rejected in D2.

### 2. Keep ktlintCheck out of check
ktlint-gradle makes `check` depend on `ktlintCheck` by default. D7 forbids that. The convention plugin removes the ktlint tasks from `check`'s dependencies after the plugin is applied. Verify with `./gradlew check --dry-run` (no `ktlint` task listed) on a KMP module and on `:androidApp`. If a plugin version offers a supported switch for this, use it instead.

### 3. Root project and build-logic
- The root `build.gradle.kts` applies `convention.ktlint`. This checks the root `build.gradle.kts` and `settings.gradle.kts`.
- The convention plugin, when applied to the root project, makes the root's `ktlintCheck` and `ktlintFormat` depend on the same-named tasks of build-logic's projects through `gradle.includedBuild("build-logic").task(":<project>:ktlintCheck")`. Running `./gradlew ktlintCheck` selects the task in every project plus build-logic's.
- build-logic can't apply its own convention plugin, so `build-logic/convention/build.gradle.kts` and `build-logic/pitest-filters/build.gradle.kts` apply `alias(libs.plugins.ktlint)` directly with the same extension settings (D14). A new minimal `build-logic/build.gradle.kts` applies it to build-logic's root project, so `build-logic/settings.gradle.kts` is checked too. The duplicated configuration is a few lines; keep it identical to the convention plugin.
- `:pitest-filters` has Java sources only, so only its build script is checked.

### 4. .editorconfig
One `[*.{kt,kts}]` section, no `[*]` section (decided with the backend):
- `ktlint_code_style = ktlint_official`
- `max_line_length = 120`
- `ktlint_function_naming_ignore_when_annotated_with = Composable` (mobile only)
- `ij_kotlin_*` keys that make IntelliJ's formatter match ktlint_official, for example: `ij_kotlin_code_style_defaults = KOTLIN_OFFICIAL`, `ij_kotlin_allow_trailing_comma = true`, `ij_kotlin_allow_trailing_comma_on_call_site = true`, `ij_kotlin_imports_layout = *,java.**,javax.**,kotlin.**,^`, `ij_kotlin_packages_to_use_import_on_demand = unset`, `ij_kotlin_name_count_to_use_star_import = 2147483647`, `ij_kotlin_name_count_to_use_star_import_for_members = 2147483647`. The exact set must match the backend's file; coordinate the final key list with the backend's change so both files are identical apart from the Composable line.

### 5. One-time reformat
After the setup commit, run `./gradlew ktlintFormat --continue`, then fix the remaining violations by hand until `./gradlew ktlintCheck --continue` passes (D9). Without `--continue`, Gradle stops after the first failing ktlint task and leaves the remaining modules unformatted. The rules for remaining violations (D10):
- Fix the code to the rule.
- If a rule clashes with a project convention (for example a Compose naming pattern or the `*Preview.kt` file layout), stop and ask the user whether to disable that rule in `.editorconfig`.
- If the only fix changes behavior or a client-visible name (a serialized property name, an API enum constant), add `@Suppress("ktlint:standard:<rule>")` at that declaration only.

The reformat goes in its own commit, separate from the setup, and all four gates must pass before it (formatting changes no behavior, so tests prove it).

### 6. Docs
CLAUDE.md: the "Lint / format" tech stack row names ktlint, ktlint-gradle and the `.editorconfig`. The Commands table gets `./gradlew ktlintCheck` and `./gradlew ktlintFormat` with their scope and report location, marked "Not a gate". The Commands table notes that `--continue` lists the violations of all modules in one run. The gates paragraph is unchanged. The convention plugin list in the "exceptions, already wired" paragraph isn't affected, since ktlint brings no dependency to modules.

## Risks / Trade-offs

- [ktlint 1.8.0 may not parse some Kotlin 2.4 syntax] → Found on the first `ktlintCheck` run as parse errors. Stop and tell the user (options: newer ktlint, exclude the file); don't work around it silently.
- [Removing the `check` dependency relies on ktlint-gradle internals] → Verified by the dry-run check in decision 2; recheck after plugin upgrades.
- [Unknown number of hand fixes] → The planning dry run was blocked by permissions. The reformat task is sized as its own commit, and per-rule clashes go to the user.
- [Large formatting diff touches almost every file] → Own commit, no other changes in it, so `git blame --ignore-rev` can skip it later.
- [New modules forget to opt in] → Same as coverage and mutation today; accepted.
- [Two copies of the extension settings (convention plugin and build-logic)] → Kept to a few lines and listed in the tasks to keep identical.

## Migration Plan

No migration. Rollback is reverting the two commits. The reformat commit can be reverted on its own.

## Open Questions

- The exact `ij_kotlin_*` key list, to be aligned with the backend's `.editorconfig` during implementation. It doesn't change scope or tasks. If the backend's file doesn't exist yet, use the keys from decision 4 and tell the user the comparison is still open.
