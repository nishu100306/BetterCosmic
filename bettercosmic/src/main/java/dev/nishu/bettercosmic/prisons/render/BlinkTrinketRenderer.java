package dev.nishu.bettercosmic.prisons.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.nishu.bettercosmic.prisons.PrisonsGate;
import dev.nishu.bettercosmic.prisons.client.BetterPrisonsClient;
import dev.nishu.bettercosmic.shared.render.PositionColorLayers;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * While the player holds the Blink Trinket, highlights the block they'd teleport to — a raycast up to
 * the trinket's blink range (shorter if a wall is in the way), drawn as a translucent cube (fill +
 * outline) even when the destination is air. Ported from BetterPrisons' {@code render/BlinkTrinketRenderer}.
 *
 * <p>Blink-trinket content stays prison; the POSITION_COLOR depth-tested layer + the
 * {@code SubmitNodeCollector} draw path are shared ({@link PositionColorLayers}) — the same 1.21.11
 * render rewrite as the beacon beam. Needs an in-game pass.
 */
public final class BlinkTrinketRenderer {

	private static final String RANGE_KEY = "cosmicprisons:trinket_blink_range_blocks";

	private BlinkTrinketRenderer() {}

	public static void register() {
		WorldRenderEvents.END_MAIN.register(BlinkTrinketRenderer::render);
	}

	private static void render(WorldRenderContext ctx) {
		if (!PrisonsGate.active() || !BetterPrisonsClient.config.blinkOverlayEnabled) {
			return;
		}
		Minecraft client = Minecraft.getInstance();
		if (client.player == null || client.level == null || ctx.matrices() == null || ctx.commandQueue() == null) {
			return;
		}
		int range = heldBlinkRange(client.player);
		if (range <= 0) {
			return; // not holding a blink trinket
		}
		BlockPos dest = computeDestination(client.player, client.level, range);
		if (dest == null) {
			return;
		}

		Vec3 camera = client.gameRenderer.getMainCamera().position();
		var pose = ctx.matrices();
		pose.pushPose();
		pose.translate(dest.getX() - camera.x, dest.getY() - camera.y, dest.getZ() - camera.z);
		ctx.commandQueue().submitCustomGeometry(pose, PositionColorLayers.DEPTH,
				(entry, consumer) -> drawHighlight(entry.pose(), consumer));
		pose.popPose();
	}

	private static void drawHighlight(Matrix4f mat, VertexConsumer c) {
		var config = BetterPrisonsClient.config;

		int fillRgb = config.blinkOverlayColor;
		float fr = ((fillRgb >> 16) & 0xFF) / 255f;
		float fg = ((fillRgb >> 8) & 0xFF) / 255f;
		float fb = (fillRgb & 0xFF) / 255f;
		float fa = Math.max(0, Math.min(255, config.blinkOverlayOpacity)) / 255f;
		if (fa > 0f) {
			drawBox(mat, c, 0f, 0f, 0f, 1f, 1f, 1f, fr, fg, fb, fa);
		}

		int t = config.blinkOverlayOutlineThickness;
		if (t > 0) {
			float h = t / 64f; // half-thickness in world units
			int oRgb = config.blinkOverlayOutlineColor;
			drawOutline(mat, c, h,
					((oRgb >> 16) & 0xFF) / 255f, ((oRgb >> 8) & 0xFF) / 255f, (oRgb & 0xFF) / 255f);
		}
	}

	/** Draws the 12 edges of the unit cube as thin bars of half-thickness {@code h}. */
	private static void drawOutline(Matrix4f mat, VertexConsumer c, float h, float r, float g, float b) {
		for (int yi = 0; yi <= 1; yi++) {
			for (int zi = 0; zi <= 1; zi++) {
				drawBox(mat, c, -h, yi - h, zi - h, 1 + h, yi + h, zi + h, r, g, b, 1f);
			}
		}
		for (int xi = 0; xi <= 1; xi++) {
			for (int zi = 0; zi <= 1; zi++) {
				drawBox(mat, c, xi - h, -h, zi - h, xi + h, 1 + h, zi + h, r, g, b, 1f);
			}
		}
		for (int xi = 0; xi <= 1; xi++) {
			for (int yi = 0; yi <= 1; yi++) {
				drawBox(mat, c, xi - h, yi - h, -h, xi + h, yi + h, 1 + h, r, g, b, 1f);
			}
		}
	}

