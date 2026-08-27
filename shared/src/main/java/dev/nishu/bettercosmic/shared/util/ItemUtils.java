package dev.nishu.bettercosmic.shared.util;

import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;

/**
 * Small content-agnostic helpers for inspecting the player's held item — reading lore lines and
 * detecting the held tool by its item type. Ported from BetterPrisons' {@code utils/ItemUtils}
 * (Yarn → Mojang); nothing here is server-specific, so it lives in the shared library.
 */
public final class ItemUtils {

	private ItemUtils() {}

	/**
	 * Returns the first lore line of the player's main-hand item that contains {@code searchString},
	 * preserving its formatting, or {@code null} if there is no such line (or nothing is held).
	 *
	 * @param searchString substring to look for (e.g. an enchant name like {@code "Super Breaker"})
	 */
	public static Component extractLoreLineFromHeldItem(String searchString) {
		Minecraft client = Minecraft.getInstance();
		if (client.player == null) {
			return null;
		}

		ItemStack heldItem = client.player.getMainHandItem();
		if (heldItem.isEmpty()) {
			return null;
		}

		ItemLore lore = heldItem.get(DataComponents.LORE);
		if (lore == null) {
			return null;
		}

		for (Component line : lore.lines()) {
			if (line.getString().contains(searchString)) {
				return line; // return the formatted line, colour intact
			}
		}
		return null;
	}

	/** @return whether the player's main-hand item is a pickaxe (by item type id). */
	public static boolean isHoldingPickaxe() {
		Minecraft client = Minecraft.getInstance();
		if (client.player == null) {
			return false;
		}
		ItemStack heldItem = client.player.getMainHandItem();
		if (heldItem.isEmpty()) {
			return false;
		}
		return heldItem.getItem().getDescriptionId().toLowerCase().contains("pickaxe");
	}
}
