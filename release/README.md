# Release automation

A `v*` tag publishes BetterCosmic to **three** places, from one workflow
([.github/workflows/release.yml](../.github/workflows/release.yml)):

| Target | What it gets | How |
|---|---|---|
| **GitHub Releases** | jar + changelog | `release` job (softprops/action-gh-release) |
| **Modrinth** | version (jar + changelog); project body/description on change | `publish_release.py` → Modrinth API |
| **Discord** | changelog message; full/short description embeds on change | `publish_release.py` → Discord bot API |

The auto-update manifest (`docs/manifest.json`) is also regenerated, as before.

## Content lives in this folder

| File | Required? | Used for |
|---|---|---|
| `changelog.md` | **yes** — build fails if empty | GitHub body, manifest, Modrinth version changelog, Discord changelog message |
| `description-full.md` | optional | Modrinth project **body** + Discord full-description embed |
| `description-short.md` | optional | Modrinth project **description** (≤256 chars) + Discord short-description embed |
| `config.json` | — | non-secret IDs (Modrinth project, Discord channels). Fill the `REPLACE_*` values. |
| `state.json` | — | **auto-managed by CI — do not hand-edit.** Discord message ids + per-target description hashes. |

Descriptions update **only when their text changes** (per-target hashes in `state.json`), so unchanged
descriptions don't re-post. A missing/empty description file is simply skipped.

## One-time setup

1. **GitHub Actions secrets** (Settings → Secrets and variables → Actions):
   - `MODRINTH_TOKEN` — a Modrinth PAT with scopes **`VERSION_CREATE`** and **`PROJECT_WRITE`**.
   - `DISCORD_BOT_TOKEN` — your bot token.
2. **Fill `config.json`**: the Modrinth project id/slug, game versions/loaders/dependencies, and the
   three Discord channel ids (changelog / full description / short description).
3. **Discord bot**: create it in the Developer Portal, invite it to your server with **Send Messages**
   and **Embed Links** in those three channels. The bot posts the description embeds itself and edits
   them thereafter (a bot can only edit its own messages), so let it post them on the first release —
   it records their message ids into `state.json` automatically.

Any target that isn't configured (placeholder id / missing secret) is **skipped**, so partial setup is
safe. The three targets are independent — one failing doesn't undo the others.

## Cutting a release

1. Update `release/changelog.md` (required) and, if changed, the description files.
2. Bump `bettercosmic_version` in `gradle.properties` to match the tag.
3. Commit, then tag and push:
   ```bash
   git tag -a v1.9.3 -m "…"   # add [mandatory] in the message to flag a critical update
   git push origin v1.9.3
   ```
4. Don't push further changes to `release/` until the workflow finishes (the publish job reads these
   files from the default branch).
