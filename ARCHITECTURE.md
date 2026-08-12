# BetterCosmic — Shared Architecture

This repo is the home of a **shared client-mod library** and the **BetterSky** mod built on
top of it. It is designed so that **BetterPrisons** can later fold into the same repo and
consume the same library, giving a single unified codebase that builds BetterSky and
BetterPrisons independently.

> Status: the three-module skeleton is **scaffolded and building** — `:shared` (library mod),
> `:bettersky`, and a `:betterprisons` **stub** (empty entrypoints, ready to be ported into).
> No feature code has been ported yet. The "Migration path" section sequences the remaining work.

---

## Decisions locked in

| Decision | Choice | Consequence |
|---|---|---|
| Mappings | **Official Mojang mappings** (`loom.officialMojangMappings()`, `fabric-loom-remap`) | BetterPrisons is on **Yarn**; every piece of BP code lifted into `:shared` must be **translated Yarn → Mojang** (class/method names *and* access-widener entries). Mechanical, but done per-port. |
| Repo shape | **Multi-module Gradle**: `:shared` + `:bettersky` (later `:betterprisons`) | Root becomes an aggregator; the current `src/` moves under `bettersky/`. |
| Root namespace | **`dev.nishu.bettercosmic`** | `…​.shared`, `…​.sky`, `…​.prisons`. |
| Minecraft | 1.21.11, Java 21, Fabric Loader ≥ 0.19.3, Fabric API | Matches the BetterSky template (BP is on the same MC version). |

---

## Module layout

```
BetterSky/                         ← root Gradle project (aggregator only, no code)
  settings.gradle                  ← include ':shared', ':bettersky'
  build.gradle                     ← common convention: loom-remap, Mojang mappings, Java 21
  gradle.properties                ← shared versions (mc, loader, fabric-api, java)
  gradle/  gradlew  gradlew.bat

  shared/                          ← the shared library (a Fabric *library mod*)
    build.gradle
    src/main/java/dev/nishu/bettercosmic/shared/…            (rarely used; server/common side)
    src/main/resources/fabric.mod.json                 (modid: "bettercosmicshared")
    src/main/resources/bettercosmicshared.accesswidener      (Mojang-named)
    src/client/java/dev/nishu/bettercosmic/shared/…          (the bulk of the library)
    src/client/resources/bettercosmicshared.client.mixins.json

  bettersky/                       ← the BetterSky mod (depends on + bundles :shared)
    build.gradle
    src/main/java/dev/nishu/bettercosmic/sky/BetterSky.java
    src/main/resources/fabric.mod.json                 (modid: "bettersky")
    src/client/java/dev/nishu/bettercosmic/sky/client/BetterSkyClient.java
    src/client/java/dev/nishu/bettercosmic/sky/…
    src/client/resources/bettersky.client.mixins.json
```

### Why `:shared` is itself a Fabric **library mod** (with its own `fabric.mod.json`)

This is the cleanest answer to the "how do mixins/access-wideners cross module boundaries"
problem. If `:shared` were a plain Java library, its mixins and access-widener would **not**
be applied, because only a mod with a `fabric.mod.json` can declare those.

Instead:

- `:shared` is a real (but user-invisible) library mod, `modid = bettercosmicshared`, that declares
  its own `mixins` config and `accessWidener` in its `fabric.mod.json`.
- `:bettersky` **`depends`** on `bettercosmicshared` and uses loom's **`include project(':shared')`**
  (Jar-in-Jar) to bundle it inside the BetterSky jar as a nested mod.
- When BetterSky loads, Fabric loads the nested `bettercosmicshared`, and its mixins + access-widener
  apply automatically. `:betterprisons` will do the same later.

This is the same pattern established libraries (owo-lib, Cardinal Components, etc.) use.

---

## The core principle: mechanism in `:shared`, policy/content in the mods

`:shared` must **never** reference server-specific content (no meteors, satchels, enchants,
islands, etc.). It provides *extension points*; each mod supplies the *content* by registering
against them.

- Prefer **Fabric API events/callbacks** (`HudRenderCallback`, `ItemTooltipCallback`,
  `ClientTickEvents`, networking) as the shared extension mechanism. Keep `:shared`
  **as mixin-light as possible** — most generic hooks in BP are already event-based.
