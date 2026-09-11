package dev.nishu.bettercosmic.shared.chestsearch;

import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * A kind of chest-search filter rule. The shared framework ships only {@link ChestSearchRegistry#NAME}
 * (name/lore contains); each mod registers its own item-specific types (e.g. BetterPrisons' enchant-book
 * percentage filters and clue-scroll step) so the shared code stays free of server-specific item logic.
 *
 * <p>The sidebar cycles a rule through the {@link ChestSearchRegistry#availableTypes() available types}
 * for the active network; {@link #matches} is handed the stack plus its pre-computed display name and
 * lore lines so a type can read whatever it needs (custom-data NBT, lore, …).
 */
public interface FilterType {

	/** Stable identifier (also used for equality when cycling). */
	String id();

	/** Short label shown on the sidebar's type-cycle button, e.g. {@code "name"} or {@code "succ% >"}. */
	String label();

	/** @return whether {@code stack} matches this filter for the user-entered {@code value}. */
	boolean matches(String value, ItemStack stack, String name, List<String> lore);
}
