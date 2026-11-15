# Decisions: add-design-system

Confirmed with the user via the orchestrator session on 2026-11-15. The questions were sent to the orchestrator session (cardiganapp-e9), which bundled them with the web client's questions and relayed the user's answers. The user agreed to record them this way.

## Summary

Build the mobile design system in `:core:designsystem` in one change: theme and tokens from the Figma Foundations page, Geist fonts, the 27 Figma icons, window size classes, and every component on the Figma Design System page except the web-only parts. Components follow Figma's visual sizes and get 48dp touch areas, theme-tinted press ripples, and previews for every state in light and dark. No component tests and no sample screen in this change.

## Decisions

### D1 — Scope and split
- **Question**: Should foundations (theme, tokens, type, fonts, icons, window classes) and components be one change or two?
- **Decision**: One change with foundations plus all components.
- **Alternatives**: Two changes (foundations, then components); two changes with single-feature components built in their feature changes.
- **Why**: User choice.
- **Depends on**: —

### D2 — Web-only parts
- **Question**: Build the Drop zone component and the Board tile Hover state?
- **Decision**: No. Both are left out.
- **Alternatives**: Build them anyway.
- **Why**: Drop zone is annotated as the web import target. The app imports through the system file picker.
- **Depends on**: D1

### D3 — Press, hover and highlight feedback
- **Question**: How do components show press, hover and keyboard highlight, given Figma has no pressed state?
- **Decision**: A ripple tinted from theme colors on press. The Figma "Highlighted" / "Hover" look is shown for pointer hover and keyboard focus (tablets with trackpad or keyboard).
- **Alternatives**: Show "Highlighted" while pressed with no ripple; no press feedback.
- **Why**: Standard on both platforms and doesn't change any designed look.
- **Depends on**: —

### D4 — Touch targets
- **Question**: How do Figma's visual sizes (phone 38–46, tablet 28–36) meet the 48dp touch target rule from CLAUDE.md?
- **Decision**: Keep the Figma visual size and extend the hit area invisibly to at least 48dp. When neighboring hit areas overlap, the nearest element wins.
- **Alternatives**: Grow the visuals to 48dp; follow Figma literally and drop the rule.
- **Why**: Keeps the design and meets the accessibility rule.
- **Depends on**: —

### D5 — Pull-to-refresh indicator
- **Question**: What does the app-only pull-to-refresh look like, since the Design System page has no indicator?
- **Decision**: The Material pull-to-refresh indicator, recolored with theme tokens.
- **Alternatives**: The design system Spinner in a custom pull container; ask for a design first.
- **Why**: Platform pattern; only the colors change.
- **Depends on**: —

### D6 — Tests
- **Question**: Which tests cover the design system components?
- **Decision**: Previews only. No `runComposeUiTest` tests for components in this change.
- **Alternatives**: Previews plus UI tests for behavior and semantics; plus screenshot tests.
- **Why**: User choice.
- **Depends on**: —

### D7 — Sample screen
- **Question**: Is there a sample or gallery screen?
- **Decision**: No. Previews are checked against the Figma frames.
- **Alternatives**: A debug-only component gallery in `:composeApp`.
- **Why**: A gallery would be UI that isn't in Figma.
- **Depends on**: D6

### D8 — Font files
- **Question**: Where do the Geist and Geist Mono files come from?
- **Decision**: Static TTFs from the official vercel/geist-font GitHub release: Geist 400/500/600/700 and Geist Mono 400/500/600, plus the SIL OFL 1.1 license file. The download is approved.
- **Alternatives**: Variable font files.
- **Why**: Static weights render the same on both platforms.
- **Depends on**: —

### D9 — Technical choices (no objection raised)
- **Question**: Proposed by the mobile session, passed on to the user without objection.
- **Decision**: Components choose the Default or Touch size from the window class through a CompositionLocal. The design system has its own `LabelColor` palette with the 20 shared-spec color names, and features map to it. Shadows use Compose `dropShadow`. Text fields are built on `BasicTextField`, because Material text fields enforce a 56dp minimum height. Material 3 is the base everywhere else.
- **Alternatives**: —
- **Why**: Size variants follow the screen size, as in Figma. The palette keeps `:core:designsystem` independent of feature modules.
- **Depends on**: D4

### D10 — Token coverage and source
- **Question**: Which tokens does the code mirror, and where do they come from?
- **Decision**: All Figma variables and effect styles, 1:1, hand-ported, named after the Figma variables: 53 colors (light and dark), 23 spacing steps, 11 radii, 12 sizes (8 `height/*` plus the 4 `width/*` variables from D23), 7 effects. The web client does the same.
- **Alternatives**: Only the tokens the Foundations page lists; generate code from an exported token file.
- **Why**: User choice. Keeps both clients aligned with Figma. The Foundations page is brought up to date in the separate change `update-figma-foundations`.
- **Depends on**: —

