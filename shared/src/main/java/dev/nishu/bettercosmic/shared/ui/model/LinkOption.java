package dev.nishu.bettercosmic.shared.ui.model;

/** A URL link, opened (behind a confirm) by a {@code LinkButton}. Not editable/resettable. */
public final class LinkOption extends Option {

	public final String url;

	LinkOption(String label, String url) {
		super(label);
		this.url = url;
	}

	@Override
	public boolean editable() {
		return false;
	}
}
