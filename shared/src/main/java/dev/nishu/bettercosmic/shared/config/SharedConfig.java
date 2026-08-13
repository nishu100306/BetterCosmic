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

	// ---- UI theme (compact token set, shared + persisted) ----
	// The config UI derives its entire look from these eight ARGB tokens; see
	// dev.nishu.bettercosmic.shared.ui.core.Theme, which copies them into static fields at load and
	// re-reads them when a token is edited in the General panel (live repaint). On/off, selection,
	// and focus states all derive from `themeAccent`, so there are deliberately no separate
	// per-widget colors. Defaults are the approved cosmic-sky palette.
	public int themeGround = 0xE6070810;       // screen dim / deepest ground
	public int themeSurface = 0xB80E121C;       // panels, cards, popup bodies
	public int themeSurfaceHover = 0xC8151B2A;  // hovered card / widget background
	public int themeLine = 0x2996ACD2;          // 1px hairline borders
	public int themeAccent = 0xFF57D4E6;        // on-states, selection, focus, slider fill
	public int themeText = 0xFFE7ECF4;          // primary text
	public int themeMuted = 0xFF8B95A9;         // secondary text / labels
	public int themeFaint = 0xFF545D70;         // disabled / placeholders
}
