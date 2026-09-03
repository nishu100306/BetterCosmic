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

	/** No-arg constructor for Gson. */
	public PvSnapshot() {}

	public PvSnapshot(int vault, long capturedAt, int rows, List<JsonElement> items) {
		this.vault = vault;
		this.capturedAt = capturedAt;
		this.rows = rows;
		this.items = items;
	}

	public int columns() {
		return 9;
	}

	public int slotCount() {
		return items == null ? 0 : items.size();
	}
}
