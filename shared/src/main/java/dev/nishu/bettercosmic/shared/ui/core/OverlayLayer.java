package dev.nishu.bettercosmic.shared.ui.core;

import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.List;

/**
 * A screen-level top-most render + input pass. The config screen uses it to host the open
 * {@link dev.nishu.bettercosmic.shared.ui.screen.FeaturePopup} above the panel grid — the popup
 * paints last and gets first refusal on input.
 *
 * <p>Floating children <em>inside</em> the popup (dropdown lists, the color picker) are not overlays
 * here; the popup owns them via {@link ModalHost}. This layer therefore usually holds just the popup,
 * but keeps its stack shape for any future screen-level overlay.
 *
 * <p>Only the topmost overlay receives the real mouse; lower ones get an off-screen mouse so nothing
 * beneath the active overlay shows a hover state or tooltip.
 */
public final class OverlayLayer {

	private final List<UiElement> overlays = new ArrayList<>();

	/** Adds an overlay on top of any existing ones. */
	public void add(UiElement overlay) {
		overlays.add(overlay);
	}

	/** Removes an overlay (typically the overlay closing itself). */
	public void remove(UiElement overlay) {
		overlays.remove(overlay);
	}

	public void clear() {
		overlays.clear();
	}

	public boolean isEmpty() {
		return overlays.isEmpty();
	}

	/** @return {@code true} if any overlay is open (the screen uses this to know input is captured). */
	public boolean isActive() {
		return !overlays.isEmpty();
	}

	/** Draws overlays bottom-to-top, after the host's normal tree; only the topmost gets the real mouse. */
	public void render(GuiGraphics g, int mouseX, int mouseY, float dt) {
		int last = overlays.size() - 1;
		for (int i = 0; i < overlays.size(); i++) {
			UiElement overlay = overlays.get(i);
			if (!overlay.visible) {
				continue;
			}
			boolean top = i == last;
			overlay.render(g, top ? mouseX : -1, top ? mouseY : -1, dt);
		}
	}

	// ---- Input, dispatched top-most first; a copy guards against close-during-dispatch mutation ----

	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		for (UiElement o : topFirst()) {
			if (o.mouseClicked(mouseX, mouseY, button)) {
				return true;
			}
		}
		return false;
	}

	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		for (UiElement o : topFirst()) {
			if (o.mouseReleased(mouseX, mouseY, button)) {
				return true;
			}
		}
		return false;
	}

	public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
		for (UiElement o : topFirst()) {
			if (o.mouseDragged(mouseX, mouseY, button, dragX, dragY)) {
				return true;
			}
		}
		return false;
	}

	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		for (UiElement o : topFirst()) {
			if (o.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) {
				return true;
			}
		}
		return false;
	}

	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		for (UiElement o : topFirst()) {
			if (o.keyPressed(keyCode, scanCode, modifiers)) {
				return true;
			}
		}
		return false;
	}

	public boolean charTyped(char chr, int modifiers) {
		for (UiElement o : topFirst()) {
			if (o.charTyped(chr, modifiers)) {
				return true;
			}
		}
		return false;
	}

	private List<UiElement> topFirst() {
		List<UiElement> copy = new ArrayList<>(overlays);
		java.util.Collections.reverse(copy);
		return copy;
	}
}
