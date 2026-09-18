package dev.nishu.bettercosmic.prisons.pvviewer;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import dev.nishu.bettercosmic.prisons.BetterPrisons;
import dev.nishu.bettercosmic.prisons.PrisonsGate;
import dev.nishu.bettercosmic.prisons.api.CosmicApi;
import dev.nishu.bettercosmic.prisons.client.BetterPrisonsClient;
import dev.nishu.bettercosmic.prisons.planet.PlanetDetector;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Fills the {@link PvVaultStore} from the Cosmic API {@code private_vault.read} action, so the PV
 * viewer's cached previews of vaults the player isn't currently looking at can be refreshed — and, on
 * top of that, discovered: the API has no "list my vaults" call, so ownership is learned by <b>probing
 * vault numbers in order (1, 2, 3, …)</b>. Locked vaults ({@code vault_locked}) can sit <em>between</em>
 * owned ones (observed: 13 locked, 14 owned), and the server never actually returns {@code invalid_vault}
 * to mark the end — it just keeps saying {@code vault_locked} past the last owned vault. So the walk
 * treats a single lock as a gap and continues, and stops only after {@value #MAX_CONSECUTIVE_LOCKS}
 * locks in a row (the real end-of-owned signal, which also bounds a 0-owned account).
 *
 * <p>What it never touches is the vault currently open in the real GUI: the API reflects the last
 * <em>saved</em> vault state (not an open window), and its items are plain — only {@code material},
 * {@code amount} and {@code name}, with no lore/enchant/custom data. So live {@link PvCapture} snapshots
 * stay authoritative for the open vault and are never overwritten by an API read (enforced by
 * {@link PvVaultStore#putFromApi}).
 *
 * <p>A probe is kicked at two points:
 * <ul>
 *   <li><b>On the handshake</b> — once per session, after the {@code player.private_vaults:read} scope
 *       resolves and the player is on a real planet.
 *   <li><b>On {@code /pv}</b> — when the player opens a vault screen themselves (the selector or a
 *       {@code /pv <n>} window), but not the mod's own auto-{@code /pv} navigation.
 * </ul>
 * The probe is strictly sequential (one request in flight, waiting for each ack before sending the
 * next) so the {@code invalid_vault} stop is clean, and it is throttled because the action "costs more
 * of the per-player rate budget." Numbers whose data is already fresh (a live capture, or a recent API
 * read) and the live-open vault are stepped over without a network call, so a re-probe is cheap.
 */
public final class PvApiReader {

	/** Scope that backs the action; without it the server rejects the read. */
	private static final String SCOPE = "player.private_vaults:read";
	/** Ack reason meaning the number is past the last vault — a hard end of the probe. */
	private static final String REASON_INVALID_VAULT = "invalid_vault";
	/** Ack reason meaning the vault isn't unlocked. Locks can be interspersed among owned vaults. */
	private static final String REASON_VAULT_LOCKED = "vault_locked";

	/** Minimum gap between two reads, to stay within the per-player rate budget. */
	private static final long SEND_GAP_MS = 800;
	/** Abandon the probe if an ack never arrives after this long, so a lost packet can't hang it. */
	private static final long ACK_TIMEOUT_MS = 8000;
	/** Don't re-read a vault whose API snapshot is younger than this. */
	private static final long API_REFRESH_TTL_MS = 5 * 60 * 1000L;
	/**
	 * Stop the walk after this many locked vaults in a row. Locked vaults appear both interspersed among
	 * owned ones (observed: vault 13 locked between owned 12 and 14) and as the tail past the last owned
	 * vault — and the server never returns {@code invalid_vault} in practice, it just keeps saying
	 * {@code vault_locked}. So a run of locks is the real "end of owned vaults" signal; a single lock is
	 * just a gap and the walk continues. This also bounds an all-locked account (0 owned) to this many
	 * reads instead of grinding to {@link #MAX_PROBE_VAULT}.
	 */
	private static final int MAX_CONSECUTIVE_LOCKS = 3;
	/** Hard safety cap on how far a probe will walk, in case neither stop condition fires. */
	private static final int MAX_PROBE_VAULT = 200;
	/** Fallback row count when we don't yet know a vault's real size. */
	private static final int DEFAULT_ROWS = 6;

	private record Pending(String profileKey, int vault, long sentAt) {}

	/** requestId -> the read it belongs to, for correlating acks and results. */
	private static final Map<String, Pending> PENDING = new HashMap<>();

	// Probe state (one sequential walk at a time).
	private static boolean probing = false;
	private static String probeProfile = null;
	private static int probeVault = 1;
	private static int consecutiveLocks = 0; // run of vault_locked acks; a known/owned vault resets it
	private static String probeAwaitingAck = null; // requestId we're waiting on; null when free to send
	private static long probeAckSentAt = 0;

	private static long lastSend = 0;
	private static String lastSeededSession = null;
	private static String lastSessionSeen = null;
	private static boolean wasPvScreen = false;

	private PvApiReader() {}

	public static void register() {
		ClientTickEvents.END_CLIENT_TICK.register(PvApiReader::tick);
	}

	private static void tick(Minecraft client) {
		if (!PrisonsGate.active()
				|| BetterPrisonsClient.config == null
				|| !BetterPrisonsClient.config.pvViewerEnabled
				|| client.player == null) {
			wasPvScreen = false;
			return;
		}

		// Drop stale state across (re)connects — session ids and pending correlations don't carry over.
		String session = CosmicApi.sessionId;
		if (session == null || !session.equals(lastSessionSeen)) {
			lastSessionSeen = session;
			resetProbe();
			PENDING.clear();
		}

		if (session == null || !CosmicApi.allowedScopes.contains(SCOPE)) {
			wasPvScreen = PvScreens.isPvScreen(client.screen);
			return;
		}

		String profileKey = PvKey.current();
		int openVault = PvScreens.vaultContentsNumber(client.screen);

		// Handshake seed: once per session, but only once we're actually on a planet — PvKey falls back to
		// an "unknown" planet before the tab header is read, and seeding under that bogus key would still
		// mark the session seeded and starve the real planet.
		String planet = PlanetDetector.detect();
		boolean onPlanet = planet != null && !planet.isEmpty();
		if (onPlanet && profileKey != null && !session.equals(lastSeededSession)) {
			lastSeededSession = session;
			startProbe(profileKey);
		}

		// /pv (or /pv <n>): the player opened a vault screen themselves (not our auto-/pv). Fires on the
		// entering edge so it kicks a fresh probe once per open, not every tick a vault GUI is up. The
		// vault they're now viewing live is stepped over inside the probe.
		boolean isPvScreen = PvScreens.isPvScreen(client.screen);
		if (isPvScreen && !wasPvScreen && !PvSidebar.isAutoOpening() && profileKey != null) {
			startProbe(profileKey);
		}
		wasPvScreen = isPvScreen;

		// Give up on a probe whose ack was lost, rather than stalling forever.
		if (probeAwaitingAck != null && System.currentTimeMillis() - probeAckSentAt > ACK_TIMEOUT_MS) {
			PENDING.remove(probeAwaitingAck);
			probeAwaitingAck = null;
			probing = false;
		}

		driveProbe(profileKey, openVault);
	}

	/** (Re)starts the sequential probe at vault 1 for {@code profileKey}. */
	private static void startProbe(String profileKey) {
		probing = true;
		probeProfile = profileKey;
		probeVault = 1;
		consecutiveLocks = 0;
	}

	private static void resetProbe() {
		probing = false;
		probeProfile = null;
		probeVault = 1;
		consecutiveLocks = 0;
		probeAwaitingAck = null;
	}

	/** Sends the next probe read, stepping over vaults that don't need one, throttled and one-at-a-time. */
	private static void driveProbe(String profileKey, int openVault) {
		if (!probing) {
			return;
		}
		if (probeProfile == null || !probeProfile.equals(profileKey)) {
			resetProbe(); // planet/profile changed under us
			return;
		}
		if (probeAwaitingAck != null) {
			return; // wait for the in-flight ack before advancing, so the stop stays clean
		}
		// Step over numbers we don't need to hit the network for: the live-open vault, and any vault whose
		// data is already fresh. Both mean a known/owned vault, so they break any run of locks — otherwise
		// a fresh owned vault sitting between two locked ones wouldn't reset the consecutive-lock count.
		while (probing && probeVault <= MAX_PROBE_VAULT
				&& (probeVault == openVault || !worthRefreshing(probeProfile, probeVault))) {
			consecutiveLocks = 0;
			probeVault++;
		}
		if (probeVault > MAX_PROBE_VAULT) {
			probing = false;
			return;
		}
		if (System.currentTimeMillis() - lastSend < SEND_GAP_MS) {
			return;
		}
		JsonObject payload = new JsonObject();
		payload.addProperty("vaultNumber", probeVault);
		String requestId = CosmicApi.sendAction("private_vault.read", payload);
		if (requestId == null) {
			probing = false; // session dropped; a later trigger will restart
			return;
		}
		long now = System.currentTimeMillis();
		PENDING.put(requestId, new Pending(probeProfile, probeVault, now));
		probeAwaitingAck = requestId;
		probeAckSentAt = now;
		lastSend = now;
	}

	/** A refresh helps only for an absent/placeholder vault or an aged API read — never over live data. */
	private static boolean worthRefreshing(String profileKey, int vault) {
		PvSnapshot snap = BetterPrisonsClient.pvVaultStore.get(profileKey, vault);
		if (snap == null || snap.capturedAt == 0) {
			return true; // never read, or an empty placeholder
		}
		if (snap.isLive()) {
			return false; // keep the richer live capture; don't spend budget on it
		}
		return System.currentTimeMillis() - snap.capturedAt > API_REFRESH_TTL_MS; // stale API read
	}

	// ---- Replies (routed from CosmicApi, on the client thread) ----

	/**
	 * An {@code ack}. Drives the probe: an accept (or {@code vault_locked}) advances to the next number;
	 * {@code invalid_vault} or any other rejection stops the walk. A non-probe ack is ignored here.
	 */
	public static void onAck(JsonObject obj) {
		String requestId = str(obj, "requestId");
		boolean accepted = !obj.has("accepted") || obj.get("accepted").getAsBoolean();
		boolean isCurrentProbe = requestId.equals(probeAwaitingAck);
		Pending p = PENDING.get(requestId);

		if (accepted) {
			if (isCurrentProbe) {
				probeAwaitingAck = null;
				consecutiveLocks = 0; // an owned vault breaks the lock run
				probeVault = (p != null ? p.vault() : probeVault) + 1;
			}
			return; // keep PENDING; the action_result follows
		}

		// Rejected — no result will come.
		PENDING.remove(requestId);
		String reason = str(obj, "reason");
		if (!isCurrentProbe) {
			return;
		}
		probeAwaitingAck = null;
		if (REASON_VAULT_LOCKED.equals(reason)) {
			// A single locked vault is just a gap (e.g. 13 between owned 12 and 14) — keep walking. Only a
			// run of locks means we're past the owned range; the server never sends invalid_vault to say so.
			consecutiveLocks++;
			if (consecutiveLocks >= MAX_CONSECUTIVE_LOCKS) {
				probing = false;
			} else {
				probeVault = (p != null ? p.vault() : probeVault) + 1;
			}
		} else {
			probing = false; // invalid_vault (hard end) or a systemic rejection (e.g. scope/rate)
			if (!REASON_INVALID_VAULT.equals(reason)) {
				BetterPrisons.LOGGER.info("Cosmic API: private_vault.read probe stopped (vault {}): {}",
						p != null ? p.vault() : "?", reason);
			}
		}
	}

	/** An {@code action_result}: translate the saved vault page and store it (non-clobber). */
	public static void onActionResult(JsonObject obj) {
		String requestId = str(obj, "requestId");
		Pending p = PENDING.remove(requestId);
		boolean accepted = !obj.has("accepted") || obj.get("accepted").getAsBoolean();
		if (!accepted) {
			BetterPrisons.LOGGER.info("Cosmic API: private_vault.read failed (vault {}): {}",
					p != null ? p.vault() : "?", str(obj, "reason"));
			return;
		}
		if (!obj.has("payload") || !obj.get("payload").isJsonObject()) {
			return;
		}
		String profileKey = p != null ? p.profileKey() : PvKey.current();
		PvSnapshot snap = buildSnapshot(profileKey, obj.getAsJsonObject("payload"));
		if (snap != null) {
			BetterPrisonsClient.pvVaultStore.putFromApi(profileKey, snap);
		}
	}

	/** Builds a snapshot from a {@code private_vault.read} result payload, or {@code null} if unusable. */
	private static PvSnapshot buildSnapshot(String profileKey, JsonObject payload) {
		int vault = asInt(payload, "vaultNumber", -1);
		if (vault < 0) {
			return null;
		}
		Minecraft mc = Minecraft.getInstance();
		RegistryAccess registries = mc.level != null ? mc.level.registryAccess() : null;
		if (registries == null) {
			return null; // can't encode items without a registry; a later read will retry
		}

		List<JsonObject> itemObjs = new ArrayList<>();
		int maxSlot = -1;
		if (payload.has("items") && payload.get("items").isJsonArray()) {
			JsonArray items = payload.getAsJsonArray("items");
			for (JsonElement el : items) {
				if (!el.isJsonObject()) {
					continue;
				}
				JsonObject io = el.getAsJsonObject();
				maxSlot = Math.max(maxSlot, asInt(io, "slot", -1));
				itemObjs.add(io);
			}
		}

		int knownRows = BetterPrisonsClient.pvVaultStore.rowsFor(profileKey, vault, DEFAULT_ROWS);
		int rows = Math.max(knownRows, maxSlot < 0 ? 0 : (maxSlot / 9) + 1);
		if (rows <= 0) {
			rows = DEFAULT_ROWS;
		}
		int size = rows * 9;

		List<JsonElement> slots = new ArrayList<>(Collections.nCopies(size, (JsonElement) JsonNull.INSTANCE));
		for (JsonObject io : itemObjs) {
			int slot = asInt(io, "slot", -1);
			if (slot < 0 || slot >= size) {
				continue;
			}
			slots.set(slot, PvItemCodec.encode(toStack(io, registries), registries));
		}
		return new PvSnapshot(vault, parseAsOf(payload), rows, slots, PvSnapshot.SOURCE_API);
	}

	/** Rebuilds a plain {@link ItemStack} from an API item entry ({@code material}, {@code amount}, {@code name}). */
	private static ItemStack toStack(JsonObject io, RegistryAccess registries) {
		String material = str(io, "material");
		int amount = asInt(io, "amount", 1);
		String name = str(io, "name");
		ItemStack stack = new ItemStack(itemOrDefault(material), Math.max(1, amount));
		if (!name.isEmpty()) {
			// The API label may carry legacy colour codes; normalise & -> § so it renders.
			stack.set(DataComponents.CUSTOM_NAME, Component.literal(name.replace('&', '§')));
		}
		return stack;
	}

	/** Resolves a Bukkit material name (e.g. {@code DIAMOND_PICKAXE}) to an item; paper for the unknown. */
	private static Item itemOrDefault(String material) {
		if (material == null || material.isEmpty()) {
			return Items.PAPER;
		}
		try {
			Identifier id = Identifier.tryParse("minecraft:" + material.toLowerCase(Locale.ROOT));
			if (id != null) {
				Item item = BuiltInRegistries.ITEM.getValue(id); // DefaultedRegistry returns AIR for unknown ids
				if (item != Items.AIR) {
					return item;
				}
			}
		} catch (Exception ignored) {
			// fall through to the placeholder
		}
		return Items.PAPER;
	}

	private static long parseAsOf(JsonObject payload) {
		if (payload.has("asOf") && payload.get("asOf").isJsonPrimitive()) {
			JsonPrimitive prim = payload.getAsJsonPrimitive("asOf");
			try {
				return prim.isNumber() ? prim.getAsLong() : Instant.parse(prim.getAsString()).toEpochMilli();
			} catch (Exception ignored) {
				// fall through to now
			}
		}
		return System.currentTimeMillis();
	}

	private static String str(JsonObject o, String key) {
		return o.has(key) && o.get(key).isJsonPrimitive() ? o.get(key).getAsString() : "";
	}

	private static int asInt(JsonObject o, String key, int def) {
		return o.has(key) && o.get(key).isJsonPrimitive() ? o.get(key).getAsInt() : def;
	}
}
