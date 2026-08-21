package dev.nishu.bettercosmic.prisons.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.nishu.bettercosmic.prisons.client.BetterPrisonsClient;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Scales the first-person held item by a per-item-type factor (pickaxe / sword / axe / other) from the
 * config. Ported from BetterPrisons' {@code HeldItemScaleMixin} (Yarn → Mojang: {@code HeldItemRenderer.
 * renderFirstPersonItem} → {@code ItemInHandRenderer.renderItem}; the render queue changed from
 * {@code OrderedRenderCommandQueue} to {@code SubmitNodeCollector}). Scaling the pose at the head of
 * {@code renderItem} — after the arm transforms are applied — matches the original injection point.
 */
@Mixin(ItemInHandRenderer.class)
public class HeldItemScaleMixin {

	@Inject(method = "renderItem", at = @At("HEAD"))
	private void bettercosmic$scaleHeldItem(LivingEntity entity, ItemStack stack, ItemDisplayContext ctx,
			PoseStack pose, SubmitNodeCollector collector, int light, CallbackInfo ci) {
		float scale = itemScale(stack);
		if (scale != 1.0f) {
			pose.scale(scale, scale, scale);
		}
	}

	private static float itemScale(ItemStack stack) {
		if (stack.isEmpty()) {
			return 1.0f;
		}
		String name = stack.getItem().toString().toLowerCase();
		if (name.contains("pickaxe")) {
			return BetterPrisonsClient.config.heldItemPickaxeScale / 100.0f;
		}
		if (name.contains("sword")) {
			return BetterPrisonsClient.config.heldItemSwordScale / 100.0f;
		}
		if (name.contains("axe")) {
			return BetterPrisonsClient.config.heldItemAxeScale / 100.0f;
		}
		return BetterPrisonsClient.config.heldItemOtherScale / 100.0f;
	}
}
