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
 * Tracker HUD: counts what you earn this session, broken out by tier (Basic / Elite / Legendary /
 * Godly / Heroic / Mythic), with a running session timer and on-HUD buttons — <b>Pause</b>/<b>Resume</b>
 * (freezes the timer), <b>Reset</b> (zeroes the counts and restarts the timer), and a <b>mode</b> toggle
 * that switches the view between:
 * <ul>
 *   <li><b>Quest</b> — Island Quests completed ("{@code … Quest COMPLETE: <Tier> …}").</li>
 *   <li><b>Adventure</b> — adventure chests dropped ("{@code <Tier> Chest dropped nearby!}").</li>
 * </ul>
 *
 * <p>Both are tracked at all times (each with its own counts and timer); the mode toggle only switches
 * which one is shown, and Pause/Reset act on the visible mode. It's fully client-local — the counts come
 * from chat (see {@link #onChatMessage}), nothing is sent to the server.
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
	private static final int BTN_GAP = 3;    // gap between buttons
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

	/** The two things the tracker can count; each keeps its own state, and the toggle switches the view. */
	private enum Mode {
		QUEST("Quest Tracker", "Adv"),
		ADVENTURE("Adventure Tracker", "Quest");

		final String title;         // HUD title line
		final String switchButton;  // toggle-button label: names the mode this one switches TO

		Mode(String title, String switchButton) {
			this.title = title;
			this.switchButton = switchButton;
		}

		Mode next() {
			return this == QUEST ? ADVENTURE : QUEST;
		}
	}

	/** One mode's independent state: per-tier counts and a freeze-aware session timer. */
	private static final class ModeState {
		final int[] counts = new int[Tier.values().length];
		long sessionStartTime = 0;
		boolean paused = false;
		long pauseStartTime = 0;
		long totalPauseDuration = 0;

		void startTimer() {
			sessionStartTime = System.currentTimeMillis();
			totalPauseDuration = 0;
			pauseStartTime = 0;
			paused = false;
		}

		void record(Tier tier) {
			if (sessionStartTime == 0) {
				startTimer();
			}
			counts[tier.ordinal()]++;
		}

		void togglePause() {
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

		void reset() {
			for (int i = 0; i < counts.length; i++) {
				counts[i] = 0;
			}
			startTimer();
		}

		String duration() {
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
	}

	private static final Style TITLE_STYLE = Style.EMPTY.withBold(true).withUnderlined(true);

	private final ModeState quest = new ModeState();
	private final ModeState adventure = new ModeState();
	private Mode mode = Mode.QUEST;

	// Absolute button hit-rects, recomputed each render and read by handleClick.
	private int pauseBtnX, pauseBtnY, pauseBtnW;
	private int resetBtnX, resetBtnY, resetBtnW;
	private int modeBtnX, modeBtnY, modeBtnW;
	private int btnH;
	private boolean btnsValid = false;

	public TrackerHud() {
		super("sky-tracker");
	}

	private static SkyConfig cfg() {
		return BetterSkyClient.config;
	}

	private ModeState current() {
		return mode == Mode.QUEST ? quest : adventure;
	}

	@Override
	public void tick(Minecraft client) {
		this.enabled = cfg().trackerHudEnabled;
		// Start each mode's timer the first time the HUD ticks on Sky (ticking is network-gated), so the
		// timer runs from when you join even before the first quest/chest.
		if (quest.sessionStartTime == 0) {
			quest.startTimer();
		}
		if (adventure.sessionStartTime == 0) {
			adventure.startTimer();
		}
	}

	/**
	 * Feeds one received chat line to the tracker. Increments the matching tier under the right mode when
	 * the line is a quest-completion or adventure-chest message; otherwise a no-op. Both modes are fed
	 * regardless of which is being viewed. Colour codes are stripped first.
	 */
	public void onChatMessage(String raw) {
		if (raw == null) {
			return;
		}
		String s = raw.replaceAll("§.", "");
		if (s.contains("Quest") && s.contains("COMPLETE:")) {
			// Tier is the first tier word after "COMPLETE:".
			Tier tier = detectTier(s.substring(s.indexOf("COMPLETE:")));
			if (tier != null) {
				quest.record(tier);
			}
		} else if (s.contains("Chest dropped")) {
			// e.g. " * Basic Chest dropped nearby! *" — tier is the first tier word on the line.
			Tier tier = detectTier(s);
			if (tier != null) {
				adventure.record(tier);
			}
		}
	}

	/** The first tier keyword appearing as a whitespace-separated token in {@code segment}, or {@code null}. */
	private static Tier detectTier(String segment) {
		for (String token : segment.replace("*", " ").split("\\s+")) {
			Tier t = Tier.byName(token);
			if (t != null) {
				return t;
			}
		}
		return null;
	}

	// ---- Buttons ----

	private String pauseLabel() {
		return current().paused ? "Resume" : "Pause";
	}

	/**
	 * Routes a click (in GUI-scaled pixels) to a button. Returns whether it hit one. Uses the hit-rects
	 * from the most recent render, so it's only meaningful while the HUD is on screen.
	 */
	public boolean handleClick(double mx, double my) {
		if (!btnsValid) {
			return false;
		}
		if (hit(mx, my, pauseBtnX, pauseBtnY, pauseBtnW)) {
			current().togglePause();
			return true;
		}
		if (hit(mx, my, resetBtnX, resetBtnY, resetBtnW)) {
			current().reset();
			return true;
		}
		if (hit(mx, my, modeBtnX, modeBtnY, modeBtnW)) {
			mode = mode.next();
			return true;
		}
		return false;
	}

	private boolean hit(double mx, double my, int bx, int by, int bw) {
		return mx >= bx && mx < bx + bw && my >= by && my < by + btnH;
	}

	// ---- Rendering ----

	/** One display row: its text and RGB color. */
	private record Row(Component text, int rgb) {}

	/** The ordered text rows (title, optional timer, per-tier counts); shared by render and sizing. */
	private List<Row> rows() {
		SkyConfig c = cfg();
		ModeState state = current();
		List<Row> r = new ArrayList<>();
		r.add(new Row(Component.literal(mode.title).setStyle(TITLE_STYLE), c.trackerTitleColor));
		if (c.trackerShowTimer) {
			r.add(new Row(Component.literal((state.paused ? "(P) " : "") + state.duration()), c.trackerTimerColor));
		}
		Tier[] tiers = Tier.values();
		for (int i = 0; i < tiers.length; i++) {
			if (c.trackerHideEmpty && state.counts[i] == 0) {
				continue;
			}
			r.add(new Row(Component.literal(tiers[i].display + ": " + state.counts[i]), tiers[i].color));
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

	/** Draws a button; returns its width so the caller can advance the button row. */
	private int drawButton(GuiGraphics ctx, Font font, String label, int bx, int by, int rgb) {
		int pad = scaled(BTN_HPAD);
		int bw = (int) (font.width(label) * scale) + pad * 2;
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
		return bw;
	}

	private int buttonWidth(Font font, String label) {
		return (int) (font.width(label) * scale) + scaled(BTN_HPAD) * 2;
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
		this.btnH = scaled(BTN_H);
		int gap = scaled(BTN_GAP);

		int contentW = 0;
		for (Row row : rows) {
			contentW = Math.max(contentW, (int) (font.width(row.text()) * scale));
		}

		int pauseW = buttonWidth(font, pauseLabel());
		int resetW = buttonWidth(font, "Reset");
		int modeW = buttonWidth(font, mode.switchButton);
		int btnRowW = pauseW + gap + resetW + gap + modeW;

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
		int bx = x;
		pauseBtnX = bx;
		pauseBtnY = btnY;
		pauseBtnW = drawButton(ctx, font, pauseLabel(), bx, btnY, current().paused ? 0x55FF55 : 0xFFFF55);
		bx += pauseBtnW + gap;
		resetBtnX = bx;
		resetBtnY = btnY;
		resetBtnW = drawButton(ctx, font, "Reset", bx, btnY, 0xFF5555);
		bx += resetBtnW + gap;
		modeBtnX = bx;
		modeBtnY = btnY;
		modeBtnW = drawButton(ctx, font, mode.switchButton, bx, btnY, 0x55FFFF);
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
		int gap = scaled(BTN_GAP);
		int btnRowW = buttonWidth(font, pauseLabel()) + gap + buttonWidth(font, "Reset")
				+ gap + buttonWidth(font, mode.switchButton);
		return Math.max(contentW, btnRowW) + PAD * 2;
	}

	@Override
	public int getHeight() {
		this.scale = cfg().trackerHudScale / 100.0f;
		int rowsH = rows().size() * scaled(LINE_H);
		return rowsH + scaled(BTN_ROW_GAP) + scaled(BTN_H);
	}
}
