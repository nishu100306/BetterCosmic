package dev.nishu.bettercosmic.shared.ui.screen;

import dev.nishu.bettercosmic.shared.ui.core.Theme;
import dev.nishu.bettercosmic.shared.ui.core.UiElement;
import dev.nishu.bettercosmic.shared.ui.model.ConfigPanel;
import dev.nishu.bettercosmic.shared.ui.model.Option;
import dev.nishu.bettercosmic.shared.ui.model.OptionGroup;
import dev.nishu.bettercosmic.shared.ui.render.ColorUtils;
import dev.nishu.bettercosmic.shared.ui.render.RenderUtils;
import dev.nishu.bettercosmic.shared.ui.widget.ColorPicker;
import dev.nishu.bettercosmic.shared.ui.widget.GroupLabel;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * A centered, modal feature popup — "one BetterPrisons tab" — opened from a panel card. Dims the
 * whole screen, draws a translucent bordered box with an icon+title header and a {@code ✕}, then a
 * scissor-clipped, scrollable body of {@link GroupLabel}s and {@link OptionRow}s with a thin
 * scrollbar when the content overflows.
 *
 * <p>Registered into the {@link OverlayLayer}, so it paints above the grid and takes input first;
 * it closes on the {@code ✕}, a click outside the box, or {@code Esc}.
 *
 * <p>The color picker opens as an attached right-hand sidebar ({@link ColorPicker}) on this same
 * layer — not a separate overlay — so there's no z-order ambiguity. While it's open the popup body is
 * inert (rows get an off-screen mouse; input routes to the picker).
 */
public final class FeaturePopup extends UiElement implements ColorPickerHost {

	private static final int POP_W = 300;
	private static final int HEADER = 18;
	private static final int PAD_X = 11;
	private static final int INNER_TOP = 4;
	private static final int PAD_BOTTOM = 9;
	private static final int SCROLLBAR = 3;

	private final ConfigPanel panel;
	private final List<UiElement> items = new ArrayList<>();
	private Runnable onClose = () -> {};

	private final int screenW;
	private final int screenH;

	// geometry, resolved in the constructor
	private final int px, py, popupH;
	private final int bodyTop, bodyVisibleH, contentH, maxScroll;
	private final boolean scrollbar;
	private int scroll = 0;

	// ✕ hit-rect, captured at render
	private int closeX, closeY;
	private static final int CLOSE = 12;

	// color-picker sidebar (attached; on this layer). null when closed.
	private ColorPicker activePicker;

	public FeaturePopup(ConfigPanel panel, int screenW, int screenH, OverlayLayer overlay) {
		this.panel = panel;
		this.screenW = screenW;
		this.screenH = screenH;

		int cH = 0;
		for (OptionGroup group : panel.groups) {
			GroupLabel gl = new GroupLabel(group.label);
			items.add(gl);
			cH += GroupLabel.HEIGHT;
			for (Option<?> opt : group.options) {
				items.add(new OptionRow(opt, overlay, this, screenH));
				cH += OptionRow.HEIGHT;
			}
		}
		this.contentH = cH;

		int bodyMax = Math.max(60, screenH - 90);
		this.bodyVisibleH = Math.min(contentH, bodyMax);
		this.maxScroll = Math.max(0, contentH - bodyVisibleH);
		this.scrollbar = maxScroll > 0;

		this.popupH = HEADER + INNER_TOP + bodyVisibleH + PAD_BOTTOM;
		this.px = (screenW - POP_W) / 2;
		this.py = (screenH - popupH) / 2;
		this.bodyTop = py + HEADER + INNER_TOP;

		// UiElement bounds cover the popup box (used for outside-click hit testing)
		bounds(px, py, POP_W, popupH);
	}

	public void setOnClose(Runnable onClose) {
		this.onClose = onClose;
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float dt) {
		// full-screen scrim
		g.fill(0, 0, screenW, screenH, 0x88050508);

		// popup box — opaque so the grid never bleeds through
		RenderUtils.rect(g, px, py, POP_W, popupH, ColorUtils.withAlpha(Theme.surfaceHover, 0xFF));
		RenderUtils.outline(g, px, py, POP_W, popupH, ColorUtils.withAlpha(Theme.line, 0xFF));

		// header
		int hIconY = py + (HEADER - 11) / 2;
		panel.icon.draw(g, px + PAD_X, hIconY, 11, Theme.accent);
		RenderUtils.text(g, panel.title, px + PAD_X + 15, py + (HEADER - 8) / 2, Theme.text);
		RenderUtils.hLine(g, px, py + HEADER, POP_W, Theme.line);

		closeX = px + POP_W - PAD_X - CLOSE;
		closeY = py + (HEADER - CLOSE) / 2;
		boolean closeHover = hit(mouseX, mouseY, closeX, closeY, CLOSE, CLOSE);
		drawCross(g, closeX, closeY, CLOSE, closeHover ? Theme.text : Theme.muted);

		// While the picker sidebar is open the body is inert: feed rows an off-screen mouse so
		// nothing behind the picker shows a hover state or tooltip.
		boolean pickerOpen = activePicker != null;
		int bmx = pickerOpen ? -1 : mouseX;
		int bmy = pickerOpen ? -1 : mouseY;

		// body (scissor-clipped, scrollable)
		int itemW = POP_W - 2 * PAD_X - (scrollbar ? SCROLLBAR + 3 : 0);
		RenderUtils.pushScissor(g, px, bodyTop, POP_W, bodyVisibleH);
		int yy = bodyTop - scroll;
		for (UiElement item : items) {
			int ih = heightOf(item);
			item.bounds(px + PAD_X, yy, itemW, ih);
			if (yy + ih > bodyTop && yy < bodyTop + bodyVisibleH) {
				item.render(g, bmx, bmy, dt);
			} else {
				item.hovered = false; // don't leave a scrolled-off row "hovered" (phantom tooltip)
			}
			yy += ih;
		}
		RenderUtils.popScissor(g);

		// scrollbar
		if (scrollbar) {
			int trackX = px + POP_W - PAD_X + 1;
			int trackH = bodyVisibleH;
			RenderUtils.rect(g, trackX, bodyTop, SCROLLBAR, trackH, Theme.line);
			int thumbH = Math.max(12, (int) ((long) trackH * bodyVisibleH / contentH));
			int thumbY = bodyTop + (int) ((long) (trackH - thumbH) * scroll / maxScroll);
			RenderUtils.rect(g, trackX, thumbY, SCROLLBAR, thumbH, Theme.accent);
		}

		// tooltip (suppressed while the picker is open)
		if (!pickerOpen) {
			renderTooltip(g, mouseX, mouseY);
		}

		// color-picker sidebar, drawn on this same layer beside the popup
		if (pickerOpen) {
			activePicker.render(g, mouseX, mouseY, dt);
		}
	}

