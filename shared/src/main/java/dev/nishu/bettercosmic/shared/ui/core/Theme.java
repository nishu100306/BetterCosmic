package dev.nishu.bettercosmic.shared.ui.core;

import dev.nishu.bettercosmic.shared.config.SharedConfig;

/**
 * The config UI's compact color theme: eight packed {@code 0xAARRGGBB} tokens, mirrored from
 * {@link SharedConfig} into static fields for cheap access during rendering.
 *
 * <p>Replaces BetterPrisons' 27-field reflection-driven theme. On/off, selection, and focus states
 * all derive from {@link #accent}, so there are deliberately no separate per-widget colors.
 *
 * <p>Call {@link #load()} once at UI open and again whenever a theme token is edited in the General
 * panel — the General panel's color options write the {@link SharedConfig} field, save, then call
 * {@link #load()} so the change repaints immediately.
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
	/** On-states, selection, focus, slider fill. */
	public static int accent = 0xFF57D4E6;
	/** Primary text. */
	public static int text = 0xFFE7ECF4;
	/** Secondary text / labels. */
	public static int muted = 0xFF8B95A9;
	/** Disabled / placeholders. */
	public static int faint = 0xFF545D70;

	/** Copies the current {@link SharedConfig} theme fields into the static tokens. */
	public static void load() {
		SharedConfig c = SharedConfig.get();
		ground = c.themeGround;
		surface = c.themeSurface;
		surfaceHover = c.themeSurfaceHover;
		line = c.themeLine;
		accent = c.themeAccent;
		text = c.themeText;
		muted = c.themeMuted;
		faint = c.themeFaint;
	}
}
