package dev.nishu.bettercosmic.shared.ui.widget;

import dev.nishu.bettercosmic.shared.ui.core.Theme;
import dev.nishu.bettercosmic.shared.ui.core.UiElement;
import dev.nishu.bettercosmic.shared.ui.model.Option;
import dev.nishu.bettercosmic.shared.ui.render.ColorUtils;
import dev.nishu.bettercosmic.shared.ui.render.RenderUtils;
import net.minecraft.client.gui.GuiGraphics;
import org.lwjgl.glfw.GLFW;

/**
 * A thin horizontal slider with a square handle and a right-aligned mono value readout. Serves both
 * {@link Option.Kind#SLIDER} (double) and {@link Option.Kind#INT_SLIDER} (integer) — the only
 * difference is snapping/casting on {@link #setValue}. Drag the handle, click the track to jump, or
 * use Left/Right to step. Writes its bound {@link Option} live.
 */
public final class Slider extends UiElement {

	public static final int WIDTH = 112;
	private static final int TRACK_H = 2;
	private static final int HANDLE = 6;
	private static final int VALUE_GAP = 6;

	private final Option<?> option;
	private final boolean intMode;

	private int trackLeft, trackW; // captured at render for hit-testing
	private boolean dragging;

	public Slider(Option<?> option) {
		this.option = option;
		this.intMode = option.kind == Option.Kind.INT_SLIDER;
	}

	private double value() {
		return ((Number) option.get()).doubleValue();
	}

	@SuppressWarnings("unchecked")
	private void setValue(double v) {
		double snapped = snap(v);
		if (intMode) {
			((Option<Integer>) option).set((int) Math.round(snapped));
		} else {
			((Option<Double>) option).set(snapped);
		}
	}

	private double snap(double v) {
		double clamped = Math.max(option.min, Math.min(option.max, v));
		if (option.step > 0) {
			clamped = option.min + Math.round((clamped - option.min) / option.step) * option.step;
			clamped = Math.max(option.min, Math.min(option.max, clamped));
		}
		// round off float drift so snapped values land exactly on their step (keeps isDefault honest)
		return Math.round(clamped * 10000.0) / 10000.0;
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float dt) {
		hovered = isMouseOver(mouseX, mouseY);

		String valueStr = option.displayValue();
		int valueW = Math.max(18, RenderUtils.textWidth(valueStr));
		trackLeft = x;
		trackW = w - valueW - VALUE_GAP;

		int ty = y + (h - TRACK_H) / 2;
		double frac = (value() - option.min) / (option.max - option.min);
		frac = Math.max(0, Math.min(1, frac));
		int fillW = (int) (frac * trackW);

		RenderUtils.rect(g, trackLeft, ty, trackW, TRACK_H, ColorUtils.withAlpha(Theme.faint, 0x66));
		RenderUtils.rect(g, trackLeft, ty, fillW, TRACK_H, Theme.accent);

		int hx = trackLeft + fillW - HANDLE / 2;
		int hy = y + (h - HANDLE) / 2;
		RenderUtils.rect(g, hx, hy, HANDLE, HANDLE, (hovered || dragging) ? Theme.text : Theme.accent);

		RenderUtils.textRight(g, valueStr, x + w, y + (h - RenderUtils.lineHeight()) / 2 + 1, Theme.muted);
	}

	private void setFromMouse(double mouseX) {
		if (trackW <= 0) {
			return;
		}
		double frac = (mouseX - trackLeft) / (double) trackW;
		setValue(option.min + frac * (option.max - option.min));
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		int ty = y + (h - HANDLE) / 2;
		boolean onTrack = mouseX >= trackLeft - 3 && mouseX <= trackLeft + trackW + 3
			&& mouseY >= ty - 3 && mouseY <= ty + HANDLE + 3;
		if (button == 0 && onTrack) {
			dragging = true;
			setFromMouse(mouseX);
			return true;
		}
		return false;
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
		if (dragging) {
			setFromMouse(mouseX);
			return true;
		}
		return false;
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		dragging = false;
		return false;
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		double stepAmount = option.step > 0 ? option.step : (option.max - option.min) / 20.0;
		if (keyCode == GLFW.GLFW_KEY_LEFT) {
			setValue(value() - stepAmount);
			return true;
		}
		if (keyCode == GLFW.GLFW_KEY_RIGHT) {
			setValue(value() + stepAmount);
			return true;
		}
		return false;
	}
}
