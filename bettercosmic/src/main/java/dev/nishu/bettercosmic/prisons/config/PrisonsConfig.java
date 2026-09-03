package dev.nishu.bettercosmic.prisons.config;

import dev.nishu.bettercosmic.shared.config.BetterCosmicConfig;

import java.util.HashMap;
import java.util.Map;

/**
 * BetterPrisons' own settings, persisted to {@code config/bettercosmic/betterprisons.json}.
 *
 * <p>Ported from BetterPrisons' single ~800-line {@code Config} god-class: the field set is kept
 * (plain public fields with defaults) but the hand-written per-field load/copy is gone — the shared
 * {@link BetterCosmicConfig} base handles (de)serialization with atomic writes and corrupt-file
 * backup. Genuinely cross-mod settings are <em>not</em> duplicated here: {@code developerMode},
 * {@code useCommaFormatting}, and the 8-token UI theme live on
 * {@link dev.nishu.bettercosmic.shared.config.SharedConfig} and are read from there. Number
 * formatting goes through {@link dev.nishu.bettercosmic.shared.util.NumberFormatUtil}.
 */
public class PrisonsConfig extends BetterCosmicConfig {

	@Override
	public String fileName() {
		return "betterprisons.json";
	}

	/** Schema version, reserved for future migrations. */
	public int configVersion = 1;

	// ---- HUD feature toggles ----
	public boolean cooldownHudEnabled = true;
	public boolean satchelHudEnabled = true;
	public boolean statsHudEnabled = true;
	public boolean enchantHudEnabled = true;
	public boolean eventsHudEnabled = true;
	public boolean peacefulMiningEnabled = true;

	// ---- Satchel HUD display ----
	public boolean satchelShowPercentage = false;
	public boolean combineSimilarSatchels = true;
	/** "Off" disables the fill filter; otherwise a percent like "75%". */
	public String satchelShowThreshold = "Off";
	public boolean satchelShowCount = true;
	public boolean satchelWhitescrollIndicators = true;
	/** Per-satchel-type visibility. Key = "<ore>_<variant>" or a special ("shard", "clue_scroll"). */
	public Map<String, Boolean> satchelTypeEnabled = new HashMap<>();

	// ---- HUD positions ----
	public int cooldownHudX = 6;
	public int cooldownHudY = 7;
	public int satchelHudX = 7;
	public int satchelHudY = 127;
	public int statsHudX = 535;
	public int statsHudY = 212;
	public int enchantHudX = 517;
	public int enchantHudY = 4;
	public int eventsHudX = 8;
	public int eventsHudY = 74;
	/** Toast corner: "Top Left", "Top Right", "Bottom Left", "Bottom Right". */
	public String toastCorner = "Bottom Right";

	// ---- Misc HUD colors ----
	public int cooldownBarColor = 0xFF00FF;
	public int satchelBarColor = 0xFF4488;

	// ---- Super Breaker Aura ----
	public int superBreakerBaseColor = 16386570;
	public int superBreakerBaseOpacity = 79;
	public int superBreakerLightColor = 1444602;
	public int superBreakerLightOpacity = 191;
	public boolean superBreakerAuraEnabled = false;
	public boolean superBreakerTimerEnabled = false;
	public int superBreakerTimerOffsetX = 0;
	public int superBreakerTimerOffsetY = -20;

	// ---- Peaceful Mining ----
	public int peacefulMiningOpacity = 50; // 0-255
	public int peacefulMiningDistance = 8;
	public boolean peacefulMiningDisableOnCombat = false;
	public boolean peacefulMiningPickaxe = true;
	public boolean peacefulMiningMace = true;
	public boolean peacefulMiningAlwaysInPrisonbreak = true;

	// ---- Pickaxe drop confirmation ----
	public boolean pickaxeDropConfirmationEnabled = true;
	public boolean pickaxeDropBlockEnabled = false;
	public boolean pickaxeDropDragBlockEnabled = false;

	// ---- Misc features ----
	public boolean autoTradeEnabled = true;
	public boolean boldXpEnergyTitles = false;
	public boolean useItemWhileMiningEnabled = true;
	public boolean chestSearchEnabled = true;
	public boolean clueScrollSortingEnabled = true;
	public int clueScrollNumberColor = 0x79FF7A;
	public boolean clueScrollNumberOutline = true;
	public boolean clueScrollUnmappedTooltipEnabled = true;
	public boolean prisonbreakTexturePackEnabled = true;
	public boolean blinkOverlayEnabled = true;
	public int blinkOverlayColor = 0x33FF33;
	public int blinkOverlayOpacity = 90;
	public int blinkOverlayOutlineColor = 0x00FF00;
	public int blinkOverlayOutlineThickness = 2;
	/** "Bundled" = the built-in pack; otherwise a resource-pack profile id. */
	public String prisonbreakTexturePack = "Bundled";
	public boolean enchantBookCostsEnabled = true;
	public int enchantBookCostsColor = 0xAA55FF;
	public boolean gangPointExpiryEnabled = true;
	public int gangPointExpiryColor = 0x55FFFF;

