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

/** The Player List HUD config panel, bound to {@link SkyConfig}. */
public final class PlayerListHudPanel {

	private PlayerListHudPanel() {}

	public static ConfigPanel create() {
		SkyConfig d = new SkyConfig();
		SkyConfig c = BetterSkyClient.config;

		OptionGroup general = new OptionGroup("General", List.<Option>of(
				Options.toggle("Player List HUD", d.playerListHudEnabled,
						() -> c.playerListHudEnabled, v -> { c.playerListHudEnabled = v; c.save(); })
						.tooltip("List the other players online on the server."),
				Options.intSlider("Scale", d.playerListHudScale, 25, 150, 5,
						() -> c.playerListHudScale, v -> { c.playerListHudScale = v; c.save(); })
						.tooltip("Text size (100% = normal)."),
				Options.toggle("Show header", d.playerListShowHeader,
						() -> c.playerListShowHeader, v -> { c.playerListShowHeader = v; c.save(); })
						.tooltip("Show the \"Players: N\" count line above the list."),
				Options.intSlider("Entries per column", d.playerListEntriesPerColumn, 1, 30, 1,
						() -> c.playerListEntriesPerColumn, v -> { c.playerListEntriesPerColumn = v; c.save(); })
						.tooltip("Names shown before wrapping into a new column."),
				Options.intSlider("Column spacing", d.playerListColumnSpacing, 0, 30, 1,
						() -> c.playerListColumnSpacing, v -> { c.playerListColumnSpacing = v; c.save(); })
						.tooltip("Horizontal gap between columns."),
				Options.intSlider("Max entries", d.playerListMaxEntries, 5, 100, 5,
						() -> c.playerListMaxEntries, v -> { c.playerListMaxEntries = v; c.save(); })
						.tooltip("Cap on rows shown; overflow collapses into a \"+N more\" line.")));

		OptionGroup colors = new OptionGroup("Colors", List.<Option>of(
				SkyOptions.colorRgb("Name", d.playerListNameColor,
						() -> c.playerListNameColor, v -> { c.playerListNameColor = v; c.save(); }),
				SkyOptions.colorRgb("Header", d.playerListHeaderColor,
						() -> c.playerListHeaderColor, v -> { c.playerListHeaderColor = v; c.save(); })));

		OptionGroup style = new OptionGroup("Background & border", List.<Option>of(
				SkyOptions.colorRgb("Background", d.playerListBgColor,
						() -> c.playerListBgColor, v -> { c.playerListBgColor = v; c.save(); }),
				Options.intSlider("Background opacity", d.playerListBgOpacity, 0, 255, 5,
						() -> c.playerListBgOpacity, v -> { c.playerListBgOpacity = v; c.save(); }),
				SkyOptions.colorRgb("Border", d.playerListBorderColor,
						() -> c.playerListBorderColor, v -> { c.playerListBorderColor = v; c.save(); }),
				Options.intSlider("Border opacity", d.playerListBorderOpacity, 0, 255, 5,
						() -> c.playerListBorderOpacity, v -> { c.playerListBorderOpacity = v; c.save(); }),
				Options.intSlider("Border thickness", d.playerListBorderThickness, 0, 5, 1,
						() -> c.playerListBorderThickness, v -> { c.playerListBorderThickness = v; c.save(); })));

		return ConfigPanel.of("sky-playerlist", "Player List",
				"Players online on the server", PanelIcon.EYE,
				List.of(general, colors, style));
	}
}
