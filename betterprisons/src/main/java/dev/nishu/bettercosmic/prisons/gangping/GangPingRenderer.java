package dev.nishu.bettercosmic.prisons.gangping;

import dev.nishu.bettercosmic.prisons.client.BetterPrisonsClient;
import dev.nishu.bettercosmic.prisons.config.PrisonsConfig;
import dev.nishu.bettercosmic.prisons.waypoint.WaypointManager;
import dev.nishu.bettercosmic.shared.render.WorldSpaceTransform;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.PlayerSkin;
import org.joml.Matrix3x2fStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Draws gang pings as 2D screen markers: a player-head icon (distance-scaled, distance-faded)
 * with a multi-line info panel (name / timer / coords / HP / facing) when on-screen, or clamped to the
 * screen edge with a direction arrow when off-screen. Projection comes from the shared
 * {@link WorldSpaceTransform}; the player-head presentation is gang-ping-specific, so it lives here
 * rather than in the shared {@code WaypointRenderer} (per the mechanism/policy split). Ported from the
 * gang-ping branch of BetterPrisons' {@code WaypointRenderer} (Yarn → Mojang: {@code PlayerSkinDrawer}
 * → {@link PlayerFaceRenderer}, {@code getPlayerListEntry} → {@code getPlayerInfo}).
 */
public final class GangPingRenderer {

	private static final int ICON_HALF = 9;
	private static final int EDGE_MARGIN = 20;
	private static final int ARROW_OFFSET = 11;
	private static final int ARROW_RADIUS = 5;
	private static final int LABEL_H = 7;
	private static final int NUDGE_STEP = 26;
	private static final int MAX_NUDGES = 8;
	private static final int PANEL_BG = 0xC0000000;

	private GangPingRenderer() {}

	public static void init() {
		HudRenderCallback.EVENT.register((ctx, tickCounter) -> render(ctx));
	}

	/**
	 * Beacon beams for active pings in the current world — registered as a shared
	 * {@link dev.nishu.bettercosmic.shared.render.BeaconBeamRenderer} source. Anchored at world Y=0
	 * (like the event beams) so they're visible from any altitude.
	 */
	public static List<dev.nishu.bettercosmic.shared.render.BeaconBeamRenderer.Beam> beams() {
		List<dev.nishu.bettercosmic.shared.render.BeaconBeamRenderer.Beam> beams = new ArrayList<>();
		PrisonsConfig c = BetterPrisonsClient.config;
		if (c == null || !c.gangPingBeamEnabled || !c.gangPingEnabled) {
			return beams;
		}
		String currentWorld = WaypointManager.detectWorldKey();
		for (GangPingManager.GangPingInfo ping : BetterPrisonsClient.gangPingManager.getActivePings()) {
			if (!ping.world.equals(currentWorld)) {
				continue;
			}
			beams.add(new dev.nishu.bettercosmic.shared.render.BeaconBeamRenderer.Beam(
					ping.x + 0.5, 0, ping.z + 0.5, 250f, c.gangPingColor, c.gangPingBeamOpacity, c.beaconBeamThroughWalls));
		}
		return beams;
	}

	private static final class Entry {
		GangPingManager.GangPingInfo ping;
		float projX, projY;
		boolean onScreen;
		double dist;
		float scale;
		float alpha;
		int ix, iy;
	}

