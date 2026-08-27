package dev.nishu.bettercosmic.shared.mixin;

import dev.nishu.bettercosmic.shared.peacefulmining.PeacefulMining;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Renders peaceful-mining target players as translucent ghosts: a configurable alpha tint plus a
 * translucent render type so the alpha actually blends. Content-agnostic — targets and opacity come
 * from the shared {@link PeacefulMining} engine. Ported from BetterPrisons'
 * {@code PeacefulMiningRendererMixin} (Yarn → Mojang).
 *
 * <p>These two methods are inherited from {@link LivingEntityRenderer} (not overridden by
 * {@code AvatarRenderer}), so injecting here reaches players. Feature-layer suppression is handled
 * separately in {@code AvatarRendererPeacefulMiningMixin}, because {@code AvatarRenderer} <em>does</em>
 * override {@code shouldRenderLayers}.
 */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererPeacefulMiningMixin {

	@Shadow
	public abstract Identifier getTextureLocation(LivingEntityRenderState state);

	@Inject(method = "getModelTint", at = @At("HEAD"), cancellable = true)
	private void bettercosmicshared$ghostTint(LivingEntityRenderState state, CallbackInfoReturnable<Integer> cir) {
		if (state instanceof AvatarRenderState avatar && PeacefulMining.isTarget(avatar.id)) {
			int alpha = PeacefulMining.opacity();
			cir.setReturnValue((alpha << 24) | 0x00FFFFFF);
		}
	}

	@Inject(method = "getRenderType", at = @At("HEAD"), cancellable = true)
	private void bettercosmicshared$ghostLayer(LivingEntityRenderState state, boolean bodyVisible,
			boolean translucent, boolean glowing, CallbackInfoReturnable<RenderType> cir) {
		if (state instanceof AvatarRenderState avatar && PeacefulMining.isTarget(avatar.id)) {
			cir.setReturnValue(RenderTypes.itemEntityTranslucentCull(getTextureLocation(state)));
		}
	}
}
