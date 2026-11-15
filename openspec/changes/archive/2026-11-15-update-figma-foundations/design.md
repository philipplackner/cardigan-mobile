# Design

## Context

Foundations (`9:2`) is one frame (`58:2`) with sections Header, Color tokens (Light and Dark panels of swatch cards), Label palette, Type (role specimens), Space & shape (Spacing and Radius scales), and Breakpoints (proposal). Frame IDs inside the page may change, so every task looks them up with `get_metadata` first. Facts about the current gaps, bindings and stroke weights are in decisions.md.

## Goals / Non-Goals

**Goals:**
- Every value on Foundations is bound to its variable or text/effect style, so the page updates when a token changes.
- The new sections match the existing card style (same fonts, sizes, borders, spacing and radius variables as the existing cards).

**Non-Goals:**
- Changing any existing variable value, style, component or screen frame.
- Documenting layout rules (D6).

## Decisions

### Tools and skills
All writes go through `use_figma`. Before the first write, load `figma:figma-use`. Also load `figma:figma-generate-library` before creating the `width/*` variables, since that skill covers variable creation and scopes. Every call uses the session from `create_session` on `MALwbOph7Uj2QtiTy21JaQ`.

### Building in place
New swatches are clones of an existing swatch card (`Swatch` + `Meta`). The clone keeps the existing styling, and its fill is rebound to the new variable with `setBoundVariableForPaint`. The Dark panel keeps its explicit Dark mode on the Color collection, so the same clone shows the dark value without separate fills. Hex values in the meta text are read from the variable's light and dark values, never typed by hand.

New sections (Size, Effects, Text styles) are auto-layout frames placed in the main frame's vertical order: Size after Space & shape, Effects after Size, Text styles after Type. Their headings reuse the existing heading layout (title + subtitle).

### Size section
One row per `height/*` and `width/*` variable: a bar whose width or height is bound to the variable, the variable name, the value, and the usage text.

### Effects section
One card per effect style: a sample tile with the effect style applied, the style name, its layers (offset, blur, spread), the shadow color variables it uses with light and dark values, and the usage text. The focus rings show their color variables (`accent/soft`, `danger/soft`) the same way. Light and dark are shown with two sample tiles, the second in Dark mode.

### Text styles table
One row per text style: style name, a specimen ("Cardigan") with the style applied, and font, weight, size, line height and letter spacing read from the style. Sorted as Figma lists them (Mono, then Sans by size).

### Width variables
`width/sidebar-tablet` 224, `width/sidebar-desktop` 248, `width/panel-tablet` 420, `width/panel-desktop` 452 in the Size collection, mode "Value", scopes `WIDTH_HEIGHT` only (D8). The breakpoint table cells that show these numbers are bound to the new variables, so the table and the variables can't drift apart.

### Usage text
Usage text is written only after the user approves the list (D4). The approved list is copied into tasks.md before task 2 starts.

### Verification
After each section, one screenshot of that section. After the page is done, a read-only check that every swatch, bar and sample on Foundations is bound to a variable or style (no unbound fills or numbers).

## Risks / Trade-offs

- [Cloned cards keep a binding to the old variable somewhere in their subtree] → The binding check in the final task looks at every fill and size in the new nodes, not only the swatch.
- [Text style specimens using Geist fail if the font isn't loaded] → Load every style's font with `loadFontAsync` before setting text, per `figma-use`.
- [The web session reads the page while it changes] → Sections are built one at a time with `placeholder = true` until done, so half-built sections are visible as such.

## Open Questions

- (none)
