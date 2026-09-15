package dev.nishu.bettercosmic.sky.feature;

import dev.nishu.bettercosmic.sky.client.BetterSkyClient;
import dev.nishu.bettercosmic.sky.config.SkyConfig;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Adds a localized expiry line to Cosmic Sky adventure quest-point notes. The server prints the expiry
 * in US-Central time (e.g. "Expires at Sep 25th 2:00PM CST"); this reads the exact epoch from NBT
 * ({@code custom_data.expiration}) instead of parsing that text, and appends a time-remaining line plus
 * the expiry in the user's own timezone. Modeled on BetterPrisons' {@code misc/GangPointTooltip}.
 */
public final class QuestPointTooltip {

	private static final DateTimeFormatter LOCAL_FORMAT =
			DateTimeFormatter.ofPattern("MMM d, h:mm a zzz", Locale.US);

	private QuestPointTooltip() {}

	public static void append(ItemStack stack, List<Component> lines) {
		try {
			SkyConfig cfg = BetterSkyClient.config;
			if (cfg == null || !cfg.questPointExpiryEnabled || stack == null || stack.isEmpty()) {
				return;
			}
			CustomData data = stack.get(DataComponents.CUSTOM_DATA);
			if (data == null) {
				return;
			}
			CompoundTag nbt = data.copyTag();
			if (!QuestPointProvider.isQuestPointNote(nbt.getStringOr("cosmicItem", ""))) {
				return;
			}
			long expirationMs = nbt.getLongOr("expiration", 0L);
			if (expirationMs <= 0) {
				return;
			}

			ZonedDateTime local = Instant.ofEpochMilli(expirationMs).atZone(ZoneId.systemDefault());
			int rgb = cfg.questPointExpiryColor & 0xFFFFFF;
			Style style = Style.EMPTY.withColor(TextColor.fromRgb(rgb)).withItalic(false);

			long remainingMs = expirationMs - System.currentTimeMillis();
			String remaining = remainingMs <= 0 ? "Expired" : formatDuration(remainingMs);
			lines.add(Component.literal("[BS] Expires in: " + remaining).setStyle(style));
			lines.add(Component.literal("[BS] Local: " + local.format(LOCAL_FORMAT)).setStyle(style));
		} catch (Exception e) {
			// Tooltips must never crash the game.
		}
	}

	private static String formatDuration(long ms) {
		long totalSecs = ms / 1000;
		long days = totalSecs / 86400;
		long hours = (totalSecs % 86400) / 3600;
		long minutes = (totalSecs % 3600) / 60;
		long seconds = totalSecs % 60;

		StringBuilder sb = new StringBuilder();
		if (days > 0) {
			sb.append(days).append("d ");
		}
		if (hours > 0 || days > 0) {
			sb.append(hours).append("h ");
		}
		if (minutes > 0 || hours > 0 || days > 0) {
			sb.append(minutes).append("m ");
		}
		sb.append(seconds).append("s");
		return sb.toString();
	}
}
