package dev.nishu.bettercosmic.shared.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.state.LevelRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Renders short-lived floating text as true 3D world-space labels (like SkyBlock proc indicators).
 * On spawn the anchor is chosen from what the player is looking at: the raytraced entity, else the
 * raytraced block, else the entity closest in angle to the look direction. An entity anchor is tracked
 * live (the label follows a moving mob); a block/point anchor is fixed. The label rises and fades over
 * its lifetime.
 *
 * <p><b>Mechanism, not content.</b> {@link #spawn(Component, long)} takes any text and TTL — what the
 * text says (an enchant proc, a pickup, ...) is up to the caller. Ported from BetterPrisons'
 * {@code FloatingTextRenderer}.
 *
 * <p><b>1.21.11 render.</b> The label is submitted as raw billboarded text via
 * {@link SubmitNodeCollector#submitText} on the Fabric world-render pass — the same submit collector the
 * vanilla name-tag path uses, but without its forced background box: we pass {@code backgroundColor = 0}.
 * We reproduce vanilla's name-tag transform (camera-facing via {@code cameraRenderState.orientation},
 * then {@code scale(0.025, -0.025, 0.025)}); the {@link Component}'s own style supplies colour + bold.
 * Like all world-space render code here it needs an in-game pass; all access is on the render/client thread.
 */
public final class FloatingTextRenderer {

	private static final double RISE_BLOCKS = 0.4;    // total upward drift over its lifetime
	private static final double DRIFT_BLOCKS = 0.6;   // total sideways drift over its lifetime (± per label)
	private static final double JITTER = 0.6;         // billboard-plane spawn spread (blocks) around the mob
	private static final double TARGET_REACH = 64.0;  // how far to look for the anchor target
	private static final int MAX_ENTRIES = 32;

	private static final List<FloatingText> active = new ArrayList<>();

	private static boolean registered = false;

	private FloatingTextRenderer() {}

	private static final class FloatingText {
		final Component text;
		final long spawnTime;
		final long durationMs;
		final Entity anchorEntity; // tracked live if present
		final Vec3 anchorPos;      // fixed fallback / block-or-point anchor
		final double jitterX;      // horizontal offset in the billboard (screen-facing) plane, blocks
		final double jitterY;      // vertical offset in the billboard plane, blocks
		final double driftDir;     // per-label sideways drift direction/strength in [-1, 1]

		FloatingText(Component text, long durationMs, Entity anchorEntity, Vec3 anchorPos) {
			this.text = text;
			this.spawnTime = System.currentTimeMillis();
			this.durationMs = durationMs;
			this.anchorEntity = anchorEntity;
			this.anchorPos = anchorPos;
			this.jitterX = (Math.random() - 0.5) * 2.0 * JITTER;
			this.jitterY = (Math.random() - 0.5) * 2.0 * JITTER;
			this.driftDir = (Math.random() - 0.5) * 2.0;
		}
	}

	/** Hooks the world-render pass. Idempotent — safe to call from more than one mod's client init. */
	public static void init() {
		if (registered) {
			return;
		}
		registered = true;
		WorldRenderEvents.AFTER_ENTITIES.register(FloatingTextRenderer::renderInWorld);
	}

	/**
	 * Spawns a floating label anchored directly on {@code anchor} (tracked live as it moves), instead of
	 * on the player's look target. For callers that already know the entity the text belongs to — e.g. a
	 * damage number over the mob that was hit.
	 */
	public static void spawn(Component text, long durationMs, Entity anchor) {
		if (text == null || anchor == null) {
			return;
		}
		if (active.size() >= MAX_ENTRIES) {
			active.remove(0);
		}
		active.add(new FloatingText(text, durationMs, anchor, anchor.getBoundingBox().getCenter()));
	}

	/**
	 * Spawns a floating label at the player's current target: raytraced entity, else raytraced block,
	 * else the entity nearest the look direction (else the raycast endpoint).
	 */
	public static void spawn(Component text, long durationMs) {
		if (text == null) {
			return;
		}
		Minecraft client = Minecraft.getInstance();
		if (client.player == null || client.level == null) {
			return;
		}
		LocalPlayer player = client.player;
		Vec3 eye = player.getEyePosition();
		Vec3 look = player.getViewVector(1.0f);
		Vec3 end = eye.add(look.scale(TARGET_REACH));

		BlockHitResult blockHit = client.level.clip(new ClipContext(
				eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
		double blockDistSq = blockHit.getType() == HitResult.Type.MISS
				? TARGET_REACH * TARGET_REACH : blockHit.getLocation().distanceToSqr(eye);

		// Entity raycast, limited to the block distance so walls occlude entities.
		AABB searchBox = player.getBoundingBox().expandTowards(look.scale(TARGET_REACH)).inflate(1.0);
		EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(player, eye, end, searchBox,
				e -> e != player && e.isAlive() && !e.isSpectator(), blockDistSq);

		Entity anchorEntity = null;
		Vec3 anchorPos;
		if (entityHit != null && entityHit.getEntity() != null) {
			anchorEntity = entityHit.getEntity();
			anchorPos = anchorEntity.getBoundingBox().getCenter();
		} else if (blockHit.getType() == HitResult.Type.BLOCK) {
			anchorPos = blockHit.getLocation();
		} else {
			anchorEntity = closestEntityByAngle(client.level, player, eye, look);
			anchorPos = anchorEntity != null ? anchorEntity.getBoundingBox().getCenter() : end;
		}

		if (active.size() >= MAX_ENTRIES) {
			active.remove(0);
		}
		active.add(new FloatingText(text, durationMs, anchorEntity, anchorPos));
	}

	/** The entity whose direction from the eye is closest to the look vector (in front only). */
	private static Entity closestEntityByAngle(ClientLevel level, Entity self, Vec3 eye, Vec3 look) {
		Entity best = null;
		double bestDot = 0.0; // require in front of the camera
		for (Entity e : level.entitiesForRendering()) {
			if (e == self || e.isSpectator() || !e.isAlive()) {
				continue;
			}
			Vec3 toE = e.getBoundingBox().getCenter().subtract(eye);
			double len = toE.length();
			if (len < 0.5 || len > TARGET_REACH) {
				continue;
			}
			double dot = look.dot(toE.scale(1.0 / len));
			if (dot > bestDot) {
				bestDot = dot;
				best = e;
			}
		}
		return best;
	}

	/** Vanilla name-tag glyph scale (1 block ≈ 40 px). Y is flipped: text space grows downward. */
	private static final float TEXT_SCALE = 0.025f;
	/** Vanilla raises the tag half a block above its anchor point; match that so placement is familiar. */
	private static final double NAMETAG_Y_OFFSET = 0.5;

	/** Submits the active labels as camera-facing text (no background box) on the world-render pass. */
	private static void renderInWorld(WorldRenderContext ctx) {
		if (active.isEmpty()) {
			return;
		}
		PoseStack pose = ctx.matrices();
		SubmitNodeCollector queue = ctx.commandQueue();
		LevelRenderState worldState = ctx.worldState();
		if (pose == null || queue == null || worldState == null) {
			return;
		}
		CameraRenderState cameraState = worldState.cameraRenderState;
		if (cameraState == null) {
			return;
		}
		Vec3 camPos = cameraState.pos;
		Font font = Minecraft.getInstance().font;
		long now = System.currentTimeMillis();

		Iterator<FloatingText> it = active.iterator();
		while (it.hasNext()) {
			FloatingText ft = it.next();
			long elapsed = now - ft.spawnTime;
			if (elapsed >= ft.durationMs) {
				it.remove();
				continue;
			}
			Vec3 base = (ft.anchorEntity != null && ft.anchorEntity.isAlive() && !ft.anchorEntity.isRemoved())
					? ft.anchorEntity.getBoundingBox().getCenter() : ft.anchorPos;

			float progress = (float) elapsed / ft.durationMs;
			double rise = RISE_BLOCKS * (1.0 - (1.0 - progress) * (1.0 - progress));
			double drift = ft.driftDir * DRIFT_BLOCKS * progress; // sideways travel, linear over life

			pose.pushPose();
			// The world-render matrix puts the camera at the origin, so translate by (world - camera).
			pose.translate(base.x - camPos.x,
					base.y + rise + NAMETAG_Y_OFFSET - camPos.y,
					base.z - camPos.z);
			pose.mulPose(cameraState.orientation);          // face the camera (billboard)
			// Scatter around the mob (jitter) plus a sideways drift over its lifetime, both in the
			// screen-facing plane (blocks, before the shrink) so they read the same from any angle.
			pose.translate(ft.jitterX + drift, ft.jitterY, 0.0);
			pose.scale(TEXT_SCALE, -TEXT_SCALE, TEXT_SCALE); // to text space + shrink

			float x = -font.width(ft.text) / 2.0f;          // centre horizontally
			// submitText(pose, x, y, text, dropShadow, displayMode, light, color, backgroundColor, outline)
			// backgroundColor 0 = no box; the Component's own style supplies colour + bold.
			queue.submitText(pose, x, 0f, ft.text.getVisualOrderText(), false,
					Font.DisplayMode.NORMAL, LightTexture.FULL_BRIGHT, 0xFFFFFFFF, 0, 0);
			pose.popPose();
		}
	}
}
