package dev.nishu.bettercosmic.shared.ui.model;

import dev.nishu.bettercosmic.shared.ui.render.ColorUtils;
import net.minecraft.client.KeyMapping;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * One configurable setting: a typed value bound to a config field through a {@code getter}/{@code
 * setter} pair, plus the metadata a UI row needs (label, tooltip, kind, and kind-specific spec).
 *
 * <p>The lambda binding replaces BetterPrisons' reflection: {@code get()}/{@code set()} close over
 * the real field and the setter is responsible for persisting (e.g. {@code config.save()}) and any
 * live side effects (e.g. {@code Theme.load()} after a theme color changes). Build instances through
 * the {@link Options} factory rather than this constructor.
 *
 * <p>Phase 2 uses only the read-only {@link #displayValue()} rendering; Phase 3 adds the interactive
 * widget per {@link #kind}.
 */
public final class Option<T> {

	/** What kind of control edits this option — drives both display and (Phase 3) the widget. */
	public enum Kind { TOGGLE, SLIDER, INT_SLIDER, DROPDOWN, TEXT, COLOR, KEYBIND, LINK, LABEL }

	public final Kind kind;
	public final String label;
	public final Supplier<T> getter;
	public final Consumer<T> setter;
	public final T defaultValue;

	/** Optional hover tooltip. Mutable so builders can attach it fluently via {@link #tooltip}. */
	public String tooltip;

	// kind-specific spec (unused fields stay at their neutral defaults)
	public final double min;
	public final double max;
	public final double step;
	public final List<String> choices;   // DROPDOWN
	public final String url;             // LINK
	public final KeyMapping keyMapping;  // KEYBIND — the client key binding this option edits

	Option(Kind kind, String label, Supplier<T> getter, Consumer<T> setter, T defaultValue,
		   double min, double max, double step, List<String> choices, String url) {
		this(kind, label, getter, setter, defaultValue, min, max, step, choices, url, null);
	}

	Option(Kind kind, String label, Supplier<T> getter, Consumer<T> setter, T defaultValue,
		   double min, double max, double step, List<String> choices, String url, KeyMapping keyMapping) {
		this.kind = kind;
		this.label = label;
		this.getter = getter;
		this.setter = setter;
		this.defaultValue = defaultValue;
		this.min = min;
		this.max = max;
		this.step = step;
		this.choices = choices;
		this.url = url;
		this.keyMapping = keyMapping;
	}

	/** Attaches a hover tooltip; returns this for chaining. */
	public Option<T> tooltip(String t) {
		this.tooltip = t;
		return this;
	}

	public T get() {
		return getter.get();
	}

	public void set(T value) {
		setter.accept(value);
	}

	/**
	 * Restores the option's default value (used by the per-row reset glyph). For a keybind this is the
	 * key binding's default key — the setter (see {@link Options#keybind}) applies and persists it.
	 */
	public void reset() {
		set(defaultValue);
	}

	public boolean isDefault() {
		if (kind == Kind.KEYBIND) {
			return keyMapping.isDefault();
		}
		return Objects.equals(get(), defaultValue);
	}

	/** ARGB color for {@link Kind#COLOR} options (undefined for other kinds). */
	public int colorValue() {
		return (Integer) get();
	}

	/** Compact, human-readable current value for a read-only row / value readouts. */
	public String displayValue() {
		if (kind == Kind.KEYBIND) {
			return keyMapping.isUnbound() ? "Unbound" : keyMapping.getTranslatedKeyMessage().getString();
		}
		Object v = get();
		return switch (kind) {
			case TOGGLE -> ((Boolean) v) ? "On" : "Off";
			case COLOR -> "#" + ColorUtils.toHex((Integer) v, false);
			case INT_SLIDER -> String.valueOf(v);
			case SLIDER -> trim(((Number) v).doubleValue());
			case DROPDOWN, TEXT -> String.valueOf(v);
			case KEYBIND -> ""; // handled above
			case LINK -> "Open";
			case LABEL -> "";
		};
	}

	private static String trim(double d) {
		if (d == Math.rint(d)) {
			return String.valueOf((long) d);
		}
		String s = String.format("%.2f", d);
		// strip trailing zeros (2.50 -> 2.5)
		return s.endsWith("0") ? s.substring(0, s.length() - 1) : s;
	}
}
