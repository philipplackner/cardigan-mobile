# Proposal

## Why

The mobile code has no linter or formatter. Style depends on each developer's IDE settings. The user wants commands to check and auto-format Kotlin code, set up the same way as in the backend, without enforcing them yet.

## What Changes

- **ktlint** (new dependency, D2, D17): the ktlint-gradle plugin 14.2.0 with ktlint 1.8.0, both pinned in the version catalog.
- **Rule settings** (D4, D5, D8, D11, D12): a new root `.editorconfig` with a Kotlin section only: `ktlint_official`, max line length 120, `@Composable` functions exempt from the function-naming rule, and matching `ij_kotlin_*` keys for the IDE formatter. No custom rules.
- **Module convention plugin** `com.plcoding.cardiganapp.convention.ktlint` (D13): opt-in per module. All 20 KMP modules, `:androidApp` and the root project apply it.
- **build-logic** (D14): applies ktlint-gradle directly. The root `ktlintCheck` and `ktlintFormat` also run build-logic's tasks.
- **Commands** (D3, D15): `./gradlew ktlintCheck` reports violations in the console and writes SARIF and Checkstyle reports under `build/reports`. `./gradlew ktlintFormat` fixes what it can. With `--continue`, both cover every module in one run instead of stopping at the first failing task.
- **Scope** (D6): main and test Kotlin in every module, `*.gradle.kts` scripts, and build-logic. `build/` and generated code are left out.
- **Not enforced** (D7): not a gate, not wired into `check`, no git hook, no CI, nothing formats automatically.
- **One-time reformat** (D9, D10): in its own commit, until `ktlintCheck` reports zero violations. Rule clashes go to the user; unfixable violations on client-visible names are suppressed at the spot.
- **Docs** (D16): CLAUDE.md's "Lint / format" row and Commands table, marked not a gate.

## Shared-spec requirements

None. This change adds local developer tooling only and implements or depends on no shared requirement. The backend gets the same setup in its own change (`be-ktlint`).

## Capabilities

### New Capabilities
- `code-style`: checking and auto-formatting the mobile Kotlin code against the project's ktlint rule settings from the terminal.

### Modified Capabilities

None.

## Impact

- `gradle/libs.versions.toml`: ktlint and ktlint-gradle versions, the plugin and its Gradle plugin library, the `convention-ktlint` alias.
- `build-logic/convention`: the new convention plugin; build-logic's own build files apply ktlint-gradle.
- Root `build.gradle.kts` and all 21 module build files: one plugin line each.
- New root `.editorconfig`.
- Every Kotlin source and Gradle script may change in the reformat commit. Formatting only, no behavior change; all gates must still pass.
- `CLAUDE.md`: docs.
- No effect on the gates, `check` or app behavior.
