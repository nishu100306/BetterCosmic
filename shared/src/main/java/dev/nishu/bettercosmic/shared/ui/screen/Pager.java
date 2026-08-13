package dev.nishu.bettercosmic.shared.ui.screen;

import dev.nishu.bettercosmic.shared.ui.core.Theme;
import dev.nishu.bettercosmic.shared.ui.render.RenderUtils;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Footer pagination cluster: a {@code ◄} button, {@code Page n / m} readout, a {@code ►} button, and
 * a row of page dots (accent = current). Arrows disable at the ends. Rendered left-anchored; it
 * remembers its hit-rects from the last {@link #render} so {@link #mouseClicked} can act.
 */
public final class Pager {

	private static final int BTN_W = 16;
	private static final int BTN_H = 14;
	private static final int GAP = 4;
	private static final int NUM_W = 66;

	private final Runnable onPrev;
	private final Runnable onNext;

	private int page;
	private int pageCount;

	// hit-rects captured at render time
	private int prevX, nextX, btnY;

	public Pager(Runnable onPrev, Runnable onNext) {
		this.onPrev = onPrev;
		this.onNext = onNext;
	}

	public void update(int page, int pageCount) {
		this.page = page;
		this.pageCount = pageCount;
	}

	/** Draws the cluster with its left edge at {@code leftX}, vertically centered on {@code centerY}. */
	public void render(GuiGraphics g, int leftX, int centerY, int mouseX, int mouseY) {
		btnY = centerY - BTN_H / 2;
		prevX = leftX;
		int numX = prevX + BTN_W + GAP;
		nextX = numX + NUM_W + GAP;

		boolean canPrev = page > 0;
		boolean canNext = page < pageCount - 1;

		drawButton(g, prevX, btnY, mouseX, mouseY, canPrev, false);
		drawButton(g, nextX, btnY, mouseX, mouseY, canNext, true);

		String num = "Page " + (page + 1) + " / " + pageCount;
		RenderUtils.textCentered(g, num, numX + NUM_W / 2, centerY - 4, Theme.muted);

		// page dots
		int dotsX = nextX + BTN_W + GAP + 2;
		int dotY = centerY - 2;
		for (int i = 0; i < pageCount; i++) {
			RenderUtils.rect(g, dotsX + i * 7, dotY, 4, 4, i == page ? Theme.accent : Theme.faint);
		}
	}

	private void drawButton(GuiGraphics g, int x, int y, int mouseX, int mouseY, boolean enabled, boolean right) {
		boolean hover = enabled && mouseX >= x && mouseX < x + BTN_W && mouseY >= y && mouseY < y + BTN_H;
		RenderUtils.panel(g, x, y, BTN_W, BTN_H, Theme.surface, hover ? Theme.accent : Theme.line);
		int color = !enabled ? Theme.faint : hover ? Theme.text : Theme.muted;
		int tw = 4, th = 8;
		int tx = x + (BTN_W - tw) / 2;
		int ty = y + (BTN_H - th) / 2;
		if (right) {
			triRight(g, tx, ty, tw, th, color);
		} else {
			triLeft(g, tx, ty, tw, th, color);
		}
	}

	public boolean mouseClicked(double mx, double my, int button) {
		if (button != 0) {
			return false;
		}
		if (page > 0 && inBtn(mx, my, prevX)) {
			onPrev.run();
			return true;
		}
		if (page < pageCount - 1 && inBtn(mx, my, nextX)) {
			onNext.run();
			return true;
		}
		return false;
	}

	private boolean inBtn(double mx, double my, int x) {
		return mx >= x && mx < x + BTN_W && my >= btnY && my < btnY + BTN_H;
	}

	// ---- filled chevron triangles ----

	private static void triLeft(GuiGraphics g, int x, int y, int w, int h, int color) {
		int cy = h / 2;
		for (int i = 0; i < h; i++) {
			int d = Math.abs(i - cy);
			int lead = (int) ((float) d / cy * w);
			g.fill(x + lead, y + i, x + w, y + i + 1, color);
		}
	}

	private static void triRight(GuiGraphics g, int x, int y, int w, int h, int color) {
		int cy = h / 2;
		for (int i = 0; i < h; i++) {
			int d = Math.abs(i - cy);
			int end = (int) (x + w - ((float) d / cy) * w);
			g.fill(x, y + i, end, y + i + 1, color);
		}
	}
}
