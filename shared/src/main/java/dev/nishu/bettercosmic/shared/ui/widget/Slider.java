package dev.nishu.bettercosmic.shared.ui.widget;

import dev.nishu.bettercosmic.shared.ui.core.ModalHost;
import dev.nishu.bettercosmic.shared.ui.core.Theme;
import dev.nishu.bettercosmic.shared.ui.core.UiElement;
import dev.nishu.bettercosmic.shared.ui.model.SliderOption;
import dev.nishu.bettercosmic.shared.ui.render.ColorUtils;
import dev.nishu.bettercosmic.shared.ui.render.RenderUtils;
import net.minecraft.client.gui.GuiGraphics;
import org.lwjgl.glfw.GLFW;

/**
 * A thin horizontal slider with a square handle and a right-aligned mono value readout, bound to a
 * {@link SliderOption} (its {@code integer} flag only affects snapping and which characters may be
 * typed). Drag the handle or click the track to jump; <b>click the value readout to type a number
 * directly</b> — Enter or clicking away commits (parsed, then clamped and snapped like a drag), Esc
 * cancels. Writes the option live.
 */
public final class Slider extends UiElement {

	public static final int WIDTH = 112;
	private static final int TRACK_H = 2;
	private static final int HANDLE = 6;
	private static final int VALUE_GAP = 6;
	private static final int MIN_FIELD_W = 44;

	private final SliderOption option;
	private final ModalHost host;

	private int trackLeft, trackW; // captured at render for hit-testing
	private int valueLeft;         // left edge of the value readout region (captured at render)
	private boolean dragging;

	// Inline numeric editing of the value readout.
	private boolean editing;
	private final StringBuilder buffer = new StringBuilder();

	public Slider(SliderOption option, ModalHost host) {
		this.option = option;
		this.host = host;
		this.w = WIDTH;
	}

	private double value() {
		return option.get();
	}

	private void setValue(double v) {
		option.set(snap(v));
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

		// Track width is based on the committed value's readout so it stays put while typing.
		String committed = option.displayValue();
		int valueW = Math.max(18, RenderUtils.textWidth(committed));
		trackLeft = x;
		trackW = w - valueW - VALUE_GAP;
		valueLeft = trackLeft + trackW; // the readout occupies everything to the right of the track

		int textY = y + (h - RenderUtils.lineHeight()) / 2 + 1;

		if (editing) {
			// While typing, the value readout becomes an input box and the track/handle are hidden, so
			// the box replaces the slider instead of being drawn on top of it.
			int fieldW = Math.max(valueW + 8, MIN_FIELD_W);
			int fieldRight = x + w;
			int fieldLeft = fieldRight - fieldW;
			int fieldH = RenderUtils.lineHeight() + 3;
			int fieldY = y + (h - fieldH) / 2;
			RenderUtils.rect(g, fieldLeft, fieldY, fieldW, fieldH, ColorUtils.withAlpha(Theme.faint, 0x99));
			RenderUtils.outline(g, fieldLeft, fieldY, fieldW, fieldH, Theme.accent);

			// Always reserve the caret's width so the (right-aligned) number stays fixed as the caret
			// blinks — otherwise appending/removing "|" shifts the whole string sideways each half-second.
			int caretW = RenderUtils.textWidth("|");
			RenderUtils.textRight(g, buffer.toString(), fieldRight - 3 - caretW, textY, Theme.text);
			if (System.currentTimeMillis() % 1000 < 500) {
				RenderUtils.text(g, "|", fieldRight - 3 - caretW, textY, Theme.text);
			}
			return;
		}

		int ty = y + (h - TRACK_H) / 2;
		double frac = (value() - option.min) / (option.max - option.min);
		frac = Math.max(0, Math.min(1, frac));
		int fillW = (int) (frac * trackW);

		RenderUtils.rect(g, trackLeft, ty, trackW, TRACK_H, ColorUtils.withAlpha(Theme.faint, 0x66));
		RenderUtils.rect(g, trackLeft, ty, fillW, TRACK_H, Theme.accent);

		int hx = trackLeft + fillW - HANDLE / 2;
		int hy = y + (h - HANDLE) / 2;
		RenderUtils.rect(g, hx, hy, HANDLE, HANDLE, (hovered || dragging) ? Theme.text : Theme.accent);

		RenderUtils.textRight(g, committed, x + w, textY, Theme.muted);
	}

	private void setFromMouse(double mouseX) {
		if (trackW <= 0) {
			return;
		}
		double frac = (mouseX - trackLeft) / (double) trackW;
		setValue(option.min + frac * (option.max - option.min));
	}

	private boolean overValue(double mouseX, double mouseY) {
		return mouseX >= valueLeft && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
	}

	private void startEditing() {
		editing = true;
		buffer.setLength(0);
		buffer.append(option.displayValue());
		host.requestFocus(this);
	}

	/** Parses and applies the typed value (clamped + snapped); a blank/invalid buffer keeps the old value. */
	private void commit() {
		String s = buffer.toString().trim();
		try {
			setValue(Double.parseDouble(s));
		} catch (NumberFormatException ignored) {
			// blank or partial entry ("-", ".") — leave the value unchanged
		}
		editing = false;
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (button == 0 && overValue(mouseX, mouseY)) {
			if (!editing) {
				startEditing();
			}
			return true;
		}
		// A click elsewhere on the slider (e.g. the track) commits any in-progress edit first.
		if (editing) {
			commit();
			host.releaseFocus(this);
		}
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
		if (!editing) {
			return false;
		}
		switch (keyCode) {
			case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
				commit();
				host.releaseFocus(this);
			}
			case GLFW.GLFW_KEY_ESCAPE -> {
				editing = false; // cancel — discard the buffer, keep the committed value
				host.releaseFocus(this);
			}
			case GLFW.GLFW_KEY_BACKSPACE -> {
				if (buffer.length() > 0) {
					buffer.deleteCharAt(buffer.length() - 1);
				}
			}
			default -> {
				// swallow every other key (arrows, tab, …) so the popup doesn't act on it while editing
			}
		}
		return true;
	}

	@Override
	public boolean charTyped(char chr, int modifiers) {
		if (!editing) {
			return false;
		}
		if (isAllowed(chr)) {
			buffer.append(chr);
		}
		return true;
	}

	private boolean isAllowed(char chr) {
		if (chr >= '0' && chr <= '9') {
			return true;
		}
		if (chr == '-') {
			return buffer.length() == 0; // a leading sign only
		}
		if (chr == '.') {
			return !option.integer && buffer.indexOf(".") < 0; // one decimal point, non-integer sliders only
		}
		return false;
	}

	@Override
	public void onBlur() {
		if (editing) {
			commit(); // clicking away / focus moving elsewhere commits the typed value
		}
	}
}
