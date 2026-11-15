# Tasks

Gates referenced below: `./gradlew :androidApp:assembleDebug testAndroidHostTest iosSimulatorArm64Test :composeApp:linkDebugFrameworkIosSimulatorArm64`. This change doesn't touch `iosApp/`.

Figma workflow for every component task: `create_session` on `MALwbOph7Uj2QtiTy21JaQ`, load `figma:figma-design-to-code`, `get_design_context` on the component set on page `9:3`, check with `get_screenshot`. If a Figma call fails, stop and report the call and node (CLAUDE.md). "Preview check" means the component code is compared with its Figma variant from `get_design_context` (tokens, sizes, spacing, text, icons, states), and no high-severity difference is left. Previews stay in `commonMain` for the user to look at. They are never rendered with the Android CLI or `android-preview-figma-verify` (D25).

## 1. Tokens and theme

- [x] 1.1 Dump all Color (light and dark), Spacing, Radius and Size variables, the 7 effect styles and all text styles from Figma with one read-only `use_figma` call, and save the dump in the scratchpad. Verify: the dump has 53 colors with two modes, 23 spacing, 11 radius, 12 size (8 `height/*` and the 4 `width/*` from update-figma-foundations), 7 effects and every text style.
- [x] 1.2 Add the color tokens (nested groups after the Figma paths, light and dark instances), spacing, radius, height and shadow tokens per design "Theme structure", plus the width tokens `width.sidebarTablet`, `sidebarDesktop`, `panelTablet`, `panelDesktop` from the Figma `width/*` variables (D23). If the dump from 1.1 doesn't contain all four `width/*` variables, stop and report that update-figma-foundations hasn't been applied. Don't fall back to the breakpoint table. Verify: `AppTheme.height` plus `AppTheme.width` together match the 12 Size variables, the other counts match the dump from 1.1, and a spot check of 5 colors per mode against Figma matches.
- [x] 1.3 Add the window class (`LocalWindowClass`, one phone condition: width < 600dp or height < 480dp; tablet 600–839dp; desktop ≥ 840dp) with the breakpoints as constants in one file. Add a pure function from width and height to class. List its break vectors (each boundary value on both sides, height rule overriding width, zero size) and unit test it in `commonTest`. Verify: `./gradlew :core:designsystem:testAndroidHostTest :core:designsystem:iosSimulatorArm64Test` passes. This is the only test in the change; it covers logic, not a component (D6).
- [x] 1.4 Add `AppTheme`: `isSystemInDarkTheme()` picks the colors, a Material 3 `ColorScheme` and `Typography` built from the tokens, the CompositionLocals, `LocalMinimumInteractiveComponentSize = Dp.Unspecified`, `LocalRippleConfiguration` with `text/primary`, and an optional `windowClass` parameter that overrides the measured class for `LocalWindowClass` and typography. Verify: a preview of a plain surface with text exists in light and dark, and the color scheme maps only to Figma tokens. The Material `Typography` is built in 2.2, once the text styles exist.
- [x] 1.5 Run all gates. Verify they pass, then commit `feat(designsystem): add theme tokens and window classes`.

## 2. Fonts and type

- [x] 2.1 Download the static TTFs from the official vercel/geist-font GitHub release (Geist 400/500/600/700, Geist Mono 400/500/600) and the OFL license into a new empty directory in the scratchpad. Copy the fonts to `core/designsystem/src/commonMain/composeResources/font/` and the license to `composeResources/files/licenses/`. Verify: 7 font files and the license exist, they come from the release assets of `vercel/geist-font` at a named tag, and the release tag and the SHA-256 of each file are noted in the commit message.
- [x] 2.2 Add all Figma text styles as `AppTheme.textStyles` (sp sizes, sp line heights, em letter spacing, caps where Figma says Caps) and the 7 semantic roles as `AppTheme.typography` with the phone switch for Page title (28), Card title (14.5) and Body (15). Verify: the style count matches the dump from 1.1, the Material `Typography` from 1.4 is built from them, and a type specimen preview at `widthDp = 390` and `834` exists whose styles match the Figma Type section on `9:2` in code.
- [x] 2.3 Run all gates. Verify they pass, then commit `feat(designsystem): add geist fonts and text styles`.

## 3. Icons and brand

- [x] 3.1 Export all 27 icons from the Icons section of `9:3` as SVG and convert them to vector drawables with `figma-to-android-vector`, with hard-coded colors replaced so `tint` applies. Add them to `CardiganIcons`. Verify: all 27 are on `CardiganIcons` with no hard-coded colors left, and a preview grid of all 27, tinted with `text/primary`, exists in light and dark.
- [x] 3.2 Add Logo (drawn in code) and Brand (Sidebar, App bar, Auth). Verify: previews per size in light and dark, then the preview check.
- [x] 3.3 Run all gates. Verify they pass, then commit `feat(designsystem): add icons and brand`.

## 4. Interaction base

- [x] 4.1 Add the shared interaction helpers: hover-or-focus state from an `InteractionSource`, the focus ring modifier (`Focus/Accent`, `Focus/Danger`), and the size mapping helper (semantic size → Touch counterpart in the phone class). Containers that hold components smaller than 48dp draw their shape with `background`/`border` and don't clip their content (design "Touch area"); this applies to every later group. Verify: used in group 5, and `:core:designsystem` compiles for Android and iOS.

