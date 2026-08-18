package dev.nishu.bettercosmic.prisons.enchants;

/**
 * Per-tick flag set by {@link EnchantSoundListener} when the wither-shoot sound plays, read by
 * {@link PowerballEnchant} to detect a Powerball activation. Cleared at the end of each client tick.
 * Ported from BetterPrisons' {@code devtools/SoundTracker} (kept the wither-shoot flag; the unused
 * dragon-sound flag was dropped).
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
