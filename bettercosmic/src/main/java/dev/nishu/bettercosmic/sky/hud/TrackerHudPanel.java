package dev.nishu.bettercosmic.sky.hud;

import dev.nishu.bettercosmic.shared.ui.model.ConfigPanel;
import dev.nishu.bettercosmic.shared.ui.model.Option;
import dev.nishu.bettercosmic.shared.ui.model.OptionGroup;
import dev.nishu.bettercosmic.shared.ui.model.Options;
import dev.nishu.bettercosmic.shared.ui.model.PanelIcon;
import dev.nishu.bettercosmic.sky.client.BetterSkyClient;
import dev.nishu.bettercosmic.sky.config.SkyConfig;
import dev.nishu.bettercosmic.sky.ui.SkyOptions;

import java.util.List;

/** The Tracker HUD config panel, bound to {@link SkyConfig}. */
public final class TrackerHudPanel {

	private TrackerHudPanel() {}

	public static ConfigPanel create() {
		SkyConfig d = new SkyConfig();
		SkyConfig c = BetterSkyClient.config;

		OptionGroup general = new OptionGroup("General", List.<Option>of(
				Options.toggle("Tracker HUD", d.trackerHudEnabled,
						() -> c.trackerHudEnabled, v -> { c.trackerHudEnabled = v; c.save(); })
						.tooltip("Track quests completed / adventure chests by tier. Toggle mode on the HUD."),
				Options.intSlider("Scale", d.trackerHudScale, 25, 150, 5,
						() -> c.trackerHudScale, v -> { c.trackerHudScale = v; c.save(); })
						.tooltip("Text size (100% = normal)."),
				Options.toggle("Show timer", d.trackerShowTimer,
						() -> c.trackerShowTimer, v -> { c.trackerShowTimer = v; c.save(); })
						.tooltip("Show the session timer line. Use the on-HUD buttons to pause or reset it."),
				Options.toggle("Hide empty tiers", d.trackerHideEmpty,
						() -> c.trackerHideEmpty, v -> { c.trackerHideEmpty = v; c.save(); })
						.tooltip("Only list tiers you've completed at least one of.")));

		OptionGroup colors = new OptionGroup("Colors", List.<Option>of(
				SkyOptions.colorRgb("Title", d.trackerTitleColor,
						() -> c.trackerTitleColor, v -> { c.trackerTitleColor = v; c.save(); }),
				SkyOptions.colorRgb("Timer", d.trackerTimerColor,
						() -> c.trackerTimerColor, v -> { c.trackerTimerColor = v; c.save(); })));

		OptionGroup style = new OptionGroup("Background & border", List.<Option>of(
				SkyOptions.colorRgb("Background", d.trackerBgColor,
						() -> c.trackerBgColor, v -> { c.trackerBgColor = v; c.save(); }),
				Options.intSlider("Background opacity", d.trackerBgOpacity, 0, 255, 5,
						() -> c.trackerBgOpacity, v -> { c.trackerBgOpacity = v; c.save(); }),
				SkyOptions.colorRgb("Border", d.trackerBorderColor,
						() -> c.trackerBorderColor, v -> { c.trackerBorderColor = v; c.save(); }),
				Options.intSlider("Border opacity", d.trackerBorderOpacity, 0, 255, 5,
						() -> c.trackerBorderOpacity, v -> { c.trackerBorderOpacity = v; c.save(); }),
				Options.intSlider("Border thickness", d.trackerBorderThickness, 0, 5, 1,
						() -> c.trackerBorderThickness, v -> { c.trackerBorderThickness = v; c.save(); })));

		return ConfigPanel.of("sky-tracker", "Tracker",
				"Quests & adventure chests by tier", PanelIcon.CHART,
				List.of(general, colors, style));
	}
}
