package dev.nishu.bettercosmic.shared.render;

import com.mojang.blaze3d.vertex.VertexConsumer;

/**
 * A {@link VertexConsumer} that discards every call. Used to suppress specific render layers (e.g.
 * armor / capes) when drawing an entity as a translucent ghost. Ported from BetterPrisons
 * (Yarn → Mojang).
 */
public final class NoOpVertexConsumer implements VertexConsumer {

	private NoOpVertexConsumer() {}

	public static NoOpVertexConsumer create() {
		return new NoOpVertexConsumer();
	}

	@Override
	public VertexConsumer addVertex(float x, float y, float z) {
		return this;
	}

	@Override
	public VertexConsumer setColor(int red, int green, int blue, int alpha) {
		return this;
	}

	@Override
	public VertexConsumer setColor(int argb) {
		return this;
	}

	@Override
	public VertexConsumer setUv(float u, float v) {
		return this;
	}

	@Override
	public VertexConsumer setUv1(int u, int v) {
		return this;
	}

	@Override
	public VertexConsumer setUv2(int u, int v) {
		return this;
	}

	@Override
	public VertexConsumer setNormal(float x, float y, float z) {
		return this;
	}

	@Override
	public VertexConsumer setLineWidth(float width) {
		return this;
	}
}
