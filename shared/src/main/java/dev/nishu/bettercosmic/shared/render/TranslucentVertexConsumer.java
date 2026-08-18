package dev.nishu.bettercosmic.shared.render;

import com.mojang.blaze3d.vertex.VertexConsumer;

/**
 * Wraps a {@link VertexConsumer} and forces a fixed alpha on every color, making whatever is drawn
 * through it translucent. Ported from BetterPrisons (Yarn → Mojang); content-agnostic (the alpha is
 * supplied by the caller).
 */
public final class TranslucentVertexConsumer implements VertexConsumer {

	private final VertexConsumer delegate;
	private final float alpha;

	/** @param alpha target alpha in {@code [0,1]} applied to every vertex color. */
	public TranslucentVertexConsumer(VertexConsumer delegate, float alpha) {
		this.delegate = delegate;
		this.alpha = alpha;
	}

	@Override
	public VertexConsumer addVertex(float x, float y, float z) {
		return delegate.addVertex(x, y, z);
	}

	@Override
	public VertexConsumer setColor(int red, int green, int blue, int a) {
		return delegate.setColor(red, green, blue, (int) (this.alpha * 255));
	}

	@Override
	public VertexConsumer setColor(int argb) {
		int transparentArgb = (argb & 0x00FFFFFF) | ((int) (this.alpha * 255) << 24);
		return delegate.setColor(transparentArgb);
	}

	@Override
	public VertexConsumer setUv(float u, float v) {
		return delegate.setUv(u, v);
	}

	@Override
	public VertexConsumer setUv1(int u, int v) {
		return delegate.setUv1(u, v);
	}

	@Override
	public VertexConsumer setUv2(int u, int v) {
		return delegate.setUv2(u, v);
	}

	@Override
	public VertexConsumer setNormal(float x, float y, float z) {
		return delegate.setNormal(x, y, z);
	}

	@Override
	public VertexConsumer setLineWidth(float width) {
		return delegate.setLineWidth(width);
	}
}
