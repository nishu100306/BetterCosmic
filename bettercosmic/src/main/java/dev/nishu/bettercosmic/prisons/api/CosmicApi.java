package dev.nishu.bettercosmic.prisons.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.nishu.bettercosmic.prisons.BetterPrisons;
import dev.nishu.bettercosmic.prisons.client.BetterPrisonsClient;
import dev.nishu.bettercosmic.prisons.hud.CooldownHud;
import dev.nishu.bettercosmic.prisons.hud.EventsHud;
import dev.nishu.bettercosmic.prisons.pvviewer.PvApiReader;
import dev.nishu.bettercosmic.shared.config.SharedConfig;
import dev.nishu.bettercosmic.shared.server.Network;
import dev.nishu.bettercosmic.shared.server.ServerContext;
import net.minecraft.network.chat.Component;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.loader.api.FabricLoader;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Client-side presence handshake for the official Cosmic mod API, over the per-mod broker channel
 * {@code cosmicapi:bettercosmic} (see {@link CosmicApiPayload}).
 *
 * <p>The registry <b>requires</b> every approved mod to send a {@code client_hello} on join (even one
 * asking for no scopes) — mods that don't aren't approved. On join this sends the hello with the app's
 * public {@code clientId} and the scopes/hooks it wants, then stores the effective access the server
 * grants back in the {@code resolve} reply.
 *
 * <p>Beyond the handshake this also routes live traffic into features: push hooks (see
 * {@link #handleEvent} — cooldowns, enchant procs, meteor landings, merchant spawn/despawn) and the
 * request/reply {@code private_vault.read} action ({@link #sendAction} / {@link #routeActionReply},
 * consumed by the PV viewer). The requested scopes/hooks below are trimmed to exactly what those
 * features consume — each entry is annotated with its consumer.
 *
 * <p>During approval the dashboard issues test credentials and the {@code resolve} reply carries
 * {@code testingMode:true}. {@link #CLIENT_ID} holds the app's public client id from the Cosmic
 * developer dashboard. The handshake is mandatory for an approved mod, so it is always sent on join —
 * there is no switch to disable it; {@link #sendHello()} only no-ops before the config has loaded.
 */
public final class CosmicApi {

	private static final int PROTOCOL_VERSION = 1;

	/** The registry mod id (matches {@code fabric.mod.json} / the {@code cosmicapi:bettercosmic} channel). */
	private static final String MOD_ID = BetterPrisons.FABRIC_MOD_ID;

	/** BetterCosmic's public client id, issued by the Cosmic developer dashboard. */
	private static final String CLIENT_ID = "client_mtlzzg2kjvn813cva1";

	/**
	 * Scopes we ask the server to grant this connection. Trimmed to only what a feature actually consumes
	 * (each backs a routed hook below, or the {@code private_vault.read} action) — unused scopes are not
	 * requested, so the approval ask matches what the mod uses.
	 */
	private static final List<String> REQUESTED_SCOPES = List.of(
			"player.cooldowns:read",          // player.cooldowns.changed -> CooldownHud
			"player.private_vaults:read",     // private_vault.read action -> PV viewer
			"server.meteors:read",            // server.meteor.landing.changed -> EventsHud
			"server.merchants:read",          // server.merchant.spawned/despawned -> EventsHud
			"hooks.player.enchant_proc:read"); // player.enchant_proc -> EnchantTracker

	/** Scopes we cannot function without. Empty — every feature degrades gracefully (chat/scrape fallback). */
	private static final List<String> REQUIRED_SCOPES = List.of();

	/** Push hooks we subscribe to — each is routed into a feature in {@link #handleEvent}. */
	private static final List<String> REQUESTED_HOOKS = List.of(
			"server.meteor.landing.changed",
			"server.merchant.spawned",
			"server.merchant.despawned",
			"player.cooldowns.changed",
			"player.enchant_proc");

	private static final Gson GSON = new Gson();
	/** Pretty printer for the full-payload dev logging (multi-line, readable). */
	private static final Gson GSON_PRETTY = new GsonBuilder().setPrettyPrinting().create();

	// Effective access granted by the server for this connection (populated from the resolve reply).
	public static volatile String sessionId = null;
	public static volatile String serverScope = null;
	public static volatile boolean testingMode = false;
	public static volatile Set<String> allowedScopes = Set.of();
	public static volatile Set<String> allowedHooks = Set.of();

	private static volatile boolean warnedMissingClientId = false;

	private CosmicApi() {}

	/** Registers the channel payload, the message receiver, and the on-join handshake. Call once at client init. */
	public static void register() {
		PayloadTypeRegistry.playC2S().register(CosmicApiPayload.TYPE, CosmicApiPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(CosmicApiPayload.TYPE, CosmicApiPayload.CODEC);

		ClientPlayNetworking.registerGlobalReceiver(CosmicApiPayload.TYPE,
				(payload, context) -> context.client().execute(() -> handleMessage(payload.json())));

		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
			resetSession();
			sendHello();
		});
	}

	private static void resetSession() {
		sessionId = null;
		serverScope = null;
		testingMode = false;
		allowedScopes = Set.of();
		allowedHooks = Set.of();
	}

	/**
	 * Whether verbose API logging is enabled. Off for normal players, so a shipped build's log stays
	 * quiet (only the concise once-per-join resolve line and genuine warnings survive). Turn it on to
	 * dump every inbound/outbound payload and the routine diagnostics: live with {@code /bcdev} (shared
	 * developer mode), or automatically in a dev environment. Reused by {@link PvApiReader}.
	 */
	public static boolean verboseLogging() {
		try {
			return SharedConfig.get().developerMode
					|| FabricLoader.getInstance().isDevelopmentEnvironment();
		} catch (Exception e) {
			return false;
		}
	}

	private static void sendHello() {
		// The presence handshake is mandatory and cannot be disabled — the registry requires every
		// approved mod to send client_hello on join. We only bail if the config isn't loaded yet
		// (installId() needs it), never on a user preference.
		if (BetterPrisonsClient.config == null) {
			return;
		}
		if (CLIENT_ID.startsWith("REPLACE")) {
			if (!warnedMissingClientId) {
				warnedMissingClientId = true;
				BetterPrisons.LOGGER.warn(
						"Cosmic API: no client id set; skipping handshake. Register the app on the Cosmic "
						+ "dashboard and set CosmicApi.CLIENT_ID to enable it.");
			}
			return;
		}
		// Send the handshake when we're connected to Cosmic Prisons. We deliberately do NOT gate on
		// ClientPlayNetworking.canSend(): Cosmic is a Paper/Bukkit server, and Bukkit plugin-message
		// listeners aren't reliably advertised through Fabric's channel negotiation, so canSend can be
		// false even though the server is listening on cosmicapi:bettercosmic. ClientPlayNetworking.send()
		// doesn't require canSend — it just emits the custom-payload packet — so we gate on the detected
		// network instead (also honouring a ServerContext override, if one is ever set for testing).
		boolean canSend = ClientPlayNetworking.canSend(CosmicApiPayload.TYPE);
		boolean onPrisons = ServerContext.detected() == Network.PRISONS
				|| ServerContext.override() == Network.PRISONS;
		if (!canSend && !onPrisons) {
			if (verboseLogging()) {
				BetterPrisons.LOGGER.info(
						"Cosmic API: not sending client_hello (not on Cosmic Prisons; detected={}, canSend=false).",
						ServerContext.detected());
			}
			return;
		}

		JsonObject hello = new JsonObject();
		hello.addProperty("type", "client_hello");
		hello.addProperty("protocolVersion", PROTOCOL_VERSION);
		hello.addProperty("clientId", CLIENT_ID);
		hello.addProperty("modId", MOD_ID);
		hello.addProperty("installId", installId());
		hello.addProperty("modLoader", "fabric");
		hello.addProperty("minecraftVersion", minecraftVersion());
		hello.addProperty("modVersion", modVersion());
		hello.add("requestedScopes", GSON.toJsonTree(REQUESTED_SCOPES));
		hello.add("requiredScopes", GSON.toJsonTree(REQUIRED_SCOPES));
		hello.add("requestedHooks", GSON.toJsonTree(REQUESTED_HOOKS));

		ClientPlayNetworking.send(new CosmicApiPayload(GSON.toJson(hello)));
		if (verboseLogging()) {
			BetterPrisons.LOGGER.info("Cosmic API -> sent client_hello:\n{}", GSON_PRETTY.toJson(hello));
		}
	}

	/**
	 * Handles an inbound channel message. <b>Every</b> received payload is logged in full first (pretty
	 * JSON when parseable, else the raw string) so the API's real message shapes can be observed. It is
	 * then dispatched by {@code type}: a {@code resolve} handshake reply is stored + summarised; an
	 * {@code ack}/{@code action_result} is correlated back to the action that issued it; an {@code event}
	 * push hook is routed to its consuming feature. Unrecognised messages are left logged-only.
	 */
	private static void handleMessage(String json) {
		logFullPayload(json);
		try {
			JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
			String type = obj.has("type") ? obj.get("type").getAsString() : "";

			if ("resolve".equals(type)
					|| obj.has("allowedScopes") || obj.has("allowedHooks")) {
				handleResolve(obj);
				return;
			}
			if ("ack".equals(type) || "action_result".equals(type)) {
				routeActionReply(type, obj);
				return;
			}
			if ("event".equals(type)) {
				handleEvent(obj);
			}
		} catch (Exception e) {
			BetterPrisons.LOGGER.warn("Cosmic API: received a non-JSON / malformed message: {}", e.toString());
		}
	}

	/**
	 * Sends a request/reply {@code action} as the player and returns its generated {@code requestId} (or
	 * {@code null} if there's no live session yet). The caller is responsible for having the backing
	 * scope in {@link #allowedScopes}; a rejected {@code ack} carries the reason. The reply arrives as an
	 * {@code ack} (immediately) and, for data actions, an {@code action_result} (async) — both correlated
	 * by {@code requestId}.
	 */
	public static String sendAction(String actionType, JsonObject payload) {
		if (sessionId == null) {
			return null;
		}
		String requestId = "req_" + UUID.randomUUID();
		JsonObject msg = new JsonObject();
		msg.addProperty("type", "action");
		msg.addProperty("sessionId", sessionId);
		msg.addProperty("actionType", actionType);
		msg.addProperty("requestId", requestId);
		msg.add("payload", payload == null ? new JsonObject() : payload);
		ClientPlayNetworking.send(new CosmicApiPayload(GSON.toJson(msg)));
		return requestId;
	}

	/** Routes an {@code ack} / {@code action_result} to the feature that issued the action, by actionType. */
	private static void routeActionReply(String type, JsonObject obj) {
		String actionType = asString(obj, "actionType", "");
		if ("private_vault.read".equals(actionType)) {
			if ("ack".equals(type)) {
				PvApiReader.onAck(obj);
			} else {
				PvApiReader.onActionResult(obj);
			}
		}
	}

	/**
	 * Routes a push hook event to the feature that consumes it. Only wired hooks act; every event is
	 * already logged in full by {@link #logFullPayload}, so unwired ones are simply ignored here.
	 */
	private static void handleEvent(JsonObject obj) {
		String eventType = obj.has("eventType") ? obj.get("eventType").getAsString() : "";
		switch (eventType) {
			case "player.cooldowns.changed" -> routeCooldowns(obj);
			case "player.enchant_proc" -> routeEnchantProc(obj);
			case "server.meteor.landing.changed" -> routeMeteor(obj);
			case "server.merchant.spawned" -> routeMerchantSpawned(obj);
			case "server.merchant.despawned" -> routeMerchantDespawned(obj);
			default -> { /* not wired into a feature yet */ }
		}
	}

	/**
	 * Hands a {@code player.enchant_proc} event to the Enchant HUD's tracker. The proc detail lives at
	 * {@code root.payload.payload = { enchantId, displayName, level }}; {@code displayName} carries
	 * legacy {@code &}/{@code §} colour codes (normalised to {@code §} for rendering).
	 */
	private static void routeEnchantProc(JsonObject root) {
		if (BetterPrisonsClient.enchantTracker == null) {
			return;
		}
		if (!root.has("payload") || !root.get("payload").isJsonObject()) {
			return;
		}
		JsonObject envelope = root.getAsJsonObject("payload");
		JsonObject data = envelope.has("payload") && envelope.get("payload").isJsonObject()
				? envelope.getAsJsonObject("payload") : null;
		if (data == null || !data.has("enchantId") || !data.get("enchantId").isJsonPrimitive()) {
			return;
		}
		String enchantId = data.get("enchantId").getAsString();
		String display = data.has("displayName") && data.get("displayName").isJsonPrimitive()
				? data.get("displayName").getAsString() : enchantId;
		int level = data.has("level") && data.get("level").isJsonPrimitive() ? data.get("level").getAsInt() : 0;
		BetterPrisonsClient.enchantTracker.onEnchantProc(enchantId, Component.literal(display.replace('&', '§')), level);
	}

	/**
	 * Routes a {@code server.meteor.landing.changed} event to the Events HUD. The meteor detail lives at
	 * {@code root.payload.payload = { state, x, y, z, meteorType, summoner, remainingMillis, ... }}. A
	 * meteor is keyed by its target {@code x/y/z}; {@code summoner} distinguishes Natural (empty) from
	 * Summoned. Observed states: {@code "in_flight"} (falling) and {@code "landed"} (crashed); the HUD
	 * treats any other state as crashed and logs it so the vocabulary can be extended.
	 */
	private static void routeMeteor(JsonObject root) {
		EventsHud hud = BetterPrisonsClient.eventsHud;
		JsonObject d = innerData(root);
		if (hud == null || d == null) {
			return;
		}
		Integer x = asInt(d, "x");
		Integer y = asInt(d, "y");
		Integer z = asInt(d, "z");
		if (x == null || y == null || z == null) {
			return;
		}
		hud.onMeteorHook(x, y, z, asString(d, "state", ""), asString(d, "summoner", ""),
				asLong(d, "remainingMillis", 0L));
	}

	/**
	 * Routes a {@code server.merchant.spawned} event to the Events HUD. Detail at
	 * {@code root.payload.payload = { merchantId, zoneId, x, y, z, ... }}; the ore tier comes from
	 * {@code zoneId} (e.g. {@code "redstone"}) and {@code merchantId} keys the later despawn.
	 */
	private static void routeMerchantSpawned(JsonObject root) {
		EventsHud hud = BetterPrisonsClient.eventsHud;
		JsonObject d = innerData(root);
		if (hud == null || d == null) {
			return;
		}
		Integer x = asInt(d, "x");
		Integer y = asInt(d, "y");
		Integer z = asInt(d, "z");
		String zoneId = asString(d, "zoneId", "");
		long merchantId = asLong(d, "merchantId", 0L);
		if (x == null || y == null || z == null || zoneId.isEmpty()) {
			return;
		}
		hud.onMerchantSpawnedHook(merchantId, zoneId, x, y, z);
	}

	/**
	 * Routes a {@code server.merchant.despawned} event to the Events HUD, removing the merchant whose
	 * {@code merchantId} matches the one from {@code server.merchant.spawned} (confirmed to echo the same
	 * id). If {@code merchantId} is somehow absent we log the data as a defensive fallback.
	 */
	private static void routeMerchantDespawned(JsonObject root) {
		EventsHud hud = BetterPrisonsClient.eventsHud;
		JsonObject d = innerData(root);
		if (hud == null || d == null) {
			return;
		}
		long merchantId = asLong(d, "merchantId", 0L);
		if (merchantId == 0L) {
			if (verboseLogging()) {
				BetterPrisons.LOGGER.info("Cosmic API: merchant.despawned without a merchantId; data:\n{}",
						GSON_PRETTY.toJson(d));
			}
			return;
		}
		hud.onMerchantDespawnedHook(merchantId);
	}

	/** The doubly-nested event data: {@code root.payload.payload}, or {@code null} if absent. */
	private static JsonObject innerData(JsonObject root) {
		if (!root.has("payload") || !root.get("payload").isJsonObject()) {
			return null;
		}
		JsonObject envelope = root.getAsJsonObject("payload");
		return envelope.has("payload") && envelope.get("payload").isJsonObject()
				? envelope.getAsJsonObject("payload") : null;
	}

	private static Integer asInt(JsonObject o, String key) {
		return o.has(key) && o.get(key).isJsonPrimitive() ? o.get(key).getAsInt() : null;
	}

	private static long asLong(JsonObject o, String key, long def) {
		return o.has(key) && o.get(key).isJsonPrimitive() ? o.get(key).getAsLong() : def;
	}

	private static String asString(JsonObject o, String key, String def) {
		return o.has(key) && o.get(key).isJsonPrimitive() ? o.get(key).getAsString() : def;
	}

	/**
	 * Hands the {@code player.cooldowns.changed} snapshot to the Cooldown HUD. The active list lives at
	 * {@code root.payload.payload.activeCooldowns[]} (the event nests an envelope inside an envelope);
	 * an absent array is treated as an empty snapshot so the HUD clears its hook-owned cooldowns.
	 */
	private static void routeCooldowns(JsonObject root) {
		CooldownHud hud = BetterPrisonsClient.cooldownHud;
		if (hud == null) {
			return;
		}
		if (!root.has("payload") || !root.get("payload").isJsonObject()) {
			return;
		}
		JsonObject envelope = root.getAsJsonObject("payload");
		JsonObject inner = envelope.has("payload") && envelope.get("payload").isJsonObject()
				? envelope.getAsJsonObject("payload") : null;
		if (inner == null || !inner.has("activeCooldowns") || !inner.get("activeCooldowns").isJsonArray()) {
			hud.onCooldownsChanged(List.of());
			return;
		}
		List<CooldownHud.ServerCooldown> list = new ArrayList<>();
		for (JsonElement el : inner.getAsJsonArray("activeCooldowns")) {
			if (!el.isJsonObject()) {
				continue;
			}
			JsonObject e = el.getAsJsonObject();
			String key = e.has("key") && e.get("key").isJsonPrimitive() ? e.get("key").getAsString()
					: (e.has("cooldownId") && e.get("cooldownId").isJsonPrimitive() ? e.get("cooldownId").getAsString() : null);
			if (key == null) {
				continue;
			}
			long remaining = e.has("remainingMillis") && e.get("remainingMillis").isJsonPrimitive()
					? e.get("remainingMillis").getAsLong() : 0L;
			list.add(new CooldownHud.ServerCooldown(key, remaining));
		}
		hud.onCooldownsChanged(list);
	}

	/**
	 * Dumps the complete inbound payload (pretty-printed when valid JSON, else verbatim). Verbose-only,
	 * so a shipped build doesn't log every hook/cooldown/action packet — flip on with {@code /bcdev} to
	 * observe raw message shapes during testing.
	 */
	private static void logFullPayload(String json) {
		if (!verboseLogging()) {
			return;
		}
		String body;
		try {
			body = GSON_PRETTY.toJson(JsonParser.parseString(json));
		} catch (Exception e) {
			body = json; // not JSON — log verbatim
		}
		BetterPrisons.LOGGER.info("Cosmic API <- received payload:\n{}", body);
	}

	/** Records the effective access from the {@code resolve} reply and logs a readable summary. */
	private static void handleResolve(JsonObject obj) {
		if (obj.has("sessionId") && obj.get("sessionId").isJsonPrimitive()) {
			sessionId = obj.get("sessionId").getAsString();
		}
		if (obj.has("serverScope") && obj.get("serverScope").isJsonPrimitive()) {
			serverScope = obj.get("serverScope").getAsString();
		}
		testingMode = obj.has("testingMode") && obj.get("testingMode").getAsBoolean();
		allowedScopes = toSet(obj, "allowedScopes");
		allowedHooks = toSet(obj, "allowedHooks");
		Set<String> deniedScopes = toSet(obj, "deniedScopes");
		Set<String> deniedHooks = toSet(obj, "deniedHooks");
		boolean allowed = !obj.has("allowed") || obj.get("allowed").getAsBoolean();
		String reason = obj.has("reason") && obj.get("reason").isJsonPrimitive()
				? obj.get("reason").getAsString() : "";

		if (!allowed) {
			// A denied handshake is worth a warning even in a quiet build — the API features won't work.
			BetterPrisons.LOGGER.warn("Cosmic API resolve denied{}.", reason.isEmpty() ? "" : " (" + reason + ")");
			return;
		}
		// One concise line per join in normal builds; the full granted/denied sets only under /bcdev.
		if (verboseLogging()) {
			BetterPrisons.LOGGER.info(
					"Cosmic API resolve: testing={} server={} scopes={} hooks={} deniedScopes={} deniedHooks={}",
					testingMode, serverScope, allowedScopes, allowedHooks, deniedScopes, deniedHooks);
		} else {
			BetterPrisons.LOGGER.info("Cosmic API connected (testing={}, {} scopes, {} hooks).",
					testingMode, allowedScopes.size(), allowedHooks.size());
		}
	}

	private static Set<String> toSet(JsonObject obj, String key) {
		Set<String> out = new HashSet<>();
		if (obj.has(key) && obj.get(key).isJsonArray()) {
			JsonArray arr = obj.getAsJsonArray(key);
			for (JsonElement el : arr) {
				if (el.isJsonPrimitive()) {
					out.add(el.getAsString());
				}
			}
		}
		return out;
	}

	/** A stable per-install id, generated once and persisted in the config. */
	private static String installId() {
		String id = BetterPrisonsClient.config.cosmicApiInstallId;
		if (id == null || id.isEmpty()) {
			id = "ins_" + UUID.randomUUID();
			BetterPrisonsClient.config.cosmicApiInstallId = id;
			BetterPrisonsClient.config.save();
		}
		return id;
	}

	private static String minecraftVersion() {
		return FabricLoader.getInstance().getModContainer("minecraft")
				.map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("unknown");
	}

	private static String modVersion() {
		return FabricLoader.getInstance().getModContainer(BetterPrisons.FABRIC_MOD_ID)
				.map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("unknown");
	}
}