### D11 — Text styles
- **Question**: Mirror all Figma text styles, or only the Foundations roles?
- **Decision**: All ~59 Figma text styles 1:1, plus the 7 Foundations roles (Page title, Screen title, Card title, Body, Label, ID & metadata, Section label) as semantic styles. The semantic styles switch to their phone size in the phone class.
- **Alternatives**: Only the 7 roles; only the raw styles.
- **Why**: User choice.
- **Depends on**: D12

### D12 — Window classes, landscape phones and Touch sizes
- **Question**: How is the window classified, including phones in landscape, and when are Touch sizes used?
- **Decision**: By width: phone < 600dp, tablet 600–839dp, desktop ≥ 840dp. A window under 480dp high is always phone. Touch sizes are used only in the phone class. There is no rule based on touch input for tablets. The app keeps its own breakpoints in dp; the web's 640/1024px are separate.
- **Alternatives**: Width only; asking for landscape-phone designs.
- **Why**: The desktop layout doesn't fit a 390dp-high window. The web does the same height rule.
- **Depends on**: —

### D13 — Toast timing
- **Question**: How long do toasts stay, and how do several toasts behave?
- **Decision**: Success toasts close 4 seconds (±0.5s) after they appear, or later when Android's accessibility timeout ("Time to take action") asks for a longer one. Error toasts stay until the user closes them. One toast at a time, and a new toast replaces the one showing. Toasts sit at the bottom of the screen; the horizontal position follows the tablet and desktop frames.
- **Alternatives**: All toasts ~4s; a queue.
- **Why**: User choice.
- **Depends on**: —

### D14 — Skeleton animation
- **Question**: Static, shimmer or pulse?
- **Decision**: Shimmer from `background/skeleton` to `background/skeleton-highlight`, about a 1.2 second loop. Static `background/skeleton` when the OS asks for reduced motion.
- **Alternatives**: Static; pulse.
- **Why**: The highlight token implies a shimmer. Both clients match.
- **Depends on**: —

### D15 — Danger hover
- **Question**: Do danger buttons get a hover state?
- **Decision**: No hover change for danger buttons and no new token.
- **Alternatives**: A new danger hover token.
- **Why**: User choice.
- **Depends on**: D3

### D16 — Large system font sizes
- **Question**: How do components behave when the system font size is large?
- **Decision**: Text scales with the system font. Figma control heights become minimum heights, so controls grow to fit. Single-line labels ellipsize horizontally. The web does the same for browser text size.
- **Alternatives**: Cap scaling at about 130%; no scaling.
- **Why**: Keeps the accessibility setting and matches the design at 100%.
- **Depends on**: D4

### D17 — One phone condition
- **Question**: Does the 480dp height rule also give Touch sizes?
- **Decision**: Yes. One phone condition (width under 600dp or height under 480dp) drives both the phone layout and Touch sizes. Landscape phones get Touch sizes.
- **Alternatives**: Touch sizes by width only.
- **Why**: User choice. Shared with web.
- **Depends on**: D12

### D18 — Icon scope
- **Question**: Which icons does this change export?
- **Decision**: All 27 Figma icons.
- **Alternatives**: Only the icons the components use; add icons per feature.
- **Why**: User choice. This overrides the CLAUDE.md default of adding an icon when a feature first needs it.
- **Depends on**: —

### D19 — Text area growth
- **Question**: Do text areas grow with their content?
- **Decision**: Yes. Title and Multi-line grow without a maximum. Description grows until the panel body scrolls.
- **Alternatives**: Fixed height with inner scrolling; a maximum number of lines.
- **Why**: User choice. Shared with web.
- **Depends on**: —

### D20 — Progress dialog
- **Question**: Can the Progress dialog be dismissed?
- **Decision**: No. Back, Escape, a tap on the scrim and any close control do nothing while it shows.
- **Alternatives**: Dismissible like other dialogs.
- **Why**: User choice. Shared with web.
- **Depends on**: —

### D21 — Single-feature components and drag visuals
- **Question**: Recorded by web and applied to the app as well (no objection from the mobile session).
- **Decision**: Components used by only one feature also live in `:core:designsystem` and take plain props, never domain objects. Drag & drop visuals (Card Dragging tilt with `Shadow/Drag`, Placeholder, Drop indicator) are prop-driven visual states only. The board view change wires the actual dragging.
- **Alternatives**: Single-feature components in their feature module.
- **Why**: CLAUDE.md asks for every Design System component in `:core:designsystem`, and this keeps it free of feature dependencies.
- **Depends on**: D1, D9

