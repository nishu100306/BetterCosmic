package dev.nishu.bettercosmic.shared.ui.screen;

import dev.nishu.bettercosmic.shared.ui.core.ModalHost;
import dev.nishu.bettercosmic.shared.ui.core.Theme;
import dev.nishu.bettercosmic.shared.ui.core.UiElement;
import dev.nishu.bettercosmic.shared.ui.model.Option;
import dev.nishu.bettercosmic.shared.ui.render.ColorUtils;
import dev.nishu.bettercosmic.shared.ui.render.RenderUtils;
import dev.nishu.bettercosmic.shared.ui.widget.ColorSwatch;
import dev.nishu.bettercosmic.shared.ui.widget.Dropdown;
import dev.nishu.bettercosmic.shared.ui.widget.Slider;
import dev.nishu.bettercosmic.shared.ui.widget.Toggle;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * One popup row: the option's label on the left, its control on the right, and — when the value
 * differs from its default — a small reset (revert) glyph just left of the control.
 *
 * <p>The control is the interactive widget for the option's {@link Option.Kind}: {@link Toggle},
 * {@link Slider}, {@link Dropdown}, {@link ColorSwatch}. TEXT (and LABEL/LINK/KEYBIND) are shown
 * read-only for now; the reset glyph and layout already work for them. Input is forwarded to the
 * hosted widget.
 */
public final class OptionRow extends UiElement {

	public static final int HEIGHT = 22;
	private static final int RESET = 7;
	private static final int GAP = 5;

	private final Option<?> option;
	private final UiElement widget; // null for read-only kinds

	private boolean resetShown;
	private int resetX, resetY;

	public OptionRow(Option<?> option, ModalHost host, int screenH) {
		this.option = option;
		this.widget = buildWidget(host, screenH);
	}

	@SuppressWarnings("unchecked")
	private UiElement buildWidget(ModalHost host, int screenH) {
		return switch (option.kind) {
			case TOGGLE -> new Toggle((Option<Boolean>) option);
			case SLIDER, INT_SLIDER -> new Slider(option);
			case DROPDOWN -> new Dropdown((Option<String>) option, host, screenH);
			case COLOR -> new ColorSwatch((Option<Integer>) option, host);
			default -> null; // TEXT read-only until it has a consumer; LABEL/LINK/KEYBIND have no widget
		};
	}

	private int widgetWidth() {
		return switch (option.kind) {
			case TOGGLE -> Toggle.WIDTH;
			case SLIDER, INT_SLIDER -> Slider.WIDTH;
			case DROPDOWN -> Dropdown.WIDTH;
			case COLOR -> ColorSwatch.WIDTH;
			default -> 0;
		};
	}

	private boolean editable() {
		return switch (option.kind) {
			case TOGGLE, SLIDER, INT_SLIDER, DROPDOWN, COLOR, TEXT, KEYBIND -> true;
			default -> false;
		};
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float dt) {
		hovered = isMouseOver(mouseX, mouseY);

		RenderUtils.hLine(g, x, y, w, ColorUtils.withAlpha(Theme.line, 0x14)); // divider

		int textY = y + (HEIGHT - RenderUtils.lineHeight()) / 2 + 1;

		if (option.kind == Option.Kind.LABEL) {
			RenderUtils.text(g, option.label, x, textY, Theme.muted);
			resetShown = false;
			return;
		}

		RenderUtils.text(g, option.label, x, textY, Theme.text);

		int controlLeft;
		if (widget != null) {
			int ww = widgetWidth();
			int wx = x + w - ww;
			widget.bounds(wx, y, ww, HEIGHT);
			widget.render(g, mouseX, mouseY, dt);
			controlLeft = wx;
		} else {
			controlLeft = renderReadOnly(g, textY);
		}

		resetShown = editable() && !option.isDefault();
		if (resetShown) {
			resetX = controlLeft - GAP - RESET;
			resetY = y + (HEIGHT - RESET) / 2;
			boolean rHover = hit(mouseX, mouseY, resetX, resetY, RESET, RESET);
			drawReset(g, resetX, resetY, RESET, rHover ? Theme.text : Theme.faint);
		}
	}

	/** Draws a read-only value (TEXT/KEYBIND/LINK) right-aligned; returns its left edge. */
	private int renderReadOnly(GuiGraphics g, int textY) {
		int rightX = x + w;
		String value = option.displayValue();
		int vw = RenderUtils.textWidth(value);
		RenderUtils.text(g, value, rightX - vw, textY, Theme.muted);
		return rightX - vw;
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (resetShown && button == 0 && hit(mouseX, mouseY, resetX, resetY, RESET, RESET)) {
			option.reset();
			return true;
		}
		return widget != null && widget.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
		return widget != null && widget.mouseDragged(mouseX, mouseY, button, dragX, dragY);
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		return widget != null && widget.mouseReleased(mouseX, mouseY, button);
	}

	@Override
	public Component tooltip() {
		return hovered && option.tooltip != null ? Component.literal(option.tooltip) : null;
	}

	private static void drawReset(GuiGraphics g, int x, int y, int size, int color) {
		int mid = y + size / 2;
		g.fill(x + 2, mid, x + size, mid + 1, color);          // shaft
		RenderUtils.triLeft(g, x, y + (size - 5) / 2, 4, 5, color); // arrowhead
	}

	private static boolean hit(double mx, double my, int x, int y, int w, int h) {
		return mx >= x && mx < x + w && my >= y && my < y + h;
	}
}
