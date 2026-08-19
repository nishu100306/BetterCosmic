package dev.nishu.bettercosmic.shared.mixin;

import dev.nishu.bettercosmic.shared.render.WorldSpaceTransform;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Captures the effective world-render FOV each frame so {@link WorldSpaceTransform} projects
 * waypoints correctly, including under any zoom mod. Content-agnostic. Ported from BetterPrisons'
 * {@code GameRendererMixin} FOV-capture (Yarn → Mojang: {@code renderWorld} → {@code renderLevel}).
 *
 * <p>Why {@link ModifyArg} on the {@code renderLevel → getProjectionMatrix} call site rather than an
 * inject on {@code getFov}: a zoom mod (e.g. Zoomify) may apply its FOV change via a MixinExtras
 * {@code @ModifyReturnValue} on {@code getFov} that could be ordered after a plain {@code @Inject} at
 * {@code RETURN}, which would then read the un-zoomed value. Intercepting the {@code fov} argument at
 * the projection-matrix call site sees the value after <em>all</em> mixin transformations, and scoping
 * to {@code renderLevel} avoids the separate held-item projection.
 */
@Mixin(GameRenderer.class)
public class GameRendererFovMixin {

	@ModifyArg(
			method = "renderLevel",
			at = @At(value = "INVOKE",
					target = "Lnet/minecraft/client/renderer/GameRenderer;getProjectionMatrix(F)Lorg/joml/Matrix4f;"),
			index = 0)
	private float bettercosmicshared$captureWorldRenderFov(float fov) {
		WorldSpaceTransform.captureFov(fov);
		return fov;
	}
}
