package dev.nishu.bettercosmic.shared.easyview;

/**
 * A small text overlay drawn within an inventory slot (the "EasyView" system).
 *
 * @param text   the text to draw (e.g. a count or level)
 * @param color  ARGB color, e.g. {@code 0xFFFFFFFF} — include the alpha byte
 * @param scale  render scale; {@code 0.5f} matches vanilla stack-count size, larger is bigger
 * @param bold   whether to render the text bold
 * @param anchor where within the slot to place the text
 */
public record SlotOverlay(String text, int color, float scale, boolean bold, Anchor anchor) {

	/** Convenience: default scale (0.5) and bold, at the given anchor. */
	public SlotOverlay(String text, int color, Anchor anchor) {
		this(text, color, 0.5f, true, anchor);
	}
}
