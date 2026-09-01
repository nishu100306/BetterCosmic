# BetterCosmic

A client-side quality-of-life mod for **CosmicPrisons** and **CosmicSky**. BetterCosmic adds
on-screen HUDs, inventory overlays, event tracking, waypoints, peaceful-mining visuals, and a range
of quality-of-life tweaks — all configurable from a fully custom in-game settings screen with no
external dependencies required.

It ships as a single mod that detects which Cosmic network you're on and enables the matching feature
set automatically:

- **BetterPrisons** features activate on CosmicPrisons.
- **BetterSky** features activate on CosmicSky.

> Client-side only. It changes what *you* see and do; it does not modify the server or give any
> unfair advantage. Everything is toggleable.

---

## Getting started

1. Install [Fabric Loader](https://fabricmc.net/) for the supported Minecraft version and
   [Fabric API](https://modrinth.com/mod/fabric-api).
2. Drop the BetterCosmic `.jar` into your `mods/` folder.
3. Launch the game and join CosmicPrisons or CosmicSky.
4. Press **I** (or open **Mod Menu**) to configure everything.

**Requires:** Minecraft 1.21.11 · Fabric Loader 0.17.3+ · Fabric API · Java 21+

---

## Features

### On-screen HUDs (CosmicPrisons)

Each HUD is optional, independently toggleable, drag-to-reposition, and individually scalable, with
configurable colors, borders, and opacity.

- **Cooldown HUD** — Live timers for command and ability cooldowns (teleports, kits, combat, and
  more), each with its own icon and color.
- **Satchel HUD** — Fill level and capacity for every satchel in your inventory, read from the
  satchel's own data so it stays correct even when renamed. Covers all ore-satchel variants and the
  special drop satchels, with optional combining of same-type satchels and threshold color alerts.
- **Stats HUD** — Session XP, XP/hour, Cosmic Energy, totals, and estimated time to next level, with
  a paused indicator and compact/comma number formatting.
- **Enchant HUD** — Countdown timers for active enchants and effects (Super Breaker, Powerball),
  plus a customizable "Powerball ready" title-and-sound alert.
- **Events HUD** — Detects and tracks meteors (with natural/summoned landing countdowns), meteorite
  showers, Ore Merchants (per-tier toggles, distance), and Badlands bandit rushes — each with icons,
  colors, and optional sound alerts.
- **Super Breaker Aura** — A crosshair-centered ring timer (WeakAura-style) with adjustable colors,
  size, opacity, and offset.

### Waypoints & world markers

- Screen-edge direction indicators (on-screen diamonds / clamped off-screen arrows) for every active
  meteor, merchant, bandit rush, and meteorite shower.
- Optional 3D beacon-beam pillars drawn in world space, visible at any render distance.
- Custom per-world waypoints you place yourself.

### Gang Pings

- Press **G** to broadcast your position, HP, and facing to gang chat; bind an extra key to ping the
  exact block you're looking at.
- Received pings render as player-head icons at the sender's location, with optional beacon beams and
  configurable info lines (name, countdown, coordinates, distance, HP, facing).
- Distance-based fading and scaling, sound alerts, and a short client-side anti-spam cooldown.

### EasyView — inventory overlays

Compact value labels drawn directly on items so you don't have to hover: Cosmic Energy, money notes,
gang points, black scrolls, charge orbs, dust, pages, prestige tokens, XP bottles, and item levels on
armor / weapons / pickaxes. Each type toggles and colors independently.

### Item tooltips

- **Enchant book costs** — Hover an enchant book (in inventory or linked in chat) to see per-level
  upgrade energy costs and the running total to max level.
- **Gang point expiry** — A live countdown plus the expiry time in your local timezone.

All mod-added tooltip lines are clearly prefixed so they're easy to spot.

### Search tools

- **Chest & book search** — Open any container to get a search bar and a filter-rule sidebar that
  highlights matching items, with per-rule colors and Any/All matching. Enchant books can be filtered
  by success rate, destroy rate, or energy cost, and clue scrolls by their current step number (type
  the number in the search bar, or add a "clue #" filter rule).
- **Clue scroll sorting** — Shows each clue scroll's current step number large on the item, in
  containers and the hotbar, so a chest of scrolls sorts at a glance.

### Peaceful mining

For mining in crowded areas: nearby players turn translucent while you hold a pickaxe/mace (or always,
in the PrisonBreak world), block-breaking progress stays visible through them, and accidental
hits/right-clicks on players are suppressed. Configurable opacity, radius, and auto-disable on combat.

### Quality-of-life

- **Message notifications** — Sound alert on private messages, with a choice of sounds and volume.
- **Held item scaling** — Resize held pickaxes, swords, axes, and other items independently.
- **Pickaxe drop protection** — Double-press-to-drop confirmation, with options to block dropping or
  dragging pickaxes out entirely.
- **Auto trade** — Shift-right-click a player to send `/trade <name>` automatically.
- **Bold XP/Energy popups** — Optionally bold the server's on-screen `+XP` / `+Energy` popups.
- **PrisonBreak texture pack** — A bundled ore texture pack that auto-applies in the PrisonBreak
  world and removes itself when you leave.

### BetterSky

- **Potion trinket charge overlay** — Shows remaining uses on potion trinkets right in the slot, with
  configurable scale, position, and color.

---

## Configuration

Everything is configured from a custom in-game screen — open it with **I** or through **Mod Menu**.
It's organized into tabs (HUD settings, feature settings, and general configuration), with collapsible
sections, per-setting reset buttons, hover tooltips, a full HSV color picker, a drag-and-drop HUD
editor (right-click a HUD for an inline scale slider), and theme customization.

Settings are saved under `config/bettercosmic/` and only your changed values are written, so untouched
settings automatically pick up new defaults on update. The in-game screen is the recommended way to
change settings.

### Keybinds

| Key | Action |
| --- | --- |
| **I** | Open the configuration screen |
| **R** | Reset Stats HUD tracking |
| **B** | Pause / resume Stats HUD tracking |
| **G** | Send a gang ping (your position) |
| *(unbound)* | Send a gang ping at the block you're looking at |
| *(unbound)* | Open the waypoints screen |

Unbound actions can be assigned in Minecraft's **Controls** menu under the mod's category.

---

## Updates

BetterCosmic checks for updates on launch and can surface a notification (and, if you opt in,
download the new version automatically). Update checking and auto-apply are both toggleable in the
General settings, and every downloaded update is integrity-verified before it's installed.

---

## License

BetterCosmic is **All Rights Reserved** — see [LICENSE](LICENSE). You may download and use it as a
client-side mod for personal play; redistribution, modification, and reuse of the code or assets are
not permitted without written permission.

Minecraft is a trademark of Mojang Synergies AB. This is an unofficial, fan-made mod and is not
affiliated with or endorsed by Mojang, Microsoft, or the CosmicPrisons / CosmicSky networks.
