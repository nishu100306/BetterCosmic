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

/**
 * Config panel for the Enchant HUD, the Powerball Ready alert, and the Super Breaker Aura — the
 * enchant-visual features, bound to {@link PrisonsConfig}.
 */
public final class EnchantHudPanel {

	private EnchantHudPanel() {}

	public static ConfigPanel create() {
		PrisonsConfig d = new PrisonsConfig();
		PrisonsConfig c = BetterPrisonsClient.config;

		OptionGroup hud = new OptionGroup("Enchant HUD", List.<Option>of(
				Options.toggle("Enchant HUD", d.enchantHudEnabled,
						() -> c.enchantHudEnabled, v -> { c.enchantHudEnabled = v; c.save(); }),
				Options.intSlider("Scale", d.enchantHudScale, 50, 150, 5,
						() -> c.enchantHudScale, v -> { c.enchantHudScale = v; c.save(); }),
				Options.toggle("Show title", d.showEnchantHudTitle,
						() -> c.showEnchantHudTitle, v -> { c.showEnchantHudTitle = v; c.save(); }),
				PrisonOptions.colorRgb("Title color", d.enchantHudTitleColor,
						() -> c.enchantHudTitleColor, v -> { c.enchantHudTitleColor = v; c.save(); }),
				PrisonOptions.colorRgb("Timer color", d.enchantTimeColor,
						() -> c.enchantTimeColor, v -> { c.enchantTimeColor = v; c.save(); })));

		OptionGroup style = new OptionGroup("Background & border", List.<Option>of(
				PrisonOptions.colorRgb("Background", d.enchantBgColor,
						() -> c.enchantBgColor, v -> { c.enchantBgColor = v; c.save(); }),
				Options.intSlider("Background opacity", d.enchantBgOpacity, 0, 255, 5,
						() -> c.enchantBgOpacity, v -> { c.enchantBgOpacity = v; c.save(); }),
				PrisonOptions.colorRgb("Border", d.enchantBorderColor,
						() -> c.enchantBorderColor, v -> { c.enchantBorderColor = v; c.save(); }),
				Options.intSlider("Border opacity", d.enchantBorderOpacity, 0, 255, 5,
						() -> c.enchantBorderOpacity, v -> { c.enchantBorderOpacity = v; c.save(); }),
				Options.intSlider("Border thickness", d.enchantBorderThickness, 0, 5, 1,
						() -> c.enchantBorderThickness, v -> { c.enchantBorderThickness = v; c.save(); })));

		OptionGroup powerball = new OptionGroup("Powerball Ready alert", List.<Option>of(
				Options.toggle("Show title alert", d.powerballAlertTitleEnabled,
						() -> c.powerballAlertTitleEnabled, v -> { c.powerballAlertTitleEnabled = v; c.save(); })
						.tooltip("Pop a title on screen when Powerball is ready again."),
				Options.text("Alert text", d.powerballAlertTitleText,
						() -> c.powerballAlertTitleText, v -> { c.powerballAlertTitleText = v; c.save(); }),
				PrisonOptions.colorRgb("Alert color", d.powerballAlertTitleColor,
						() -> c.powerballAlertTitleColor, v -> { c.powerballAlertTitleColor = v; c.save(); })));

		OptionGroup aura = new OptionGroup("Super Breaker Aura", List.<Option>of(
				Options.toggle("Aura", d.superBreakerAuraEnabled,
						() -> c.superBreakerAuraEnabled, v -> { c.superBreakerAuraEnabled = v; c.save(); }),
				Options.intSlider("Aura scale", d.superBreakerAuraScale, 50, 150, 5,
						() -> c.superBreakerAuraScale, v -> { c.superBreakerAuraScale = v; c.save(); }),
				PrisonOptions.colorRgb("Base color", d.superBreakerBaseColor,
						() -> c.superBreakerBaseColor, v -> { c.superBreakerBaseColor = v; c.save(); }),
				Options.intSlider("Base opacity", d.superBreakerBaseOpacity, 0, 255, 5,
						() -> c.superBreakerBaseOpacity, v -> { c.superBreakerBaseOpacity = v; c.save(); }),
				PrisonOptions.colorRgb("Fill color", d.superBreakerLightColor,
						() -> c.superBreakerLightColor, v -> { c.superBreakerLightColor = v; c.save(); }),
				Options.intSlider("Fill opacity", d.superBreakerLightOpacity, 0, 255, 5,
						() -> c.superBreakerLightOpacity, v -> { c.superBreakerLightOpacity = v; c.save(); }),
				Options.toggle("Show timer", d.superBreakerTimerEnabled,
						() -> c.superBreakerTimerEnabled, v -> { c.superBreakerTimerEnabled = v; c.save(); }),
				Options.intSlider("Timer X offset", d.superBreakerTimerOffsetX, -200, 200, 5,
						() -> c.superBreakerTimerOffsetX, v -> { c.superBreakerTimerOffsetX = v; c.save(); }),
				Options.intSlider("Timer Y offset", d.superBreakerTimerOffsetY, -200, 200, 5,
						() -> c.superBreakerTimerOffsetY, v -> { c.superBreakerTimerOffsetY = v; c.save(); })));

		return ConfigPanel.of("prisons-enchant", "Enchant HUD",
				"Active enchants, Powerball alert & Super Breaker aura", PanelIcon.SPARKLE,
				List.of(hud, style, powerball, aura));
	}
}
