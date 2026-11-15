# Proposal

## Why

Both clients now mirror every Figma variable, effect style and text style 1:1 (add-design-system D10, D11), but the Foundations page documents only part of that set. A developer reading Foundations can't find most tokens. The sidebar and side panel widths exist only as numbers in a table.

## What Changes

- Color tokens: add the 13 missing non-label colors as swatches in light and dark, bound to their variables, with approved usage text.
- Spacing and Radius: add the missing 16 spacing steps and 5 radii to the existing scales.
- New Size section: all `height/*` variables plus four new `width/*` variables for sidebar and side panel widths.
- New Effects section: the 7 effect styles, each shown with the shadow color variables it uses, in light and dark.
- New Text styles section: a table of all text styles below the role specimens.
- Type: Page title phone size "26–28" becomes "phone 28", and a new role "Large title (phone board list)" is added.
- Outside Foundations: correct the Overview component count text and the Icons doc text (stroke 1.6 → 1.7).
- Four new Figma variables in the Size collection: `width/sidebar-tablet`, `width/sidebar-desktop`, `width/panel-tablet`, `width/panel-desktop`.

All work happens in Figma through the Figma MCP. No code or file changes in this repo or the web repo, apart from these OpenSpec files.

## Capabilities

### New Capabilities
- (none)

### Modified Capabilities
- (none)

No app behavior changes, so the change sets `skip_specs: true`.

### Shared-spec requirements

- (none) This change documents tokens. It doesn't implement shared product behavior.

## Impact

- Figma file `MALwbOph7Uj2QtiTy21JaQ`: Foundations page `9:2`, Overview page `0:1` (count text), Design System page `9:3` (Icons doc text), and the Size variable collection.
- Order: this change is applied before add-design-system, whose task 1.2 stops if the `width/*` variables are missing. The web client follows the same order.
- No repo code, dependencies or build changes.
