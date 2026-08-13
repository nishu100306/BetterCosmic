package dev.nishu.bettercosmic.shared.ui.model;

import net.minecraft.client.gui.GuiGraphics;

/**
 * The small monochrome glyph shown on a {@link ConfigPanel} card. Drawn with primitive fills so the
 * framework carries no texture assets yet; Phase 7 may swap these for 16×16 PNG sprites (see the
 * icons subplan in {@code CONFIG_UI_PLAN.md} §3.2). Each glyph is drawn tinted to a single color
 * inside a {@code size}×{@code size} box at ({@code x},{@code y}).
 */
public enum PanelIcon {
	/** Potion flask — trinkets. */
	FLASK,
	/** Gear — general/settings. */
	GEAR,
	/** Padlock — locked "coming soon" placeholders. */
	LOCK;

	public void draw(GuiGraphics g, int x, int y, int size, int color) {
		switch (this) {
			case FLASK -> flask(g, x, y, size, color);
			case GEAR -> gear(g, x, y, size, color);
			case LOCK -> lock(g, x, y, size, color);
		}
	}

	private static void flask(GuiGraphics g, int x, int y, int s, int color) {
		int cx = x + s / 2;
		int neckBottom = y + Math.round(s * 0.38f);
		g.fill(cx - 3, y, cx + 3, y + 1, color);              // lip
		g.fill(cx - 2, y, cx - 1, neckBottom, color);          // neck left
		g.fill(cx + 1, y, cx + 2, neckBottom, color);          // neck right
		int bottom = y + s - 1;
		int span = bottom - neckBottom;
		for (int i = 0; i < span; i++) {                       // widening body
			float f = span == 0 ? 0 : i / (float) span;
			int half = Math.round(1 + f * (s * 0.42f));
			int yy = neckBottom + i;
			g.fill(cx - half, yy, cx + half, yy + 1, color);
		}
	}

	private static void gear(GuiGraphics g, int x, int y, int s, int color) {
		int inset = 2;
		// ring
		g.fill(x + inset, y + inset, x + s - inset, y + inset + 2, color);         // top
		g.fill(x + inset, y + s - inset - 2, x + s - inset, y + s - inset, color); // bottom
		g.fill(x + inset, y + inset, x + inset + 2, y + s - inset, color);         // left
		g.fill(x + s - inset - 2, y + inset, x + s - inset, y + s - inset, color); // right
		int cx = x + s / 2, cy = y + s / 2;
		g.fill(cx - 1, y, cx + 1, y + 2, color);           // nubs
		g.fill(cx - 1, y + s - 2, cx + 1, y + s, color);
		g.fill(x, cy - 1, x + 2, cy + 1, color);
		g.fill(x + s - 2, cy - 1, x + s, cy + 1, color);
	}

	private static void lock(GuiGraphics g, int x, int y, int s, int color) {
		int shW = Math.round(s * 0.5f);
		int shX = x + (s - shW) / 2;
		int shTop = y + 1;
		int shBottom = y + Math.round(s * 0.5f);
		g.fill(shX, shTop, shX + shW, shTop + 1, color);           // shackle top
		g.fill(shX, shTop, shX + 1, shBottom, color);              // shackle left
		g.fill(shX + shW - 1, shTop, shX + shW, shBottom, color);  // shackle right
		int bodyTop = y + Math.round(s * 0.42f);
		g.fill(x + 1, bodyTop, x + s - 1, y + s - 1, color);       // body
	}
}
