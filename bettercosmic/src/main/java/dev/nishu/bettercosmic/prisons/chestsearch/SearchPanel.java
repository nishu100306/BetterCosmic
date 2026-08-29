package dev.nishu.bettercosmic.prisons.chestsearch;

import dev.nishu.bettercosmic.prisons.client.BetterPrisonsClient;
import dev.nishu.bettercosmic.prisons.config.PrisonsConfig;
import dev.nishu.bettercosmic.prisons.ui.PrisonOptions;
import dev.nishu.bettercosmic.shared.ui.model.ConfigPanel;
import dev.nishu.bettercosmic.shared.ui.model.Option;
import dev.nishu.bettercosmic.shared.ui.model.OptionGroup;
import dev.nishu.bettercosmic.shared.ui.model.Options;
import dev.nishu.bettercosmic.shared.ui.model.PanelIcon;

import java.util.List;

/** Config panel for Chest Search and Clue Scroll sorting, bound to {@link PrisonsConfig}. */
public final class SearchPanel {

	private SearchPanel() {}

	public static ConfigPanel create() {
		PrisonsConfig d = new PrisonsConfig();
		PrisonsConfig c = BetterPrisonsClient.config;

		OptionGroup chest = new OptionGroup("Chest search", List.<Option>of(
				Options.toggle("Chest search", d.chestSearchEnabled,
						() -> c.chestSearchEnabled, v -> { c.chestSearchEnabled = v; c.save(); })
						.tooltip("Search bar + filter-rule sidebar in containers; matches are highlighted.")));

		OptionGroup clue = new OptionGroup("Clue scroll", List.<Option>of(
				Options.toggle("Clue scroll sorting", d.clueScrollSortingEnabled,
						() -> c.clueScrollSortingEnabled, v -> { c.clueScrollSortingEnabled = v; c.save(); })
						.tooltip("Show each clue scroll's current step number on the item."),
				PrisonOptions.colorRgb("Number color", d.clueScrollNumberColor,
						() -> c.clueScrollNumberColor, v -> { c.clueScrollNumberColor = v; c.save(); }),
				Options.toggle("Number outline", d.clueScrollNumberOutline,
						() -> c.clueScrollNumberOutline, v -> { c.clueScrollNumberOutline = v; c.save(); })
						.tooltip("Draw a black outline behind the step number for readability."),
				Options.toggle("Report unmapped steps", d.clueScrollUnmappedTooltipEnabled,
						() -> c.clueScrollUnmappedTooltipEnabled, v -> { c.clueScrollUnmappedTooltipEnabled = v; c.save(); })
						.tooltip("Add a tooltip flagging clue step types the mod doesn't recognize yet.")));

		return ConfigPanel.of("prisons-search", "Search",
				"Chest search & clue scroll sorting", PanelIcon.EYE,
				List.of(chest, clue));
	}
}
