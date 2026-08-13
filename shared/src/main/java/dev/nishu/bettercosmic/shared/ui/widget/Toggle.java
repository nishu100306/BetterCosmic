package dev.nishu.bettercosmic.shared.ui.widget;

import dev.nishu.bettercosmic.shared.ui.core.Theme;
import dev.nishu.bettercosmic.shared.ui.core.UiElement;
import dev.nishu.bettercosmic.shared.ui.model.Option;
import dev.nishu.bettercosmic.shared.ui.render.ColorUtils;
import dev.nishu.bettercosmic.shared.ui.render.RenderUtils;
import net.minecraft.client.gui.GuiGraphics;

/**
 * A boolean pill toggle: off = muted track + light knob (left); on = accent track + accent knob
 * (right). Reads/writes its bound {@link Option} live, so a reset repaints correctly.
 */
public final class Toggle extends UiElement {

	public static final int WIDTH = 22;
	private static final int TRACK_H = 12;
	private static final int KNOB = 8;

	private final Option<Boolean> option;

	public Toggle(Option<Boolean> option) {
		this.option = option;
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float dt) {
		hovered = isMouseOver(mouseX, mouseY);
		boolean on = option.get();

		int ty = y + (h - TRACK_H) / 2;
		int track = on ? ColorUtils.withAlpha(Theme.accent, 0x66) : ColorUtils.withAlpha(Theme.faint, 0x55);
		int border = on ? Theme.accent : Theme.line;
		RenderUtils.rect(g, x, ty, WIDTH, TRACK_H, track);
		RenderUtils.outline(g, x, ty, WIDTH, TRACK_H, border);

		int knobX = on ? x + WIDTH - KNOB - 2 : x + 2;
		int knobY = ty + (TRACK_H - KNOB) / 2;
		int knob = on ? Theme.accent : (hovered ? Theme.text : Theme.muted);
		RenderUtils.rect(g, knobX, knobY, KNOB, KNOB, knob);
	}

	@Override
	public boolean isMouseOver(double mouseX, double mouseY) {
		int ty = y + (h - TRACK_H) / 2;
		return mouseX >= x && mouseX < x + WIDTH && mouseY >= ty && mouseY < ty + TRACK_H;
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (button == 0 && isMouseOver(mouseX, mouseY)) {
			option.set(!option.get());
			return true;
		}
		return false;
	}
}
