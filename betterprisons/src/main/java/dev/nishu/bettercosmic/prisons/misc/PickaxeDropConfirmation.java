package dev.nishu.bettercosmic.prisons.misc;

import dev.nishu.bettercosmic.prisons.client.BetterPrisonsClient;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * Guards against accidentally dropping a pickaxe from the hotbar: either blocks it outright, or
 * requires a second drop press within a 3-second window to confirm. Ported from BetterPrisons'
 * {@code misc/PickaxeDropConfirmation} (Yarn → Mojang). Driven by {@code ItemDropMixin} (the drop key)
 * and ticked to expire the confirmation window.
 */
public final class PickaxeDropConfirmation {

	private static final long CONFIRMATION_WINDOW_MS = 3000;
	private long lastDropAttemptTime = 0;

	/** @return true if the drop should proceed, false to block it (disabled or awaiting confirmation). */
	public boolean canDrop(ItemStack stack) {
		if (!BetterPrisonsClient.config.pickaxeDropConfirmationEnabled) {
			return true;
		}
		if (!isPickaxe(stack)) {
			return true;
		}
		if (BetterPrisonsClient.config.pickaxeDropBlockEnabled) {
			showMessage("§c§l[!] §cPickaxe dropping is disabled.");
			return false;
		}
		long now = System.currentTimeMillis();
		if (now - lastDropAttemptTime < CONFIRMATION_WINDOW_MS) {
			lastDropAttemptTime = 0; // confirmed
			return true;
		}
		lastDropAttemptTime = now;
		showMessage("§e§l[!] §6Are you sure you want to drop your pickaxe? Press drop again to confirm.");
		return false;
	}

	/** Expires the confirmation window. Call each client tick. */
	public void tick() {
		if (lastDropAttemptTime > 0 && System.currentTimeMillis() - lastDropAttemptTime >= CONFIRMATION_WINDOW_MS) {
			lastDropAttemptTime = 0;
		}
	}

	private static void showMessage(String text) {
		Minecraft client = Minecraft.getInstance();
		if (client.player != null) {
			client.player.displayClientMessage(Component.literal(text), false);
		}
	}

	private static boolean isPickaxe(ItemStack stack) {
		return !stack.isEmpty() && stack.getItem().toString().toLowerCase().contains("pickaxe");
	}
}
