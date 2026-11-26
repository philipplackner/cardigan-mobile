# Decisions: add-detekt-konsist

Confirmed with the user on 2026-11-26. The orchestrator session (`cardigan-ae`) asked the questions and relayed the answers, so they could be merged with the backend's `be-detekt-konsist` change. Decisions marked "both repos" were made once for backend and mobile and must stay identical.

Scope given by the user: set up detekt and Konsist with the build setup and Gradle commands to run them. No advanced rules yet. Add a separate module for tooling and future custom detekt rules, with one sample custom rule that shows the idea. Decisions, command structure and build setup must match the backend.

**Workflow**: Test-first (careful-planning-tdd) for the sample custom detekt rule only; not for the build setup, Gradle tasks, detekt config, the Konsist check or the docs, which become `[plain]` tasks. The rule is the only new code with behavior a unit test can observe.

## Summary

Add detekt 2.0.0-alpha.6 and Konsist 1.0.0-beta.1 as on-demand tools. A new JVM module `:tooling` holds the custom detekt rule set `cardigan` with one rule, `NoMockingLibraries`, and one Konsist layer check in its tests. Three root commands run them: `detekt`, `konsist` and `staticAnalysis`. Nothing becomes a gate, and existing findings are neither fixed nor baselined.

## Decisions

### D1 — detekt version
- **Question**: Which detekt version?
- **Decision**: `2.0.0-alpha.6` (plugin id `dev.detekt`, group `dev.detekt`), pinned in the version catalog. Both repos.
- **Alternatives**: 2.0 alpha on mobile and 1.23.8 on backend; wait for 2.0 stable.
- **Why**: 1.23.8 can't analyze Kotlin 2.4. The repos must stay identical.
- **Depends on**: —

### D2 — Konsist version
- **Question**: Which Konsist version?
- **Decision**: `1.0.0-beta.1`, pinned in the version catalog. Both repos.
- **Alternatives**: 0.18.1 (newest stable).
- **Depends on**: —

### D3 — Tooling module
- **Question**: How is tooling code split into modules?
- **Decision**: One top-level JVM module `:tooling`. Custom detekt rules live in `src/main`, Konsist checks in `src/test`. A small new `jvm` convention plugin builds it. Both repos.
- **Alternatives**: two modules `:tooling:detekt-rules` and `:tooling:konsist`; rules module inside build-logic.
- **Depends on**: —

### D4 — Not gates
- **Question**: Do detekt and Konsist become gates?
- **Decision**: No. On demand only: not in the CLAUDE.md gates, not hooked into `check` or CI, no git or agent hook. All tools in all repos.
- **Alternatives**: both gates; Konsist gate only.
- **Depends on**: —

### D5 — Failure on findings
- **Question**: What does the detekt command do when it finds issues?
- **Decision**: It fails on any finding when it is run.
- **Alternatives**: report only; fail above a threshold property.
- **Depends on**: D4

### D6 — Existing findings
- **Question**: How are findings in the existing code handled?
- **Decision**: Neither fixed nor baselined. No baseline file. The user: "I just want to have the tooling to find these issues. Since it's not integrated anywhere in CI or a local hook or so, we can just leave it."
- **Alternatives**: fix in this change; commit a baseline; fix some and baseline the rest.
- **Depends on**: D4, D5

### D7 — Active rules
- **Question**: Which detekt rules are active?
- **Decision**: detekt's default active rules. `config/detekt/detekt.yml` holds only overrides (built on the default config). Both repos.
- **Alternatives**: all rules; defaults minus style rules.
- **Depends on**: D1

### D8 — Relation to ktlint
- **Question**: How does detekt relate to the `add-ktlint` change?
- **Decision**: ktlint owns formatting. No ktlint wrapper rule set in detekt. detekt rules that overlap ktlint are turned off in `detekt.yml`. The two changes are independent and can land in any order. Whichever lands second also applies the ktlint convention plugin to `:tooling`.
- **Alternatives**: keep the overlapping detekt rules; replace standalone ktlint with detekt's wrapper.
- **Depends on**: D7

### D9 — Commands
- **Question**: What Gradle command structure runs static analysis?
- **Decision**: Root tasks `detekt` (all opted-in modules plus build-logic), `konsist` (Konsist checks only) and `staticAnalysis` (both). Same names in both repos.
- **Alternatives**: only `check`; `detekt` and `konsistTest` without an aggregate.
- **Depends on**: D4

