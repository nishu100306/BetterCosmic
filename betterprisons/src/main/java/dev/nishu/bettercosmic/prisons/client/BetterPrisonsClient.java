package dev.nishu.bettercosmic.prisons.client;

import dev.nishu.bettercosmic.prisons.BetterPrisons;
import dev.nishu.bettercosmic.prisons.config.PrisonsConfig;
import dev.nishu.bettercosmic.shared.config.BetterCosmicConfig;
import dev.nishu.bettercosmic.shared.config.SharedConfig;
import net.fabricmc.api.ClientModInitializer;

/**
 * Client entrypoint for BetterPrisons.
 *
 * <p><strong>STUB.</strong> Config loading is wired up; the ported BetterPrisons client systems
 * (HUDs, features, CosmicApi wiring, ...) will be initialized here on top of the {@code :shared}
 * library.
 */
public class BetterPrisonsClient implements ClientModInitializer {

	/** Shared config (config/bettercosmic/shared.json) — the same instance every mod uses. */
	public static SharedConfig sharedConfig;

	/** BetterPrisons' own config (config/bettercosmic/betterprisons.json). */
	public static PrisonsConfig config;

	@Override
	public void onInitializeClient() {
		// Load the shared config and BetterPrisons' own config.
		sharedConfig = SharedConfig.get();
		config = BetterCosmicConfig.load(PrisonsConfig.class);

		BetterPrisons.LOGGER.info("Loaded configs: {} and {}",
				sharedConfig.configPath(), config.configPath());
	}
}
