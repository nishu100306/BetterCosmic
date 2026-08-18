package dev.nishu.bettercosmic.prisons.enchants;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/**
 * Base class for a tracked Cosmic Prisons enchant/effect shown on the Enchant HUD. Holds active
 * state and a countdown; subclasses detect activation their own way (chat, sound, item lore) and call
 * {@link #activate}. Ported from BetterPrisons (Yarn → Mojang).
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

	/** Chat-based detection hook (override in subclasses). */
	public void onChatMessage(String message) {
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
