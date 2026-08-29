package dev.nishu.bettercosmic.prisons.chestsearch;

import dev.nishu.bettercosmic.shared.util.NumberFormatUtil;

import java.util.List;

/**
 * One row in the no-code filter sidebar: a filter type + user value + an auto-assigned highlight
 * color. Ported from BetterPrisons (Yarn → Mojang; number parsing via the shared NumberFormatUtil).
 *
 * <p>Types: NAME (name/lore contains value), SUCCESS_RATE / DESTROY_RATE (enchant-book % compare,
 * optional leading operator, default {@code >=} / {@code <=}), ENERGY_COST (book cost ceiling, value
 * accepts k/m/b/t suffixes).
 */
public class ChestSearchFilterRule {

	public enum Type {
		NAME("name"),
		SUCCESS_RATE("succ% >"),
		DESTROY_RATE("dest% <"),
		ENERGY_COST("nrg cost <");

		public final String label;

		Type(String label) {
			this.label = label;
		}

		public Type next() {
			Type[] vs = values();
			return vs[(ordinal() + 1) % vs.length];
		}
	}

	public Type type = Type.NAME;
	public String value = "";
	public int color = 0x8032CD32; // lime (auto-assigned on creation)

	public boolean isActive() {
		return value != null && !value.isEmpty();
	}

	/** @param book book attributes, or null if the stack is not an enchant book. */
	public boolean matches(String name, List<String> lore, BookAttributes book) {
		if (!isActive()) {
			return false;
		}
		switch (type) {
			case NAME: {
				String v = value.toLowerCase();
				if (name.toLowerCase().contains(v)) {
					return true;
				}
				for (String l : lore) {
					if (l.toLowerCase().contains(v)) {
						return true;
					}
				}
				return false;
			}
			case SUCCESS_RATE:
				return book != null && compare(value, book.successPercent, ">=");
			case DESTROY_RATE:
				return book != null && compare(value, book.destroyPercent, "<=");
			case ENERGY_COST: {
				if (book == null) {
					return false;
				}
				long ceiling = NumberFormatUtil.parse(value);
				if (ceiling <= 0) {
					return false;
				}
				return book.energyCost <= ceiling;
			}
			default:
				return false;
		}
	}

	private static boolean compare(String raw, double actual, String defaultOp) {
		raw = raw.trim();
		if (raw.isEmpty()) {
			return false;
		}
		String op = defaultOp;
		int idx = 0;
		if (raw.startsWith(">=") || raw.startsWith("<=")) {
			op = raw.substring(0, 2);
			idx = 2;
		} else if (raw.startsWith(">") || raw.startsWith("<") || raw.startsWith("=")) {
			op = raw.substring(0, 1);
			idx = 1;
		}
		String numStr = raw.substring(idx).trim().replace(",", "").replace("%", "");
		double target;
		try {
			target = Double.parseDouble(numStr);
		} catch (NumberFormatException e) {
			return false;
		}
		return switch (op) {
			case ">" -> actual > target;
			case "<" -> actual < target;
			case "<=" -> actual <= target;
			case "=" -> Math.abs(actual - target) < 0.5;
			default -> actual >= target;
		};
	}

	/** Snapshot of an enchant book's searchable numeric attributes. */
	public static final class BookAttributes {
		public final double successPercent; // 0–100
		public final double destroyPercent; // 0–100
		public final double energyCost;     // raw required energy

		public BookAttributes(double successPercent, double destroyPercent, double energyCost) {
			this.successPercent = successPercent;
			this.destroyPercent = destroyPercent;
			this.energyCost = energyCost;
		}
	}
}
