package dev.nishu.bettercosmic.prisons.notification;

import net.minecraft.client.Minecraft;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Detects incoming private messages and name mentions in chat and fires the corresponding
 * {@link Notifications} ({@link NotificationType#MESSAGE} / {@link NotificationType#MENTION}). Ported
 * from the {@code checkPrivateMessage}/{@code checkUsernameMention} logic of BetterPrisons'
 * {@code ChatReceiveMixin} (Yarn → Mojang: {@code getSession().getUsername()} → {@code getUser().getName()});
 * runs from {@code ClientReceiveMessageEvents.GAME}. Parses the §-stripped message.
 */
public final class MessageNotifications {

	/** A private message like "[realm] [username to me] ..." or "... -> me ...". */
	private static final Pattern PM_PATTERN =
			Pattern.compile("\\[.*?]\\s*\\[.+?(?:\\s*->\\s*|\\s+to\\s+)me].*");

	private MessageNotifications() {}

	/** Handles one received game-chat line (raw, with §-codes intact). */
	public static void handle(String rawText) {
		String text = rawText.replaceAll("§.", "");
		checkPrivateMessage(text);
		checkUsernameMention(text);
	}

	private static void checkPrivateMessage(String text) {
		if (!Notifications.isEnabled(NotificationType.MESSAGE)) {
			return;
		}
		if (PM_PATTERN.matcher(text).matches()) {
			Notifications.trigger(NotificationType.MESSAGE);
		}
	}

	private static void checkUsernameMention(String text) {
		if (!Notifications.isEnabled(NotificationType.MENTION)) {
			return;
		}
		Minecraft client = Minecraft.getInstance();
		if (client.player == null || client.getUser() == null) {
			return;
		}
		// Private messages already notify separately — don't double-notify.
		if (PM_PATTERN.matcher(text).matches()) {
			return;
		}
		String username = client.getUser().getName();
		if (username == null || username.isEmpty()) {
			return;
		}
		// Whole-word, case-insensitive match of the username.
		Matcher matcher = Pattern.compile("(?i)(?<![\\w])" + Pattern.quote(username) + "(?![\\w])").matcher(text);
		if (!matcher.find()) {
			return;
		}
		// Avoid self-pinging on your own chat: if the only match is in the sender slot (before the
		// first ':'), require another occurrence in the message body.
		int colon = text.indexOf(':');
		if (colon >= 0 && matcher.start() < colon && !matcher.find(colon)) {
			return;
		}
		Notifications.trigger(NotificationType.MENTION);
	}
}
