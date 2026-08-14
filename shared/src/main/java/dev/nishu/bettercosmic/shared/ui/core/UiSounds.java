package dev.nishu.bettercosmic.shared.ui.core;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;

/**
 * Small helper for UI interaction sounds — the vanilla button click, played on committing actions
 * (opening a panel, toggling, choosing, confirming). Kept central so the feel is consistent.
 */
public final class UiSounds {

	private UiSounds() {}

	/** Plays the vanilla UI button click. */
	public static void click() {
		Minecraft mc = Minecraft.getInstance();
		if (mc != null && mc.getSoundManager() != null) {
			mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
		}
	}
}
