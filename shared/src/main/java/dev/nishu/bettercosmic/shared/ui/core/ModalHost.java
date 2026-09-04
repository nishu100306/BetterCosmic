package dev.nishu.bettercosmic.shared.ui.core;

/**
 * A container that can host a single transient <em>modal</em> child on its own layer — an open
 * dropdown list, the color picker, and (later) keybind capture or a link-confirm dialog. Implemented
 * by the feature popup.
 *
 * <p>Only one modal is open at a time: {@link #openModal} replaces whatever was active, and while a
 * modal is open the host makes its normal content inert (no hover/tooltips) and routes all input to
 * the modal. This lets row widgets ({@code Dropdown}, {@code ColorSwatch}) open floating children
 * without depending on the screen layer — breaking the previous {@code ui.widget ↔ ui.screen} cycle.
 */
public interface ModalHost {

	/** Opens {@code modal} as the host's single active modal (replaces any current one). */
	void openModal(UiElement modal);

	/** Closes the active modal, if any. */
	void closeModal();

	/**
	 * A screen position at which to place a modal of the given size <em>beside</em> the host's content
	 * (a right-hand sidebar, falling back to the left, then centered when cramped).
	 *
	 * @return {@code {x, y}} top-left in GUI space
	 */
	int[] sidebarPosition(int modalW, int modalH);

	/**
	 * Requests keyboard focus for {@code element}, so the host routes key and character input to it
	 * (used by inline editable fields such as a slider's typed value). Any previously focused element
	 * is blurred first. Default no-op for hosts without keyboard focus.
	 */
	default void requestFocus(UiElement element) {
	}

	/** Releases keyboard focus if {@code element} currently holds it. Default no-op. */
	default void releaseFocus(UiElement element) {
	}
}
