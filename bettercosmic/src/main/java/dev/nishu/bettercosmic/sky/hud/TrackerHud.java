package dev.nishu.bettercosmic.sky.hud;

import dev.nishu.bettercosmic.shared.hud.BaseHud;
import dev.nishu.bettercosmic.sky.client.BetterSkyClient;
import dev.nishu.bettercosmic.sky.config.SkyConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.joml.Matrix3x2fStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Tracker HUD: counts the Island Quests you complete this session, broken out by tier
 * (Basic / Elite / Legendary / Godly / Heroic / Mythic), with a running session timer and two
 * on-HUD buttons — <b>Pause</b>/<b>Resume</b> (freezes the timer) and <b>Reset</b> (zeroes the counts
 * and restarts the timer).
 *
 * <p>Counts are fed from chat: BetterSky watches for the server's "{@code … Quest COMPLETE: <Tier> …}"
 * message (see {@link #onChatMessage}). It's fully client-local — nothing is sent to the server.
 *
 * <p>The buttons can't be clicked while the cursor is grabbed (normal gameplay). Following the shared
 * toast system's approach, {@link BetterSkyClient} routes clicks to {@link #handleClick} while the
 * chat screen is open — the HUD stays visible behind chat, so you open chat, click, and close it.
 * Extends the shared {@link BaseHud}: the editor moves it, scale/colors come from config.
 */
public class TrackerHud extends BaseHud {

	/** Unscaled height of one text row, in GUI pixels. */
	private static final int LINE_H = 10;
	/** Unscaled inner padding around the content, matching the other HUDs' 2px inset. */
	private static final int PAD = 2;
	/** Unscaled button metrics. */
	private static final int BTN_H = 12;
	private static final int BTN_HPAD = 4;   // horizontal padding each side of a button label
	private static final int BTN_GAP = 3;    // gap between the two buttons
	private static final int BTN_ROW_GAP = 3; // gap between the last text row and the button row

	/** Quest tiers, in ascending prestige, each with a display name and label color (nearest vanilla). */
	private enum Tier {
		BASIC("Basic", 0xFFFFFF),      // white §f
		ELITE("Elite", 0x00AAAA),      // dark_aqua §3
		LEGENDARY("Legendary", 0xFFAA00), // gold §6
		GODLY("Godly", 0xAA0000),      // dark_red §4
		HEROIC("Heroic", 0xFF55FF),    // light_purple §d
		MYTHIC("Mythic", 0xAA00AA);    // dark_purple §5

		final String display;
		final int color;

		Tier(String display, int color) {
			this.display = display;
			this.color = color;
		}

		/** The tier whose name equals {@code s} (case-insensitive), or {@code null}. */
		static Tier byName(String s) {
			for (Tier t : values()) {
				if (t.display.equalsIgnoreCase(s)) {
					return t;
				}
			}
			return null;
		}
	}

	private static final Component TITLE = Component.literal("Quest Tracker")
			.setStyle(Style.EMPTY.withBold(true).withUnderlined(true));

	/** Completion counts, indexed by {@link Tier#ordinal()}. */
	private final int[] counts = new int[Tier.values().length];

	// Session timer (same freeze-aware scheme as the prisons Stats HUD).
	private long sessionStartTime = 0;
	private boolean paused = false;
	private long pauseStartTime = 0;
	private long totalPauseDuration = 0;

	// Absolute button hit-rects, recomputed each render and read by handleClick.
	private int pauseBtnX, pauseBtnY, pauseBtnW;
	private int resetBtnX, resetBtnY, resetBtnW;
	private int btnH;
	private boolean btnsValid = false;

	public TrackerHud() {
		super("sky-tracker");
	}

	private static SkyConfig cfg() {
		return BetterSkyClient.config;
	}

	@Override
	public void tick(Minecraft client) {
		this.enabled = cfg().trackerHudEnabled;
		// Start the session timer the first time the HUD ticks on Sky (ticking is network-gated).
		if (sessionStartTime == 0) {
			startTimer();
		}
	}

	/**
	 * Feeds one received chat line to the tracker. Increments the matching tier's count when the line
	 * is a quest-completion message; otherwise a no-op. Colour codes are stripped first.
	 */
	public void onChatMessage(String raw) {
		if (raw == null) {
			return;
		}
		String s = raw.replaceAll("§.", "");
		if (!s.contains("Quest") || !s.contains("COMPLETE:")) {
			return;
		}
		Tier tier = detectTier(s);
		if (tier == null) {
			return;
		}
		if (sessionStartTime == 0) {
			startTimer();
		}
		counts[tier.ordinal()]++;
	}

	/** Finds the tier keyword following "COMPLETE:" in a completion line, or {@code null}. */
	private static Tier detectTier(String line) {
		int i = line.indexOf("COMPLETE:");
		if (i < 0) {
			return null;
		}
		String after = line.substring(i + "COMPLETE:".length()).replace("*", " ").trim();
		for (String token : after.split("\\s+")) {
			Tier t = Tier.byName(token);
			if (t != null) {
				return t;
			}
		}
		return null;
	}

	// ---- Timer / buttons ----

	private void startTimer() {
		sessionStartTime = System.currentTimeMillis();
		totalPauseDuration = 0;
		pauseStartTime = 0;
		paused = false;
	}

	/** Toggles the timer's paused state, accumulating paused time so it's excluded from the duration. */
	public void togglePause() {
		if (paused) {
			if (pauseStartTime > 0) {
				totalPauseDuration += System.currentTimeMillis() - pauseStartTime;
				pauseStartTime = 0;
			}
			paused = false;
		} else {
			pauseStartTime = System.currentTimeMillis();
			paused = true;
		}
	}

	/** Zeroes every tier count and restarts the timer. */
	public void reset() {
		for (int i = 0; i < counts.length; i++) {
			counts[i] = 0;
		}
		startTimer();
	}

	private String sessionDuration() {
		if (sessionStartTime == 0) {
			return "0:00:00";
		}
		long elapsed = System.currentTimeMillis() - sessionStartTime - totalPauseDuration;
		if (paused && pauseStartTime > 0) {
			elapsed -= System.currentTimeMillis() - pauseStartTime;
		}
		long seconds = (elapsed / 1000) % 60;
		long minutes = (elapsed / 60000) % 60;
		long hours = elapsed / 3600000;
		return String.format("%d:%02d:%02d", hours, minutes, seconds);
	}

	private String pauseLabel() {
		return paused ? "Resume" : "Pause";
	}

	/**
	 * Routes a click (in GUI-scaled pixels) to a button. Returns whether it hit one. Uses the hit-rects
	 * from the most recent render, so it's only meaningful while the HUD is on screen.
	 */
	public boolean handleClick(double mx, double my) {
		if (!btnsValid) {
			return false;
		}
		if (mx >= pauseBtnX && mx < pauseBtnX + pauseBtnW && my >= pauseBtnY && my < pauseBtnY + btnH) {
			togglePause();
			return true;
		}
		if (mx >= resetBtnX && mx < resetBtnX + resetBtnW && my >= resetBtnY && my < resetBtnY + btnH) {
			reset();
			return true;
		}
		return false;
	}

	// ---- Rendering ----

	/** One display row: its text and RGB color. */
	private record Row(Component text, int rgb) {}

	/** The ordered text rows (title, optional timer, per-tier counts); shared by render and sizing. */
	private List<Row> rows() {
		SkyConfig c = cfg();
		List<Row> r = new ArrayList<>();
		r.add(new Row(TITLE, c.trackerTitleColor));
		if (c.trackerShowTimer) {
			r.add(new Row(Component.literal((paused ? "(P) " : "") + sessionDuration()), c.trackerTimerColor));
		}
		Tier[] tiers = Tier.values();
		for (int i = 0; i < tiers.length; i++) {
			if (c.trackerHideEmpty && counts[i] == 0) {
				continue;
			}
			r.add(new Row(Component.literal(tiers[i].display + ": " + counts[i]), tiers[i].color));
		}
		return r;
	}

	/** Draws {@code text} at the absolute GUI position ({@code ax},{@code ay}) at the HUD's scale. */
	private void drawScaled(GuiGraphics ctx, Font font, Component text, int ax, int ay, int rgb) {
		Matrix3x2fStack m = ctx.pose();
		m.pushMatrix();
		m.scale(scale, scale);
		m.translate(ax / scale, ay / scale);
		ctx.drawString(font, text, 0, 0, 0xFF000000 | (rgb & 0xFFFFFF), true);
		m.popMatrix();
	}

	private void drawButton(GuiGraphics ctx, Font font, String label, int bx, int by, int bw, int rgb) {
		int line = 0xFF000000 | (rgb & 0xFFFFFF);
		ctx.fill(bx, by, bx + bw, by + btnH, 0x80000000);          // translucent fill
		ctx.fill(bx, by, bx + bw, by + 1, line);                   // top
		ctx.fill(bx, by + btnH - 1, bx + bw, by + btnH, line);     // bottom
		ctx.fill(bx, by, bx + 1, by + btnH, line);                 // left
		ctx.fill(bx + bw - 1, by, bx + bw, by + btnH, line);       // right
		int lw = (int) (font.width(label) * scale);
		int lx = bx + (bw - lw) / 2;
		int ly = by + (btnH - scaled(8)) / 2;
		drawScaled(ctx, font, Component.literal(label), lx, ly, rgb);
	}

	@Override
	public void render(GuiGraphics ctx, Minecraft client) {
		if (!enabled || client.player == null) {
			return;
		}
		SkyConfig c = cfg();
		this.scale = c.trackerHudScale / 100.0f;
		Font font = client.font;

		List<Row> rows = rows();
		int lineH = scaled(LINE_H);

		int contentW = 0;
		for (Row row : rows) {
			contentW = Math.max(contentW, (int) (font.width(row.text()) * scale));
		}

		int pad = scaled(BTN_HPAD);
		int gap = scaled(BTN_GAP);
		this.btnH = scaled(BTN_H);
		int pauseW = (int) (font.width(pauseLabel()) * scale) + pad * 2;
		int resetW = (int) (font.width("Reset") * scale) + pad * 2;
		int btnRowW = pauseW + gap + resetW;

		int bgW = Math.max(contentW, btnRowW);
		int rowsH = rows.size() * lineH;
		int btnRowGap = scaled(BTN_ROW_GAP);
		int bgH = rowsH + btnRowGap + btnH;

		// Background + border (opacity kept separate from the RGB color, as the other HUDs do).
		int bg = (c.trackerBgOpacity << 24) | (c.trackerBgColor & 0xFFFFFF);
		int border = (c.trackerBorderOpacity << 24) | (c.trackerBorderColor & 0xFFFFFF);
		int t = c.trackerBorderThickness;
		if (c.trackerBgOpacity > 0) {
			ctx.fill(x - PAD, y - PAD, x + bgW + PAD, y + bgH, bg);
		}
		if (t > 0 && c.trackerBorderOpacity > 0) {
			ctx.fill(x - PAD, y - PAD - t, x + bgW + PAD, y - PAD, border);            // top
			ctx.fill(x - PAD, y + bgH, x + bgW + PAD, y + bgH + t, border);            // bottom
			ctx.fill(x - PAD - t, y - PAD - t, x - PAD, y + bgH + t, border);          // left
			ctx.fill(x + bgW + PAD, y - PAD - t, x + bgW + PAD + t, y + bgH + t, border); // right
		}

		int ry = y;
		for (Row row : rows) {
			drawScaled(ctx, font, row.text(), x, ry, row.rgb());
			ry += lineH;
		}

		int btnY = y + rowsH + btnRowGap;
		int pauseX = x;
		int resetX = x + pauseW + gap;
		drawButton(ctx, font, pauseLabel(), pauseX, btnY, pauseW, paused ? 0x55FF55 : 0xFFFF55);
		drawButton(ctx, font, "Reset", resetX, btnY, resetW, 0xFF5555);

		pauseBtnX = pauseX;
		pauseBtnY = btnY;
		pauseBtnW = pauseW;
		resetBtnX = resetX;
		resetBtnY = btnY;
		resetBtnW = resetW;
		btnsValid = true;
	}

	@Override
	public int getWidth() {
		Minecraft client = Minecraft.getInstance();
		if (client == null || client.font == null) {
			return scaled(120);
		}
		this.scale = cfg().trackerHudScale / 100.0f;
		Font font = client.font;
		int contentW = 0;
		for (Row row : rows()) {
			contentW = Math.max(contentW, (int) (font.width(row.text()) * scale));
		}
		int pad = scaled(BTN_HPAD);
		int gap = scaled(BTN_GAP);
		int pauseW = (int) (font.width(pauseLabel()) * scale) + pad * 2;
		int resetW = (int) (font.width("Reset") * scale) + pad * 2;
		return Math.max(contentW, pauseW + gap + resetW) + PAD * 2;
	}

	@Override
	public int getHeight() {
		this.scale = cfg().trackerHudScale / 100.0f;
		int rowsH = rows().size() * scaled(LINE_H);
		return rowsH + scaled(BTN_ROW_GAP) + scaled(BTN_H);
	}
}
