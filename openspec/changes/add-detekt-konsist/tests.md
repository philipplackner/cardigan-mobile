# Test plan: add-detekt-konsist

<!-- Written by the tdd-test-designer agent from the change's planning
     artifacts only. Every test states where its expected result comes from.
     A test that asserts something no artifact states is a bug in this plan. -->

Scope (decisions.md, Workflow line): test-first applies to the sample custom
detekt rule only (`NoMockingLibraries` and the `cardigan` rule set that ships
it). Build setup, Gradle tasks, the Konsist check and the docs are `[plain]`
tasks and get no `A` or `C` tests here. The `cardigan` entry of
`config/detekt/detekt.yml` is exercised by the `A` tests, because D25 runs
them with that file.

Conventions for every test below:

- All tests are JVM tests in `tooling/src/test/kotlin/com/plcoding/cardiganapp/tooling/detekt/`,
  package `com.plcoding.cardiganapp.tooling.detekt`. They must stay in that
  package: `:tooling:test` excludes the `...tooling.konsist` package (design,
  `:tooling` module and Konsist), and `./gradlew :tooling:test` runs them (D21).
- Frameworks: `kotlin-test` on JUnit 5, AssertK (design, Public surface > Test levels).
- Parameterized cases follow the existing build-logic test style: a list of
  inputs checked in one test with `forEach`, each assertion carrying the input
  as its message, so a failing case names itself.
- "Finding at line N" means the finding's source location line. Lines are
  1-based and count the file content as written in the test, so build code
  strings with `trimIndent()` and a known line layout.
- The finding message is always the full text from D26:
  `Import of '<path>' is not allowed. Use fakes instead of mocking libraries.`

### Acceptance harness (all `A` tests)

Source: decisions.md D24, D25; design.md > Public surface > Test levels > Acceptance.

- Run real detekt in-process through `dev.detekt:detekt-core` (test-only
  dependency), with the `cardigan` rule set loaded the way detekt loads
  plugins (it is on the test classpath; registered through
  `META-INF/services/dev.detekt.api.RuleSetProvider`).
- Config: the repo's `config/detekt/detekt.yml`. Its absolute path comes
  from the system property `cardigan.detekt.config`, which the `test` task
  sets; when the property is missing, the test fails with a message naming
  it (design.md > `:tooling` module and Konsist; Public surface > Test
  levels > Acceptance). The config is built on detekt's default
  config as in the real setup (`buildUponDefaultConfig = true`). A06 uses its
  own config instead.
- Input: Kotlin files written to a JUnit temp directory laid out like source
  sets, for example `<tmp>/x/src/commonTest/kotlin/com/example/FooTest.kt`.
- Assertions look only at findings whose rule set id is `cardigan` and rule
  id is `NoMockingLibraries`. Findings of other default rules are ignored.
- `A` tests never reference the `NoMockingLibraries` or
  `CardiganRuleSetProvider` classes. They use only detekt's public API, the
  rule set id, the rule id and the config file.
- Every "no finding" test also puts one control file into the same run: a
  `commonTest` file with `import io.mockk.every`. It must produce exactly one
  finding, so a run where the rule never loaded cannot pass by accident. The
  control finding is excluded from the "no finding" assertion by its file path.

### Component harness (all `C` tests)

Source: design.md > Public surface > Test levels > Component.

- Create `NoMockingLibraries` with a config that activates it (detekt-test's
  `TestConfig` with `active` set to `true`) and call `lint` from
  `dev.detekt:detekt-test` on a code string. No type resolution.

## Coverage

