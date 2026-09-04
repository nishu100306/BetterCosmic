package dev.nishu.bettercosmic.shared.ui.screen;

import dev.nishu.bettercosmic.shared.ui.core.ModalHost;
import dev.nishu.bettercosmic.shared.ui.core.Theme;
import dev.nishu.bettercosmic.shared.ui.core.UiElement;
import dev.nishu.bettercosmic.shared.ui.core.UiSounds;
import dev.nishu.bettercosmic.shared.ui.model.ColorOption;
import dev.nishu.bettercosmic.shared.ui.model.DropdownOption;
import dev.nishu.bettercosmic.shared.ui.model.KeybindOption;
import dev.nishu.bettercosmic.shared.ui.model.LabelOption;
import dev.nishu.bettercosmic.shared.ui.model.LinkOption;
import dev.nishu.bettercosmic.shared.ui.model.Option;
import dev.nishu.bettercosmic.shared.ui.model.SliderOption;
import dev.nishu.bettercosmic.shared.ui.model.TextOption;
import dev.nishu.bettercosmic.shared.ui.model.ToggleOption;
import dev.nishu.bettercosmic.shared.ui.render.RenderUtils;
import dev.nishu.bettercosmic.shared.ui.widget.ColorSwatch;
import dev.nishu.bettercosmic.shared.ui.widget.Dropdown;
import dev.nishu.bettercosmic.shared.ui.widget.KeybindButton;
import dev.nishu.bettercosmic.shared.ui.widget.LinkButton;
import dev.nishu.bettercosmic.shared.ui.widget.Slider;
import dev.nishu.bettercosmic.shared.ui.widget.Toggle;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * One popup row: the option's label on the left, its control on the right, and — when the value
 * differs from its default — a small reset (revert) glyph just left of the control.
 *
 * <p>The control widget is chosen by an <b>exhaustive pattern switch</b> over the sealed
 * {@link Option} hierarchy — adding a new {@code *Option} subtype is a compile error here until it's
 * handled (no silent blank rows), and each case receives the concrete type with no cast. A
 * {@link TextOption} has no widget yet (shown read-only) and a {@link LabelOption} is a control-less
 * informational row.
 */
public final class OptionRow extends UiElement {

	public static final int HEIGHT = 22;
	private static final int RESET = 9;
	private static final int GAP = 5;

	private final Option option;
	private final UiElement widget; // null for read-only / informational rows

	private boolean resetShown;
	private int resetX, resetY;

	public OptionRow(Option option, ModalHost host, int screenH) {
		this.option = option;
		this.widget = buildWidget(host, screenH);
	}

	private UiElement buildWidget(ModalHost host, int screenH) {
		return switch (option) {
			case ToggleOption o -> new Toggle(o);
			case SliderOption o -> new Slider(o, host);
			case DropdownOption o -> new Dropdown(o, host, screenH);
			case ColorOption o -> new ColorSwatch(o, host);
			case KeybindOption o -> new KeybindButton(o, host);
			case LinkOption o -> new LinkButton(o);
			case TextOption o -> null;  // read-only until a text field widget exists
			case LabelOption o -> null; // informational row, no control
		};
	}

	@Override
	public int preferredHeight() {
		return HEIGHT;
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float dt) {
		hovered = isMouseOver(mouseX, mouseY);

		// No per-row divider — the only separators are the group-heading underlines (GroupLabel).
		int textY = y + (HEIGHT - RenderUtils.lineHeight()) / 2 + 1;

		if (option.informational()) {
			RenderUtils.text(g, option.displayLabel(), x, textY, Theme.muted);
			resetShown = false;
			return;
		}

		RenderUtils.text(g, option.displayLabel(), x, textY, Theme.text);

		int controlLeft;
		if (widget != null) {
			int ww = widget.w; // each widget sets its own preferred width at construction
			int wx = x + w - ww;
			widget.bounds(wx, y, ww, HEIGHT);
			widget.render(g, mouseX, mouseY, dt);
			controlLeft = wx;
		} else {
			controlLeft = renderReadOnly(g, textY);
		}

		resetShown = option.editable() && !option.isDefault();
		if (resetShown) {
			resetX = controlLeft - GAP - RESET;
			resetY = y + (HEIGHT - RESET) / 2;
			boolean rHover = RenderUtils.hit(mouseX, mouseY, resetX, resetY, RESET, RESET);
			drawReset(g, resetX, resetY, RESET, rHover ? Theme.text : Theme.faint);
		}
	}

	/** Draws a read-only value (e.g. a text option) right-aligned; returns its left edge. */
	private int renderReadOnly(GuiGraphics g, int textY) {
		int rightX = x + w;
		String value = option.displayValue();
		int vw = RenderUtils.textWidth(value);
		RenderUtils.text(g, value, rightX - vw, textY, Theme.muted);
		return rightX - vw;
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (resetShown && button == 0 && RenderUtils.hit(mouseX, mouseY, resetX, resetY, RESET, RESET)) {
			UiSounds.click();
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

	/** A small counter-clockwise circular arrow — the "reset to default" / refresh glyph. */
	private static void drawReset(GuiGraphics g, int x, int y, int size, int color) {
		int cx = x + size / 2;
		int cy = y + size / 2;
		double r = size / 2.0 - 0.5;
		// ~3/4 ring, leaving a gap at the top-right where the arrowhead sits
		for (int deg = 55; deg <= 340; deg += 11) {
			double a = Math.toRadians(deg);
			int gx = cx + (int) Math.round(Math.cos(a) * r);
			int gy = cy - (int) Math.round(Math.sin(a) * r);
			g.fill(gx, gy, gx + 1, gy + 1, color);
		}
		// arrowhead at the ring's top opening (deg≈55), pointing up-left to imply CCW rotation
		double a = Math.toRadians(55);
		int hx = cx + (int) Math.round(Math.cos(a) * r);
		int hy = cy - (int) Math.round(Math.sin(a) * r);
		g.fill(hx - 1, hy, hx + 2, hy + 1, color);      // horizontal barb
		g.fill(hx, hy, hx + 1, hy + 3, color);          // vertical barb
	}
}
