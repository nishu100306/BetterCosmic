package dev.nishu.bettercosmic.prisons.gangping;

import dev.nishu.bettercosmic.prisons.BetterPrisons;
import dev.nishu.bettercosmic.prisons.client.BetterPrisonsClient;
import dev.nishu.bettercosmic.prisons.config.PrisonsConfig;
import dev.nishu.bettercosmic.prisons.waypoint.WaypointManager;
import dev.nishu.bettercosmic.shared.notification.Sounds;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Detects incoming gang ({@code [!]}) ping chat lines and feeds them to the {@link GangPingManager},
 * honoring the gang-chat ({@code [GC]}) / show-non-gang gates. Ported from the ping-parsing block of
 * BetterPrisons' {@code ChatReceiveMixin} (Yarn → Mojang); runs from
 * {@code ClientReceiveMessageEvents.GAME}. The raw text (with §-codes and the chat-channel tags) is
 * required, so parsing uses the unstripped message.
 */
public final class GangPingChatParser {

	private static final Pattern GANG_PING = Pattern.compile(
			"\\[!]\\s+(\\S+)\\s+has pinged at\\s+(-?\\d+)x\\s+(-?\\d+)y\\s+(-?\\d+)z\\s+(\\S+)\\s+\\|\\s+HP:?\\s+([\\d.]+)/([\\d.]+)\\s+\\|\\s+Facing:?\\s+(\\w+)");

	private GangPingChatParser() {}

	/** Handles one received game-chat line (raw, with §-codes and channel tags intact). */
	public static void handle(String rawText) {
		if (!rawText.contains("has pinged at")) {
			return;
		}
		PrisonsConfig c = BetterPrisonsClient.config;
		if (!c.gangPingEnabled) {
			return;
		}
		boolean showNonGang = c.gangPingShowNonGang;
		boolean fromGangChat = rawText.contains("[GC]");

		// Gang pings: accept from [GC], or any chat if show-non-gang is on.
		if (!rawText.contains("[!]") || !(fromGangChat || showNonGang)) {
			return;
		}
		Matcher matcher = GANG_PING.matcher(rawText);
		if (!matcher.find()) {
			return;
		}

		try {
			String playerName = matcher.group(1);
			int px = Integer.parseInt(matcher.group(2));
			int py = Integer.parseInt(matcher.group(3));
			int pz = Integer.parseInt(matcher.group(4));
			String world = matcher.group(5);
			float hp = Float.parseFloat(matcher.group(6));
			float maxHp = Float.parseFloat(matcher.group(7));
			String facing = matcher.group(8);
			BetterPrisonsClient.gangPingManager.onGangPingReceived(playerName, px, py, pz, world, hp, maxHp, facing);

			if (c.gangPingSoundEnabled && world.equals(WaypointManager.detectWorldKey())) {
				Sounds.play("note_pling", c.gangPingSoundVolume / 100.0f, 2.0f);
			}
		} catch (NumberFormatException e) {
			BetterPrisons.LOGGER.warn("Failed to parse gang ping: {}", rawText);
		}
	}
}
