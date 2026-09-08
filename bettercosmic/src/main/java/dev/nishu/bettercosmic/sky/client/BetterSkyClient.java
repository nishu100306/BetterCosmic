package dev.nishu.bettercosmic.sky.client;

import dev.nishu.bettercosmic.shared.config.BetterCosmicConfig;
import dev.nishu.bettercosmic.shared.config.SharedConfig;
import dev.nishu.bettercosmic.shared.easyview.EasyView;
import dev.nishu.bettercosmic.shared.server.Network;
import dev.nishu.bettercosmic.shared.ui.model.ConfigPanel;
import dev.nishu.bettercosmic.shared.ui.model.ConfigRegistry;
import dev.nishu.bettercosmic.shared.ui.model.OptionGroup;
import dev.nishu.bettercosmic.shared.ui.model.Options;
import dev.nishu.bettercosmic.shared.ui.model.PanelIcon;
import dev.nishu.bettercosmic.sky.BetterSky;
import dev.nishu.bettercosmic.sky.config.SkyConfig;
import dev.nishu.bettercosmic.sky.feature.TrinketChargesProvider;
import net.fabricmc.api.ClientModInitializer;

import java.util.List;

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

		// EasyView: show potion trinket charges in the slot corner (only on Cosmic Sky).
		EasyView.register(new TrinketChargesProvider(), Network.SKY);

		// Config UI: register BetterSky's own feature panel under the Sky profile. The shared General
		// panel (dev mode, formatting, theme) is registered by the shared library, and the header
		// profile selector labels the screen — so nothing is branded here.
		SkyConfig def = new SkyConfig(); // code defaults (so reset restores these, not the persisted values)
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

		BetterSky.LOGGER.info("Loaded configs: {} and {}",
				sharedConfig.configPath(), config.configPath());
	}
}
