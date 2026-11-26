# Decisions: add-ktlint

Confirmed with the user on 2026-11-26. The orchestrator session (`cardigan-ae`) asked the questions and relayed the answers, so they could be batched with the backend ktlint change (`be-ktlint`) and the detekt/Konsist and web changes. Cross-repo decisions are identical for backend and mobile.

Scope given by the user:
- Set up ktlint as linter and formatter, with Gradle commands to check and to auto-format the code.
- "Do not enforce any of that yet. Do not auto-format. Just add the capability for us to call these CLI commands. I will add this later."
- Rule settings only. No custom rules (those come later through detekt) and no advanced rule work.
- Decisions, CLI command structure and build setup must be identical between backend and mobile.

## Summary

Add ktlint through the ktlint-gradle plugin with the plugin's own `ktlintCheck` and `ktlintFormat` tasks, run by hand only. Rule settings live in a new root `.editorconfig` (ktlint_official, max line 120, composables exempt from function naming, matching IntelliJ formatter keys). Every module, the root project and build-logic are covered. The existing code is reformatted once, in its own commit, until `ktlintCheck` reports zero violations.

## Decisions

### D1 — Workflow
- **Question**: Test-first (careful-planning-tdd) or plain (careful-planning)?
- **Decision**: Plain.
- **Alternatives**: Test-first.
- **Why**: Build tooling only; verified by running the tasks, not by new tests.
- **Depends on**: —

### D2 — Gradle integration
- **Question**: How is ktlint wired into Gradle in both repos?
- **Decision**: The ktlint-gradle plugin (`org.jlleitschuh.gradle.ktlint`).
- **Alternatives**: Spotless with a ktlint step; our own task running the ktlint CLI.
- **Why**: Chosen by the user, same as the backend. The AGP 9 risk was checked: built-in Kotlin is supported since 13.1.0, so there is no known blocker.
- **Depends on**: —

### D3 — Command names
- **Question**: Which task names check and auto-format the code?
- **Decision**: The plugin's own `ktlintCheck` and `ktlintFormat`. No custom alias tasks.
- **Alternatives**: `lint` + `format`; other tool defaults.
- **Why**: User: just add the capability to call these commands.
- **Depends on**: D2

### D4 — Code style
- **Question**: Which `ktlint_code_style`?
- **Decision**: `ktlint_official`.
- **Alternatives**: `intellij_idea`, `android_studio`.
- **Depends on**: —

### D5 — Rule settings file
- **Question**: Where do rule settings live, and what is in them?
- **Decision**: A new root `.editorconfig` with a Kotlin section only (no general `[*]` section): `ktlint_code_style = ktlint_official`, `max_line_length = 120`, and on mobile `ktlint_function_naming_ignore_when_annotated_with = Composable`. No existing rule settings existed, and the user said "decide now".
- **Alternatives**: none offered; no settings existed to follow.
- **Depends on**: D4, D8, D11

### D6 — File scope
- **Question**: Which files are checked and formatted?
- **Decision**: Main and test Kotlin sources of every module (on mobile: commonMain, androidMain, iosMain and their tests), Gradle Kotlin scripts (`*.gradle.kts`), and build-logic sources. `build/` and generated code are excluded.
- **Alternatives**: module `src/` Kotlin only.
- **Depends on**: —

### D7 — Enforcement
- **Question**: Is the ktlint check a commit gate?
- **Decision**: On demand only. Not in the CLAUDE.md gates, not wired into `check`, no git hook, no CI. Nothing formats automatically.
- **Alternatives**: gate plus `check`; gate only.
- **Why**: User: "Do not enforce any of that yet." Applies to every tool in every repo.
- **Depends on**: —

### D8 — Compose function naming
- **Question**: How to handle ktlint's function-naming rule for PascalCase `@Composable` functions?
- **Decision**: Ignore it for functions annotated with `Composable`, via `ktlint_function_naming_ignore_when_annotated_with = Composable`. The rule stays on for everything else. Mobile only.
- **Alternatives**: disable the rule.
- **Depends on**: —

### D9 — Existing code
- **Question**: How is existing code that violates the style handled?
- **Decision**: One-time reformat in this change, as its own commit. Goal is zero violations: `ktlintFormat` plus hand fixes until `ktlintCheck` passes.
- **Alternatives**: baseline file.
- **Depends on**: D2