## 5. Actions

- [x] 5.1 Button: 5 styles × Small/Medium (Touch small/Touch in the phone class) × Default/Disabled, optional leading icon, loading spinner. Disabled and loading aren't clickable. Danger styles have no hover change. Primary and Danger provide a `text/on-accent` ripple. Verify: previews for every style × state × class, the loading state, and font scale 2×, then the preview check.
- [x] 5.2 Icon button: Ghost/Boxed × Default/Selected × Small/Medium/Large, optional badge, content description required. Filter button: Default/Active with count. Verify: previews per variant, then the preview check.
- [x] 5.3 Run all gates. Verify they pass, then commit `feat(designsystem): add buttons`.

## 6. Inputs

- [x] 6.1 Input on `BasicTextField`: Default/Focus/Error/Read-only/Disabled × Default/Touch × Value/Placeholder, leading and trailing icon, focus ring. Verify: previews for every state × size × content, then the preview check.
- [x] 6.2 Text area: Multi-line/Title/Description × Default/Focus/Error × Default/Touch, growing with content and no `maxLines`. Verify: previews per variant plus a 5-line preview, then the preview check.
- [x] 6.3 Checkbox, Password rule (Met true/false), Error message, Field (label above, Control slot, Default/Touch), Option (Default/Highlighted on hover/focus, Default/Touch). Verify: previews per variant, then the preview check.
- [x] 6.4 Run all gates. Verify they pass, then commit `feat(designsystem): add inputs`.

## 7. Feedback

- [x] 7.1 Spinner (Small/Large). Skeleton with the 1.2s shimmer and `expect`/`actual` `rememberReduceMotion()` (Android animator duration scale, iOS `UIAccessibilityIsReduceMotionEnabled`). Verify: previews of the shimmering and the reduced-motion (static) skeleton, then the preview check.
- [x] 7.2 Banner (Error/Info), Empty state (Default/Error, Actions slot). Verify: previews per variant, then the preview check.
- [x] 7.3 Toast look (Success/Error with close icon) and `ToastHostState`: replace-current, success 4s (Android lengthened by the accessibility recommended timeout), error indefinite. Read the toast's horizontal position on tablet and desktop from the screen frames first. Verify: previews per type, then the preview check.
- [x] 7.4 Pull-to-refresh indicator: `PullToRefreshBox` with `PullToRefreshDefaults.Indicator`, container `background/surface`, spinner `accent/default`. Verify: a preview of the indicator in light and dark.
- [x] 7.5 Run all gates. Verify they pass, then commit `feat(designsystem): add feedback components`.

## 8. Overlays

- [x] 8.1 Menu divider, Menu item (Default/Highlighted on hover/focus/Danger/Disabled/Header × Default/Touch), Menu on `DropdownMenu`. Verify: previews per variant, then the preview check.
- [x] 8.2 Popover on `Popup` with `Shadow/Popover`. Dialog (Default/Progress × Default/Touch) on `BasicAlertDialog`; Progress ignores back, Escape and scrim taps. Verify: previews per variant, then the preview check.
- [x] 8.3 Bottom sheet on `ModalBottomSheet` with every visible Material default overridden. Verify: preview, then the preview check.
- [x] 8.4 Run all gates. Verify they pass, then commit `feat(designsystem): add menus, dialogs and sheets`.

## 9. Labels and data display

- [x] 9.1 `LabelColor` with the 20 shared-spec names mapped to `colors.label.*`. Label dot, Label (Small/Medium/Large), Label chip (Removable), Swatch (Selected), Label row (Default/Touch, divider). Verify: a preview of all 20 dots in light and dark, previews per component variant, then the preview check.
- [x] 9.2 Key (2–5 uppercase letters) and Avatar. Verify: previews with a 2-letter and a 5-letter key, then the preview check.
- [ ] 9.3 Run all gates. Verify they pass, then commit `feat(designsystem): add labels, key and avatar`.

## 10. Board, boards, card detail and transfer

- [ ] 10.1 Card (Default/Selected/Dragging/Placeholder × Default/Touch, labels slot; Dragging with 2.2° tilt and `Shadow/Drag`), Drop indicator, Add card button (Default/Touch), Add column button, Column (Default/Touch, Cards slot). Plain props only. Verify: previews per variant, then the preview check.
- [ ] 10.2 Board tile (Default only, no Hover) and Board row (divider). Verify: previews, then the preview check.
- [ ] 10.3 Panel (Body slot scrolls, Footer slot) and File row. Verify: previews including a long description in the body, then the preview check.
- [ ] 10.4 Run all gates. Verify they pass, then commit `feat(designsystem): add board, card and transfer components`.

## 11. Navigation

- [ ] 11.1 Nav item (Default/Selected/Dim, key), Sidebar (Boards slot), Page header (Actions slot, key, meta), App bar (Brand/Title/Centered × Raised). Verify: previews per variant, then the preview check.
- [ ] 11.2 Run all gates. Verify they pass, then commit `feat(designsystem): add navigation components`.

## 12. Integration check

- [ ] 12.1 Go through every component on page `9:3` (except Drop zone) and confirm it has a composable and a preview per Figma variant. Run `grep -rn "Color(0x" core/designsystem/src/commonMain/kotlin --include=*.kt` and confirm hex values appear only in the token files. Verify: a checklist with no gaps, and the grep result.
