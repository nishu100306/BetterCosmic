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
						.tooltip("Cache your player vaults as you open /pv and show them as read-only preview "
								+ "sidebars on the /pv screens. Contents update the next time you open each vault."),
				Options.toggle("Show empty vaults", d.pvViewerShowEmpty,
						() -> c.pvViewerShowEmpty, v -> { c.pvViewerShowEmpty = v; c.save(); })
						.tooltip("Show vaults with no items in the viewer and preview sidebars. When off, empty "
								+ "vaults are hidden (starred vaults always stay visible)."),
				Options.intSlider("Preview background opacity", d.pvPreviewBgOpacity, 0, 255, 5,
						() -> c.pvPreviewBgOpacity, v -> { c.pvPreviewBgOpacity = v; c.save(); })
						.tooltip("How opaque the preview sidebar's background panel is. 0 is fully "
								+ "transparent (default); the frame stays visible either way."),
				Options.intSlider("Preview scale", d.pvCompactScalePercent, 50, 100, 5,
						() -> c.pvCompactScalePercent, v -> { c.pvCompactScalePercent = v; c.save(); })
						.tooltip("Shrinks the preview sidebars so more vaults fit on screen. 100% is off "
								+ "(full size); lower values make each vault preview smaller. The vanilla "
								+ "vault window is left untouched.")));

		return ConfigPanel.of("prisons-pvviewer", "Vaults",
				"Cached player-vault viewer", PanelIcon.EYE,
				List.of(viewer));
	}
}
