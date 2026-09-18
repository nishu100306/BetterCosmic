package dev.nishu.bettercosmic.prisons.pvviewer;

import dev.nishu.bettercosmic.prisons.planet.PlanetDetector;
import net.minecraft.client.Minecraft;

/**
 * Builds the cache key a vault snapshot is stored under: {@code <planet>/<uuid>}. Each Cosmic Prisons
 * planet has its own separate vault storage, and a player may log in on multiple accounts, so both the
 * planet and the account's UUID scope the cache. The account UUID (not username) is used so a cache
 * entry survives a username change.
 *
 * <p>When the planet can't be determined (e.g. in a hub/menu — {@link PlanetDetector#detect()} returns
 * {@code null}) the planet segment is {@code "unknown"}; capture only runs on a planet's {@code /pv}
 * screen, so real snapshots always carry a real planet.
 */
public final class PvKey {

	private PvKey() {}

	/** The key for the current planet + local player's account UUID, or {@code null} if no player. */
	public static String current() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) {
			return null;
		}
		String planet = PlanetDetector.detect();
		if (planet == null || planet.isEmpty()) {
			planet = "unknown";
		}
		String uuid = mc.player.getUUID().toString();
		return sanitize(planet) + "/" + sanitize(uuid);
	}

	/** Keeps keys filesystem/JSON-friendly and free of the {@code /} separator. */
	private static String sanitize(String s) {
		return s.replaceAll("[^A-Za-z0-9_.-]", "_");
	}
}