| Spec > Requirement > Scenario | Tests |
|---|---|
| `static-analysis` > Mocking library imports are reported in test code > MockK import in a test | A01, C04 |
| `static-analysis` > Mocking library imports are reported in test code > Star and aliased imports | A02, C01 |
| `static-analysis` > Mocking library imports are reported in test code > Production code | A03 |
| `static-analysis` > Mocking library imports are reported in test code > Suppressed import | A04 |
| `static-analysis` > Mocking library imports are reported in test code (requirement text: "every import of a declaration in `org.mockito` or `io.mockk` or their subpackages") | C01, C04, C05 |
| `static-analysis` > Mocking rule message > Plain import message | A05 |
| `static-analysis` > Mocking rule message > Star import message | A05 |
| `static-analysis` > Mocking rule message > Aliased import message | A05 |
| `static-analysis` > Mocking rule message (requirement text: path "as written without an alias, including `.*` for star imports") | C06 |
| `static-analysis` > Mocking rule is off unless configured > Not configured | A06 |
| `static-analysis` > Mocking rule ignores everything but those imports > Fully qualified reference | A07, C03 |
| `static-analysis` > Mocking rule ignores everything but those imports > Similar package name | A08, C02 |
| `static-analysis` > Mocking rule ignores everything but those imports > Different letter case | A09 |
| design.md > Public surface > `CardiganRuleSetProvider` (rule set id `cardigan`, contains `NoMockingLibraries`, registered as a service) | A01 |
| `static-analysis` > One command runs detekt on the whole codebase > Whole codebase | not test-first (Workflow line) |
| `static-analysis` > One command runs detekt on the whole codebase > Module without the plugin | not test-first (Workflow line) |
| `static-analysis` > One module runs through its task path > Single module | not test-first (Workflow line) |
| `static-analysis` > detekt fails on any finding > Finding present | not test-first (Workflow line) |
| `static-analysis` > detekt fails on any finding > No findings | not test-first (Workflow line) |
| `static-analysis` > Production and test code are analyzed > Test code | not test-first (Workflow line) |
| `static-analysis` > Production and test code are analyzed > Generated code | not test-first (Workflow line) |
| `static-analysis` > Type resolution where the platform allows it > iOS-only file | not test-first (Workflow line) |
| `static-analysis` > Type resolution where the platform allows it > Shared file with a type-aware finding | not test-first (Workflow line) |
| `static-analysis` > Default rules with project overrides > Formatting is left to ktlint | not test-first (Workflow line) |
| `static-analysis` > build-logic uses default rules only > build-logic finding | not test-first (Workflow line) |
| `static-analysis` > HTML and SARIF reports > Reports after a run | not test-first (Workflow line) |
| `static-analysis` > Konsist checks run on demand > Konsist run | not test-first (Workflow line) |
| `static-analysis` > Domain code stays free of other layers and frameworks > Domain imports Ktor | not test-first (Workflow line); the check itself is written by the test-writer as a `[plain]` task (D30) |
| `static-analysis` > Domain code stays free of other layers and frameworks > Clean domain | not test-first (Workflow line); written by the test-writer as a `[plain]` task (D30) |
| `static-analysis` > One command runs both tools > detekt fails | not test-first (Workflow line) |
| `static-analysis` > Static analysis stays on demand > Gate run | not test-first (Workflow line) |
| `static-analysis` > Custom rule tests run separately > Rule tests | not test-first (Workflow line); the tests in this plan only need to sit in the `...tooling.detekt` package so that this task picks them up |

## Acceptance tests

<!-- `A` IDs. Black-box tests through the public interface only. -->

Level for every `A` test: JVM acceptance test in `:tooling` (`src/test`),
real detekt in-process with `config/detekt/detekt.yml` (D24, D25), using the
acceptance harness above.

### A01: a MockK import in any test source set is reported as `cardigan:NoMockingLibraries` at that import
- Source: specs/static-analysis/spec.md > Mocking library imports are reported in test code > MockK import in a test; design.md > D-config (`includes` lists the test source set paths); design.md > Public surface (`CardiganRuleSetProvider`, rule id)
- Level: acceptance (D24, D25)
- Arrange: one run per test source set path from D-config: `src/commonTest/kotlin`, `src/androidHostTest/kotlin`, `src/androidDeviceTest/kotlin`, `src/iosTest/kotlin`, `src/iosArm64Test/kotlin`, `src/iosSimulatorArm64Test/kotlin`, `src/test/kotlin`, each under `<tmp>/x/`. The file `com/example/FooTest.kt` has line 1 `package com.example`, line 2 empty, line 3 `import io.mockk.every`, line 4 empty, line 5 `class FooTest`.
- Act: run detekt on `<tmp>`.
- Expect: exactly one `cardigan` / `NoMockingLibraries` finding, in that file, at line 3.
- Must fail when: the service registration file is missing or names the wrong class (no `cardigan` rule set loads); the rule set id is `cardigan-rules`; the rule id is `ForbiddenMockingImport`; `detekt.yml` does not activate the rule or its `includes` misses a test source set (for example `**/androidHostTest/**` or `**/src/test/**` left out); the rule reports at line 1 instead of the import.

### A02: star and aliased Mockito imports in a test file are each reported
- Source: specs/static-analysis/spec.md > Mocking library imports are reported in test code > Star and aliased imports
- Level: acceptance (D24, D25)
- Arrange: `<tmp>/x/src/commonTest/kotlin/com/example/FooTest.kt` with line 1 `package com.example`, line 2 empty, line 3 `import org.mockito.*`, line 4 `import org.mockito.Mockito.mock as m`, line 5 empty, line 6 `class FooTest`.
- Act: run detekt on `<tmp>`.
- Expect: exactly two `NoMockingLibraries` findings, at lines 3 and 4.
- Must fail when: the rule requires the imported FQ name to start with `org.mockito.`, so the star import of the bare package `org.mockito` is missed; it reads the alias `m` instead of the imported name; or it reports once per file.

