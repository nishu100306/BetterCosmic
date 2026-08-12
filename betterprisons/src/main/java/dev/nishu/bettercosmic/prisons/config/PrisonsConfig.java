package dev.nishu.bettercosmic.prisons.config;

import dev.nishu.bettercosmic.shared.config.BetterCosmicConfig;

/**
 * BetterPrisons' own settings, persisted to {@code config/bettercosmic/betterprisons.json}.
 * Genuinely cross-mod settings belong in
 * {@link dev.nishu.bettercosmic.shared.config.SharedConfig} instead.
 */
public class PrisonsConfig extends BetterCosmicConfig {

	@Override
	public String fileName() {
		return "betterprisons.json";
	}

	// ---- BetterPrisons settings ----
	// The ported BetterPrisons settings land here as features are moved over; anything generic
	// enough to also apply to BetterSky belongs in SharedConfig.

	/** Schema version, reserved for future migrations. */
	public int configVersion = 1;
}
