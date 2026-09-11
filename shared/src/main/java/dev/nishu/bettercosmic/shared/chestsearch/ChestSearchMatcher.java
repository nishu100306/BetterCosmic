package dev.nishu.bettercosmic.shared.chestsearch;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;

import java.util.ArrayList;
import java.util.List;

/**
 * Decides the highlight color for a slot's item: filter-rule matches take priority when the sidebar
 * is open with active rules, otherwise the inline text query is used. Item-specific matching is
 * delegated to the {@link ChestSearchRegistry} (the active network's filter types + query matchers),
 * so this class stays free of server-specific item logic.
 */
public final class ChestSearchMatcher {

	/** Default highlight color for a plain text-query match: 50% alpha lime green (ARGB). */
	public static final int DEFAULT_COLOR = 0x8032CD32;
	/** Sentinel meaning "no highlight". */
	public static final int NO_MATCH = 0;

	private ChestSearchMatcher() {}

	public static int matchColor(ItemStack stack) {
		if (stack == null || stack.isEmpty()) {
			return NO_MATCH;
		}
		String name = stack.getHoverName().getString();
		List<String> lore = loreLines(stack);

		if (FilterState.sidebarOpen && FilterState.hasActiveRules()) {
			return FilterState.evaluate(stack, name, lore);
		}
		String query = ChestSearchState.query;
		if (query == null || query.isEmpty()) {
			return NO_MATCH;
		}
		return ChestSearchRegistry.simpleQueryMatches(query, stack, name, lore) ? DEFAULT_COLOR : NO_MATCH;
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
}
