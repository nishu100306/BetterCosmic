#!/usr/bin/env bash
#
# Generates the BetterCosmic auto-update manifest JSON on stdout.
#
# The manifest is a single small static file served from GitHub Pages (docs/manifest.json). The mod's
# UpdateChecker fetches it on launch and compares `latest` against the installed version. See
# planning/AUTO_UPDATER_PLAN.md §4.
#
# Values are taken from environment variables so this is usable both from the release workflow and by
# hand (e.g. to cut a manifest for a rollback). jq handles JSON-safe escaping of the changelog.
#
#   MODID      mod id            (default: bettercosmic)
#   VERSION    latest version    (required, e.g. 1.1.0)
#   MCVERSION  Minecraft version (required, e.g. 1.21.11)
#   CHANNEL    release channel   (default: release)
#   URL        jar download URL  (required)
#   SHA256     jar SHA-256 hex   (required)
#   CHANGELOG  short human text  (default: "")
#   MANDATORY  true|false        (default: false)
#
# Usage:
#   VERSION=1.1.0 MCVERSION=1.21.11 URL=… SHA256=… ./scripts/make-manifest.sh > docs/manifest.json
set -euo pipefail

: "${VERSION:?VERSION is required}"
: "${MCVERSION:?MCVERSION is required}"
: "${URL:?URL is required}"
: "${SHA256:?SHA256 is required}"

MODID="${MODID:-bettercosmic}"
CHANNEL="${CHANNEL:-release}"
CHANGELOG="${CHANGELOG:-}"
MANDATORY="${MANDATORY:-false}"

# Normalise MANDATORY to a strict JSON boolean.
case "$(printf '%s' "$MANDATORY" | tr '[:upper:]' '[:lower:]')" in
	true|1|yes) MANDATORY=true ;;
	*)          MANDATORY=false ;;
esac

jq -n \
	--arg     modId     "$MODID" \
	--arg     latest    "$VERSION" \
	--arg     minecraft "$MCVERSION" \
	--arg     channel   "$CHANNEL" \
	--arg     url       "$URL" \
	--arg     sha256    "$SHA256" \
	--arg     changelog "$CHANGELOG" \
	--argjson mandatory "$MANDATORY" \
	'{
		modId:     $modId,
		latest:    $latest,
		minecraft: $minecraft,
		channel:   $channel,
		url:       $url,
		sha256:    $sha256,
		changelog: $changelog,
		mandatory: $mandatory
	}'
