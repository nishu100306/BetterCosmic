package dev.nishu.bettercosmic.shared.render;

import net.minecraft.client.gui.GuiGraphics;

import java.util.List;

/**
 * Shared 2D helpers for screen-edge marker renderers (waypoints, gang pings, ...): overlap testing +
 * nudge sequencing for collision avoidance, and a filled direction-arrow. Content-agnostic geometry;
 * factored out so each renderer doesn't carry its own copy.
 */
public final class ScreenMarkers {

	private ScreenMarkers() {}

	/** Converts a nudge index to a signed offset: 0→0, 1→+1, 2→−1, 3→+2, 4→−2, … */
	public static int nudgeOffset(int i) {
		if (i == 0) {
			return 0;
		}
		return (i % 2 == 1) ? (i + 1) / 2 : -(i / 2);
	}

	/** Whether the rect {@code [x1,y1,x2,y2]} overlaps any already-placed rect. */
	public static boolean overlapsAny(int[] r, List<int[]> placed) {
		for (int[] p : placed) {
			if (r[0] < p[2] && r[2] > p[0] && r[1] < p[3] && r[3] > p[1]) {
				return true;
			}
		}
		return false;
	}

	/** Draws a small filled triangle pointing along {@code angle}, centered near ({@code cx},{@code cy}). */
	public static void drawArrow(GuiGraphics ctx, int cx, int cy, double angle, int r, int color) {
		int tipX = cx + (int) (r * Math.cos(angle));
		int tipY = cy + (int) (r * Math.sin(angle));
		double perp = angle + Math.PI / 2;
		int baseHalf = r - 1;
		int baseX = cx - (int) ((r / 2.0) * Math.cos(angle));
		int baseY = cy - (int) ((r / 2.0) * Math.sin(angle));
		int b1x = baseX + (int) (baseHalf * Math.cos(perp));
		int b1y = baseY + (int) (baseHalf * Math.sin(perp));
		int b2x = baseX - (int) (baseHalf * Math.cos(perp));
		int b2y = baseY - (int) (baseHalf * Math.sin(perp));
		fillTriangle(ctx, tipX, tipY, b1x, b1y, b2x, b2y, color);
	}

	/** Fills the triangle (x0,y0)-(x1,y1)-(x2,y2) with a solid color (barycentric scanline). */
	public static void fillTriangle(GuiGraphics ctx, int x0, int y0, int x1, int y1, int x2, int y2, int color) {
		int minX = Math.min(x0, Math.min(x1, x2));
		int maxX = Math.max(x0, Math.max(x1, x2));
		int minY = Math.min(y0, Math.min(y1, y2));
		int maxY = Math.max(y0, Math.max(y1, y2));
		int denom = (y1 - y2) * (x0 - x2) + (x2 - x1) * (y0 - y2);
		if (denom == 0) {
			return;
		}
		for (int py = minY; py <= maxY; py++) {
			for (int px = minX; px <= maxX; px++) {
				int w0 = (y1 - y2) * (px - x2) + (x2 - x1) * (py - y2);
				int w1 = (y2 - y0) * (px - x2) + (x0 - x2) * (py - y2);
				int w2 = denom - w0 - w1;
				if (denom > 0 ? (w0 >= 0 && w1 >= 0 && w2 >= 0) : (w0 <= 0 && w1 <= 0 && w2 <= 0)) {
					ctx.fill(px, py, px + 1, py + 1, color);
				}
			}
		}
	}
}
