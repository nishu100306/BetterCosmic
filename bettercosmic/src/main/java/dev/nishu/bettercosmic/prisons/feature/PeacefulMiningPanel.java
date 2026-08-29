package dev.nishu.bettercosmic.prisons.feature;

import dev.nishu.bettercosmic.prisons.client.BetterPrisonsClient;
import dev.nishu.bettercosmic.prisons.config.PrisonsConfig;
import dev.nishu.bettercosmic.shared.ui.model.ConfigPanel;
import dev.nishu.bettercosmic.shared.ui.model.Option;
import dev.nishu.bettercosmic.shared.ui.model.OptionGroup;
import dev.nishu.bettercosmic.shared.ui.model.Options;
import dev.nishu.bettercosmic.shared.ui.model.PanelIcon;

import java.util.List;

/** Config panel for Peaceful Mining, bound to {@link PrisonsConfig}. */
public final class PeacefulMiningPanel {

	private PeacefulMiningPanel() {}

	public static ConfigPanel create() {
		PrisonsConfig d = new PrisonsConfig();
		PrisonsConfig c = BetterPrisonsClient.config;

		OptionGroup general = new OptionGroup("Peaceful mining", List.<Option>of(
				Options.toggle("Enabled", d.peacefulMiningEnabled,
						() -> c.peacefulMiningEnabled, v -> { c.peacefulMiningEnabled = v; c.save(); })
						.tooltip("Ghost nearby players and mine through them while holding a mining tool."),
				Options.toggle("Active with pickaxe", d.peacefulMiningPickaxe,
						() -> c.peacefulMiningPickaxe, v -> { c.peacefulMiningPickaxe = v; c.save(); }),
				Options.toggle("Active with mace", d.peacefulMiningMace,
						() -> c.peacefulMiningMace, v -> { c.peacefulMiningMace = v; c.save(); }),
				Options.toggle("Always in PrisonBreak", d.peacefulMiningAlwaysInPrisonbreak,
						() -> c.peacefulMiningAlwaysInPrisonbreak, v -> { c.peacefulMiningAlwaysInPrisonbreak = v; c.save(); })
						.tooltip("Stay active in the PrisonBreak world regardless of held item."),
				Options.intSlider("Ghost opacity", d.peacefulMiningOpacity, 0, 255, 5,
						() -> c.peacefulMiningOpacity, v -> { c.peacefulMiningOpacity = v; c.save(); })
						.tooltip("How see-through other players become (0 = invisible, 255 = solid)."),
				Options.intSlider("Radius", d.peacefulMiningDistance, 1, 32, 1,
						() -> c.peacefulMiningDistance, v -> { c.peacefulMiningDistance = v; c.save(); })
						.tooltip("How close a player must be to be ghosted (blocks)."),
				Options.toggle("Auto-disable in combat", d.peacefulMiningDisableOnCombat,
						() -> c.peacefulMiningDisableOnCombat, v -> { c.peacefulMiningDisableOnCombat = v; c.save(); })
						.tooltip("Turn peaceful mining off when you enter combat, back on when it ends.")));

		return ConfigPanel.of("prisons-peaceful", "Peaceful Mining",
				"See and mine through nearby players", PanelIcon.GEAR,
				List.of(general));
	}
}
