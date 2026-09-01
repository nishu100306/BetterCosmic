package dev.nishu.bettercosmic.prisons.client;

import dev.nishu.bettercosmic.prisons.BetterPrisons;
import dev.nishu.bettercosmic.prisons.PrisonWorlds;
import dev.nishu.bettercosmic.prisons.PrisonsGate;
import dev.nishu.bettercosmic.prisons.api.CosmicApi;
import dev.nishu.bettercosmic.prisons.chestsearch.ChestSearchTintProvider;
import dev.nishu.bettercosmic.prisons.chestsearch.ClueScrollProvider;
import dev.nishu.bettercosmic.prisons.chestsearch.SearchPanel;
import dev.nishu.bettercosmic.prisons.config.PrisonsConfig;
import dev.nishu.bettercosmic.prisons.devtools.PrisonDevCommands;
import dev.nishu.bettercosmic.prisons.easyview.EasyViewPanel;
import dev.nishu.bettercosmic.prisons.easyview.EasyViewProvider;
import dev.nishu.bettercosmic.prisons.easyview.ItemCooldownProvider;
import dev.nishu.bettercosmic.prisons.feature.AutoTrade;
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
import dev.nishu.bettercosmic.prisons.misc.EnchantBookTooltip;
import dev.nishu.bettercosmic.prisons.misc.GangPointTooltip;
import dev.nishu.bettercosmic.prisons.misc.PickaxeDropConfirmation;
import dev.nishu.bettercosmic.prisons.misc.PrisonbreakTexturePack;
import dev.nishu.bettercosmic.prisons.misc.QolPanel;
import dev.nishu.bettercosmic.prisons.misc.TooltipsPanel;
import dev.nishu.bettercosmic.prisons.notification.MessageNotifications;
import dev.nishu.bettercosmic.prisons.notification.NotificationsPanel;
import dev.nishu.bettercosmic.prisons.render.BlinkTrinketRenderer;
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
import dev.nishu.bettercosmic.shared.server.Network;
import dev.nishu.bettercosmic.shared.ui.model.ConfigRegistry;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;

/**
 * Client entrypoint for BetterPrisons.
 *
 * <p>Loads configs and stands up the shared services BetterPrisons builds on ({@link HudRenderer},
 * {@link ToastRenderer}), then registers every feature system — HUDs, EasyView providers, waypoints,
 * gang pings, peaceful mining, tooltips, and the quality-of-life features — each gated at runtime to
 * the Cosmic Prisons network.
 */
public class BetterPrisonsClient implements ClientModInitializer {

	/** Shared config (config/bettercosmic/shared.json) — the same instance every mod uses. */
	public static SharedConfig sharedConfig;

	/** BetterPrisons' own config (config/bettercosmic/betterprisons.json). */
	public static PrisonsConfig config;

	// ---- Feature systems ----
	public static SatchelHud satchelHud;
	public static StatsHud statsHud;
	public static CooldownHud cooldownHud;
	public static EnchantHud enchantHud;
	public static EventsHud eventsHud;
	public static SuperBreakerAura superBreakerAura;
	public static EnchantTracker enchantTracker;
	public static WaypointManager waypointManager;
	public static GangPingManager gangPingManager;
	public static PickaxeDropConfirmation pickaxeDropConfirmation;
	private static final EventChatParser eventChatParser = new EventChatParser();

