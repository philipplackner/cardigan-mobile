# Decisions: update-figma-foundations

Confirmed with the user via the orchestrator session on 2026-11-15. The questions were sent to the orchestrator session (cardiganapp-e9), which relayed the user's answers.

## Summary

Bring the Figma Foundations page (`9:2`) in line with the full token set that both clients mirror (add-design-system D10, D11). Existing sections are extended in place, and new Size, Effects and Text styles sections are added. The change also fixes the Page title phone size and two small errors outside Foundations. All edits happen in Figma through the Figma MCP. No code or files change in either repo, apart from this change's OpenSpec files.

## Decisions

### D1 — Page structure
- **Question**: Extend the existing Foundations sections or rebuild the page?
- **Decision**: Extend the existing sections in place, in the same card style. Add new sections: Size, Effects, Text styles.
- **Alternatives**: Rebuild the page.
- **Why**: Least churn; existing nodes and links stay valid.
- **Depends on**: —

### D2 — Shadow colors
- **Question**: Where do the 7 shadow color variables go?
- **Decision**: In the Effects section, next to the effect style each one feeds, with light and dark values.
- **Alternatives**: As swatches in Color tokens.
- **Why**: They only exist as parts of shadows.
- **Depends on**: D1

### D3 — Text styles
- **Question**: How are all the text styles shown?
- **Decision**: A compact table of every text style (name, specimen, font, weight, size, line height, letter spacing) below the existing role specimens.
- **Alternatives**: A full specimen per style.
- **Why**: Readable and fits the page.
- **Depends on**: D1

### D4 — Usage text for new swatches and tiles
- **Question**: Where does the usage text for new colors, radii and sizes come from?
- **Decision**: Derived from where each variable is bound in the file. The full list goes to the user (through the orchestrator) for approval before anything is written to Figma.
- **Alternatives**: Name and value only.
- **Why**: Matches the existing swatches.
- **Depends on**: —

### D5 — Page title phone size
- **Question**: Foundations says "phone 26–28". Which is right?
- **Decision**: Change the Page title to "phone 28". Add the 26 style as its own role: "Large title (phone board list)".
- **Alternatives**: Only "phone 28"; leave it.
- **Why**: Matches the frames and the code.
- **Depends on**: —

### D6 — Layout rules on Foundations
- **Question**: Document the 480dp height rule and the Touch-size rule on Foundations?
- **Decision**: No. Foundations shows tokens only.
- **Alternatives**: Add both rules, marked app-only.
- **Why**: User choice.
- **Depends on**: —

### D7 — Fixes outside Foundations
- **Question**: Fix the Overview's component count and the icon stroke text in this change?
- **Decision**: Yes, both. The icon stroke text must match what the icon vectors actually use.
- **Alternatives**: Leave them.
- **Why**: User choice.
- **Depends on**: —

### D8 — Layout width variables
- **Question**: Should the sidebar and side panel widths become Figma variables?
- **Decision**: Yes. Add four variables to the Size collection, one per value (the collection has a single mode): `width/sidebar-tablet` 224, `width/sidebar-desktop` 248, `width/panel-tablet` 420, `width/panel-desktop` 452. Show them in the new Size section. Both clients name their tokens after them.
- **Alternatives**: Keep them only in the breakpoint table; one variable per element with tablet/desktop modes.
- **Why**: User choice. The names follow the existing `height/*` pattern. Modes would affect every Size variable.
- **Depends on**: D1

### D9 — Approved text
- **Question**: Approve the derived usage-text list (D4), the Overview count wording, and usage text for the two unbound colors.
- **Decision**: The list of 39 items is approved as proposed, except `accent/hover`, which gets "Primary button hover" (the web client uses it for primary button hover). `success/soft` gets "Not used yet". The Overview count becomes "44 components · 27 icons". The full text is in tasks.md under "Approved text".
- **Alternatives**: Keep "71 components"; name and value only for unbound colors.
- **Why**: User choice.
- **Depends on**: D4, D7

### D10 — Order with add-design-system
- **Question**: In which order are this change and add-design-system applied?
- **Decision**: This change is applied first. add-design-system stops if the four `width/*` variables are missing. The web client follows the same order.
- **Alternatives**: add-design-system falls back to the breakpoint table values.
- **Why**: User choice. Keeps both clients' tokens tied to real Figma variables.
- **Depends on**: D8

### D11 — Breakpoints table binding
- **Question**: Figma can't bind FLOAT variables to text content (`variable of resolved type 'FLOAT' cannot be bound to 'characters'`). How should the width numbers in the Breakpoints table be handled?
- **Decision**: Leave them as plain text. The Size section is the bound source for the four `width/*` variables. No new variables.
- **Alternatives**: Bound bars in the table cells; STRING variables holding the numbers.
- **Why**: User choice during apply, 2026-11-15.
- **Depends on**: D8

### D12 — Overview count pill layout
- **Question**: "44 components · 27 icons" doesn't fit next to the Design System card title (needs 296px, has 278px). How should it fit?
- **Decision**: The card's title row wraps, so the pill sits under the title. All 8 page cards on Overview get the same new fixed height (126px).
- **Alternatives**: Shorter text; tighter pill on this card only.
- **Why**: User choice during apply, 2026-11-15. Keeps the approved text and an even grid.
- **Depends on**: D9

## Facts

- Foundations today shows 13 of 33 non-label colors per mode, 7 of 23 spacing steps, 6 of 11 radii, no sizes, no effects, and 7 type roles out of 60 text styles. The 20 label colors and the breakpoints table are complete — source: `get_metadata` on `9:2`, variable and style dump
- Every icon vector has a 1.13px stroke at 16px, which is 1.7px on the 24px grid (1.7 × 16/24). The icon component descriptions say "1.7px stroke"; the Icons doc text says "1.6 stroke" — source: stroke weights of the 27 icon components on `9:3`
- The Design System page has 71 top-level components: 27 icons, 27 component sets and 17 single components. The Overview's "71 components" is correct when icons count — source: component count on `9:3`
- `background/scrim` is bound only on screen pages (dialog and sheet scrims). `accent/hover` and `success/soft` aren't bound anywhere in the file — source: variable binding scan of pages `9:3`–`9:9`

## Out of scope

- Any change to variables, styles, components or screen frames, apart from adding the four `width/*` variables (D8). Otherwise only the Foundations documentation, the Overview count text and the Icons doc text change.
- Any code or file change in either repo.

## Deferred

- (none)
