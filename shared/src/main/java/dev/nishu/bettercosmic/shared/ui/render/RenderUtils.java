package dev.nishu.bettercosmic.shared.ui.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * Thin drawing helpers for the config UI, built on Mojang-mapped {@link GuiGraphics}.
 *
 * <p>Ported from BetterPrisons' {@code RenderUtils}, with two modernizations noted in the plan:
 * scissor clipping delegates to {@link GuiGraphics#enableScissor}/{@link GuiGraphics#disableScissor}
 * (which already handle the framebuffer/scale-factor math and maintain an intersecting stack), so
 * the old manual {@code GL11.glScissor} bookkeeping is gone; and text goes through {@code drawString}
 * with the shared MC {@link Font}.
 *
 * <p>All coordinates are GUI-space; all colors are packed {@code 0xAARRGGBB}. Fills and text respect
 * the current {@code graphics.pose()} transform.
 */
public final class RenderUtils {

	private RenderUtils() {}

	/** The shared Minecraft font. */
	public static Font font() {
		return Minecraft.getInstance().font;
	}

	/** Filled rectangle at ({@code x},{@code y}) of size {@code w}×{@code h}. */
	public static void rect(GuiGraphics g, int x, int y, int w, int h, int color) {
		g.fill(x, y, x + w, y + h, color);
	}

	/** 1px hairline border just inside the given bounds. */
	public static void outline(GuiGraphics g, int x, int y, int w, int h, int color) {
		outline(g, x, y, w, h, color, 1);
	}

	/** Border of the given thickness, drawn inside the bounds. */
	public static void outline(GuiGraphics g, int x, int y, int w, int h, int color, int t) {
		g.fill(x, y, x + w, y + t, color);                 // top
		g.fill(x, y + h - t, x + w, y + h, color);         // bottom
		g.fill(x, y + t, x + t, y + h - t, color);         // left
		g.fill(x + w - t, y + t, x + w, y + h - t, color); // right
	}

	/** Filled rectangle with a 1px border, in one call. */
	public static void panel(GuiGraphics g, int x, int y, int w, int h, int fill, int border) {
		rect(g, x, y, w, h, fill);
		outline(g, x, y, w, h, border);
	}

	/** Horizontal hairline of the given length. */
	public static void hLine(GuiGraphics g, int x, int y, int w, int color) {
		g.fill(x, y, x + w, y + 1, color);
	}

	/** Vertical hairline of the given length. */
	public static void vLine(GuiGraphics g, int x, int y, int h, int color) {
		g.fill(x, y, x + 1, y + h, color);
	}

	/** Left-aligned text with drop shadow. */
	public static void text(GuiGraphics g, String s, int x, int y, int color) {
		g.drawString(font(), s, x, y, color, true);
	}

	/** Left-aligned {@link Component} with drop shadow. */
	public static void text(GuiGraphics g, Component s, int x, int y, int color) {
		g.drawString(font(), s, x, y, color, true);
	}

	/** Text whose right edge sits at {@code rightX}. */
	public static void textRight(GuiGraphics g, String s, int rightX, int y, int color) {
		g.drawString(font(), s, rightX - font().width(s), y, color, true);
	}

	/** Text horizontally centered on {@code centerX}. */
	public static void textCentered(GuiGraphics g, String s, int centerX, int y, int color) {
		g.drawString(font(), s, centerX - font().width(s) / 2, y, color, true);
	}

	/** Width of {@code s} in the shared font. */
	public static int textWidth(String s) {
		return font().width(s);
	}

	/** Line height of the shared font (≈9px). */
	public static int lineHeight() {
		return font().lineHeight;
	}

	/** Filled triangle pointing left (apex at the left edge of the box). */
	public static void triLeft(GuiGraphics g, int x, int y, int w, int h, int color) {
		int cy = h / 2;
		for (int i = 0; i < h; i++) {
			int lead = (int) ((float) Math.abs(i - cy) / cy * w);
			g.fill(x + lead, y + i, x + w, y + i + 1, color);
		}
	}

	/** Filled triangle pointing right (apex at the right edge of the box). */
	public static void triRight(GuiGraphics g, int x, int y, int w, int h, int color) {
		int cy = h / 2;
		for (int i = 0; i < h; i++) {
			int end = (int) (x + w - ((float) Math.abs(i - cy) / cy) * w);
			g.fill(x, y + i, end, y + i + 1, color);
		}
	}

	/** Filled triangle pointing down (apex at the bottom-center of the box). */
	public static void triDown(GuiGraphics g, int x, int y, int w, int h, int color) {
		for (int i = 0; i < h; i++) {
			int inset = (int) ((float) i / h * (w / 2f));
			g.fill(x + inset, y + i, x + w - inset, y + i + 1, color);
		}
	}

	/**
	 * Clips subsequent drawing to the given rectangle until {@link #popScissor} is called. Nested
	 * calls intersect (handled by {@code GuiGraphics}). Coordinates are GUI-space.
	 */
	public static void pushScissor(GuiGraphics g, int x, int y, int w, int h) {
		g.enableScissor(x, y, x + w, y + h);
	}

	/** Ends the innermost {@link #pushScissor} region. */
	public static void popScissor(GuiGraphics g) {
		g.disableScissor();
	}
}
