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

/** The Satchel HUD config panel, bound to {@link PrisonsConfig}. */
public final class SatchelHudPanel {

	private SatchelHudPanel() {}

	public static ConfigPanel create() {
		PrisonsConfig d = new PrisonsConfig();
		PrisonsConfig c = BetterPrisonsClient.config;

		OptionGroup general = new OptionGroup("General", List.<Option>of(
				Options.toggle("Satchel HUD", d.satchelHudEnabled,
						() -> c.satchelHudEnabled, v -> { c.satchelHudEnabled = v; c.save(); }),
				Options.intSlider("Scale", d.satchelHudScale, 50, 150, 5,
						() -> c.satchelHudScale, v -> { c.satchelHudScale = v; c.save(); }),
				Options.toggle("Show title", d.showSatchelHudTitle,
						() -> c.showSatchelHudTitle, v -> { c.showSatchelHudTitle = v; c.save(); }),
				PrisonOptions.colorRgb("Title color", d.satchelHudTitleColor,
						() -> c.satchelHudTitleColor, v -> { c.satchelHudTitleColor = v; c.save(); }),
				Options.toggle("Show count on title", d.satchelShowCount,
						() -> c.satchelShowCount, v -> { c.satchelShowCount = v; c.save(); })));

		OptionGroup display = new OptionGroup("Display", List.<Option>of(
				Options.toggle("Show as percentage", d.satchelShowPercentage,
						() -> c.satchelShowPercentage, v -> { c.satchelShowPercentage = v; c.save(); })
						.tooltip("Show fill as a percentage instead of current / max."),
				Options.toggle("Combine same type", d.combineSimilarSatchels,
						() -> c.combineSimilarSatchels, v -> { c.combineSimilarSatchels = v; c.save(); })
						.tooltip("Merge multiple satchels of the same type into one entry."),
				Options.toggle("Whitescroll indicators", d.satchelWhitescrollIndicators,
						() -> c.satchelWhitescrollIndicators, v -> { c.satchelWhitescrollIndicators = v; c.save(); }),
				Options.dropdown("Fill threshold", d.satchelShowThreshold,
						List.of("Off", "25%", "50%", "75%", "90%", "95%"),
						() -> c.satchelShowThreshold, v -> { c.satchelShowThreshold = v; c.save(); })
						.tooltip("Hide satchels below this fill level.")));

		OptionGroup thresholds = new OptionGroup("Threshold colors", List.<Option>of(
				PrisonOptions.colorRgb("Under 20%", d.satchelColorUnder20,
						() -> c.satchelColorUnder20, v -> { c.satchelColorUnder20 = v; c.save(); }),
				PrisonOptions.colorRgb("20-60%", d.satchelColor20to60,
						() -> c.satchelColor20to60, v -> { c.satchelColor20to60 = v; c.save(); }),
				PrisonOptions.colorRgb("60-95%", d.satchelColor60to95,
						() -> c.satchelColor60to95, v -> { c.satchelColor60to95 = v; c.save(); }),
				PrisonOptions.colorRgb("95%+", d.satchelColor95Plus,
						() -> c.satchelColor95Plus, v -> { c.satchelColor95Plus = v; c.save(); })));

		OptionGroup style = new OptionGroup("Background & border", List.<Option>of(
				PrisonOptions.colorRgb("Background", d.satchelBgColor,
						() -> c.satchelBgColor, v -> { c.satchelBgColor = v; c.save(); }),
				Options.intSlider("Background opacity", d.satchelBgOpacity, 0, 255, 5,
						() -> c.satchelBgOpacity, v -> { c.satchelBgOpacity = v; c.save(); }),
				PrisonOptions.colorRgb("Border", d.satchelBorderColor,
						() -> c.satchelBorderColor, v -> { c.satchelBorderColor = v; c.save(); }),
				Options.intSlider("Border opacity", d.satchelBorderOpacity, 0, 255, 5,
						() -> c.satchelBorderOpacity, v -> { c.satchelBorderOpacity = v; c.save(); }),
				Options.intSlider("Border thickness", d.satchelBorderThickness, 0, 5, 1,
						() -> c.satchelBorderThickness, v -> { c.satchelBorderThickness = v; c.save(); })));

		return ConfigPanel.of("prisons-satchel", "Satchel HUD",
				"Satchel fill tracking overlay", PanelIcon.SATCHEL,
				List.of(general, display, thresholds, style));
	}
}
