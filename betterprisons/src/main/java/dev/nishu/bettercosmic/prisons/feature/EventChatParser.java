package dev.nishu.bettercosmic.prisons.feature;

import dev.nishu.bettercosmic.prisons.BetterPrisons;
import dev.nishu.bettercosmic.prisons.hud.EventsHud;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Detects Cosmic Prisons world-event chat lines and feeds them to the {@link EventsHud}. Meteors and
 * meteorite showers are two-line announcements (a header line, then a coordinates line), so the
 * previous stripped line is remembered as listener state. Ported from BetterPrisons'
 * {@code ChatReceiveMixin} event-parsing (Yarn → Mojang); it now runs from
 * {@code ClientReceiveMessageEvents.GAME} instead of a mixin. Gang-ping parsing stays with that
 * feature.
 */
public final class EventChatParser {

	private static final Pattern MERCHANT_SPAWN_PATTERN = Pattern.compile(
			"\\(!\\) A (\\w+) Ore Merchant traveled to (-?\\d+)x, (-?\\d+)y, (-?\\d+)z");
	private static final Pattern MERCHANT_SLAIN_PATTERN = Pattern.compile(
			"\\(!\\) A (\\w+) Ore Merchant has been slain by .+ at (-?\\d+)x, (-?\\d+)y, (-?\\d+)z");
	private static final Pattern BANDIT_RUSH_PATTERN = Pattern.compile(
			"(\\w+) BANDIT RUSH has spawned at (-?\\d+), (-?\\d+), (-?\\d+)");
	private static final Pattern BANDIT_RUSH_WON_PATTERN = Pattern.compile(
			"won the\\s+(\\w+)\\s+BANDIT RUSH\\s+at\\s+(-?\\d+),\\s*(-?\\d+),\\s*(-?\\d+)");

	private String previousMessage = "";

	/** Handles one received game-chat line (raw, with §-codes intact). */
	public void handle(EventsHud eventsHud, String rawText) {
		String strippedText = rawText.replaceAll("§.", "");

		// --- Merchant spawn ---
		Matcher merchantSpawn = MERCHANT_SPAWN_PATTERN.matcher(strippedText);
		if (merchantSpawn.find()) {
			try {
				eventsHud.onMerchantSpawned(merchantSpawn.group(1),
						Integer.parseInt(merchantSpawn.group(2)),
						Integer.parseInt(merchantSpawn.group(3)),
						Integer.parseInt(merchantSpawn.group(4)));
			} catch (NumberFormatException e) {
				BetterPrisons.LOGGER.warn("Failed to parse merchant spawn coordinates: {}", strippedText);
			}
		}

		// --- Merchant slain ---
		Matcher merchantSlain = MERCHANT_SLAIN_PATTERN.matcher(strippedText);
		if (merchantSlain.find()) {
			try {
				eventsHud.onMerchantSlain(merchantSlain.group(1),
						Integer.parseInt(merchantSlain.group(2)),
						Integer.parseInt(merchantSlain.group(3)),
						Integer.parseInt(merchantSlain.group(4)));
			} catch (NumberFormatException e) {
				BetterPrisons.LOGGER.warn("Failed to parse merchant slain coordinates: {}", strippedText);
			}
		}

		// --- Meteor falling (coords on this line, header on the previous) ---
		if (previousMessage.contains("A METEOR IS FALLING FROM THE SKY")) {
			eventsHud.onMeteorFalling(strippedText, EventsHud.MeteorType.NATURAL);
		} else if (previousMessage.contains("A METEOR WILL CRASH")) {
			EventsHud.MeteorType type = strippedText.contains("Summoned by")
					? EventsHud.MeteorType.SUMMONED : EventsHud.MeteorType.NATURAL;
			eventsHud.onMeteorFalling(strippedText, type);
		} else if (previousMessage.startsWith("(!) A meteor is falling from the sky at:")) {
			eventsHud.onMeteorFalling(strippedText, EventsHud.MeteorType.NATURAL);
		} else if (previousMessage.startsWith("(!) A meteor summoned by")
				&& previousMessage.contains("is falling from the sky at:")) {
			eventsHud.onMeteorFalling(strippedText, EventsHud.MeteorType.SUMMONED);
		}

		// --- Meteor crashed ---
		if (previousMessage.contains("(!) A meteor has crashed at:")
				|| previousMessage.contains("A METEOR HAS CRASHED")) {
			eventsHud.onMeteorCrashed(strippedText);
		}

		// --- Meteorite shower (coords on this line, header on the previous) ---
		if (previousMessage.contains("METEORITE SHOWER WILL CRASH")) {
			eventsHud.onMeteoriteShower(strippedText, false);
		} else if (previousMessage.contains("METEORITE SHOWER HAS CRASHED")) {
			eventsHud.onMeteoriteShower(strippedText, true);
		}

		// --- Bandit rush spawn ---
		Matcher banditRush = BANDIT_RUSH_PATTERN.matcher(strippedText);
		if (banditRush.find()) {
			try {
				eventsHud.onBanditRushSpawned(banditRush.group(1),
						Integer.parseInt(banditRush.group(2)),
						Integer.parseInt(banditRush.group(3)),
						Integer.parseInt(banditRush.group(4)));
			} catch (NumberFormatException e) {
				BetterPrisons.LOGGER.warn("Failed to parse bandit rush coordinates: {}", strippedText);
			}
		}

		// --- Bandit rush won ---
		if (strippedText.contains("BANDIT RUSH") && strippedText.contains("won the")) {
			Matcher banditWon = BANDIT_RUSH_WON_PATTERN.matcher(strippedText);
			if (banditWon.find()) {
				try {
					eventsHud.onBanditRushWon(banditWon.group(1),
							Integer.parseInt(banditWon.group(2)),
							Integer.parseInt(banditWon.group(4)));
				} catch (NumberFormatException e) {
					BetterPrisons.LOGGER.warn("Failed to parse bandit rush won coordinates: {}", strippedText);
				}
			}
		}

		previousMessage = strippedText;
	}
}
