package dev.nishu.bettercosmic.shared.render;

import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Renders translucent vertical "beacon" beams in 3D world space at registered positions, visible
 * through walls (via a no-depth pipeline) or occluded by terrain (a depth-tested pipeline), per beam.
 *
 * <p><b>Mechanism, not content.</b> What to draw comes from registered {@link BeamSource}s; the mod
 * feeds meteor / merchant / waypoint / gang-ping beams. Ported from BetterPrisons'
 * {@code BeaconBeamRenderer}, which looped concrete feature managers inline — de-hardcoded here to a
 * supplier registry so any Cosmic mod can add beams.
 *
 * <p><b>1.21.11 render port.</b> BetterPrisons submitted geometry through the old
 * {@code OrderedRenderCommandQueue}, which no longer exists. This uses the new
 * {@link SubmitNodeCollector#submitCustomGeometry} path. The two pipelines are built from
 * {@code DEBUG_FILLED_SNIPPET} (the position-color / translucent / quads snippet — the successor to
 * BetterPrisons' Yarn {@code POSITION_COLOR_SNIPPET}) and registered exactly the way vanilla builds
 * {@code DEBUG_QUADS}; each is wrapped in a submittable {@link RenderType}. These names are reached via
 * the shared access-widener. Like all world-space render code in this port, it needs an in-game pass.
 */
public final class BeaconBeamRenderer {

	/** A single beam: a vertical column rising {@code height} blocks from ({@code x},{@code y},{@code z}). */
	public record Beam(double x, double y, double z, float height, int rgb, int opacity, boolean throughWalls) {}

	/** Supplies the beams to draw this frame (already gated on the mod's own config). */
	@FunctionalInterface
	public interface BeamSource {
		List<Beam> beams();
	}

	private static final float BEAM_HALF_WIDTH = 0.15f;
	// Angular half-width scale: keeps the beam ~5 screen px wide at typical FOV across all distances.
	private static final float BEAM_DIST_SCALE = 0.005f;

	private static final List<BeamSource> SOURCES = new ArrayList<>();

	private BeaconBeamRenderer() {}

	/** Registers a beam source. Each frame all sources are drawn. */
	public static void addSource(BeamSource source) {
		SOURCES.add(source);
	}

	/** Hooks the world-render pass. Call once at client init. */
	public static void init() {
		WorldRenderEvents.END_MAIN.register(BeaconBeamRenderer::render);
	}

	private static void render(WorldRenderContext ctx) {
		if (SOURCES.isEmpty()) {
			return;
		}
		Minecraft client = Minecraft.getInstance();
		if (client.player == null || ctx.matrices() == null) {
			return;
		}
		SubmitNodeCollector queue = ctx.commandQueue();
		if (queue == null) {
			return;
		}
		Vec3 camera = client.gameRenderer.getMainCamera().position();

		for (BeamSource source : SOURCES) {
			List<Beam> beams;
			try {
				beams = source.beams();
			} catch (Exception e) {
				continue; // a misbehaving source must never break world rendering
			}
			if (beams == null) {
				continue;
			}
			for (Beam beam : beams) {
				submitBeam(ctx, queue, client, camera, beam);
			}
		}
	}

	private static void submitBeam(WorldRenderContext ctx, SubmitNodeCollector queue, Minecraft client,
			Vec3 camera, Beam beam) {
		float r = ((beam.rgb() >> 16) & 0xFF) / 255f;
		float g = ((beam.rgb() >> 8) & 0xFF) / 255f;
		float b = (beam.rgb() & 0xFF) / 255f;
		float a = Math.max(0, Math.min(255, beam.opacity())) / 255f;

		double dx = beam.x() - camera.x;
		double dy = beam.y() - camera.y;
		double dz = beam.z() - camera.z;

		double horizDist = Math.sqrt(dx * dx + dz * dz);
		double effectiveHorizDist = horizDist;

		if (beam.throughWalls()) {
			// Cap the beam into the fog-free zone so a through-walls beam is always fully visible.
			// Fog starts near (renderDistance-2)*16 blocks.
			int renderChunks = client.options.getEffectiveRenderDistance();
			double maxHorizDist = Math.max(16.0, (renderChunks - 4) * 16.0);
			if (horizDist > maxHorizDist) {
				double scale = maxHorizDist / horizDist;
				dx *= scale;
				dz *= scale;
				effectiveHorizDist = maxHorizDist;
			}
		}
		// When NOT through walls, do not cap: beyond render distance there is no terrain to occlude the
		// beam, so the depth test passes naturally; capping would push it into loaded terrain.

		float halfWidth = Math.max(BEAM_HALF_WIDTH, (float) effectiveHorizDist * BEAM_DIST_SCALE);

		var pose = ctx.matrices();
		pose.pushPose();
		pose.translate(dx, dy, dz);

		RenderType layer = beam.throughWalls() ? PositionColorLayers.NO_DEPTH : PositionColorLayers.DEPTH;
		float h = beam.height();
		queue.submitCustomGeometry(pose, layer,
				(entry, consumer) -> drawBeam(consumer, entry.pose(), r, g, b, a, halfWidth, h));

		pose.popPose();
	}

	private static void drawBeam(com.mojang.blaze3d.vertex.VertexConsumer consumer, org.joml.Matrix4f mat,
			float r, float g, float b, float a, float w, float h) {
		// North face
		consumer.addVertex(mat, w, 0, -w).setColor(r, g, b, a);
		consumer.addVertex(mat, w, h, -w).setColor(r, g, b, a);
		consumer.addVertex(mat, -w, h, -w).setColor(r, g, b, a);
		consumer.addVertex(mat, -w, 0, -w).setColor(r, g, b, a);
		// South face
		consumer.addVertex(mat, -w, 0, w).setColor(r, g, b, a);
		consumer.addVertex(mat, -w, h, w).setColor(r, g, b, a);
		consumer.addVertex(mat, w, h, w).setColor(r, g, b, a);
		consumer.addVertex(mat, w, 0, w).setColor(r, g, b, a);
		// West face
		consumer.addVertex(mat, -w, 0, -w).setColor(r, g, b, a);
		consumer.addVertex(mat, -w, h, -w).setColor(r, g, b, a);
		consumer.addVertex(mat, -w, h, w).setColor(r, g, b, a);
		consumer.addVertex(mat, -w, 0, w).setColor(r, g, b, a);
		// East face
		consumer.addVertex(mat, w, 0, w).setColor(r, g, b, a);
		consumer.addVertex(mat, w, h, w).setColor(r, g, b, a);
		consumer.addVertex(mat, w, h, -w).setColor(r, g, b, a);
		consumer.addVertex(mat, w, 0, -w).setColor(r, g, b, a);
	}
}
