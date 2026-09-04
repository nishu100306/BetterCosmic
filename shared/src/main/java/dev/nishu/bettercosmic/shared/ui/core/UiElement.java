package dev.nishu.bettercosmic.shared.ui.core;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * Lightweight base for every config-UI component — a positioned, rectangular, optionally interactive
 * widget. Deliberately <em>not</em> vanilla {@code AbstractWidget}, whose chrome (backgrounds, focus
 * navigation, narration) is too heavy for this compact look.
 *
 * <p>The host {@code Screen} owns the element tree and forwards Minecraft's input callbacks here; the
 * event methods use the same signatures as vanilla {@code Screen}/{@code GuiEventListener} so
 * forwarding is a straight pass-through. Event methods return {@code true} when they consume the
 * event (so the host can stop propagating it to elements below).
 */
public abstract class UiElement {

	public int x;
	public int y;
	public int w;
	public int h;

	public boolean visible = true;
	public boolean enabled = true;
	/** Updated each frame in {@link #render} from the mouse position. */
	public boolean hovered = false;

	protected UiElement() {}

	protected UiElement(int x, int y, int w, int h) {
		this.x = x;
		this.y = y;
		this.w = w;
		this.h = h;
	}

	public UiElement bounds(int x, int y, int w, int h) {
		this.x = x;
		this.y = y;
		this.w = w;
		this.h = h;
		return this;
	}

	/**
	 * Draws the element. Implementations should refresh {@link #hovered} (typically
	 * {@code hovered = enabled && isMouseOver(mouseX, mouseY)}) so hover visuals and later hit-tests
	 * agree.
	 *
	 * @param dt partial-tick delta for animation
	 */
	public abstract void render(GuiGraphics g, int mouseX, int mouseY, float dt);

	/** @return whether ({@code mouseX},{@code mouseY}) is within this element's visible bounds. */
	public boolean isMouseOver(double mouseX, double mouseY) {
		return visible
			&& mouseX >= x && mouseX < x + w
			&& mouseY >= y && mouseY < y + h;
	}

	/**
	 * The height this element wants when laid out in a vertical list (e.g. the feature-popup body).
	 * Overridden by list items that have a fixed row height; defaults to the current {@link #h}.
	 */
	public int preferredHeight() {
		return h;
	}

	/** Tooltip to show while hovered, or {@code null} for none. */
	public Component tooltip() {
		return null;
	}

	// ---- Input (override as needed; defaults consume nothing) ----

	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		return false;
	}

	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		return false;
	}

	public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
		return false;
	}

	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		return false;
	}

	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		return false;
	}

	public boolean charTyped(char chr, int modifiers) {
		return false;
	}

	/**
	 * Called when this element loses keyboard focus — e.g. the host hands focus to another element or a
	 * click lands outside it. Editable elements should commit their pending value here. Default no-op.
	 */
	public void onBlur() {
	}
}
