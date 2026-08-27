package dev.nishu.bettercosmic.prisons.mixin;

import dev.nishu.bettercosmic.prisons.client.BetterPrisonsClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Blocks dragging a pickaxe out of a container or throwing it from a slot (Q / Ctrl+Q) when the
 * "drag/drop block" QoL setting is on. Ported from BetterPrisons' {@code InventoryDropMixin}
 * (Yarn → Mojang: {@code HandledScreen.onMouseClick} → {@code AbstractContainerScreen.slotClicked},
 * {@code SlotActionType} → {@code ClickType}, {@code getCursorStack} → {@code getCarried},
 * {@code slot.getStack} → {@code slot.getItem}).
 */
@Mixin(AbstractContainerScreen.class)
public class InventoryDropMixin {

	@Inject(method = "slotClicked", at = @At("HEAD"), cancellable = true)
	private void bettercosmic$blockPickaxeDrop(Slot slot, int slotId, int button, ClickType actionType, CallbackInfo ci) {
		if (!BetterPrisonsClient.config.pickaxeDropConfirmationEnabled
				|| !BetterPrisonsClient.config.pickaxeDropDragBlockEnabled) {
			return;
		}
		Minecraft client = Minecraft.getInstance();
		if (client.player == null) {
			return;
		}

		// Dropping the carried item outside the inventory (slotId == -999).
		if (slotId == -999 && (actionType == ClickType.PICKUP || actionType == ClickType.QUICK_CRAFT)) {
			ItemStack carried = client.player.containerMenu.getCarried();
			if (!carried.isEmpty() && isPickaxe(carried)) {
				client.player.displayClientMessage(
						Component.literal("§c§l[!] §cPickaxe dragging out of inventory is disabled."), false);
				ci.cancel();
				return;
			}
		}

		// Q / Ctrl+Q throw while hovering a slot.
		if (actionType == ClickType.THROW && slot != null) {
			ItemStack slotStack = slot.getItem();
			if (!slotStack.isEmpty() && isPickaxe(slotStack)) {
				client.player.displayClientMessage(
						Component.literal("§c§l[!] §cPickaxe dropping is disabled."), false);
				ci.cancel();
			}
		}
	}

	private static boolean isPickaxe(ItemStack stack) {
		return stack.getItem().getDescriptionId().toLowerCase().contains("pickaxe");
	}
}
