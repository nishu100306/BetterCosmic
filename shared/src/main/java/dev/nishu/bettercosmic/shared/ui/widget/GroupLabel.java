package dev.nishu.bettercosmic.shared.ui.widget;

import dev.nishu.bettercosmic.shared.ui.core.Theme;
import dev.nishu.bettercosmic.shared.ui.core.UiElement;
import dev.nishu.bettercosmic.shared.ui.core.UiSounds;
import dev.nishu.bettercosmic.shared.ui.render.RenderUtils;
import net.minecraft.client.gui.GuiGraphics;

import java.util.Locale;
import java.util.function.BooleanSupplier;

/**
 * A collapsible section header inside a feature popup: a chevron plus an uppercase group title with a
 * hairline underline spanning the content width. Clicking anywhere on the row toggles the section
 * open/closed (the chevron points down when open, right when collapsed); the owning popup hides the
 * section's option rows while collapsed.
 */
public final class GroupLabel extends UiElement {

	public static final int HEIGHT = 15;
	/** Extra space left below a collapsed header, so stacked collapsed sections read as separate. */
	public static final int COLLAPSED_GAP = 6;

	private final String text;
	private final BooleanSupplier collapsed;
	private final Runnable onToggle;

	public GroupLabel(String label, BooleanSupplier collapsed, Runnable onToggle) {
		this.text = label.toUpperCase(Locale.ROOT);
		this.collapsed = collapsed;
		this.onToggle = onToggle;
	}

	@Override
	public int preferredHeight() {
		return HEIGHT + (collapsed.getAsBoolean() ? COLLAPSED_GAP : 0);
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float dt) {
		hovered = isMouseOver(mouseX, mouseY);
		int color = hovered ? Theme.text : Theme.muted;
		if (collapsed.getAsBoolean()) {
			RenderUtils.triRight(g, x, y + 3, 5, 7, color);
		} else {
			RenderUtils.triDown(g, x, y + 4, 7, 5, color);
		}
		RenderUtils.text(g, text, x + 10, y + 4, color);
		RenderUtils.hLine(g, x, y + HEIGHT - 1, w, Theme.line);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (button == 0 && isMouseOver(mouseX, mouseY)) {
			UiSounds.click();
			onToggle.run();
			return true;
		}
		return false;
	}
}
