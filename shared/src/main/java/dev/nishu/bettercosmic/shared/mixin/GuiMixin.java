package dev.nishu.bettercosmic.shared.mixin;

import dev.nishu.bettercosmic.shared.easyview.EasyView;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Renders EasyView slot overlays on hotbar/offhand item slots, right after each item is drawn, at
 * the item's own coordinates — keeping the overlay directly above the item texture.
 */
@Mixin(Gui.class)
public abstract class GuiMixin {

	@Inject(method = "renderSlot", at = @At("TAIL"))
	private void bettercosmicshared$easyViewHotbarOverlay(GuiGraphics guiGraphics, int x, int y,
			DeltaTracker deltaTracker, Player player, ItemStack itemStack, int seed, CallbackInfo ci) {
		if (!itemStack.isEmpty()) {
			EasyView.renderSlotTints(guiGraphics, x, y, itemStack);
			EasyView.renderSlotOverlays(guiGraphics, x, y, itemStack);
		}
	}
}
