package dev.nishu.bettercosmic.shared.ui.model;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** A one-of-many string setting, edited by a {@code Dropdown}. */
public final class DropdownOption extends Option {

	private final Supplier<String> getter;
	private final Consumer<String> setter;
	private final String defaultValue;
	public final List<String> choices;

	DropdownOption(String label, String defaultValue, List<String> choices,
				   Supplier<String> getter, Consumer<String> setter) {
		super(label);
		this.choices = List.copyOf(choices);
		this.getter = getter;
		this.setter = setter;
		this.defaultValue = defaultValue;
	}

	public String get() {
		return getter.get();
	}

	public void set(String value) {
		setter.accept(value);
	}

	@Override
	public boolean isDefault() {
		return Objects.equals(get(), defaultValue);
	}

	@Override
	public void reset() {
		set(defaultValue);
	}

	@Override
	public String displayValue() {
		return get();
	}
}
