package dev.nishu.bettercosmic.prisons.easyview;

import dev.nishu.bettercosmic.prisons.client.BetterPrisonsClient;
import dev.nishu.bettercosmic.prisons.config.PrisonsConfig;
import dev.nishu.bettercosmic.prisons.ui.PrisonOptions;
import dev.nishu.bettercosmic.shared.ui.model.ConfigPanel;
import dev.nishu.bettercosmic.shared.ui.model.Option;
import dev.nishu.bettercosmic.shared.ui.model.OptionGroup;
import dev.nishu.bettercosmic.shared.ui.model.Options;
import dev.nishu.bettercosmic.shared.ui.model.PanelIcon;

import java.util.List;

/**
 * The EasyView config panel: a master toggle plus a per-item-type toggle and overlay color, grouped
 * by kind. Bound to {@link PrisonsConfig} via the shared {@code Options} lambdas.
 */
public final class EasyViewPanel {

	private EasyViewPanel() {}

	public static ConfigPanel create() {
		PrisonsConfig d = new PrisonsConfig();
		PrisonsConfig c = BetterPrisonsClient.config;

		OptionGroup general = new OptionGroup("General", List.of(
				Options.toggle("EasyView", d.easyViewEnabled,
						() -> c.easyViewEnabled, v -> { c.easyViewEnabled = v; c.save(); })
						.tooltip("Master toggle for all inventory overlays.")));

		OptionGroup value = new OptionGroup("Value notes", List.<Option>of(
				Options.toggle("Cosmic Energy", d.easyViewEnergyEnabled,
						() -> c.easyViewEnergyEnabled, v -> { c.easyViewEnergyEnabled = v; c.save(); }),
				PrisonOptions.colorRgb("Energy color", d.easyViewEnergyColor,
						() -> c.easyViewEnergyColor, v -> { c.easyViewEnergyColor = v; c.save(); }),
				Options.toggle("Energy: bold", d.easyViewEnergyBold,
						() -> c.easyViewEnergyBold, v -> { c.easyViewEnergyBold = v; c.save(); }),
				Options.intSlider("Energy scale", d.easyViewEnergyScale, 25, 150, 5,
						() -> c.easyViewEnergyScale, v -> { c.easyViewEnergyScale = v; c.save(); }),
				Options.toggle("Money notes", d.easyViewMoneyEnabled,
						() -> c.easyViewMoneyEnabled, v -> { c.easyViewMoneyEnabled = v; c.save(); }),
				PrisonOptions.colorRgb("Money color", d.easyViewMoneyColor,
						() -> c.easyViewMoneyColor, v -> { c.easyViewMoneyColor = v; c.save(); }),
				Options.toggle("Money: bold", d.easyViewMoneyBold,
						() -> c.easyViewMoneyBold, v -> { c.easyViewMoneyBold = v; c.save(); }),
				Options.intSlider("Money scale", d.easyViewMoneyScale, 25, 150, 5,
						() -> c.easyViewMoneyScale, v -> { c.easyViewMoneyScale = v; c.save(); }),
				Options.toggle("Gang points", d.easyViewGangPointsEnabled,
						() -> c.easyViewGangPointsEnabled, v -> { c.easyViewGangPointsEnabled = v; c.save(); }),
				PrisonOptions.colorRgb("Gang points color", d.easyViewGangPointsColor,
						() -> c.easyViewGangPointsColor, v -> { c.easyViewGangPointsColor = v; c.save(); }),
				Options.toggle("Gang points: bold", d.easyViewGangPointsBold,
						() -> c.easyViewGangPointsBold, v -> { c.easyViewGangPointsBold = v; c.save(); }),
				Options.intSlider("Gang points scale", d.easyViewGangPointsScale, 25, 150, 5,
						() -> c.easyViewGangPointsScale, v -> { c.easyViewGangPointsScale = v; c.save(); })));

		OptionGroup percent = new OptionGroup("Percent items", List.<Option>of(
				Options.toggle("Black scrolls", d.easyViewBlackScrollEnabled,
						() -> c.easyViewBlackScrollEnabled, v -> { c.easyViewBlackScrollEnabled = v; c.save(); }),
				PrisonOptions.colorRgb("Black scroll color", d.easyViewBlackScrollColor,
						() -> c.easyViewBlackScrollColor, v -> { c.easyViewBlackScrollColor = v; c.save(); }),
				Options.toggle("Black scroll: bold", d.easyViewBlackScrollBold,
						() -> c.easyViewBlackScrollBold, v -> { c.easyViewBlackScrollBold = v; c.save(); }),
				Options.intSlider("Black scroll scale", d.easyViewBlackScrollScale, 25, 150, 5,
						() -> c.easyViewBlackScrollScale, v -> { c.easyViewBlackScrollScale = v; c.save(); }),
				Options.toggle("Charge orbs", d.easyViewChargeOrbEnabled,
						() -> c.easyViewChargeOrbEnabled, v -> { c.easyViewChargeOrbEnabled = v; c.save(); }),
				PrisonOptions.colorRgb("Charge orb color", d.easyViewChargeOrbColor,
						() -> c.easyViewChargeOrbColor, v -> { c.easyViewChargeOrbColor = v; c.save(); }),
				Options.toggle("Charge orb: bold", d.easyViewChargeOrbBold,
						() -> c.easyViewChargeOrbBold, v -> { c.easyViewChargeOrbBold = v; c.save(); }),
				Options.intSlider("Charge orb scale", d.easyViewChargeOrbScale, 25, 150, 5,
						() -> c.easyViewChargeOrbScale, v -> { c.easyViewChargeOrbScale = v; c.save(); }),
				Options.toggle("Dust", d.easyViewDustEnabled,
						() -> c.easyViewDustEnabled, v -> { c.easyViewDustEnabled = v; c.save(); }),
				PrisonOptions.colorRgb("Dust color", d.easyViewDustColor,
						() -> c.easyViewDustColor, v -> { c.easyViewDustColor = v; c.save(); }),
				Options.toggle("Dust: bold", d.easyViewDustBold,
						() -> c.easyViewDustBold, v -> { c.easyViewDustBold = v; c.save(); }),
				Options.intSlider("Dust scale", d.easyViewDustScale, 25, 150, 5,
						() -> c.easyViewDustScale, v -> { c.easyViewDustScale = v; c.save(); }),
				Options.toggle("Pages", d.easyViewPagesEnabled,
						() -> c.easyViewPagesEnabled, v -> { c.easyViewPagesEnabled = v; c.save(); }),
				PrisonOptions.colorRgb("Pages color", d.easyViewPagesColor,
						() -> c.easyViewPagesColor, v -> { c.easyViewPagesColor = v; c.save(); }),
				Options.toggle("Pages: bold", d.easyViewPagesBold,
						() -> c.easyViewPagesBold, v -> { c.easyViewPagesBold = v; c.save(); }),
				Options.intSlider("Pages scale", d.easyViewPagesScale, 25, 150, 5,
						() -> c.easyViewPagesScale, v -> { c.easyViewPagesScale = v; c.save(); }),
				Options.toggle("Pages: tier color", d.easyViewPagesTierColor,
						() -> c.easyViewPagesTierColor, v -> { c.easyViewPagesTierColor = v; c.save(); })
						.tooltip("Color the page overlay by its tier instead of a fixed color.")));

		OptionGroup gear = new OptionGroup("Gear levels", List.<Option>of(
				Options.toggle("Armor", d.easyViewArmorEnabled,
						() -> c.easyViewArmorEnabled, v -> { c.easyViewArmorEnabled = v; c.save(); }),
				PrisonOptions.colorRgb("Armor color", d.easyViewArmorColor,
						() -> c.easyViewArmorColor, v -> { c.easyViewArmorColor = v; c.save(); }),
				Options.toggle("Armor: bold", d.easyViewArmorBold,
						() -> c.easyViewArmorBold, v -> { c.easyViewArmorBold = v; c.save(); }),
				Options.intSlider("Armor scale", d.easyViewArmorScale, 25, 150, 5,
						() -> c.easyViewArmorScale, v -> { c.easyViewArmorScale = v; c.save(); }),
				Options.toggle("Weapons", d.easyViewWeaponsEnabled,
						() -> c.easyViewWeaponsEnabled, v -> { c.easyViewWeaponsEnabled = v; c.save(); }),
				PrisonOptions.colorRgb("Weapons color", d.easyViewWeaponsColor,
						() -> c.easyViewWeaponsColor, v -> { c.easyViewWeaponsColor = v; c.save(); }),
				Options.toggle("Weapons: bold", d.easyViewWeaponsBold,
						() -> c.easyViewWeaponsBold, v -> { c.easyViewWeaponsBold = v; c.save(); }),
				Options.intSlider("Weapons scale", d.easyViewWeaponsScale, 25, 150, 5,
						() -> c.easyViewWeaponsScale, v -> { c.easyViewWeaponsScale = v; c.save(); }),
				Options.toggle("Pickaxes", d.easyViewPickaxesEnabled,
						() -> c.easyViewPickaxesEnabled, v -> { c.easyViewPickaxesEnabled = v; c.save(); }),
				PrisonOptions.colorRgb("Pickaxes color", d.easyViewPickaxesColor,
						() -> c.easyViewPickaxesColor, v -> { c.easyViewPickaxesColor = v; c.save(); }),
				Options.toggle("Pickaxes: bold", d.easyViewPickaxesBold,
						() -> c.easyViewPickaxesBold, v -> { c.easyViewPickaxesBold = v; c.save(); }),
				Options.intSlider("Pickaxes scale", d.easyViewPickaxesScale, 25, 150, 5,
						() -> c.easyViewPickaxesScale, v -> { c.easyViewPickaxesScale = v; c.save(); })));

		OptionGroup misc = new OptionGroup("Misc", List.<Option>of(
				Options.toggle("Prestige tokens", d.easyViewPrestigeTokenEnabled,
						() -> c.easyViewPrestigeTokenEnabled, v -> { c.easyViewPrestigeTokenEnabled = v; c.save(); }),
				PrisonOptions.colorRgb("Prestige token color", d.easyViewPrestigeTokenColor,
						() -> c.easyViewPrestigeTokenColor, v -> { c.easyViewPrestigeTokenColor = v; c.save(); }),
				Options.toggle("Prestige token: bold", d.easyViewPrestigeTokenBold,
						() -> c.easyViewPrestigeTokenBold, v -> { c.easyViewPrestigeTokenBold = v; c.save(); }),
				Options.intSlider("Prestige token scale", d.easyViewPrestigeTokenScale, 25, 150, 5,
						() -> c.easyViewPrestigeTokenScale, v -> { c.easyViewPrestigeTokenScale = v; c.save(); }),
				Options.toggle("XP bottles", d.easyViewXpBottleEnabled,
						() -> c.easyViewXpBottleEnabled, v -> { c.easyViewXpBottleEnabled = v; c.save(); }),
				PrisonOptions.colorRgb("XP bottle color", d.easyViewXpBottleColor,
						() -> c.easyViewXpBottleColor, v -> { c.easyViewXpBottleColor = v; c.save(); }),
				Options.toggle("XP bottle: bold", d.easyViewXpBottleBold,
						() -> c.easyViewXpBottleBold, v -> { c.easyViewXpBottleBold = v; c.save(); }),
				Options.intSlider("XP bottle scale", d.easyViewXpBottleScale, 25, 150, 5,
						() -> c.easyViewXpBottleScale, v -> { c.easyViewXpBottleScale = v; c.save(); }),
				Options.toggle("XP bottle: tier color", d.easyViewXpBottleTierColor,
						() -> c.easyViewXpBottleTierColor, v -> { c.easyViewXpBottleTierColor = v; c.save(); })
						.tooltip("Color the XP-bottle overlay by its tier.")));

		OptionGroup cooldowns = new OptionGroup("Item cooldowns", List.<Option>of(
				Options.toggle("Item cooldown timers", d.itemCooldownsEnabled,
						() -> c.itemCooldownsEnabled, v -> { c.itemCooldownsEnabled = v; c.save(); })
						.tooltip("Show a m:ss timer on pets, trinkets, and bandit boxes."),
				Options.toggle("Pets", d.itemCooldownsPetEnabled,
						() -> c.itemCooldownsPetEnabled, v -> { c.itemCooldownsPetEnabled = v; c.save(); }),
				PrisonOptions.colorRgb("Pet cooldown color", d.itemCooldownsPetCooldownColor,
						() -> c.itemCooldownsPetCooldownColor, v -> { c.itemCooldownsPetCooldownColor = v; c.save(); })
						.tooltip("Timer color while the pet is on cooldown."),
				PrisonOptions.colorRgb("Pet active color", d.itemCooldownsPetActiveColor,
						() -> c.itemCooldownsPetActiveColor, v -> { c.itemCooldownsPetActiveColor = v; c.save(); })
						.tooltip("Timer color while the pet's effect is active."),
				Options.toggle("Pet: bold", d.itemCooldownsPetBold,
						() -> c.itemCooldownsPetBold, v -> { c.itemCooldownsPetBold = v; c.save(); }),
				Options.toggle("Trinkets", d.itemCooldownsTrinketEnabled,
						() -> c.itemCooldownsTrinketEnabled, v -> { c.itemCooldownsTrinketEnabled = v; c.save(); }),
				PrisonOptions.colorRgb("Trinket color", d.itemCooldownsTrinketColor,
						() -> c.itemCooldownsTrinketColor, v -> { c.itemCooldownsTrinketColor = v; c.save(); }),
				Options.toggle("Trinket: bold", d.itemCooldownsTrinketBold,
						() -> c.itemCooldownsTrinketBold, v -> { c.itemCooldownsTrinketBold = v; c.save(); }),
				Options.toggle("Bandit boxes", d.itemCooldownsBanditBoxEnabled,
						() -> c.itemCooldownsBanditBoxEnabled, v -> { c.itemCooldownsBanditBoxEnabled = v; c.save(); }),
				PrisonOptions.colorRgb("Bandit box color", d.itemCooldownsBanditBoxColor,
						() -> c.itemCooldownsBanditBoxColor, v -> { c.itemCooldownsBanditBoxColor = v; c.save(); }),
				Options.toggle("Bandit box: bold", d.itemCooldownsBanditBoxBold,
						() -> c.itemCooldownsBanditBoxBold, v -> { c.itemCooldownsBanditBoxBold = v; c.save(); })));

		return ConfigPanel.of("prisons-easyview", "EasyView",
				"Inventory value & level overlays", PanelIcon.EYE,
				List.of(general, value, percent, gear, misc, cooldowns));
	}
}
