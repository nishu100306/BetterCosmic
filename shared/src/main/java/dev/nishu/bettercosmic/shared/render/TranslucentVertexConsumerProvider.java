package dev.nishu.bettercosmic.shared.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;

/**
 * Wraps a {@link MultiBufferSource} so entities drawn through it become translucent, with armor,
 * capes and elytra suppressed entirely — producing a clean ghost silhouette. Ported from
 * BetterPrisons (Yarn → Mojang) and made content-agnostic: the alpha is a constructor argument
 * rather than a config read, so the shared library carries no feature state.
 */
public final class TranslucentVertexConsumerProvider implements MultiBufferSource {

	private final MultiBufferSource delegate;
	private final float alpha;

	/** @param alpha target alpha in {@code [0,1]} for the ghost body. */
	public TranslucentVertexConsumerProvider(MultiBufferSource delegate, float alpha) {
		this.delegate = delegate;
		this.alpha = alpha;
	}

	@Override
	public VertexConsumer getBuffer(RenderType layer) {
		String layerName = layer.toString().toLowerCase();
		if (layerName.contains("armor") || layerName.contains("cape") || layerName.contains("elytra")) {
			return NoOpVertexConsumer.create();
		}
		return new TranslucentVertexConsumer(delegate.getBuffer(layer), alpha);
	}
}
