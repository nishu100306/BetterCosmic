package dev.nishu.bettercosmic.prisons.notification;

import dev.nishu.bettercosmic.prisons.client.BetterPrisonsClient;
import dev.nishu.bettercosmic.prisons.config.PrisonsConfig;
import dev.nishu.bettercosmic.shared.notification.Notifier;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * BetterPrisons' notification facade: resolves per-{@link NotificationType} settings (enable / sound
 * / volume) from {@link PrisonsConfig} and hands them to the shared {@link Notifier} engine. The
 * catalog and its config storage are prison-side; sound resolution and playback are shared.
 *
 * <p>Ported from BetterPrisons' {@code notification/Notifications}.
 */
public final class Notifications {

	private Notifications() {}

	public static boolean isEnabled(NotificationType type) {
		PrisonsConfig cfg = BetterPrisonsClient.config;
		return cfg.notificationEnabled.getOrDefault(type.id, type.defaultEnabled);
	}

	public static String getSound(NotificationType type) {
		PrisonsConfig cfg = BetterPrisonsClient.config;
		return cfg.notificationSound.getOrDefault(type.id, type.defaultSound);
	}

	public static int getVolume(NotificationType type) {
		PrisonsConfig cfg = BetterPrisonsClient.config;
		return cfg.notificationVolume.getOrDefault(type.id, type.defaultVolume);
	}

	/** Plays the type's configured sound (if enabled). */
	public static void trigger(NotificationType type) {
		if (isEnabled(type)) {
			Notifier.sound(getSound(type), getVolume(type) / 100.0f);
		}
	}

	/** Shows a vanilla title + plays the sound (if enabled). */
	public static void trigger(NotificationType type, Component title) {
		if (isEnabled(type)) {
			Notifier.title(title, null, getSound(type), getVolume(type) / 100.0f);
		}
	}

	/** Shows a toast card with an item icon + plays the sound (if enabled). */
	public static void toast(NotificationType type, Component title, Component description, ItemStack icon) {
		if (isEnabled(type)) {
			Notifier.toast(title, description, icon, 4000L, getSound(type), getVolume(type) / 100.0f);
		}
	}
}
