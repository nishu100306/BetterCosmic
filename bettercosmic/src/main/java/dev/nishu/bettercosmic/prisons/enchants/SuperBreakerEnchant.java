package dev.nishu.bettercosmic.prisons.enchants;

import net.minecraft.network.chat.Component;

/**
 * Super Breaker enchant tracking. Activation comes from the Cosmic API {@code player.enchant_proc}
 * hook: each proc (re)activates the effect for a short window, so it stays lit while mining keeps
 * proccing it and fades shortly after the last proc. Ported from BetterPrisons (Yarn → Mojang);
 * the old sound + particle + lore detection was replaced by the API.
 */
public class SuperBreakerEnchant extends BaseEnchant {

	/** How long each proc keeps Super Breaker shown; re-armed on every proc event. */
	private static final double PROC_DURATION_SECONDS = 2.5;

	public SuperBreakerEnchant() {
		super("super_breaker", "Super Breaker");
	}

	@Override
	public void onProc(Component displayText, int level) {
		activate(PROC_DURATION_SECONDS, displayText);
	}
}
