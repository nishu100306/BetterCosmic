package dev.nishu.bettercosmic.prisons.enchants;

import dev.nishu.bettercosmic.prisons.client.BetterPrisonsClient;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Chat-parsing helpers for enchant detection: pull a duration out of an activation message, test a
 * message against a pattern, and start a matching Cooldown-HUD entry. Ported from BetterPrisons'
 * {@code enchants/EnchantParsing} (its generic number parser now lives in the shared
 * {@code NumberFormatUtil}).
 */
public class EnchantParsing {

	private static final Pattern SECONDS_PATTERN =
			Pattern.compile("(\\d+)\\s*(?:second|sec|s)", Pattern.CASE_INSENSITIVE);

	/** Extracts a "<n> seconds" duration from a message, or 0 if absent. */
	public int parseSecondsFromMessage(String message, String pattern) {
		Matcher matcher = SECONDS_PATTERN.matcher(message);
		if (matcher.find()) {
			try {
				return Integer.parseInt(matcher.group(1));
			} catch (NumberFormatException ignored) {
			}
		}
		return 0;
	}

	/** Tests a message against a pattern: {@code regex:} prefix for regex, otherwise case-insensitive contains. */
	public boolean messageMatches(String message, String pattern) {
		if (pattern == null || pattern.isEmpty()) {
			return false;
		}
		if (pattern.startsWith("regex:")) {
			try {
				return message.matches(pattern.substring(6));
			} catch (Exception e) {
				return false;
			}
		}
		return message.toLowerCase().contains(pattern.toLowerCase());
	}

	/** Adds a cooldown to the Cooldown HUD for a detected enchant. */
	public void startCooldown(String enchantName, int durationSeconds) {
		BetterPrisonsClient.cooldownHud.addCooldown(enchantName, durationSeconds);
	}
}
