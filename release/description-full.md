# BetterCosmic

**BetterCosmic** is a client-side quality-of-life mod for **Cosmic Prisons**. It layers a suite of
fully customizable on-screen HUDs, inventory overlays, event tracking, navigation aids, and
quality-of-life tools on top of the vanilla client — all configured from a custom in-game menu, with
no external libraries required.

Everything is optional and independently toggleable. HUDs can be dragged, scaled, recolored, and
faded; overlays and features each have their own settings; and nothing is forced on you.

> Client-side only — install it on your own client. Nothing is required on the server.

---

## On-screen HUDs

Every HUD is an optional overlay you can move with the drag-and-drop **HUD editor**, scale
independently, recolor (background, border, opacity, title), and toggle on its own.

- **Cooldown HUD** — live timers for server commands and abilities, each with an optional icon and its
  own color. Tracks `/jet`, `/feed`, `/fix`, `/home`, `/tpa`, `/tpahere`, `/dangle`, `/adangle`,
  `/near`, `/pulse`, and a combat timer. `/adangle` is applied optimistically and auto-cancels if the
  server reports it failed.
- **Satchel HUD** — detects satchels in your inventory and shows fill vs. capacity, read from each
  satchel's data so it stays correct even when renamed. Covers every ore-satchel variant (regular,
  Deepslate, Block-of) plus Shard, Contraband, and Clue Scroll satchels. Optionally combines satchels
  of the same type, shows percent or raw numbers, and highlights nearly-full/empty ones by color.
- **Stats HUD** — current XP, XP per hour, Cosmic Energy, and session totals, with an estimated time to
  your next level and a clear paused indicator. Choose which stats to show and compact (1.2M) vs. comma
  (1,234,567) formatting.
- **Enchant HUD** — lists active timed enchants/effects with remaining time. Supports **Super Breaker**
  and **Powerball**, including a customizable on-screen alert (with optional sound) the moment Powerball
  comes off cooldown.
- **Events HUD** — a live tracker for server events:
  - **Meteors** with coordinates, a natural/player-spawned heading, and a landing countdown (7 min for
    natural, 1 min for summoned) that rolls into `(Imminent)` and then `[Crashed]`.
  - **Meteorite Showers** with their own countdown, waypoint, and beacon beam.
  - **Ore Merchants** with tier, coordinates, and distance, plus per-tier toggles (Coal → Emerald).
  - **Bandit Rushes** in the Badlands, filtered to your current sub-world region.

## Waypoints & navigation

- Screen-edge indicators for every active meteor, merchant, bandit rush, and meteorite shower — a
  colored diamond when the target is on screen, or an arrow clamped to the edge pointing the right way
  when it's off screen.
- Optional 3D **beacon beam** pillars drawn in the world at each event location, visible from any
  distance and render-distance-safe.
- A waypoints screen (bindable key) for managing your own custom waypoints alongside the automatic ones.
- Per-type toggles, opacity, and beam on/off are all configurable.

## Gang Pings

- Press **G** to broadcast your position, HP, and facing to gang chat; a second (unbound) key sends a
  ping at the exact block you're looking at (raycast up to 200 blocks) so you can mark a spot without
  walking to it.
- Received pings render as player-head icons at the sender's location with optional beacon beams,
  distance fade, and configurable info lines (name, countdown, coordinates + distance, HP, facing) —
  each line individually toggleable, with adjustable scale and readability backgrounds.
- A sound plays when a ping arrives in your world, with a short client-side anti-spam cooldown.

## Super Breaker Aura

- A centered ring timer (WeakAura-style) for your Super Breaker duration, with adjustable colors,
  opacity, size, X/Y offset, and an optional countdown number.

## EasyView — inventory value overlays

Compact text overlays drawn right on items so you can read important values at a glance without
hovering: **Cosmic Energy, Money Notes, Gang Points, Black Scrolls, Charge Orbs, Dust, Pages, Prestige
Tokens, XP Bottles**, and item levels for **armor, weapons, and pickaxes**. Each type toggles on its
own with a configurable color.

## Item tooltips

- **Enchant book costs** — hover an enchant book (in your inventory or linked in chat) to see the energy
  cost to reach each higher level plus a running total, using the correct tier-aware formula.
- **Gang point expiry** — adds a live countdown to expiry and the exact time in *your* local timezone,
  instead of the server's fixed EDT.

## Chest & book search

- Open any chest or container to get a search bar plus a filter-rule sidebar that highlights matching
  items. Each rule has its own color and an Any/All (OR/AND) mode so you can stack conditions.
- Enchant books can be filtered by success rate, destroy rate, or energy cost — not just by name.
- **Clue scroll sorting** shows a scroll's current step number large and centered on the item, in both
  containers and your hotbar, so a chest of scrolls can be sorted at a glance.

## Peaceful mining

For mining in crowded areas without accidentally targeting other players:

- Nearby players turn translucent while you hold a pickaxe or mace (each has its own toggle), and stay
  ghosted the whole time in the PrisonBreak world.
- Block-breaking progress still renders *through* the ghosted players, so you can always see what you're
  mining.
- All interaction with other players is disabled while active, preventing accidental hits or clicks.
- Adjustable opacity and radius, plus optional auto-disable when you enter combat (re-enabling after).

## Quality-of-life tools

- **Message notifications** — a sound alert when you get a DM, with 7 selectable sounds and a volume
  slider.
- **Held item scaling** — resize items in your hand (pickaxes, swords, axes, other) from 25–150%.
- **Pickaxe drop protection** — a double-press confirmation before dropping a pickaxe, with optional
  full block on dropping and on dragging pickaxes out of your inventory.
- **Auto trade** — shift-right-click a player to send `/trade <name>` automatically.
- **Powerball ready alert** — a customizable title + optional sound when Powerball is ready again.
- **Bold XP/Energy popups** — optionally bold the server's `+XP` / `+Energy` mining popups.
- **PrisonBreak texture pack** — a bundled ore texture pack that auto-applies in the PrisonBreak world
  and removes itself when you leave.

## Built-in updater

BetterCosmic checks for new versions and tells you in-game with a clickable toast. With auto-install
enabled it downloads and verifies the update and installs it on your next restart — no manual
re-download. Fully optional; toggle it in the config menu.

## Configuration

- A fully custom in-game config screen — open it with the config key (default **I**) or from **Mod
  Menu**. No external config library required.
- Organized into panels with collapsible sections, a drag-and-drop HUD editor (right-click a HUD for a
  live scale slider), full HSV color pickers with hex input, a per-setting reset, and a reset-all.
- Theme customization, including a per-server accent color.
- Settings persist between sessions; the in-game menu is the recommended way to change them.

## Keybinds

| Key | Action |
|---|---|
| **I** | Open the config screen |
| **R** | Reset Stats HUD tracking |
| **B** | Pause / resume Stats HUD tracking |
| **G** | Send a gang ping |
| *(unbound)* | Gang ping the block you're looking at |
| *(unbound)* | Open the waypoints screen |

---

**Support & feedback:** join the Discord — https://discord.gg/vJaH4Yr5Dq — for help, bug reports, and
suggestions.
