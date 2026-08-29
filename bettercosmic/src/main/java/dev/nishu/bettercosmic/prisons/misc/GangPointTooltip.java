package dev.nishu.bettercosmic.prisons.misc;

import dev.nishu.bettercosmic.prisons.client.BetterPrisonsClient;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Adds a localized expiry countdown to Cosmic Prisons gang-point notes. The server lore prints expiry
 * in US-Eastern time (e.g. "Expires at May. 15, 5:58 AM EDT"); this parses it and appends a
 * time-remaining line plus the expiry localized to the user's timezone. Ported from BetterPrisons'
 * {@code misc/GangPointTooltip} (Yarn → Mojang: {@code LoreComponent} → {@code ItemLore},
 * {@code NbtComponent.copyNbt} → {@code CustomData.copyTag}).
 */
public final class GangPointTooltip {

	private static final Pattern EXPIRY_PATTERN = Pattern.compile(
			"Expires at\\s+(\\w+)\\.?\\s+(\\d+),\\s+(\\d+):(\\d+)\\s+(AM|PM)\\s+([A-Za-z]+)");

	private static final Map<String, Integer> MONTHS = new HashMap<>();

	static {
		MONTHS.put("Jan", 1); MONTHS.put("Feb", 2); MONTHS.put("Mar", 3); MONTHS.put("Apr", 4);
		MONTHS.put("May", 5); MONTHS.put("Jun", 6); MONTHS.put("Jul", 7); MONTHS.put("Aug", 8);
		MONTHS.put("Sep", 9); MONTHS.put("Oct", 10); MONTHS.put("Nov", 11); MONTHS.put("Dec", 12);
	}

	private static final DateTimeFormatter LOCAL_FORMAT =
			DateTimeFormatter.ofPattern("MMM d, h:mm a zzz", Locale.US);

	private GangPointTooltip() {}

	public static void append(ItemStack stack, List<Component> lines) {
		try {
			if (!BetterPrisonsClient.config.gangPointExpiryEnabled) {
				return;
			}
			if (stack == null || stack.isEmpty()) {
				return;
			}
			CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
			if (customData == null) {
				return;
			}
			CompoundTag bukkit = customData.copyTag().getCompound("PublicBukkitValues").orElse(null);
			if (bukkit == null || bukkit.isEmpty()) {
				return;
			}
			if (!"gang_point_note".equals(bukkit.getStringOr("cosmicprisons:custom_item_id", ""))) {
				return;
			}

			Instant expiry = findExpiryInLore(stack);
			if (expiry == null) {
				return;
			}
			ZonedDateTime expiryLocal = expiry.atZone(ZoneId.systemDefault());
			int rgb = BetterPrisonsClient.config.gangPointExpiryColor & 0xFFFFFF;
			Style style = Style.EMPTY.withColor(TextColor.fromRgb(rgb)).withItalic(false);

			long remainingMs = expiry.toEpochMilli() - System.currentTimeMillis();
			String remainingStr = remainingMs <= 0 ? "Expired" : formatDuration(remainingMs);
			lines.add(Component.literal("[BP] Expires in: " + remainingStr).setStyle(style));
			lines.add(Component.literal("[BP] Local: " + expiryLocal.format(LOCAL_FORMAT)).setStyle(style));
		} catch (Exception e) {
			// Tooltips must never crash the game.
		}
	}

	private static Instant findExpiryInLore(ItemStack stack) {
		ItemLore lore = stack.get(DataComponents.LORE);
		if (lore == null) {
			return null;
		}
		for (Component line : lore.lines()) {
			Matcher m = EXPIRY_PATTERN.matcher(line.getString());
			if (m.find()) {
				return parseExpiry(m);
			}
		}
		return null;
	}

	private static Instant parseExpiry(Matcher m) {
		String monthStr = m.group(1);
		int day, hour, minute;
		try {
			day = Integer.parseInt(m.group(2));
			hour = Integer.parseInt(m.group(3));
			minute = Integer.parseInt(m.group(4));
		} catch (NumberFormatException e) {
			return null;
		}
		String ampm = m.group(5);
		String tzAbbrev = m.group(6).toUpperCase();

		Integer month = MONTHS.get(monthStr);
		if (month == null) {
			return null;
		}
		if ("PM".equals(ampm) && hour < 12) {
			hour += 12;
		} else if ("AM".equals(ampm) && hour == 12) {
			hour = 0;
		}

		ZoneId zone;
		switch (tzAbbrev) {
			case "EDT":
			case "EST": zone = ZoneId.of("America/New_York"); break;
			case "PDT":
			case "PST": zone = ZoneId.of("America/Los_Angeles"); break;
			case "CDT":
			case "CST": zone = ZoneId.of("America/Chicago"); break;
			case "MDT":
			case "MST": zone = ZoneId.of("America/Denver"); break;
			case "UTC":
			case "GMT": zone = ZoneId.of("UTC"); break;
			default: zone = ZoneId.of("America/New_York"); break;
		}

		int year = LocalDate.now(zone).getYear();
		ZonedDateTime candidate;
		try {
			candidate = ZonedDateTime.of(year, month, day, hour, minute, 0, 0, zone);
		} catch (Exception e) {
			return null;
		}
		// If the parsed date is more than a day in the past, assume it's actually next year.
		if (candidate.toInstant().isBefore(Instant.now().minusSeconds(86400))) {
			candidate = candidate.plusYears(1);
		}
		return candidate.toInstant();
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