	// ---- Held item scaling (25-150%) ----
	public int heldItemPickaxeScale = 100;
	public int heldItemSwordScale = 100;
	public int heldItemAxeScale = 100;
	public int heldItemOtherScale = 100;

	// ---- EasyView ----
	public boolean easyViewEnabled = true;
	public boolean easyViewEnergyEnabled = true;
	public boolean easyViewMoneyEnabled = true;
	public boolean easyViewGangPointsEnabled = true;
	public boolean easyViewBlackScrollEnabled = true;
	public boolean easyViewChargeOrbEnabled = true;
	public boolean easyViewArmorEnabled = true;
	public boolean easyViewWeaponsEnabled = true;
	public boolean easyViewPickaxesEnabled = true;
	public boolean easyViewDustEnabled = true;
	public boolean easyViewPagesEnabled = true;
	public boolean easyViewPrestigeTokenEnabled = true;
	public boolean easyViewXpBottleEnabled = true;
	public int easyViewEnergyColor = 0x6EDDDD;
	public int easyViewMoneyColor = 0x00FF00;
	public int easyViewGangPointsColor = 65535;
	public int easyViewBlackScrollColor = 0xFF00FF;
	public int easyViewChargeOrbColor = 16755200;
	public int easyViewArmorColor = 0x00FF00;
	public int easyViewWeaponsColor = 0x00FF00;
	public int easyViewPickaxesColor = 0x00FF00;
	public int easyViewDustColor = 0xD2691E;
	public int easyViewPagesColor = 0xF5DEB3;
	public int easyViewPrestigeTokenColor = 0xFFD700;
	public int easyViewXpBottleColor = 0xFFFFFF;
	public int easyViewPickaxesScale = 70;
	public int easyViewWeaponsScale = 70;
	public int easyViewArmorScale = 70;
	public int easyViewEnergyScale = 50;
	public int easyViewMoneyScale = 50;
	public int easyViewGangPointsScale = 50;
	public int easyViewBlackScrollScale = 50;
	public int easyViewChargeOrbScale = 50;
	public int easyViewDustScale = 50;
	public int easyViewPagesScale = 50;
	public int easyViewPrestigeTokenScale = 50;
	public int easyViewXpBottleScale = 50;
	public boolean easyViewEnergyBold = true;
	public boolean easyViewMoneyBold = true;
	public boolean easyViewGangPointsBold = true;
	public boolean easyViewBlackScrollBold = true;
	public boolean easyViewChargeOrbBold = true;
	public boolean easyViewArmorBold = true;
	public boolean easyViewWeaponsBold = true;
	public boolean easyViewPickaxesBold = true;
	public boolean easyViewDustBold = true;
	public boolean easyViewPagesBold = true;
	public boolean easyViewPagesTierColor = true;
	public boolean easyViewPrestigeTokenBold = true;
	public boolean easyViewXpBottleBold = true;
	public boolean easyViewXpBottleTierColor = true;

	// ---- Item Cooldowns ----
	public boolean itemCooldownsEnabled = true;
	public boolean itemCooldownsPetEnabled = true;
	public int itemCooldownsPetCooldownColor = 0xFF5555;
	public int itemCooldownsPetActiveColor = 0x00FF00;
	public boolean itemCooldownsPetBold = true;
	public boolean itemCooldownsTrinketEnabled = true;
	public int itemCooldownsTrinketColor = 0xFF5555;
	public boolean itemCooldownsTrinketBold = true;
	public boolean itemCooldownsBanditBoxEnabled = true;
	public int itemCooldownsBanditBoxColor = 0x00FF00;
	public boolean itemCooldownsBanditBoxBold = true;

	// ---- HUD scaling (percent) ----
	public int cooldownHudScale = 100;
	public int satchelHudScale = 100;
	public int statsHudScale = 100;
	public int enchantHudScale = 100;
	public int eventsHudScale = 100;
	public int superBreakerAuraScale = 100;

	// ---- Cooldown HUD styling ----
	public int cooldownBgColor = 0x000000;
	public int cooldownBgOpacity = 128;
	public int cooldownBorderColor = 0xFFFFFF;
	public int cooldownBorderOpacity = 128;
	public int cooldownBorderThickness = 2;

