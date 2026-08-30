package dev.nishu.bettercosmic.shared.ui.model;

/**
 * One configurable setting. A <b>sealed</b> hierarchy with a subclass per control type
 * ({@link ToggleOption}, {@link SliderOption}, {@link DropdownOption}, {@link ColorOption},
 * {@link KeybindOption}, {@link TextOption}, {@link LinkOption}, {@link LabelOption}). Each subclass
 * carries its own typed binding and spec, so there are no {@code kind}-tagged switches and no
 * unchecked casts — {@code OptionRow} builds the matching widget with an exhaustive pattern switch,
 * which the compiler forces you to extend when a new subtype is added.
 *
 * <p>Build instances through the {@link Options} factory. Bindings are lambda closures over the real
 * config field (not reflection); a subtype's setter persists and applies any side effects.
 */
public sealed abstract class Option
	permits ToggleOption, SliderOption, DropdownOption, ColorOption,
			KeybindOption, TextOption, LinkOption, LabelOption {

	public final String label;

	/** Optional hover tooltip. */
	public String tooltip;

	protected Option(String label) {
		this.label = label;
	}

	/** Attaches a hover tooltip; returns this for chaining. */
	public Option tooltip(String t) {
		this.tooltip = t;
		return this;
	}

	/**
	 * The label text to render, resolved each frame. Defaults to the fixed {@link #label}; a
	 * {@link LabelOption} built from a supplier overrides this so its text can reflect live state (e.g.
	 * the update-check status) even though the panel is built once.
	 */
	public String displayLabel() {
		return label;
	}

	/** Whether the value equals its default (drives the per-row reset glyph). */
	public boolean isDefault() {
		return true;
	}

	/** Restores the default value. No-op for options that aren't editable. */
	public void reset() {
	}

	/** Whether this option can be edited/reset (false for links and static labels). */
	public boolean editable() {
		return true;
	}

	/** Whether this is a full-width informational label with no control (the {@link LabelOption}). */
	public boolean informational() {
		return false;
	}

	/** Compact current value for a read-only row / value readout. */
	public String displayValue() {
		return "";
	}
}
