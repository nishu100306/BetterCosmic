package dev.nishu.bettercosmic.shared.ui.model;

import dev.nishu.bettercosmic.shared.ui.render.ColorUtils;

import java.util.function.Consumer;
import java.util.function.Supplier;

/** A packed ARGB color setting, edited by a {@code ColorSwatch} / {@code ColorPicker}. */
public final class ColorOption extends Option {

	private final Supplier<Integer> getter;
	private final Consumer<Integer> setter;
	private final int defaultValue;

	ColorOption(String label, int defaultValue, Supplier<Integer> getter, Consumer<Integer> setter) {
		super(label);
		this.getter = getter;
		this.setter = setter;
		this.defaultValue = defaultValue;
	}

	public int get() {
		return getter.get();
	}

	public void set(int value) {
		setter.accept(value);
	}

	@Override
	public boolean isDefault() {
		return get() == defaultValue;
	}

	@Override
	public void reset() {
		set(defaultValue);
	}

	@Override
	public String displayValue() {
		return "#" + ColorUtils.toHex(get(), false);
	}
}
