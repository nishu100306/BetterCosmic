package dev.nishu.bettercosmic.shared.ui.model;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Factory for {@link Option}s — one builder per control type. Each takes the option's <b>default</b>
 * (the factory/code default, which reset restores — <em>not</em> the live value) plus the binding
 * lambdas and any spec. This is the stable public surface; callers use these rather than the
 * {@code *Option} constructors, and typically source the default from a fresh config instance so it
 * never drifts to the persisted value.
 *
 * <pre>{@code
 * MyConfig d = new MyConfig(); // code defaults
 * Options.toggle("Some overlay", d.someOverlay,
 *     () -> config.someOverlay,
 *     v  -> { config.someOverlay = v; config.save(); });
 * }</pre>
 */
public final class Options {

	private Options() {}

	public static ToggleOption toggle(String label, boolean def, Supplier<Boolean> get, Consumer<Boolean> set) {
		return new ToggleOption(label, def, get, set);
	}

	public static SliderOption slider(String label, double def, double min, double max, double step,
									  Supplier<Double> get, Consumer<Double> set) {
		return new SliderOption(label, def, min, max, step, false, get::get, set::accept);
	}

	public static SliderOption intSlider(String label, int def, int min, int max, int step,
										 Supplier<Integer> get, Consumer<Integer> set) {
		return new SliderOption(label, def, min, max, step, true,
			() -> get.get(), v -> set.accept((int) Math.round(v)));
	}

	public static DropdownOption dropdown(String label, String def, List<String> choices,
										  Supplier<String> get, Consumer<String> set) {
		return new DropdownOption(label, def, choices, get, set);
	}

	public static TextOption text(String label, String def, Supplier<String> get, Consumer<String> set) {
		return new TextOption(label, def, get, set);
	}

	/** ARGB color option. */
	public static ColorOption color(String label, int def, Supplier<Integer> get, Consumer<Integer> set) {
		return new ColorOption(label, def, get, set);
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

	/** An informational label whose text is resolved live each frame (no control). */
	public static LabelOption label(Supplier<String> text) {
		return new LabelOption(text);
	}
}
