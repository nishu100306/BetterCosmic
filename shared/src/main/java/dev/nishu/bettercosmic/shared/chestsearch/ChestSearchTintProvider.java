package dev.nishu.bettercosmic.shared.chestsearch;

import dev.nishu.bettercosmic.shared.easyview.SlotTintProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.item.ItemStack;

/**
 * Highlights container slots whose item matches the chest-search query / filter rules, via the shared
 * EasyView {@link SlotTintProvider}. Registered once by the shared library (network-agnostic); gated
 * on {@link ChestSearchRegistry#enabled()} and on a container screen being open, so the highlight
 * never leaks onto the bare hotbar or an off-network server.
 */
public final class ChestSearchTintProvider implements SlotTintProvider {

	@Override
	public int tint(ItemStack stack) {
		if (!ChestSearchRegistry.enabled()) {
			return 0;
		}
		if (!(Minecraft.getInstance().screen instanceof AbstractContainerScreen<?>)) {
			return 0;
		}
		return ChestSearchMatcher.matchColor(stack);
	}
}
