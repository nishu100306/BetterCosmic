package dev.nishu.bettercosmic.prisons.client;

import dev.nishu.bettercosmic.prisons.BetterPrisons;
import dev.nishu.bettercosmic.prisons.api.CosmicApi;
import dev.nishu.bettercosmic.prisons.chestsearch.ChestSearchTintProvider;
import dev.nishu.bettercosmic.prisons.chestsearch.ClueScrollProvider;
import dev.nishu.bettercosmic.prisons.chestsearch.SearchPanel;
import dev.nishu.bettercosmic.prisons.config.PrisonsConfig;
import dev.nishu.bettercosmic.prisons.devtools.PrisonDevCommands;
import dev.nishu.bettercosmic.prisons.easyview.EasyViewPanel;
import dev.nishu.bettercosmic.prisons.easyview.EasyViewProvider;
import dev.nishu.bettercosmic.prisons.easyview.ItemCooldownProvider;
import dev.nishu.bettercosmic.prisons.feature.EventChatParser;
import dev.nishu.bettercosmic.prisons.gangping.GangPingChatParser;
import dev.nishu.bettercosmic.prisons.gangping.GangPingManager;
import dev.nishu.bettercosmic.prisons.gangping.GangPingRenderer;
import dev.nishu.bettercosmic.prisons.gangping.GangPingsPanel;
import dev.nishu.bettercosmic.prisons.feature.PeacefulMiningPanel;
import dev.nishu.bettercosmic.prisons.feature.PrisonsPeacefulMiningPolicy;
import dev.nishu.bettercosmic.prisons.enchantprocs.EnchantProcManager;
import dev.nishu.bettercosmic.prisons.enchants.EnchantSoundListener;
import dev.nishu.bettercosmic.prisons.enchants.EnchantTracker;
import dev.nishu.bettercosmic.prisons.enchants.SoundTracker;
import dev.nishu.bettercosmic.prisons.enchants.SuperBreakerDetector;
import dev.nishu.bettercosmic.prisons.hud.CooldownHud;
import dev.nishu.bettercosmic.prisons.hud.CooldownHudPanel;
import dev.nishu.bettercosmic.prisons.hud.EnchantHud;
import dev.nishu.bettercosmic.prisons.hud.EnchantHudPanel;
import dev.nishu.bettercosmic.prisons.hud.EventsHud;
import dev.nishu.bettercosmic.prisons.hud.EventsHudPanel;
import dev.nishu.bettercosmic.prisons.hud.SatchelHud;
import dev.nishu.bettercosmic.prisons.hud.SatchelHudPanel;
import dev.nishu.bettercosmic.prisons.hud.StatsHud;
import dev.nishu.bettercosmic.prisons.hud.StatsHudPanel;
import dev.nishu.bettercosmic.prisons.hud.SuperBreakerAura;
import dev.nishu.bettercosmic.prisons.input.PrisonKeybinds;
import dev.nishu.bettercosmic.prisons.waypoint.WaypointManager;
import dev.nishu.bettercosmic.prisons.waypoint.WaypointSuppliers;
import dev.nishu.bettercosmic.prisons.waypoint.WaypointsPanel;
import dev.nishu.bettercosmic.shared.config.BetterCosmicConfig;
import dev.nishu.bettercosmic.shared.config.SharedConfig;
import dev.nishu.bettercosmic.shared.easyview.EasyView;
import dev.nishu.bettercosmic.shared.hud.HudRegistry;
import dev.nishu.bettercosmic.shared.hud.HudRenderer;
import dev.nishu.bettercosmic.shared.notification.ToastRenderer;
import dev.nishu.bettercosmic.shared.peacefulmining.PeacefulMining;
import dev.nishu.bettercosmic.shared.render.BeaconBeamRenderer;
import dev.nishu.bettercosmic.shared.render.FloatingTextRenderer;
import dev.nishu.bettercosmic.shared.render.WaypointRenderer;
import dev.nishu.bettercosmic.shared.render.WorldSpaceTransform;
import dev.nishu.bettercosmic.shared.ui.ConfigUi;
import dev.nishu.bettercosmic.shared.ui.model.ConfigPanel;
import dev.nishu.bettercosmic.shared.ui.model.ConfigRegistry;
import dev.nishu.bettercosmic.shared.ui.model.OptionGroup;
import dev.nishu.bettercosmic.shared.ui.model.Options;
import dev.nishu.bettercosmic.shared.ui.model.PanelIcon;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;

