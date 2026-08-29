package dev.nishu.bettercosmic.prisons.notification;

/**
 * BetterPrisons' notification catalog. Each type has a stable config id (the key into the
 * {@code notification*} maps on {@code PrisonsConfig}), a display name for the Notifications panel,
 * and defaults (enabled / sound / volume). This is prison content — the shared library only provides
 * the sound catalog and the {@code Notifier} engine.
 *
 * <p>Ported from BetterPrisons' {@code notification/NotificationType}.
 */
public enum NotificationType {
	MESSAGE("message", "Message Received", true, "anvil", 100),
	MENTION("mention", "Name Mentioned", false, "anvil", 100),
	POWERBALL("powerball", "Powerball Ready", true, "level_up", 100),
	SATCHEL_FULL("satchel_full", "Satchel Full", true, "anvil", 100);

	public final String id;
	public final String displayName;
	public final boolean defaultEnabled;
	public final String defaultSound;
	public final int defaultVolume;

	NotificationType(String id, String displayName, boolean defaultEnabled, String defaultSound, int defaultVolume) {
		this.id = id;
		this.displayName = displayName;
		this.defaultEnabled = defaultEnabled;
		this.defaultSound = defaultSound;
		this.defaultVolume = defaultVolume;
	}
}