### A03: mocking imports in production source sets are not reported
- Source: specs/static-analysis/spec.md > Mocking library imports are reported in test code > Production code; decisions.md D18; design.md > D-config (`includes` only test paths)
- Level: acceptance (D24, D25)
- Arrange: one file per production source set, each `package com.example` / `import io.mockk.every` / `class Foo`: `<tmp>/x/src/commonMain/kotlin/com/example/Foo.kt`, `<tmp>/x/src/androidMain/kotlin/com/example/Foo.kt`, `<tmp>/x/src/iosMain/kotlin/com/example/Foo.kt`, `<tmp>/y/src/main/kotlin/com/example/Foo.kt`. Plus the control file.
- Act: run detekt on `<tmp>`.
- Expect: no `NoMockingLibraries` finding in any of the four production files; exactly one in the control file.
- Must fail when: `detekt.yml` has no `includes` for the rule or uses a pattern that also matches main code (for example `**/src/**`); the rule is applied to all analyzed code.

### A04: a file-level suppression silences the rule
- Source: specs/static-analysis/spec.md > Mocking library imports are reported in test code > Suppressed import; design.md > Public surface ("Standard detekt suppression ... applies")
- Level: acceptance (D24, D25)
- Arrange: `<tmp>/x/src/commonTest/kotlin/com/example/SuppressedTest.kt` with line 1 `@file:Suppress("NoMockingLibraries")`, line 2 empty, line 3 `package com.example`, line 4 empty, line 5 `import io.mockk.every`, line 6 empty, line 7 `class SuppressedTest`. Plus the control file.
- Act: run detekt on `<tmp>`.
- Expect: no `NoMockingLibraries` finding in `SuppressedTest.kt`; exactly one in the control file.
- Must fail when: the rule reports through a path that bypasses detekt's suppression check (for example it collects findings itself and reports them under a different rule id or entity than the import directive), or its id differs from `NoMockingLibraries` so the suppression name does not match.

### A05: the finding message names the import path as written, without alias
- Source: specs/static-analysis/spec.md > Mocking rule message > Plain import message, Star import message, Aliased import message; decisions.md D26, D27
- Level: acceptance (D24, D25)
- Arrange: one `commonTest` file per case, each `package com.example`, the import, `class FooTest`:
  - `import io.mockk.every` → `Import of 'io.mockk.every' is not allowed. Use fakes instead of mocking libraries.`
  - `import io.mockk.*` → `Import of 'io.mockk.*' is not allowed. Use fakes instead of mocking libraries.`
  - `import org.mockito.Mockito.mock as m` → `Import of 'org.mockito.Mockito.mock' is not allowed. Use fakes instead of mocking libraries.`
- Act: run detekt on `<tmp>`.
- Expect: each file has exactly one `NoMockingLibraries` finding whose message equals the text given for its case.
- Must fail when: the message names the package `io.mockk` for the star import (imported FQ name instead of the path as written); it includes ` as m`; it names the alias; or the fixed text differs from D26 (for example a missing period or "Use fakes instead.").

### A06: without configuration the rule reports nothing
- Source: specs/static-analysis/spec.md > Mocking rule is off unless configured > Not configured; decisions.md D29; design.md > Public surface ("the rule's default config is `active: false`")
- Level: acceptance (D24, D25), with a test-owned config instead of the repo config
- Arrange: a config file in the temp directory that does not mention the `cardigan` rule set (for example only `style:` overrides, or empty), with `buildUponDefaultConfig = true`, and the `cardigan` rule set still on the classpath. File `<tmp>/x/src/commonTest/kotlin/com/example/FooTest.kt` with `package com.example` / `import io.mockk.every` / `class FooTest`.
- Act: run detekt on `<tmp>` with that config.
- Expect: no `NoMockingLibraries` finding. As a check that the run itself worked, the same run with the repo's `config/detekt/detekt.yml` on the same file gives exactly one finding.
- Must fail when: the rule set's bundled default config sets `active: true` for `NoMockingLibraries`, or the rule set has no default config entry and detekt treats the rule as active.

