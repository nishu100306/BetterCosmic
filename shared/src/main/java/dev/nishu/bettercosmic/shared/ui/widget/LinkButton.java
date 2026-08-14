package dev.nishu.bettercosmic.shared.ui.widget;

import dev.nishu.bettercosmic.shared.ui.core.Theme;
import dev.nishu.bettercosmic.shared.ui.core.UiElement;
import dev.nishu.bettercosmic.shared.ui.model.Option;
import dev.nishu.bettercosmic.shared.ui.render.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;

import java.net.URI;

/**
 * A link control: an accent "Open" affordance (underlined on hover). Clicking it hands the option's
 * URL to vanilla {@link ConfirmLinkScreen}, which shows the standard "open this link?" confirmation
 * (displaying the URL) and opens the browser only on confirm — the required web-link safety step.
 */
public final class LinkButton extends UiElement {

	public static final int WIDTH = 34;
	private static final String LABEL = "Open";

	private final Option<?> option;

	public LinkButton(Option<?> option) {
		this.option = option;
	}

	private int textX() {
		return x + w - RenderUtils.textWidth(LABEL);
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float dt) {
		hovered = isMouseOver(mouseX, mouseY);
		int tx = textX();
		int textY = y + (h - RenderUtils.lineHeight()) / 2 + 1;
		RenderUtils.text(g, LABEL, tx, textY, Theme.accent);
		if (hovered) {
			RenderUtils.hLine(g, tx, textY + RenderUtils.lineHeight() - 1, RenderUtils.textWidth(LABEL), Theme.accent);
		}
	}

	@Override
	public boolean isMouseOver(double mouseX, double mouseY) {
		return mouseX >= textX() - 2 && mouseX < x + w && mouseY >= y && mouseY < y + h;
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (button == 0 && isMouseOver(mouseX, mouseY)) {
			Minecraft mc = Minecraft.getInstance();
			ConfirmLinkScreen.confirmLinkNow(mc.screen, URI.create(option.url));
			return true;
		}
		return false;
	}
}
