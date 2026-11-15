# Tasks

Every task works only in Figma file `MALwbOph7Uj2QtiTy21JaQ`, through the Figma MCP. No code or file changes in this repo or the web repo. Before the first write: `create_session`, load `figma:figma-use`. If a Figma call fails, stop and report the call and node.

Order: apply this change before add-design-system (decisions.md D10).

## Approved text

Approved by the user via the orchestrator session on 2026-11-15 (decisions.md D9).

Color tokens:
- background/hover: "Hover and highlighted rows: menu items, options, icon buttons"
- background/skeleton: "Loading placeholders"
- background/skeleton-highlight: "Shimmer highlight on loading placeholders"
- background/scrim: "Dim layer behind dialogs and sheets"
- background/toast: "Toast background"
- text/on-accent: "Text and icons on accent fills"
- text/on-danger: "Text and icons on danger buttons"
- text/toast: "Toast text and icons"
- border/accent-subtle: "Info banner border"
- border/danger-subtle: "Error banner border"
- danger/soft: "Error banner background, error focus ring"
- accent/hover: "Primary button hover"
- success/soft: "Not used yet"

Radius:
- radius/1: "Logo columns"
- radius/2: "Drop indicator, sheet handle"
- radius/5: "Menu items, options"
- radius/7: "Icon buttons"
- radius/16: "Bottom sheet top corners"

Size:
- height/control-sm (30): "Small buttons"
- height/control-md (34): "Medium buttons, filter button"
- height/control-touch-sm (38): "Touch small buttons"
- height/control-touch (44): "Touch buttons and inputs"
- height/input (36): "Inputs"
- height/icon-button-small (28): "Icon buttons in column headers"
- height/icon-button-medium (32): "Icon buttons"
- height/icon-button-large (40): "Icon buttons in the phone app bar"
- width/sidebar-tablet (224): "Sidebar on tablet"
- width/sidebar-desktop (248): "Sidebar on desktop"
- width/panel-tablet (420): "Card detail panel on tablet"
- width/panel-desktop (452): "Card detail panel on desktop"

Effects:
- Shadow/Small (shadow/small): "Buttons, cards, columns, board tiles"
- Shadow/Popover (shadow/popover-1, popover-2): "Menus, popovers, dialogs, sheets, toasts"
- Shadow/Drag (shadow/drag-1, drag-2): "Card while dragged"
- Shadow/Panel (shadow/panel): "Card detail panel"
- Shadow/Raised (shadow/raised): "Phone app bar once content scrolls under it"
- Focus/Accent (accent/soft): "Focus ring on inputs and controls"
- Focus/Danger (danger/soft): "Focus ring on inputs with an error"

Type and other text:
- New role "Large title (phone board list)", spec text "Geist 600 · 26/28.6 · -2.5% LS"
- Page title spec text: "phone 26–28" → "phone 28"
- Icons doc text: "27 icons, 16 × 16, 1.6 stroke." → "27 icons, 16 × 16 on a 24px grid, 1.7 stroke." (rest of the sentence unchanged)
- Overview Design System card count: "71 components" → "44 components · 27 icons"
- Spacing steps: no usage text, like the existing ones

## 1. Preparation

- [x] 1.1 Confirm the user approved the usage-text list (decisions.md D4) and that G1 and G2 are answered. Copy the approved list and answers into this file under "Approved text". Verify: the section exists and every item from the list is there.
- [ ] 1.2 Run `get_metadata` on `9:2` and record the current node IDs of the main frame, the Color tokens panels, Type, Space & shape and Breakpoints. Verify: the IDs are listed in the task notes.

## 2. Variables

- [ ] 2.1 Load `figma:figma-generate-library`. Add `width/sidebar-tablet` 224, `width/sidebar-desktop` 248, `width/panel-tablet` 420 and `width/panel-desktop` 452 to the Size collection, scope `WIDTH_HEIGHT` only. Verify: a read-only call lists 12 Size variables with these values and scopes.

## 3. Color tokens

- [ ] 3.1 Add swatch cards for the 13 missing colors to the Light and Dark panels, cloned from an existing card, fills bound to the variable, meta text "name · hex" from the variable values, usage text from the approved list. Verify: screenshot of the Color tokens section, and both panels show 26 swatches.

## 4. Space and shape

- [ ] 4.1 Add the 16 missing spacing steps and the 5 missing radii (with approved usage text) to the existing scales, sizes and radii bound to their variables. Verify: screenshot of Space & shape; 23 spacing steps and 11 radius tiles.
- [ ] 4.2 Add the Size section with all 8 `height/*` and 4 `width/*` variables, bars bound to the variables, usage text from the approved list. Bind the width numbers in the Breakpoints table to the new `width/*` variables. Verify: screenshot of Size and Breakpoints.

## 5. Effects and type

- [ ] 5.1 Add the Effects section: one card per effect style (7) with light and dark sample tiles, layer values, shadow color variables with light and dark values, and usage text. Verify: screenshot of Effects.
- [ ] 5.2 In Type, change the Page title spec text to "phone 28" and add the "Large title (phone board list)" specimen with `Sans/26 SemiBold/110% · -2.5% LS`. Add the Text styles table with every text style, specimen bound to the style, values read from the style. Verify: screenshot of Type and Text styles; the table row count equals the number of local text styles.

## 6. Fixes outside Foundations

- [ ] 6.1 On Overview (`0:1`), change the Design System page card count to the approved wording (G1). On the Design System page (`9:3`), change the Icons doc text "1.6 stroke" to the approved text. Verify: screenshots of both cards.

## 7. Final check

- [ ] 7.1 Read-only check over `9:2`: every swatch fill, scale size, bar and sample in the new and changed nodes is bound to a variable or style, and no `placeholder` is left on. Take one screenshot of the whole page in light. Verify: the check returns no unbound values, and the screenshot is attached to the summary sent to the orchestrator.
