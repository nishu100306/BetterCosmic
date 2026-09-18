package dev.nishu.bettercosmic.prisons.pvviewer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.reflect.TypeToken;
import dev.nishu.bettercosmic.prisons.BetterPrisons;

import java.io.File;
import java.io.FileReader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * On-disk cache of captured player vaults, persisted to
 * {@code config/bettercosmic/betterprisons/vaults.json}. Keyed by {@link PvKey} ({@code
 * <planet>/<username>} — each planet has its own vault storage, and alt accounts don't collide) then
 * by vault number. The viewer reads snapshots from here; {@link PvCapture} writes them as vaults are
 * opened in-game. Follows the same atomic-write pattern as {@code WaypointManager}.
 */
public final class PvVaultStore {

	private static final File FILE =
			new File("config/bettercosmic/betterprisons/vaults.json");
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
	private static final Type TYPE =
			new TypeToken<Map<String, TreeMap<Integer, PvSnapshot>>>() {}.getType();

	/** {@code <planet>/<username>} -> (vault number -> snapshot), sorted by vault number. */
	private final Map<String, TreeMap<Integer, PvSnapshot>> byProfile = new HashMap<>();

	// ---- Persistence ----

	public void load() {
		byProfile.clear();
		if (!FILE.exists()) {
			return;
		}
		try (FileReader reader = new FileReader(FILE)) {
			Map<String, TreeMap<Integer, PvSnapshot>> loaded = GSON.fromJson(reader, TYPE);
			if (loaded != null) {
				byProfile.putAll(loaded);
			}
		} catch (Exception e) {
			BetterPrisons.LOGGER.error("[PvViewer] Failed to read {} — starting empty", FILE, e);
		}
	}

	public void save() {
		try {
			Path target = FILE.toPath();
			Files.createDirectories(target.getParent());
			Path tmp = target.resolveSibling(target.getFileName() + ".tmp");
			try (Writer writer = Files.newBufferedWriter(tmp)) {
				GSON.toJson(byProfile, TYPE, writer);
			}
			try {
				Files.move(tmp, target,
						StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
			} catch (AtomicMoveNotSupportedException e) {
				Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
			}
		} catch (Exception e) {
			BetterPrisons.LOGGER.error("[PvViewer] Failed to save {}", FILE, e);
		}
	}

	// ---- Access ----
	// profileKey is a PvKey ("<planet>/<username>"); a null key (no player) reads as empty / no-ops.

	/** Stores (replacing) a snapshot under the given profile key and persists to disk. */
	public void put(String profileKey, PvSnapshot snapshot) {
		if (profileKey == null) {
			return;
		}
		TreeMap<Integer, PvSnapshot> map = byProfile.computeIfAbsent(profileKey, k -> new TreeMap<>());
		PvSnapshot existing = map.get(snapshot.vault);
		if (existing != null && existing.favorite) {
			snapshot.favorite = true; // don't lose a star when the vault is re-captured on open
		}
		map.put(snapshot.vault, snapshot);
		save();
	}

	/**
	 * Stores a snapshot read from the Cosmic API ({@code private_vault.read}), applying the non-clobber
	 * policy: an API read is lower fidelity (no lore/enchant/custom data) and reflects the last <em>saved</em>
	 * state, so it must never overwrite a full-fidelity live capture. It is written only when the vault is
	 * absent, an empty placeholder ({@code capturedAt == 0}), or a previous API read — never over a live
	 * snapshot. The favorite flag is carried over. Returns whether it was written.
	 */
	public boolean putFromApi(String profileKey, PvSnapshot snapshot) {
		if (profileKey == null) {
			return false;
		}
		TreeMap<Integer, PvSnapshot> map = byProfile.computeIfAbsent(profileKey, k -> new TreeMap<>());
		PvSnapshot existing = map.get(snapshot.vault);
		if (existing != null && existing.isLive()) {
			return false; // keep the richer live capture
		}
		if (existing != null && existing.favorite) {
			snapshot.favorite = true;
		}
		snapshot.source = PvSnapshot.SOURCE_API;
		map.put(snapshot.vault, snapshot);
		save();
		return true;
	}

	/** Row count to render an API snapshot at: the existing snapshot's rows if known, else {@code defaultRows}. */
	public int rowsFor(String profileKey, int vault, int defaultRows) {
		PvSnapshot existing = get(profileKey, vault);
		return existing != null && existing.rows > 0 ? existing.rows : defaultRows;
	}

	/** Flips the favorite (starred) flag on a vault and persists. No-op if the vault isn't cached. */
	public void toggleFavorite(String profileKey, int vault) {
		PvSnapshot snap = get(profileKey, vault);
		if (snap != null) {
			snap.favorite = !snap.favorite;
			save();
		}
	}

	public boolean isFavorite(String profileKey, int vault) {
		PvSnapshot snap = get(profileKey, vault);
		return snap != null && snap.favorite;
	}

	/**
	 * Ensures every listed vault exists in the cache, adding an empty placeholder snapshot
	 * ({@code capturedAt == 0}, {@code rows} rows of air) for any not already present. Existing
	 * snapshots (real or placeholder) are left untouched. Saves once if anything was added.
	 *
	 * <p>Used when the {@code Vaults} selector is opened: it tells us which vaults the player owns, so
	 * unopened ones can be shown as known-empty until they're actually opened and captured for real.
	 */
	public void ensurePlaceholders(String profileKey, Collection<Integer> vaultNumbers, int rows) {
		if (profileKey == null || vaultNumbers.isEmpty()) {
			return;
		}
		TreeMap<Integer, PvSnapshot> map = byProfile.computeIfAbsent(profileKey, k -> new TreeMap<>());
		boolean changed = false;
		for (int vault : vaultNumbers) {
			if (!map.containsKey(vault)) {
				List<JsonElement> empty = new ArrayList<>(Collections.nCopies(rows * 9, (JsonElement) JsonNull.INSTANCE));
				map.put(vault, new PvSnapshot(vault, 0L, rows, empty));
				changed = true;
			}
		}
		if (changed) {
			save();
		}
	}

	/** The snapshot for one vault, or {@code null} if never captured. */
	public PvSnapshot get(String profileKey, int vault) {
		TreeMap<Integer, PvSnapshot> vaults = profileKey == null ? null : byProfile.get(profileKey);
		return vaults == null ? null : vaults.get(vault);
	}

	/** The captured vault numbers for a profile: favorites first, each group ascending. Empty if none. */
	public List<Integer> vaults(String profileKey) {
		return vaults(profileKey, true);
	}

	/**
	 * The captured vault numbers for a profile: favorites first, each group ascending. When
	 * {@code includeEmpty} is false, vaults with no items (including unopened placeholders) are dropped —
	 * except starred ones, which the player asked to keep in view. Empty if none.
	 */
	public List<Integer> vaults(String profileKey, boolean includeEmpty) {
		TreeMap<Integer, PvSnapshot> vaults = profileKey == null ? null : byProfile.get(profileKey);
		if (vaults == null) {
			return List.of();
		}
		List<Integer> favorites = new ArrayList<>();
		List<Integer> rest = new ArrayList<>();
		for (Map.Entry<Integer, PvSnapshot> e : vaults.entrySet()) {
			PvSnapshot snap = e.getValue();
			if (!includeEmpty && !snap.favorite && snap.isEmptyContents()) {
				continue;
			}
			(snap.favorite ? favorites : rest).add(e.getKey());
		}
		favorites.addAll(rest);
		return favorites;
	}

	/** Removes every cached vault for a profile and persists. Used by the "clear cache" action. */
	public void clear(String profileKey) {
		if (profileKey != null && byProfile.remove(profileKey) != null) {
			save();
		}
	}
}
