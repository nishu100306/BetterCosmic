package dev.nishu.bettercosmic.sky.hud;

import dev.nishu.bettercosmic.shared.hud.BaseHud;
import dev.nishu.bettercosmic.sky.client.BetterSkyClient;
import dev.nishu.bettercosmic.sky.config.SkyConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.AbstractClientPlayer;
import org.joml.Matrix3x2fStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Player List HUD: a compact, small-text list of the other players loaded in the client's world
 * ({@code ClientLevel.players()}, excluding yourself), sorted alphabetically and laid out in columns
 * of a configurable height. An optional "Players: N" header shows the total, and a configurable cap
 * collapses any overflow into a trailing "+N more" line.
 *
 * <p>Names only — no distance, ping, or health — per the feature's scope. Extends the shared
 * {@link BaseHud}; position is moved by the shared HUD editor and scale comes from config.
 */
public class PlayerListHud extends BaseHud {

	/** Unscaled height of one text row, in GUI pixels (kept tight for a compact list). */
	private static final int LINE_H = 10;
	/** Unscaled inner padding around the content, matching the other HUDs' 2px inset. */
	private static final int PAD = 2;

	public PlayerListHud() {
		super("sky-playerlist");
	}

	private static SkyConfig cfg() {
		return BetterSkyClient.config;
	}

	@Override
	public void tick(Minecraft client) {
		this.enabled = cfg().playerListHudEnabled;
	}

	/** The resolved rows and measurements for one frame; shared by render and the editor bounds. */
	private record Layout(List<String> rows, int total, int columns, int perColumn,
			int colWidth, int headerWidth, boolean showHeader) {

		int longestColumn() {
			if (rows.isEmpty()) {
				return 0;
			}
			return Math.min(perColumn, rows.size());
		}
	}

	private Layout layout(Minecraft client) {
		SkyConfig c = cfg();
		Font font = client.font;

		List<String> names = new ArrayList<>();
		if (client.level != null && client.player != null) {
			for (AbstractClientPlayer p : client.level.players()) {
				if (p.getUUID().equals(client.player.getUUID())) {
					continue; // exclude yourself
				}
				// getName() is the plain profile username (team/prefix formatting lives on
				// getDisplayName()), which is what we want for a flat, alphabetically sortable list.
				names.add(p.getName().getString());
			}
		}
		names.sort(Comparator.comparing(String::toLowerCase));

		int total = names.size();
		int cap = Math.max(1, c.playerListMaxEntries);

		List<String> rows;
		if (total > cap) {
			int keep = Math.max(0, cap - 1); // reserve one row for the "+N more" summary
			rows = new ArrayList<>(names.subList(0, keep));
			rows.add("+" + (total - keep) + " more");
		} else {
			rows = names;
		}

		int perColumn = Math.max(1, c.playerListEntriesPerColumn);
		int columns = rows.isEmpty() ? 0 : (int) Math.ceil(rows.size() / (double) perColumn);

		int colWidth = 0;
		for (String row : rows) {
			colWidth = Math.max(colWidth, font.width(row));
		}

		boolean showHeader = c.playerListShowHeader;
		int headerWidth = showHeader ? font.width("Players: " + total) : 0;

		return new Layout(rows, total, columns, perColumn, colWidth, headerWidth, showHeader);
	}

	/** Draws {@code text} at the absolute GUI position ({@code ax},{@code ay}) at the HUD's scale. */
	private void drawScaled(GuiGraphics ctx, Font font, String text, int ax, int ay, int rgb) {
		Matrix3x2fStack m = ctx.pose();
		m.pushMatrix();
		m.scale(scale, scale);
		m.translate(ax / scale, ay / scale);
		ctx.drawString(font, text, 0, 0, 0xFF000000 | (rgb & 0xFFFFFF), true);
		m.popMatrix();
	}

	@Override
	public void render(GuiGraphics ctx, Minecraft client) {
		if (!enabled || client.player == null || client.level == null) {
			return;
		}
		SkyConfig c = cfg();
		this.scale = c.playerListHudScale / 100.0f;

		Layout l = layout(client);
		if (l.rows().isEmpty() && !l.showHeader()) {
			return; // nothing to show and no header requested
		}

		int lineH = scaled(LINE_H);
		int headerH = l.showHeader() ? lineH : 0;
		int bodyH = l.longestColumn() * lineH;
		int bgH = headerH + bodyH;

		int colWidthPx = scaled(l.colWidth());
		int spacingPx = scaled(c.playerListColumnSpacing);
		int contentW = l.columns() > 0
				? l.columns() * colWidthPx + (l.columns() - 1) * spacingPx
				: 0;
		int bgW = Math.max(contentW, scaled(l.headerWidth()));

		// Background + border (opacity kept separate from the RGB color, as the other HUDs do).
		int bg = (c.playerListBgOpacity << 24) | (c.playerListBgColor & 0xFFFFFF);
		int border = (c.playerListBorderOpacity << 24) | (c.playerListBorderColor & 0xFFFFFF);
		int t = c.playerListBorderThickness;
		if (c.playerListBgOpacity > 0) {
			ctx.fill(x - PAD, y - PAD, x + bgW + PAD, y + bgH, bg);
		}
		if (t > 0 && c.playerListBorderOpacity > 0) {
			ctx.fill(x - PAD, y - PAD - t, x + bgW + PAD, y - PAD, border);            // top
			ctx.fill(x - PAD, y + bgH, x + bgW + PAD, y + bgH + t, border);            // bottom
			ctx.fill(x - PAD - t, y - PAD - t, x - PAD, y + bgH + t, border);          // left
			ctx.fill(x + bgW + PAD, y - PAD - t, x + bgW + PAD + t, y + bgH + t, border); // right
		}

		Font font = client.font;
		if (l.showHeader()) {
			drawScaled(ctx, font, "Players: " + l.total(), x, y, c.playerListHeaderColor);
		}

		int bodyY = y + headerH;
		List<String> rows = l.rows();
		for (int i = 0; i < rows.size(); i++) {
			int col = i / l.perColumn();
			int rowInCol = i % l.perColumn();
			int ax = x + col * (colWidthPx + spacingPx);
			int ay = bodyY + rowInCol * lineH;
			drawScaled(ctx, font, rows.get(i), ax, ay, c.playerListNameColor);
		}
	}

	@Override
	public int getWidth() {
		Minecraft client = Minecraft.getInstance();
		if (client == null || client.font == null) {
			return scaled(120);
		}
		this.scale = cfg().playerListHudScale / 100.0f;
		Layout l = layout(client);
		int colWidthPx = scaled(l.colWidth());
		int spacingPx = scaled(cfg().playerListColumnSpacing);
		int contentW = l.columns() > 0
				? l.columns() * colWidthPx + (l.columns() - 1) * spacingPx
				: 0;
		return Math.max(contentW, scaled(l.headerWidth())) + PAD * 2;
	}

	@Override
	public int getHeight() {
		Minecraft client = Minecraft.getInstance();
		if (client == null || client.font == null) {
			return scaled(LINE_H);
		}
		this.scale = cfg().playerListHudScale / 100.0f;
		Layout l = layout(client);
		int headerH = l.showHeader() ? scaled(LINE_H) : 0;
		return headerH + l.longestColumn() * scaled(LINE_H);
	}
}
