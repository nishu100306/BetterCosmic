package dev.nishu.bettercosmic.sky.client;

import dev.nishu.bettercosmic.shared.config.BetterCosmicConfig;
import dev.nishu.bettercosmic.shared.config.SharedConfig;
import dev.nishu.bettercosmic.shared.chestsearch.ChestSearchRegistry;
import dev.nishu.bettercosmic.shared.easyview.EasyView;
import dev.nishu.bettercosmic.shared.hud.HudRegistry;
import dev.nishu.bettercosmic.shared.render.FloatingTextRenderer;
import dev.nishu.bettercosmic.shared.server.Network;
import dev.nishu.bettercosmic.shared.server.ServerContext;
import dev.nishu.bettercosmic.shared.ui.model.ConfigPanel;
import dev.nishu.bettercosmic.shared.ui.model.ConfigRegistry;
import dev.nishu.bettercosmic.shared.ui.model.OptionGroup;
import dev.nishu.bettercosmic.shared.ui.model.Options;
import dev.nishu.bettercosmic.shared.ui.model.PanelIcon;
import dev.nishu.bettercosmic.sky.BetterSky;
import dev.nishu.bettercosmic.sky.config.SkyConfig;
import dev.nishu.bettercosmic.sky.feature.AutoTrade;
import dev.nishu.bettercosmic.sky.feature.DamageIndicators;
import dev.nishu.bettercosmic.sky.feature.PetCooldownProvider;
import dev.nishu.bettercosmic.sky.feature.TrinketChargesProvider;
import dev.nishu.bettercosmic.sky.hud.PlayerListHud;
import dev.nishu.bettercosmic.sky.hud.PlayerListHudPanel;
import dev.nishu.bettercosmic.sky.hud.TrackerHud;
import dev.nishu.bettercosmic.sky.hud.TrackerHudPanel;
import dev.nishu.bettercosmic.sky.ui.SkyOptions;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.gui.screens.ChatScreen;

import java.util.List;

public class BetterSkyClient implements ClientModInitializer {

	/** Shared config (config/bettercosmic/shared.json) — the same instance every mod uses. */
	public static SharedConfig sharedConfig;

	/** BetterSky's own config (config/bettercosmic/bettersky.json). */
	public static SkyConfig config;

	/** Compact alphabetical list of the other players online on the server. */
	public static PlayerListHud playerListHud;

	/** Session tracker of Island Quests completed, by tier, with a timer and pause/reset buttons. */
	public static TrackerHud trackerHud;

