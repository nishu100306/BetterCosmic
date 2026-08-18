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
				Options.toggle("Money notes", d.easyViewMoneyEnabled,
						() -> c.easyViewMoneyEnabled, v -> { c.easyViewMoneyEnabled = v; c.save(); }),
				PrisonOptions.colorRgb("Money color", d.easyViewMoneyColor,
						() -> c.easyViewMoneyColor, v -> { c.easyViewMoneyColor = v; c.save(); }),
				Options.toggle("Gang points", d.easyViewGangPointsEnabled,
						() -> c.easyViewGangPointsEnabled, v -> { c.easyViewGangPointsEnabled = v; c.save(); }),
				PrisonOptions.colorRgb("Gang points color", d.easyViewGangPointsColor,
						() -> c.easyViewGangPointsColor, v -> { c.easyViewGangPointsColor = v; c.save(); })));

		OptionGroup percent = new OptionGroup("Percent items", List.<Option>of(
				Options.toggle("Black scrolls", d.easyViewBlackScrollEnabled,
						() -> c.easyViewBlackScrollEnabled, v -> { c.easyViewBlackScrollEnabled = v; c.save(); }),
				PrisonOptions.colorRgb("Black scroll color", d.easyViewBlackScrollColor,
						() -> c.easyViewBlackScrollColor, v -> { c.easyViewBlackScrollColor = v; c.save(); }),
				Options.toggle("Charge orbs", d.easyViewChargeOrbEnabled,
						() -> c.easyViewChargeOrbEnabled, v -> { c.easyViewChargeOrbEnabled = v; c.save(); }),
				PrisonOptions.colorRgb("Charge orb color", d.easyViewChargeOrbColor,
						() -> c.easyViewChargeOrbColor, v -> { c.easyViewChargeOrbColor = v; c.save(); }),
				Options.toggle("Dust", d.easyViewDustEnabled,
						() -> c.easyViewDustEnabled, v -> { c.easyViewDustEnabled = v; c.save(); }),
				PrisonOptions.colorRgb("Dust color", d.easyViewDustColor,
						() -> c.easyViewDustColor, v -> { c.easyViewDustColor = v; c.save(); }),
				Options.toggle("Pages", d.easyViewPagesEnabled,
						() -> c.easyViewPagesEnabled, v -> { c.easyViewPagesEnabled = v; c.save(); }),
				PrisonOptions.colorRgb("Pages color", d.easyViewPagesColor,
						() -> c.easyViewPagesColor, v -> { c.easyViewPagesColor = v; c.save(); }),
				Options.toggle("Pages: tier color", d.easyViewPagesTierColor,
						() -> c.easyViewPagesTierColor, v -> { c.easyViewPagesTierColor = v; c.save(); })
						.tooltip("Color the page overlay by its tier instead of a fixed color.")));

		OptionGroup gear = new OptionGroup("Gear levels", List.<Option>of(
				Options.toggle("Armor", d.easyViewArmorEnabled,
						() -> c.easyViewArmorEnabled, v -> { c.easyViewArmorEnabled = v; c.save(); }),
				PrisonOptions.colorRgb("Armor color", d.easyViewArmorColor,
						() -> c.easyViewArmorColor, v -> { c.easyViewArmorColor = v; c.save(); }),
				Options.toggle("Weapons", d.easyViewWeaponsEnabled,
						() -> c.easyViewWeaponsEnabled, v -> { c.easyViewWeaponsEnabled = v; c.save(); }),
				PrisonOptions.colorRgb("Weapons color", d.easyViewWeaponsColor,
						() -> c.easyViewWeaponsColor, v -> { c.easyViewWeaponsColor = v; c.save(); }),
				Options.toggle("Pickaxes", d.easyViewPickaxesEnabled,
						() -> c.easyViewPickaxesEnabled, v -> { c.easyViewPickaxesEnabled = v; c.save(); }),
				PrisonOptions.colorRgb("Pickaxes color", d.easyViewPickaxesColor,
						() -> c.easyViewPickaxesColor, v -> { c.easyViewPickaxesColor = v; c.save(); })));

		OptionGroup misc = new OptionGroup("Misc", List.<Option>of(
				Options.toggle("Prestige tokens", d.easyViewPrestigeTokenEnabled,
						() -> c.easyViewPrestigeTokenEnabled, v -> { c.easyViewPrestigeTokenEnabled = v; c.save(); }),
				PrisonOptions.colorRgb("Prestige token color", d.easyViewPrestigeTokenColor,
						() -> c.easyViewPrestigeTokenColor, v -> { c.easyViewPrestigeTokenColor = v; c.save(); }),
				Options.toggle("XP bottles", d.easyViewXpBottleEnabled,
						() -> c.easyViewXpBottleEnabled, v -> { c.easyViewXpBottleEnabled = v; c.save(); }),
				PrisonOptions.colorRgb("XP bottle color", d.easyViewXpBottleColor,
						() -> c.easyViewXpBottleColor, v -> { c.easyViewXpBottleColor = v; c.save(); }),
				Options.toggle("XP bottle: tier color", d.easyViewXpBottleTierColor,
						() -> c.easyViewXpBottleTierColor, v -> { c.easyViewXpBottleTierColor = v; c.save(); })
						.tooltip("Color the XP-bottle overlay by its tier.")));

		return ConfigPanel.of("prisons-easyview", "EasyView",
				"Inventory value & level overlays", PanelIcon.EYE,
				List.of(general, value, percent, gear, misc));
	}
}
