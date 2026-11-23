# Proposal

## Why

Line coverage shows which code the tests run, not whether they would catch a bug in it. The user wants a starting point for measuring mutation scores on the mobile code, run by hand and kept out of the normal workflow.

## What Changes

- **PIT** (new dependency, approved in D15): `pitest-command-line` and `pitest-aggregator` in the version catalog, resolved only when the task runs.
- **Module convention plugin** `com.plcoding.cardiganapp.convention.mutation` (D1, D3): registers a per-module PIT run on the module's Android host test classpath and test classes. `*UiTest` is excluded, as in the host run (D2).
- **All 20 KMP modules opt in** (D3). `:androidApp` doesn't.
- **Root task** `./gradlew mutationReport [-Pmutation.modules=…] [-Pmutation.threads=…] [-Pmutation.minScore=…]` (D4, D5, D13, D14):
  - runs PIT for the selected modules (default: all opted-in)
  - merges the results into one HTML report at `build/reports/pitest/index.html` (D6)
  - prints mutation score, test strength, mutant counts and the report path (D7)
  - analyzes every mutant on every run; incremental analysis was dropped because open-source PIT no longer has a history store (D12)
  - fails on a low score only when `-Pmutation.minScore` is passed (D14)
- **Configuration**: `DEFAULTS` mutators (D9), Kotlin noise filtered in config (D8), the coverage exclusion patterns (D10), composables kept in (D11).
- **Same names as the backend** for the task and all properties (D17).
- **Not part of the workflow**: not wired into `check`, `build`, the gates, CI or any hook.
- **Docs** (D16): the command goes into the Commands and Tech Stack tables in `CLAUDE.md`, marked "Not a gate".

## Shared-spec requirements

None. This change adds local developer tooling only and depends on no shared requirement.

## Capabilities

### New Capabilities
- `mutation-testing`: running mutation tests by hand on a chosen set of mobile modules and producing an HTML report.

### Modified Capabilities

None.

## Impact

- `gradle/libs.versions.toml`: PIT version and libraries, the `convention-mutation` and `convention-mutation-root` plugin aliases.
- `build-logic/convention`: the module plugin and the root plugin. No new Gradle plugin dependency.
- Root build: applies `convention.mutation.root` next to `convention.coverage` (see design.md decision 2).
- The 20 KMP module `build.gradle.kts` files: one `alias(libs.plugins.convention.mutation)` line each.
- `CLAUDE.md`: docs.
- No effect on the gates, app code or tests.
