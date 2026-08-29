package dev.nishu.bettercosmic.shared.ui.model;

import dev.nishu.bettercosmic.shared.server.Network;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The ordered set of {@link ConfigPanel}s the config screen shows. Each mod registers its panels at
 * client init, in the order they should appear in the grid. Registration order drives pagination and
 * placeholder fill.
 *
 * <p>Each panel is tagged with the {@link Network} it belongs to, so the config screen can scope the
 * grid to the profile the player is viewing: BetterPrisons panels register under {@link Network#PRISONS},
 * BetterSky panels under {@link Network#SKY}, and shared panels (the General panel) register with a
 * {@code null} network so they show under every profile. See {@link #panels(Network)}.
 */
public final class ConfigRegistry {

	/** A registered panel together with the network it is scoped to ({@code null} = global). */
	private record Entry(ConfigPanel panel, Network network) {}

	private static final List<Entry> ENTRIES = new ArrayList<>();

	private ConfigRegistry() {}

	/** Appends a global panel (shown under every profile). Call at client init. */
	public static void register(ConfigPanel panel) {
		register(panel, null);
	}

	/** Appends a panel scoped to {@code network} ({@code null} = global). Call at client init. */
	public static void register(ConfigPanel panel, Network network) {
		ENTRIES.add(new Entry(panel, network));
	}

	/** Every registered panel, in grid order, ignoring scope (unmodifiable). */
	public static List<ConfigPanel> panels() {
		List<ConfigPanel> out = new ArrayList<>(ENTRIES.size());
		for (Entry e : ENTRIES) {
			out.add(e.panel());
		}
		return Collections.unmodifiableList(out);
	}

	/**
	 * The panels visible under the {@code selected} profile, in grid order: global panels (network
	 * {@code null}) plus those scoped to {@code selected}. A {@code null} argument returns global-only.
	 */
	public static List<ConfigPanel> panels(Network selected) {
		List<ConfigPanel> out = new ArrayList<>(ENTRIES.size());
		for (Entry e : ENTRIES) {
			if (e.network() == null || e.network() == selected) {
				out.add(e.panel());
			}
		}
		return Collections.unmodifiableList(out);
	}

	/** Test/util hook — removes all registrations. */
	public static void clear() {
		ENTRIES.clear();
	}
}