import java.util.List;

/**
 * Client entrypoint for BetterPrisons.
 *
 * <p>Loads configs and stands up the shared services BetterPrisons builds on ({@link HudRenderer},
 * {@link ToastRenderer}). Feature systems (HUDs, EasyView providers, waypoints, CosmicApi, ...) are
 * registered here as they are ported in during Phase C; today this wires the config pipeline and one
 * real feature panel so the shared config screen shows a BetterPrisons tab.
 */
public class BetterPrisonsClient implements ClientModInitializer {

	/** Shared config (config/bettercosmic/shared.json) — the same instance every mod uses. */
	public static SharedConfig sharedConfig;

	/** BetterPrisons' own config (config/bettercosmic/betterprisons.json). */
	public static PrisonsConfig config;

	// ---- Feature systems (populated as they are ported in Phase C) ----
	public static SatchelHud satchelHud;
	public static StatsHud statsHud;
	public static CooldownHud cooldownHud;
	public static EnchantHud enchantHud;
	public static EventsHud eventsHud;
	public static SuperBreakerAura superBreakerAura;
	public static EnchantTracker enchantTracker;
	public static WaypointManager waypointManager;
	public static GangPingManager gangPingManager;
	private static final EventChatParser eventChatParser = new EventChatParser();

	@Override
	public void onInitializeClient() {
		sharedConfig = SharedConfig.get();
		config = BetterCosmicConfig.load(PrisonsConfig.class);

		// Brand the shared config screen and route toasts to the configured corner.
		ConfigUi.setSubtitle("Prisons");
		ToastRenderer.setCornerSupplier(() -> config.toastCorner);
		ToastRenderer.register();

		// Waypoint store (custom + auto-added event waypoints) — needed by the Events HUD and renderers.
		waypointManager = new WaypointManager();
		waypointManager.load();

		// Gang ping tracking.
		gangPingManager = new GangPingManager();

		// Enchant tracking (Super Breaker, Powerball) — must exist before the HUDs that read it.
		enchantTracker = new EnchantTracker();
		registerEnchantSystem();

		// Feature HUDs — construct, load position from config, register with the shared framework.
		registerHuds();

		// Draw + tick registered HUDs, respecting F1.
		HudRenderer.register();

		// Key bindings (reset/pause Stats HUD, ...).
		PrisonKeybinds.register();

		// Developer/debug commands (gated behind the shared /bdev developer mode).
		PrisonDevCommands.register();

		// Feed the Cooldown HUD from command sends and chat messages (replaces BP's chat mixins).
		ClientSendMessageEvents.COMMAND.register(command -> cooldownHud.onCommandSent("/" + command));
		ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
			if (!overlay) {
				String text = message.getString();
				cooldownHud.onChatReceived(text);
				enchantTracker.onChatMessage(text);
				eventChatParser.handle(eventsHud, text);
				GangPingChatParser.handle(text);
			}
		});

		// Waypoints + beacon beams: capture camera/FOV for projection, then draw beams (3D) and
		// screen-edge markers (2D) from the prison suppliers (events + custom waypoints).
		WorldSpaceTransform.register();
		BeaconBeamRenderer.init();
		WaypointRenderer.init();
		BeaconBeamRenderer.addSource(WaypointSuppliers::beams);
		WaypointRenderer.addSource(WaypointSuppliers::edgeTargets);

		// Gang pings: screen markers (player heads + info panel) + beacon beams, expiring each tick.
		GangPingRenderer.init();
		BeaconBeamRenderer.addSource(GangPingRenderer::beams);
		ClientTickEvents.END_CLIENT_TICK.register(client -> gangPingManager.tick());

		// Enchant procs: floating world-space labels driven by the Cosmic API's player.enchant_proc hook.
		FloatingTextRenderer.init();
		EnchantProcManager.init();

		// Cosmic API: client_hello handshake on join + hook routing (schedule → Events HUD, enchant
		// procs → EnchantProcManager, effects → Enchant HUD).
		CosmicApi.register();

		// Track the current world for per-world custom waypoints; clear stale event waypoints on join.
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			String world = WaypointManager.detectWorldKey();
			if (!world.equals(waypointManager.getCurrentWorld())) {
				waypointManager.setCurrentWorld(world);
			}
		});
		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
			waypointManager.clearAllEventWaypoints();
			eventsHud.clearMeteors();
			eventsHud.clearMerchants();
			eventsHud.clearBanditRushes();
			eventsHud.clearMeteoriteShowers();
		});

		// EasyView inventory/hotbar overlays (drawn by the shared EasyView mixins).
		EasyView.register(new EasyViewProvider());
		// Item cooldown timers (pet / trinket / bandit box), centered on the item.
		EasyView.register(new ItemCooldownProvider());
		// Clue scroll step number (overlay) + chest-search match highlight (tint).
		EasyView.register(new ClueScrollProvider());
		EasyView.registerTint(new ChestSearchTintProvider());

		// Item tooltips: flag unmapped clue-scroll steps.
		ItemTooltipCallback.EVENT.register((stack, tooltipContext, tooltipType, lines) ->
				ClueScrollProvider.appendTooltip(stack, lines));

		// Peaceful mining: register the prison policy and start the shared engine (ghost render +
		// block-through targeting + interaction blocking live in :shared).
		PeacefulMining.setPolicy(new PrisonsPeacefulMiningPolicy());
		PeacefulMining.init();

		registerPanels();

		BetterPrisons.LOGGER.info("BetterPrisons initialized. Configs: {} and {}",
				sharedConfig.configPath(), config.configPath());
	}

	/**
	 * Ticks the enchant tracker each client tick (then clears the per-tick sound flag) and registers
	 * the sound listener that detects Powerball's wither-shoot tell.
	 */
	private void registerEnchantSystem() {
		final boolean[] soundListenerRegistered = {false};
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			// Register the sound listener once the sound manager is available.
			if (!soundListenerRegistered[0] && client.getSoundManager() != null) {
				client.getSoundManager().addListener(new EnchantSoundListener());
				soundListenerRegistered[0] = true;
			}
			enchantTracker.tick(client);
			// Super Breaker activation: correlate this tick's nearest flame/spell particle with the
			// dragon-growl sound. Runs after the enchant tick and before the sound flags are cleared.
			SuperBreakerDetector.evaluate();
			SoundTracker.clearTickCache();
		});
	}

	/**
	 * Constructs the feature HUDs, loads their saved position from config, and registers them with the
	 * shared {@link HudRegistry} (each with a callback that persists its position after a drag).
	 */
	private void registerHuds() {
		PrisonsConfig def = new PrisonsConfig();

		satchelHud = new SatchelHud();
		satchelHud.x = config.satchelHudX;
		satchelHud.y = config.satchelHudY;
		satchelHud.defaultX = def.satchelHudX;
		satchelHud.defaultY = def.satchelHudY;
		satchelHud.enabled = config.satchelHudEnabled;
		HudRegistry.register(satchelHud, () -> {
			config.satchelHudX = satchelHud.x;
			config.satchelHudY = satchelHud.y;
			config.save();
		});

		statsHud = new StatsHud();
		statsHud.x = config.statsHudX;
		statsHud.y = config.statsHudY;
		statsHud.defaultX = def.statsHudX;
		statsHud.defaultY = def.statsHudY;
		statsHud.enabled = config.statsHudEnabled;
		HudRegistry.register(statsHud, () -> {
			config.statsHudX = statsHud.x;
			config.statsHudY = statsHud.y;
			config.save();
		});

		cooldownHud = new CooldownHud();
		cooldownHud.loadFromDefinitions();
		cooldownHud.x = config.cooldownHudX;
		cooldownHud.y = config.cooldownHudY;
		cooldownHud.defaultX = def.cooldownHudX;
		cooldownHud.defaultY = def.cooldownHudY;
		cooldownHud.enabled = config.cooldownHudEnabled;
		HudRegistry.register(cooldownHud, () -> {
			config.cooldownHudX = cooldownHud.x;
			config.cooldownHudY = cooldownHud.y;
			config.save();
		});

		enchantHud = new EnchantHud();
		enchantHud.x = config.enchantHudX;
		enchantHud.y = config.enchantHudY;
		enchantHud.defaultX = def.enchantHudX;
		enchantHud.defaultY = def.enchantHudY;
		enchantHud.enabled = config.enchantHudEnabled;
		HudRegistry.register(enchantHud, () -> {
			config.enchantHudX = enchantHud.x;
			config.enchantHudY = enchantHud.y;
			config.save();
		});

		eventsHud = new EventsHud();
		eventsHud.x = config.eventsHudX;
		eventsHud.y = config.eventsHudY;
		eventsHud.defaultX = def.eventsHudX;
		eventsHud.defaultY = def.eventsHudY;
		eventsHud.enabled = config.eventsHudEnabled;
		HudRegistry.register(eventsHud, () -> {
			config.eventsHudX = eventsHud.x;
			config.eventsHudY = eventsHud.y;
			config.save();
		});

		// Super Breaker Aura is crosshair-centered (config X/Y offsets), so it isn't drag-editable.
		superBreakerAura = new SuperBreakerAura();
		superBreakerAura.enabled = config.superBreakerAuraEnabled;
		HudRegistry.register(superBreakerAura, () -> {}, false);
	}

	/**
	 * Registers the BetterPrisons config panels. Per-feature panels are added alongside their features
	 * in Phase C.
	 */
	private void registerPanels() {
		PrisonsConfig def = new PrisonsConfig(); // code defaults, so reset restores these

		OptionGroup qol = new OptionGroup("Quality of life", List.of(
				Options.toggle("Auto-trade", def.autoTradeEnabled,
						() -> config.autoTradeEnabled,
						v -> { config.autoTradeEnabled = v; config.save(); })
						.tooltip("Shift-right-click a player to send /trade <name>."),
				Options.toggle("Bold XP/Energy popups", def.boldXpEnergyTitles,
						() -> config.boldXpEnergyTitles,
						v -> { config.boldXpEnergyTitles = v; config.save(); })
						.tooltip("Bold the server's +XP / +Energy title popups."),
				Options.toggle("PrisonBreak texture pack", def.prisonbreakTexturePackEnabled,
						() -> config.prisonbreakTexturePackEnabled,
						v -> { config.prisonbreakTexturePackEnabled = v; config.save(); })
						.tooltip("Auto-apply the bundled ore pack in the PrisonBreak world.")));

		OptionGroup search = new OptionGroup("Search", List.of(
				Options.toggle("Chest search", def.chestSearchEnabled,
						() -> config.chestSearchEnabled,
						v -> { config.chestSearchEnabled = v; config.save(); })
						.tooltip("Search bar + filter sidebar in containers."),
				Options.toggle("Clue scroll sorting", def.clueScrollSortingEnabled,
						() -> config.clueScrollSortingEnabled,
						v -> { config.clueScrollSortingEnabled = v; config.save(); })
						.tooltip("Show each clue scroll's step number on the item.")));

		ConfigRegistry.register(ConfigPanel.of("prisons-misc", "Misc",
				"General BetterPrisons features", PanelIcon.GEAR, List.of(qol, search)));

		ConfigRegistry.register(EasyViewPanel.create());
		ConfigRegistry.register(SatchelHudPanel.create());
		ConfigRegistry.register(StatsHudPanel.create());
		ConfigRegistry.register(CooldownHudPanel.create());
		ConfigRegistry.register(EnchantHudPanel.create());
		ConfigRegistry.register(EventsHudPanel.create());
		ConfigRegistry.register(WaypointsPanel.create());
		ConfigRegistry.register(GangPingsPanel.create());
		ConfigRegistry.register(SearchPanel.create());
		ConfigRegistry.register(PeacefulMiningPanel.create());
	}
}
