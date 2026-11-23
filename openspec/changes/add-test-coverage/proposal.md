# Proposal

## Why

There is no way to see how much of the mobile code the tests cover, or whether the code changed on a branch is tested. The user wants a setup that generates these reports from the terminal on demand, with the same commands as the other Cardigan repos (D14).

## What Changes

- **Kover** (new dependency, approved in D1) is added to the version catalog and to `build-logic`.
- **Module convention plugin** `com.plcoding.cardiganapp.convention.kover`. A module opts in by applying this one plugin id. It measures the module's Android host JVM tests only (D2), and applies the shared exclusions:
  - every `@Composable` function, previews included (D3)
  - generated code, Koin DI modules, debug-only fixtures and androidMain `actual`s, all by pattern or annotation (D4)
- **All 20 KMP modules opt in**: core, feature and `:composeApp`. `:androidApp` doesn't (D5).
- **Normal test runs stay as they are**: Kover instruments tests only when `coverageReport` or `diffCoverage` runs. The `testAndroidHostTest` gate isn't instrumented and isn't slower (D17).
- **Root convention plugin** `com.plcoding.cardiganapp.convention.coverage`, applied in the root build, with two tasks:
  - `./gradlew coverageReport [-Pcoverage.modules=…] [-Pcoverage.minLine=…] [-Pcoverage.minBranch=…]`. It runs the host tests of the selected modules, writes one merged XML and HTML report, and prints total line and branch coverage. Without `coverage.modules` it covers every module that opted in (D6, D7). It fails only when a threshold is passed and not met (D8).
  - `./gradlew diffCoverage [-Pcoverage.modules=…] [-Pcoverage.compareBranch=…]`. It builds the Kover XML, then runs `uvx --exclude-newer <date> diff-cover==11.0.4` (the same version as the other repos, D16, with its dependency set fixed by date) against `origin/master` by default. Committed, staged, unstaged and untracked changes all count. It prints the result and writes an HTML report under `build/`. It never fails because of low coverage (D9, D11, D12, D18).
  - If a coverage or Kover report task is requested while instrumentation is off, for example when called as `cR`, the build fails before any task runs and asks for the full task name, the same way as in the backend repo (D20).
- **Docs**: the new commands go into the Commands table in `CLAUDE.md`, and Kover and diff-cover into the Tech Stack table. Coverage isn't added to the gates (D10).
- No CI (D13), no iOS or on-device coverage (D2), and no coverage policy anywhere (D15).

## Shared-spec requirements

None. This change adds local developer tooling only. Per D15, no coverage policy goes into cardigan-shared-specs, and this change doesn't depend on any shared requirement.

## Capabilities

### New Capabilities
- `test-coverage`: generating test coverage reports for a chosen set of mobile modules, and diff coverage against a base branch, from the terminal.

### Modified Capabilities

None.

## Impact

- `gradle/libs.versions.toml`: Kover version and plugin, two convention plugin aliases.
- `build-logic/convention`: Kover as a `compileOnly` dependency, two new convention plugins.
- Root `build.gradle.kts`: loads Kover once and applies the root coverage plugin.
- The 20 KMP module `build.gradle.kts` files: one `alias(libs.plugins.convention.kover)` line each.
- `CLAUDE.md` (a symlink to `AGENTS.md`): Commands and Tech Stack tables.
- Developer machines: `diffCoverage` needs `uv` on the `PATH` and a `git` checkout with the base ref present. `coverageReport` needs nothing new.
- Existing gates and test tasks keep their names, results and runtime (D17).
