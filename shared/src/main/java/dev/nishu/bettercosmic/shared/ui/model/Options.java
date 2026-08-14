package dev.nishu.bettercosmic.shared.ui.model;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Factory for {@link Option}s — one builder per {@link Option.Kind}. Each takes the binding lambdas
 * (and any kind-specific spec) and infers the default value from the getter at build time, so a
 * freshly registered panel's defaults match the config's current values.
 *
 * <pre>{@code
 * Options.toggle("Charge overlay",
 *     () -> config.trinketChargesOverlay,
 *     v  -> { config.trinketChargesOverlay = v; config.save(); });
 * }</pre>
 */
public final class Options {

	private Options() {}

	public static Option<Boolean> toggle(String label, Supplier<Boolean> get, Consumer<Boolean> set) {
		return new Option<>(Option.Kind.TOGGLE, label, get, set, get.get(), 0, 0, 0, null, null);
	}

	public static Option<Double> slider(String label, double min, double max, double step,
										Supplier<Double> get, Consumer<Double> set) {
		return new Option<>(Option.Kind.SLIDER, label, get, set, get.get(), min, max, step, null, null);
	}

	public static Option<Integer> intSlider(String label, int min, int max, int step,
											Supplier<Integer> get, Consumer<Integer> set) {
		return new Option<>(Option.Kind.INT_SLIDER, label, get, set, get.get(), min, max, step, null, null);
	}

	public static Option<String> dropdown(String label, List<String> choices,
										  Supplier<String> get, Consumer<String> set) {
		return new Option<>(Option.Kind.DROPDOWN, label, get, set, get.get(), 0, 0, 0, List.copyOf(choices), null);
	}

	public static Option<String> text(String label, Supplier<String> get, Consumer<String> set) {
		return new Option<>(Option.Kind.TEXT, label, get, set, get.get(), 0, 0, 0, null, null);
	}

	/** ARGB color option. */
	public static Option<Integer> color(String label, Supplier<Integer> get, Consumer<Integer> set) {
		return new Option<>(Option.Kind.COLOR, label, get, set, get.get(), 0, 0, 0, null, null);
	}

	/**
	 * A client key binding row. The bound key is edited via capture in the UI; the setter applies the
	 * new key ({@code null}/UNKNOWN unbinds), refreshes the key map, and persists to MC options. Reset
	 * restores the binding's default key. Value type is {@link InputConstants.Key}.
	 */
	public static Option<InputConstants.Key> keybind(String label, KeyMapping mapping) {
		return new Option<>(Option.Kind.KEYBIND, label,
			() -> null, // current key isn't read directly; display/isDefault use the mapping
			key -> {
				mapping.setKey(key);
				KeyMapping.resetMapping();
				Minecraft.getInstance().options.save();
			},
			mapping.getDefaultKey(),
			0, 0, 0, null, null, mapping);
	}

	/** A URL link row (opens behind a vanilla confirm — see {@code LinkButton}). No binding. */
	public static Option<String> link(String label, String url) {
		return new Option<>(Option.Kind.LINK, label, () -> url, v -> {}, url, 0, 0, 0, null, url);
	}

	/** A static informational label row (no control). */
	public static Option<String> label(String text) {
		return new Option<>(Option.Kind.LABEL, text, () -> "", v -> {}, "", 0, 0, 0, null, null);
	}
}
