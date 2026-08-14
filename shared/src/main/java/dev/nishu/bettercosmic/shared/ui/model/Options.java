package dev.nishu.bettercosmic.shared.ui.model;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Factory for {@link Option}s — one builder per control type. Each takes the binding lambdas (and any
 * spec) and infers the default from the getter at build time. This is the stable public surface;
 * callers use these rather than the {@code *Option} constructors.
 *
 * <pre>{@code
 * Options.toggle("Charge overlay",
 *     () -> config.trinketChargesOverlay,
 *     v  -> { config.trinketChargesOverlay = v; config.save(); });
 * }</pre>
 */
public final class Options {

	private Options() {}

	public static ToggleOption toggle(String label, Supplier<Boolean> get, Consumer<Boolean> set) {
		return new ToggleOption(label, get, set);
	}

	public static SliderOption slider(String label, double min, double max, double step,
									  Supplier<Double> get, Consumer<Double> set) {
		return new SliderOption(label, min, max, step, false, get::get, set::accept);
	}

	public static SliderOption intSlider(String label, int min, int max, int step,
										 Supplier<Integer> get, Consumer<Integer> set) {
		return new SliderOption(label, min, max, step, true,
			() -> get.get(), v -> set.accept((int) Math.round(v)));
	}

	public static DropdownOption dropdown(String label, List<String> choices,
										  Supplier<String> get, Consumer<String> set) {
		return new DropdownOption(label, choices, get, set);
	}

	public static TextOption text(String label, Supplier<String> get, Consumer<String> set) {
		return new TextOption(label, get, set);
	}

	/** ARGB color option. */
	public static ColorOption color(String label, Supplier<Integer> get, Consumer<Integer> set) {
		return new ColorOption(label, get, set);
	}

	/**
	 * A client key binding row. Editing captures a key in the UI; unbinding uses
	 * {@link InputConstants#UNKNOWN}. Reset restores the binding's default key.
	 */
	public static KeybindOption keybind(String label, KeyMapping mapping) {
		return new KeybindOption(label, mapping);
	}

	/** A URL link row (opens behind a vanilla confirm — see {@code LinkButton}). */
	public static LinkOption link(String label, String url) {
		return new LinkOption(label, url);
	}

	/** A static informational label row (no control). */
	public static LabelOption label(String text) {
		return new LabelOption(text);
	}
}
