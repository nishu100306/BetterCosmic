package dev.nishu.bettercosmic.prisons.enchants;

/**
 * Per-tick flag for Powerball's wither-shoot sound tell, set by {@link EnchantSoundListener} and
 * cleared at the end of each client tick. (Super Breaker no longer uses sound detection — it is driven
 * by the Cosmic API {@code player.enchant_proc} hook — so only the wither-shoot flag remains here.)
 * Ported from BetterPrisons' {@code devtools/SoundTracker}.
 */
public final class SoundTracker {

	private static boolean witherShootSoundHeardThisTick = false;

	private SoundTracker() {}

	public static void markWitherShootSoundHeard() {
		witherShootSoundHeardThisTick = true;
	}

	public static boolean wasWitherShootSoundHeard() {
		return witherShootSoundHeardThisTick;
	}

	public static void clearTickCache() {
		witherShootSoundHeardThisTick = false;
	}
}
