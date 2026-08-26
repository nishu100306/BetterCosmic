package dev.nishu.bettercosmic.prisons.mixin;

import dev.nishu.bettercosmic.prisons.client.BetterPrisonsClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Lets the player use an item (right-click) while actively breaking a block, which vanilla otherwise
 * suppresses. {@link Minecraft#startUseItem()} bails early on {@code this.gameMode.isDestroying()}; when
 * the feature is enabled this redirect reports {@code false} so item use proceeds mid-swing. Ported from
 * BetterPrisons' {@code UseItemWhileMiningMixin} (Yarn → Mojang: {@code MinecraftClient.doItemUse} →
 * {@code Minecraft.startUseItem}, {@code ClientPlayerInteractionManager.isBreakingBlock} →
 * {@code MultiPlayerGameMode.isDestroying}). Gated on {@code useItemWhileMiningEnabled}; when off it
 * returns the real value, so vanilla behavior is unchanged.
 */
@Mixin(Minecraft.class)
public class UseItemWhileMiningMixin {

	@Redirect(method = "startUseItem", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;isDestroying()Z"))
	private boolean bettercosmic$allowUseWhileMining(MultiPlayerGameMode gameMode) {
		if (BetterPrisonsClient.config != null && BetterPrisonsClient.config.useItemWhileMiningEnabled) {
			return false; // pretend we're not breaking a block → item use proceeds
		}
		return gameMode.isDestroying();
	}
}
