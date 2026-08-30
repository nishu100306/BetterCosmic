package dev.nishu.bettercosmic.shared.ui.model;

import java.util.function.Supplier;

/**
 * A static, full-width informational label row with no control. The text may be fixed or, when built
 * from a {@link Supplier}, resolved live each frame (see {@link #displayLabel()}) so a row can reflect
 * changing state — e.g. the auto-updater's status — without rebuilding the panel.
 */
public final class LabelOption extends Option {

	private final Supplier<String> dynamic; // null for a fixed label

	LabelOption(String text) {
		super(text);
		this.dynamic = null;
	}

	LabelOption(Supplier<String> text) {
		super(""); // fixed label unused; resolved via displayLabel()
		this.dynamic = text;
	}

	@Override
	public String displayLabel() {
		if (dynamic != null) {
			String s = dynamic.get();
			return s != null ? s : "";
		}
		return label;
	}

	@Override
	public boolean editable() {
		return false;
	}

	@Override
	public boolean informational() {
		return true;
	}
}
