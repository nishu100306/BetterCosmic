package dev.nishu.bettercosmic.shared.ui.screen;

import dev.nishu.bettercosmic.shared.ui.model.ConfigPanel;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * The 3×2 panel grid. Lays registered {@link ConfigPanel}s out six-to-a-page; trailing cells on the
 * last page become generic empty "coming soon" cards. Owns the current page and rebuilds its
 * {@link PanelCard}s whenever the page or layout changes.
 */
public final class PanelGrid {

	public static final int COLS = 3;
	public static final int ROWS = 2;
	public static final int PER_PAGE = COLS * ROWS;
	private static final int GAP = 8;

	private final List<ConfigPanel> panels;
	private final Consumer<ConfigPanel> onOpen;

	private int page = 0;
	private int gx, gy, gw, gh;
	private final List<PanelCard> cards = new ArrayList<>();

	public PanelGrid(List<ConfigPanel> panels, Consumer<ConfigPanel> onOpen) {
		this.panels = panels;
		this.onOpen = onOpen;
	}

	public int pageCount() {
		return Math.max(1, (int) Math.ceil(panels.size() / (double) PER_PAGE));
	}

	public int page() {
		return page;
	}

	/** Positions the grid within the given rectangle and (re)builds the current page's cards. */
	public void layout(int x, int y, int w, int h) {
		this.gx = x;
		this.gy = y;
		this.gw = w;
		this.gh = h;
		rebuild();
	}

	public void setPage(int p) {
		int clamped = Math.max(0, Math.min(pageCount() - 1, p));
		if (clamped != page) {
			page = clamped;
			rebuild();
		}
	}

	public void nextPage() {
		setPage(page + 1);
	}

	public void prevPage() {
		setPage(page - 1);
	}

	private void rebuild() {
		cards.clear();
		int cardW = (gw - GAP * (COLS - 1)) / COLS;
		int cardH = (gh - GAP * (ROWS - 1)) / ROWS;
		for (int i = 0; i < PER_PAGE; i++) {
			int col = i % COLS;
			int row = i / COLS;
			int cx = gx + col * (cardW + GAP);
			int cy = gy + row * (cardH + GAP);
			int idx = page * PER_PAGE + i;
			ConfigPanel p = idx < panels.size() ? panels.get(idx) : null;
			cards.add(new PanelCard(p, cx, cy, cardW, cardH, onOpen));
		}
	}

	public void render(GuiGraphics g, int mouseX, int mouseY, float dt) {
		for (PanelCard card : cards) {
			card.render(g, mouseX, mouseY, dt);
		}
	}

	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		for (PanelCard card : cards) {
			if (card.mouseClicked(mouseX, mouseY, button)) {
				return true;
			}
		}
		return false;
	}
}
