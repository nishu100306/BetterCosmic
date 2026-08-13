package dev.nishu.bettercosmic.sky.client;

import dev.nishu.bettercosmic.shared.config.BetterCosmicConfig;
import dev.nishu.bettercosmic.shared.config.SharedConfig;
import dev.nishu.bettercosmic.shared.easyview.EasyView;
import dev.nishu.bettercosmic.shared.ui.ConfigUi;
import dev.nishu.bettercosmic.shared.ui.model.ConfigPanel;
import dev.nishu.bettercosmic.shared.ui.model.ConfigRegistry;
import dev.nishu.bettercosmic.shared.ui.model.PanelIcon;
import dev.nishu.bettercosmic.sky.BetterSky;
import dev.nishu.bettercosmic.sky.config.SkyConfig;
import dev.nishu.bettercosmic.sky.feature.TrinketChargesProvider;
import net.fabricmc.api.ClientModInitializer;

public class BetterSkyClient implements ClientModInitializer {

	/** Shared config (config/bettercosmic/shared.json) — the same instance every mod uses. */
	public static SharedConfig sharedConfig;

	/** BetterSky's own config (config/bettercosmic/bettersky.json). */
	public static SkyConfig config;

	@Override
	public void onInitializeClient() {
		// Load the shared config and BetterSky's own config.
		sharedConfig = SharedConfig.get();
		config = BetterCosmicConfig.load(SkyConfig.class);

		// EasyView: show potion trinket charges in the slot corner.
		EasyView.register(new TrinketChargesProvider());

		// Config UI: brand the shared screen as "Sky" and register BetterSky's panels. Real panels
		// come first; the placeholders below are the roadmap chips shown as "Coming soon" cards
		// (their settings counts are provisional until the option model lands in Phase 2/6).
		ConfigUi.setSubtitle("Sky");
		ConfigRegistry.register(ConfigPanel.of("trinkets", "Trinkets",
				"Potion trinket charge overlay", PanelIcon.FLASK, 5));
		ConfigRegistry.register(ConfigPanel.of("general", "General",
				"Access, formatting & theme", PanelIcon.GEAR, 6));
		for (String soon : new String[] {
				"HUD", "Waypoints", "Notifications", "Chat",
				"Minimap", "Rendering", "Inventory", "Combat", "Events", "Misc" }) {
			ConfigRegistry.register(
					ConfigPanel.placeholder(soon.toLowerCase(), soon, PanelIcon.LOCK));
		}

		BetterSky.LOGGER.info("Loaded configs: {} and {}",
				sharedConfig.configPath(), config.configPath());
	}
}