### D10 — Module selection
- **Question**: Does `detekt` take a module selector property?
- **Decision**: No. One module runs through its Gradle task path, for example `./gradlew :feature:boards:data:detekt`.
- **Alternatives**: `-Pdetekt.modules=` like coverage and mutation on mobile.
- **Depends on**: D9

### D11 — Analyzed code
- **Question**: Which code does detekt analyze?
- **Decision**: Production and test source sets of every opted-in module, plus build-logic and `:tooling`. Generated code under build directories is excluded.
- **Alternatives**: any subset of these.
- **Depends on**: D12

### D12 — Opt-in per module
- **Question**: How do modules get detekt?
- **Decision**: Through an opt-in detekt convention plugin applied in each module's build file, like the coverage and mutation pattern. All KMP modules, `:androidApp` and `:tooling` apply it. Decided once for ktlint and detekt in both repos.
- **Alternatives**: applied automatically to all modules.
- **Depends on**: —

### D13 — Type resolution
- **Question**: Does detekt run with type resolution?
- **Decision**: Yes, where detekt supports it.
- **Alternatives**: without type resolution.
- **Depends on**: D1

### D14 — iOS source sets
- **Question**: iOS code can't be type-resolved. How is it covered?
- **Decision**: `commonMain`, `androidMain` and Android host tests are analyzed type-resolved through the Android compilation. `iosMain` and `iosTest` are analyzed in light mode (no type resolution, type-aware rules skipped). `./gradlew detekt` runs both. Mobile only.
- **Alternatives**: skip iOS source sets; light mode everywhere.
- **Depends on**: D13

### D15 — build-logic
- **Question**: How is build-logic analyzed?
- **Decision**: detekt is wired in build-logic's own build file with the same version and `config/detekt/detekt.yml`, default rules only, no custom rule set. The root `detekt` task also runs it. Decided once for ktlint and detekt in both repos.
- **Alternatives**: leave build-logic out.
- **Depends on**: D11

### D16 — Reports
- **Question**: Which report formats?
- **Decision**: HTML and SARIF per module. SARIF is also merged into one file at `build/reports/detekt/`.
- **Alternatives**: Checkstyle XML; Markdown.
- **Depends on**: —

### D17 — Sample custom rule
- **Question**: What does the sample custom rule check?
- **Decision**: No mocking libraries: reports imports of `org.mockito.*` and `io.mockk.*`, including star imports and aliased imports. Fully qualified references without an import are not reported. Both repos.
- **Alternatives**: no `println`; no `java.*` imports in `commonMain`; a rule per repo.
- **Depends on**: —

### D18 — Sample rule scope
- **Question**: Where does `NoMockingLibraries` apply?
- **Decision**: Test source sets only. Both repos.
- **Alternatives**: all analyzed code.
- **Depends on**: D17

### D19 — Rule names
- **Question**: What are the custom rule set and rule called?
- **Decision**: Rule set id `cardigan`, rule `NoMockingLibraries`. Both repos.
- **Alternatives**: `cardigan-rules` / `ForbiddenMockingImport`.
- **Depends on**: D17

### D20 — Konsist check
- **Question**: Which Konsist check ships?
- **Decision**: One layer check: files in `*.domain` packages of the core and feature domain modules don't import `*.data.*`, `*.presentation.*`, `*.designsystem.*`, `io.ktor.*`, `org.koin.*`, `androidx.*` (including `androidx.compose.*`) or `org.jetbrains.compose.*`. Mobile equivalent of the backend's "domain imports no Spring/jakarta.persistence".
- **Alternatives**: a naming check; only the project layers; only the framework packages; no check.
- **Depends on**: D3

### D21 — Running the tooling tests
- **Question**: How do the `:tooling` tests run?
- **Decision**: `./gradlew konsist` runs only the Konsist checks. The rule's unit tests run with `./gradlew :tooling:test`, which leaves out the Konsist checks, when tooling code changes. Nothing new goes into the gates. Both repos.
- **Alternatives**: `konsist` runs all `:tooling` tests; rule tests become a gate.
- **Depends on**: D3, D4

### D22 — Aggregate failure
- **Question**: Does `staticAnalysis` still run Konsist when detekt fails?
- **Decision**: Yes. It runs both, then fails if either failed. Both repos.
- **Alternatives**: stop at the first failure.
- **Depends on**: D9

### D23 — Coverage and mutation
- **Question**: Is `:tooling` part of `coverageReport` and `mutationReport`?
- **Decision**: No.
- **Depends on**: D3