### A07: a fully qualified MockK call without an import is not reported
- Source: specs/static-analysis/spec.md > Mocking rule ignores everything but those imports > Fully qualified reference
- Level: acceptance (D24, D25)
- Arrange: `<tmp>/x/src/commonTest/kotlin/com/example/FooTest.kt` with `package com.example`, no imports, `class Foo`, and `class FooTest { fun setUp() { val foo = io.mockk.mockk<Foo>() } }`. Plus the control file.
- Act: run detekt on `<tmp>`.
- Expect: no `NoMockingLibraries` finding in `FooTest.kt`; exactly one in the control file.
- Must fail when: the rule visits dot-qualified expressions or any text containing `io.mockk.` instead of only import directives.

### A08: packages that only share a name prefix are not reported
- Source: specs/static-analysis/spec.md > Mocking rule ignores everything but those imports > Similar package name
- Level: acceptance (D24, D25)
- Arrange: `<tmp>/x/src/commonTest/kotlin/com/example/FooTest.kt` with `package com.example`, `import io.mockkfake.Thing`, `import org.mockitox.Thing as OtherThing`, `class FooTest`. Plus the control file.
- Act: run detekt on `<tmp>`.
- Expect: no `NoMockingLibraries` finding in `FooTest.kt`; exactly one in the control file.
- Must fail when: the rule matches with `startsWith("io.mockk")` / `startsWith("org.mockito")` without the dot boundary.

### A09: imports that differ only in letter case are not reported
- Source: specs/static-analysis/spec.md > Mocking rule ignores everything but those imports > Different letter case; decisions.md D28
- Level: acceptance (D24, D25)
- Arrange: `<tmp>/x/src/commonTest/kotlin/com/example/FooTest.kt` with `package com.example`, `import Org.Mockito.Mockito`, `import IO.MockK.every`, `class FooTest`. Plus the control file.
- Act: run detekt on `<tmp>`.
- Expect: no `NoMockingLibraries` finding in `FooTest.kt`; exactly one in the control file.
- Must fail when: the rule lowercases the imported name or compares with `ignoreCase = true`.

## Component tests

<!-- `C` IDs. Tests against the public surface the design names. -->

Level for every `C` test: JVM unit test in `:tooling` (`src/test`),
`NoMockingLibraries` with an activating config, detekt-test `lint` (component
harness above).

### C01: every import of a declaration in `io.mockk` or `org.mockito` or a subpackage is reported, in all import forms
- Source: specs/static-analysis/spec.md > Mocking library imports are reported in test code (requirement text: "every import of a declaration in `org.mockito` or `io.mockk` or their subpackages, including star and aliased imports"); design.md > Public surface ("imported FQ name is `org.mockito` or `io.mockk`, or starts with `org.mockito.` or `io.mockk.`")
- Level: component (D24)
- Unit under test: `NoMockingLibraries(config)`
- Technique: equivalence classes (two library roots x exact package / direct member / deep subpackage x plain / star / aliased) plus break vectors (irregular whitespace, trailing semicolon, backticked segment)
- Arrange: one code string per case, each `package com.example`, then the single import below, then `class FooTest`:
  - `import io.mockk.*`
  - `import io.mockk.mockk as createMock`
  - `import io.mockk.impl.annotations.MockK`
  - `import org.mockito.Mockito`
  - `import org.mockito.kotlin.*`
  - `import org.mockito.kotlin.whenever as on`
  - `import  io.mockk.every` (two spaces after `import`)
  - `import io.mockk.every;` (trailing semicolon)
  - ``import io.`mockk`.every`` (backticked segment; the imported FQ name is `io.mockk.every`)
- Act: lint each code string with a new activated `NoMockingLibraries`.
- Expect: exactly one finding per case, at the line of the import.
- Must fail when: only one of the two roots is handled; only the exact root or only the `root.` prefix is checked; aliased imports are matched on the alias; the rule matches raw import text with a regex like `^import io\.mockk\.` (two spaces or backticks break it); or a subpackage depth limit is applied.

### C02: imports outside the two libraries are not reported
- Source: specs/static-analysis/spec.md > Mocking rule ignores everything but those imports > Similar package name; design.md > Public surface ("Everything else is not reported: other imports ... prefix-only matches")
- Level: component (D24)
- Unit under test: `NoMockingLibraries(config)`
- Technique: equivalence classes and break vectors (shorter root, suffix match, infix match, look-alike alias, look-alike last segment). Letter case is covered by A09.
- Arrange: one code string per case, `package com.example`, the import, `class FooTest`:
  - `import kotlin.test.Test`
  - `import assertk.assertThat`
  - `import io.mock.Thing`
  - `import io.mockk_extra.Thing`
  - `import com.example.io.mockk.Thing`
  - `import org.example.mockito.Thing`
  - `import com.example.FakeRepository as mockk`
  - `import com.example.testing.mockk`