- Where shared behavior genuinely needs a mixin, it lives in `bettercosmicshared.client.mixins.json`
  and stays content-agnostic (e.g. a generic "chat received" or "tooltip append" hook that
  fires a callback the mods subscribe to).
- Feature-specific mixins stay in the owning mod's mixin config.

---

## `:shared` package map (`dev.nishu.bettercosmic.shared`)

| Package | Responsibility | Source in BP (to translate later) |
|---|---|---|
| `BetterCosmicShared` | Library `ClientModInitializer`: registers shared services (API transport, HUD renderer, event bridges). | — (new) |
| `ui.core` | `Component`, `Container`, `Constraint`, `Theme`, `TooltipProvider` | `ui/custom/core/*` |
| `ui.widgets` | Toggle, Slider, IntSlider, Dropdown, TextInput, ColorPicker, Keybind, Label, Collapsible, LinkButton, Tooltip | `ui/custom/widgets/*` |
| `ui.containers` | Category, Sidebar, Scroll, ColorPickerPopup | `ui/custom/containers/*` |
| `ui.rendering` | RenderUtils, AnimationHelper, ColorUtils | `ui/custom/rendering/*` |
| `ui.binding` | BindingRegistry, FieldBinding, MapBinding, ConfigBinding | `ui/custom/binding/*` |
| `ui.screens` | **Content-agnostic** `CustomConfigScreen` driven by a mod-supplied category/widget tree; generic HUD editor screen | `ui/custom/screens/*` (de-hardcoded) |
| `hud` | `BaseHud`, `HudRenderer`, `HudRegistry`, HUD editor | `hud/BaseHud`, `HudRenderer`, `HudEditorScreen`, `EnhancedHudEditorScreen` |
| `config` | `ConfigManager` / base persistence to `config/<modid>/config.json` (Gson) | pattern from `Config` (persistence only) |
| `notification` | `Notifications`, `NotificationType`, toast plumbing | `notification/*`, `render/ToastRenderer` |
| `render` | `WorldSpaceTransform`, `BeaconBeamRenderer`, generic waypoint edge-indicator renderer (fed by a supplier), vertex-consumer utils | `render/*` (content stripped out) |
| `input` | Keybinding registration helpers | `KeyBindings` scaffolding |
| `util` | `ItemUtils`, `JsonLoader`, number/time formatting, color helpers | `utils/ItemUtils`, `JsonLoader` |

### Two components that need **de-hardcoding** as they move to `:shared`

1. **`CustomConfigScreen`** — BP's version hardcodes every category. In `:shared` it becomes a
   generic screen that takes a `List<CategoryContainer>` (or a builder) from the mod. Each mod
   assembles its own categories from shared widgets and binds them to its own config fields via
   the existing reflection-based binding system.
2. **`HudRenderer` / HUD editor** — operate over a `HudRegistry` that mods populate, instead of
   referencing a fixed set of HUD fields.

---

## Mod package map (`dev.nishu.bettercosmic.sky`) — and the future prisons mirror

```
dev.nishu.bettercosmic.sky
  BetterSky                 (main entrypoint — server/common)
  client.BetterSkyClient    (client entrypoint: builds config categories, registers HUDs,
                             registers API scopes/hooks/handlers with :shared)
  config.SkyConfig          (Sky's own fields, persisted via shared ConfigManager)
  feature.*                 (Sky-specific QoL features)
  hud.*                     (Sky-specific HUDs, registered with shared HudRegistry)
  mixin.client.*            (Sky-specific mixins)

dev.nishu.bettercosmic.prisons    (future — mirrors the above; BP content refactored onto :shared)
```

---

## Subsystem notes

### Config
- `:shared` owns **persistence** (`ConfigManager`: load/save JSON at `config/<modid>/config.json`,
  pretty-printed Gson) — not the field set.
