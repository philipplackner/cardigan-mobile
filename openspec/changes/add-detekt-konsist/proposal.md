# Proposal

## Why

The mobile codebase has no static analysis. Code smells, likely bugs and architecture violations (for example a domain module that imports Ktor) are only found in review. The user wants the tooling in place to find them on demand, set up the same way as in the backend, before any project-specific rules are written.

## What Changes

- Add detekt `2.0.0-alpha.6` and Konsist `1.0.0-beta.1` to the version catalog (D1, D2).
- Add a detekt convention plugin. Every KMP module, `:androidApp` and the new `:tooling` module opt in by applying it (D12). It uses detekt's default rules plus the overrides in a new `config/detekt/detekt.yml`. detekt rules that overlap ktlint are turned off (D7, D8).
- Analyze production and test source sets. `commonMain`, `androidMain`, Android host tests and Android device tests run with type resolution, `iosMain` and `iosTest` without (D11, D13, D14). build-logic's `:convention` module is analyzed with the same config and default rules only (D15); the Java-only `:pitest-filters` is not.
- Add a new top-level JVM module `:tooling`, built by a new `jvm` convention plugin (D3):
  - `src/main`: custom detekt rule set `cardigan` with the sample rule `NoMockingLibraries`. It reports imports of `org.mockito.*` and `io.mockk.*` in test source sets (D17–D19).
  - `src/test`: unit tests for the rule, and one Konsist check that domain packages don't import data, presentation, design system or framework packages (D20).
- Add root commands `./gradlew detekt`, `./gradlew konsist` and `./gradlew staticAnalysis`. detekt fails on any finding. `staticAnalysis` runs both, then fails if either failed (D5, D9, D22). Single modules run through their task path (D10).
- Write HTML and SARIF reports per module, plus one merged SARIF file (D16).
- Update CLAUDE.md (tech stack, commands) and `.claude/tdd/project.md` (area `:tooling`, its test command).
- Nothing becomes a gate. Existing findings are neither fixed nor baselined (D4, D6).

## Capabilities

### New Capabilities

- `static-analysis`: On-demand detekt and Konsist commands for the mobile codebase: what they analyze, when they fail, their reports, and the custom rule set they load.

### Modified Capabilities

None.

## Shared specs

This change implements and depends on no requirement in `cardigan-shared-specs`. Tooling is per repo. Command names, versions, module layout and the sample rule match the backend change `be-detekt-konsist` by decision, not by a shared spec.

## Impact

- **Build**: `gradle/libs.versions.toml`, root `build.gradle.kts`, `settings.gradle.kts` (adds `:tooling`), `build-logic/convention` (two new convention plugins), `build-logic/build.gradle.kts` or its modules' build files (detekt for build-logic), every module's `build.gradle.kts` (opt-in plugin id).
- **New files**: `config/detekt/detekt.yml`, the `tooling/` module.
- **Docs**: `CLAUDE.md`, `.claude/tdd/project.md`.
- **Dependencies**: detekt (an alpha, so config keys and the rule API can still change before 2.0 stable) and Konsist (a beta). Both need user approval per CLAUDE.md, which this change records.
- **Related change**: `add-ktlint` is independent and can land before or after. Whichever lands second applies the ktlint convention plugin to `:tooling` and checks that the detekt overlap list matches the final ktlint settings (`ktlint_official`, max line length 120).
- **Backend**: `be-detekt-konsist` makes the same change in the backend repo. Keep versions, command names, the rule set and `detekt.yml` overrides in sync.
- Gates, `testAndroidHostTest`, `coverageReport` and `mutationReport` are unchanged.
