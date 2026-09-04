package dev.nishu.bettercosmic.prisons.misc;

import dev.nishu.bettercosmic.prisons.client.BetterPrisonsClient;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Adds per-level Cosmic-Energy cost lines to Cosmic Prisons gear enchant books (both inventory items
 * and chat-hover SHOW_TEXT bodies). The current level-up cost is read exactly from NBT / the energy
 * bar; deeper levels are projected as {@code level² × tierMultiplier}. Ported from BetterPrisons'
 * {@code misc/EnchantBookTooltip} (Yarn → Mojang: {@code LoreComponent} → {@code ItemLore},
 * {@code NbtComponent.copyNbt} → {@code CustomData.copyTag}, {@code TextColor.getRgb} →
 * {@code getValue}, {@code stack.isOf} → {@code stack.is}).
 */
public final class EnchantBookTooltip {

	private static final double DISPLAY_COST_FACTOR = 1.75;

	private static final Pattern MAX_LEVEL_PATTERN = Pattern.compile("Max Level:\\s*([IVXLCDM]+)");
	private static final Pattern NAME_LEVEL_PATTERN = Pattern.compile("\\s([IVXLCDM]+)\\s*\\(\\s*\\d+\\s*%\\s*\\)");
	private static final Pattern ENERGY_LINE_PATTERN = Pattern.compile("(\\d[\\d,]*)\\s*/\\s*(\\d[\\d,]*)");
	private static final int GOLD_RGB = 0xFFAA00; // LEGENDARY enchant name color

	private EnchantBookTooltip() {}

	public static void append(ItemStack stack, List<Component> lines) {
		try {
			if (!BetterPrisonsClient.config.enchantBookCostsEnabled) {
				return;
			}
			if (stack == null || stack.isEmpty()) {
				return;
			}

			int currentLevel = 0;
			double currentCost = 0; // displayed level-up cost (current level -> next)
			String tier = "";

			// Preferred source: custom_data NBT (present on inventory items).
			CompoundTag bukkit = getBukkit(stack);
			if (bukkit != null && "gear_enchant_book".equals(bukkit.getStringOr("cosmicprisons:custom_item_id", ""))) {
				currentLevel = bukkit.getIntOr("cosmicprisons:gear_enchant_level", 0);
				double required = bukkit.getDoubleOr("cosmicprisons:gear_enchant_required", 0.0);
				currentCost = required * DISPLAY_COST_FACTOR;
				tier = bukkit.getStringOr("cosmicprisons:gear_enchant_tier", "");
			}

			// Fall back to the visible lore when the NBT path didn't yield usable data.
			if (currentLevel < 1 || currentCost <= 0) {
				if (!looksLikeEnchantBook(stack)) {
					return;
				}
				currentLevel = parseCurrentLevelFromName(stack);
				currentCost = parseDisplayedCostFromLore(stack);
				if (tier.isEmpty()) {
					tier = isGoldName(stack) ? "LEGENDARY" : "RARE";
				}
			}

			if (currentLevel < 1 || currentCost <= 0) {
				return;
			}
			int maxLevel = parseMaxLevel(stack);
			if (maxLevel <= currentLevel) {
				return;
			}
			appendCostLines(currentLevel, currentCost, maxLevel, tier, lines::add);
		} catch (Exception e) {
			// Tooltip code must never crash the game — silently ignore.
		}
	}

