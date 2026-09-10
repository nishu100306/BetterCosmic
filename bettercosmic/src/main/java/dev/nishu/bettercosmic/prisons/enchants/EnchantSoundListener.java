package dev.nishu.bettercosmic.prisons.enchants;

import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEventListener;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.resources.Identifier;

/**
 * Flags Powerball's wither-shoot "tell" sound on {@link SoundTracker} for the current tick. Registered
 * on the client's {@code SoundManager}. (Super Breaker's dragon-growl detection was dropped when it
 * moved to the Cosmic API {@code player.enchant_proc} hook.) Ported from BetterPrisons'
 * {@code devtools/SoundDebugListener} (reduced to the Powerball detection).
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
