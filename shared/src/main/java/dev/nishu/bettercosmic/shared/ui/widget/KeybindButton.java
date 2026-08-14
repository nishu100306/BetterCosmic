package dev.nishu.bettercosmic.shared.ui.widget;

import com.mojang.blaze3d.platform.InputConstants;
import dev.nishu.bettercosmic.shared.ui.core.ModalHost;
import dev.nishu.bettercosmic.shared.ui.core.Theme;
import dev.nishu.bettercosmic.shared.ui.core.UiElement;
import dev.nishu.bettercosmic.shared.ui.core.UiSounds;
import dev.nishu.bettercosmic.shared.ui.model.KeybindOption;
import dev.nishu.bettercosmic.shared.ui.render.RenderUtils;
import net.minecraft.client.gui.GuiGraphics;
import org.lwjgl.glfw.GLFW;

/**
 * A key-binding control: a box showing the bound key (or "Unbound"). Clicking it enters a
 * <em>listening</em> state ("Press a key…") by becoming the host popup's modal, so the next key or
 * mouse button is captured and bound (writing through the {@link Option}'s setter, which persists to
 * MC options). {@code Esc} unbinds. Reset (the row glyph) restores the binding's default key.
 */
public final class KeybindButton extends UiElement {

	public static final int WIDTH = 104;
	private static final int BOX_H = 14;

	private final KeybindOption option;
	private final ModalHost host;
	private boolean listening;

	public KeybindButton(KeybindOption option, ModalHost host) {
		this.option = option;
		this.host = host;
		this.w = WIDTH;
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float dt) {
		hovered = isMouseOver(mouseX, mouseY);
		int by = y + (h - BOX_H) / 2;
		int border = (listening || hovered) ? Theme.accent : Theme.line;
		RenderUtils.panel(g, x, by, WIDTH, BOX_H, Theme.surface, border);

		String text = listening ? "Press a key…" : option.displayValue();
		RenderUtils.textCentered(g, text, x + WIDTH / 2, by + (BOX_H - 8) / 2, listening ? Theme.accent : Theme.text);
	}

	@Override
	public boolean isMouseOver(double mouseX, double mouseY) {
		int by = y + (h - BOX_H) / 2;
		return mouseX >= x && mouseX < x + WIDTH && mouseY >= by && mouseY < by + BOX_H;
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (listening) {
			// Clicking the box binds that mouse button; clicking off it cancels without changing.
			if (isMouseOver(mouseX, mouseY)) {
				option.bind(InputConstants.Type.MOUSE.getOrCreate(button));
			}
			stop();
			return true;
		}
		if (button == 0 && isMouseOver(mouseX, mouseY)) {
			listening = true;
			UiSounds.click();
			host.openModal(this); // capture all input until a key/button is pressed
			return true;
		}
		return false;
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (!listening) {
			return false;
		}
		if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
			option.bind(InputConstants.UNKNOWN); // Esc unbinds
		} else {
			option.bind(InputConstants.Type.KEYSYM.getOrCreate(keyCode));
		}
		stop();
		return true;
	}

	private void stop() {
		listening = false;
		host.closeModal();
	}
}
