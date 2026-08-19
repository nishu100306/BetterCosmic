package dev.nishu.bettercosmic.shared.mixin;

import dev.nishu.bettercosmic.shared.peacefulmining.PeacefulMining;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Strips the outer skin layers and cape from peaceful-mining ghost players, so the translucent
 * silhouette is just the body — a cape or second skin layer would defeat the "see through players"
 * effect. Armor and other feature layers are already suppressed by
 * {@link LivingEntityRendererPeacefulMiningMixin} ({@code shouldRenderLayers=false}); this handles the
 * parts baked into the render state instead.
 *
 * <p>Injected at the tail of {@code AvatarRenderer.extractRenderState} (targeted by the erased
 * descriptor — its first parameter is the {@code AvatarlikeEntity} type variable, erasing to
 * {@link Avatar}). Content-agnostic — gated on the shared {@link PeacefulMining} target set. Ported
 * from BetterPrisons' {@code PeacefulMiningMixin} skin-stripping (Yarn → Mojang).
 */
@Mixin(AvatarRenderer.class)
public class AvatarRendererPeacefulMiningMixin {

	@Inject(method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V",
			at = @At("TAIL"))
	private void bettercosmicshared$stripGhostLayers(Avatar entity, AvatarRenderState state, float partialTick, CallbackInfo ci) {
		if (!PeacefulMining.isTarget(state.id)) {
			return;
		}
		state.showHat = false;
		state.showJacket = false;
		state.showLeftSleeve = false;
		state.showRightSleeve = false;
		state.showLeftPants = false;
		state.showRightPants = false;
		state.showCape = false;
	}
}
