package dev.nishu.bettercosmic.prisons.pvviewer;

import dev.nishu.bettercosmic.prisons.client.BetterPrisonsClient;
import dev.nishu.bettercosmic.prisons.config.PrisonsConfig;
import dev.nishu.bettercosmic.shared.ui.model.ConfigPanel;
import dev.nishu.bettercosmic.shared.ui.model.Option;
import dev.nishu.bettercosmic.shared.ui.model.OptionGroup;
import dev.nishu.bettercosmic.shared.ui.model.Options;
import dev.nishu.bettercosmic.shared.ui.model.PanelIcon;

import java.util.List;

/** Config panel for the Vault (PV) viewer, bound to {@link PrisonsConfig}. */
public final class PvViewerPanel {

	private PvViewerPanel() {}

	public static ConfigPanel create() {
		PrisonsConfig d = new PrisonsConfig();
		PrisonsConfig c = BetterPrisonsClient.config;

		OptionGroup viewer = new OptionGroup("Vault viewer", List.<Option>of(
				Options.toggle("Vault viewer", d.pvViewerEnabled,
						() -> c.pvViewerEnabled, v -> { c.pvViewerEnabled = v; c.save(); })
						.tooltip("Cache your player vaults as you open /pv, and browse them from a keybind "
								+ "(set it in Controls). Read-only; contents update the next time you open each vault.")));

		return ConfigPanel.of("prisons-pvviewer", "Vaults",
				"Cached player-vault viewer", PanelIcon.EYE,
				List.of(viewer));
	}
}
