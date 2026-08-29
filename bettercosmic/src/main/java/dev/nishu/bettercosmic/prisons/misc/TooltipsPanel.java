package dev.nishu.bettercosmic.prisons.misc;

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
 * Config panel for the extra tooltip lines: gear enchant-book upgrade costs and gang-point-note expiry
 * countdowns. Bound to {@link PrisonsConfig} via the shared {@code Options} lambdas.
 */
public final class TooltipsPanel {

	private TooltipsPanel() {}

	public static ConfigPanel create() {
		PrisonsConfig d = new PrisonsConfig();
		PrisonsConfig c = BetterPrisonsClient.config;

		OptionGroup enchantBooks = new OptionGroup("Enchant book costs", List.<Option>of(
				Options.toggle("Show upgrade costs", d.enchantBookCostsEnabled,
						() -> c.enchantBookCostsEnabled, v -> { c.enchantBookCostsEnabled = v; c.save(); })
						.tooltip("Per-level Cosmic Energy cost lines on gear enchant books (inventory + chat hover)."),
				PrisonOptions.colorRgb("Cost text color", d.enchantBookCostsColor,
						() -> c.enchantBookCostsColor, v -> { c.enchantBookCostsColor = v; c.save(); })));

		OptionGroup gangPoints = new OptionGroup("Gang point expiry", List.<Option>of(
				Options.toggle("Show expiry countdown", d.gangPointExpiryEnabled,
						() -> c.gangPointExpiryEnabled, v -> { c.gangPointExpiryEnabled = v; c.save(); })
						.tooltip("Time-remaining + localized expiry on gang point notes."),
				PrisonOptions.colorRgb("Expiry text color", d.gangPointExpiryColor,
						() -> c.gangPointExpiryColor, v -> { c.gangPointExpiryColor = v; c.save(); })));

		return ConfigPanel.of("prisons-tooltips", "Tooltips",
				"Enchant book costs & gang point expiry", PanelIcon.CHART,
				List.of(enchantBooks, gangPoints));
	}
}
