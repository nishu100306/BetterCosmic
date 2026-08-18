package dev.nishu.bettercosmic.prisons.enchants;

import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEventListener;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.resources.Identifier;

/**
 * Listens for the wither-shoot sound (Powerball's tell) and flags it on {@link SoundTracker} for the
 * current tick. Registered on the client's {@code SoundManager}. Ported from BetterPrisons'
 * {@code devtools/SoundDebugListener} (reduced to just the Powerball detection).
 */
public final class EnchantSoundListener implements SoundEventListener {

	private static final Identifier WITHER_SHOOT =
			Identifier.fromNamespaceAndPath("minecraft", "entity.wither.shoot");

	@Override
	public void onPlaySound(SoundInstance sound, WeighedSoundEvents soundSet, float range) {
		if (WITHER_SHOOT.equals(sound.getIdentifier())) {
			SoundTracker.markWitherShootSoundHeard();
		}
	}
}
