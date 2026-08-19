package dev.nishu.bettercosmic.shared.render;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3x2fStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Draws 2D screen-edge waypoint indicators for world positions: an item icon (or colored square) with
 * a distance label when on-screen, clamped to the screen edge with a direction arrow when off-screen.
 * All markers are collected, sorted nearest-first, and nudged apart so labels don't overlap.
 *
 * <p><b>Mechanism, not content.</b> Positions come from registered {@link Source}s; the mod supplies
 * meteor / merchant / custom-waypoint targets. Ported from BetterPrisons' {@code WaypointRenderer},
 * de-hardcoded from its inline feature loops to a supplier registry (the gang-ping player-head variant
 * stays with that feature). Projection uses {@link WorldSpaceTransform}, so the shared FOV mixin must
 * be active for the coordinates to be correct.
 */
public final class WaypointRenderer {

	/** One screen-edge target. {@code x/y/z} are the exact world point to project (mod adds any +0.5). */
	public static final class EdgeTarget {
		public final double x, y, z;
		public final int rgb;
		/** Item sprite to draw; {@code null} → a colored square. */
		public final ItemStack icon;
		/** Label under the marker; {@code null} → the distance, e.g. {@code "128m"}. */
		public String label;
		public float onScreenScale = 1.0f;
		public float offScreenScale = 1.0f;
		/** Whether to show this target as an edge indicator when it is off-screen. */
		public boolean edgeEnabled = true;

		public EdgeTarget(double x, double y, double z, int rgb, ItemStack icon) {
			this.x = x;
			this.y = y;
			this.z = z;
			this.rgb = rgb;
			this.icon = icon;
		}
	}

	/** Supplies the targets to draw this frame (already gated on the mod's own config). */
	@FunctionalInterface
	public interface Source {
		List<EdgeTarget> targets();
	}

	private static final int ICON_HALF = 8;
	private static final int EDGE_MARGIN = 20;
	private static final int ARROW_OFFSET = 11;
	private static final int ARROW_RADIUS = 5;
	private static final int LABEL_H = 7;
	private static final int NUDGE_STEP = 26;
	private static final int MAX_NUDGES = 8;

	private static final List<Source> SOURCES = new ArrayList<>();

	private WaypointRenderer() {}

	/** Registers a target source. */
	public static void addSource(Source source) {
		SOURCES.add(source);
	}

	/** Hooks the HUD render pass. Call once at client init. */
	public static void init() {
		HudRenderCallback.EVENT.register((ctx, tickCounter) -> render(ctx));
	}

	// --- One resolved, placed marker ---
	private static final class Entry {
		int rgb;
		ItemStack icon;
		float projX, projY;
		boolean onScreen;
		double dist;
		String label;
		int labelWidth;
		int ix, iy;
		float onScreenScale = 1.0f;
		float offScreenScale = 1.0f;

		float scale() {
			return onScreen ? onScreenScale : offScreenScale;
		}
	}

	private static void render(GuiGraphics ctx) {
		if (SOURCES.isEmpty()) {
			return;
		}
		Minecraft client = Minecraft.getInstance();
		if (client.player == null || client.screen != null || client.options.hideGui) {
			return;
		}

		int screenW = ctx.guiWidth();
		int screenH = ctx.guiHeight();
		Font font = client.font;

		List<Entry> entries = new ArrayList<>();
		for (Source source : SOURCES) {
			List<EdgeTarget> targets;
			try {
				targets = source.targets();
			} catch (Exception e) {
				continue;
			}
			if (targets == null) {
				continue;
			}
			for (EdgeTarget t : targets) {
				Entry e = buildEntry(font, t, screenW, screenH);
				if (e != null && (e.onScreen || t.edgeEnabled)) {
					entries.add(e);
				}
			}
		}

		// Nearest-first so closer markers get priority placement.
		entries.sort((a, b) -> Double.compare(a.dist, b.dist));

		List<int[]> placed = new ArrayList<>();
		for (Entry e : entries) {
			resolveCollision(e, placed, screenW, screenH);
			placed.add(boundingRect(e.ix, e.iy, e.labelWidth, e.scale()));
			drawEntry(ctx, font, e);
		}
	}

	private static Entry buildEntry(Font font, EdgeTarget t, int screenW, int screenH) {
		float[] pos = WorldSpaceTransform.worldToScreen(t.x, t.y, t.z, screenW, screenH);
		if (pos == null) {
			return null;
		}
		double dx = t.x - WorldSpaceTransform.getCamX();
		double dy = t.y - WorldSpaceTransform.getCamY();
		double dz = t.z - WorldSpaceTransform.getCamZ();

		Entry e = new Entry();
		e.rgb = t.rgb;
		e.icon = t.icon;
		e.projX = pos[0];
		e.projY = pos[1];
		e.dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
		e.onScreenScale = Math.max(0.1f, t.onScreenScale);
		e.offScreenScale = Math.max(0.1f, t.offScreenScale);
		e.label = t.label != null ? t.label : (int) e.dist + "m";
		e.labelWidth = font.width(e.label);

		boolean offLeft = e.projX < EDGE_MARGIN;
		boolean offRight = e.projX > screenW - EDGE_MARGIN;
		boolean offTop = e.projY < EDGE_MARGIN;
		boolean offBottom = e.projY > screenH - EDGE_MARGIN;
		e.onScreen = !offLeft && !offRight && !offTop && !offBottom;

		float clampedX;
		if (!e.onScreen && !offLeft && !offRight) {
			clampedX = e.projX < screenW / 2f ? EDGE_MARGIN : screenW - EDGE_MARGIN;
		} else {
			clampedX = Math.max(EDGE_MARGIN, Math.min(screenW - EDGE_MARGIN, e.projX));
		}
		float clampedY = Math.max(EDGE_MARGIN, Math.min(screenH - EDGE_MARGIN, e.projY));
		e.ix = (int) clampedX;
		e.iy = (int) clampedY;
		return e;
	}

