package dev.nishu.bettercosmic.shared.ui.widget;

import dev.nishu.bettercosmic.shared.ui.core.Theme;
import dev.nishu.bettercosmic.shared.ui.core.UiElement;
import dev.nishu.bettercosmic.shared.ui.render.RenderUtils;
import net.minecraft.client.gui.GuiGraphics;

import java.util.Locale;

/**
 * A section eyebrow inside a feature popup: an uppercase, muted group title with a hairline underline
 * spanning the content width. Non-interactive.
 */
public final class GroupLabel extends UiElement {

	public static final int HEIGHT = 15;

	private final String text;

	public GroupLabel(String label) {
		this.text = label.toUpperCase(Locale.ROOT);
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float dt) {
		RenderUtils.text(g, text, x, y + 4, Theme.faint);
		RenderUtils.hLine(g, x, y + HEIGHT - 1, w, Theme.line);
	}
}
