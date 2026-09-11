package dev.nishu.bettercosmic.shared.chestsearch;

import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * One row in the no-code filter sidebar: a {@link FilterType} + user value + an auto-assigned
 * highlight color. Shared across mods; the set of selectable types comes from
 * {@link ChestSearchRegistry} for the active network.
 */
public class FilterRule {

	public FilterType type;
	public String value = "";
	public int color = 0x8032CD32; // lime (auto-assigned on creation)

	public FilterRule(FilterType type) {
		this.type = type;
	}

	public boolean isActive() {
		return value != null && !value.isEmpty();
	}

	public boolean matches(ItemStack stack, String name, List<String> lore) {
		return isActive() && type != null && type.matches(value, stack, name, lore);
	}
}
