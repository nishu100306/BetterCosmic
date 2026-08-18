package dev.nishu.bettercosmic.prisons.hud;

import dev.nishu.bettercosmic.prisons.client.BetterPrisonsClient;
import dev.nishu.bettercosmic.prisons.config.PrisonsConfig;
import dev.nishu.bettercosmic.prisons.ui.PrisonOptions;
import dev.nishu.bettercosmic.shared.ui.model.ConfigPanel;
import dev.nishu.bettercosmic.shared.ui.model.Option;
import dev.nishu.bettercosmic.shared.ui.model.OptionGroup;
import dev.nishu.bettercosmic.shared.ui.model.Options;
import dev.nishu.bettercosmic.shared.ui.model.PanelIcon;

import java.util.List;

/** The Cooldown HUD config panel, bound to {@link PrisonsConfig}. */
public final class CooldownHudPanel {

	private CooldownHudPanel() {}

	public static ConfigPanel create() {
		PrisonsConfig d = new PrisonsConfig();
		PrisonsConfig c = BetterPrisonsClient.config;

		OptionGroup general = new OptionGroup("General", List.<Option>of(
				Options.toggle("Cooldown HUD", d.cooldownHudEnabled,
						() -> c.cooldownHudEnabled, v -> { c.cooldownHudEnabled = v; c.save(); }),
				Options.intSlider("Scale", d.cooldownHudScale, 50, 150, 5,
						() -> c.cooldownHudScale, v -> { c.cooldownHudScale = v; c.save(); }),
				Options.toggle("Show title", d.showCooldownHudTitle,
						() -> c.showCooldownHudTitle, v -> { c.showCooldownHudTitle = v; c.save(); }),
				PrisonOptions.colorRgb("Title color", d.cooldownHudTitleColor,
						() -> c.cooldownHudTitleColor, v -> { c.cooldownHudTitleColor = v; c.save(); })));

		OptionGroup commands = new OptionGroup("Commands", List.<Option>of(
				Options.toggle("Home", d.homeEnabled, () -> c.homeEnabled, v -> { c.homeEnabled = v; c.save(); }),
				PrisonOptions.colorRgb("Home color", d.homeColor, () -> c.homeColor, v -> { c.homeColor = v; c.save(); }),
				Options.toggle("Jet", d.jetEnabled, () -> c.jetEnabled, v -> { c.jetEnabled = v; c.save(); }),
				PrisonOptions.colorRgb("Jet color", d.jetColor, () -> c.jetColor, v -> { c.jetColor = v; c.save(); }),
				Options.toggle("Feed", d.feedEnabled, () -> c.feedEnabled, v -> { c.feedEnabled = v; c.save(); }),
				PrisonOptions.colorRgb("Feed color", d.feedColor, () -> c.feedColor, v -> { c.feedColor = v; c.save(); }),
				Options.toggle("Fix", d.fixEnabled, () -> c.fixEnabled, v -> { c.fixEnabled = v; c.save(); }),
				PrisonOptions.colorRgb("Fix color", d.fixColor, () -> c.fixColor, v -> { c.fixColor = v; c.save(); }),
				Options.toggle("Combat", d.combatEnabled, () -> c.combatEnabled, v -> { c.combatEnabled = v; c.save(); }),
				PrisonOptions.colorRgb("Combat color", d.combatColor, () -> c.combatColor, v -> { c.combatColor = v; c.save(); }),
				Options.toggle("tpa", d.tpaEnabled, () -> c.tpaEnabled, v -> { c.tpaEnabled = v; c.save(); }),
				PrisonOptions.colorRgb("tpa color", d.tpaColor, () -> c.tpaColor, v -> { c.tpaColor = v; c.save(); }),
				Options.toggle("tpahere", d.tpahereEnabled, () -> c.tpahereEnabled, v -> { c.tpahereEnabled = v; c.save(); }),
				PrisonOptions.colorRgb("tpahere color", d.tpahereColor, () -> c.tpahereColor, v -> { c.tpahereColor = v; c.save(); }),
				Options.toggle("Dangle", d.dangleEnabled, () -> c.dangleEnabled, v -> { c.dangleEnabled = v; c.save(); }),
				PrisonOptions.colorRgb("Dangle color", d.dangleColor, () -> c.dangleColor, v -> { c.dangleColor = v; c.save(); }),
				Options.toggle("Adangle", d.adangleEnabled, () -> c.adangleEnabled, v -> { c.adangleEnabled = v; c.save(); }),
				PrisonOptions.colorRgb("Adangle color", d.adangleColor, () -> c.adangleColor, v -> { c.adangleColor = v; c.save(); }),
				Options.toggle("Near", d.nearEnabled, () -> c.nearEnabled, v -> { c.nearEnabled = v; c.save(); }),
				PrisonOptions.colorRgb("Near color", d.nearColor, () -> c.nearColor, v -> { c.nearColor = v; c.save(); }),
				Options.toggle("Pulse", d.pulseEnabled, () -> c.pulseEnabled, v -> { c.pulseEnabled = v; c.save(); }),
				PrisonOptions.colorRgb("Pulse color", d.pulseColor, () -> c.pulseColor, v -> { c.pulseColor = v; c.save(); })));

		OptionGroup style = new OptionGroup("Background & border", List.<Option>of(
				PrisonOptions.colorRgb("Background", d.cooldownBgColor,
						() -> c.cooldownBgColor, v -> { c.cooldownBgColor = v; c.save(); }),
				Options.intSlider("Background opacity", d.cooldownBgOpacity, 0, 255, 5,
						() -> c.cooldownBgOpacity, v -> { c.cooldownBgOpacity = v; c.save(); }),
				PrisonOptions.colorRgb("Border", d.cooldownBorderColor,
						() -> c.cooldownBorderColor, v -> { c.cooldownBorderColor = v; c.save(); }),
				Options.intSlider("Border opacity", d.cooldownBorderOpacity, 0, 255, 5,
						() -> c.cooldownBorderOpacity, v -> { c.cooldownBorderOpacity = v; c.save(); }),
				Options.intSlider("Border thickness", d.cooldownBorderThickness, 0, 5, 1,
						() -> c.cooldownBorderThickness, v -> { c.cooldownBorderThickness = v; c.save(); })));

		return ConfigPanel.of("prisons-cooldown", "Cooldown HUD",
				"Command & ability cooldown timers", PanelIcon.GEAR,
				List.of(general, commands, style));
	}
}
