# Design

## Context

`:core:designsystem` applies the `cmp.library` convention plugin and is empty. That plugin already brings Compose runtime, foundation, ui, Material 3 (`1.12.0-alpha03`), compose resources and preview tooling, so this change needs no new dependency. Feature presentation modules get `:core:designsystem` through `cmp.feature`.

Figma is the source for every value (see decisions.md D10, D11). The Design System page (`9:3`) has 71 top-level components: 27 icons and 44 others (27 component sets and 17 single components). The phone frames use their Touch variants, the tablet and desktop frames their Default variants.

## Goals / Non-Goals

**Goals:**
- One theme entry point, `AppTheme`, that every screen and preview wraps itself in.
- Tokens and text styles mirrored 1:1 from Figma, under the Figma names, so a token in Figma can be found in code by name.
- Components that take their Default/Touch size from the window class, so screens never pass a size for the class.

**Non-Goals:**
- Screen layouts, navigation, sidebars as screens, drag & drop logic. Components only render the visual states (Card Dragging and Placeholder, Drop indicator).
- Any component state held in the component. Components are stateless; callers own state, as in `kmp-compose-components`.

## Decisions

### Theme structure
`AppTheme` (name from CLAUDE.md) provides through CompositionLocals:
- `AppTheme.colors`: all 53 Color variables as nested groups that follow the Figma paths: `colors.background.surface`, `colors.text.muted`, `colors.accent.default`, `colors.shadow.popover1`, `colors.label.teal`. One light and one dark instance, chosen by `isSystemInDarkTheme()`.
- `AppTheme.space`, `AppTheme.radius`, `AppTheme.height`: every Spacing, Radius and Size variable.
- `AppTheme.width`: `sidebarTablet` 224, `sidebarDesktop` 248, `panelTablet` 420, `panelDesktop` 452, named after the Size variables `width/*` (D23). Dependency: these variables are created by `update-figma-foundations`, which is applied before this change. Task 1.2 stops if they're missing. Numeric Figma names get a letter prefix because Kotlin names can't start with a digit: `space/12` → `space.s12`, `radius/8` → `radius.r8`, `radius/full` → `radius.full`, `height/control-touch` → `height.controlTouch`.
- `AppTheme.shadows`: the 7 effect styles as data (layers with color token, offset, blur, spread), drawn with Compose `dropShadow`. The focus rings (`Focus/Accent`, `Focus/Danger`) are drawn the same way, as a 3dp spread with no blur.
- `AppTheme.textStyles`: all ~59 Figma text styles as `TextStyle`s, named after the style (`Sans/13.5 Medium/138%` → `sans13_5Medium138`).
- `AppTheme.typography`: the 7 Foundations roles (`pageTitle`, `screenTitle`, `cardTitle`, `body`, `label`, `idMetadata`, `sectionLabel`). Each points to one raw style, and `pageTitle`, `cardTitle` and `body` switch to their phone style in the phone class. Page title on phone is the 28 style (fact in decisions.md).

A Material 3 `ColorScheme` and `Typography` are built from the same tokens and passed to `MaterialTheme`, so Material bases (menus, sheets, dialogs, pull-to-refresh) start from our colors. Components read only `AppTheme.*`, never `MaterialTheme.*` directly. Alternative: only extended colors without a Material scheme. Rejected, because Material bases would then render default purple until every slot is overridden.

Dynamic color is never used. The dark/light switch is `isSystemInDarkTheme()` only.

### Fonts
Static TTFs from the official vercel/geist-font release (D8) in `core/designsystem/src/commonMain/composeResources/font/`: Geist 400/500/600/700, Geist Mono 400/500/600. The OFL 1.1 license ships with the app in `composeResources/files/licenses/`. Text sizes and line heights use sp and letter spacing uses em. Fractional sizes (13.5, 12.5) stay fractional.

