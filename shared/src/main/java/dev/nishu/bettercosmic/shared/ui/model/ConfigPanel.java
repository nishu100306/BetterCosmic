package dev.nishu.bettercosmic.shared.ui.model;

import java.util.List;

/**
 * One feature's tile in the config grid. A <em>real</em> panel is clickable and opens a feature
 * popup built from its {@link OptionGroup}s; a <em>placeholder</em> panel renders as a locked "coming
 * soon" card for a feature that isn't built yet.
 */
public final class ConfigPanel {

	public final String id;
	public final String title;
	public final String description;
	public final PanelIcon icon;
	public final boolean placeholder;
	public final List<OptionGroup> groups;

	private ConfigPanel(String id, String title, String description, PanelIcon icon,
						boolean placeholder, List<OptionGroup> groups) {
		this.id = id;
		this.title = title;
		this.description = description;
		this.icon = icon;
		this.placeholder = placeholder;
		this.groups = groups;
	}

	/** A real, clickable feature panel with its option groups. */
	public static ConfigPanel of(String id, String title, String description, PanelIcon icon,
								 List<OptionGroup> groups) {
		return new ConfigPanel(id, title, description, icon, false, List.copyOf(groups));
	}

	/** A locked "coming soon" placeholder for a not-yet-built feature. */
	public static ConfigPanel placeholder(String id, String title, PanelIcon icon) {
		return new ConfigPanel(id, title, "Not yet available", icon, true, List.of());
	}

	/** Total number of options across all groups (shown in the card's meta line). */
	public int settingsCount() {
		int n = 0;
		for (OptionGroup g : groups) {
			n += g.options.size();
		}
		return n;
	}
}
