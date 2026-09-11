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

	// ---- Player List HUD ----
	// A compact, small-text list of the other players online on the server (the Social Interactions
	// roster, connection.getOnlinePlayers(), excluding yourself), sorted alphabetically and laid out
	// in columns of a configurable height.

	/** Show the Player List HUD. */
	public boolean playerListHudEnabled = true;

	/** HUD position (GUI-scaled pixels), moved by the drag-and-drop HUD editor. */
	public int playerListHudX = 5;
	public int playerListHudY = 40;

	/** Render scale as a percent (100 = 1.0x). Defaults small since the list is meant to be compact. */
	public int playerListHudScale = 80;

	/** Show the "Players: N" header line above the columns (N is the full count, even when capped). */
	public boolean playerListShowHeader = true;

	/** Names per column; past this the list wraps into a new column to the right. */
	public int playerListEntriesPerColumn = 10;

	/** Horizontal gap (GUI pixels, pre-scale) between columns. */
	public int playerListColumnSpacing = 8;

	/** Hard cap on rows shown; overflow collapses into a trailing "+N more" line. */
	public int playerListMaxEntries = 60;

	/** RGB color of the player names. */
	public int playerListNameColor = 0xFFFFFF;

	/** RGB color of the "Players: N" header line. */
	public int playerListHeaderColor = 0xF1C40F;

	/** Background fill: RGB color + separate 0–255 opacity (0 = no background). */
	public int playerListBgColor = 0x000000;
	public int playerListBgOpacity = 120;

	/** Border: RGB color + separate 0–255 opacity + thickness in pixels (0 = no border). */
	public int playerListBorderColor = 0xFFFFFF;
	public int playerListBorderOpacity = 60;
	public int playerListBorderThickness = 1;
}
