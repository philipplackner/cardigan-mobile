## MODIFIED Requirements

### Requirement: Excluded code
The reports SHALL leave out every `@Composable` function, previews included. They SHALL also leave out generated code, Koin DI module definitions, debug-only fixtures, androidMain `actual` implementations, design tokens in the design system theme package, and navigation routes and graphs. These exclusions SHALL be matched by annotation or name pattern, so new code that follows the project layout is excluded without a build change. Window size logic in the theme package SHALL stay in. Here that means `WindowClassKt` only. The whole `AppThemeKt`, `TypographyKt`, `ShadowsKt` and `TextStylesKt` classes, including the functions that build tokens from the window class (`appTypography`) or the theme (`toColorScheme`, `appShadows`), are left out.

#### Scenario: Composable function is excluded
- **WHEN** a selected module contains a `@Composable` screen and a ViewModel
- **THEN** the report lists the ViewModel and does not list the screen function

#### Scenario: Generated and wiring code is excluded
- **WHEN** a report is generated for all modules
- **THEN** it lists no BuildKonfig class, no Compose resources (`Res`) class, no class in a `di` or `fixture` package, and no class whose name matches `*Debug*Overlay*` (today `DebugSessionOverlay`)

#### Scenario: androidMain actual is excluded
- **WHEN** a report is generated for `:core:domain`
- **THEN** the Android `actual` of `toNfc` is not listed, and the common code that calls it is

#### Scenario: Design tokens are excluded
- **WHEN** a report is generated for `:core:designsystem`
- **THEN** it lists no color, typography, text style, shadow or `App*` dimension token class from the theme package
- **AND** it still lists `WindowClassKt`
- **AND** it does not list `TypographyKt` or `AppThemeKt`

#### Scenario: Navigation wiring is excluded
- **WHEN** a report is generated for all modules
- **THEN** it lists no `*Route` class and no `*GraphKt`, `*NavigationKt` or `*NavHostKt` class in a `navigation` package
- **AND** it still lists `SessionGateKt` and `AppShellKt`, which are outside `navigation` packages

#### Scenario: Mutation-only exclusions stay measured
- **WHEN** a report is generated for all modules
- **THEN** inline functions such as `safeCall`, the NavController helpers in `AppShellKt`, `UiText.asStringAsync` and `HttpClientFactory` are still measured
