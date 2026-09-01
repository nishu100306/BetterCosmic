package dev.nishu.bettercosmic.prisons.enchants;

import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;

/**
 * Registry and ticker for tracked enchants/effects. Holds the built-in enchants (Super Breaker,
 * Powerball) and feeds the Enchant HUD via {@link #getActiveEnchants()}. Ported from BetterPrisons
 * (Yarn → Mojang).
 */
public class EnchantTracker {

	public final List<BaseEnchant> enchants = new ArrayList<>();

	public EnchantTracker() {
		enchants.add(new SuperBreakerEnchant());
		enchants.add(new PowerballEnchant());
	}

	public void tick(Minecraft client) {
		for (BaseEnchant enchant : enchants) {
			if (enchant.enabled) {
				enchant.tick(client);
			}
		}
	}

	public void onChatMessage(String message) {
		for (BaseEnchant enchant : enchants) {
			if (enchant.enabled) {
				enchant.onChatMessage(message);
			}
		}
	}

	public BaseEnchant getEnchant(String id) {
		for (BaseEnchant e : enchants) {
			if (e.id.equals(id)) {
				return e;
			}
		}
		return null;
	}

	public List<BaseEnchant> getActiveEnchants() {
		List<BaseEnchant> active = new ArrayList<>();
		for (BaseEnchant e : enchants) {
			if (e.isActive) {
				active.add(e);
			}
		}
		return active;
	}
}