### Icons
The 27 Figma icons are exported as SVG with the Figma MCP and converted to vector drawable XML with the `figma-to-android-vector` skill, into `composeResources/drawable/`. Hard-coded stroke and fill colors are replaced by one neutral color, and the icon is tinted through `Icon`'s `tint` / `LocalContentColor`. They're exposed on a `CardiganIcons` object as `Painter`s, as in `kmp-compose-components`. No Material Icons (scaffold D4).

The Logo is drawn in code as a tile in `text/primary` with three columns. It has two theme colors, which a single tinted vector can't express.

### Window classes and size variants
`AppTheme` reads the window size from `LocalWindowInfo.current.containerSize`, converts it to dp, and provides `LocalWindowClass` (Phone, Tablet, Desktop) with the rules from D12. The breakpoints (600, 840, height 480) are constants in one file. Alternative: Material 3 adaptive `currentWindowAdaptiveInfo()`. Rejected, because it would add the `material3-adaptive` dependency and uses different breakpoints.

Components with Default/Touch variants read `LocalWindowClass` and use Touch in Phone. Where Figma has two sizes per class (Button Small/Medium ↔ Touch small/Touch), the caller passes the semantic size (`Small` or `Medium`) and the component maps it to the Touch counterpart in Phone.

### Touch area
Components keep their Figma visual size. `AppTheme` sets `LocalMinimumInteractiveComponentSize` to `Dp.Unspecified`, so Material components don't grow their layout to 48dp. The 48dp hit area comes from Compose's hit testing instead: pointer input regions smaller than `ViewConfiguration.minimumTouchTargetSize` (48dp) get an extended touch area, and overlapping extended areas go to the nearest node. That matches D4 without changing spacing. The extension doesn't pass a clipping parent (`Modifier.clip`, `graphicsLayer(clip = true)`, a clipping `Surface`). Containers that hold components smaller than 48dp (Column, Card, Panel, Dialog, Popover, App bar, Page header) draw their shape with `background(color, shape)` and `border(..., shape)` and don't clip their content. Where a container must clip, the small component gets an invisible 48dp hit box instead (the fallback in Risks). Alternative: `Modifier.minimumInteractiveComponentSize()`. Rejected, because it grows the layout size and breaks Figma spacing.

### Interaction feedback
- Press: a ripple from `ripple()` whose color comes from `LocalRippleConfiguration`. `AppTheme` sets it to `text/primary`. Components with accent or danger fills (Primary and Danger buttons, Selected icon buttons where the fill is accent) provide `LocalRippleConfiguration` with `text/on-accent` around their content.
- Hover and keyboard focus: components with a Highlighted or Hover state collect `collectIsHoveredAsState()` and `collectIsFocusedAsState()` from their `MutableInteractionSource` and show the Figma Highlighted look while either is true (D3). Danger buttons have no hover change (D15).

### Font scaling
Figma heights are applied with `heightIn(min = …)`, never fixed `height` (D16). Single-line text uses `maxLines = 1` and `TextOverflow.Ellipsis`.

### Component bases
Material 3 bases first, as in `kmp-compose-components`:

| Component | Base |
|---|---|
| Button, Icon button, Filter button | `Button` / `IconButton` families with our colors, shapes, padding |
| Checkbox | `Checkbox` with our colors |
| Input, Text area | `BasicTextField` with our decoration (D9). Material text fields force a 56dp minimum height |
| Menu, Menu item, Menu divider | `DropdownMenu`, `DropdownMenuItem`, `HorizontalDivider` |
| Dialog | `BasicAlertDialog` with our surface |
| Bottom sheet | `ModalBottomSheet` with our container, shape, scrim and drag handle |
| Popover | `Popup` with our surface and `Shadow/Popover` |
| Toast | `SnackbarHost` with a custom `Snackbar` look |
| Spinner | `CircularProgressIndicator` with our colors |
| Pull-to-refresh | `PullToRefreshBox` with `PullToRefreshDefaults.Indicator`, container `background/surface`, spinner `accent/default` (D5) |
| Everything else (Card, Column, Label chip, Key, Avatar, Skeleton, …) | Foundation layouts. No Material equivalent models them |

