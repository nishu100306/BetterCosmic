package dev.nishu.bettercosmic.shared.ui.model;

import net.minecraft.client.gui.GuiGraphics;

/**
 * The small glyph shown on a {@link ConfigPanel} card. Two kinds coexist: <em>parametric</em> glyphs
 * are drawn with primitive fills that scale to any {@code size}; <em>bitmap</em> glyphs carry a
 * hand-authored 16×16 {@code '#'}/{@code '.'} mask (traced from / styled after Minecraft item
 * textures) and are nearest-neighbor scaled into the box. Every glyph is tinted to a single
 * {@code color} at ({@code x},{@code y}).
 */
public enum PanelIcon {
	// ---- parametric glyphs (scale to any size) ----
	/** Potion flask — legacy trinkets icon (superseded by {@link #POTION}). */
	FLASK,
	/** Padlock — locked "coming soon" placeholders. */
	LOCK,
	/** Eye — inventory/overlay views (EasyView). */
	EYE,
	/** Pouch — legacy satchel icon (superseded by {@link #BUNDLE}). */
	SATCHEL,
	/** Bar chart — stats/tracking. */
	CHART,
	/** Sparkle/star — legacy enchant/alert icon (superseded by feature-specific icons). */
	SPARKLE,

	// ---- 16×16 bitmap glyphs (traced from / styled after Minecraft item textures) ----
	/** Splash potion — trinket charge overlay. */
	POTION(
		"................", "................", "...........##...", "..........###...",
		".........####...", "........#####...", ".......#####....", "......#####.....",
		".....#######....", "....#########...", "....#########...", "....#########...",
		"....#########...", ".....#######....", "......#####.....", "................"),
	/** Bundle — satchel tracking (folded-flap leather pouch, redrawn from the texture). */
	BUNDLE(
		"................", "..############..", "..############..", "..############..",
		"...##########...", ".##############.", ".##############.", ".##############.",
		".##############.", ".##############.", ".##############.", "..############..",
		"..############..", "...##########...", "....########....", "................"),
	/** Beacon — waypoints & beams (glass frame + glowing core, redrawn from the item). */
	BEACON(
		"................", "..############..", "..#..........#..", "..#..........#..",
		"..#..........#..", "..#..........#..", "..#...####...#..", "..#...####...#..",
		"..#...####...#..", "..#...####...#..", "..#..........#..", "..#..........#..",
		"..#..........#..", "..#..........#..", "..############..", "................"),
	/** Angled falling block + streaks — events (meteors). */
	METEOR(
		".........#......", "..........#.#...", "...........#.#..", "..........#...#.",
		"...........#....", ".....#......#...", "....###.........", "...#####........",
		"..#######.......", ".#########......", "..#######.......", "...#####........",
		"....###.........", ".....#..........", "................", "................"),
	/** Magnifying glass — chest search. */
	MAGNIFIER(
		"................", ".....###........", "...#######......", "..#########.....",
		"..##.....##.....", ".###.....###....", ".###.....###....", ".###.....###....",
		"..##.....##.....", "..#########.....", "...#########....", ".....###..###...",
		"...........###..", "............###.", ".............###", "..............##"),
	/** Diamond pickaxe — peaceful mining. */
	PICKAXE(
		"................", "................", "......#####.....", ".....#########..",
		"......########..", "..........####..", ".........######.", "........###.###.",
		".......###..###.", "......###...###.", ".....###....###.", "....###......#..",
		"...###..........", "..###...........", "..##............", "................"),
	/** Diamond sword — enchants & auras. */
	SWORD(
		".............###", "............####", "...........#####", "..........#####.",
		".........#####..", "........#####...", "..##...#####....", "..###.#####.....",
		"...#######......", "...######.......", "....####........", "...######.......",
		"..###.####......", "####....##......", "###.............", "###............."),
	/** Clock — command & ability cooldowns. */
	CLOCK(
		"................", "......####......", "....########....", "...###....###...",
		"..##....#...##..", "..##....#...##..", ".##.....#....##.", ".##....##....##.",
		".##....##....##.", ".##......##..##.", "..##.......###..", "..##........##..",
		"...###....###...", "....########....", "......####......", "................"),
	/** Bell — notifications (mounting bar + hanging bell). */
	BELL(
		"..############..", "..############..", "......####......", ".....######.....",
		"....########....", "...##########...", "...##########...", "...##########...",
		"...##########...", "...##########...", "..############..", "..############..",
		".##############.", "......####......", "......####......", "................"),
	/** Map pin — gang pings. */
	MARKER(
		"......####......", "....########....", "...##########...", "..############..",
		"..####....####..", "..###......###..", "..####....####..", "..############..",
		"...##########...", "...##########...", "....########....", ".....######.....",
		"......####......", ".......##.......", ".......##.......", "................"),
	/** Speech bubble — item tooltips. */
	BUBBLE(
		"................", "................", "...##########...", "..############..",
		".##############.", ".##############.", ".##############.", ".##############.",
		".##############.", "..############..", "...##########...", "....#####.......",
		"....####........", "....###.........", "....##..........", "....#..........."),
	/** Sliders — quality of life. */
	SLIDERS(
		"................", "................", "....###.........", "....###.........",
		"..############..", "....###.........", "....###..###....", ".........###....",
		"..############..", ".........###....", "......######....", "......###.......",
		"..############..", "......###.......", "......###.......", "................"),
	/** Gear — general/settings (cog with a center hole). */
	GEAR(
		"......####......", "......####......", "..############..", ".##############.",
		".##############.", ".##############.", "######....######", "######....######",
		"######....######", "######....######", ".##############.", ".##############.",
		".##############.", "..############..", "......####......", "......####......"),
	/** Closed book silhouette — enchants & auras. */
	BOOK(
		"................", "..############..", ".##############.", ".##############.",
		".##############.", ".##############.", ".##############.", ".##############.",
		".##############.", ".##############.", ".##############.", "..############..",
		"..############..", "................", "................", "................"),
	/** Exclamation mark — events alert. */
	EXCLAMATION(
		"................", "......####......", "......####......", "......####......",
		"......####......", "......####......", "......####......", ".......##.......",
		".......##.......", "................", "................", "......####......",
		"......####......", "......####......", "................", "................");