- Act: lint each code string with a new activated `NoMockingLibraries`.
- Expect: no findings for every case.
- Must fail when: the rule uses `contains("io.mockk")` or `contains("mockito")`; it inspects the alias or the last segment instead of the imported FQ name; or it uses `startsWith("io.mock")`.

### C03: mentions of the libraries outside import directives are not reported
- Source: specs/static-analysis/spec.md > Mocking rule ignores everything but those imports > Fully qualified reference; design.md > Public surface ("It visits import directives"; "fully qualified references in code" are not reported)
- Level: component (D24)
- Unit under test: `NoMockingLibraries(config)`
- Technique: break vector (text that looks like a mocking import in places that are not imports)
- Arrange: one code string per case, no import directives:
  - fully qualified type: `package com.example` / `class FooTest { val mock: org.mockito.Mockito? = null }`
  - string literal: `package com.example` / `val text = "import io.mockk.every"`
  - line comment: `package com.example` / `// import io.mockk.every` / `class FooTest`
  - KDoc: `package com.example` / `/** Do not use [io.mockk.mockk]. */` / `class FooTest`
  - package directive: `package io.mockk.fakes` / `class FakeThing`
- Act: lint each code string with a new activated `NoMockingLibraries`.
- Expect: no findings for every case.
- Must fail when: the rule scans file text or lines for `import io.mockk` / `org.mockito`; it visits qualified type references; or it treats the package directive as an import.

### C04: findings land on the mocking imports only, in a file with mixed imports
- Source: design.md > Public surface ("For each import whose imported FQ name ... it reports one finding at that import directive"); specs/static-analysis/spec.md > Mocking library imports are reported in test code > MockK import in a test ("at that import")
- Level: component (D24)
- Unit under test: `NoMockingLibraries(config)`
- Technique: equivalence classes for count (none, one, many) and location
- Arrange: line 1 `package com.example`, line 2 empty, line 3 `import kotlin.test.Test`, line 4 `import io.mockk.every`, line 5 `import assertk.assertThat`, line 6 `import org.mockito.Mockito.verify`, line 7 `import kotlin.test.assertEquals`, line 8 empty, line 9 `class FooTest`.
- Act: lint with an activated `NoMockingLibraries`.
- Expect: exactly two findings, at lines 4 and 6.
- Must fail when: the rule reports once at the import list or the file (one finding at line 3 or line 1); it reports every import once one matches (five findings); or it stops after the first match (one finding).

### C05: the same mocking import twice gives two findings
- Source: design.md > Public surface ("For each import ... it reports one finding at that import directive")
- Level: component (D24)
- Unit under test: `NoMockingLibraries(config)`
- Technique: break vector: duplicates
- Arrange: line 1 `package com.example`, line 2 empty, line 3 `import io.mockk.every`, line 4 `import io.mockk.every`, line 5 empty, line 6 `class FooTest`.
- Act: lint with an activated `NoMockingLibraries`.
- Expect: exactly two findings, at lines 3 and 4.
- Must fail when: the rule collects matching names into a set and reports each distinct name once.

### C06: the message path keeps deep star imports and drops aliases of deep members
- Source: specs/static-analysis/spec.md > Mocking rule message (requirement text: "`<path>` is the import path as written without an alias, including `.*` for star imports"); decisions.md D26, D27
- Level: component (D24)
- Unit under test: `NoMockingLibraries(config)`
- Technique: equivalence classes beyond the three spec examples (star import of a subpackage, aliased deep member, Mockito root star)
- Arrange: one code string per case, `package com.example`, the import, `class FooTest`:
  - `import org.mockito.kotlin.*` → `Import of 'org.mockito.kotlin.*' is not allowed. Use fakes instead of mocking libraries.`
  - `import org.mockito.kotlin.whenever as on` → `Import of 'org.mockito.kotlin.whenever' is not allowed. Use fakes instead of mocking libraries.`
  - `import org.mockito.*` → `Import of 'org.mockito.*' is not allowed. Use fakes instead of mocking libraries.`
- Act: lint each code string with an activated `NoMockingLibraries`.
- Expect: the single finding's message equals the text given for its case.
- Must fail when: the star suffix is added only for the exact roots (`io.mockk.*` handled as a special case, `org.mockito.kotlin.*` printed as `org.mockito.kotlin`); or the alias is stripped by cutting at the last `.` instead of dropping the `as` clause (`org.mockito.kotlin` printed).
