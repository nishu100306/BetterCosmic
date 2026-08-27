package dev.nishu.bettercosmic.shared.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;

/**
 * Shared translucent POSITION_COLOR world-space render layers, submittable through
 * {@code SubmitNodeCollector.submitCustomGeometry}. Two variants: {@link #NO_DEPTH} renders through
 * all blocks (no depth test), {@link #DEPTH} is occluded by nearer terrain (LEQUAL). Both are built
 * from {@code DEBUG_FILLED_SNIPPET} (the 1.21.11 position-color / translucent / quads snippet — the
 * successor to BetterPrisons' Yarn {@code POSITION_COLOR_SNIPPET}) and registered exactly the way
 * vanilla builds {@code DEBUG_QUADS}; the snippet, {@code RenderPipelines.register}, and
 * {@code RenderType.create} are reached via the shared access-widener.
 *
 * <p>Content-agnostic infrastructure used by {@link BeaconBeamRenderer} and by prison renderers (e.g.
 * the Blink-trinket destination highlight). Like all world-space render code in this port it needs an
 * in-game pass.
 */
public final class PositionColorLayers {

	private PositionColorLayers() {}

	private static RenderPipeline pipeline(String path, DepthTestFunction depth) {
		return RenderPipelines.register(
				RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
						.withLocation(Identifier.fromNamespaceAndPath("bettercosmicshared", "pipeline/" + path))
						.withDepthTestFunction(depth)
						.withCull(false)
						.build());
	}

	/** POSITION_COLOR, translucent, no depth test, no cull — visible through all blocks. */
	public static final RenderType NO_DEPTH = RenderType.create(
			"bettercosmicshared:position_color_no_depth",
			RenderSetup.builder(pipeline("position_color_no_depth", DepthTestFunction.NO_DEPTH_TEST)).createRenderSetup());

	/** POSITION_COLOR, translucent, standard depth test (occluded by nearer terrain), no cull. */
	public static final RenderType DEPTH = RenderType.create(
			"bettercosmicshared:position_color_depth",
			RenderSetup.builder(pipeline("position_color_depth", DepthTestFunction.LEQUAL_DEPTH_TEST)).createRenderSetup());
}
