package dev.nishu.bettercosmic.shared.ui.screen;

import dev.nishu.bettercosmic.shared.ui.core.Theme;
import dev.nishu.bettercosmic.shared.ui.core.UiElement;
import dev.nishu.bettercosmic.shared.ui.model.Option;
import dev.nishu.bettercosmic.shared.ui.render.ColorUtils;
import dev.nishu.bettercosmic.shared.ui.render.RenderUtils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * One popup row: the option's label on the left and its control on the right, separated by a hairline
 * from the row above.
 *
 * <p>Phase 2 renders the control area <em>read-only</em> — the option's current value as text (plus a
 * 2px swatch for colors). Phase 3 replaces the right side with the interactive widget for the
 * option's {@link Option.Kind} and adds the per-row reset glyph; the label/tooltip/layout here carry
 * over unchanged.
 */
public final class OptionRow extends UiElement {

	public static final int HEIGHT = 22;

	private final Option<?> option;

	public OptionRow(Option<?> option) {
		this.option = option;
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float dt) {
		hovered = isMouseOver(mouseX, mouseY);

		// hairline divider at the top of each row
		RenderUtils.hLine(g, x, y, w, ColorUtils.withAlpha(Theme.line, 0x14));

		int textY = y + (HEIGHT - RenderUtils.lineHeight()) / 2 + 1;

		if (option.kind == Option.Kind.LABEL) {
			RenderUtils.text(g, option.label, x, textY, Theme.muted);
			return;
		}

		RenderUtils.text(g, option.label, x, textY, Theme.text);

		int rightX = x + w;
		if (option.kind == Option.Kind.COLOR) {
			String hex = option.displayValue();
			int hexW = RenderUtils.textWidth(hex);
			RenderUtils.text(g, hex, rightX - hexW, textY, Theme.muted);
			// 2px swatch to the left of the hex
			int sw = 8;
			int sx = rightX - hexW - 5 - sw;
			int sy = y + (HEIGHT - sw) / 2;
			RenderUtils.rect(g, sx, sy, sw, sw, 0xFF000000 | (option.colorValue() & 0xFFFFFF));
			RenderUtils.outline(g, sx, sy, sw, sw, Theme.line);
		} else {
			RenderUtils.textRight(g, option.displayValue(), rightX, textY, Theme.muted);
		}
	}

	@Override
	public Component tooltip() {
		return hovered && option.tooltip != null ? Component.literal(option.tooltip) : null;
	}
}
