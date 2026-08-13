package dev.nishu.bettercosmic.shared.ui.screen;

import dev.nishu.bettercosmic.shared.ui.core.Theme;
import dev.nishu.bettercosmic.shared.ui.render.RenderUtils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Throwaway Phase-0 smoke screen: proves the rendering foundation ({@link Theme},
 * {@link RenderUtils}) draws a themed, hairline-bordered panel over the blurred world. Not wired to
 * any keybind by default; it is superseded by the real {@code ConfigScreen} in Phase 1 and can be
 * deleted then.
 */
public final class ThemeTestScreen extends Screen {

	public ThemeTestScreen() {
		super(Component.literal("BetterCosmic Theme Test"));
	}

	@Override
	protected void init() {
		Theme.load();
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float dt) {
		super.render(g, mouseX, mouseY, dt); // blurred world + dim

		int w = 300;
		int h = 200;
		int x = (this.width - w) / 2;
		int y = (this.height - h) / 2;

		RenderUtils.panel(g, x, y, w, h, Theme.surface, Theme.line);
		RenderUtils.text(g, "BetterCosmic", x + 10, y + 10, Theme.text);
		RenderUtils.text(g, "theme foundation ok", x + 10, y + 24, Theme.muted);
		RenderUtils.hLine(g, x + 10, y + 38, w - 20, Theme.line);
		RenderUtils.rect(g, x + 10, y + 48, 40, 12, Theme.accent);
		RenderUtils.textRight(g, "faint", x + w - 10, y + 48, Theme.faint);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
