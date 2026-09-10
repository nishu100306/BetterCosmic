package dev.nishu.bettercosmic.prisons.enchants;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/**
 * Base class for a tracked Cosmic Prisons enchant/effect shown on the Enchant HUD. Holds active
 * state and a countdown; subclasses activate in response to the Cosmic API {@code player.enchant_proc}
 * hook (see {@link #onProc}). Ported from BetterPrisons (Yarn → Mojang).
 */
public abstract class BaseEnchant {

	public String id;
	public String displayName;
	public boolean enabled = true;
	public boolean showOnHud = true;

	public boolean isActive = false;
	public long activatedAt = 0;
	public double durationSeconds = 0;
	/** Formatted display text (with color from item lore), or {@code null} to use {@link #displayName}. */
	public Component displayText = null;

	public BaseEnchant(String id, String displayName) {
		this.id = id;
		this.displayName = displayName;
	}

	/** Per-tick hook; the default expires the effect when its duration elapses. */
	public void tick(Minecraft client) {
		if (isActive && System.currentTimeMillis() > activatedAt + (long) (durationSeconds * 1000.0)) {
			isActive = false;
		}
	}

	/**
	 * Called when the Cosmic API reports this enchant proccing ({@code player.enchant_proc}). Subclasses
	 * activate the effect (and may apply their own gating). {@code displayText} is the server's coloured
	 * label; {@code level} is the reported enchant level (0 if unknown).
	 */
	public void onProc(Component displayText, int level) {
	}

	public void activate(double duration) {
		isActive = true;
		activatedAt = System.currentTimeMillis();
		durationSeconds = duration;
		displayText = null;
	}

	public void activate(double duration, Component displayText) {
		isActive = true;
		activatedAt = System.currentTimeMillis();
		durationSeconds = duration;
		this.displayText = displayText;
	}

	public double getRemainingSeconds() {
		if (!isActive) {
			return 0;
		}
		long elapsed = System.currentTimeMillis() - activatedAt;
		return Math.max(0, durationSeconds - (elapsed / 1000.0));
	}
}
