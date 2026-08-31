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
	 * unusable. Toggle it in-game with {@code /bcdev}. See
	 * {@link dev.nishu.bettercosmic.shared.command.DevCommands}.
	 */
	public boolean developerMode = false;

	/**
	 * Restrict each mod's features to its own Cosmic network — BetterPrisons only on
	 * {@code cosmicprisons.com}, BetterSky only on {@code cosmicsky.net}. When on (the default),
	 * everywhere else only the config UI is available. Turn off to run every feature on every server
	 * (legacy behaviour). See {@link dev.nishu.bettercosmic.shared.server.ServerContext}.
	 */
	public boolean restrictFeaturesToServer = true;

	/** Format large numbers with commas (1,234,567) instead of compact suffixes (1.2M). */
	public boolean useCommaFormatting = false;

	// ---- Auto-updater (phase 1: detect + notify) ----

	/**
	 * Check GitHub for a newer BetterCosmic build on launch and surface it (toast + config row +
	 * ModMenu badge). On by default; a static-JSON read, no jar is downloaded. See
	 * {@link dev.nishu.bettercosmic.shared.update.UpdateChecker}.
	 */
	public boolean autoUpdateCheck = true;

	/**
	 * Automatically download, verify, and install updates (phase 2). Off by default. When on, a found
	 * update is downloaded + SHA-256-verified, then a detached helper installs it on game exit (a running
	 * JVM can't replace its own locked jar). See {@link dev.nishu.bettercosmic.shared.update.UpdateApplier}.
	 */
	public boolean autoUpdateApply = false;

	/**
	 * The config profile (network) last viewed in the config screen, as a {@link
	 * dev.nishu.bettercosmic.shared.server.Network} name. Used as the selector's default when the
	 * client isn't connected to a recognised network; {@code null} until the player first switches.
	 */
	public String lastConfigProfile = null;

	// ---- UI theme (compact token set, shared + persisted) ----
	// The config UI derives its entire look from these ARGB tokens; see
	// dev.nishu.bettercosmic.shared.ui.core.Theme, which copies them into static fields at load and
	// re-reads them when a token is edited in the General panel (live repaint). On/off, selection,
	// and focus states all derive from the accent, so there are deliberately no separate per-widget
	// colors.
	public int themeGround = 0xE6070810;       // screen dim / deepest ground
	public int themeSurface = 0xB80E121C;       // panels, cards, popup bodies
	public int themeSurfaceHover = 0xC8151B2A;  // hovered card / widget background
	public int themeLine = 0x2996ACD2;          // 1px hairline borders
	public int themeText = 0xFFE7ECF4;          // primary text
	public int themeMuted = 0xFF8B95A9;         // secondary text / labels
	public int themeFaint = 0xFF545D70;         // disabled / placeholders

	// The accent (on-states, selection, focus, slider fill) is per-network so each mod has its own
	// identity: warm orange on Cosmic Prisons, golden yellow on Cosmic Sky. Theme picks the field for
	// the active/viewed profile; the General panel exposes both so either can be recolored.
	public int themeAccentPrisons = 0xFFFF6F00; // Prisons — orange
	public int themeAccentSky = 0xFFF1C40F;      // Sky — yellow
}