	private static void render(GuiGraphics ctx) {
		PrisonsConfig c = BetterPrisonsClient.config;
		if (c == null || !c.gangPingEnabled) {
			return;
		}
		Minecraft client = Minecraft.getInstance();
		if (client.player == null || client.screen != null || client.options.hideGui) {
			return;
		}

		int screenW = ctx.guiWidth();
		int screenH = ctx.guiHeight();
		Font font = client.font;
		String currentWorld = WaypointManager.detectWorldKey();
		float baseAlpha = c.gangPingBaseOpacity / 255f;

		List<Entry> entries = new ArrayList<>();
		for (GangPingManager.GangPingInfo ping : BetterPrisonsClient.gangPingManager.getActivePings()) {
			if (!ping.world.equals(currentWorld)) {
				continue;
			}
			float[] pos = WorldSpaceTransform.worldToScreen(ping.x + 0.5, ping.y, ping.z + 0.5, screenW, screenH);
			if (pos == null) {
				continue;
			}
			Entry e = new Entry();
			e.ping = ping;
			e.projX = pos[0];
			e.projY = pos[1];
			double dx = ping.x + 0.5 - WorldSpaceTransform.getCamX();
			double dy = ping.y - WorldSpaceTransform.getCamY();
			double dz = ping.z + 0.5 - WorldSpaceTransform.getCamZ();
			e.dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
			e.scale = GangPingManager.calculateScale((float) e.dist,
					c.gangPingIconMinScale, c.gangPingIconMaxScale, c.gangPingDistanceScaling);
			e.alpha = GangPingManager.calculateOpacity((float) e.dist, baseAlpha);

			boolean offLeft = e.projX < EDGE_MARGIN;
			boolean offRight = e.projX > screenW - EDGE_MARGIN;
			boolean offTop = e.projY < EDGE_MARGIN;
			boolean offBottom = e.projY > screenH - EDGE_MARGIN;
			e.onScreen = !offLeft && !offRight && !offTop && !offBottom;
			if (!e.onScreen) {
				e.scale = 0.5f;
			}
			if (e.alpha <= 0.01f || (!e.onScreen && !c.gangPingEdgeEnabled)) {
				continue;
			}

			float clampedX;
			if (!e.onScreen && !offLeft && !offRight) {
				clampedX = e.projX < screenW / 2f ? EDGE_MARGIN : screenW - EDGE_MARGIN;
			} else {
				clampedX = Math.max(EDGE_MARGIN, Math.min(screenW - EDGE_MARGIN, e.projX));
			}
			e.ix = (int) clampedX;
			e.iy = (int) Math.max(EDGE_MARGIN, Math.min(screenH - EDGE_MARGIN, e.projY));
			entries.add(e);
		}

		entries.sort((a, b) -> Double.compare(a.dist, b.dist));

		List<int[]> placed = new ArrayList<>();
		for (Entry e : entries) {
			resolveCollision(e, placed, screenW, screenH);
			int half = Math.max(1, (int) (ICON_HALF * e.scale));
			placed.add(new int[]{e.ix - half, e.iy - half, e.ix + half, e.iy + half + 2 + LABEL_H});
			drawEntry(ctx, font, client, e);
		}
	}

	private static void resolveCollision(Entry e, List<int[]> placed, int screenW, int screenH) {
		if (placed.isEmpty()) {
			return;
		}
		int baseY = e.iy;
		int half = Math.max(1, (int) (ICON_HALF * e.scale));
		for (int yi = 0; yi < MAX_NUDGES * 2; yi++) {
			int off = (yi == 0 ? 0 : (yi % 2 == 1 ? (yi + 1) / 2 : -(yi / 2))) * NUDGE_STEP;
			int tryY = Math.max(EDGE_MARGIN, Math.min(screenH - EDGE_MARGIN, baseY + off));
			int[] r = {e.ix - half, tryY - half, e.ix + half, tryY + half + 2 + LABEL_H};
			if (!overlapsAny(r, placed)) {
				e.iy = tryY;
				return;
			}
		}
	}

	private static boolean overlapsAny(int[] r, List<int[]> placed) {
		for (int[] p : placed) {
			if (r[0] < p[2] && r[2] > p[0] && r[1] < p[3] && r[3] > p[1]) {
				return true;
			}
		}
		return false;
	}

