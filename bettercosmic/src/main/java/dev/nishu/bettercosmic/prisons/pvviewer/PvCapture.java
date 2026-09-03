package dev.nishu.bettercosmic.prisons.pvviewer;

import com.google.gson.JsonElement;
import dev.nishu.bettercosmic.prisons.PrisonsGate;
import dev.nishu.bettercosmic.prisons.client.BetterPrisonsClient;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Watches the open screen each client tick and snapshots any {@code Player Vault #X} window into the
 * {@link PvVaultStore}. Purely client-side and read-only: it copies the container's current contents,
 * it never sends clicks.
 *
 * <p>The vault container's slots are identified by not belonging to the player's own inventory, and the
 * row count is derived from how many such slots exist — so a smaller vault is stored at its real size.
 *
 * <p>The generic {@code Vaults} selector is also handled: opening it enumerates which vaults the player
 * owns (from the icons named {@code Vault N}) and registers an empty placeholder for any not already
 * cached, so unopened vaults show up as known-empty until opened for real.
 *
 * <p>Capture is throttled: after a vault window first appears (or its number changes) it waits a few
 * ticks for the server to sync the contents, then captures — and re-captures periodically while the
 * window stays open, so any rearranging the player does before closing is picked up. Empty vaults are
 * cached too; the settle delay is what prevents a not-yet-synced (transiently empty) window from being
 * stored, so we don't need to drop empty snapshots.
 */
public final class PvCapture {

	private static final int RECAPTURE_INTERVAL_TICKS = 15;
	/** Ticks to wait after a vault window opens before the first capture, so the server can sync it. */
	private static final int SETTLE_TICKS = 5;
	/** Placeholder row count for a not-yet-opened vault (most vaults are 6 rows; corrected on open). */
	private static final int PLACEHOLDER_ROWS = 6;

	private static int lastVault = -1;
	private static int cooldown = 0;
	private static int settle = 0;

	// Selector state: scan the Vaults selector once per opening, after it has settled.
	private static boolean onSelector = false;
	private static int selectorSettle = 0;
	private static boolean selectorScanned = false;

	private PvCapture() {}

	public static void register() {
		ClientTickEvents.END_CLIENT_TICK.register(PvCapture::tick);
	}

	private static void tick(Minecraft client) {
		if (!PrisonsGate.active()
				|| BetterPrisonsClient.config == null
				|| !BetterPrisonsClient.config.pvViewerEnabled
				|| client.player == null) {
			lastVault = -1;
			onSelector = false;
			return;
		}

		// Vaults selector: enumerate owned vaults once, after a settle delay for it to sync.
		if (PvScreens.isVaultSelector(client.screen)) {
			if (!onSelector) {
				onSelector = true;
				selectorScanned = false;
				selectorSettle = SETTLE_TICKS;
			}
			if (!selectorScanned) {
				if (selectorSettle > 0) {
					selectorSettle--;
				} else {
					scanSelector(client, (AbstractContainerScreen<?>) client.screen);
					selectorScanned = true;
				}
			}
		} else {
			onSelector = false;
		}

		int vault = PvScreens.vaultContentsNumber(client.screen);
		if (vault < 0) {
			lastVault = -1;
			return;
		}

		if (vault != lastVault) {
			// Freshly opened (or switched) vault — wait for the server to sync before the first capture.
			lastVault = vault;
			settle = SETTLE_TICKS;
			cooldown = 0;
			return;
		}
		if (settle > 0) {
			settle--;
			return;
		}
		if (cooldown > 0) {
			cooldown--;
			return;
		}
		cooldown = RECAPTURE_INTERVAL_TICKS;

		capture(client, (AbstractContainerScreen<?>) client.screen, vault);
	}

	private static void capture(Minecraft client, AbstractContainerScreen<?> screen, int vault) {
		Container playerInv = client.player.getInventory();
		List<JsonElement> items = new ArrayList<>();

		for (Slot slot : screen.getMenu().slots) {
			if (slot.container == playerInv) {
				continue; // player inventory slots — not part of the vault
			}
			items.add(PvItemCodec.encode(slot.getItem(), client.level.registryAccess()));
		}

		int slots = items.size();
		if (slots == 0 || slots % 9 != 0) {
			return; // not a chest-shaped vault window we understand
		}

		int rows = slots / 9;
		BetterPrisonsClient.pvVaultStore.put(PvKey.current(),
				new PvSnapshot(vault, System.currentTimeMillis(), rows, items));
	}

	/**
	 * Reads the {@code Vaults} selector's icons (each named {@code Vault N}) to learn which vaults the
	 * player owns, and registers an empty placeholder for any not already cached.
	 */
	private static void scanSelector(Minecraft client, AbstractContainerScreen<?> screen) {
		Container playerInv = client.player.getInventory();
		Set<Integer> owned = new HashSet<>();
		for (Slot slot : screen.getMenu().slots) {
			if (slot.container == playerInv) {
				continue;
			}
			ItemStack stack = slot.getItem();
			if (stack.isEmpty()) {
				continue;
			}
			int n = PvScreens.vaultNumberFromLabel(stack.getHoverName().getString());
			if (n > 0) {
				owned.add(n);
			}
		}
		BetterPrisonsClient.pvVaultStore.ensurePlaceholders(PvKey.current(), owned, PLACEHOLDER_ROWS);
	}
}
