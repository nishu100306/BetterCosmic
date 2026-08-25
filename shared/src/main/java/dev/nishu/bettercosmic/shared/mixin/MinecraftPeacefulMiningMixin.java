package dev.nishu.bettercosmic.shared.mixin;

import dev.nishu.bettercosmic.shared.peacefulmining.PeacefulMining;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Suppresses item use (right-click) while the player is actively mining and peaceful mining is active,
 * so you can't accidentally use an item on the block behind a translucent player you're mining through.
 * Content-agnostic — gated on the shared {@link PeacefulMining} policy plus the vanilla "destroying a
 * block" state, so normal right-click use still works when you aren't mid-swing. Companion to the other
 * peaceful-mining mixins (ghost render, block-through targeting, interaction blocking).
 */
@Mixin(Minecraft.class)
public class MinecraftPeacefulMiningMixin {

	@Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true)
	private void bettercosmicshared$suppressUseWhileMining(CallbackInfo ci) {
		Minecraft self = (Minecraft) (Object) this;
		if (PeacefulMining.isActive() && self.gameMode != null && self.gameMode.isDestroying()) {
			ci.cancel();
		}
	}
}
