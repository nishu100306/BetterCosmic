# BetterCosmic Config UI — Implementation Plan

A from-scratch, sleek/compact config UI built in the **`:shared`** library so both BetterSky and
BetterPrisons use it. Mods register *panels*; the framework renders a paginated 6-up **panel grid**,
and clicking a panel opens a translucent centered **feature popup** (≈ one BetterPrisons "tab") laid
out as `label ⇒ widget` rows.

Design is locked by the approved mockup (`config-ui-mockup.html`): cosmic-sky dark ground, translucent
panels, 1px hairline borders, 10–12px type, one accent (sky-cyan `#57D4E6`), 2px color swatches.

> This document is the build contract. It (1) maps every BetterPrisons UI capability to an equivalent
> or replacement, (2) gives a subplan per mockup component, and (3) sequences the work into testable
> phases. Nothing here has been built yet.

---

## 1. Architecture

New package tree, all under `:shared`:

```
dev.nishu.bettercosmic.shared.ui
  .render     RenderUtils, ColorUtils                 — GuiGraphics drawing + color math
  .core       UiElement (base), Theme, OverlayLayer,  — base component, theme tokens, screen-level
              ModalHost                                 overlay stack, popup-owned modal contract
  .widget     Toggle, Slider, IntSlider, Dropdown, DropdownList, TextField,
              ColorSwatch, ColorPicker, KeybindButton, GroupLabel, LinkButton
  .screen     ConfigScreen, PanelGrid, Pager, FeaturePopup
  .model      ConfigPanel, OptionGroup, Option (+ Options builder factory), ConfigRegistry
```

> **Architecture revision (post-P4).** `OverlayLayer` and the popup's modal contract (`ModalHost`)
> live in `ui.core`, not `ui.screen`, so `ui.widget` depends only on `ui.core` (no `widget ↔ screen`
> cycle). Floating children *inside* a popup — the open dropdown list and the color picker — are not
> screen overlays; the `FeaturePopup` owns them as a single `ModalHost` modal (see §1 layering and
> §4.8). `ColorPickerPopup` was renamed `ColorPicker` and is a popup-attached sidebar, not a modal
> overlay.

**Two-model split** (mechanism vs content, per `ARCHITECTURE.md`):
- The framework (`ui.*`) is content-agnostic.
- Each mod builds `ConfigPanel`s from the `Options` factory and calls `ConfigRegistry.register(panel)`
  at client init. BetterSky registers **Trinkets** + **General**; empty grid cells render as
  "Coming soon" placeholders.

**Data model**
- `Option<T>` — one setting: `label`, optional `tooltip`, `Supplier<T> get`, `Consumer<T> set`,
  `T defaultValue`, and a `buildWidget()` that returns the bound widget. Concrete builders:
  `Options.toggle`, `.slider`, `.intSlider`, `.dropdown`, `.text`, `.color`, `.keybind`, `.link`,
  `.label`. This **replaces BetterPrisons' reflection binding** with type-safe lambdas:
  ```java
  Options.toggle("Charge overlay",
      () -> config.trinketChargesOverlay,
      v  -> { config.trinketChargesOverlay = v; config.save(); });
  ```
- `OptionGroup` — `{ String label, List<Option> }` → the uppercase eyebrow + its rows.
- `ConfigPanel` — `{ String id, String title, PanelIcon icon, String description,
  List<OptionGroup> groups }`.
- `ConfigRegistry` — ordered list of registered panels; drives pagination and placeholder fill.

**Base component (`UiElement`)** — lightweight, Mojang-native, **not** vanilla `AbstractWidget`
(too chunky for this look). Fields: `x,y,w,h, visible, enabled, hovered`. Methods:
`render(GuiGraphics, int mouseX, int mouseY, float dt)`, `mouseClicked/Released/Dragged`,
`keyPressed/charTyped`, `isMouseOver`, `tooltip()`. The screen extends vanilla `Screen` for lifecycle
and forwards events to the element tree.

