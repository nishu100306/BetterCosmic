package dev.nishu.bettercosmic.prisons.chestsearch;

import dev.nishu.bettercosmic.shared.chestsearch.FilterType;
import dev.nishu.bettercosmic.shared.chestsearch.QueryMatcher;
import dev.nishu.bettercosmic.shared.util.NumberFormatUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.List;
import java.util.function.BiPredicate;

/**
 * BetterPrisons' item-specific chest-search filter types and inline-query matcher, registered into
 * the shared {@link dev.nishu.bettercosmic.shared.chestsearch.ChestSearchRegistry}. Covers enchant
 * books (success %, destroy %, energy cost) and clue scrolls (assigned step number). The generic NAME
 * type and the search UI live in the shared library; the item NBT parsing stays here.
 */
public final class PrisonSearchTypes {

	private PrisonSearchTypes() {}

	/** The prison-specific filter types, in sidebar cycle order (after the shared NAME type). */
	public static List<FilterType> types() {
		return List.of(SUCCESS_RATE, DESTROY_RATE, ENERGY_COST, CLUE_STEP);
	}

	/** A bare-number query highlights clue scrolls whose current step number matches. */
	public static QueryMatcher clueQueryMatcher() {
		return (query, stack) -> {
			String q = query.toLowerCase();
			if (!isDigits(q)) {
				return false;
			}
			Integer step = ClueScrollProvider.displayedStep(stack);
			return step != null && Integer.toString(step).equals(q);
		};
	}

	private static final FilterType SUCCESS_RATE = type("succ", "succ% >", (value, stack) -> {
		BookAttributes b = bookAttributes(stack);
		return b != null && compare(value, b.successPercent, ">=");
	});

	private static final FilterType DESTROY_RATE = type("dest", "dest% <", (value, stack) -> {
		BookAttributes b = bookAttributes(stack);
		return b != null && compare(value, b.destroyPercent, "<=");
	});

	private static final FilterType ENERGY_COST = type("nrg", "nrg cost <", (value, stack) -> {
		BookAttributes b = bookAttributes(stack);
		if (b == null) {
			return false;
		}
		long ceiling = NumberFormatUtil.parse(value);
		return ceiling > 0 && b.energyCost <= ceiling;
	});

	private static final FilterType CLUE_STEP = type("clue", "clue #", (value, stack) -> {
		Integer step = ClueScrollProvider.displayedStep(stack);
		return step != null && compare(value, step, "=");
	});

	/** Adapts a (value, stack) predicate into a {@link FilterType} (these types ignore name/lore). */
	private static FilterType type(String id, String label, BiPredicate<String, ItemStack> pred) {
		return new FilterType() {
			@Override
			public String id() {
				return id;
			}

			@Override
			public String label() {
				return label;
			}

			@Override
			public boolean matches(String value, ItemStack stack, String name, List<String> lore) {
				return pred.test(value, stack);
			}
		};
	}

	/**
	 * Compares {@code actual} against a user value that may carry a leading operator
	 * ({@code >=}, {@code <=}, {@code >}, {@code <}, {@code =}); {@code defaultOp} applies when none is
	 * given. The value may include commas and a trailing {@code %}.
	 */
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

	private static boolean isDigits(String s) {
		if (s.isEmpty()) {
			return false;
		}
		for (int i = 0; i < s.length(); i++) {
			if (!Character.isDigit(s.charAt(i))) {
				return false;
			}
		}
		return true;
	}

	/** Enchant-book attributes (success %, destroy %, energy cost) from custom data, or null. */
	private static BookAttributes bookAttributes(ItemStack stack) {
		try {
			CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
			if (customData == null) {
				return null;
			}
			CompoundTag bukkit = customData.copyTag().getCompound("PublicBukkitValues").orElse(null);
			if (bukkit == null || bukkit.isEmpty()) {
				return null;
			}
			if (!"gear_enchant_book".equals(bukkit.getString("cosmicprisons:custom_item_id").orElse(""))) {
				return null;
			}
			double success = bukkit.getDouble("cosmicprisons:gear_enchant_success").orElse(0.0) * 100.0;
			double destroy = bukkit.getDouble("cosmicprisons:gear_enchant_destroy").orElse(0.0) * 100.0;
			double energy = bukkit.getDouble("cosmicprisons:gear_enchant_required").orElse(0.0);
			return new BookAttributes(success, destroy, energy);
		} catch (Exception e) {
			return null;
		}
	}

	/** Snapshot of an enchant book's searchable numeric attributes. */
	private record BookAttributes(double successPercent, double destroyPercent, double energyCost) {}
}
