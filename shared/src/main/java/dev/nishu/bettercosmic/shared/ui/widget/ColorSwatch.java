package dev.nishu.bettercosmic.shared.ui.widget;

import dev.nishu.bettercosmic.shared.ui.core.Theme;
import dev.nishu.bettercosmic.shared.ui.core.UiElement;
import dev.nishu.bettercosmic.shared.ui.model.Option;
import dev.nishu.bettercosmic.shared.ui.render.RenderUtils;
import dev.nishu.bettercosmic.shared.ui.screen.OverlayLayer;
import net.minecraft.client.gui.GuiGraphics;

/**
 * The row control for a {@link Option.Kind#COLOR} option: a small filled swatch plus the {@code
 * #RRGGBB} value. Clicking anywhere on it opens a {@link ColorPickerPopup} in the shared
 * {@link OverlayLayer}. The swatch tracks the option's live value, so the picker's live preview shows
 * here too.
 */
public final class ColorSwatch extends UiElement {

	public static final int WIDTH = 62;
	private static final int SW = 10;

	private final Option<Integer> option;
	private final OverlayLayer overlay;
	private final int screenW;
	private final int screenH;

	private ColorPickerPopup openPicker;

	public ColorSwatch(Option<Integer> option, OverlayLayer overlay, int screenW, int screenH) {
		this.option = option;
		this.overlay = overlay;
		this.screenW = screenW;
		this.screenH = screenH;
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float dt) {
		hovered = isMouseOver(mouseX, mouseY);

		int rightX = x + w;
		String hex = "#" + dev.nishu.bettercosmic.shared.ui.render.ColorUtils.toHex(option.get(), false);
		int hexW = RenderUtils.textWidth(hex);
		int textY = y + (h - RenderUtils.lineHeight()) / 2 + 1;
		RenderUtils.text(g, hex, rightX - hexW, textY, hovered ? Theme.text : Theme.muted);

		int sx = rightX - hexW - 5 - SW;
		int sy = y + (h - SW) / 2;
		RenderUtils.rect(g, sx, sy, SW, SW, 0xFF000000 | (option.get() & 0xFFFFFF));
		RenderUtils.outline(g, sx, sy, SW, SW, hovered ? Theme.accent : Theme.line);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (button == 0 && isMouseOver(mouseX, mouseY) && openPicker == null) {
			openPicker = new ColorPickerPopup(option, screenW, screenH, this::close);
			overlay.add(openPicker);
			return true;
		}
		return false;
	}

	private void close() {
		if (openPicker != null) {
			overlay.remove(openPicker);
			openPicker = null;
		}
	}
}
