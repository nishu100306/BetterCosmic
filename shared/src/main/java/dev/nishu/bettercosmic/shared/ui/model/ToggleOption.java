package dev.nishu.bettercosmic.shared.ui.model;

import java.util.function.Consumer;
import java.util.function.Supplier;

/** A boolean setting, edited by a {@code Toggle}. */
public final class ToggleOption extends Option {

	private final Supplier<Boolean> getter;
	private final Consumer<Boolean> setter;
	private final boolean defaultValue;

	ToggleOption(String label, boolean defaultValue, Supplier<Boolean> getter, Consumer<Boolean> setter) {
		super(label);
		this.getter = getter;
		this.setter = setter;
		this.defaultValue = defaultValue;
	}

	public boolean get() {
		return getter.get();
	}

	public void set(boolean value) {
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
		return get() ? "On" : "Off";
	}
}
