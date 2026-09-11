package dev.nishu.bettercosmic.shared.chestsearch;

import dev.nishu.bettercosmic.shared.config.SharedConfig;
import dev.nishu.bettercosmic.shared.server.Network;
import dev.nishu.bettercosmic.shared.server.ServerContext;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;

/**
 * Central registry for the shared chest-search feature. Each mod registers its network with an
 * enable flag, any item-specific {@link FilterType}s, and any extra {@link QueryMatcher}s; the shared
 * search bar, filter sidebar, and slot highlight then run for whichever network is active. The
 * built-in {@link #NAME} type (name/lore contains) is available everywhere and prepended to every
 * mod's type list.
 *
 * <p>The "active" network is the connected/overridden one when it has a registration; when the global
 * restrict toggle is off (features-everywhere) and no recognised network is connected, the first
 * enabled registration wins, so the feature still works in that legacy mode.
 */
public final class ChestSearchRegistry {

	/** Built-in filter: item display name or any lore line contains the value (case-insensitive). */
	public static final FilterType NAME = new FilterType() {
		@Override
		public String id() {
			return "name";
		}

		@Override
		public String label() {
			return "name";
		}

		@Override
		public boolean matches(String value, ItemStack stack, String name, List<String> lore) {
			String v = value.toLowerCase();
			if (name.toLowerCase().contains(v)) {
				return true;
			}
			for (String line : lore) {
				if (line.toLowerCase().contains(v)) {
					return true;
				}
			}
			return false;
		}
	};

	private static final class Entry {
		final BooleanSupplier enabled;
		final List<FilterType> types;          // NAME first, then the mod's extras
		final List<QueryMatcher> queryMatchers;

		Entry(BooleanSupplier enabled, List<FilterType> extras, List<QueryMatcher> queryMatchers) {
			this.enabled = enabled;
			List<FilterType> t = new ArrayList<>();
			t.add(NAME);
			if (extras != null) {
				t.addAll(extras);
			}
			this.types = List.copyOf(t);
			this.queryMatchers = queryMatchers == null ? List.of() : List.copyOf(queryMatchers);
		}
	}

	private static final Map<Network, Entry> ENTRIES = new LinkedHashMap<>();

	private ChestSearchRegistry() {}

	/**
	 * Registers chest search for {@code network}.
	 *
	 * @param enabled       reads the owning mod's "chest search enabled" config flag
	 * @param extraTypes    item-specific filter types (NAME is added automatically); may be empty
	 * @param queryMatchers extra inline-query matches beyond name/lore contains; may be empty
	 */
	public static void register(Network network, BooleanSupplier enabled,
			List<FilterType> extraTypes, List<QueryMatcher> queryMatchers) {
		ENTRIES.put(network, new Entry(enabled, extraTypes, queryMatchers));
	}

	private static Entry activeEntry() {
		Network net = ServerContext.hasOverride() ? ServerContext.override() : ServerContext.detected();
		if (net != null) {
			Entry e = ENTRIES.get(net);
			if (e != null) {
				return e;
			}
		}
		// Features-everywhere legacy mode with no recognised network: first enabled registration.
		if (!SharedConfig.get().restrictFeaturesToServer) {
			for (Entry e : ENTRIES.values()) {
				if (e.enabled.getAsBoolean()) {
					return e;
				}
			}
		}
		return null;
	}

	/** Whether chest search is enabled for the active network right now. */
	public static boolean enabled() {
		Entry e = activeEntry();
		return e != null && e.enabled.getAsBoolean();
	}

	/** Filter types selectable on the active network (NAME first). Falls back to just NAME. */
	public static List<FilterType> availableTypes() {
		Entry e = activeEntry();
		return e == null ? List.of(NAME) : e.types;
	}

	/** The default type for a new rule. */
	public static FilterType defaultType() {
		return NAME;
	}

	/** The next type when the sidebar's type button is clicked (wraps within the available types). */
	public static FilterType nextType(FilterType current) {
		List<FilterType> types = availableTypes();
		int idx = types.indexOf(current);
		return types.get((idx + 1) % types.size());
	}

	/** Inline-query match: name/lore contains, or any active-network query matcher. */
	public static boolean simpleQueryMatches(String query, ItemStack stack, String name, List<String> lore) {
		if (NAME.matches(query, stack, name, lore)) {
			return true;
		}
		Entry e = activeEntry();
		if (e != null) {
			for (QueryMatcher qm : e.queryMatchers) {
				if (qm.matches(query, stack)) {
					return true;
				}
			}
		}
		return false;
	}
}
