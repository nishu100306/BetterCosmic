package dev.nishu.bettercosmic.shared.easyview;

import net.minecraft.world.item.ItemStack;

/**
 * Supplies a background tint color for an inventory slot, drawn over the item as a translucent
 * highlight. Returns an ARGB color (with an alpha byte) to tint the slot, or {@code 0} for no tint.
 * The companion to {@link ItemOverlayProvider} (text overlays); this fills the slot rect instead.
 *
 * <p>Extension point for slot highlighting — e.g. BetterPrisons' chest-search match highlight — that
 * keeps the item-matching logic in the mods and out of the shared library.
 */
@FunctionalInterface
public interface SlotTintProvider {

	/** @return an ARGB tint (alpha included) to fill the slot with, or {@code 0} for none. */
	int tint(ItemStack stack);
}
