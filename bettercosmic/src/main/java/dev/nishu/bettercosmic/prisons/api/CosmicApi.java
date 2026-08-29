package dev.nishu.bettercosmic.prisons.api;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.nishu.bettercosmic.prisons.BetterPrisons;
import dev.nishu.bettercosmic.prisons.client.BetterPrisonsClient;
import dev.nishu.bettercosmic.prisons.enchantprocs.EnchantProc;
import dev.nishu.bettercosmic.prisons.enchantprocs.EnchantProcManager;
import dev.nishu.bettercosmic.prisons.enchants.ApiEffect;
import dev.nishu.bettercosmic.prisons.enchants.BaseEnchant;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Client-side integration with the Cosmic Mods server API (the {@code cosmicapi:main} plugin channel).
 * On joining a server it sends a {@code client_hello} with this app's public clientId and the
 * scopes/hooks it wants, stores the effective access the server grants back, and routes push hooks to
 * the relevant BetterPrisons feature — {@code server.event.schedule.changed} → the Events HUD,
 * {@code player.enchant_proc} → the {@link EnchantProcManager}, {@code player.effects.changed} → the
 * Enchant HUD's effect set.
 *
 * <p>Prison-only (Cosmic Prisons is the sole server exposing this API). Ported from BetterPrisons'
 * {@code api/CosmicApi} (Yarn → Mojang: {@code Text} → {@code Component}, payload {@code ID} → {@code TYPE}).
 *
 * <p>NOTE — two values are app-specific and not derivable from the protocol docs: {@link #CLIENT_ID}
 * (BetterPrisons' approved public client id) and the requested {@link #REQUESTED_SCOPES} /
 * {@link #REQUESTED_HOOKS}.
 */
public final class CosmicApi {

	private static final int PROTOCOL_VERSION = 1;
	private static final String MOD_ID = "betterprisons";

	/** BetterPrisons' approved public client id (from the Cosmic dashboard). */
	private static final String CLIENT_ID = "client_mqo5z17at3zeg17bx1";

	private static final List<String> REQUESTED_SCOPES = List.of(
			"events:read",
			"server.merchants:read",
			"server.meteors:read",
			"player.effects:read");
	private static final List<String> REQUESTED_HOOKS = List.of(
			"server.event.schedule.changed",
			"server.meteor.landing.changed",
			"server.merchant.spawned",
			"server.merchant.despawned",
			"player.enchant_proc",
			"player.effects.changed");

	private static final Gson GSON = new Gson();

	// Effective access granted by the server for this connection.
	public static volatile String sessionId = null;
	public static volatile Set<String> allowedScopes = Set.of();
	public static volatile Set<String> allowedHooks = Set.of();

	private CosmicApi() {}

	public static void register() {
		// Register the payload type both directions.
		PayloadTypeRegistry.playC2S().register(CosmicApiPayload.TYPE, CosmicApiPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(CosmicApiPayload.TYPE, CosmicApiPayload.CODEC);

		// Receive server messages (handshake reply, hook events, ...).
		ClientPlayNetworking.registerGlobalReceiver(CosmicApiPayload.TYPE,
				(payload, context) -> context.client().execute(() -> handleMessage(payload.json())));

		// Send the client_hello once we join a server.
		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
			sessionId = null;
			allowedScopes = Set.of();
			allowedHooks = Set.of();
			sendHello();
		});
	}

	private static void sendHello() {
		if (!dev.nishu.bettercosmic.prisons.PrisonsGate.active()) {
			return; // don't announce ourselves to a non-Prisons server
		}
		if (BetterPrisonsClient.config == null || !BetterPrisonsClient.config.cosmicApiEnabled) {
			return;
		}
		if (CLIENT_ID.startsWith("REPLACE")) {
			return;
		}
		if (!ClientPlayNetworking.canSend(CosmicApiPayload.TYPE)) {
			return;
		}

		JsonObject hello = new JsonObject();
		hello.addProperty("type", "client_hello");
		hello.addProperty("protocolVersion", PROTOCOL_VERSION);
		hello.addProperty("clientId", CLIENT_ID);
		hello.addProperty("modId", MOD_ID);
		hello.addProperty("installId", installId());
		hello.addProperty("modLoader", "fabric");
		hello.addProperty("minecraftVersion", FabricLoader.getInstance()
				.getModContainer("minecraft").map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("unknown"));
		hello.addProperty("modVersion", modVersion());
		hello.add("requestedScopes", GSON.toJsonTree(REQUESTED_SCOPES));
		hello.add("requestedHooks", GSON.toJsonTree(REQUESTED_HOOKS));

		ClientPlayNetworking.send(new CosmicApiPayload(GSON.toJson(hello)));
	}

	private static void handleMessage(String json) {
		try {
			JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
			String type = obj.has("type") ? obj.get("type").getAsString() : "";

			// Push hook event: { "type":"event", "event":"hook", "eventType":..., "payload":... }
			if ("event".equals(type) || obj.has("eventType")) {
				handleHook(obj);
				return;
			}

			// Handshake reply: effective access for this connection.
			if (obj.has("allowedScopes") || obj.has("allowedHooks") || obj.has("allowedHookEvents")) {
				if (obj.has("sessionId")) {
					sessionId = obj.get("sessionId").getAsString();
				}
				allowedScopes = toSet(obj.has("allowedScopes") ? obj.getAsJsonArray("allowedScopes") : null);
				String hooksField = obj.has("allowedHooks") ? "allowedHooks"
						: (obj.has("allowedHookEvents") ? "allowedHookEvents" : null);
				allowedHooks = toSet(hooksField != null ? obj.getAsJsonArray(hooksField) : null);
			}
		} catch (Exception e) {
			// Ignore malformed messages.
		}
	}

	/** Routes a push hook event to the relevant BetterPrisons feature. */
	private static void handleHook(JsonObject obj) {
		String eventType = obj.has("eventType") ? obj.get("eventType").getAsString() : "?";
		JsonElement payload = obj.get("payload");

		if ("server.event.schedule.changed".equals(eventType) && payload != null && payload.isJsonObject()) {
			handleScheduleChanged(payload.getAsJsonObject());
		}

		if ("player.enchant_proc".equals(eventType)) {
			BetterPrisons.LOGGER.info("[CosmicApi] ENCHANT PROC eventType={} payload={}", eventType, payload);
			if (payload != null && payload.isJsonObject()) {
				handleEnchantProc(payload.getAsJsonObject());
			}
		}

		if ("player.effects.changed".equals(eventType) && payload != null && payload.isJsonObject()) {
			handleEffectsChanged(payload.getAsJsonObject());
		}
		// TODO: server.merchant.spawned / server.meteor.landing.changed wiring.
	}

	/**
	 * Handles {@code player.effects.changed} by replacing the Enchant HUD's API-effect set. The exact
	 * payload field names are not yet confirmed, so this parses defensively across the likely shapes;
	 * the raw payload is logged above so the mappings can be corrected once the real shape is observed.
	 */
	private static void handleEffectsChanged(JsonObject envelope) {
		try {
			if (!envelope.has("payload") || !envelope.get("payload").isJsonObject()) {
				return;
			}
			JsonObject data = envelope.getAsJsonObject("payload");
			JsonArray arr = firstArray(data, "effects", "activeEffects", "effectsList", "list");
			List<BaseEnchant> effects = new ArrayList<>();
			if (arr != null) {
				for (JsonElement el : arr) {
					if (!el.isJsonObject()) {
						continue;
					}
					JsonObject e = el.getAsJsonObject();
					String name = firstString(e, "displayName", "name", "effectId", "id", "type");
					if (name == null) {
						continue;
					}
					double remaining = effectRemainingSeconds(e);
					BaseEnchant fx = new ApiEffect("effect:" + name, name);
					fx.activate(remaining, Component.literal(name.replace('&', '§')));
					effects.add(fx);
				}
			}
			BetterPrisonsClient.enchantTracker.setApiEffects(effects);
		} catch (Exception ex) {
			// Ignore malformed effects payloads.
		}
	}

	/** Best-effort remaining-duration extraction; defaults to a long time if unknown. */
	private static double effectRemainingSeconds(JsonObject e) {
		for (String k : new String[]{"remainingMillis", "remainingMs", "durationMillis", "durationMs"}) {
			if (e.has(k)) {
				return Math.max(0, e.get(k).getAsLong() / 1000.0);
			}
		}
		for (String k : new String[]{"remainingSeconds", "durationSeconds", "remaining", "duration"}) {
			if (e.has(k)) {
				return Math.max(0, e.get(k).getAsDouble());
			}
		}
		return 9999; // unknown — persist until the next effects.changed replaces it
	}

	private static String firstString(JsonObject o, String... keys) {
		for (String k : keys) {
			if (o.has(k) && o.get(k).isJsonPrimitive()) {
				return o.get(k).getAsString();
			}
		}
		return null;
	}

	private static JsonArray firstArray(JsonObject o, String... keys) {
		for (String k : keys) {
			if (o.has(k) && o.get(k).isJsonArray()) {
				return o.getAsJsonArray(k);
			}
		}
		return null;
	}

	/**
	 * Parses a {@code player.enchant_proc} hook into an {@link EnchantProc} and hands it to the
	 * {@link EnchantProcManager}. The proc details are nested under {@code payload.payload}:
	 * {@code { source, level, displayName, enchantId }}, while {@code playerName} sits on this envelope.
	 * {@code displayName} carries legacy colour codes ({@code &}/{@code §}); normalised to {@code §} here.
	 */
	private static void handleEnchantProc(JsonObject envelope) {
		try {
			if (!envelope.has("payload") || !envelope.get("payload").isJsonObject()) {
				return;
			}
			JsonObject data = envelope.getAsJsonObject("payload");
			String enchantId = data.has("enchantId") ? data.get("enchantId").getAsString() : "unknown";
			String displayName = data.has("displayName") ? data.get("displayName").getAsString() : enchantId;
			int level = data.has("level") && data.get("level").isJsonPrimitive() ? data.get("level").getAsInt() : 0;
			String source = data.has("source") ? data.get("source").getAsString() : "";
			String playerName = envelope.has("playerName") ? envelope.get("playerName").getAsString() : "";

			Component text = Component.literal(displayName.replace('&', '§'));
			EnchantProcManager.handle(new EnchantProc(enchantId, text, level, source, playerName));
		} catch (Exception ex) {
			// Ignore malformed enchant proc payloads.
		}
	}

	/**
	 * Parses {@code server.event.schedule.changed} and forwards the whitelisted entries to the Events
	 * HUD. The envelope nests the data one level deep: {@code payload.payload = { serverTimeMillis,
	 * events:[ { eventId, name, status, enabled, nextStartAtMillis }, ... ] }}.
	 */
	private static void handleScheduleChanged(JsonObject envelope) {
		try {
			if (!envelope.has("payload") || !envelope.get("payload").isJsonObject()) {
				return;
			}
			JsonObject data = envelope.getAsJsonObject("payload");
			if (!data.has("events") || !data.get("events").isJsonArray()) {
				return;
			}
			long serverTime = data.has("serverTimeMillis")
					? data.get("serverTimeMillis").getAsLong() : System.currentTimeMillis();
			long now = System.currentTimeMillis();
			JsonArray events = data.getAsJsonArray("events");

			for (JsonElement el : events) {
				if (!el.isJsonObject()) {
					continue;
				}
				JsonObject e = el.getAsJsonObject();
				if (!e.has("eventId")) {
					continue;
				}
				String id = e.get("eventId").getAsString();
				String name = e.has("name") ? e.get("name").getAsString() : id;
				String status = e.has("status") ? e.get("status").getAsString() : "";
				boolean enabled = e.has("enabled") && e.get("enabled").getAsBoolean();
				long nextStart = e.has("nextStartAtMillis") ? e.get("nextStartAtMillis").getAsLong() : 0L;
				boolean active = "active".equalsIgnoreCase(status) || (enabled && nextStart == 0L);

				// Anchor the countdown to the client clock using the server-relative delta, so it stays
				// correct regardless of clock skew. Ignore sentinel/disabled times.
				long displayUntil = 0L;
				if (enabled && nextStart > 0L && nextStart < Long.MAX_VALUE) {
					displayUntil = now + (nextStart - serverTime);
				}
				BetterPrisonsClient.eventsHud.updateScheduledEvent(id, name, enabled, active, displayUntil);
			}
		} catch (Exception ex) {
			// Ignore malformed schedule payloads.
		}
	}

	private static Set<String> toSet(JsonArray arr) {
		Set<String> out = new HashSet<>();
		if (arr != null) {
			for (JsonElement el : arr) {
				out.add(el.getAsString());
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

	private static String modVersion() {
		return FabricLoader.getInstance().getModContainer(MOD_ID)
				.map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("unknown");
	}
}
