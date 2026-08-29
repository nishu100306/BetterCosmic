package dev.nishu.bettercosmic.prisons.mixin;

import dev.nishu.bettercosmic.prisons.PrisonsGate;
import dev.nishu.bettercosmic.prisons.client.BetterPrisonsClient;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Intercepts the hotbar drop key to add pickaxe-drop protection. Ported from BetterPrisons'
 * {@code ItemDropMixin} (Yarn → Mojang: {@code ClientPlayerEntity.dropSelectedItem} →
 * {@code LocalPlayer.drop}, {@code Inventory.getSelectedStack} → {@code getSelectedItem}). Delegates
 * the block/confirm decision to {@code PickaxeDropConfirmation}.
 */
@Mixin(LocalPlayer.class)
public class ItemDropMixin {

	@Inject(method = "drop(Z)Z", at = @At("HEAD"), cancellable = true)
	private void bettercosmic$confirmPickaxeDrop(boolean entireStack, CallbackInfoReturnable<Boolean> cir) {
		if (!PrisonsGate.active()) {
			return;
		}
		LocalPlayer player = (LocalPlayer) (Object) this;
		ItemStack stack = player.getInventory().getSelectedItem();
		if (!BetterPrisonsClient.pickaxeDropConfirmation.canDrop(stack)) {
			cir.setReturnValue(false);
		}
	}
}
