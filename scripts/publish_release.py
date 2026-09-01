#!/usr/bin/env python3
"""Publish a BetterCosmic release to Modrinth and Discord.

GitHub Releases are handled by the `release` job in release.yml; this covers the other two of
the three targets. Content comes from files in `release/`:

  changelog.md          required — the release notes (fails if empty)
  description-full.md    optional — Discord full-description embed (NOT published to Modrinth)
  description-short.md   optional — Modrinth project body + Discord short-description embed
                         (the Modrinth summary field is left untouched — set it manually)
  config.json           non-secret IDs (Modrinth project, Discord channels); REPLACE_* = skip that bit
  state.json            auto-managed: Discord message ids + description hashes (committed back by CI)

Descriptions update only when their text changed (hash vs state). The three platforms are
independent — a Discord failure doesn't undo the Modrinth (or GitHub) release; the script attempts
everything, then exits non-zero if anything failed. Tokens come from the environment:
MODRINTH_TOKEN, DISCORD_BOT_TOKEN.

Usage:  python scripts/publish_release.py --version 1.9.2 --jar dist/bettercosmic-1.9.2.jar
"""
import argparse
import hashlib
import json
import os
import pathlib
import sys

import requests

ROOT = pathlib.Path(__file__).resolve().parent.parent
RELEASE = ROOT / "release"
MODRINTH_API = "https://api.modrinth.com/v2"
DISCORD_API = "https://discord.com/api/v10"
USER_AGENT = "nishu100306/BetterCosmic release-automation"
EMBED_LIMIT = 4096  # Discord embed description max


def read_text(name: str) -> str:
    try:
        return (RELEASE / name).read_text(encoding="utf-8").strip()
    except FileNotFoundError:
        return ""


def load_json(name: str, default):
    try:
        return json.loads((RELEASE / name).read_text(encoding="utf-8"))
    except (FileNotFoundError, json.JSONDecodeError):
        return default


def sha(text: str) -> str:
    return hashlib.sha256(text.encode("utf-8")).hexdigest()


def clip(text: str, limit: int) -> str:
    return text if len(text) <= limit else text[: limit - 1] + "…"


def configured(value) -> bool:
    return bool(value) and not str(value).startswith("REPLACE")


# ---------------------------------------------------------------- Modrinth

def publish_modrinth(cfg, version, jar_path, changelog, short_desc, state, token):
    m = cfg.get("modrinth", {})
    project = m.get("projectId")
    if not configured(token) or not configured(project):
        print("Modrinth: not configured (token/projectId) — skipping.")
        return
    headers = {"Authorization": token, "User-Agent": USER_AGENT}

    # Idempotency: don't re-upload a version that already exists (e.g. a re-run of the tag).
    resp = requests.get(f"{MODRINTH_API}/project/{project}/version", headers=headers, timeout=30)
    resp.raise_for_status()
    if any(v.get("version_number") == version for v in resp.json()):
        print(f"Modrinth: version {version} already exists — skipping upload.")
    else:
        data = {
            "project_id": project,
            "version_number": version,
            "name": f"BetterCosmic {version}",
            "changelog": changelog,
            "game_versions": m.get("gameVersions", []),
            "loaders": m.get("loaders", ["fabric"]),
            "version_type": m.get("versionType", "release"),
            "featured": False,  # required by the API; let Modrinth auto-feature the latest
            "dependencies": [
                {k: v for k, v in dep.items() if not k.startswith("_")}
                for dep in m.get("dependencies", [])
            ],
            "file_parts": ["file"],
            "primary_file": "file",
        }
        jar = pathlib.Path(jar_path)
        with open(jar, "rb") as fh:
            files = {
                "data": (None, json.dumps(data), "application/json"),
                "file": (jar.name, fh, "application/java-archive"),
            }
            r = requests.post(f"{MODRINTH_API}/version", headers=headers, files=files, timeout=180)
        if r.status_code >= 300:
            raise RuntimeError(f"version create HTTP {r.status_code}: {r.text}")
        print(f"Modrinth: published version {version}.")

    # The Modrinth project BODY is set from the SHORT description. The full description is intentionally
    # not published to Modrinth (it lives on Discord only), and the Modrinth summary (the short
    # 'description' field) is left untouched — set that manually on Modrinth. Only pushes when the short
    # description changed since Modrinth last got it; the hash advances only on a successful push.
    if short_desc and sha(short_desc) != state.get("modrinthBodyHash"):
        r = requests.patch(
            f"{MODRINTH_API}/project/{project}",
            headers={**headers, "Content-Type": "application/json"},
            data=json.dumps({"body": short_desc}), timeout=30)
        if r.status_code >= 300:
            raise RuntimeError(f"project patch HTTP {r.status_code}: {r.text}")
        state["modrinthBodyHash"] = sha(short_desc)
        print("Modrinth: updated project body from the short description.")


# ---------------------------------------------------------------- Discord

def _discord_post(headers, channel, embed):
    r = requests.post(f"{DISCORD_API}/channels/{channel}/messages",
                      headers=headers, data=json.dumps({"embeds": [embed]}), timeout=30)
    if r.status_code >= 300:
        raise RuntimeError(f"post message HTTP {r.status_code}: {r.text}")
    return r.json().get("id")


def _discord_edit_or_post(headers, channel, mid, embed):
    """Edit message `mid` in place, or post a new one if `mid` is None / was deleted. Returns the id."""
    if mid:
        r = requests.patch(f"{DISCORD_API}/channels/{channel}/messages/{mid}",
                           headers=headers, data=json.dumps({"embeds": [embed]}), timeout=30)
        if r.status_code == 404:
            mid = None  # message was deleted — repost below
        elif r.status_code >= 300:
            raise RuntimeError(f"edit message HTTP {r.status_code}: {r.text}")
        else:
            return mid
    return _discord_post(headers, channel, embed)


