package dev.nishu.bettercosmic.shared.ui.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The ordered set of {@link ConfigPanel}s the config screen shows. Each mod registers its panels at
 * client init (BetterSky its feature panels; shared its General panel later), in the order they
 * should appear in the grid. Registration order drives pagination and placeholder fill.
 */
public final class ConfigRegistry {

	private static final List<ConfigPanel> PANELS = new ArrayList<>();

	private ConfigRegistry() {}

	/** Appends a panel to the grid. Call at client init. */
	public static void register(ConfigPanel panel) {
		PANELS.add(panel);
	}

	/** The registered panels, in grid order (unmodifiable). */
	public static List<ConfigPanel> panels() {
		return Collections.unmodifiableList(PANELS);
	}

	/** Test/util hook — removes all registrations. */
	public static void clear() {
		PANELS.clear();
	}
}
