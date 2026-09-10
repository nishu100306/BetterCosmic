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
 * grants back in the {@code resolve} reply. <b>This is intentionally handshake-only:</b> it requests
 * scopes/hooks and records what's granted so the flow can be tested end-to-end, but it does <em>not</em>
 * yet route any push hooks or action results into features — that comes later, feature by feature.
 *
 * <p><b>Before this can talk to the live server</b> the app must be registered on the Cosmic developer
 * dashboard and its public client id pasted into {@link #CLIENT_ID}. Until then {@link #sendHello()}
 * no-ops (logged once). During approval the dashboard issues test credentials and the reply carries
 * {@code testingMode:true}. Requested scopes/hooks below are provisional — a superset we expect to grow
 * into — and can be trimmed to match approval.
 */
public final class CosmicApi {

	private static final int PROTOCOL_VERSION = 1;

	/** The registry mod id (matches {@code fabric.mod.json} / the {@code cosmicapi:bettercosmic} channel). */
	private static final String MOD_ID = BetterPrisons.FABRIC_MOD_ID;

	/** BetterCosmic's public client id from the Cosmic developer dashboard. Paste it here to go live. */
	private static final String CLIENT_ID = "client_mtlzzg2kjvn813cva1";

	/**
	 * Scopes we ask the server to grant this connection. Provisional (nothing consumes them yet); each
	 * hook below is paired with its backing read scope. See the coverage map in the compliance notes.
	 */
	private static final List<String> REQUESTED_SCOPES = List.of(
			"events:read",
			"player.effects:read",
			"player.cooldowns:read",
			"player.satchels:read",
			"player.private_vaults:read",
			"server.meteors:read",
			"server.merchants:read",
			"gang.pings:read",
			"hooks.player.enchant_proc:read");

	/** Scopes we cannot function without. Empty for the handshake-only phase — nothing is load-bearing yet. */
	private static final List<String> REQUIRED_SCOPES = List.of();

	/** Push hooks we ask to subscribe to. Provisional; not routed anywhere yet. */
	private static final List<String> REQUESTED_HOOKS = List.of(
			"server.event.schedule.changed",
			"server.meteor.landing.changed",
			"server.merchant.spawned",
			"player.effects.changed",
			"player.cooldowns.changed",
			"player.enchant_proc",
			"gang.ping.created");

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

	private static void sendHello() {
		if (BetterPrisonsClient.config == null || !BetterPrisonsClient.config.cosmicApiEnabled) {
			return;
		}
		if (CLIENT_ID.startsWith("REPLACE")) {
			if (!warnedMissingClientId) {
				warnedMissingClientId = true;
				BetterPrisons.LOGGER.info(
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
			BetterPrisons.LOGGER.info(
					"Cosmic API: not sending client_hello (not on Cosmic Prisons; detected={}, canSend=false).",
					ServerContext.detected());
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
		BetterPrisons.LOGGER.info("Cosmic API -> sent client_hello:\n{}", GSON_PRETTY.toJson(hello));
	}

	/**
	 * Handles an inbound channel message. <b>Every</b> received payload is logged in full first (pretty
	 * JSON when parseable, else the raw string) so the API's real message shapes can be observed during
	 * this testing phase. Only the {@code resolve} handshake reply is then acted on (stored + summarised);
	 * any other message (push hooks, action results) is logged but not yet routed into features.
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
			if ("event".equals(type)) {
				handleEvent(obj);
			}
		} catch (Exception e) {
			BetterPrisons.LOGGER.warn("Cosmic API: received a non-JSON / malformed message: {}", e.toString());
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

	/** Logs the complete inbound payload — pretty-printed if it's valid JSON, otherwise the raw text. */
	private static void logFullPayload(String json) {
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

		BetterPrisons.LOGGER.info(
				"Cosmic API resolve: allowed={} testing={} server={} scopes={} hooks={} deniedScopes={} "
				+ "deniedHooks={}{}",
				allowed, testingMode, serverScope, allowedScopes, allowedHooks, deniedScopes, deniedHooks,
				reason.isEmpty() ? "" : " reason=" + reason);
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
