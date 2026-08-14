package dev.nishu.bettercosmic.shared.ui.model;

/** A static, full-width informational label row with no control. */
public final class LabelOption extends Option {

	LabelOption(String text) {
		super(text);
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
