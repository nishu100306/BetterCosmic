package dev.nishu.bettercosmic.sky;

import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * BetterSky mod identity and shared constants.
 *
 * <p>Not a Fabric entrypoint: BetterSky is a client-only mod, so all initialization happens in
 * {@link dev.nishu.bettercosmic.sky.client.BetterSkyClient}. This class just holds the mod id,
 * logger, and id helper.
 */
public final class BetterSky {
	public static final String MOD_ID = "bettersky";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private BetterSky() {}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