	private static void resolveCollision(Entry e, List<int[]> placed, int screenW, int screenH) {
		if (placed.isEmpty()) {
			return;
		}
		int baseX = e.ix;
		int baseY = e.iy;
		// Edge markers only nudge in Y (stay on the edge); on-screen markers try Y then Y+X.
		for (int xi = 0; xi <= (e.onScreen ? MAX_NUDGES : 0); xi++) {
			int xOff = nudgeOffset(xi) * NUDGE_STEP;
			for (int yi = 0; yi < MAX_NUDGES * 2; yi++) {
				int yOff = nudgeOffset(yi) * NUDGE_STEP;
				int tryX = clamp(baseX + xOff, screenW);
				int tryY = clamp(baseY + yOff, screenH);
				if (!overlapsAny(boundingRect(tryX, tryY, e.labelWidth, e.scale()), placed)) {
					e.ix = tryX;
					e.iy = tryY;
					return;
				}
			}
		}
	}

	/** 0→0, 1→+1, 2→−1, 3→+2, 4→−2, … */
	private static int nudgeOffset(int i) {
		if (i == 0) {
			return 0;
		}
		return (i % 2 == 1) ? (i + 1) / 2 : -(i / 2);
	}

	private static int clamp(int v, int screen) {
		return Math.max(EDGE_MARGIN, Math.min(screen - EDGE_MARGIN, v));
	}

	private static int[] boundingRect(int cx, int cy, int labelWidth, float scale) {
		int scaledHalf = Math.max(1, (int) (ICON_HALF * scale));
		int halfW = Math.max(scaledHalf, labelWidth / 2) + 2;
		return new int[]{cx - halfW, cy - scaledHalf, cx + halfW, cy + scaledHalf + 2 + LABEL_H};
	}

	private static boolean overlapsAny(int[] r, List<int[]> placed) {
		for (int[] p : placed) {
			if (r[0] < p[2] && r[2] > p[0] && r[1] < p[3] && r[3] > p[1]) {
				return true;
			}
		}
		return false;
	}

	private static void drawEntry(GuiGraphics ctx, Font font, Entry e) {
		int ix = e.ix;
		int iy = e.iy;
		float scale = e.scale();
		int scaledHalf = Math.max(1, (int) (ICON_HALF * scale));

		Matrix3x2fStack ms = ctx.pose();
		ms.pushMatrix();
		ms.translate(ix, iy);
		ms.scale(scale, scale);

		if (e.icon != null && !e.icon.isEmpty()) {
			ctx.renderItem(e.icon, -ICON_HALF, -ICON_HALF);
		} else {
			int fill = 0xFF000000 | (e.rgb & 0xFFFFFF);
			ctx.fill(-5, -5, 5, 5, fill);
			ctx.fill(-6, -6, 6, -5, 0x80000000);
			ctx.fill(-6, 5, 6, 6, 0x80000000);
			ctx.fill(-6, -5, -5, 5, 0x80000000);
			ctx.fill(5, -5, 6, 5, 0x80000000);
		}
		ms.popMatrix();

		// Off-screen: an arrow badge pointing toward the (off-screen) projected position.
		if (!e.onScreen) {
			double arrowAngle = Math.atan2(e.projY - iy, e.projX - ix);
			int arrowOffset = Math.max(ARROW_OFFSET, scaledHalf + 3);
			int argb = 0xFF000000 | (e.rgb & 0xFFFFFF);
			int arrowCX = ix + (int) (arrowOffset * Math.cos(arrowAngle));
			int arrowCY = iy + (int) (arrowOffset * Math.sin(arrowAngle));
			drawArrow(ctx, arrowCX, arrowCY, arrowAngle, ARROW_RADIUS, argb);
		}

		int textArgb = 0xFF000000 | (e.rgb & 0xFFFFFF);
		ctx.drawString(font, Component.literal(e.label), ix - e.labelWidth / 2, iy + scaledHalf + 2, textArgb, true);
	}

	private static void drawArrow(GuiGraphics ctx, int cx, int cy, double angle, int r, int color) {
		int tipX = cx + (int) (r * Math.cos(angle));
		int tipY = cy + (int) (r * Math.sin(angle));
		double perpAngle = angle + Math.PI / 2;
		int baseHalfWidth = r - 1;
		int baseX = cx - (int) ((r / 2.0) * Math.cos(angle));
		int baseY = cy - (int) ((r / 2.0) * Math.sin(angle));
		int b1x = baseX + (int) (baseHalfWidth * Math.cos(perpAngle));
		int b1y = baseY + (int) (baseHalfWidth * Math.sin(perpAngle));
		int b2x = baseX - (int) (baseHalfWidth * Math.cos(perpAngle));
		int b2y = baseY - (int) (baseHalfWidth * Math.sin(perpAngle));
		fillTriangle(ctx, tipX, tipY, b1x, b1y, b2x, b2y, color);
	}

	private static void fillTriangle(GuiGraphics ctx, int x0, int y0, int x1, int y1, int x2, int y2, int color) {
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
