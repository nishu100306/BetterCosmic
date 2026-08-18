package dev.nishu.bettercosmic.prisons.enchants;

import dev.nishu.bettercosmic.shared.util.ItemUtils;
import net.minecraft.client.Minecraft;

/**
 * Super Breaker enchant tracking. While active it stays up only as long as the player holds a pickaxe
 * with the Super Breaker enchant. Ported from BetterPrisons (Yarn → Mojang).
 *
 * <p>Note: the chat activation pattern is empty in BetterPrisons ("filled in after server testing"),
 * so this never activates from chat yet — the Enchant HUD entry and Super Breaker Aura are dormant
 * until an activation trigger is added upstream. Ported faithfully.
 */
public class SuperBreakerEnchant extends BaseEnchant {

	public String activationPattern = "";

	public SuperBreakerEnchant() {
		super("super_breaker", "Super Breaker");
	}

	@Override
	public void tick(Minecraft client) {
		if (isActive) {
			boolean holdingValidPickaxe = ItemUtils.isHoldingPickaxe()
					&& ItemUtils.extractLoreLineFromHeldItem("Super Breaker") != null;
			if (!holdingValidPickaxe) {
				isActive = false;
				return;
			}
		}
		super.tick(client);
	}

	@Override
	public void onChatMessage(String message) {
		EnchantParsing parsing = new EnchantParsing();
		if (parsing.messageMatches(message, activationPattern)) {
			int duration = parsing.parseSecondsFromMessage(message, activationPattern);
			if (duration > 0) {
				activate(duration);
				if (showOnHud) {
					parsing.startCooldown(displayName, duration);
				}
			}
		}
	}
}
