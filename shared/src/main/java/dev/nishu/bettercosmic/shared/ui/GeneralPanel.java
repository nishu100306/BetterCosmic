package dev.nishu.bettercosmic.shared.ui;

import dev.nishu.bettercosmic.shared.config.SharedConfig;
import dev.nishu.bettercosmic.shared.ui.core.Theme;
import dev.nishu.bettercosmic.shared.ui.model.ConfigPanel;
import dev.nishu.bettercosmic.shared.ui.model.Option;
import dev.nishu.bettercosmic.shared.ui.model.OptionGroup;
import dev.nishu.bettercosmic.shared.ui.model.Options;
import dev.nishu.bettercosmic.shared.ui.model.PanelIcon;

import java.util.List;

/**
 * The shared <b>General</b> config panel — dev mode, number formatting, and the compact UI theme —
 * bound to {@link SharedConfig}. Lives in {@code :shared} so both mods expose the same panel; each
 * mod registers it via {@link #create()}. Theme color options save and reload {@link Theme} so edits
 * repaint the UI immediately.
 */
public final class GeneralPanel {

	private GeneralPanel() {}

	public static ConfigPanel create() {
		SharedConfig c = SharedConfig.get();
		SharedConfig d = new SharedConfig(); // code defaults (so reset restores these, not persisted values)

		OptionGroup access = new OptionGroup("Access", List.of(
			Options.keybind("Open config", ConfigUi.openKeyMapping())
				.tooltip("Key that opens this menu. Esc while listening unbinds it.")
		));

		OptionGroup general = new OptionGroup("General", List.of(
			Options.toggle("Developer mode", d.developerMode,
				() -> c.developerMode,
				v -> { c.developerMode = v; c.save(); })
				.tooltip("Enables the shared dev commands (/bcitem)."),
			Options.toggle("Restrict to server", d.restrictFeaturesToServer,
				() -> c.restrictFeaturesToServer,
				v -> { c.restrictFeaturesToServer = v; c.save(); })
				.tooltip("Run each mod's features only on its own Cosmic server. Off = everywhere."),
			Options.toggle("Comma number format", d.useCommaFormatting,
				() -> c.useCommaFormatting,
				v -> { c.useCommaFormatting = v; c.save(); })
				.tooltip("1,234,567 instead of 1.2M.")
		));

		OptionGroup theme = new OptionGroup("Theme", List.of(
			themeColor("Accent", d.themeAccent, () -> c.themeAccent, v -> c.themeAccent = v),
			themeColor("Surface", d.themeSurface, () -> c.themeSurface, v -> c.themeSurface = v),
			themeColor("Surface hover", d.themeSurfaceHover, () -> c.themeSurfaceHover, v -> c.themeSurfaceHover = v),
			themeColor("Ground", d.themeGround, () -> c.themeGround, v -> c.themeGround = v),
			themeColor("Line", d.themeLine, () -> c.themeLine, v -> c.themeLine = v),
			themeColor("Text", d.themeText, () -> c.themeText, v -> c.themeText = v),
			themeColor("Muted", d.themeMuted, () -> c.themeMuted, v -> c.themeMuted = v),
			themeColor("Faint", d.themeFaint, () -> c.themeFaint, v -> c.themeFaint = v)
		));

		OptionGroup links = new OptionGroup("Links", List.of(
			// Placeholder link (opens behind the vanilla confirm) — swap for the real Discord/Modrinth.
			Options.link("Fabric", "https://fabricmc.net/")
		));

		return ConfigPanel.of("general", "General", "Access, formatting & theme",
			PanelIcon.GEAR, List.of(access, general, theme, links));
	}

	private static Option themeColor(String label, int def,
									 java.util.function.Supplier<Integer> get,
									 java.util.function.Consumer<Integer> set) {
		SharedConfig c = SharedConfig.get();
		return Options.color(label, def, get, v -> {
			set.accept(v);
			c.save();
			Theme.load(); // live repaint
		});
	}
}
