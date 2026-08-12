package dev.nishu.bettercosmic.shared.config;

/**
 * Settings shared by every BetterCosmic mod, persisted to {@code config/bettercosmic/shared.json}.
 *
 * <p>Accessed through the {@link #get()} singleton so that BetterSky and BetterPrisons — which both
 * bundle the same {@code bettercosmicshared} library — read and write one shared instance rather
 * than each keeping their own copy of the same file.
 */
public class SharedConfig extends BetterCosmicConfig {

	private static SharedConfig instance;

	/** The single shared config instance, loaded from disk on first access. */
	public static SharedConfig get() {
		if (instance == null) {
			instance = BetterCosmicConfig.load(SharedConfig.class);
		}
		return instance;
	}

	@Override
	public String fileName() {
		return "shared.json";
	}

	// ---- Shared settings (apply to every BetterCosmic mod) ----

	/** Schema version, reserved for future migrations. */
	public int configVersion = 1;

	/**
	 * Developer mode. When off (the default), the shared dev/debug commands are hidden and
	 * unusable. Toggle it in-game with {@code /bdev}. See
	 * {@link dev.nishu.bettercosmic.shared.command.DevCommands}.
	 */
	public boolean developerMode = false;

	/** Format large numbers with commas (1,234,567) instead of compact suffixes (1.2M). */
	public boolean useCommaFormatting = false;

	// The UI theme is a strong shared candidate; a couple of representative colors live here for
	// now, with the full theme (borders, sidebar, tooltips, ...) landing here as the shared UI
	// framework is ported over from BetterPrisons.
	public int themeAccentColor = 0xFF4488;
	public int themeBackgroundColor = 0xE0101010;
}