	private static void drawEntry(GuiGraphics ctx, Font font, Minecraft client, Entry e) {
		PrisonsConfig c = BetterPrisonsClient.config;
		int ix = e.ix, iy = e.iy;
		float scale = e.scale;
		int scaledHalf = Math.max(1, (int) (ICON_HALF * scale));
		int pingColor = c.gangPingColor;
		int alphaInt = Math.max(0, Math.min(255, (int) (e.alpha * 255)));

		Matrix3x2fStack ms = ctx.pose();
		ms.pushMatrix();
		ms.translate(ix, iy);
		ms.scale(scale, scale);
		drawPlayerHead(ctx, client, e.ping, pingColor, alphaInt);
		ms.popMatrix();

		if (!e.onScreen) {
			double angle = Math.atan2(e.projY - iy, e.projX - ix);
			int arrowOffset = Math.max(ARROW_OFFSET, scaledHalf + 3);
			int argb = (alphaInt << 24) | (pingColor & 0xFFFFFF);
			drawArrow(ctx, ix + (int) (arrowOffset * Math.cos(angle)),
					iy + (int) (arrowOffset * Math.sin(angle)), angle, ARROW_RADIUS, argb);
		}

		// --- Info lines ---
		List<String> lines = new ArrayList<>();
		if (c.gangPingShowName) {
			lines.add(e.ping.playerName);
		}
		if (e.onScreen) {
			if (c.gangPingShowTimer) {
				lines.add((System.currentTimeMillis() - e.ping.createdAt) / 1000L + "s ago");
			}
			if (c.gangPingShowCoords) {
				lines.add(e.ping.x + ", " + e.ping.y + ", " + e.ping.z + " (" + (int) e.dist + "m)");
			}
			if (c.gangPingShowHp) {
				lines.add("HP: " + (int) e.ping.hp + "/" + (int) e.ping.maxHp);
			}
			if (c.gangPingShowFacing) {
				lines.add("Facing: " + e.ping.facing);
			}
		}
		if (lines.isEmpty()) {
			return;
		}

		int textArgb = 0xFF000000 | (pingColor & 0xFFFFFF);
		float textScale = Math.max(0.5f, c.gangPingTextScale);
		int bgPad = 2;
		int lineSpacing = (int) ((LABEL_H + bgPad * 2 + 1) * textScale);
		int lineY = iy + scaledHalf + 2;
		for (String line : lines) {
			int w = font.width(line);
			int scaledW = (int) (w * textScale);
			int scaledH = (int) (LABEL_H * textScale);
			ctx.fill(ix - scaledW / 2 - bgPad, lineY - bgPad,
					ix + (scaledW + 1) / 2 + bgPad, lineY + scaledH + bgPad, PANEL_BG);
			ms.pushMatrix();
			ms.translate(ix, lineY);
			ms.scale(textScale, textScale);
			ctx.drawString(font, Component.literal(line), -w / 2, 0, textArgb, true);
			ms.popMatrix();
			lineY += lineSpacing;
		}
	}

	/** Draws the pinged player's head (face + hat) with a colored border, tinted by the ping alpha. */
	private static void drawPlayerHead(GuiGraphics ctx, Minecraft client,
			GangPingManager.GangPingInfo ping, int color, int alphaInt) {
		int borderArgb = (alphaInt << 24) | (color & 0xFFFFFF);
		int skinArgb = (alphaInt << 24) | 0x00FFFFFF;

		PlayerSkin skin = null;
		if (client.getConnection() != null) {
			PlayerInfo info = client.getConnection().getPlayerInfo(ping.playerName);
			if (info != null) {
				skin = info.getSkin();
			}
		}

		ctx.fill(-9, -9, 9, 9, borderArgb);
		if (skin != null) {
			PlayerFaceRenderer.draw(ctx, skin, -8, -8, 16, skinArgb);
		} else {
			ctx.fill(-8, -8, 8, 8, borderArgb);
			String initial = ping.playerName.substring(0, 1).toUpperCase();
			int textW = client.font.width(initial);
			ctx.drawString(client.font, Component.literal(initial), -textW / 2, -4, 0xFFFFFFFF, true);
		}
	}

	private static void drawArrow(GuiGraphics ctx, int cx, int cy, double angle, int r, int color) {
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