def _discord_delete(headers, channel, mid):
    """Best-effort delete of a leftover message (ignores 404 / errors)."""
    try:
        requests.delete(f"{DISCORD_API}/channels/{channel}/messages/{mid}", headers=headers, timeout=30)
    except Exception:  # noqa: BLE001 — cleanup is best-effort
        pass


def _discord_upsert(headers, state, key, channel, embed):
    """Edit the tracked embed message, or post + record it the first time (or if it was deleted)."""
    if not configured(channel):
        raise RuntimeError(f"channel for {key} not configured")
    state[key] = _discord_edit_or_post(headers, channel, state.get(key), embed)


def split_embed(text, limit):
    """Split `text` into chunks no longer than `limit`, preferring paragraph/line/word boundaries."""
    text = text.strip()
    if len(text) <= limit:
        return [text]
    chunks, rest = [], text
    while len(rest) > limit:
        window = rest[:limit]
        cut = window.rfind("\n\n")
        if cut < limit // 2:
            cut = window.rfind("\n")
        if cut < limit // 2:
            cut = window.rfind(" ")
        if cut <= 0:
            cut = limit
        chunks.append(rest[:cut].strip())
        rest = rest[cut:].strip()
    if rest:
        chunks.append(rest)
    return chunks


def publish_discord(cfg, version, changelog, full_desc, short_desc, state, token):
    d = cfg.get("discord", {})
    if not configured(token):
        print("Discord: no bot token — skipping.")
        return
    headers = {"Authorization": f"Bot {token}", "Content-Type": "application/json", "User-Agent": USER_AGENT}
    color = d.get("embedColor", 0xF1C40F)

    # Changelog: a new message in the changelog channel, once per release version. Guarded by the last
    # posted version so a re-run of the same release doesn't post a duplicate changelog.
    ch = d.get("changelogChannelId")
    if not configured(ch):
        print("Discord: changelog channel not configured — skipping changelog message.")
    elif state.get("discordChangelogVersion") == version:
        print(f"Discord: changelog for {version} already posted — skipping.")
    else:
        _discord_post(headers, ch, {
            "title": f"BetterCosmic {version}",
            "description": clip(changelog, EMBED_LIMIT),
            "color": color,
        })
        state["discordChangelogVersion"] = version
        print("Discord: posted changelog.")

    # Full description: split across as many single-embed messages as needed (Discord caps one embed at
    # 4096 chars), editing existing messages in place and deleting any that are no longer needed. Only
    # when the text changed since Discord last got it.
    fch = d.get("fullDescriptionChannelId")
    if full_desc and not configured(fch):
        print("Discord: full-description channel not configured — skipping.")
    elif full_desc and sha(full_desc) != state.get("discordFullHash"):
        chunks = split_embed(full_desc, EMBED_LIMIT)
        # Migrate the old single-id form, then reuse existing messages in order.
        ids = list(state.get("fullDescMessageIds")
                   or ([state["fullDescMessageId"]] if state.get("fullDescMessageId") else []))
        new_ids = []
        for i, chunk in enumerate(chunks):
            embed = {"description": chunk, "color": color}
            if i == 0:
                embed["title"] = "BetterCosmic"
            new_ids.append(_discord_edit_or_post(headers, fch, ids[i] if i < len(ids) else None, embed))
        for stale in ids[len(chunks):]:  # fewer chunks than before — remove the extras
            _discord_delete(headers, fch, stale)
        state["fullDescMessageIds"] = new_ids
        state.pop("fullDescMessageId", None)  # superseded by the list form
        state["discordFullHash"] = sha(full_desc)
        print(f"Discord: updated full-description ({len(chunks)} embed message(s)).")

    # Short description: a single embed (it always fits).
    if short_desc and sha(short_desc) != state.get("discordShortHash"):
        _discord_upsert(headers, state, "shortDescMessageId", d.get("shortDescriptionChannelId"),
                        {"title": "BetterCosmic", "description": clip(short_desc, EMBED_LIMIT), "color": color})
        state["discordShortHash"] = sha(short_desc)
        print("Discord: updated short-description embed.")


# ---------------------------------------------------------------- main

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--version", required=True)
    ap.add_argument("--jar", required=True)
    args = ap.parse_args()

    cfg = load_json("config.json", {})
    state = load_json("state.json", {})
    changelog = read_text("changelog.md")
    full_desc = read_text("description-full.md")
    short_desc = read_text("description-short.md")

    if not changelog:
        sys.exit("release/changelog.md is empty — a changelog is required for every release.")

    failures = []
    try:
        publish_modrinth(cfg, args.version, args.jar, changelog, short_desc, state,
                         os.environ.get("MODRINTH_TOKEN", ""))
    except Exception as e:  # noqa: BLE001 — keep platforms independent
        failures.append(f"Modrinth: {e}")
        print(f"::error::Modrinth publish failed: {e}")
    try:
        publish_discord(cfg, args.version, changelog, full_desc, short_desc, state,
                        os.environ.get("DISCORD_BOT_TOKEN", ""))
    except Exception as e:  # noqa: BLE001
        failures.append(f"Discord: {e}")
        print(f"::error::Discord publish failed: {e}")

    # Per-target hashes and message ids are advanced inside each platform only after a successful
    # push, so persisting here records exactly what actually went out (and nothing that was skipped).
    (RELEASE / "state.json").write_text(json.dumps(state, indent=2) + "\n", encoding="utf-8")

    if failures:
        sys.exit("Release automation had failures:\n  " + "\n  ".join(failures))
    print("Release automation complete.")


if __name__ == "__main__":
    main()