### Dialog and text area behavior
- The Progress dialog passes `DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)` and has no close control, which also covers Escape (D20). Default dialogs keep both set to true.
- Text areas use `BasicTextField` without `maxLines`, so they grow with their content (D19). The Description field never scrolls itself. The card detail Panel body is a vertical scroll container.

### Drag visuals
Card takes a `state` prop (Default, Selected, Dragging, Placeholder). Dragging applies a 2.2° `graphicsLayer` rotation and `Shadow/Drag`. Placeholder and Drop indicator are plain visuals. No gesture handling in this change (D21).

### Toast host
A design-system `ToastHostState` wraps `SnackbarHostState`. `show(message, type)` dismisses the current toast before showing the new one, so there's never a queue (D13). Success uses a 4s duration. On Android the duration goes through `AccessibilityManager.calculateRecommendedTimeoutMillis`, as Material's `SnackbarHost` already does, so the "Time to take action" setting can lengthen it. iOS uses 4s. Error uses an indefinite duration and closes through the close icon. The host sits at the bottom of the screen. Screens own the state and place the host.

### Skeleton shimmer and reduced motion
The shimmer is an infinite transition of a linear gradient from `background/skeleton` to `background/skeleton-highlight`, 1.2s per loop (D14). An `expect fun rememberReduceMotion(): Boolean` reads the OS setting: on Android, the animator duration scale is 0 ("Remove animations"); on iOS, `UIAccessibilityIsReduceMotionEnabled()`. When true, the skeleton draws the static color.

### Label palette
`LabelColor` in `:core:designsystem` lists the 20 shared-spec color names, and each maps to its `colors.label.*` token. Feature modules map their domain color to it (D9).

### Previews
Every component gets one `@Preview` per distinct state in `commonMain`, in light and dark, plus phone and tablet widths where the size variant changes (`widthDp = 390` and `834`). `AppTheme` takes `windowClass: WindowClass? = null`. When set, it replaces the measured class for both `LocalWindowClass` and the phone switch in `AppTheme.typography`. Tablet and desktop previews pass it explicitly. Previews are for the user to look at in the IDE. They are not rendered by the agent: the Android CLI and `android-preview-figma-verify` only render previews in `androidMain`, and previews stay in `commonMain` (D25). The agent checks each component against its Figma variant in code instead. There are no component tests (D6).

### Package layout
`com.plcoding.cardiganapp.core.designsystem` with `theme/` (colors, tokens, type, shadows, window class, AppTheme), `icons/`, and `components/<section>/`, using the Figma section names (`actions`, `inputs`, `overlays`, `feedback`, `labels`, `board`, `boards`, `navigation`, `brand`, `datadisplay`, `carddetail`, `transfer`).

## Risks / Trade-offs

- [Open risk: the extended touch area through `minimumTouchTargetSize` may not apply on iOS] → Not verified in this change (D22, previews only). The first feature change that runs screens on the iOS simulator should check a Small icon button. If iOS doesn't extend the hit area, fall back to an invisible 48dp hit box on the components smaller than 48dp.
- [Material `DropdownMenu` and `ModalBottomSheet` add their own padding, elevation tonal overlay or drag handle] → Override every visible parameter. If Figma can't be matched, wrap a `Popup` instead and note it in the commit.
- [Open risk: `ModalBottomSheet` may behave differently on iOS (insets, keyboard)] → Not verified in this change (D22). Check it in the first feature change that shows a sheet.
- [About 60 raw text styles and 53 colors are a lot to hand-port] → Copy names and values from a read-only Figma dump in one task. Check every value against the Figma variable once, not per component.
- [Without component tests or on-device checks, behavior (toast timing, Progress dialog dismissal, reduced motion) shows up only once screens use it] → Accepted by D6 and D22.
- [Window class from `LocalWindowInfo` updates on every resize] → The class value only changes at the breakpoints, so recomposition stays limited to class changes.

## Open Questions

- Toast horizontal position on tablet and desktop: read from the frames during the toast task (doesn't change specs).
