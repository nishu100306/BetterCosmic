package dev.nishu.bettercosmic.prisons.chestsearch;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;

import java.util.ArrayList;
import java.util.List;

/**
 * Decides the highlight color for a slot's item stack: filter-rule matches take priority when the
 * sidebar is open with active rules, otherwise the simple text query is used. Ported from
 * BetterPrisons (Yarn → Mojang).
 */
public final class ChestSearchMatcher {

	/** Default highlight color: 50% alpha lime green (ARGB). */
	public static final int DEFAULT_COLOR = 0x8032CD32;
	/** Sentinel meaning "no highlight". */
	public static final int NO_MATCH = 0;

	private ChestSearchMatcher() {}

	public static int matchColor(ItemStack stack) {
		if (stack == null || stack.isEmpty()) {
			return NO_MATCH;
		}
		if (ChestSearchFilterState.sidebarOpen && ChestSearchFilterState.hasActiveRules()) {
			String name = stack.getHoverName().getString();
			return ChestSearchFilterState.evaluate(name, loreLines(stack), bookAttributes(stack));
		}
		String query = ChestSearchState.query;
		if (query == null || query.isEmpty()) {
			return NO_MATCH;
		}
		return matchesSimple(stack, query) ? DEFAULT_COLOR : NO_MATCH;
	}

	private static boolean matchesSimple(ItemStack stack, String query) {
		String q = query.toLowerCase();
		if (stack.getHoverName().getString().toLowerCase().contains(q)) {
			return true;
		}
		for (String line : loreLines(stack)) {
			if (line.toLowerCase().contains(q)) {
				return true;
			}
		}
		return false;
	}

	private static List<String> loreLines(ItemStack stack) {
		List<String> lines = new ArrayList<>();
		ItemLore lore = stack.get(DataComponents.LORE);
		if (lore != null) {
			for (Component l : lore.lines()) {
				lines.add(l.getString());
			}
		}
		return lines;
	}

	/** Enchant-book attributes (success %, destroy %, energy cost) from custom data, or null. */
	private static ChestSearchFilterRule.BookAttributes bookAttributes(ItemStack stack) {
		try {
			CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
			if (customData == null) {
				return null;
			}
			CompoundTag bukkit = customData.copyTag().getCompound("PublicBukkitValues").orElse(null);
			if (bukkit == null || bukkit.isEmpty()) {
				return null;
			}
			if (!"gear_enchant_book".equals(bukkit.getString("cosmicprisons:custom_item_id").orElse(""))) {
				return null;
			}
			double success = bukkit.getDouble("cosmicprisons:gear_enchant_success").orElse(0.0) * 100.0;
			double destroy = bukkit.getDouble("cosmicprisons:gear_enchant_destroy").orElse(0.0) * 100.0;
			double energy = bukkit.getDouble("cosmicprisons:gear_enchant_required").orElse(0.0);
			return new ChestSearchFilterRule.BookAttributes(success, destroy, energy);
		} catch (Exception e) {
			return null;
		}
	}
}