	// ---- Satchel HUD styling ----
	public int satchelBgColor = 0x000000;
	public int satchelBgOpacity = 128;
	public int satchelBorderColor = 0xFFFFFF;
	public int satchelBorderOpacity = 128;
	public int satchelBorderThickness = 2;

	// ---- Stats HUD styling ----
	public int statsBgColor = 0x000000;
	public int statsBgOpacity = 128;
	public int statsBorderColor = 0xFFFFFF;
	public int statsBorderOpacity = 128;
	public int statsBorderThickness = 2;

	// ---- Enchant HUD styling ----
	public int enchantBgColor = 0x000000;
	public int enchantBgOpacity = 128;
	public int enchantBorderColor = 0xFFFFFF;
	public int enchantBorderOpacity = 128;
	public int enchantBorderThickness = 2;
	public int enchantTimeColor = 1045763;

	// ---- Events HUD styling ----
	public int eventsBgColor = 0x000000;
	public int eventsBgOpacity = 128;
	public int eventsBorderColor = 0xFFFFFF;
	public int eventsBorderOpacity = 128;
	public int eventsBorderThickness = 2;

	// ---- Stats HUD element toggles ----
	public boolean statsShowCurrentXP = false;
	public boolean statsShowXPPerHour = true;
	public boolean statsShowXPPerMinute = true;
	public boolean statsShowSessionXP = true;
	public boolean statsShowCurrentCE = false;
	public boolean statsShowCEPerHour = true;
	public boolean statsShowCEPerMinute = true;
	public boolean statsShowSessionCE = true;
	public boolean statsShowSessionDuration = true;
	public boolean statsShowMillisOnSessionDuration = false;
	public boolean statsShowTimeTillLevelUp = true;

	// ---- Stats HUD text colors ----
	public int statsCurrentXPColor = 1045763;
	public int statsXPPerHourColor = 1045763;
	public int statsXPPerMinuteColor = 1045763;
	public int statsSessionXPColor = 1045763;
	public int statsCurrentCEColor = 240124;
	public int statsCEPerHourColor = 240124;
	public int statsCEPerMinuteColor = 240124;
	public int statsSessionCEColor = 240124;
	public int statsSessionDurationColor = 14352636;
	public int statsTimeTillLevelUpColor = 0xFFD700;

	// ---- Satchel HUD capacity threshold colors ----
	public int satchelColorUnder20 = 1045763;
	public int satchelColor20to60 = 16776960;
	public int satchelColor60to95 = 16746496;
	public int satchelColor95Plus = 11141120;

	// ---- Cooldown command configs ----
	public boolean homeEnabled = true;
	public int homeColor = 1045763;
	public boolean jetEnabled = true;
	public int jetColor = 14576132;
	public boolean feedEnabled = true;
	public int feedColor = 6700312;
	public boolean fixEnabled = true;
	public int fixColor = 12632256;
	public boolean combatEnabled = true;
	public int combatColor = 9835026;
	public boolean tpaEnabled = true;
	public int tpaColor = 5636095;
	public boolean tpahereEnabled = true;
	public int tpahereColor = 5636095;
	public boolean dangleEnabled = true;
	public int dangleColor = 0xFFAA00;
	public boolean adangleEnabled = true;
	public int adangleColor = 0x55FFFF;
	public boolean nearEnabled = true;
	public int nearColor = 0x55FFFF;
	public boolean pulseEnabled = true;
	public int pulseColor = 0xFF5555;

	// ---- HUD titles (show/hide + color) ----
	public boolean showCooldownHudTitle = true;
	public int cooldownHudTitleColor = 14550187;
	public boolean showSatchelHudTitle = true;
	public int satchelHudTitleColor = 11722244;
	public boolean showStatsHudTitle = true;
	public int statsHudTitleColor = 14352636;
	public boolean showEnchantHudTitle = true;
	public int enchantHudTitleColor = 300510;
	public boolean showEventsHudTitle = true;
	public int eventsHudTitleColor = 14558468;

	// ---- Events HUD text/heading colors ----
	public int eventsTextColor = 14558468;
	public int eventsNaturalHeadingColor = 0x00FF00;
	public int eventsSummonedHeadingColor = 0xFF4500;

	// ---- Events HUD meteor toggles / icon / timing ----
	public boolean naturalMeteorsEnabled = true;
	public boolean summonedMeteorsEnabled = true;
	public String eventsIconItemId = "nether_quartz_ore";
	public int eventsCrashedDisplayDuration = 15;
	public boolean meteorShowDistance = true;
	public boolean merchantShowDistance = true;