**Layering / modals** — MC `GuiGraphics` draws in call order, so rendering runs in passes. At the
screen level: `grid → OverlayLayer (the open FeaturePopup) → tooltip`; only the topmost overlay gets
the real mouse, so lower layers show no hover. *Inside* the popup: `body → tooltip → active modal`.
The popup is a `ModalHost` with a single `activeModal` slot — an open dropdown list or the color
picker register via `openModal`, paint above sibling rows, receive input first, and make the body
inert while open (fixes the classic "dropdown drawn under the next row" problem and the "popup
behind still hovers" problem, with one enforced-single-modal mechanism instead of two).

---

## 2. BetterPrisons UI → New UI parity

Every BP `ui/custom` element, and where it lands. **Port** = translate Yarn→Mojang + restyle compact;
**Replace** = different design, same capability; **Defer** = not needed until a feature needs it.

| BetterPrisons element | New UI | Kind | Notes |
|---|---|---|---|
| `core/Component` | `ui.core.UiElement` | Replace | Leaner base; same event surface. |
| `core/Container` | element children list on containers | Port | Folded into screen/popup/group. |
| `core/Constraint` | — | Drop | BP's relative-constraint system replaced by explicit row/grid layout — simpler, fewer surprises. |
| `core/Theme` (27 colors, reflection) | `ui.core.Theme` (~8 tokens, config-backed) | Replace | Curated compact token set (see §6); no reflection. |
| `core/TooltipProvider` + `widgets/TooltipWidget` | `ui.core.Tooltip` (+ `Option.tooltip`) | Port | Hover tooltip, drawn in the top pass. |
| `rendering/RenderUtils` (GL11 scissor) | `ui.render.RenderUtils` | Port | Use `GuiGraphics.enableScissor/disableScissor` instead of raw GL11. |
| `rendering/ColorUtils` | `ui.render.ColorUtils` | Port | Pure math — copy verbatim (mapping-independent): `hsvToRgb`, `rgbToHsv`, `parseHex`, `toHex`. |
| `rendering/AnimationHelper` | `ui.render` easing helpers | Port (light) | BP shipped with animations disabled; add subtle, optional easing. |
| `containers/SidebarContainer` (tab list) | `ui.screen.PanelGrid` + `Pager` | Replace | New nav: 6-up grid + pagination instead of a scrolling sidebar. |
| `containers/CategoryContainer` (tab body) | `ui.screen.FeaturePopup` body | Replace | Same job (scrollable option list), now a centered popup. |
| `containers/ScrollContainer` | scroll baked into `FeaturePopup` (+ reusable helper) | Port | Scissor-clipped content + thin scrollbar. |
| `containers/ColorPickerPopup` | `ui.widget.ColorPicker` (popup modal sidebar) | Port | Keep HSV square + hue bar + hex + drag; restyle compact, delete debug logging; opened via `ModalHost`, not a screen overlay (see §4.8). |
| `screens/CustomConfigScreen` (1688 lines) | `ui.screen.ConfigScreen` + `FeaturePopup` | Replace | Split host screen (grid) from feature popup; no hardcoded categories. |
| `screens/EnhancedHudEditorScreen` | (future) HUD editor | Defer | BetterSky has no HUDs yet. |
| `screens/WaypointsScreen`, energy-calc GUI | BetterPrisons content | Out of scope | Feature content, not framework. |
| `widgets/ToggleWidget` | `ui.widget.Toggle` | Port | Pill + knob; on=accent. |
| `widgets/SliderWidget` | `ui.widget.Slider` | Port | Thin 2px track, square handle, mono value. |
| `widgets/IntSliderWidget` | `ui.widget.IntSlider` | Port | As Slider, integer steps. |
| `widgets/DropdownWidget` | `ui.widget.Dropdown` | Port | Compact box + caret; list in OverlayLayer. |
| `widgets/TextInputWidget` | `ui.widget.TextField` | Port | Keep cursor move, selection, horizontal scroll. |
| `widgets/ColorPickerWidget` | `ui.widget.ColorSwatch` | Port | 2px swatch + hex + reset; opens `ColorPickerPopup`. |
| `widgets/KeybindWidget` | `ui.widget.KeybindButton` | Port | "Press a key…" capture; conflict-agnostic (client keybind). |
| `widgets/LabelWidget` | `ui.widget.GroupLabel` | Port | Uppercase eyebrow section header. |
| `widgets/LinkButtonWidget` | `ui.widget.LinkButton` | Port | Opens URL (Discord/Modrinth) — behind a confirm, per web-link safety. |
| `widgets/CollapsibleWidget` | `ui.widget.CollapsibleGroup` | Defer | Popups are short; add only if a panel grows long. |
| `binding/BindingRegistry`, `ConfigBinding`, `FieldBinding`, `MapBinding` | `Option` get/set lambdas | Replace | Type-safe closures over config fields; live-apply + `config.save()`. |
| Per-widget **reset** (↻ button) | per-`Option` reset affordance | Port | Small reset glyph per row, back to `Option.defaultValue`. |

**Capabilities preserved from `MOD_FEATURES.md`:** per-setting reset, hover tooltips, dropdowns/popups
that block click-through, text-field cursor/selection/scroll, HSV color picker with hex, scroll when
content overflows, real-time theme updates. **Deliberately dropped:** the left sidebar (replaced by
the grid) and 27-way theming (replaced by a compact token set).

---

## 3. Mockup component subplans (faithful MC reproduction)

For each mockup piece: what it is, how it renders in MC, interaction, and faithfulness/risk.

### 3.1 Config root & background
- **Render:** vanilla `Screen.renderBackground` gives the blurred world; over it, one translucent
  panel `fill()` (`~rgba(14,18,28,.72)` → `0xB80E121C`) with a 1px hairline border, centered, fixed
  logical size (~ 300×200 GUI px). Header row: wordmark text + "Reset all"; footer row: pager + Done.
- **Faithful?** Yes. The mockup's CSS `backdrop-filter` blur ≈ MC's built-in screen blur. The
  **starfield is a mockup flourish** representing the game world — not reproduced; the real world
  shows through instead. Optional: a faint accent vignette via a large translucent gradient `fill`.

### 3.2 Panel grid (3×2) + panel card
- **Render:** grid of 6 cells laid out by explicit math (columns × rows, fixed gap). Each card:
  translucent `fill` + 1px border, an **icon** (16px), title (MC font), description (secondary color),
  and a footer line — `● N settings` (accent dot) for real panels, or a `Coming soon` chip for
  placeholders (dimmed, non-interactive).
- **Interaction:** hover → border to accent + 1px lift (y-offset); click a real card → open its popup.
- **Icons subplan:** small monochrome PNG sprites (16×16, tinted to accent/muted) under
  `assets/bettercosmicshared/textures/gui/panel/*.png`, drawn via `GuiGraphics.blit`. Alternative
  considered: draw primitives (limited) or item icons (off-brand). **Recommended: sprite sheet**, with
  a generic `lock` sprite for placeholders.
- **Faithful?** Yes; MC font is smaller/pixel vs the mockup's system-ui, which actually reinforces the
  "compact" goal.

### 3.3 Pagination (`Pager`)
- **Render:** `‹` / `›` buttons (drawn chevrons), `Page n / m` in mono with tabular alignment, page
  dots (accent = current). Disable arrows at ends.
- **Interaction:** click cycles page; also bind `Left`/`Right` arrow keys and mouse-wheel over the grid.
- **Faithful?** Yes.

### 3.4 Feature popup (`FeaturePopup`)
- **Render:** dim the grid (`fill 0x80000000`), draw a centered translucent popup (1px border, header
  with icon + title + `✕`), then a **scissor-clipped** scrollable body of groups/rows, with a thin
  scrollbar when overflowing. Grows to content up to a max height.
- **Interaction:** open on panel click; close via `✕`, click-outside, or `Esc`. Blocks click-through to
  the grid. Scroll via wheel/drag scrollbar.
- **Faithful?** Yes — this is the core of the design.

### 3.5 Group label + row
- **Group label:** uppercase, letter-spaced, muted, with a hairline underline (`GroupLabel`).
- **Row:** `label` left, widget right, fixed min-height, hairline divider between rows, plus a small
  **reset** glyph per row when the value ≠ default.
- **Faithful?** Yes.

### 3.6–3.12 Widgets — see §4 (each widget gets its own subplan there).

---

## 4. Widget subplans

Common: each widget is a `UiElement`, exposes `value`/`onChange`, supports `enabled`, draws its own
hover state, and reports a `tooltip`. All colors come from `Theme`.

### 4.1 Toggle
- Pill track (26×14) + knob (10). Off = muted track + light knob; on = accent-soft track, accent knob,
  faint glow. Click flips; `onChange(boolean)`. Optional 80ms knob slide (respects reduced-motion).

### 4.2 Slider / 4.3 IntSlider
- 2px track, filled portion accent, 8px square handle; mono value readout (tabular). Drag or click-to-
  set; arrow keys step; `IntSlider` snaps to integer steps. Ports `SliderWidget`/`IntSliderWidget`
  math. `onChange(float|int)`.

### 4.4 Dropdown
- Compact box `{ current ▾ }`; clicking opens a list **in `OverlayLayer`** (so it paints over rows
  below), hover highlight, selected shows accent + tick. Click option or outside closes.
  `onChange(String)` (or generic `<E>` enum variant). Ports `DropdownWidget`.

### 4.5 TextField
- 1px box, MC font, blinking caret, **text selection** (shift+arrows, drag), **horizontal scroll** when
  text exceeds width, copy/paste. Focus = accent border. Ports `TextInputWidget` (its cursor/selection/
  scroll logic is the valuable part). `onChange(String)` + validation hook.

### 4.6 KeybindButton
- Box showing the bound key or `Unbound`; click → `listening` state ("Press a key…") capturing the next
  key/mouse button; `Esc` clears. Ports `KeybindWidget`. Binds a Fabric `KeyMapping` (client keybind);
  no server conflict concerns.

### 4.7 GroupLabel / LinkButton
- `GroupLabel`: section eyebrow (see §3.5). `LinkButton`: accent text, underline on hover, opens a URL
  **via a confirmation** (per web-link safety) — for a mod Discord/Modrinth link.

### 4.8 Color swatch + Color picker popup (the emphasized one)
- **Swatch row (`ColorSwatch`):** 2px filled swatch + `#RRGGBB` mono + reset glyph; click opens the
  picker. Ports `ColorPickerWidget` (restyle: 20px preview → 2px swatch).
- **Picker (`ColorPickerPopup`) — fully functional:** port BP's popup with these changes:
  - **Restyle compact:** SV square ~120px (down from 200), thin hue bar (~8px), 2px selectors, 1px
    borders, sky palette.
  - **Keep the good parts:** HSV-square **cache** regenerated only when hue changes, block rendering
    for perf, hue bar cache, hex text input (typed, live), **drag** on SV + hue, OK/Cancel, click-
    outside cancels. `ColorUtils.hsvToRgb/rgbToHsv/parseHex` port verbatim.
  - **Fix/modernize:** delete all `System.out.println` debug logging; replace GL11 scissor with
    `GuiGraphics.enableScissor`; live-preview the target option as you drag (call `Option.set` on
    change, revert on Cancel); open as the popup's `ModalHost` modal — a right-hand sidebar on the
    popup's own layer (falls back to left, then center) — so it's above the body with no separate
    overlay (renamed `ColorPickerPopup` → `ColorPicker`).
  - **Risk:** SV-square block rendering cost — keep the cache; at 120px/4px blocks that's ~900 fills,
    cheap and cached.
- **Faithful?** Yes, and upgraded (live preview, cleaner scissor).

---

## 5. Rendering & math foundation

- **`RenderUtils` (Mojang):** `fill`, `outline(1px)`, `roundedRect` (optional; design is mostly sharp),
  `hLine/vLine`, gradient fill, `text`/`textRight`/`textCentered` (via `GuiGraphics.drawString` + MC
  `font`), and scissor push/pop wrapping `GuiGraphics.enableScissor/disableScissor` (framebuffer-safe,
  replaces BP's manual GL11 + scale-factor math).
- **`ColorUtils`:** ported verbatim from BP (pure int/float math).
- **Z / draw order:** enforced by pass order in `ConfigScreen.render` (§1). No manual GL depth.
- **Fonts:** MC `font` only; sizes are inherent (≈7px cap height). "Smaller text" is achieved by using
  single-line rows + tight padding rather than sub-pixel scaling (scaling MC font ↓ hurts legibility).
  Where the mockup scales type, we instead rely on MC's already-small font.

---

## 6. Theme (compact, config-backed, shared)

Replace BP's 27 fields with **~8 tokens** on `SharedConfig` (so it's shared and persisted), exposed in
the **General** panel and applied live:

| Token | Default (sky) | Use |
|---|---|---|
| `ground` | `0xE6070810` | screen dim / deepest |
| `surface` | `0xB80E121C` | panels/cards/popup |
| `surfaceHover` | `0xC8151B2A` | hovered card / widget bg |
| `line` | `0x2996ACD2` | 1px hairline borders |
| `accent` | `0xFF57D4E6` | on-states, selection, focus, slider fill |
| `text` | `0xFFE7ECF4` | primary text |
| `muted` | `0xFF8B95A9` | secondary/labels |
| `faint` | `0xFF545D70` | disabled/placeholders |

`Theme.load(SharedConfig)` copies these into static fields; editing a theme color in the General panel
updates the token and repaints immediately (BP's "real-time theme" behavior). Semantic on/off derive
from `accent` (no separate toggle/slider colors → fewer knobs, sleeker).

---

## 7. Opening the screen

- **Keybind:** register a Fabric `KeyMapping` "Open BetterCosmic config" (default **unbound** to avoid
  conflicts). Lives in `:shared` so both mods share one screen; each mod passes its registered panels.
- **ModMenu:** add `modImplementation modmenu` + a `ModMenuApi` entrypoint in **each mod**
  (`:bettersky` now, `:betterprisons` later) returning the `ConfigScreen`. Adds the TerraformersMC
  maven + modmenu dep to `bettersky/build.gradle` (BetterPrisons already anticipates this — see the
  note in `betterprisons/build.gradle`).

---

## 8. Build phases (each independently testable)

| Phase | Deliverable | Acceptance |
|---|---|---|
| **P0 — Foundation** | `RenderUtils`, `ColorUtils`, `Theme`, `UiElement`, `OverlayLayer` | Compiles; a throwaway test screen draws a themed panel + border. |
| **P1 — Shell** | `ConfigScreen` + `PanelGrid` + `Pager` + placeholder cards; keybind + ModMenu open it | Screen opens, 6-up grid paginates, placeholders show "Coming soon". |
| **P2 — Popup + model** | `Option`/`OptionGroup`/`ConfigPanel`/`ConfigRegistry` + `FeaturePopup` shell + `GroupLabel` | Clicking a registered panel opens a scrollable popup with group headers; close works. |
| **P3 — Core widgets** | `Toggle`, `Slider`, `IntSlider`, `Dropdown`, `TextField` + per-row reset + tooltips | Each edits a bound value live and persists; overlay dropdown paints on top. |
| **P4 — Color picker** | `ColorSwatch` + fully-functional `ColorPickerPopup` (live preview) | Pick via SV/hue/hex, drag, OK/Cancel; value persists; no debug spam. |
| **P5 — Aux widgets** | `KeybindButton`, `LinkButton` (+ `CollapsibleGroup` only if needed) | Rebind a key; link opens after confirm. |
| **P6 — Real panels** | **Trinkets** (BetterSky) + **General** (shared: dev mode, comma format, open-config keybind, theme colors) | Real settings editable; theme edits repaint live. |
| **P7 — Polish** | Subtle animation, click/hover sounds, keyboard nav/focus ring, edge cases; in-game verification | Feels sleek; matches mockup; no crashes across GUI scales. |

Phases map to the mockup: P1–P2 = the grid + popup shell, P3–P5 = the widget row types, P6 = wiring the
two real panels, P7 = the "sleek" finish.

---

## 9. Open decisions (recommendations in **bold**)

1. **Theme scope:** **compact ~8 tokens** (§6) vs BP's 27. Fewer knobs = sleeker; expand later if needed.
2. **Panel icons:** **16×16 PNG sprites** in shared assets vs drawn primitives vs item icons.
3. **Collapsible groups:** **defer** — popups are short; add if a panel outgrows one screen.
4. **Component base:** **custom `UiElement`** vs vanilla `AbstractWidget` (too chunky for this look).
5. **Text scaling:** **rely on MC's native small font** + tight layout rather than sub-pixel scaling.

Unless you redirect any of these, I'll proceed with the bold choices, starting at **P0 → P1**.
