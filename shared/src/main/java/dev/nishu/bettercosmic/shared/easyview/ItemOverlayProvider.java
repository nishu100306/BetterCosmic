package dev.nishu.bettercosmic.shared.easyview;

import net.minecraft.world.item.ItemStack;

/**
 * Supplies an {@link SlotOverlay} for a given item stack, or {@code null} if the item should have
 * no overlay. Mods register providers with {@link EasyView#register(ItemOverlayProvider)}; each
 * provider is responsible for its own gating (e.g. a config toggle).
 *
 * <p>This is the extension point that keeps server-specific item logic (Cosmic Sky trinkets,
 * Cosmic Prisons satchels, ...) out of the shared library.
 */
@FunctionalInterface
public interface ItemOverlayProvider {

	/** @return the overlay to draw for {@code stack}, or {@code null} for none. */
	SlotOverlay getOverlay(ItemStack stack);
}
