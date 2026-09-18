# BetterCosmic 2.2.0 Changelog

This release is focused on **Cosmic Prisons**: a new Player Vault viewer, a move onto Cosmic's official
mod API for more reliable tracking, and a switch to Cosmic's official auto-updater. Cosmic Sky is
unchanged. Everything is optional and toggleable from the in-game config menu (default **I**).

## New on Cosmic Prisons

- **Player Vault viewer** — preview your `/pv` vaults without digging through them one at a time. Open
  any vault screen and cached previews of your other vaults appear in sidebars down each edge:
  - **Star** the vaults you care about to pin them to the front.
  - **Click an unopened vault** to jump straight to it, or drag an item onto it to deposit.
  - The vault you're actually in stays fully interactive through its preview.
  - Tune it to taste: a **preview scale** slider to fit more vaults on screen, a **background opacity**
    slider (transparent by default), and a **show/hide empty vaults** toggle.

## Improvements

- **More reliable HUDs via Cosmic's official mod API.** Where the server supports it, your **cooldown
  timers**, **active enchant procs** (Super Breaker), and **Events HUD** meteors and ore merchants now
  come straight from the server instead of being read out of chat — so they're more accurate and update
  the instant something changes. If the API isn't available, everything falls back to the previous
  chat-based detection automatically, so nothing stops working.

## Changed

- **Auto-updates are now handled by Cosmic's official updater.** BetterCosmic's own updater has been
  retired in favor of Cosmic's official nested auto-updater, controlled in-game with **`/cosmicupdater`**.
  It keeps you on the latest **approved** build. If you're updating from 2.1.0, this is the last time the
  old updater runs — after that, `/cosmicupdater` takes over.

---

Download in **#mod-download** in the BetterCosmic Discord: https://discord.gg/vJaH4Yr5Dq
