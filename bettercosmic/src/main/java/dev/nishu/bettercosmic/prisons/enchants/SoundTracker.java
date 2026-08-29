package dev.nishu.bettercosmic.prisons.enchants;

/**
 * Per-tick flags for enchant sound detection, set by {@link EnchantSoundListener} and cleared at the
 * end of each client tick:
 * <ul>
 *   <li><b>wither shoot</b> — Powerball's tell (read by {@link PowerballEnchant}).</li>
 *   <li><b>ender dragon growl</b> — Super Breaker's tell (read by {@link SuperBreakerDetector},
 *       combined with a nearby flame/spell particle).</li>
 * </ul>
 * Ported from BetterPrisons' {@code devtools/SoundTracker}.
 */
public final class SoundTracker {

	private static boolean witherShootSoundHeardThisTick = false;
	private static boolean dragonSoundHeardThisTick = false;

	private SoundTracker() {}

	public static void markWitherShootSoundHeard() {
		witherShootSoundHeardThisTick = true;
	}

	public static boolean wasWitherShootSoundHeard() {
		return witherShootSoundHeardThisTick;
	}

	public static void markDragonSoundHeard() {
		dragonSoundHeardThisTick = true;
	}

	public static boolean wasDragonSoundHeard() {
		return dragonSoundHeardThisTick;
	}

	public static void clearTickCache() {
		witherShootSoundHeardThisTick = false;
		dragonSoundHeardThisTick = false;
	}
}