	@Override
	public void onInitializeClient() {
		sharedConfig = SharedConfig.get();
		config = BetterCosmicConfig.load(PrisonsConfig.class);

		// Route toasts to the configured corner. (The config screen labels itself from its profile
		// selector; BetterPrisons registers its panels under Network.PRISONS in registerPanels().)
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

		// Developer/debug commands (gated behind the shared /bcdev developer mode).
		PrisonDevCommands.register();

		// Feed the Cooldown HUD from command sends and chat messages (replaces BP's chat mixins).
		ClientSendMessageEvents.COMMAND.register(command -> {
			if (PrisonsGate.active()) {
				cooldownHud.onCommandSent("/" + command);
			}
		});
		ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
			if (!overlay && PrisonsGate.active()) {
				String text = message.getString();
				cooldownHud.onChatReceived(text);
				enchantTracker.onChatMessage(text);
				eventChatParser.handle(eventsHud, text);
				GangPingChatParser.handle(text);
				MessageNotifications.handle(text);
			}
		});

		// Waypoints + beacon beams: capture camera/FOV for projection, then draw beams (3D) and
		// screen-edge markers (2D) from the prison suppliers (events + custom waypoints).
		WorldSpaceTransform.register();
		BeaconBeamRenderer.init();
		WaypointRenderer.init();
		BeaconBeamRenderer.addSource(WaypointSuppliers::beams, Network.PRISONS);
		WaypointRenderer.addSource(WaypointSuppliers::edgeTargets, Network.PRISONS);

		// Gang pings: screen markers (player heads + info panel) + beacon beams, expiring each tick.
		GangPingRenderer.init();
		BeaconBeamRenderer.addSource(GangPingRenderer::beams, Network.PRISONS);
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (PrisonsGate.active()) {
				gangPingManager.tick();
			}
		});

		// QoL: pickaxe drop protection (drop mixins call this), auto-trade (shift-right-click a player),
		// and the Blink-trinket destination overlay. Held-item scale + bold titles are pure mixins.
		pickaxeDropConfirmation = new PickaxeDropConfirmation();
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (PrisonsGate.active()) {
				pickaxeDropConfirmation.tick();
			}
		});
		AutoTrade.register();
		BlinkTrinketRenderer.register();

		// PrisonBreak texture pack: bundle it and auto-apply/remove by world each tick.
		PrisonbreakTexturePack.register();
		ClientTickEvents.END_CLIENT_TICK.register(client ->
				PrisonbreakTexturePack.update(PrisonsGate.active()
						&& PrisonWorlds.PRISONBREAK.equals(WaypointManager.detectWorldKey())));

		// Enchant procs: floating world-space labels driven by the Cosmic API's player.enchant_proc hook.
		FloatingTextRenderer.init();
		EnchantProcManager.init();

		// Cosmic API: client_hello handshake on join + hook routing (schedule → Events HUD, enchant
		// procs → EnchantProcManager, effects → Enchant HUD).
		CosmicApi.register();

		// Track the current world for per-world custom waypoints; clear stale event waypoints on join.
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (!PrisonsGate.active()) {
				return;
			}
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
		EasyView.register(new EasyViewProvider(), Network.PRISONS);
		// Item cooldown timers (pet / trinket / bandit box), centered on the item.
		EasyView.register(new ItemCooldownProvider(), Network.PRISONS);
		// Clue scroll step number (overlay) + chest-search match highlight (tint).
		EasyView.register(new ClueScrollProvider(), Network.PRISONS);
		EasyView.registerTint(new ChestSearchTintProvider(), Network.PRISONS);

		// Item tooltips: clue-scroll unmapped-step warning, enchant-book upgrade costs, gang-point expiry.
		ItemTooltipCallback.EVENT.register((stack, tooltipContext, tooltipType, lines) -> {
			if (!PrisonsGate.active()) {
				return;
			}
			ClueScrollProvider.appendTooltip(stack, lines);
			EnchantBookTooltip.append(stack, lines);
			GangPointTooltip.append(stack, lines);
		});

		// Peaceful mining: register the prison policy and start the shared engine (ghost render +
		// block-through targeting + interaction blocking live in :shared).
		PeacefulMining.setPolicy(new PrisonsPeacefulMiningPolicy(), Network.PRISONS);
		PeacefulMining.init();

		// Refresh the Combat cooldown when you attack another player (being hit is handled by
		// LocalPlayerHurtMixin). PASS so the hit is never consumed here.
		AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
			if (PrisonsGate.active() && world.isClientSide() && entity != player
					&& entity instanceof net.minecraft.world.entity.player.Player && cooldownHud != null) {
				cooldownHud.resetCombatCooldown();
			}
			return net.minecraft.world.InteractionResult.PASS;
		});

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
			if (!PrisonsGate.active()) {
				return;
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
		}, Network.PRISONS);

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
		}, Network.PRISONS);

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
		}, Network.PRISONS);

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
		}, Network.PRISONS);

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
		}, Network.PRISONS);

		// Super Breaker Aura is crosshair-centered (config X/Y offsets), so it isn't drag-editable.
		superBreakerAura = new SuperBreakerAura();
		superBreakerAura.enabled = config.superBreakerAuraEnabled;
		HudRegistry.register(superBreakerAura, () -> {}, false, Network.PRISONS);
	}

	/**
	 * Registers the BetterPrisons config panels, one per feature area, under the Prisons profile.
	 */
	private void registerPanels() {
		// Ordered by the config re-categorization sections: HUD overlays, then world & waypoints,
		// inventory & items, gameplay, and alerts. (Auto-trade, bold popups, and the texture pack — the
		// former "Misc" panel — now live on the Quality of life panel; chest search / clue scrolls live
		// on the Search panel. The global General panel is registered by the shared library.)
		ConfigRegistry.register(StatsHudPanel.create(), Network.PRISONS);
		ConfigRegistry.register(SatchelHudPanel.create(), Network.PRISONS);
		ConfigRegistry.register(CooldownHudPanel.create(), Network.PRISONS);
		ConfigRegistry.register(EnchantHudPanel.create(), Network.PRISONS);
		ConfigRegistry.register(EventsHudPanel.create(), Network.PRISONS);
		ConfigRegistry.register(WaypointsPanel.create(), Network.PRISONS);
		ConfigRegistry.register(GangPingsPanel.create(), Network.PRISONS);
		ConfigRegistry.register(EasyViewPanel.create(), Network.PRISONS);
		ConfigRegistry.register(SearchPanel.create(), Network.PRISONS);
		ConfigRegistry.register(TooltipsPanel.create(), Network.PRISONS);
		ConfigRegistry.register(PeacefulMiningPanel.create(), Network.PRISONS);
		ConfigRegistry.register(QolPanel.create(), Network.PRISONS);
		ConfigRegistry.register(NotificationsPanel.create(), Network.PRISONS);
	}
}
