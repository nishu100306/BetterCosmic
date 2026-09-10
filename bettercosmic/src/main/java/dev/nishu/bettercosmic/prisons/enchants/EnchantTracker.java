package dev.nishu.bettercosmic.prisons.enchants;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Registry and ticker for tracked enchants/effects. Holds the built-in enchants (Super Breaker,
 * Powerball) and feeds the Enchant HUD via {@link #getActiveEnchants()}. Activation is driven by the
 * Cosmic API {@code player.enchant_proc} hook, dispatched through {@link #onEnchantProc}. Ported from
 * BetterPrisons (Yarn → Mojang).
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

	/**
	 * Dispatches a {@code player.enchant_proc} hook to the matching enchant. The server's {@code
	 * enchantId} (e.g. {@code "superbreaker"}, {@code "powerball"}) is matched against each enchant's id
	 * after normalising both (lower-case, non-alphanumerics stripped) so {@code super_breaker} matches
	 * {@code superbreaker}. Unknown ids are ignored.
	 */
	public void onEnchantProc(String enchantId, Component displayText, int level) {
		String target = normalize(enchantId);
		for (BaseEnchant enchant : enchants) {
			if (enchant.enabled && normalize(enchant.id).equals(target)) {
				enchant.onProc(displayText, level);
				return;
			}
		}
	}

	private static String normalize(String s) {
		return s == null ? "" : s.toLowerCase().replaceAll("[^a-z0-9]", "");
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
