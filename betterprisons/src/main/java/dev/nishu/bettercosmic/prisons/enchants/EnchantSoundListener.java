package dev.nishu.bettercosmic.prisons.enchants;

import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEventListener;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.resources.Identifier;

/**
 * Flags the two enchant "tell" sounds on {@link SoundTracker} for the current tick: the wither-shoot
 * sound (Powerball) and the ender-dragon growl (Super Breaker, when paired with a nearby particle).
 * Registered on the client's {@code SoundManager}. Ported from BetterPrisons'
 * {@code devtools/SoundDebugListener} (reduced to the enchant detections).
 */
public final class EnchantSoundListener implements SoundEventListener {

	private static final Identifier WITHER_SHOOT =
			Identifier.fromNamespaceAndPath("minecraft", "entity.wither.shoot");
	private static final Identifier DRAGON_GROWL =
			Identifier.fromNamespaceAndPath("minecraft", "entity.ender_dragon.growl");

	@Override
	public void onPlaySound(SoundInstance sound, WeighedSoundEvents soundSet, float range) {
		Identifier id = sound.getIdentifier();
		if (WITHER_SHOOT.equals(id)) {
			SoundTracker.markWitherShootSoundHeard();
		} else if (DRAGON_GROWL.equals(id)) {
			SoundTracker.markDragonSoundHeard();
		}
	}
}
