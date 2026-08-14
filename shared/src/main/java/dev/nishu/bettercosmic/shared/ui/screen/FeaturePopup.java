package dev.nishu.bettercosmic.shared.ui.screen;

import dev.nishu.bettercosmic.shared.ui.core.ModalHost;
import dev.nishu.bettercosmic.shared.ui.core.OverlayLayer;
import dev.nishu.bettercosmic.shared.ui.core.Theme;
import dev.nishu.bettercosmic.shared.ui.core.UiElement;
import dev.nishu.bettercosmic.shared.ui.model.ConfigPanel;
import dev.nishu.bettercosmic.shared.ui.model.Option;
import dev.nishu.bettercosmic.shared.ui.model.OptionGroup;
import dev.nishu.bettercosmic.shared.ui.render.ColorUtils;
import dev.nishu.bettercosmic.shared.ui.render.RenderUtils;
import dev.nishu.bettercosmic.shared.ui.widget.GroupLabel;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * A centered feature popup — "one BetterPrisons tab" — opened from a panel card. Dims the whole
 * screen, draws a bordered box with an icon+title header and a {@code ✕}, then a scissor-clipped,
 * scrollable body of {@link GroupLabel}s and {@link OptionRow}s with a thin scrollbar when the
 * content overflows. Held by the screen's {@link OverlayLayer}; closes on {@code ✕}, click-outside,
 * or {@code Esc}.
 *
 * <p>As a {@link ModalHost} it owns a single transient <em>modal</em> child on its own layer — the
 * color picker (a right-hand sidebar) or an open dropdown list. While a modal is open the body shows
 * no hover, and input goes to the modal first. A modal that declines an outside click (the color
 * picker) is dismissed only by its own controls, the {@code ✕} (which then closes only the modal), or
 * by clicking a different option — clicking empty space leaves it open; self-dismissing modals (the
 * dropdown) close on any outside click as usual.
 */
public final class FeaturePopup extends UiElement implements ModalHost {

	// Feature-popup geometry.
	// Height: a fixed fraction of the screen ("full size") so every panel gets the same generous box
	// regardless of option count — short panels simply leave empty space below. Adjust POPUP_HEIGHT_FRACTION.
	// Width: POP_W_BASE is the right-edge reference (where the color-picker sidebar attaches); extra
	// width is added on the LEFT only (POP_W_EXTRA_LEFT), so the right edge — and sidebar room — is unchanged.
	private static final float POPUP_HEIGHT_FRACTION = 0.85f;
	private static final int POP_W_BASE = 320;
	private static final int POP_W_EXTRA_LEFT = 28;
	private static final int POP_W = POP_W_BASE + POP_W_EXTRA_LEFT;
	private static final int HEADER = 18;
	private static final int PAD_X = 12;
	private static final int INNER_TOP = 5;
	private static final int PAD_BOTTOM = 10;
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
	private static final int CLOSE = 8;

	// single active modal (color picker sidebar or open dropdown list). null when none.
	private UiElement activeModal;

	public FeaturePopup(ConfigPanel panel, int screenW, int screenH) {
		this.panel = panel;
		this.screenW = screenW;
		this.screenH = screenH;

		int cH = 0;
		for (OptionGroup group : panel.groups) {
			GroupLabel gl = new GroupLabel(group.label);
			items.add(gl);
			cH += GroupLabel.HEIGHT;
			for (Option<?> opt : group.options) {
				items.add(new OptionRow(opt, this, screenH));
				cH += OptionRow.HEIGHT;
			}
		}
		this.contentH = cH;

		// Fixed "full size": height is a fraction of the screen; the body fills what's left after the
		// header/padding. Content top-aligns and scrolls only if it ever exceeds the (large) body.
		this.popupH = Math.round(screenH * POPUP_HEIGHT_FRACTION);
		this.bodyVisibleH = Math.max(0, popupH - HEADER - INNER_TOP - PAD_BOTTOM);
		this.maxScroll = Math.max(0, contentH - bodyVisibleH);
		this.scrollbar = maxScroll > 0;

		// Right edge stays where a base-width centered popup's would be; the extra width extends left.
		int rightEdge = (screenW + POP_W_BASE) / 2;
		this.px = rightEdge - POP_W;
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

		// While a modal is open the body is inert: feed rows an off-screen mouse so nothing behind
		// the modal shows a hover state or tooltip.
		boolean modalOpen = activeModal != null;
		int bmx = modalOpen ? -1 : mouseX;
		int bmy = modalOpen ? -1 : mouseY;

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

		// tooltip (suppressed while a modal is open)
		if (!modalOpen) {
			renderTooltip(g, mouseX, mouseY);
		}

		// active modal (picker sidebar / dropdown list), drawn on this same layer, on top
		if (modalOpen) {
			activeModal.render(g, mouseX, mouseY, dt);
		}
	}

