package dev.nishu.bettercosmic.sky.ui;

import dev.nishu.bettercosmic.shared.ui.model.ColorOption;
import dev.nishu.bettercosmic.shared.ui.model.Options;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * BetterSky-side option helpers. Parallels BetterPrisons' {@code PrisonOptions}: the shared
 * {@link ColorOption} works in 32-bit ARGB, while BetterSky stores plain 24-bit RGB in config.
 * {@link #colorRgb} bridges the two — it presents an opaque ARGB value to the color picker and
 * writes back only the RGB bytes.
 */
public final class SkyOptions {

	private SkyOptions() {}

	public static ColorOption colorRgb(String label, int defRgb, Supplier<Integer> getRgb, Consumer<Integer> setRgb) {
		return Options.color(label, 0xFF000000 | (defRgb & 0xFFFFFF),
				() -> 0xFF000000 | (getRgb.get() & 0xFFFFFF),
				v -> setRgb.accept(v & 0xFFFFFF));
	}
}
