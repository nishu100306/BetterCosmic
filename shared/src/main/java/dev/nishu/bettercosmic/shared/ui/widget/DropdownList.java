package dev.nishu.bettercosmic.shared.ui.widget;

import dev.nishu.bettercosmic.shared.ui.core.Theme;
import dev.nishu.bettercosmic.shared.ui.core.UiElement;
import dev.nishu.bettercosmic.shared.ui.render.ColorUtils;
import dev.nishu.bettercosmic.shared.ui.render.RenderUtils;
import net.minecraft.client.gui.GuiGraphics;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.function.Consumer;

/**
 * The open list of a {@link Dropdown}, rendered as a floating {@link UiElement} in the screen's
 * {@link dev.nishu.bettercosmic.shared.ui.screen.OverlayLayer} so it paints above every popup row and
 * takes input first. Opaque, with the current value marked by an accent tick. Selecting an item or
 * clicking away closes it.
 */
public final class DropdownList extends UiElement {

	private static final int ITEM_H = 14;

	private final List<String> choices;
	private final String current;
	private final Consumer<String> onSelect;
	private final Runnable onClose;

	public DropdownList(List<String> choices, String current, int boxX, int boxY, int boxW,
						int screenH, Consumer<String> onSelect, Runnable onClose) {
		this.choices = choices;
		this.current = current;
		this.onSelect = onSelect;
		this.onClose = onClose;

		int listH = choices.size() * ITEM_H;
		int belowY = boxY + ITEM_H + 1;
		// open downward if it fits, otherwise upward
		int oy = (belowY + listH <= screenH - 2) ? belowY : boxY - listH - 1;
		bounds(boxX, oy, boxW, listH);
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float dt) {
		RenderUtils.rect(g, x, y, w, h, ColorUtils.withAlpha(Theme.surfaceHover, 0xFF));
		RenderUtils.outline(g, x, y, w, h, Theme.accent);

		for (int i = 0; i < choices.size(); i++) {
			int iy = y + i * ITEM_H;
			boolean hover = mouseX >= x && mouseX < x + w && mouseY >= iy && mouseY < iy + ITEM_H;
			if (hover) {
				RenderUtils.rect(g, x + 1, iy, w - 2, ITEM_H, ColorUtils.withAlpha(Theme.accent, 0x22));
			}
			String choice = choices.get(i);
			boolean sel = choice.equals(current);
			RenderUtils.text(g, choice, x + 5, iy + 3, sel ? Theme.accent : Theme.text);
			if (sel) {
				RenderUtils.rect(g, x + w - 8, iy + ITEM_H / 2 - 1, 3, 3, Theme.accent);
			}
		}
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (button == 0 && isMouseOver(mouseX, mouseY)) {
			int i = (int) ((mouseY - y) / ITEM_H);
			if (i >= 0 && i < choices.size()) {
				onSelect.accept(choices.get(i));
			}
		}
		onClose.run(); // any click closes (selection applied above)
		return true;
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
			onClose.run();
			return true;
		}
		return true; // modal while open
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		onClose.run(); // close and let the popup handle the scroll
		return false;
	}
}
