package dev.nishu.bettercosmic.shared.ui.model;

import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;

/**
 * A numeric setting on a range, edited by a {@code Slider}. Operates in {@code double}; when
 * {@link #integer} is set, values are whole numbers (the binding stores an {@code int}) and display
 * omits decimals.
 */
public final class SliderOption extends Option {

	private final DoubleSupplier getter;
	private final DoubleConsumer setter;
	private final double defaultValue;
	public final double min;
	public final double max;
	public final double step;
	public final boolean integer;

	SliderOption(String label, double min, double max, double step, boolean integer,
				 DoubleSupplier getter, DoubleConsumer setter) {
		super(label);
		this.min = min;
		this.max = max;
		this.step = step;
		this.integer = integer;
		this.getter = getter;
		this.setter = setter;
		this.defaultValue = getter.getAsDouble();
	}

	public double get() {
		return getter.getAsDouble();
	}

	public void set(double value) {
		setter.accept(value);
	}

	@Override
	public boolean isDefault() {
		return Math.abs(get() - defaultValue) < 1e-6;
	}

	@Override
	public void reset() {
		set(defaultValue);
	}

	@Override
	public String displayValue() {
		double v = get();
		if (integer || v == Math.rint(v)) {
			return String.valueOf((long) v);
		}
		String s = String.format("%.2f", v);
		return s.endsWith("0") ? s.substring(0, s.length() - 1) : s; // 2.50 -> 2.5
	}
}
