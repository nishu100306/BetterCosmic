package dev.nishu.bettercosmic.prisons.enchants;

import dev.nishu.bettercosmic.prisons.client.BetterPrisonsClient;
import dev.nishu.bettercosmic.shared.util.ItemUtils;
import net.minecraft.network.chat.Component;

/**
 * Detects Super Breaker activation client-side. Cosmic Prisons emits an ender-dragon growl sound plus
 * flame/spell particles at the player while Super Breaker is proccing; this correlates the two: the
 * {@code SuperBreakerParticleMixin} reports nearby flame/spell particles here, and each tick
 * {@link #evaluate()} activates the Super Breaker enchant (2.5s) when the closest particle is within
 * 0.75 blocks and the dragon growl was heard this tick. Re-activating each tick keeps it up while the
 * proc's particles/sound continue.
 *
 * <p>Ported from BetterPrisons' misleadingly-named {@code devtools/ParticleDebugTracker} — this is the
 * real Super Breaker activation logic, not debug tooling.
 */
public final class SuperBreakerDetector {

	private static double closestDistance = Double.MAX_VALUE;

	private SuperBreakerDetector() {}

	/** Reports a candidate flame/spell particle near the player (called from the particle mixin). */
	public static void considerParticle(double distance) {
		if (distance > 3.0) {
			return;
		}
		if (distance < closestDistance) {
			closestDistance = distance;
		}
	}

	/**
	 * Activates Super Breaker if this tick's closest particle is within 0.75 blocks and the dragon
	 * growl was heard, then resets for the next tick. Call once per client tick (before the sound
	 * flags are cleared).
	 */
	public static void evaluate() {
		if (closestDistance <= 0.75 && SoundTracker.wasDragonSoundHeard()) {
			BaseEnchant superBreaker = BetterPrisonsClient.enchantTracker.getEnchant("super_breaker");
			if (superBreaker != null) {
				Component enchantText = ItemUtils.extractLoreLineFromHeldItem("Super Breaker");
				if (enchantText != null) {
					superBreaker.activate(2.5, enchantText);
				} else {
					superBreaker.activate(2.5);
				}
			}
		}
		closestDistance = Double.MAX_VALUE;
	}
}
