package dev.nishu.bettercosmic.prisons.enchants;

import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;

/**
 * Registry and ticker for tracked enchants/effects. Holds the built-in enchants (Super Breaker,
 * Powerball) plus a live set of API-driven player effects (replaced wholesale by the
 * {@code player.effects.changed} hook). Feeds the Enchant HUD via {@link #getActiveEnchants()}.
 * Ported from BetterPrisons (Yarn → Mojang).
 */
public class EnchantTracker {

	public final List<BaseEnchant> enchants = new ArrayList<>();
	private final List<BaseEnchant> apiEffects = new ArrayList<>();

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
		apiEffects.removeIf(e -> {
			e.tick(client);
			return !e.isActive;
		});
	}

	/** Replaces the current API-driven player effects (from {@code player.effects.changed}). */
	public void setApiEffects(List<BaseEnchant> effects) {
		apiEffects.clear();
		apiEffects.addAll(effects);
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
		for (BaseEnchant e : apiEffects) {
			if (e.isActive) {
				active.add(e);
			}
		}
		return active;
	}
}
