package dev.nishu.bettercosmic.prisons;

import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * BetterPrisons mod identity and shared constants.
 *
 * <p>Not a Fabric entrypoint: BetterPrisons is a client-only mod, so all initialization happens in
 * {@link dev.nishu.bettercosmic.prisons.client.BetterPrisonsClient}. This class just holds the mod
 * id, logger, and id helper.
 */
public final class BetterPrisons {
	public static final String MOD_ID = "betterprisons";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private BetterPrisons() {}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