	private void renderTooltip(GuiGraphics g, int mouseX, int mouseY) {
		Component tip = null;
		for (UiElement item : items) {
			Component t = item.tooltip();
			if (t != null) {
				tip = t;
				break;
			}
		}
		if (tip == null) {
			return;
		}
		String s = tip.getString();
		int tw = RenderUtils.textWidth(s) + 8;
		int th = RenderUtils.lineHeight() + 6;
		int tx = Math.min(mouseX + 10, screenW - tw - 2);
		int ty = Math.max(2, mouseY - th - 2);
		RenderUtils.rect(g, tx, ty, tw, th, Theme.ground);
		RenderUtils.outline(g, tx, ty, tw, th, Theme.accent);
		RenderUtils.text(g, s, tx + 4, ty + 4, Theme.text);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (activePicker != null) {
			activePicker.mouseClicked(mouseX, mouseY, button); // handles inside / OK / Cancel / away
			return true;
		}
		if (button == 0 && hit(mouseX, mouseY, closeX, closeY, CLOSE, CLOSE)) {
			close();
			return true;
		}
		if (!isMouseOver(mouseX, mouseY)) {
			close(); // click outside the box closes
			return true;
		}
		for (UiElement item : items) {
			if (item.mouseClicked(mouseX, mouseY, button)) {
				return true;
			}
		}
		return true; // modal: swallow everything else
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		if (activePicker != null) {
			activePicker.mouseReleased(mouseX, mouseY, button);
			return true;
		}
		for (UiElement item : items) {
			item.mouseReleased(mouseX, mouseY, button);
		}
		return true;
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
		if (activePicker != null) {
			activePicker.mouseDragged(mouseX, mouseY, button, dragX, dragY);
			return true;
		}
		for (UiElement item : items) {
			if (item.mouseDragged(mouseX, mouseY, button, dragX, dragY)) {
				return true;
			}
		}
		return true;
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (activePicker != null) {
			return true; // ignore scroll while picking
		}
		if (maxScroll > 0) {
			scroll = Math.max(0, Math.min(maxScroll, scroll - (int) (scrollY * 14)));
		}
		return true;
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (activePicker != null) {
			activePicker.keyPressed(keyCode, scanCode, modifiers); // Esc cancels the picker
			return true;
		}
		if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
			close();
			return true;
		}
		for (UiElement item : items) {
			if (item.keyPressed(keyCode, scanCode, modifiers)) {
				return true;
			}
		}
		return true; // modal
	}

	@Override
	public boolean charTyped(char chr, int modifiers) {
		if (activePicker != null) {
			activePicker.charTyped(chr, modifiers); // hex typing
			return true;
		}
		for (UiElement item : items) {
			if (item.charTyped(chr, modifiers)) {
				return true;
			}
		}
		return true;
	}

	@Override
	public void openColorPicker(Option<Integer> option) {
		int gap = 6;
		int sx;
		if (px + POP_W + gap + ColorPicker.WIDTH <= screenW - 2) {
			sx = px + POP_W + gap;                 // sidebar on the right (preferred)
		} else if (px - gap - ColorPicker.WIDTH >= 2) {
			sx = px - gap - ColorPicker.WIDTH;     // no room right — sidebar on the left
		} else {
			sx = (screenW - ColorPicker.WIDTH) / 2; // too cramped — center over the popup
		}
		int sy = Math.max(2, Math.min(py, screenH - ColorPicker.HEIGHT - 2));
		activePicker = new ColorPicker(option, sx, sy, () -> activePicker = null);
	}

	private void close() {
		onClose.run();
	}

	private static int heightOf(UiElement item) {
		return item instanceof GroupLabel ? GroupLabel.HEIGHT : OptionRow.HEIGHT;
	}

	private static void drawCross(GuiGraphics g, int x, int y, int size, int color) {
		for (int i = 0; i < size; i++) {
			g.fill(x + i, y + i, x + i + 1, y + i + 1, color);
			g.fill(x + (size - 1 - i), y + i, x + (size - 1 - i) + 1, y + i + 1, color);
		}
	}

	private static boolean hit(double mx, double my, int x, int y, int w, int h) {
		return mx >= x && mx < x + w && my >= y && my < y + h;
	}
}
