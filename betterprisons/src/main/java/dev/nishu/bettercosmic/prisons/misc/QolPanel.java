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
 * Config panel for the quality-of-life render/interaction tweaks: first-person held-item scaling,
 * pickaxe drop protection, and the Blink-trinket destination overlay. (Auto-trade, bold XP/Energy
 * titles, and the texture pack live on the Misc panel.) Bound to {@link PrisonsConfig} via the shared
 * {@code Options} lambdas.
 */
public final class QolPanel {

	private QolPanel() {}

	public static ConfigPanel create() {
		PrisonsConfig d = new PrisonsConfig();
		PrisonsConfig c = BetterPrisonsClient.config;

		OptionGroup heldItem = new OptionGroup("Held item scale", List.<Option>of(
				Options.intSlider("Pickaxe", d.heldItemPickaxeScale, 50, 150, 5,
						() -> c.heldItemPickaxeScale, v -> { c.heldItemPickaxeScale = v; c.save(); }),
				Options.intSlider("Sword", d.heldItemSwordScale, 50, 150, 5,
						() -> c.heldItemSwordScale, v -> { c.heldItemSwordScale = v; c.save(); }),
				Options.intSlider("Axe", d.heldItemAxeScale, 50, 150, 5,
						() -> c.heldItemAxeScale, v -> { c.heldItemAxeScale = v; c.save(); }),
				Options.intSlider("Other", d.heldItemOtherScale, 50, 150, 5,
						() -> c.heldItemOtherScale, v -> { c.heldItemOtherScale = v; c.save(); })));

		OptionGroup pickaxeDrop = new OptionGroup("Pickaxe drop protection", List.<Option>of(
				Options.toggle("Confirm before dropping", d.pickaxeDropConfirmationEnabled,
						() -> c.pickaxeDropConfirmationEnabled, v -> { c.pickaxeDropConfirmationEnabled = v; c.save(); })
						.tooltip("Require a second drop press within 3s to drop a pickaxe."),
				Options.toggle("Block dropping entirely", d.pickaxeDropBlockEnabled,
						() -> c.pickaxeDropBlockEnabled, v -> { c.pickaxeDropBlockEnabled = v; c.save(); }),
				Options.toggle("Block dragging/throwing in containers", d.pickaxeDropDragBlockEnabled,
						() -> c.pickaxeDropDragBlockEnabled, v -> { c.pickaxeDropDragBlockEnabled = v; c.save(); })));

		OptionGroup blink = new OptionGroup("Blink trinket overlay", List.<Option>of(
				Options.toggle("Show destination overlay", d.blinkOverlayEnabled,
						() -> c.blinkOverlayEnabled, v -> { c.blinkOverlayEnabled = v; c.save(); })
						.tooltip("Highlight the block the Blink Trinket would teleport you to."),
				PrisonOptions.colorRgb("Fill color", d.blinkOverlayColor,
						() -> c.blinkOverlayColor, v -> { c.blinkOverlayColor = v; c.save(); }),
				Options.intSlider("Fill opacity", d.blinkOverlayOpacity, 0, 255, 5,
						() -> c.blinkOverlayOpacity, v -> { c.blinkOverlayOpacity = v; c.save(); }),
				PrisonOptions.colorRgb("Outline color", d.blinkOverlayOutlineColor,
						() -> c.blinkOverlayOutlineColor, v -> { c.blinkOverlayOutlineColor = v; c.save(); }),
				Options.intSlider("Outline thickness", d.blinkOverlayOutlineThickness, 0, 6, 1,
						() -> c.blinkOverlayOutlineThickness, v -> { c.blinkOverlayOutlineThickness = v; c.save(); })));

		return ConfigPanel.of("prisons-qol", "Quality of Life",
				"Held-item scale, drop protection, Blink overlay", PanelIcon.GEAR,
				List.of(heldItem, pickaxeDrop, blink));
	}
}
