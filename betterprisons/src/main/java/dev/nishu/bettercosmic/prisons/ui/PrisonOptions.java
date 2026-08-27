package dev.nishu.bettercosmic.prisons.ui;

import dev.nishu.bettercosmic.shared.ui.model.ColorOption;
import dev.nishu.bettercosmic.shared.ui.model.Options;

import java.util.function.Supplier;
import java.util.function.Consumer;

/**
 * Small helpers shared by the BetterPrisons config panels.
 *
 * <p>BetterPrisons stores colors as 24-bit RGB ({@code 0xRRGGBB}, no alpha), while the shared
 * {@link ColorOption} works in 32-bit ARGB. {@link #colorRgb} bridges the two: it presents the option
 * as opaque ARGB to the UI/color-picker and writes back only the RGB bytes to the config field.
 */
public final class PrisonOptions {

	private PrisonOptions() {}

	/**
	 * A color option bound to a 24-bit RGB config field. The picker sees an opaque ARGB color; the
	 * setter strips the alpha so the stored field stays {@code 0xRRGGBB}.
	 *
	 * @param defRgb the code default in RGB (reset restores this)
	 * @param getRgb reads the current RGB field
	 * @param setRgb writes the RGB field (and should persist)
	 */
	public static ColorOption colorRgb(String label, int defRgb, Supplier<Integer> getRgb, Consumer<Integer> setRgb) {
		return Options.color(label, 0xFF000000 | (defRgb & 0xFFFFFF),
				() -> 0xFF000000 | (getRgb.get() & 0xFFFFFF),
				v -> setRgb.accept(v & 0xFFFFFF));
	}
}
