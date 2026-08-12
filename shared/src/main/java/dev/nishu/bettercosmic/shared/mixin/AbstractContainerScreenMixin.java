package dev.nishu.bettercosmic.shared.mixin;

import dev.nishu.bettercosmic.shared.easyview.EasyView;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Renders EasyView slot overlays right after a container slot's item is drawn, at the item's own
 * coordinates. {@code renderSlot} performs no pose manipulation, so the overlay lands directly on
 * the item texture — and, crucially, before the screen draws its tooltip, so tooltips stay on top.
 */
@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin {

	@Inject(method = "renderSlot", at = @At("TAIL"))
	private void bettercosmicshared$easyViewSlotOverlay(GuiGraphics guiGraphics, Slot slot,
			int leftPos, int topPos, CallbackInfo ci) {
		ItemStack stack = slot.getItem();
		if (!stack.isEmpty()) {
			EasyView.renderSlotOverlays(guiGraphics, slot.x, slot.y, stack);
		}
	}
}
