package dev.nishu.bettercosmic.shared.util;

import net.minecraft.client.Minecraft;

/**
 * Content-agnostic helpers for the client's current world. Nothing here is server-specific, so it
 * lives in the shared library. Lifted from BetterPrisons' {@code WaypointManager.detectWorldKey}.
 */
public final class WorldUtil {

	private WorldUtil() {}

	/** The dimension key string of the world the client is currently in, or {@code "unknown"}. */
	public static String detectWorldKey() {
		Minecraft client = Minecraft.getInstance();
		if (client.level != null) {
			return client.level.dimension().identifier().toString();
		}
		return "unknown";
	}
}
