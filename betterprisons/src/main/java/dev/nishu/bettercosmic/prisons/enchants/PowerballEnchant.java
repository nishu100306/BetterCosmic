package dev.nishu.bettercosmic.prisons.enchants;

import dev.nishu.bettercosmic.prisons.client.BetterPrisonsClient;
import dev.nishu.bettercosmic.prisons.notification.NotificationType;
import dev.nishu.bettercosmic.prisons.notification.Notifications;
import dev.nishu.bettercosmic.shared.util.ItemUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;

/**
 * Powerball enchant tracking. Activation is detected by the wither-shoot sound (see
 * {@link EnchantSoundListener}/{@link SoundTracker}) fired within 2s of a right-click while holding a
 * Powerball pickaxe; the duration comes from the enchant level in the pickaxe lore. When the cooldown
 * elapses it fires the "Powerball Ready" alert. Ported from BetterPrisons (Yarn → Mojang).
 */
public class PowerballEnchant extends BaseEnchant {

	private long lastRightClickTime = 0;
	private boolean wasActiveLastTick = false;

	public PowerballEnchant() {
		super("powerball", "Powerball");
	}

	@Override
	public void tick(Minecraft client) {
		boolean wasActive = wasActiveLastTick;
		super.tick(client);
		if (wasActive && !isActive) {
			firePowerballReadyAlert(client);
		}
		wasActiveLastTick = isActive;

		if (client.player == null) {
			return;
		}
		if (client.options.keyUse.isDown()) {
			lastRightClickTime = System.currentTimeMillis();
		}
		if (SoundTracker.wasWitherShootSoundHeard()) {
			ItemStack heldItem = client.player.getMainHandItem();
			long timeSinceRightClick = System.currentTimeMillis() - lastRightClickTime;
			if (heldItem.getItem().getDescriptionId().contains("pickaxe") && timeSinceRightClick <= 2000) {
				onWitherSoundDetected(heldItem);
			}
		}
	}

	private void firePowerballReadyAlert(Minecraft client) {
		if (client.player == null) {
			return;
		}
		if (BetterPrisonsClient.config.powerballAlertTitleEnabled) {
			int rgb = BetterPrisonsClient.config.powerballAlertTitleColor & 0xFFFFFF;
			Component title = Component.literal(BetterPrisonsClient.config.powerballAlertTitleText)
					.setStyle(Style.EMPTY.withColor(TextColor.fromRgb(rgb)).withBold(true));
			Notifications.trigger(NotificationType.POWERBALL, title);
		} else {
			Notifications.trigger(NotificationType.POWERBALL);
		}
	}

	/** Activates Powerball from the held pickaxe: duration by level, display text from lore. */
	public void onWitherSoundDetected(ItemStack pickaxe) {
		if (pickaxe == null || pickaxe.isEmpty()) {
			return;
		}
		int level = getPowerballLevel(pickaxe);
		int duration = switch (level) {
			case 1 -> 60;
			case 2 -> 50;
			case 3 -> 40;
			default -> 0;
		};
		if (duration == 0) {
			return;
		}
		Component enchantText = ItemUtils.extractLoreLineFromHeldItem("Powerball");
		if (enchantText != null) {
			activate(duration, enchantText);
		} else {
			activate(duration);
		}
	}

	/** Reads the Powerball level (1-3) from pickaxe lore, or 0 if not present. */
	private int getPowerballLevel(ItemStack pickaxe) {
		try {
			ItemLore lore = pickaxe.get(DataComponents.LORE);
			if (lore == null || lore.lines().isEmpty()) {
				return 0;
			}
			for (Component line : lore.lines()) {
				String lineText = line.getString().toLowerCase().replaceAll("§.", "");
				if (lineText.contains("powerball")) {
					if (lineText.contains("iii")) {
						return 3;
					}
					if (lineText.contains("ii")) {
						return 2;
					}
					if (lineText.contains("i")) {
						return 1;
					}
					if (lineText.contains("3")) {
						return 3;
					}
					if (lineText.contains("2")) {
						return 2;
					}
					if (lineText.contains("1")) {
						return 1;
					}
				}
			}
		} catch (Exception e) {
			// ignore parse errors
		}
		return 0;
	}

	@Override
	public void onChatMessage(String message) {
		// Powerball is detected via sound, not chat.
	}
}