### D22 — Verification scope
- **Question**: Does "previews only" (D6) cover pure logic, and are on-device checks part of the tasks?
- **Decision**: The pure window-class function gets a unit test. Components are verified by previews only: no temporary preview-host screens and no on-device checks in this change. The risk that the 48dp touch area doesn't extend on iOS stays open in design.md.
- **Alternatives**: No tests at all; temporary on-device checks that are removed before commit.
- **Why**: User choice.
- **Depends on**: D6, D7

### D23 — Layout widths
- **Question**: How do the sidebar widths (224, 248) and side panel widths (420, 452) get into code, given they have no Figma variables?
- **Decision**: As theme tokens named after the Figma Size variables that update-figma-foundations adds: `width/sidebar-tablet` (224), `width/sidebar-desktop` (248), `width/panel-tablet` (420), `width/panel-desktop` (452). In code: `AppTheme.width.sidebarTablet` and so on.
- **Alternatives**: A token block citing only the breakpoint table; hard-coded in the screens that use them.
- **Why**: The user chose to add real Figma variables, so the code follows the same naming as every other token. Web uses the same names. update-figma-foundations is applied first, and this change stops if the variables are missing (no fallback to the breakpoint table).
- **Depends on**: D10

### D24 — No button hover
- **Question**: Should primary buttons use `accent/hover` on pointer hover, as the web client does?
- **Decision**: No. Buttons show only the ripple on press. Hover stays limited to components with a Highlighted state (D3). The app doesn't use `accent/hover` for now; the token is still mirrored (D10).
- **Alternatives**: Primary button hover with `accent/hover` for trackpad and mouse users.
- **Why**: User choice. Touch devices already get press feedback from the ripple.
- **Depends on**: D3, D15

### D25 — Previews aren't rendered by the agent
- **Question**: How are the `commonMain` previews checked against Figma, given `android studio render-compose-preview` can't render them?
- **Decision**: Previews stay in `commonMain` next to their components and are for the user to look at in the IDE. The agent doesn't render them with the Android CLI or the `android-preview-figma-verify` skill. It checks each component against its Figma variant in code (`get_design_context` values: tokens, sizes, spacing, text, icons, states). The Material `Typography` that task 1.4 names is built in 2.2, once the text styles exist.
- **Alternatives**: Move previews to `androidMain` so the Android CLI can render them.
- **Why**: User choice, 2026-11-15, after rendering failed during apply. KMP previews in `commonMain` aren't renderable by the Android CLI, and the user doesn't want previews in `androidMain`.
- **Depends on**: D6, D7, D22

### D26 — Dark `text/on-accent` is white
- **Question**: Which text color goes on `accent/default` in the dark theme?
- **Decision**: White (`#FFFFFF`), not Figma's `#0B0E16`. It's the token, so it applies to everything on an accent fill: Primary button text and ripple, the Checkbox check mark, Icon button badges and the Filter button count.
- **Alternatives**: Keep Figma's `#0B0E16`. Change only the Primary button.
- **Why**: User choice, 2026-11-15, after reviewing the dark Primary button. The Figma variable still says `#0B0E16` and should be updated there.
- **Depends on**: D10

## Facts

- Foundations lists the Page title phone size as "26–28". The frames use 28 (`Sans/28 SemiBold/120% · -2% LS`) for the phone auth titles. 26 (`Sans/26 SemiBold/110% · -2.5% LS`) is only the large "Boards" title on the phone board list — source: Figma pages `9:4` and `9:5`, text style usage
- The Toast component has a close icon in both types — source: Figma component `12:676`

- Phone frames use the Touch size variants; tablet and desktop frames use Default — source: Figma Board list page `9:5`, instance variant count
- Figma variables: Color (Light/Dark, 53), Spacing (23), Radius (11), Size (8); about 60 text styles; effect styles Shadow/{Small, Popover, Drag, Panel, Raised} and Focus/{Accent, Danger} — source: Figma file `MALwbOph7Uj2QtiTy21JaQ`
- The 20 label colors in Figma match the color names in the shared spec — source: cardigan-shared-specs `domain-label`, "Label color is a palette name"
- Board keys are 2 to 5 letters A–Z — source: cardigan-shared-specs `domain-board`, "Board key format"
- `~/cardigan-specs` is a stale checkout with a different design (other fonts, 8 label colors). The registered store and Figma agree with each other — source: `openspec list --specs --store cardigan-shared-specs`
- Breakpoints (proposal): phone < 600dp, tablet 600–839dp, desktop / tablet landscape ≥ 840dp — source: Figma Foundations `9:2`

## Out of scope

- Drop zone component and Board tile Hover state (D2)
- Component UI tests and screenshot tests (D6)
- Sample or gallery screen (D7)
- Feature screens, navigation and drag & drop behavior. The Card's Dragging and Placeholder looks and the Drop indicator are built only as visual states.

## Deferred

- (none)