	// ---- Merchant settings ----
	public boolean merchantsEnabled = true;
	public int merchantTimeoutMinutes = 20;
	public int merchantSlainDisplayDuration = 10;
	public boolean coalMerchantEnabled = true;
	public boolean ironMerchantEnabled = true;
	public boolean lapisMerchantEnabled = true;
	public boolean redstoneMerchantEnabled = true;
	public boolean goldMerchantEnabled = true;
	public boolean diamondMerchantEnabled = true;
	public boolean emeraldMerchantEnabled = true;
	public int coalMerchantHeadingColor = 0x555555;
	public int ironMerchantHeadingColor = 0xAAAAAA;
	public int lapisMerchantHeadingColor = 0x5555FF;
	public int redstoneMerchantHeadingColor = 0xFF5555;
	public int goldMerchantHeadingColor = 0xFFAA00;
	public int diamondMerchantHeadingColor = 0x55FFFF;
	public int emeraldMerchantHeadingColor = 0x55FF55;

	// ---- Bandit Rush ----
	public boolean banditRushEnabled = true;
	public int banditRushHeadingColor = 0xFFAA00;
	public int banditRushTextColor = 0xFFAAAA;
	public boolean banditRushShowDistance = true;
	public int banditRushTimeoutSeconds = 60;
	public int banditRushBeamOpacity = 160;
	public String banditRushIconItemId = "iron_sword";
	public boolean banditRushSoundEnabled = true;
	public String banditRushSound = "note_pling";
	public int banditRushSoundVolume = 100;
	public boolean waypointBanditRushEnabled = true;
	public boolean waypointBanditRushEdgeEnabled = true;

	// ---- Meteorite Shower ----
	public boolean meteoriteShowerEnabled = true;
	public int meteoriteShowerHeadingColor = 0xFF5500;
	public int meteoriteShowerTextColor = 0xFFAA88;
	public boolean meteoriteShowerShowDistance = true;
	public int meteoriteShowerTimeoutSeconds = 180;
	public int meteoriteShowerBeamOpacity = 160;
	public String meteoriteShowerIconItemId = "magma_block";
	public boolean meteoriteShowerSoundEnabled = true;
	public String meteoriteShowerSound = "ender_eye";
	public int meteoriteShowerSoundVolume = 100;
	public boolean waypointMeteoriteShowerEnabled = true;
	public boolean waypointMeteoriteShowerEdgeEnabled = true;

	// ---- Waypoints ----
	public boolean waypointsEnabled = true;
	public boolean waypointMeteorsEnabled = true;
	public boolean waypointMerchantsEnabled = true;
	public boolean waypointCustomEnabled = true;
	public boolean waypointMeteorsEdgeEnabled = true;
	public boolean waypointMerchantsEdgeEnabled = true;
	public boolean waypointCustomEdgeEnabled = true;
	public boolean beaconBeamsEnabled = true;
	public boolean beaconBeamThroughWalls = false;
	public int meteorBeamOpacity = 160;
	public int merchantBeamOpacity = 160;
	public int customWaypointDefaultOpacity = 255;
	public float customWaypointOnScreenScale = 1.0f;
	public float customWaypointOffScreenScale = 1.0f;

	// ---- Gang Ping ----
	public boolean gangPingEnabled = true;
	public int gangPingColor = 0xAA55FF;
	public int gangPingBaseOpacity = 200;
	public boolean gangPingBeamEnabled = true;
	public int gangPingBeamOpacity = 120;
	public boolean gangPingEdgeEnabled = false;
	public boolean gangPingSoundEnabled = true;
	public int gangPingSoundVolume = 80;
	public boolean gangPingShowName = true;
	public boolean gangPingShowTimer = false;
	public boolean gangPingShowCoords = true;
	public boolean gangPingShowHp = false;
	public boolean gangPingShowFacing = false;
	public boolean gangPingTextBackground = false;
	public float gangPingTextScale = 1.0f;
	public float gangPingIconMinScale = 0.5f;
	public float gangPingIconMaxScale = 1.5f;
	public boolean gangPingDistanceScaling = true;
	public boolean gangPingShowNonGang = false;

	// (Minimap HUD is out of scope — its config fields were dropped along with the feature.)

	// ---- Notifications (per-NotificationType.id, absent = registry default) ----
	public Map<String, Boolean> notificationEnabled = new HashMap<>();
	public Map<String, String> notificationSound = new HashMap<>();
	public Map<String, Integer> notificationVolume = new HashMap<>();

	// ---- Powerball alert title (enable/sound/volume live in the notifications maps) ----
	public boolean powerballAlertTitleEnabled = true;
	public String powerballAlertTitleText = "Powerball Ready!";
	public int powerballAlertTitleColor = 0xFFAA00;
}