	/** Draws an axis-aligned box (6 quads) from (x0,y0,z0) to (x1,y1,z1). */
	private static void drawBox(Matrix4f mat, VertexConsumer c,
			float x0, float y0, float z0, float x1, float y1, float z1,
			float r, float g, float b, float a) {
		// Down (y0)
		c.addVertex(mat, x0, y0, z0).setColor(r, g, b, a);
		c.addVertex(mat, x1, y0, z0).setColor(r, g, b, a);
		c.addVertex(mat, x1, y0, z1).setColor(r, g, b, a);
		c.addVertex(mat, x0, y0, z1).setColor(r, g, b, a);
		// Up (y1)
		c.addVertex(mat, x0, y1, z0).setColor(r, g, b, a);
		c.addVertex(mat, x0, y1, z1).setColor(r, g, b, a);
		c.addVertex(mat, x1, y1, z1).setColor(r, g, b, a);
		c.addVertex(mat, x1, y1, z0).setColor(r, g, b, a);
		// North (z0)
		c.addVertex(mat, x0, y0, z0).setColor(r, g, b, a);
		c.addVertex(mat, x0, y1, z0).setColor(r, g, b, a);
		c.addVertex(mat, x1, y1, z0).setColor(r, g, b, a);
		c.addVertex(mat, x1, y0, z0).setColor(r, g, b, a);
		// South (z1)
		c.addVertex(mat, x0, y0, z1).setColor(r, g, b, a);
		c.addVertex(mat, x1, y0, z1).setColor(r, g, b, a);
		c.addVertex(mat, x1, y1, z1).setColor(r, g, b, a);
		c.addVertex(mat, x0, y1, z1).setColor(r, g, b, a);
		// West (x0)
		c.addVertex(mat, x0, y0, z0).setColor(r, g, b, a);
		c.addVertex(mat, x0, y0, z1).setColor(r, g, b, a);
		c.addVertex(mat, x0, y1, z1).setColor(r, g, b, a);
		c.addVertex(mat, x0, y1, z0).setColor(r, g, b, a);
		// East (x1)
		c.addVertex(mat, x1, y0, z0).setColor(r, g, b, a);
		c.addVertex(mat, x1, y1, z0).setColor(r, g, b, a);
		c.addVertex(mat, x1, y1, z1).setColor(r, g, b, a);
		c.addVertex(mat, x1, y0, z1).setColor(r, g, b, a);
	}

	/** The blink range of a held blink trinket (main or off hand), or 0 if none is held. */
	private static int heldBlinkRange(LocalPlayer player) {
		int r = rangeFromStack(player.getMainHandItem());
		return r > 0 ? r : rangeFromStack(player.getOffhandItem());
	}

	private static int rangeFromStack(ItemStack stack) {
		if (stack == null || stack.isEmpty()) {
			return 0;
		}
		CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
		if (customData == null) {
			return 0;
		}
		CompoundTag bukkit = customData.copyTag().getCompound("PublicBukkitValues").orElse(null);
		if (bukkit == null) {
			return 0;
		}
		return bukkit.getIntOr(RANGE_KEY, 0);
	}

	/**
	 * Raycasts along the player's look direction up to {@code range} blocks. Returns the endpoint block
	 * on a miss, else the open block just before the wall that was hit.
	 */
	private static BlockPos computeDestination(LocalPlayer player, Level level, int range) {
		Vec3 eye = player.getEyePosition(1.0f);
		Vec3 look = player.getViewVector(1.0f);
		Vec3 end = eye.add(look.scale(range));

		BlockHitResult hit = level.clip(new ClipContext(
				eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));

		if (hit.getType() == HitResult.Type.MISS) {
			return BlockPos.containing(end);
		}
		return hit.getBlockPos().relative(hit.getDirection());
	}
}
