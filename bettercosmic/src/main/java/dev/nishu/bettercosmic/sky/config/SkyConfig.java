package dev.nishu.bettercosmic.sky.config;

import dev.nishu.bettercosmic.shared.config.BetterCosmicConfig;

/**
 * BetterSky's own settings, persisted to {@code config/bettercosmic/bettersky.json}.
 * Genuinely cross-mod settings belong in
 * {@link dev.nishu.bettercosmic.shared.config.SharedConfig} instead.
 */
public class SkyConfig extends BetterCosmicConfig {

	@Override
	public String fileName() {
		return "bettersky.json";
	}

	// ---- BetterSky settings ----
	// Feature settings (HUD toggles/positions, colors, feature flags, ...) are added here as
	// BetterSky features are built.

	/** Schema version, reserved for future migrations. */
	public int configVersion = 1;

	/**
	 * Show a potion trinket's remaining usable charges — min(current charges, max uses per world) —
	 * in the bottom-left of its inventory slot.
	 */
	public boolean trinketChargesOverlay = true;

	/** Text scale of the trinket-charges overlay number. */
	public double trinketChargesScale = 0.7;

	/**
	 * Where in the slot the trinket-charges number sits — one of the friendly labels
	 * "Top-left", "Top-right", "Bottom-left", "Bottom-right", "Center".
	 */
	public String trinketChargesAnchor = "Bottom-left";

	/**
	 * Where the overlay number's color comes from: "Potion color" (from the trinket's potion) or
	 * "Custom" (always {@link #trinketChargesColor}).
	 */
	public String trinketColorSource = "Potion color";

	/** RGB color of the trinket-charges overlay number (used when the source is "Custom"). */
	public int trinketChargesColor = 0xFFFFFF;
}
