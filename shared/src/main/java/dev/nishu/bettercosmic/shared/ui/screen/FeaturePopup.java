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
 * picker) is dismissed by its own controls, by clicking a different option, or by the {@code ✕}
 * (which closes the whole popup, and the modal with it) — clicking empty space leaves it open;
 * self-dismissing modals (the dropdown) close on any outside click as usual.
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
	private final int bodyTop, bodyVisibleH;
	private int contentH, maxScroll;   // recomputed when a section is collapsed/expanded
	private boolean scrollbar;
	private int scroll = 0;

	// collapsible sections: a clickable header + its option rows per group, each with a collapsed flag
	private final boolean[] collapsed;
	private final List<GroupLabel> headers = new ArrayList<>();
	private final List<List<OptionRow>> rowGroups = new ArrayList<>();

	// ✕ hit-rect, captured at render
	private int closeX, closeY;
	private static final int CLOSE = 8;

	// single active modal (color picker sidebar or open dropdown list). null when none.
	private UiElement activeModal;

	private final long openTime = System.currentTimeMillis(); // for the scrim fade-in

	public FeaturePopup(ConfigPanel panel, int screenW, int screenH) {
		this.panel = panel;
		this.screenW = screenW;
		this.screenH = screenH;

		// Build a collapsible section per group: a clickable header + its option rows.
		this.collapsed = new boolean[panel.groups.size()];
		for (int i = 0; i < panel.groups.size(); i++) {
			OptionGroup group = panel.groups.get(i);
			final int gi = i;
			headers.add(new GroupLabel(group.label, () -> collapsed[gi], () -> toggle(gi)));
			List<OptionRow> rows = new ArrayList<>();
			for (Option opt : group.options) {
				rows.add(new OptionRow(opt, this, screenH));
			}
			rowGroups.add(rows);
		}

		// Fixed "full size": height is a fraction of the screen; the body fills what's left after the
		// header/padding. Content top-aligns and scrolls only if it exceeds the body.
		this.popupH = Math.round(screenH * POPUP_HEIGHT_FRACTION);
		this.bodyVisibleH = Math.max(0, popupH - HEADER - INNER_TOP - PAD_BOTTOM);

		// Right edge stays where a base-width centered popup's would be; the extra width extends left.
		int rightEdge = (screenW + POP_W_BASE) / 2;
		this.px = rightEdge - POP_W;
		this.py = (screenH - popupH) / 2;
		this.bodyTop = py + HEADER + INNER_TOP;

		// Populate the visible item list + scroll metrics from the initial (all-expanded) state.
		rebuildLayout();

		// UiElement bounds cover the popup box (used for outside-click hit testing)
		bounds(px, py, POP_W, popupH);
	}

	public void setOnClose(Runnable onClose) {
		this.onClose = onClose;
	}

	/** Toggles a section open/closed and relays out the body. */
	private void toggle(int groupIndex) {
		collapsed[groupIndex] = !collapsed[groupIndex];
		rebuildLayout();
	}

	/**
	 * Rebuilds the visible {@link #items} list — every section header, plus a section's option rows only
	 * while it is expanded — and recomputes the content height, scrollbar, and clamped scroll.
	 */
	private void rebuildLayout() {
		items.clear();
		int cH = 0;
		for (int i = 0; i < headers.size(); i++) {
			items.add(headers.get(i));
			cH += GroupLabel.HEIGHT;
			if (!collapsed[i]) {
				for (OptionRow row : rowGroups.get(i)) {
					items.add(row);
					cH += OptionRow.HEIGHT;
				}
			}
		}
		contentH = cH;
		maxScroll = Math.max(0, contentH - bodyVisibleH);
		scrollbar = maxScroll > 0;
		scroll = Math.max(0, Math.min(scroll, maxScroll));
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float dt) {
		// full-screen scrim, fading in over ~120ms as the popup opens
		float appear = Math.min(1f, (System.currentTimeMillis() - openTime) / 120f);
		g.fill(0, 0, screenW, screenH, ((int) (0x88 * appear) << 24) | 0x050508);

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
		boolean closeHover = RenderUtils.hit(mouseX, mouseY, closeX, closeY, CLOSE, CLOSE);
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
			int ih = item.preferredHeight();
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
			// picker). The ✕ closes the whole popup (and the modal with it); selecting a different
			// option closes just the modal; empty space keeps it open.
			if (button == 0 && RenderUtils.hit(mouseX, mouseY, closeX, closeY, CLOSE, CLOSE)) {
				close(); // ✕ closes the popup — the owned modal closes with it
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
		if (button == 0 && RenderUtils.hit(mouseX, mouseY, closeX, closeY, CLOSE, CLOSE)) {
			close();
			return true;
		}
		if (!isMouseOver(mouseX, mouseY)) {
			close(); // click outside the box closes the popup
			return true;
		}
		// Snapshot: a header's click toggles its section, which rebuilds `items` mid-loop.
		for (UiElement item : new ArrayList<>(items)) {
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
			activeModal.keyPressed(keyCode, scanCode, modifiers); // Esc cancels the modal, keybind captures
			return true;
		}
		if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
			close();
			return true;
		}
		return true; // rows have no keyboard focus; the popup swallows keys
	}

	@Override
	public boolean charTyped(char chr, int modifiers) {
		if (activeModal != null) {
			activeModal.charTyped(chr, modifiers); // hex typing while the picker is open
			return true;
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

	private static void drawCross(GuiGraphics g, int x, int y, int size, int color) {
		for (int i = 0; i < size; i++) {
			g.fill(x + i, y + i, x + i + 1, y + i + 1, color);
			g.fill(x + (size - 1 - i), y + i, x + (size - 1 - i) + 1, y + i + 1, color);
		}
	}
}
