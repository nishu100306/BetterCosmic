package dev.nishu.bettercosmic.shared.util;

import dev.nishu.bettercosmic.shared.config.SharedConfig;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Number formatting shared across BetterCosmic HUDs and overlays: either compact suffixes
 * ({@code 1.2M}) or grouped digits ({@code 1,234,567}). Which one is used is a user preference on
 * {@link SharedConfig#useCommaFormatting}, so both mods format numbers consistently.
 *
 * <p>Ported/consolidated from the ad-hoc formatting scattered through BetterPrisons (Stats/Satchel
 * HUDs, EasyView) into one place.
 */
public final class NumberFormatUtil {

	private static final NumberFormat GROUPED = NumberFormat.getNumberInstance(Locale.US);

	/** Matches a formatted number with an optional K/M/B/T suffix, e.g. {@code 1,234}, {@code 1.2M}. */
	private static final Pattern NUMBER_PATTERN =
			Pattern.compile("([0-9,]+\\.?[0-9]*)\\s*([KMBT]?)", Pattern.CASE_INSENSITIVE);

	private NumberFormatUtil() {}

	/**
	 * Parses a human-formatted number back to a {@code long}: strips commas and applies a trailing
	 * K/M/B/T multiplier ({@code "1,234"} → 1234, {@code "1.2M"} → 1200000). Returns {@code 0} when no
	 * number can be parsed. The inverse of {@link #compact}/{@link #withCommas}. Ported from
	 * BetterPrisons' {@code EnchantParsing.parseFormattedNumber}.
	 */
	public static long parse(String text) {
		if (text == null || text.isEmpty()) {
			return 0;
		}
		Matcher matcher = NUMBER_PATTERN.matcher(text.trim());
		if (!matcher.find()) {
			return 0;
		}
		try {
			double base = Double.parseDouble(matcher.group(1).replace(",", ""));
			return switch (matcher.group(2).toUpperCase()) {
				case "K" -> (long) (base * 1_000L);
				case "M" -> (long) (base * 1_000_000L);
				case "B" -> (long) (base * 1_000_000_000L);
				case "T" -> (long) (base * 1_000_000_000_000L);
				default -> (long) base;
			};
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	/** Formats using the shared preference: commas when {@code useCommaFormatting} is on, else compact. */
	public static String format(long value) {
		return SharedConfig.get().useCommaFormatting ? withCommas(value) : compact(value);
	}

	/** Grouped digits, e.g. {@code 1,234,567}. */
	public static String withCommas(long value) {
		return GROUPED.format(value);
	}

	/**
	 * Compact suffix notation, e.g. {@code 1.2K}, {@code 3.4M}, {@code 5.6B}, {@code 7.8T}. Values
	 * below 1,000 are printed as-is. One decimal place, trailing {@code .0} trimmed. Negatives are
	 * handled symmetrically.
	 */
	public static String compact(long value) {
		if (value < 0) {
			return "-" + compact(-value);
		}
		if (value < 1_000L) {
			return Long.toString(value);
		}
		String suffix;
		double scaled;
		if (value < 1_000_000L) {
			scaled = value / 1_000d;
			suffix = "K";
		} else if (value < 1_000_000_000L) {
			scaled = value / 1_000_000d;
			suffix = "M";
		} else if (value < 1_000_000_000_000L) {
			scaled = value / 1_000_000_000d;
			suffix = "B";
		} else {
			scaled = value / 1_000_000_000_000d;
			suffix = "T";
		}
		String num = String.format(Locale.US, "%.1f", scaled);
		if (num.endsWith(".0")) {
			num = num.substring(0, num.length() - 2);
		}
		return num + suffix;
	}
}