	private void renderTooltip(GuiGraphics g, int mouseX, int mouseY) {
		UiElement hoveredItem = null;
		Component tip = null;
		for (UiElement item : items) {
			Component t = item.tooltip();
			if (t != null) {
				tip = t;
				hoveredItem = item;
				break;
			}
		}
		if (tip == null) {
			return;
		}
		String s = tip.getString();
		int tw = RenderUtils.textWidth(s) + 8;
		int th = RenderUtils.lineHeight() + 6;
		int tx = Math.max(2, Math.min(mouseX + 10, screenW - tw - 2));
		// Anchor to the hovered row (above it, or below if there's no room) so the tooltip never
		// covers the row it describes.
		int ty = hoveredItem.y - th - 2;
		if (ty < 2) {
			ty = hoveredItem.y + hoveredItem.h + 2;
		}
		RenderUtils.rect(g, tx, ty, tw, th, ColorUtils.withAlpha(Theme.ground, 0xFF));
		RenderUtils.outline(g, tx, ty, tw, th, Theme.accent);
		RenderUtils.text(g, s, tx + 4, ty + 4, Theme.text);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (activeModal != null) {
			if (activeModal.mouseClicked(mouseX, mouseY, button)) {
				return true; // the modal handled it (inside interaction, OK/Cancel, or self-dismiss)
			}
			// The modal declined — the click landed outside it and it doesn't self-dismiss (the color
			// picker). It closes only via the ✕ or by selecting a different option; empty space keeps it.
			if (button == 0 && hit(mouseX, mouseY, closeX, closeY, CLOSE, CLOSE)) {
				closeModal(); // ✕ closes the modal only, not the popup
				return true;
			}
			UiElement row = interactiveItemAt(mouseX, mouseY);
			if (row != null) {
				closeModal();                             // switching options closes the modal (value kept)
				row.mouseClicked(mouseX, mouseY, button); // ...and activates the option clicked
				return true;
			}
			return true; // clicking empty space leaves the modal open
		}
		if (button == 0 && hit(mouseX, mouseY, closeX, closeY, CLOSE, CLOSE)) {
			close();
			return true;
		}
		if (!isMouseOver(mouseX, mouseY)) {
			close(); // click outside the box closes the popup
			return true;
		}
		for (UiElement item : items) {
			if (item.mouseClicked(mouseX, mouseY, button)) {
				return true;
			}
		}
		return true; // swallow everything else
	}

	/** The interactive option row under ({@code mx},{@code my}) within the body viewport, or null. */
	private UiElement interactiveItemAt(double mx, double my) {
		if (my < bodyTop || my >= bodyTop + bodyVisibleH) {
			return null;
		}
		for (UiElement item : items) {
			if (item instanceof OptionRow && item.isMouseOver(mx, my)) {
				return item;
			}
		}
		return null;
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		if (activeModal != null) {
			activeModal.mouseReleased(mouseX, mouseY, button);
			return true;
		}
		for (UiElement item : items) {
			item.mouseReleased(mouseX, mouseY, button);
		}
		return true;
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
		if (activeModal != null) {
			activeModal.mouseDragged(mouseX, mouseY, button, dragX, dragY);
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
		if (activeModal != null) {
			activeModal.mouseScrolled(mouseX, mouseY, scrollX, scrollY); // e.g. dropdown list closes
			return true;
		}
		if (maxScroll > 0) {
			scroll = Math.max(0, Math.min(maxScroll, scroll - (int) (scrollY * 14)));
		}
		return true;
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (activeModal != null) {
			activeModal.keyPressed(keyCode, scanCode, modifiers); // Esc cancels the modal
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
		if (activeModal != null) {
			activeModal.charTyped(chr, modifiers); // hex typing
			return true;
		}
		for (UiElement item : items) {
			if (item.charTyped(chr, modifiers)) {
				return true;
			}
		}
		return true;
	}

	// ---- ModalHost ----

	@Override
	public void openModal(UiElement modal) {
		activeModal = modal;
	}

	@Override
	public void closeModal() {
		activeModal = null;
	}

	@Override
	public int[] sidebarPosition(int modalW, int modalH) {
		int gap = 6;
		int sx;
		if (px + POP_W + gap + modalW <= screenW - 2) {
			sx = px + POP_W + gap;              // sidebar on the right (preferred)
		} else if (px - gap - modalW >= 2) {
			sx = px - gap - modalW;             // no room right — sidebar on the left
		} else {
			sx = (screenW - modalW) / 2;        // too cramped — center over the popup
		}
		int sy = Math.max(2, Math.min(py, screenH - modalH - 2));
		return new int[] { sx, sy };
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
