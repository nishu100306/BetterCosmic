package dev.nishu.bettercosmic.prisons.pvviewer;

import com.google.gson.JsonElement;

import java.util.List;

/**
 * A cached snapshot of a single player vault, as captured the last time the player had it open. Stored
 * in {@code vaults.json} keyed by player UUID and vault number.
 *
 * <p>{@link #items} is a flat, row-major list of length {@code rows * 9} (vaults are always 9 columns
 * wide); each entry is a {@link PvItemCodec}-encoded stack, with {@code JsonNull} for an empty slot.
 * {@link #rows} is read from the live container at capture time rather than assumed, so smaller vault
 * windows are stored and rendered at their true size.
 */
public final class PvSnapshot {

	/** Vault number as it appeared in the {@code Player Vault #X} title. */
	public int vault;

	/** Epoch millis when this snapshot was taken (drives the "last seen" label). */
	public long capturedAt;

	/** Number of rows in the vault window at capture time. Columns are always 9. */
	public int rows;

	/** Row-major slot contents, length {@code rows * 9}; entries may be JSON null for empty slots. */
	public List<JsonElement> items;

	/** Whether the player has starred this vault (pins it first in the viewer and previews). */
	public boolean favorite;

	/** Live capture from an open vault window (full-fidelity ItemStack data). */
	public static final String SOURCE_LIVE = "live";
	/** Cosmic API {@code private_vault.read} (last saved state; plain items — no lore/enchant/custom data). */
	public static final String SOURCE_API = "api";

	/**
	 * Where this snapshot came from: {@link #SOURCE_LIVE} (open-window scrape, full fidelity) or
	 * {@link #SOURCE_API} (API read, plain items). A {@code null}/absent value means a legacy live
	 * capture (all captures were live before the API reader existed). Placeholders leave this null and
	 * are identified by {@code capturedAt == 0}. Drives the non-clobber policy in {@code PvVaultStore}.
	 */
	public String source;

	/** No-arg constructor for Gson. */
	public PvSnapshot() {}

	public PvSnapshot(int vault, long capturedAt, int rows, List<JsonElement> items) {
		this.vault = vault;
		this.capturedAt = capturedAt;
		this.rows = rows;
		this.items = items;
	}

	public PvSnapshot(int vault, long capturedAt, int rows, List<JsonElement> items, String source) {
		this(vault, capturedAt, rows, items);
		this.source = source;
	}

	/** True for a real live-window capture (including legacy captures with no {@code source} recorded). */
	public boolean isLive() {
		return capturedAt > 0 && !SOURCE_API.equals(source);
	}

	public int columns() {
		return 9;
	}

	public int slotCount() {
		return items == null ? 0 : items.size();
	}

	/** True when the vault holds no items — every slot is empty (also true for an unopened placeholder). */
	public boolean isEmptyContents() {
		if (items == null) {
			return true;
		}
		for (JsonElement item : items) {
			if (item != null && !item.isJsonNull()) {
				return false;
			}
		}
		return true;
	}
}