	/**
	 * Emits one cost line per level from {@code currentLevel + 1} up to {@code maxLevel}.
	 *
	 * <p>Only {@code currentCost} is measured — the real cost of the next level-up, i.e. of leaving
	 * {@code currentLevel}. A level-up's cost scales with the {@link #relativeCost weight} of the
	 * level being left, so the step to {@code level} is projected from that level's predecessor:
	 * {@code currentCost} times {@code relativeCost(level - 1)} over the anchor weight. The anchor is
	 * {@code currentLevel} itself, so the measured next level-up comes back out as exactly
	 * {@code currentCost}.
	 */
	private static void appendCostLines(int currentLevel, double currentCost, int maxLevel,
			String tier, Consumer<Component> sink) {
		int rgb = BetterPrisonsClient.config.enchantBookCostsColor & 0xFFFFFF;
		Style style = Style.EMPTY.withColor(TextColor.fromRgb(rgb)).withItalic(false);

		double anchorWeight = relativeCost(tier, currentLevel);
		if (anchorWeight <= 0) {
			return;
		}

		long runningTotal = 0;
		for (int level = currentLevel + 1; level <= maxLevel; level++) {
			long cost = Math.round(currentCost * relativeCost(tier, level - 1) / anchorWeight);
			runningTotal += cost;

			String text = "[BP] L" + (level - 1) + "→L" + level
					+ ": " + formatNumber(cost)
					+ "  |  Total " + formatNumber(runningTotal);
			sink.accept(Component.literal(text).setStyle(style));
		}
	}

	/**
	 * Unit-less weight standing in for a level's cost. Costs grow with the square of the level, and
	 * the high-cost tiers additionally cost {@code tierMultiplier}× from level 3 on. Only the ratio
	 * between two levels' weights is meaningful; see {@link #appendCostLines}.
	 */
	private static double relativeCost(String tier, int level) {
		return (double) level * level * tierMultiplier(tier, level);
	}

	/**
	 * Per-level cost multiplier. Levels 1 and 2 are always 1×. From level 3 on, the high-cost tiers
	 * (LEGENDARY, plus any unrecognised or future tier) jump to 3×; every known lower tier stays 1×.
	 */
	private static int tierMultiplier(String tier, int level) {
		if (level <= 2) {
			return 1;
		}
		switch (tier.toUpperCase()) {
			case "SIMPLE":
			case "COMMON":
			case "UNCOMMON":
			case "RARE":
			case "ELITE":
			case "ULTIMATE":
				return 1;
			case "LEGENDARY":
				return 3;
			default:
				return 3; // unknown / future tiers — assume the high-cost pattern
		}
	}

	/**
	 * Appends level-up cost lines to a chat hover's SHOW_TEXT body when it looks like an enchant book.
	 * Chat hovers carry no item — everything is parsed from the hover text. The energy line already
	 * shows the displayed cost, so no ×1.75 is applied. Tier is inferred from a gold first line.
	 */
	public static Component appendChatHoverCost(Component hoverText) {
		try {
			if (hoverText == null || !BetterPrisonsClient.config.enchantBookCostsEnabled) {
				return hoverText;
			}
			String full = hoverText.getString();
			if (!full.contains("Enchant Chance") || !full.contains("Max Level:")) {
				return hoverText;
			}
			String[] lines = full.split("\n");
			int currentLevel = 0;
			double currentCost = 0;
			int maxLevel = 0;
			for (String l : lines) {
				if (currentLevel == 0) {
					Matcher m = NAME_LEVEL_PATTERN.matcher(l);
					if (m.find()) {
						currentLevel = parseRoman(m.group(1));
					}
				}
				if (currentCost <= 0) {
					Matcher m = ENERGY_LINE_PATTERN.matcher(l);
					if (m.find()) {
						try {
							currentCost = Double.parseDouble(m.group(2).replace(",", ""));
						} catch (NumberFormatException ignored) {
							// keep scanning
						}
					}
				}
				if (maxLevel == 0) {
					Matcher m = MAX_LEVEL_PATTERN.matcher(l);
					if (m.find()) {
						maxLevel = parseRoman(m.group(1));
					}
				}
			}
			if (currentLevel < 1 || currentCost <= 0 || maxLevel <= currentLevel) {
				return hoverText;
			}
			String tier = firstLineGold(hoverText) ? "LEGENDARY" : "RARE";
			MutableComponent result = hoverText.copy();
			appendCostLines(currentLevel, currentCost, maxLevel, tier,
					t -> result.append(Component.literal("\n")).append(t));
			return result;
		} catch (Exception e) {
			return hoverText;
		}
	}

