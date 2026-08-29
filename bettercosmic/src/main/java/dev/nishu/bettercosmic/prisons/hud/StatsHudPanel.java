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

/** The Stats HUD config panel, bound to {@link PrisonsConfig}. */
public final class StatsHudPanel {

	private StatsHudPanel() {}

	public static ConfigPanel create() {
		PrisonsConfig d = new PrisonsConfig();
		PrisonsConfig c = BetterPrisonsClient.config;

		OptionGroup general = new OptionGroup("General", List.<Option>of(
				Options.toggle("Stats HUD", d.statsHudEnabled,
						() -> c.statsHudEnabled, v -> { c.statsHudEnabled = v; c.save(); }),
				Options.intSlider("Scale", d.statsHudScale, 50, 150, 5,
						() -> c.statsHudScale, v -> { c.statsHudScale = v; c.save(); }),
				Options.toggle("Show title", d.showStatsHudTitle,
						() -> c.showStatsHudTitle, v -> { c.showStatsHudTitle = v; c.save(); }),
				PrisonOptions.colorRgb("Title color", d.statsHudTitleColor,
						() -> c.statsHudTitleColor, v -> { c.statsHudTitleColor = v; c.save(); })));

		OptionGroup elements = new OptionGroup("Elements", List.<Option>of(
				Options.toggle("Current XP", d.statsShowCurrentXP,
						() -> c.statsShowCurrentXP, v -> { c.statsShowCurrentXP = v; c.save(); }),
				Options.toggle("Time to level", d.statsShowTimeTillLevelUp,
						() -> c.statsShowTimeTillLevelUp, v -> { c.statsShowTimeTillLevelUp = v; c.save(); }),
				Options.toggle("XP / hour", d.statsShowXPPerHour,
						() -> c.statsShowXPPerHour, v -> { c.statsShowXPPerHour = v; c.save(); }),
				Options.toggle("XP / minute", d.statsShowXPPerMinute,
						() -> c.statsShowXPPerMinute, v -> { c.statsShowXPPerMinute = v; c.save(); }),
				Options.toggle("Session XP", d.statsShowSessionXP,
						() -> c.statsShowSessionXP, v -> { c.statsShowSessionXP = v; c.save(); }),
				Options.toggle("Current CE", d.statsShowCurrentCE,
						() -> c.statsShowCurrentCE, v -> { c.statsShowCurrentCE = v; c.save(); }),
				Options.toggle("CE / hour", d.statsShowCEPerHour,
						() -> c.statsShowCEPerHour, v -> { c.statsShowCEPerHour = v; c.save(); }),
				Options.toggle("CE / minute", d.statsShowCEPerMinute,
						() -> c.statsShowCEPerMinute, v -> { c.statsShowCEPerMinute = v; c.save(); }),
				Options.toggle("Session CE", d.statsShowSessionCE,
						() -> c.statsShowSessionCE, v -> { c.statsShowSessionCE = v; c.save(); }),
				Options.toggle("Session duration", d.statsShowSessionDuration,
						() -> c.statsShowSessionDuration, v -> { c.statsShowSessionDuration = v; c.save(); }),
				Options.toggle("Duration millis", d.statsShowMillisOnSessionDuration,
						() -> c.statsShowMillisOnSessionDuration, v -> { c.statsShowMillisOnSessionDuration = v; c.save(); })));

		OptionGroup colors = new OptionGroup("Element colors", List.<Option>of(
				PrisonOptions.colorRgb("Current XP", d.statsCurrentXPColor,
						() -> c.statsCurrentXPColor, v -> { c.statsCurrentXPColor = v; c.save(); }),
				PrisonOptions.colorRgb("XP / hour", d.statsXPPerHourColor,
						() -> c.statsXPPerHourColor, v -> { c.statsXPPerHourColor = v; c.save(); }),
				PrisonOptions.colorRgb("XP / minute", d.statsXPPerMinuteColor,
						() -> c.statsXPPerMinuteColor, v -> { c.statsXPPerMinuteColor = v; c.save(); }),
				PrisonOptions.colorRgb("Session XP", d.statsSessionXPColor,
						() -> c.statsSessionXPColor, v -> { c.statsSessionXPColor = v; c.save(); }),
				PrisonOptions.colorRgb("Current CE", d.statsCurrentCEColor,
						() -> c.statsCurrentCEColor, v -> { c.statsCurrentCEColor = v; c.save(); }),
				PrisonOptions.colorRgb("CE / hour", d.statsCEPerHourColor,
						() -> c.statsCEPerHourColor, v -> { c.statsCEPerHourColor = v; c.save(); }),
				PrisonOptions.colorRgb("CE / minute", d.statsCEPerMinuteColor,
						() -> c.statsCEPerMinuteColor, v -> { c.statsCEPerMinuteColor = v; c.save(); }),
				PrisonOptions.colorRgb("Session CE", d.statsSessionCEColor,
						() -> c.statsSessionCEColor, v -> { c.statsSessionCEColor = v; c.save(); }),
				PrisonOptions.colorRgb("Session duration", d.statsSessionDurationColor,
						() -> c.statsSessionDurationColor, v -> { c.statsSessionDurationColor = v; c.save(); }),
				PrisonOptions.colorRgb("Time to level", d.statsTimeTillLevelUpColor,
						() -> c.statsTimeTillLevelUpColor, v -> { c.statsTimeTillLevelUpColor = v; c.save(); })));

		OptionGroup style = new OptionGroup("Background & border", List.<Option>of(
				PrisonOptions.colorRgb("Background", d.statsBgColor,
						() -> c.statsBgColor, v -> { c.statsBgColor = v; c.save(); }),
				Options.intSlider("Background opacity", d.statsBgOpacity, 0, 255, 5,
						() -> c.statsBgOpacity, v -> { c.statsBgOpacity = v; c.save(); }),
				PrisonOptions.colorRgb("Border", d.statsBorderColor,
						() -> c.statsBorderColor, v -> { c.statsBorderColor = v; c.save(); }),
				Options.intSlider("Border opacity", d.statsBorderOpacity, 0, 255, 5,
						() -> c.statsBorderOpacity, v -> { c.statsBorderOpacity = v; c.save(); }),
				Options.intSlider("Border thickness", d.statsBorderThickness, 0, 5, 1,
						() -> c.statsBorderThickness, v -> { c.statsBorderThickness = v; c.save(); })));

		return ConfigPanel.of("prisons-stats", "Stats HUD",
				"XP & energy session tracking", PanelIcon.CHART,
				List.of(general, elements, colors, style));
	}
}
