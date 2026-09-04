package dev.nishu.bettercosmic.prisons.misc;

import dev.nishu.bettercosmic.prisons.misc.EnergyCalculator.PickType;
import dev.nishu.bettercosmic.shared.ui.model.ConfigPanel;
import dev.nishu.bettercosmic.shared.ui.model.Option;
import dev.nishu.bettercosmic.shared.ui.model.OptionGroup;
import dev.nishu.bettercosmic.shared.ui.model.Options;
import dev.nishu.bettercosmic.shared.ui.model.PanelIcon;

import java.util.List;

/**
 * Config panel hosting the pickaxe {@link EnergyCalculator}. Choose a pickaxe, a level range and a
 * prestige stage; the result labels recompute live (the shared {@code label(Supplier)} re-resolves
 * each frame), so there is no button or command — just the readout.
 *
 * <p>The panel uses a single blank-label {@link OptionGroup}, which the shared popup renders headerless
 * and always open, so the whole calculator reads as one flat list with no collapsible sections. Result
 * rows are plain {@code label} strings drawn through {@code drawString}, so they honour legacy
 * {@code §} colour codes for the pickaxe tint and highlighted numbers. The selections are transient UI
 * scratch state, not persisted settings, so they are held in plain static fields rather than
 * {@code PrisonsConfig}.
 */
public final class EnergyCalculatorPanel {

	private static String pickChoice = "Stone";
	private static int startLevel = 1;
	private static int endLevel = 100;
	private static int prestige = 0;

	private EnergyCalculatorPanel() {}

	public static ConfigPanel create() {
		OptionGroup all = new OptionGroup("", List.<Option>of(
				Options.dropdown("Pickaxe", "Stone", List.of("Stone", "Iron", "Diamond"),
						() -> pickChoice, v -> pickChoice = v),
				Options.intSlider("Start level", 1, 1, EnergyCalculator.MAX_LEVEL - 1, 1,
						() -> startLevel, v -> startLevel = v),
				Options.intSlider("End level", EnergyCalculator.MAX_LEVEL,
						EnergyCalculator.MIN_LEVEL, EnergyCalculator.MAX_LEVEL, 1,
						() -> endLevel, v -> endLevel = v),
				Options.intSlider("Prestige (P)", 0, EnergyCalculator.MIN_PRESTIGE, EnergyCalculator.MAX_PRESTIGE, 1,
						() -> prestige, v -> prestige = v),
				Options.label(EnergyCalculatorPanel::headingLine),
				Options.label(EnergyCalculatorPanel::rangeLine),
				Options.label(EnergyCalculatorPanel::nextLevelLine)));

		return ConfigPanel.of("prisons-energy-calc", "Energy calculator",
				"Pickaxe upgrade energy costs", PanelIcon.PICKAXE, List.of(all));
	}

	/** Coloured heading naming the selected pickaxe. */
	private static String headingLine() {
		return pickColor() + "◆ " + pickChoice + " pickaxe";
	}

	/** Total energy to go from the start level up to the end level at the chosen prestige. */
	private static String rangeLine() {
		if (endLevel <= startLevel) {
			return "§cEnd level must be above the start level.";
		}
		long total = EnergyCalculator.rangeCost(pickType(), startLevel, endLevel, prestige);
		return "§7Total §fL" + startLevel + " §7→ §fL" + endLevel + " §7at §fP" + prestige
				+ "§7:  §a" + EnergyCalculator.formatEnergy(total) + " §7energy";
	}

	/** Cost of just the single upgrade that reaches the end level. */
	private static String nextLevelLine() {
		long one = EnergyCalculator.singleLevelCost(pickType(), endLevel, prestige);
		return "§8Reaching L" + endLevel + " alone: §e" + EnergyCalculator.formatEnergy(one) + " §8energy";
	}

	private static PickType pickType() {
		return PickType.valueOf(pickChoice.toUpperCase());
	}

	/** Legacy colour code tinting the selected pickaxe: Stone gray, Iron white, Diamond aqua. */
	private static String pickColor() {
		return switch (pickChoice) {
			case "Iron" -> "§f";
			case "Diamond" -> "§b";
			default -> "§7";
		};
	}
}