	private static boolean firstLineGold(Component text) {
		Optional<Boolean> result = text.visit((style, str) -> {
			String seg = str;
			boolean endOfLine = false;
			int nl = seg.indexOf('\n');
			if (nl >= 0) {
				seg = seg.substring(0, nl);
				endOfLine = true;
			}
			TextColor c = style.getColor();
			if (c != null && c.getValue() == GOLD_RGB && !seg.trim().isEmpty()) {
				return Optional.of(Boolean.TRUE);
			}
			if (endOfLine) {
				return Optional.of(Boolean.FALSE);
			}
			return Optional.empty();
		}, Style.EMPTY);
		return result.orElse(false);
	}

	private static CompoundTag getBukkit(ItemStack stack) {
		CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
		if (customData == null) {
			return null;
		}
		CompoundTag bukkit = customData.copyTag().getCompound("PublicBukkitValues").orElse(null);
		return (bukkit == null || bukkit.isEmpty()) ? null : bukkit;
	}

	private static boolean looksLikeEnchantBook(ItemStack stack) {
		if (!stack.is(Items.ENCHANTED_BOOK)) {
			return false;
		}
		ItemLore lore = stack.get(DataComponents.LORE);
		if (lore == null) {
			return false;
		}
		boolean hasChance = false, hasMax = false;
		for (Component line : lore.lines()) {
			String s = line.getString();
			if (s.contains("Enchant Chance")) {
				hasChance = true;
			}
			if (s.contains("Max Level:")) {
				hasMax = true;
			}
		}
		return hasChance && hasMax;
	}

	private static int parseCurrentLevelFromName(ItemStack stack) {
		Matcher m = NAME_LEVEL_PATTERN.matcher(stack.getHoverName().getString());
		return m.find() ? parseRoman(m.group(1)) : 0;
	}

	private static double parseDisplayedCostFromLore(ItemStack stack) {
		ItemLore lore = stack.get(DataComponents.LORE);
		if (lore == null) {
			return 0;
		}
		for (Component line : lore.lines()) {
			Matcher m = ENERGY_LINE_PATTERN.matcher(line.getString());
			if (m.find()) {
				try {
					return Double.parseDouble(m.group(2).replace(",", ""));
				} catch (NumberFormatException e) {
					return 0;
				}
			}
		}
		return 0;
	}

	private static boolean isGoldName(ItemStack stack) {
		Component name = stack.getHoverName();
		TextColor c = name.getStyle().getColor();
		if (c != null && c.getValue() == GOLD_RGB) {
			return true;
		}
		for (Component sibling : name.getSiblings()) {
			TextColor sc = sibling.getStyle().getColor();
			if (sc != null && sc.getValue() == GOLD_RGB) {
				return true;
			}
		}
		return false;
	}

	private static int parseMaxLevel(ItemStack stack) {
		ItemLore lore = stack.get(DataComponents.LORE);
		if (lore == null) {
			return 0;
		}
		for (Component line : lore.lines()) {
			Matcher m = MAX_LEVEL_PATTERN.matcher(line.getString());
			if (m.find()) {
				return parseRoman(m.group(1));
			}
		}
		return 0;
	}

	private static int parseRoman(String s) {
		int result = 0, prev = 0;
		for (int i = s.length() - 1; i >= 0; i--) {
			int val = romanValue(s.charAt(i));
			if (val == 0) {
				return 0;
			}
			if (val < prev) {
				result -= val;
			} else {
				result += val;
			}
			prev = val;
		}
		return result;
	}

	private static int romanValue(char c) {
		switch (c) {
			case 'I': return 1;
			case 'V': return 5;
			case 'X': return 10;
			case 'L': return 50;
			case 'C': return 100;
			case 'D': return 500;
			case 'M': return 1000;
			default: return 0;
		}
	}

	private static String formatNumber(long n) {
		return String.format("%,d", n);
	}
}
