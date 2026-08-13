package dev.nishu.bettercosmic.shared.ui.model;

/**
 * One feature's tile in the config grid. A <em>real</em> panel is clickable and (from Phase 2) opens
 * a feature popup built from its option groups; a <em>placeholder</em> panel renders as a locked
 * "coming soon" card for a feature that isn't built yet.
 *
 * <p>Phase 1 carries only the card-facing shape (title, description, icon, settings count). The
 * option model ({@code OptionGroup}/{@code Option}) and the popup opener are added in Phase 2, at
 * which point {@link #settingsCount} becomes derived from the real groups rather than passed in.
 */
public final class ConfigPanel {

	public final String id;
	public final String title;
	public final String description;
	public final PanelIcon icon;
	public final boolean placeholder;

	/** Number shown in the card's "N settings" meta line. Provisional until options exist (Phase 2). */
	public final int settingsCount;

	private ConfigPanel(String id, String title, String description, PanelIcon icon,
						boolean placeholder, int settingsCount) {
		this.id = id;
		this.title = title;
		this.description = description;
		this.icon = icon;
		this.placeholder = placeholder;
		this.settingsCount = settingsCount;
	}

	/** A real, clickable feature panel. */
	public static ConfigPanel of(String id, String title, String description, PanelIcon icon, int settingsCount) {
		return new ConfigPanel(id, title, description, icon, false, settingsCount);
	}

	/** A locked "coming soon" placeholder for a not-yet-built feature. */
	public static ConfigPanel placeholder(String id, String title, PanelIcon icon) {
		return new ConfigPanel(id, title, "Not yet available", icon, true, 0);
	}
}
