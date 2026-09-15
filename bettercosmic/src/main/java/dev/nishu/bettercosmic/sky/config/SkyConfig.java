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
	// A compact, small-text list of the other players on the tab list (connection.getListedOnlinePlayers(),
	// excluding yourself), sorted alphabetically and laid out in columns of a configurable height.

	/** Show the Player List HUD. */
	public boolean playerListHudEnabled = true;

	/** HUD position (GUI-scaled pixels), moved by the drag-and-drop HUD editor. */
	public int playerListHudX = 5;
	public int playerListHudY = 40;

	/** Render scale as a percent (100 = 1.0x). Defaults small since the list is meant to be compact. */
	public int playerListHudScale = 55;

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

	// ---- Pet Cooldown overlay ----
	// A centered m:ss timer on inventory pets (custom_data.persistentItem == "inventory_pet"),
	// anchored off custom_data.lastUsed: green while the ability effect is active, red while on
	// cooldown. See dev.nishu.bettercosmic.sky.feature.PetCooldownProvider.

	/** Show the pet cooldown / active-effect timer. */
	public boolean petCooldownOverlay = true;

	/** RGB color of the timer while the pet is on cooldown. */
	public int petCooldownColor = 0xFF5555;

	/** RGB color of the timer while the pet's ability effect is still active. */
	public int petActiveColor = 0x00FF00;

	/** Render the timer text bold. */
	public boolean petCooldownBold = true;

	// ---- Chest search ----

	/**
	 * Search bar + no-code filter sidebar in container screens, highlighting matching slots. The
	 * shared framework provides the UI + name/lore filtering; BetterSky adds no item-specific filter
	 * types (Cosmic Sky has no enchant books or clue scrolls). See
	 * {@link dev.nishu.bettercosmic.shared.chestsearch.ChestSearchRegistry}.
	 */
	public boolean chestSearchEnabled = true;

	// ---- Auto-trade ----

	/** Shift-right-click another player to send {@code /trade <name>}. */
	public boolean autoTradeEnabled = true;

	// ---- Damage indicators ----
	// Floating combat numbers over nearby entities, inferred from client-visible health changes:
	// red -N on damage, green +N on a heal. See dev.nishu.bettercosmic.sky.feature.DamageIndicators.

	/** Show floating damage/heal numbers over nearby entities. */
	public boolean damageIndicatorsEnabled = true;

	/** Only entities within this many blocks of the player get indicators. */
	public int damageIndicatorRadius = 10;

	/** RGB color of a damage indicator ({@code -N}). Pure red. */
	public int damageIndicatorColor = 0xFF0000;

	/** RGB color of a heal indicator ({@code +N}). Pure green. */
	public int healIndicatorColor = 0x00FF00;

	// ---- Quest points ----
	// EasyView corner overlay + expiry tooltip for quest-point notes — both the regular
	// (custom_data.cosmicItem == "island_quest_point_note") and adventure
	// ("adventure_island_quest_point_note") variants, which share this config. See
	// dev.nishu.bettercosmic.sky.feature.QuestPointProvider / QuestPointTooltip.

	/** Show the point value in the corner of quest-point notes. */
	public boolean questPointOverlayEnabled = true;

	/** RGB color of the point-value overlay. */
	public int questPointColor = 0x55FFFF;

	/** Overlay text scale, as a percent (100 = normal). */
	public int questPointScale = 50;

	/** Render the overlay bold. */
	public boolean questPointBold = true;

	/** Append a time-remaining + local-timezone expiry line to the note's tooltip. */
	public boolean questPointExpiryEnabled = true;

	/** RGB color of the expiry tooltip lines. */
	public int questPointExpiryColor = 0xFF5555;

	// ---- Money notes ----
	// EasyView corner overlay for money notes (custom_data.cosmicItem == "money_note"), showing
	// custom_data.noteValue. See dev.nishu.bettercosmic.sky.feature.MoneyNoteProvider.

	/** Show the dollar value in the corner of money notes. */
	public boolean moneyNoteOverlayEnabled = true;

	/** RGB color of the money-note overlay. */
	public int moneyNoteColor = 0x55FF55;

	/** Overlay text scale, as a percent (100 = normal). */
	public int moneyNoteScale = 50;

	/** Render the overlay bold. */
	public boolean moneyNoteBold = true;

	// ---- EXP bottles ----
	// EasyView corner overlay for EXP bottles (custom_data.cosmicItem == "exp_bottle"), showing
	// custom_data.expValue. See dev.nishu.bettercosmic.sky.feature.XpBottleProvider.

	/** Show the XP value in the corner of EXP bottles. */
	public boolean xpBottleOverlayEnabled = true;

	/** RGB color of the EXP-bottle overlay. */
	public int xpBottleColor = 0xFF55FF;

	/** Overlay text scale, as a percent (100 = normal). */
	public int xpBottleScale = 50;

	/** Render the overlay bold. */
	public boolean xpBottleBold = true;

	// ---- Tracker HUD ----
	// Counts what you earn this session by tier (Basic/Elite/Legendary/Godly/Heroic/Mythic), with a
	// session timer and on-HUD Pause/Reset buttons plus a mode toggle. Fed from chat: Island Quests
	// ("… Quest COMPLETE: <Tier> …") in Quest mode and adventure chests ("<Tier> Chest dropped nearby!")
	// in Adventure mode. See dev.nishu.bettercosmic.sky.hud.TrackerHud.

	/** Show the Tracker HUD. */
	public boolean trackerHudEnabled = true;

	/** HUD position (GUI-scaled pixels), moved by the drag-and-drop HUD editor. */
	public int trackerHudX = 5;
	public int trackerHudY = 90;

	/** Render scale as a percent (100 = 1.0x). Defaults small to keep the tracker compact. */
	public int trackerHudScale = 65;

	/** Show the session timer line. */
	public boolean trackerShowTimer = true;

	/** Hide tiers whose count is still 0 (so the HUD only lists tiers you've completed). */
	public boolean trackerHideEmpty = false;

	/** RGB color of the "Quest Tracker" title. */
	public int trackerTitleColor = 0xF1C40F;

	/** RGB color of the session-timer line. */
	public int trackerTimerColor = 0xFFFFFF;

	/** Background fill: RGB color + separate 0–255 opacity (0 = no background). */
	public int trackerBgColor = 0x000000;
	public int trackerBgOpacity = 120;

	/** Border: RGB color + separate 0–255 opacity + thickness in pixels (0 = no border). */
	public int trackerBorderColor = 0xFFFFFF;
	public int trackerBorderOpacity = 60;
	public int trackerBorderThickness = 1;
}
