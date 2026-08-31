package dev.nishu.bettercosmic.shared.ui.core;

import dev.nishu.bettercosmic.shared.config.SharedConfig;
import dev.nishu.bettercosmic.shared.server.Network;
import dev.nishu.bettercosmic.shared.server.ServerContext;

/**
 * The config UI's compact color theme: packed {@code 0xAARRGGBB} tokens, mirrored from
 * {@link SharedConfig} into static fields for cheap access during rendering.
 *
 * <p>Replaces BetterPrisons' 27-field reflection-driven theme. On/off, selection, and focus states
 * all derive from {@link #accent}, so there are deliberately no separate per-widget colors.
 *
 * <p><b>Per-profile accent.</b> The accent is network-scoped — orange on Prisons, yellow on Sky — so
 * each mod has its own identity. {@link #load()} resolves which one to use from {@link #profile}: the
 * config screen {@linkplain #setProfile pins} the profile it's showing, and everything else leaves it
 * {@code null} so the accent auto-follows the connected server (see {@link #resolveProfile()}).
 *
 * <p>Call {@link #load()} at UI open, whenever a theme token is edited in the General panel (live
 * repaint), and once per frame in-world (via {@code HudRenderer}) so the accent tracks the server.
 */
public final class Theme {

	private Theme() {}

	/** Screen dim / deepest ground. */
	public static int ground = 0xE6070810;
	/** Panels, cards, popup bodies. */
	public static int surface = 0xB80E121C;
	/** Hovered card / widget background. */
	public static int surfaceHover = 0xC8151B2A;
	/** 1px hairline borders. */
	public static int line = 0x2996ACD2;
	/** On-states, selection, focus, slider fill. Resolved per {@link #profile} at {@link #load()}. */
	public static int accent = 0xFFF08A2B;
	/** Primary text. */
	public static int text = 0xFFE7ECF4;
	/** Secondary text / labels. */
	public static int muted = 0xFF8B95A9;
	/** Disabled / placeholders. */
	public static int faint = 0xFF545D70;

	/** Pinned profile whose accent to use, or {@code null} to auto-follow the connected server. */
	private static Network profile = null;

	/** Pins the profile whose accent should show (the config screen calls this), then reloads. */
	public static void setProfile(Network network) {
		profile = network;
		load();
	}

	/** The accent-driving profile: the pinned one, else the server/override, else last-viewed, else Prisons. */
	public static Network resolveProfile() {
		if (profile != null) {
			return profile;
		}
		Network n = ServerContext.override();
		if (n == null) {
			n = ServerContext.detected();
		}
		if (n == null) {
			String saved = SharedConfig.get().lastConfigProfile;
			if (saved != null) {
				try {
					n = Network.valueOf(saved);
				} catch (IllegalArgumentException ignored) {
					// stale/renamed value — fall through
				}
			}
		}
		return n != null ? n : Network.PRISONS;
	}

	/** Copies the current {@link SharedConfig} theme fields into the static tokens. */
	public static void load() {
		SharedConfig c = SharedConfig.get();
		ground = c.themeGround;
		surface = c.themeSurface;
		surfaceHover = c.themeSurfaceHover;
		line = c.themeLine;
		text = c.themeText;
		muted = c.themeMuted;
		faint = c.themeFaint;
		accent = resolveProfile() == Network.SKY ? c.themeAccentSky : c.themeAccentPrisons;
	}
}