### D10 — Violations the formatter can't fix
- **Question**: What happens with violations `ktlintFormat` can't fix?
- **Decision**: Fix by hand. If a rule clashes with a project convention, the implementer stops and asks whether to disable it. A violation that can't be fixed without changing behavior or a client-visible name (serialized property, API enum constant, DB column) is suppressed at the spot with `@Suppress("ktlint:standard:<rule>")`; the rule stays on elsewhere.
- **Alternatives**: fix all by hand and never disable; disable every rule that still fails.
- **Depends on**: D9

### D11 — Max line length
- **Question**: Which max line length?
- **Decision**: 120.
- **Depends on**: D4

### D12 — IDE formatter settings
- **Question**: Does `.editorconfig` also set IntelliJ's `ij_kotlin_*` keys?
- **Decision**: Yes, so IDE "Reformat Code" produces ktlint_official output.
- **Alternatives**: ktlint keys only.
- **Depends on**: D4, D5

### D13 — Module opt-in
- **Question**: How do modules get ktlint, and which modules apply it in this change?
- **Decision**: An opt-in convention plugin that each module applies, like the coverage/mutation pattern. All existing modules, `:androidApp` and the root project apply it now. New modules opt in when they are created.
- **Alternatives**: applied automatically; plugin only, no modules.
- **Depends on**: D2, D6

### D14 — build-logic
- **Question**: How is build-logic covered, given it is an included build?
- **Decision**: Apply ktlint-gradle directly in build-logic's build files. The root `ktlintCheck` and `ktlintFormat` depend on build-logic's tasks, so one command covers everything.
- **Alternatives**: a separate `./gradlew -p build-logic ktlintCheck` command.
- **Depends on**: D3, D6, D13

### D15 — Reports
- **Question**: Which report outputs?
- **Decision**: Console output plus SARIF and Checkstyle report files under `build/reports`.
- **Depends on**: D2

### D16 — Docs
- **Question**: How are the new commands documented?
- **Decision**: CLAUDE.md's "Lint / format" tech stack row and the Commands table are updated with `ktlintCheck` and `ktlintFormat`, marked not a gate. They are not added to the gates.
- **Depends on**: D3, D7

### D17 — Versions
- **Question**: Which versions?
- **Decision**: ktlint-gradle 14.2.0 and ktlint 1.8.0 (latest at planning time), both pinned in the version catalog. Confirmed as part of the shared-understanding summary.
- **Depends on**: D2

## Facts

- No `.editorconfig`, ktlint or Spotless config exists in the mobile repo, the backend repo or the shared specs. Both repos set only `kotlin.code.style=official`; the IDE code style is KOTLIN_OFFICIAL defaults — source: `gradle.properties` in both repos, Android Studio `codestyles/Default.xml`.
- Latest ktlint-gradle is 14.2.0; latest ktlint is 1.8.0 — source: Gradle plugin portal and Maven Central metadata.
- ktlint-gradle supports AGP 9 built-in Kotlin since 13.1.0 (PR #1016, issue #1008). `:androidApp` uses built-in Kotlin (no `kotlin-android` plugin) — source: AGP 9 plugin status page, `AndroidApplicationConventionPlugin.kt`.
- Every KMP module opts into coverage/mutation by applying a convention plugin in its own `build.gradle.kts`; 20 KMP modules plus `:androidApp` — source: module build files.
- build-logic is an included build with `:convention` and `:pitest-filters` — source: `build-logic/settings.gradle.kts`.
- There are about 370 `.kt`/`.kts` files written without ktlint — source: `find`.
- A dry ktlint run was not possible during planning: running the downloaded binary was blocked by this session's permissions. The number of violations the formatter can't fix is unknown.
- Not verified: whether ktlint 1.8.0 parses all Kotlin 2.4 syntax used in the repo.

## Out of scope

- Custom rules and the compose-rules ruleset (later, through detekt).
- detekt and Konsist (separate change `add-detekt-konsist`, independent order). detekt owns no formatting; its ktlint-overlapping rules are off there. The `:tooling` module from that change applies the ktlint convention plugin; whichever change lands second does that.
- Running `ktlintCheck` from a `staticAnalysis` task.
- Any enforcement: gates, `check`, git hooks, CI, automatic formatting.
- Swift code in `iosApp`.
- Baseline files.

## Deferred

- Whether a specific ktlint_official rule should be disabled because it clashes with a project convention — settle before: implementation (the implementer asks per rule, D10).
