package dev.nishu.bettercosmic.prisons.planet;

import dev.nishu.bettercosmic.shared.util.TabListUtil;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Detects which Cosmic Prisons "planet" the client is currently on. Each planet is a separate backend
 * server, but they are indistinguishable by dimension key ({@code minecraft:overworld} everywhere) and
 * by connection address (all behind the same proxy IP). They <em>are</em> distinguished by the tab-list
 * header, which carries a line like {@code "Aether Planet (46ms)"}.
 *
 * <p>This is the prisons-specific parser; the generic tab-header reading it builds on lives in the
 * shared {@link TabListUtil}. Detection is best-effort and read-only — it returns {@code null} whenever
 * the header is absent or does not match the expected format (e.g. not on Prisons, or in a menu/hub).
 */
public final class PlanetDetector {

	/**
	 * Matches the planet line and captures the planet name. Anchored per line, reluctant up to the
	 * standalone word {@code Planet} — the {@code \b} keeps it from matching a "Planets" count line.
	 * Example: {@code "Aether Planet (46ms)"} → {@code "Aether"}.
	 */
	private static final Pattern PLANET_LINE = Pattern.compile("^(.+?)\\s+Planet\\b.*$");

	private PlanetDetector() {}

	/**
	 * The current planet name (e.g. {@code "Aether"}), or {@code null} if it cannot be determined from
	 * the tab-list header. The header may be multi-line; each line is checked independently.
	 */
	public static String detect() {
		String header = TabListUtil.headerText();
		if (header.isEmpty()) {
			return null;
		}
		for (String line : header.split("\n")) {
			Matcher matcher = PLANET_LINE.matcher(line.trim());
			if (matcher.matches()) {
				return matcher.group(1).trim();
			}
		}
		return null;
	}
}
