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
 * interactions (use-item-while-mining, auto-trade), pickaxe drop protection, the Blink-trinket
 * destination overlay, and extras (bold XP/Energy popups, the PrisonBreak texture pack). The extras
 * and auto-trade moved here from the removed "Misc" panel. Bound to {@link PrisonsConfig} via the
 * shared {@code Options} lambdas.
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

		OptionGroup interactions = new OptionGroup("Interactions", List.<Option>of(
				Options.toggle("Use items while mining", d.useItemWhileMiningEnabled,
						() -> c.useItemWhileMiningEnabled, v -> { c.useItemWhileMiningEnabled = v; c.save(); })
						.tooltip("Allow right-click item use while actively breaking a block."),
				Options.toggle("Auto-trade", d.autoTradeEnabled,
						() -> c.autoTradeEnabled, v -> { c.autoTradeEnabled = v; c.save(); })
						.tooltip("Shift-right-click a player to send /trade <name>.")));

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

		OptionGroup extras = new OptionGroup("Popups & packs", List.<Option>of(
				Options.toggle("Bold XP/Energy popups", d.boldXpEnergyTitles,
						() -> c.boldXpEnergyTitles, v -> { c.boldXpEnergyTitles = v; c.save(); })
						.tooltip("Bold the server's +XP / +Energy title popups."),
				Options.toggle("PrisonBreak texture pack", d.prisonbreakTexturePackEnabled,
						() -> c.prisonbreakTexturePackEnabled, v -> { c.prisonbreakTexturePackEnabled = v; c.save(); })
						.tooltip("Auto-apply the bundled ore pack in the PrisonBreak world.")));

		return ConfigPanel.of("prisons-qol", "Quality of life",
				"Item scale, interactions, drop protection & extras", PanelIcon.SLIDERS,
				List.of(heldItem, interactions, pickaxeDrop, blink, extras));
	}
}
