package dev.nishu.bettercosmic.shared.ui.model;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * A free-text setting. Shown read-only for now (no text field widget exists yet); the binding is in
 * place so it becomes editable as soon as one does.
 */
public final class TextOption extends Option {

	private final Supplier<String> getter;
	private final Consumer<String> setter;
	private final String defaultValue;

	TextOption(String label, String defaultValue, Supplier<String> getter, Consumer<String> setter) {
		super(label);
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