	@Override
	public void onInitializeClient() {
		// Load the shared config and BetterSky's own config.
		sharedConfig = SharedConfig.get();
		config = BetterCosmicConfig.load(SkyConfig.class);

		// Code defaults, so a config "reset" restores these rather than the persisted values.
		SkyConfig def = new SkyConfig();

		// EasyView: show potion trinket charges in the slot corner (only on Cosmic Sky).
		EasyView.register(new TrinketChargesProvider(), Network.SKY);

		// EasyView: centered cooldown / active-effect timer on inventory pets (only on Cosmic Sky).
		EasyView.register(new PetCooldownProvider(), Network.SKY);

		// Auto-trade: shift-right-click another player to send /trade <name> (only on Cosmic Sky).
		AutoTrade.register();

		// Damage indicators: floating red/green combat numbers over nearby entities (only on Cosmic Sky).
		// FloatingTextRenderer.init() is idempotent (BetterPrisonsClient also calls it in this build).
		FloatingTextRenderer.init();
		DamageIndicators.register();

		// Chest search: reuse the shared search bar + filter sidebar. Sky adds no item-specific filter
		// types (no enchant books / clue scrolls), so just NAME rules + the text query. The shared
		// library owns the tint provider + UI mixin.
		ChestSearchRegistry.register(Network.SKY, () -> config.chestSearchEnabled, List.of(), List.of());

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

		// HUD: session tracker of completed Island Quests by tier, with a timer and Pause/Reset buttons.
		trackerHud = new TrackerHud();
		trackerHud.x = config.trackerHudX;
		trackerHud.y = config.trackerHudY;
		trackerHud.defaultX = def.trackerHudX;
		trackerHud.defaultY = def.trackerHudY;
		trackerHud.enabled = config.trackerHudEnabled;
		HudRegistry.register(trackerHud, () -> {
			config.trackerHudX = trackerHud.x;
			config.trackerHudY = trackerHud.y;
			config.save();
		}, Network.SKY);

		// Feed the tracker from chat: the server's "… Quest COMPLETE: <Tier> …" completion message.
		ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
			if (!overlay && ServerContext.isActive(Network.SKY)) {
				trackerHud.onChatMessage(message.getString());
			}
		});

		// The tracker's Pause/Reset buttons can't be clicked while the cursor is grabbed. The HUD stays
		// visible behind the chat screen, so route clicks to it while chat is open (the shared toast
		// system solves over-gameplay clicks the same way).
		ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
			if (!(screen instanceof ChatScreen)) {
				return;
			}
			ScreenMouseEvents.afterMouseClick(screen).register((scr, ctx, consumed) ->
					ctx.button() == 0 && ServerContext.isActive(Network.SKY)
							&& trackerHud.enabled && trackerHud.handleClick(ctx.x(), ctx.y()));
		});

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

		OptionGroup petGroup = new OptionGroup("Cooldown timer", List.of(
				Options.toggle("Cooldown overlay", def.petCooldownOverlay,
						() -> config.petCooldownOverlay,
						v -> { config.petCooldownOverlay = v; config.save(); })
						.tooltip("Show a centered timer on inventory pets."),
				SkyOptions.colorRgb("Cooldown color", def.petCooldownColor,
						() -> config.petCooldownColor,
						v -> { config.petCooldownColor = v; config.save(); }),
				SkyOptions.colorRgb("Active color", def.petActiveColor,
						() -> config.petActiveColor,
						v -> { config.petActiveColor = v; config.save(); }),
				Options.toggle("Bold", def.petCooldownBold,
						() -> config.petCooldownBold,
						v -> { config.petCooldownBold = v; config.save(); })
		));
		ConfigRegistry.register(ConfigPanel.of("pets", "Pets",
				"Pet cooldown & active-effect timer", PanelIcon.CLOCK, List.of(petGroup)),
				Network.SKY);

		OptionGroup searchGroup = new OptionGroup("Chest search", List.of(
				Options.toggle("Chest search", def.chestSearchEnabled,
						() -> config.chestSearchEnabled,
						v -> { config.chestSearchEnabled = v; config.save(); })
						.tooltip("Search bar + filter-rule sidebar in containers; matches are highlighted.")
		));
		ConfigRegistry.register(ConfigPanel.of("sky-search", "Search",
				"Search & highlight items in containers", PanelIcon.MAGNIFIER, List.of(searchGroup)),
				Network.SKY);
		ConfigRegistry.register(PlayerListHudPanel.create(), Network.SKY);
		ConfigRegistry.register(TrackerHudPanel.create(), Network.SKY);

		OptionGroup interactionsGroup = new OptionGroup("Interactions", List.of(
				Options.toggle("Auto-trade", def.autoTradeEnabled,
						() -> config.autoTradeEnabled,
						v -> { config.autoTradeEnabled = v; config.save(); })
						.tooltip("Shift-right-click a player to send /trade <name>.")));
		ConfigRegistry.register(ConfigPanel.of("sky-interactions", "Interactions",
				"Player interaction shortcuts", PanelIcon.BUBBLE, List.of(interactionsGroup)),
				Network.SKY);

		OptionGroup damageGroup = new OptionGroup("Damage indicators", List.of(
				Options.toggle("Damage indicators", def.damageIndicatorsEnabled,
						() -> config.damageIndicatorsEnabled,
						v -> { config.damageIndicatorsEnabled = v; config.save(); })
						.tooltip("Floating red/green damage & heal numbers over nearby entities."),
				Options.intSlider("Radius (blocks)", def.damageIndicatorRadius, 5, 30, 1,
						() -> config.damageIndicatorRadius,
						v -> { config.damageIndicatorRadius = v; config.save(); })
						.tooltip("Only entities within this many blocks get indicators."),
				SkyOptions.colorRgb("Damage color", def.damageIndicatorColor,
						() -> config.damageIndicatorColor,
						v -> { config.damageIndicatorColor = v; config.save(); }),
				SkyOptions.colorRgb("Heal color", def.healIndicatorColor,
						() -> config.healIndicatorColor,
						v -> { config.healIndicatorColor = v; config.save(); })));
		ConfigRegistry.register(ConfigPanel.of("sky-damage", "Damage Indicators",
				"Floating damage & heal numbers", PanelIcon.SWORD, List.of(damageGroup)),
				Network.SKY);

		BetterSky.LOGGER.info("Loaded configs: {} and {}",
				sharedConfig.configPath(), config.configPath());
	}
}
