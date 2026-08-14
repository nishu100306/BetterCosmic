package dev.nishu.bettercosmic.shared.ui.model;

import java.util.List;

/**
 * A titled cluster of {@link Option}s inside a {@link ConfigPanel}'s popup — rendered as an
 * uppercase eyebrow header (the {@code GroupLabel}) followed by its option rows.
 */
public final class OptionGroup {

	public final String label;
	public final List<Option> options;

	public OptionGroup(String label, List<? extends Option> options) {
		this.label = label;
		this.options = List.copyOf(options); // widens a single-subtype list (e.g. all ColorOption) to List<Option>
	}
}
