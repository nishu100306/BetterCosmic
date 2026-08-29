package dev.nishu.bettercosmic.prisons.chestsearch;

import dev.nishu.bettercosmic.prisons.client.BetterPrisonsClient;
import dev.nishu.bettercosmic.shared.easyview.SlotTintProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.item.ItemStack;

/**
 * Highlights container slots whose item matches the chest-search query / filter rules, via the shared
 * EasyView {@link SlotTintProvider}. Gated on {@code chestSearchEnabled} and on a container screen
 * being open, so the highlight doesn't leak onto the bare hotbar from a stale query.
 */
public final class ChestSearchTintProvider implements SlotTintProvider {

	@Override
	public int tint(ItemStack stack) {
		if (BetterPrisonsClient.config == null || !BetterPrisonsClient.config.chestSearchEnabled) {
			return 0;
		}
		if (!(Minecraft.getInstance().screen instanceof AbstractContainerScreen<?>)) {
			return 0;
		}
		return ChestSearchMatcher.matchColor(stack);
	}
}
