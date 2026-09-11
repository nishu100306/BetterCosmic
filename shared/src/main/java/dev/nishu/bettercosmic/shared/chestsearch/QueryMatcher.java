package dev.nishu.bettercosmic.shared.chestsearch;

import net.minecraft.world.item.ItemStack;

/**
 * An extra match for the inline text query, beyond the built-in name/lore contains. Mods register
 * these for special cases — e.g. BetterPrisons highlights a clue scroll when the query is the bare
 * number of its current step.
 */
@FunctionalInterface
public interface QueryMatcher {

	/** @return whether {@code stack} should be highlighted for the raw text {@code query}. */
	boolean matches(String query, ItemStack stack);
}
