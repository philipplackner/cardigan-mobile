# Proposal

## Why

Every screen in Figma is built from Design System instances, and `:core:designsystem` is still empty: no theme, no fonts, no icons, no components. The feature work for auth, boards, cards, labels and transfer can't start until the shared look exists in code.

## What Changes

- `AppTheme` in `:core:designsystem` with the Figma Color variables (light and dark, following the system setting), mapped to a custom Material 3 color scheme plus extended colors. Includes spacing, radius, size, shadow and focus-ring tokens.
- Typography in Geist and Geist Mono, bundled as compose resources with the OFL license, including the phone-specific sizes from Foundations.
- The 27 Figma icons as vector drawables on the project icon object, tinted by their context.
- Window size classes from the Foundations breakpoints, defined in one place. Components pick their Default or Touch size from them.
- The 20-color label palette, keyed by the color names in the shared `domain-label` spec.
- Every component on the Figma Design System page except the web-only Drop zone and Board tile Hover state: brand, feedback, labels, data display, inputs, overlays, board, actions, navigation, boards, card detail panel and file row.
- Pull-to-refresh indicator in theme colors.
- Touch areas of at least 48dp on every interactive component without changing its visual size, a theme-tinted press ripple, and the Highlighted/Hover look for pointer hover and keyboard focus.
- A preview for every distinct state of every component, in light and dark.

## Capabilities

### New Capabilities
- `design-system`: how the app's shared UI looks and behaves across themes, window sizes and input methods. Covers theming, window classes, touch targets, interaction feedback, and the mobile-specific behavior of the shared components.

### Modified Capabilities
- (none)

### Shared-spec requirements

This change implements or depends on:
- cardigan-shared-specs `domain-label`, "Label color is a palette name": the 20 palette names the label palette is keyed by.
- cardigan-shared-specs `domain-board`, "Board key format": the Key badge shows a key of 2 to 5 uppercase letters.

## Impact

- `:core:designsystem`: new theme, tokens, fonts, icons, window classes and components. Window classes are computed from the window size Compose already provides, so no new library is needed.
- `core/designsystem/src/commonMain/composeResources/`: font files, OFL license, icon vector drawables.
- No feature, navigation, data or API changes. `App()` stays empty.
- No new dependencies.
- Order: apply `update-figma-foundations` first. It creates the `width/*` Figma variables this change's width tokens are named after.
- Tests: none beyond previews in this change, by decision.
