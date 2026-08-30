package dev.nishu.bettercosmic.shared.update;

/**
 * Immutable snapshot of the update check's result, held by {@link UpdateChecker} and read by the
 * notification surfaces (toast, config-screen row, ModMenu badge). Always describes a completed
 * comparison; {@code UpdateChecker.state()} is {@code null} until the first check finishes.
 */
public final class UpdateState {

	public final String installed;   // the running version
	public final String latest;      // newest published version (== installed when up to date)
	public final boolean available;  // a newer, compatible build exists
	public final String url;         // download URL for `latest` (phase 2), may be null
	public final String changelog;   // short text for `latest`, may be null
	public final boolean mandatory;  // `latest` is flagged critical

	private UpdateState(String installed, String latest, boolean available,
						String url, String changelog, boolean mandatory) {
		this.installed = installed;
		this.latest = latest;
		this.available = available;
		this.url = url;
		this.changelog = changelog;
		this.mandatory = mandatory;
	}

	/** No newer build (or the newest isn't for this Minecraft version). */
	static UpdateState upToDate(String installed) {
		return new UpdateState(installed, installed, false, null, null, false);
	}

	/** A newer, compatible build is available. */
	static UpdateState available(String installed, UpdateManifest m) {
		return new UpdateState(installed, m.latest, true, m.url, m.changelog, m.mandatory);
	}
}
