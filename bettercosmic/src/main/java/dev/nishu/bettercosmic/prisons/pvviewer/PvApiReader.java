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
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Fills the {@link PvVaultStore} from the Cosmic API {@code private_vault.read} action, so the PV
 * viewer's cached previews of vaults the player isn't currently looking at can be refreshed without
 * opening each one in-game.
 *
 * <p>What it does <b>not</b> touch is the vault currently open in the real GUI: the API reflects the
 * last <em>saved</em> vault state (not an open window), and its items are plain — only {@code material},
 * {@code amount} and {@code name}, with no lore/enchant/custom data. So live {@link PvCapture} snapshots
 * stay authoritative for the open vault and are never overwritten by an API read (enforced by
 * {@link PvVaultStore#putFromApi}).
 *
 * <p>Reads happen at two points, both throttled through one queue because the action "costs more of the
 * per-player rate budget":
 * <ul>
 *   <li><b>On the handshake</b> — once per session, after the {@code player.private_vaults:read} scope
 *       resolves, every known vault for the current profile is queued to seed initial state.
 *   <li><b>On a genuine in-game open</b> — when the player opens a {@code /pv} screen themselves (not
 *       the mod's own auto-{@code /pv} navigation, and not the vault they're now viewing live), the
 *       other vaults are refreshed.
 * </ul>
 * A vault is only queued when a refresh would actually help: it's skipped when a full-fidelity live
 * snapshot exists (that data is richer and shouldn't be spent on) or a recent API read is still fresh.
 */
public final class PvApiReader {

	/** Scope that backs the action; without it the server rejects the read. */
	private static final String SCOPE = "player.private_vaults:read";

	/** Minimum gap between two reads, to stay within the per-player rate budget. */
	private static final long SEND_GAP_MS = 800;
	/** How many reads may be awaiting a result at once. */
	private static final int MAX_INFLIGHT = 2;
	/** Drop a pending read whose result never arrived after this long, so its slot frees up. */
	private static final long PENDING_TIMEOUT_MS = 8000;
	/** Don't re-read a vault whose API snapshot is younger than this. */
	private static final long API_REFRESH_TTL_MS = 5 * 60 * 1000L;
	/** Fallback row count when we don't yet know a vault's real size. */
	private static final int DEFAULT_ROWS = 6;

	private record Queued(String profileKey, int vault) {}

	private record Pending(String profileKey, int vault, long sentAt) {}

	private static final Deque<Queued> QUEUE = new ArrayDeque<>();
	private static final Map<String, Pending> PENDING = new HashMap<>();
	/** {@code profileKey#vault} entries currently queued or in flight, so we never double-request one. */
	private static final Set<String> IN_FLIGHT_OR_QUEUED = new HashSet<>();

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

		// Drop stale correlation state across (re)connects — session ids don't carry over.
		String session = CosmicApi.sessionId;
		if (session == null || !session.equals(lastSessionSeen)) {
			lastSessionSeen = session;
			QUEUE.clear();
			PENDING.clear();
			IN_FLIGHT_OR_QUEUED.clear();
		}

		boolean ready = session != null && CosmicApi.allowedScopes.contains(SCOPE);
		if (!ready) {
			wasPvScreen = PvScreens.isPvScreen(client.screen);
			return;
		}

		String profileKey = PvKey.current();
		int openVault = PvScreens.vaultContentsNumber(client.screen);

		// Handshake seed: once per session, but only once we're actually on a planet — PvKey falls back to
		// an "unknown" planet before the tab header is read, and seeding under that bogus key (which caches
		// nothing) would still mark the session seeded and starve the real planet.
		String planet = PlanetDetector.detect();
		boolean onPlanet = planet != null && !planet.isEmpty();
		if (onPlanet && profileKey != null && !session.equals(lastSeededSession)) {
			lastSeededSession = session;
			enqueueRefresh(profileKey, openVault);
		}

		// Genuine in-game open: the player opened a /pv screen themselves (not our auto-/pv), so refresh
		// the other vaults. Fires only on the entering edge, so paging within the viewer doesn't re-queue.
		boolean isPvScreen = PvScreens.isPvScreen(client.screen);
		if (isPvScreen && !wasPvScreen && !PvSidebar.isAutoOpening() && profileKey != null) {
			enqueueRefresh(profileKey, openVault);
		}
		wasPvScreen = isPvScreen;

		expirePending();
		drain();
	}

	/** Queues every known vault worth refreshing for the profile, skipping {@code excludeVault}. */
	private static void enqueueRefresh(String profileKey, int excludeVault) {
		for (int vault : BetterPrisonsClient.pvVaultStore.vaults(profileKey)) {
			if (vault == excludeVault || !worthRefreshing(profileKey, vault)) {
				continue;
			}
			String key = key(profileKey, vault);
			if (IN_FLIGHT_OR_QUEUED.add(key)) {
				QUEUE.add(new Queued(profileKey, vault));
			}
		}
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

	private static void drain() {
		long now = System.currentTimeMillis();
		while (!QUEUE.isEmpty() && PENDING.size() < MAX_INFLIGHT && now - lastSend >= SEND_GAP_MS) {
			Queued q = QUEUE.poll();
			JsonObject payload = new JsonObject();
			payload.addProperty("vaultNumber", q.vault());
			String requestId = CosmicApi.sendAction("private_vault.read", payload);
			if (requestId == null) {
				IN_FLIGHT_OR_QUEUED.remove(key(q.profileKey(), q.vault()));
				return; // session dropped mid-drain; try again next tick
			}
			PENDING.put(requestId, new Pending(q.profileKey(), q.vault(), now));
			lastSend = now;
		}
	}

	private static void expirePending() {
		long now = System.currentTimeMillis();
		PENDING.entrySet().removeIf(e -> {
			if (now - e.getValue().sentAt() > PENDING_TIMEOUT_MS) {
				IN_FLIGHT_OR_QUEUED.remove(key(e.getValue().profileKey(), e.getValue().vault()));
				return true;
			}
			return false;
		});
	}

	// ---- Replies (routed from CosmicApi, on the client thread) ----

	/** An {@code ack}: a rejection (locked/invalid vault) ends the read; an accept awaits the result. */
	public static void onAck(JsonObject obj) {
		boolean accepted = !obj.has("accepted") || obj.get("accepted").getAsBoolean();
		if (accepted) {
			return; // the action_result will follow
		}
		String requestId = str(obj, "requestId");
		Pending p = PENDING.remove(requestId);
		if (p != null) {
			IN_FLIGHT_OR_QUEUED.remove(key(p.profileKey(), p.vault()));
		}
		BetterPrisons.LOGGER.info("Cosmic API: private_vault.read rejected (vault {}): {}",
				p != null ? p.vault() : "?", str(obj, "reason"));
	}

	/** An {@code action_result}: translate the saved vault page and store it (non-clobber). */
	public static void onActionResult(JsonObject obj) {
		String requestId = str(obj, "requestId");
		Pending p = PENDING.remove(requestId);
		if (p != null) {
			IN_FLIGHT_OR_QUEUED.remove(key(p.profileKey(), p.vault()));
		}
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
			return null; // can't encode items without a registry; try again on a later read
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

	private static String key(String profileKey, int vault) {
		return profileKey + "#" + vault;
	}

	private static String str(JsonObject o, String key) {
		return o.has(key) && o.get(key).isJsonPrimitive() ? o.get(key).getAsString() : "";
	}

	private static int asInt(JsonObject o, String key, int def) {
		return o.has(key) && o.get(key).isJsonPrimitive() ? o.get(key).getAsInt() : def;
	}
}