	/** 16×16 glyph bitmap, or {@code null} for a parametric glyph (drawn by {@link #draw}'s switch). */
	private final String[] bits;

	PanelIcon(String... bits) {
		this.bits = bits.length == 0 ? null : bits;
	}

	public void draw(GuiGraphics g, int x, int y, int size, int color) {
		if (bits != null) {
			bitmap(g, x, y, size, color, bits);
			return;
		}
		switch (this) {
			case FLASK -> flask(g, x, y, size, color);
			case LOCK -> lock(g, x, y, size, color);
			case EYE -> eye(g, x, y, size, color);
			case SATCHEL -> satchel(g, x, y, size, color);
			case CHART -> chart(g, x, y, size, color);
			case SPARKLE -> sparkle(g, x, y, size, color);
			default -> { /* bitmap glyphs handled above */ }
		}
	}

	/** Nearest-neighbor scales a 16×16 {@code '#'}/{@code '.'} bitmap into the size×size box. */
	private static void bitmap(GuiGraphics g, int x, int y, int size, int color, String[] rows) {
		for (int dy = 0; dy < size; dy++) {
			String row = rows[dy * 16 / size];
			for (int dx = 0; dx < size; dx++) {
				int sx = dx * 16 / size;
				if (sx < row.length() && row.charAt(sx) == '#') {
					g.fill(x + dx, y + dy, x + dx + 1, y + dy + 1, color);
				}
			}
		}
	}

	private static void sparkle(GuiGraphics g, int x, int y, int s, int color) {
		int cx = x + s / 2;
		int cy = y + s / 2;
		int arm = s / 2 - 1;
		// four-point star: a vertical and horizontal spike tapering to the center
		for (int i = 0; i <= arm; i++) {
			int half = Math.max(0, (arm - i) / 3);
			g.fill(cx - half, cy - i, cx + half + 1, cy - i + 1, color); // up
			g.fill(cx - half, cy + i, cx + half + 1, cy + i + 1, color); // down
			g.fill(cx - i, cy - half, cx - i + 1, cy + half + 1, color); // left
			g.fill(cx + i, cy - half, cx + i + 1, cy + half + 1, color); // right
		}
	}

	private static void chart(GuiGraphics g, int x, int y, int s, int color) {
		int base = y + s - 1;
		int barW = Math.max(2, s / 5);
		int gap = Math.max(1, (s - barW * 3) / 4);
		int[] heights = {Math.round(s * 0.45f), Math.round(s * 0.75f), Math.round(s * 0.6f)};
		int bx = x + gap;
		for (int h : heights) {
			g.fill(bx, base - h, bx + barW, base, color);
			bx += barW + gap;
		}
	}

	private static void eye(GuiGraphics g, int x, int y, int s, int color) {
		int cy = y + s / 2;
		int left = x + 1;
		int right = x + s - 1;
		// almond outline: taller in the middle, tapering to the corners
		int span = right - left;
		for (int i = 0; i <= span; i++) {
			float f = span == 0 ? 0 : i / (float) span;
			int half = Math.round((float) Math.sin(f * Math.PI) * (s * 0.28f));
			int xx = left + i;
			g.fill(xx, cy - half, xx + 1, cy - half + 1, color);       // top lid
			g.fill(xx, cy + half - 1, xx + 1, cy + half, color);       // bottom lid
		}
		g.fill(x + s / 2 - 2, cy - 2, x + s / 2 + 2, cy + 2, color);   // pupil
	}

	private static void satchel(GuiGraphics g, int x, int y, int s, int color) {
		int top = y + Math.round(s * 0.32f);
		int bottom = y + s - 1;
		int span = bottom - top;
		for (int i = 0; i < span; i++) {                               // widening body (trapezoid)
			float f = span == 0 ? 0 : i / (float) span;
			int half = Math.round((s * 0.24f) + f * (s * 0.18f));
			int cx = x + s / 2;
			int yy = top + i;
			g.fill(cx - half, yy, cx + half, yy + 1, color);
		}
		int cx = x + s / 2;
		g.fill(cx - Math.round(s * 0.22f), y + Math.round(s * 0.2f),   // flap
				cx + Math.round(s * 0.22f), top + 1, color);
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
