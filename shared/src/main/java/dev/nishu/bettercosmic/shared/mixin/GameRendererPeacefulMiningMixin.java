package dev.nishu.bettercosmic.shared.mixin;

import dev.nishu.bettercosmic.shared.peacefulmining.PeacefulMining;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Forces block-only crosshair targeting while peaceful mining is active, so you can mine through the
 * translucent ghost players instead of targeting them. Runs after vanilla picking and replaces a
 * non-block hit with a block raycast. Content-agnostic — gated on the shared {@link PeacefulMining}
 * engine. Ported from BetterPrisons' {@code GameRendererMixin} crosshair logic (Yarn → Mojang).
 */
@Mixin(GameRenderer.class)
public class GameRendererPeacefulMiningMixin {

	@Inject(method = "pick", at = @At("TAIL"))
	private void bettercosmicshared$mineThroughGhosts(float partialTick, CallbackInfo ci) {
		if (!PeacefulMining.isActive()) {
			return;
		}
		Minecraft client = Minecraft.getInstance();
		if (client.player == null || client.level == null) {
			return;
		}
		// Already a block hit — leave it alone.
		if (client.hitResult instanceof BlockHitResult) {
			return;
		}
		Vec3 eye = client.player.getEyePosition(1.0f);
		Vec3 look = client.player.getViewVector(1.0f);
		double reach = client.player.blockInteractionRange();
		Vec3 end = eye.add(look.scale(reach));
		ClipContext ctx = new ClipContext(eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, client.player);
		HitResult blockHit = client.level.clip(ctx);
		client.hitResult = blockHit;
	}
}
