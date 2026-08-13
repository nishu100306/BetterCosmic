package dev.nishu.bettercosmic.shared.ui.widget;

import dev.nishu.bettercosmic.shared.ui.core.Theme;
import dev.nishu.bettercosmic.shared.ui.core.UiElement;
import dev.nishu.bettercosmic.shared.ui.model.Option;
import dev.nishu.bettercosmic.shared.ui.render.RenderUtils;
import dev.nishu.bettercosmic.shared.ui.screen.OverlayLayer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * A compact dropdown: a bordered box showing the current value + a caret. Clicking opens a
 * {@link DropdownList} in the shared {@link OverlayLayer} (so it floats above the popup body);
 * choosing an item writes the bound {@link Option} live and closes the list.
 */
public final class Dropdown extends UiElement {

	public static final int WIDTH = 104;
	private static final int BOX_H = 14;

	private final Option<String> option;
	private final OverlayLayer overlay;
	private final int screenH;

	private DropdownList openList;

	public Dropdown(Option<String> option, OverlayLayer overlay, int screenH) {
		this.option = option;
		this.overlay = overlay;
		this.screenH = screenH;
	}

	public boolean isOpen() {
		return openList != null;
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float dt) {
		hovered = isMouseOver(mouseX, mouseY);
		int by = y + (h - BOX_H) / 2;
		int border = (hovered || isOpen()) ? Theme.accent : Theme.line;
		RenderUtils.panel(g, x, by, WIDTH, BOX_H, Theme.surface, border);

		// caret
		int caretW = 5, caretH = 3;
		int caretX = x + WIDTH - caretW - 5;
		int caretY = by + (BOX_H - caretH) / 2;
		RenderUtils.triDown(g, caretX, caretY, caretW, caretH, Theme.muted);

		// current value, truncated to fit before the caret
		Font font = RenderUtils.font();
		String text = font.plainSubstrByWidth(option.get(), caretX - (x + 5) - 2);
		RenderUtils.text(g, text, x + 5, by + (BOX_H - 8) / 2, Theme.text);
	}

	@Override
	public boolean isMouseOver(double mouseX, double mouseY) {
		int by = y + (h - BOX_H) / 2;
		return mouseX >= x && mouseX < x + WIDTH && mouseY >= by && mouseY < by + BOX_H;
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (button == 0 && isMouseOver(mouseX, mouseY)) {
			if (isOpen()) {
				close();
			} else {
				open();
			}
			return true;
		}
		return false;
	}

	private void open() {
		int by = y + (h - BOX_H) / 2;
		openList = new DropdownList(option.choices, option.get(), x, by, WIDTH, screenH,
			choice -> option.set(choice), this::close);
		overlay.add(openList);
	}

	private void close() {
		if (openList != null) {
			overlay.remove(openList);
			openList = null;
		}
	}
}
