# Artifact review (light): add-design-system

**Verdict:** Ready
**Reviewed:** 2026-11-15, schema `careful-planning`, repo kind `code`

| Pass | Blocker | Major | Minor |
|---|---|---|---|
| Testability | 0 | 1 | 3 |
| Security | 0 | 0 | 1 |
| Hidden assumptions | 0 | 2 | 0 |
| Design contract | 0 | 1 | 2 |

## Findings

### D1: Size variable count contradicts the width tokens
**Severity:** major · **Status:** applied
**Location:** tasks.md > 1.1 (and 1.2 verify step)
**Current text:** "Verify: the dump has 53 colors with two modes, 23 spacing, 11 radius, 8 size, 7 effects and every text style."
**Failure scenario:** update-figma-foundations adds four `width/*` variables to the Size collection, so the dump has 12 size variables. Task 1.1's check fails, or the implementer "fixes" it by dropping the widths, and task 1.2's "count of the properties matches the dump" can't be met by `height` (8) plus `width` (4) split across two token groups.
**Fix:** "Verify: the dump has 53 colors with two modes, 23 spacing, 11 radius, 12 size (8 `height/*` and the 4 `width/*` from update-figma-foundations), 7 effects and every text style." In 1.2: "Verify: `AppTheme.height` plus `AppTheme.width` together match the 12 Size variables, the other counts match the dump from 1.1, and a spot check of 5 colors per mode against Figma matches." (decisions.md D10 says "8 sizes" because it was written before D23; it isn't edited here.)

### T2: Ripple color is unspecified in the spec, and one global ripple color can't do what design.md describes
**Severity:** major · **Status:** applied
**Location:** specs/design-system/spec.md > Press feedback; design.md > Interaction feedback
**Current text:** spec: "Every interactive component SHALL show a ripple tinted from the theme colors while pressed, in both themes." design: "a ripple from `ripple()` whose color comes from `LocalRippleConfiguration`, set in `AppTheme` to a theme token (`text/primary` on neutral surfaces, `text/on-accent` on accent and danger fills)."
**Failure scenario:** `AppTheme` can set only one `LocalRippleConfiguration`. An implementer sets it to `text/primary` and stops there, so Primary and Danger buttons get a dark ripple on the accent fill. "Tinted from the theme colors" is still true, so nothing flags it, and previews don't show ripples.
**Fix:** spec: "Every interactive component SHALL show a ripple while pressed. The ripple color SHALL be `text/primary` on neutral surfaces and `text/on-accent` on accent and danger fills, from the active theme." Add scenario: "WHEN the user presses a Primary button in the light theme THEN the ripple uses the light `text/on-accent` color". design.md: "`AppTheme` sets `LocalRippleConfiguration` to `text/primary`. Components with accent or danger fills (Primary and Danger buttons, Selected icon buttons where the fill is accent) provide `LocalRippleConfiguration` with `text/on-accent` around their content." Catches: a Primary button using the global `text/primary` ripple.

### A1: Toast timeout ignores platform accessibility timeouts
**Severity:** major · **Status:** applied
**Location:** specs/design-system/spec.md > Toast timing; design.md > Toast host
**Current text:** "A success toast SHALL close on its own after about 4 seconds."
**Failure scenario:** Material's `SnackbarHost` passes the duration through Android's `AccessibilityManager.calculateRecommendedTimeoutMillis`, so with TalkBack or the Android "Time to take action" setting a success toast stays much longer than 4s. iOS has no such adjustment. The behavior differs between platforms, and a test of the spec scenario fails on Android with accessibility on. Two implementers could either keep Material's adjustment or bypass it with a custom timer.
**Fix (proposed, user decides):** "A success toast SHALL close on its own 4 seconds after it appears, or after the longer timeout the platform's accessibility settings recommend (Android \"Time to take action\"). iOS uses 4 seconds." Add scenario: "WHEN Android's \"Time to take action\" is set to 1 minute and a success toast appears THEN it stays for 1 minute."

### A2: Extended touch area doesn't reach outside clipping parents
**Severity:** major · **Status:** applied
**Location:** design.md > Touch area; spec > Minimum touch area
**Current text:** "pointer input regions smaller than `ViewConfiguration.minimumTouchTargetSize` (48dp) get an extended touch area, and overlapping extended areas go to the nearest node."
**Failure scenario:** Compose's hit test stops at a layer that clips (`Modifier.clip`, `graphicsLayer(clip = true)`, a clipping `Surface`). A 28dp icon button in a Column header inside a rounded, clipped Column gets no extension past the Column's edge, so the spec's "a touch 8dp outside its visible edge still activates it" fails near container edges. The design states the mechanism without that precondition, so implementers will clip containers by default.
**Fix:** add to design.md > Touch area: "The extension doesn't pass a clipping parent. Containers that hold components smaller than 48dp (Column, Card, Panel, Dialog, Popover, App bar, Page header) draw their shape with `background(color, shape)` and `border(..., shape)` and don't clip their content. Where a container must clip, the small component gets an invisible 48dp hit box instead (the iOS fallback in Risks)."

### T1: "About" bounds on toast and shimmer timing can't fail
**Severity:** minor · **Status:** applied
**Location:** spec > Toast timing, Skeleton shimmer
**Current text:** "closes after about 4 seconds"; "in a loop of about 1.2 seconds"
**Failure scenario:** Using `SnackbarDuration.Long` (10s) or a 3s shimmer still counts as "about" for a lenient reader.
**Fix:** "closes 4 seconds (±0.5s) after it appears" (subject to A1) and "in a loop of 1.2 seconds (±0.1s)". Catches: `SnackbarDuration.Long` for success toasts.

### T3: Accessible name requirement isn't checkable at the component level
**Severity:** minor · **Status:** applied
**Location:** spec > Accessible names from string resources
**Current text:** "Every interactive component that shows only an icon SHALL expose a content description to TalkBack and VoiceOver, taken from string resources."
**Failure scenario:** The component can't know where the caller's string came from, so the "from string resources" part can't fail in this module. An icon button with `contentDescription: String? = null` meets the wording if callers happen to pass one.
**Fix:** "Every interactive component that shows only an icon SHALL take a non-null `contentDescription` parameter and expose it as its accessibility label. Callers pass a string resource." Catches: an `IconButton` with a nullable or defaulted description.

### T4: Pull-to-refresh colors aren't named
**Severity:** minor · **Status:** open (token choice is the user's)
**Location:** spec > Pull-to-refresh indicator; design.md > Component bases
**Current text:** "THEN the indicator shows on a dark theme surface with the accent color"
**Failure scenario:** One implementer uses `background/surface`, another `background/raised` (or whatever the elevated surface token is). Both pass the scenario.
**Fix:** "THEN the indicator container uses `<surface token>` and the spinner uses `accent/default`", with the surface token confirmed by the user.

### S1: Font download has no integrity record
**Severity:** minor · **Status:** applied
**Location:** tasks.md > 2.1
**Current text:** "Verify: 7 font files and the license exist, and the release tag is noted in the commit message."
**Failure scenario:** A wrong or altered asset (wrong release, a mirror, a re-uploaded asset) ships inside both apps, and font files are parsed by native code on every device. Nothing records what was downloaded.
**Fix:** "Verify: 7 font files and the license exist, they come from the release assets of `vercel/geist-font` at a named tag, and the release tag and the SHA-256 of each file are noted in the commit message."

### D2: Tablet previews can't switch typography through `LocalWindowClass`
**Severity:** minor · **Status:** applied
**Location:** design.md > Previews
**Current text:** "The tablet preview sets a window class override through `LocalWindowClass`."
**Failure scenario:** `AppTheme` resolves `AppTheme.typography` (phone switch) from the class it measures. Providing `LocalWindowClass` inside `AppTheme` changes component sizes but not type, so tablet previews show phone text sizes and fail the preview check. Implementers will pick different workarounds.
**Fix:** "`AppTheme` takes `windowClass: WindowClass? = null`. When set, it replaces the measured class for both `LocalWindowClass` and the phone switch in `AppTheme.typography`. Tablet and desktop previews pass it explicitly."

### D3: Behavior shared with web is specified only in this repo
**Severity:** minor · **Status:** applied
**Location:** proposal.md > Shared-spec requirements; spec > Toast timing, Progress dialog, Text areas, Window size classes
**Current text:** config: "Only specify behavior that differs per platform or exists only on mobile"
**Failure scenario:** decisions D12, D13, D17, D19, D20 are marked "Shared with web", but no shared UI spec exists, so the same behavior is specified twice (here and in the web repo) and can drift.
**Fix:** add to proposal.md > Shared-spec requirements: "Toast timing, Progress dialog, text area growth and the window classes are agreed with the web client but have no shared spec yet. They are specified here until a shared UI spec exists."

## Trust boundaries

- Font download from GitHub (task 2.1): a wrong or altered asset could ship. Not stopped; see S1.
- Figma MCP reads (tokens, SVG icons): read-only, converted to vector XML and reviewed in previews. Low risk; no finding.
- Bundled resources (fonts, vectors, license) parsed on device: only as trusted as their source; covered by S1.
- No network, auth, user data, storage, IPC or deep links in this change.

## Coverage

All 14 requirements and their scenarios in `specs/design-system/spec.md` checked. Read decisions.md, proposal.md, design.md, tasks.md, README.md, `openspec/config.yaml`, CLAUDE.md, the `cmp.library` convention plugin, `core/designsystem/build.gradle.kts`, the version catalog, update-figma-foundations proposal/decisions/design (overlap check), and shared spec `domain-label`. No main specs exist yet. Not read: Figma (no MCP calls), Compose/Material source to confirm A1 and A2, `domain-board`, the `kmp-*` skills.

## Applied changes

- `specs/design-system/spec.md`: Press feedback names both ripple colors and adds a Primary-button scenario (T2). Toast timing uses 4s ±0.5s with the Android accessibility timeout and a new scenario (A1, T1). Skeleton loop is 1.2s ±0.1s (T1). Accessible names require a non-null `contentDescription` (T3).
- `design.md`: ripple per fill (T2), clipping rule in Touch area (A2), Android accessibility timeout in Toast host (A1), 1.2s shimmer (T1), `AppTheme(windowClass)` for previews (D2).
- `tasks.md`: 1.1 and 1.2 expect 12 Size variables (D1), 1.4 ripple and `windowClass` parameter (T2, D2), 2.1 tag and SHA-256 (S1), 4.1 no-clip rule (A2), 5.1 on-accent ripple (T2), 7.3 toast timing (A1).
- `proposal.md`: note on behavior shared with web without a shared spec (D3).
- T4 stays open: the pull-to-refresh surface token needs the user's choice.
- decisions.md: D13 says "about 4 seconds" and doesn't mention accessibility timeouts. A1 refines it; not a contradiction, but D13 could be updated by the user. D10's "8 sizes" predates D23 (D1).
- `openspec validate add-design-system --strict` passes. README.md is not stale.
