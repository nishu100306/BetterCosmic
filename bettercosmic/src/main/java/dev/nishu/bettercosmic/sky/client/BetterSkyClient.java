package dev.nishu.bettercosmic.sky.client;

import dev.nishu.bettercosmic.shared.config.BetterCosmicConfig;
import dev.nishu.bettercosmic.shared.config.SharedConfig;
import dev.nishu.bettercosmic.shared.easyview.EasyView;
import dev.nishu.bettercosmic.shared.hud.HudRegistry;
import dev.nishu.bettercosmic.shared.server.Network;
import dev.nishu.bettercosmic.shared.ui.model.ConfigPanel;
import dev.nishu.bettercosmic.shared.ui.model.ConfigRegistry;
import dev.nishu.bettercosmic.shared.ui.model.OptionGroup;
import dev.nishu.bettercosmic.shared.ui.model.Options;
import dev.nishu.bettercosmic.shared.ui.model.PanelIcon;
import dev.nishu.bettercosmic.sky.BetterSky;
import dev.nishu.bettercosmic.sky.config.SkyConfig;
import dev.nishu.bettercosmic.sky.feature.TrinketChargesProvider;
import dev.nishu.bettercosmic.sky.hud.PlayerListHud;
import dev.nishu.bettercosmic.sky.hud.PlayerListHudPanel;
import net.fabricmc.api.ClientModInitializer;

import java.util.List;

public class BetterSkyClient implements ClientModInitializer {

	/** Shared config (config/bettercosmic/shared.json) — the same instance every mod uses. */
	public static SharedConfig sharedConfig;

	/** BetterSky's own config (config/bettercosmic/bettersky.json). */
	public static SkyConfig config;

	/** Compact alphabetical list of the other players online on the server. */
	public static PlayerListHud playerListHud;

	@Override
	public void onInitializeClient() {
		// Load the shared config and BetterSky's own config.
		sharedConfig = SharedConfig.get();
		config = BetterCosmicConfig.load(SkyConfig.class);

		// Code defaults, so a config "reset" restores these rather than the persisted values.
		SkyConfig def = new SkyConfig();

		// EasyView: show potion trinket charges in the slot corner (only on Cosmic Sky).
		EasyView.register(new TrinketChargesProvider(), Network.SKY);

		// HUD: compact alphabetical list of the other players online on the server (only on Cosmic Sky). The
		// shared HudRenderer (registered by BetterPrisonsClient, which always loads alongside Sky in
		// this build) ticks/draws every HudRegistry entry, gated by its owning network.
		playerListHud = new PlayerListHud();
		playerListHud.x = config.playerListHudX;
		playerListHud.y = config.playerListHudY;
		playerListHud.defaultX = def.playerListHudX;
		playerListHud.defaultY = def.playerListHudY;
		playerListHud.enabled = config.playerListHudEnabled;
		HudRegistry.register(playerListHud, () -> {
			config.playerListHudX = playerListHud.x;
			config.playerListHudY = playerListHud.y;
			config.save();
		}, Network.SKY);

		// Config UI: register BetterSky's own feature panels under the Sky profile. The shared General
		// panel (dev mode, formatting, theme) is registered by the shared library, and the header
		// profile selector labels the screen — so nothing is branded here.
		OptionGroup overlayGroup = new OptionGroup("Overlay", List.of(
				Options.toggle("Charge overlay", def.trinketChargesOverlay,
						() -> config.trinketChargesOverlay,
						v -> { config.trinketChargesOverlay = v; config.save(); })
						.tooltip("Show remaining uses on potion trinkets."),
				Options.slider("Scale", def.trinketChargesScale, 0.3, 1.5, 0.05,
						() -> config.trinketChargesScale,
						v -> { config.trinketChargesScale = v; config.save(); })
						.tooltip("Text size of the charge number."),
				Options.dropdown("Position", def.trinketChargesAnchor,
						List.of("Top-left", "Top-right", "Bottom-left", "Bottom-right", "Center"),
						() -> config.trinketChargesAnchor,
						v -> { config.trinketChargesAnchor = v; config.save(); })
		));
		OptionGroup colorGroup = new OptionGroup("Color", List.of(
				Options.dropdown("Source", def.trinketColorSource, List.of("Potion color", "Custom"),
						() -> config.trinketColorSource,
						v -> { config.trinketColorSource = v; config.save(); })
						.tooltip("Use the trinket's potion color, or your custom color."),
				Options.color("Custom color", def.trinketChargesColor,
						() -> config.trinketChargesColor,
						v -> { config.trinketChargesColor = v; config.save(); })
						.tooltip("Used only when Source is Custom.")
		));
		ConfigRegistry.register(ConfigPanel.of("trinkets", "Trinkets",
				"Potion trinket charge overlay", PanelIcon.POTION, List.of(overlayGroup, colorGroup)),
				Network.SKY);
		ConfigRegistry.register(PlayerListHudPanel.create(), Network.SKY);

		BetterSky.LOGGER.info("Loaded configs: {} and {}",
				sharedConfig.configPath(), config.configPath());
	}
}
