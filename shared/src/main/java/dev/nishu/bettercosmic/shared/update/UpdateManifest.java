package dev.nishu.bettercosmic.shared.update;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * The parsed update manifest — one small static JSON served from the update host (GitHub Pages). It
 * declares the newest published build; the client compares its {@link #latest} against the installed
 * version and, if newer <em>and</em> built for the running Minecraft version, surfaces an update.
 *
 * <p>Shape (see {@code planning/AUTO_UPDATER_PLAN.md} §4):
 * <pre>
 * {
 *   "modId": "bettercosmic",
 *   "latest": "0.0.3",
 *   "minecraft": "1.21.11",
 *   "channel": "release",
 *   "url": "https://github.com/.../releases/download/v0.0.3/bettercosmic-0.0.3.jar",
 *   "sha256": "9f2c…",
 *   "changelog": "…",
 *   "mandatory": false
 * }
 * </pre>
 *
 * <p>Parsing is deliberately defensive (same posture as {@code prisons.api.CosmicApi}): a malformed
 * or hostile response yields {@code null} rather than throwing, and every field is optional except
 * {@link #latest}. Network responses are untrusted data and must never crash the client.
 */
public final class UpdateManifest {

	public final String modId;
	public final String latest;
	public final String minecraft; // MC version this build targets, or null if unspecified
	public final String channel;   // "release"; reserved for a future beta channel (Phase 3), unused today
	public final String url;       // direct jar download (used by phase 2), may be null
	public final String sha256;    // lowercase hex, used by phase 2, may be null
	public final String changelog; // short human text, may be null
	public final boolean mandatory;

	private UpdateManifest(String modId, String latest, String minecraft, String channel,
						   String url, String sha256, String changelog, boolean mandatory) {
		this.modId = modId;
		this.latest = latest;
		this.minecraft = minecraft;
		this.channel = channel;
		this.url = url;
		this.sha256 = sha256;
		this.changelog = changelog;
		this.mandatory = mandatory;
	}

	/**
	 * Parses a manifest from its JSON text, or returns {@code null} if the text is malformed or is
	 * missing the required {@code latest} field. Never throws.
	 */
	public static UpdateManifest parse(String json) {
		try {
			JsonObject o = JsonParser.parseString(json).getAsJsonObject();
			String latest = str(o, "latest");
			if (latest == null || latest.isBlank()) {
				return null; // a manifest without a version is unusable
			}
			return new UpdateManifest(
					str(o, "modId"),
					latest,
					str(o, "minecraft"),
					str(o, "channel"),
					str(o, "url"),
					str(o, "sha256"),
					str(o, "changelog"),
					o.has("mandatory") && o.get("mandatory").isJsonPrimitive()
							&& o.get("mandatory").getAsBoolean());
		} catch (Exception e) {
			return null;
		}
	}

	private static String str(JsonObject o, String key) {
		return o.has(key) && o.get(key).isJsonPrimitive() ? o.get(key).getAsString() : null;
	}
}