### D24 — Acceptance level for :tooling
- **Question**: The project's acceptance level (feature UI tests) can't observe a detekt rule. What is the acceptance level for `:tooling`?
- **Decision**: JVM tests in `tooling/src/test` that reach the rule only through detekt's public mechanisms, never the rule class. Recorded in `.claude/tdd/project.md`. Asked after the blind test plan (round 4).
- **Alternatives**: no acceptance level, component tests only.
- **Depends on**: D3, D17

### D25 — Real detekt in acceptance tests
- **Question**: How do the acceptance tests run the rule?
- **Decision**: `dev.detekt:detekt-core` 2.0.0-alpha.6 is a test-only dependency of `:tooling` (approved). Acceptance tests run real detekt in-process with `config/detekt/detekt.yml` and cover the test-sources-only scope (a main file is not flagged, a test file is) and `@Suppress` handling. Both repos.
- **Alternatives**: ServiceLoader + rule set only.
- **Depends on**: D24, D18

### D26 — Finding message
- **Question**: Does the finding message have an exact text?
- **Decision**: Yes, shared by both repos: `Import of '<path>' is not allowed. Use fakes instead of mocking libraries.` Tests assert the full text.
- **Alternatives**: only path and "fake" must appear.
- **Depends on**: D17

### D27 — Path for star imports
- **Question**: Which path does the message name for `import io.mockk.*`?
- **Decision**: As written: `io.mockk.*`. For aliased imports the path without the alias (`org.mockito.Mockito.mock`). Both repos.
- **Alternatives**: the package `io.mockk`.
- **Depends on**: D26

### D28 — Case sensitivity
- **Question**: Is matching case-sensitive?
- **Decision**: Yes. `Org.Mockito` is not flagged. Both repos.
- **Depends on**: D17

### D29 — Rule off unless configured
- **Question**: Is the rule active by default?
- **Decision**: No. The rule is inactive unless configured; the shared `detekt.yml` sets `active: true`. Both repos.
- **Depends on**: D7, D17

### D30 — Who writes the Konsist check
- **Question**: The Konsist check lives in `tooling/src/test`, which the TDD path guard reserves for test code. Who writes it?
- **Decision**: The test-writer agent, as a `[plain]` task. The implementer's path guard stays unchanged. Both repos (mobile has the same guard: `*/src/test/*` in `.claude/tdd/tdd.conf`).
- **Depends on**: D20

## Facts

- Project uses Kotlin 2.4.20, AGP 9.1.1, Gradle 9, KMP modules on `com.android.kotlin.multiplatform.library` — source: `gradle/libs.versions.toml`
- detekt 1.23.8 is the last stable release and is built for Kotlin 2.0; 2.0.0-alpha.6 (Aug 2026) is the newest and supports Kotlin 2.4 — source: Maven Central metadata for `io.gitlab.arturbosch.detekt` and `dev.detekt`, detekt.dev 2.0.0 changelog
- detekt 2.0 on KMP: full analysis (type resolution) only for JVM and Android compilations; other compilations and the plain `detekt`/`detekt<SourceSet>SourceSet` tasks run without type resolution — source: detekt.dev migration guide and type-resolution docs
- detekt 2.0 ships a ktlint wrapper rule set (`detekt-rules-ktlint-wrapper`) — source: Maven Central `dev.detekt` artifacts
- Konsist 1.0.0-beta.1 (Nov 4, 2026) bundles kotlin-compiler-embeddable 2.4.21 — source: its POM on Maven Central
- build-logic is an included build used by the main build for its convention plugins, with a second JVM module `pitest-filters` — source: `settings.gradle.kts`, `build-logic/settings.gradle.kts`
- The main build has no plain JVM module or JVM convention plugin yet — source: `build-logic/convention/src/main/kotlin/`
- `testAndroidHostTest` only covers KMP modules — source: `.claude/tdd/project.md`
- Existing on-demand tools use root tasks (`coverageReport`, `coverageDiff`, `mutationReport`) and are not gates — source: `CLAUDE.md`
- No shared spec in `cardigan-shared-specs` covers tooling or static analysis — source: `openspec list --specs --store cardigan-shared-specs`

## Out of scope

- Advanced or project-specific detekt rules beyond the sample, Compose-specific detekt rule sets
- Fixing existing findings, a baseline file
- CI, git hooks, agent hooks, adding anything to the gates or to `check`
- ktlint setup (change `add-ktlint`), running `ktlintCheck` from `staticAnalysis`
- More Konsist checks

## Deferred

- None.
