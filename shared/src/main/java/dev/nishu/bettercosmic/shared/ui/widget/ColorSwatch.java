package dev.nishu.bettercosmic.shared.ui.widget;

import dev.nishu.bettercosmic.shared.ui.core.ModalHost;
import dev.nishu.bettercosmic.shared.ui.core.Theme;
import dev.nishu.bettercosmic.shared.ui.core.UiElement;
import dev.nishu.bettercosmic.shared.ui.core.UiSounds;
import dev.nishu.bettercosmic.shared.ui.model.ColorOption;
import dev.nishu.bettercosmic.shared.ui.render.ColorUtils;
import dev.nishu.bettercosmic.shared.ui.render.RenderUtils;
import net.minecraft.client.gui.GuiGraphics;

/**
 * The row control for a {@link ColorOption}: a small filled swatch plus the {@code #RRGGBB} value.
 * Clicking opens a {@link ColorPicker} as the host popup's modal, placed as a sidebar via
 * {@link ModalHost#sidebarPosition}. The swatch reads the option's live value, so the picker's live
 * preview shows here too.
 */
public final class ColorSwatch extends UiElement {

	public static final int WIDTH = 62;
	private static final int SW = 10;

	private final ColorOption option;
	private final ModalHost host;

	public ColorSwatch(ColorOption option, ModalHost host) {
		this.option = option;
		this.host = host;
		this.w = WIDTH;
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float dt) {
		hovered = isMouseOver(mouseX, mouseY);

		int rightX = x + w;
		String hex = "#" + ColorUtils.toHex(option.get(), false);
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
		if (button == 0 && isMouseOver(mouseX, mouseY)) {
			UiSounds.click();
			int[] pos = host.sidebarPosition(ColorPicker.WIDTH, ColorPicker.HEIGHT);
			host.openModal(new ColorPicker(option, pos[0], pos[1], host::closeModal));
			return true;
		}
		return false;
	}
}
