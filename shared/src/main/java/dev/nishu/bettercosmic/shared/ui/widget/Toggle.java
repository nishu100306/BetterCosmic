package dev.nishu.bettercosmic.shared.ui.widget;

import dev.nishu.bettercosmic.shared.ui.core.Theme;
import dev.nishu.bettercosmic.shared.ui.core.UiElement;
import dev.nishu.bettercosmic.shared.ui.core.UiSounds;
import dev.nishu.bettercosmic.shared.ui.model.Option;
import dev.nishu.bettercosmic.shared.ui.render.ColorUtils;
import dev.nishu.bettercosmic.shared.ui.render.RenderUtils;
import net.minecraft.client.gui.GuiGraphics;

/**
 * A boolean pill toggle: off = muted track + light knob (left); on = accent track + accent knob
 * (right), with the knob sliding and colors easing between states. Reads/writes its bound
 * {@link Option} live.
 */
public final class Toggle extends UiElement {

	public static final int WIDTH = 22;
	private static final int TRACK_H = 12;
	private static final int KNOB = 8;

	private final Option<Boolean> option;
	private float anim = -1f; // knob position 0..1; -1 = snap to current value on first render

	public Toggle(Option<Boolean> option) {
		this.option = option;
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float dt) {
		hovered = isMouseOver(mouseX, mouseY);
		float target = option.get() ? 1f : 0f;
		anim = anim < 0 ? target : anim + (target - anim) * 0.35f; // ease toward state

		int ty = y + (h - TRACK_H) / 2;
		int track = ColorUtils.blend(ColorUtils.withAlpha(Theme.faint, 0x55), ColorUtils.withAlpha(Theme.accent, 0x66), anim);
		int border = ColorUtils.blend(Theme.line, Theme.accent, anim);
		RenderUtils.rect(g, x, ty, WIDTH, TRACK_H, track);
		RenderUtils.outline(g, x, ty, WIDTH, TRACK_H, border);

		int offX = x + 2;
		int onX = x + WIDTH - KNOB - 2;
		int knobX = Math.round(offX + (onX - offX) * anim);
		int knobY = ty + (TRACK_H - KNOB) / 2;
		int knob = ColorUtils.blend(hovered ? Theme.text : Theme.muted, Theme.accent, anim);
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
			UiSounds.click();
			return true;
		}
		return false;
	}
}