- Each mod defines its own config class with its own fields and default values.
- The config **UI** is built by the mod from shared widgets + the shared binding registry, then
  handed to the shared `CustomConfigScreen`. Theme (BP's 27 color settings) lives in shared as a
  reusable `Theme`, with the mod choosing whether to expose it.

### Cosmic server API — prison-only, NOT shared
- **Cosmic Sky exposes no server API.** BetterSky is therefore purely client-local: it reads
  chat, inventory, world, and player state and renders — it never receives server data pushes.
- The `cosmicapi:main` handshake/hook layer (`api/CosmicApi`, `api/CosmicApiPayload`) is used by
  **exactly one mod, BetterPrisons**. By the "no shared abstraction before a second consumer"
  principle it **stays in `:betterprisons`** (`dev.nishu.bettercosmic.prisons.api.*`), not `:shared`.
- If a second mod ever needs a server API, the transport/handshake/dispatch core can be promoted
  to `dev.nishu.bettercosmic.shared.api` at that point, leaving event semantics in each mod.

### HUD system
- `:shared`: `BaseHud` (position/scale/enabled/render), `HudRenderer` (iterates the registry,
  respects F1/`hudHidden`), `HudRegistry`, and the drag-and-drop + right-click-scale editor.
- Each mod instantiates its HUDs, loads positions from its config, and registers them.

### Rendering
- `WorldSpaceTransform`, `BeaconBeamRenderer`, the screen-edge waypoint indicator renderer, and
  toast rendering are generic and move to `:shared`. The *set* of waypoints/beams to draw is
  content — the shared renderer takes a supplier/registry the mod feeds.

---

## Build wiring (target)

- **Root `settings.gradle`**: `pluginManagement` (Fabric + Central + plugin portal) and
  `include ':shared', ':bettersky'`.
- **Root `build.gradle`** (or a convention plugin / `subprojects {}`): apply `fabric-loom-remap`,
  Java 21, `loom.officialMojangMappings()`, `splitEnvironmentSourceSets()`, and the shared
  `minecraft` / `fabric-loader` / `fabric-api` versions from `gradle.properties`.
- **`:shared/build.gradle`**: loom config as above; `loom.mods { "bettercosmicshared" { … } }`;
  no `include` of others. Produces the library jar.
- **`:bettersky/build.gradle`**: `implementation project(':shared')` **and**
  `include project(':shared')` (Jar-in-Jar bundle); `loom.mods { "bettersky" { … } }`.
- ModMenu is optional per-mod (BP uses it; BetterSky can add it later). It is **not** a `:shared`
  dependency.

---

## Migration path (phased, sequences the real work)

1. **Restructure only** — turn this repo into `root aggregator + :shared (library mod) + :bettersky`,
   switch every module to Mojang mappings, apply the package renames below. No feature code moves.
2. **Skeleton extension points** — stand up empty shared APIs: `HudRegistry`, config-screen
   builder, `HookRegistry`, `ConfigManager`. Prove BetterSky boots with an empty config screen.
3. **Port generic infra from BP → `:shared`**, module by module (UI framework first, then HUD
   base + editor, render utils, notifications), translating Yarn → Mojang and re-expressing
   access-widener entries as each piece lands. (The CosmicApi layer is *not* part of this — it
   stays in `:betterprisons`.)
4. **First BetterSky feature** built in `:bettersky` on top of `:shared`.
5. **Fold in BetterPrisons** as `:betterprisons` in this repo, refactored onto `:shared`
   (content stays; infrastructure is deleted in favor of the shared versions).

### Renames required by step 1

| Now | Becomes |
|---|---|
| `src/` (repo root) | `bettersky/src/` |
| package `bettersky.bettersky` | `dev.nishu.bettercosmic.sky` (main) / `dev.nishu.bettercosmic.sky.client` (client) |
| `BetterSky.java`, `BetterSkyClient.java` entrypoints | updated in `fabric.mod.json` to the new packages |
| `bettersky.mixins.json` / `bettersky.client.mixins.json` | stay with the mod; new `bettercosmicshared.*.mixins.json` created in `:shared` |
| *(new)* | `shared/` module, `bettercosmicshared` modid, `dev.nishu.bettercosmic.shared.*` |

### Rename map when BP folds in (step 5)

| BP (Yarn) | Target |
|---|---|
| `BetterPrisons.modid.ui.custom.*` | `dev.nishu.bettercosmic.shared.ui.*` |
| `BetterPrisons.modid.hud.BaseHud` / `HudRenderer` | `dev.nishu.bettercosmic.shared.hud.*` |
| `BetterPrisons.modid.api.*` (prison-only) | `dev.nishu.bettercosmic.prisons.api.*` |
| `BetterPrisons.modid.render.*` (generic) | `dev.nishu.bettercosmic.shared.render.*` |
| `BetterPrisons.modid.*` (prison content) | `dev.nishu.bettercosmic.prisons.*` |
</content>
</invoke>
