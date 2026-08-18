package dev.nishu.bettercosmic.shared.notification;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * Content-agnostic notification engine: plays a chosen sound and optionally shows a vanilla
 * title/subtitle or a {@link ToastRenderer} card. It carries no notion of <em>which</em>
 * notifications exist — the consuming mod owns that catalog (enable/sound/volume per type) and calls
 * these methods with the resolved values.
 *
 * <p>Ported from BetterPrisons' {@code Notifications} (Yarn → Mojang); the prison-specific
 * {@code NotificationType} registry and its config maps stay in {@code :betterprisons}.
 */
public final class Notifier {

	private Notifier() {}

	/** Plays a sound only. {@code volume} is linear (1.0 = 100%); {@code "none"} is silent. */
	public static void sound(String soundName, float volume) {
		Sounds.play(soundName, volume, 1.0f);
	}

	/**
	 * Shows a vanilla title (and optional subtitle) on screen, then plays a sound.
	 *
	 * @param title    the title line (required)
	 * @param subtitle the subtitle line, or {@code null} for none
	 */
	public static void title(Component title, Component subtitle, String soundName, float volume) {
		Minecraft client = Minecraft.getInstance();
		if (client.player == null) {
			return;
		}
		if (title != null && client.gui != null) {
			client.gui.setTitle(title);
			client.gui.setSubtitle(subtitle != null ? subtitle : Component.empty());
			client.gui.setTimes(5, 40, 10); // fadeIn, stay, fadeOut (ticks)
		}
		Sounds.play(soundName, volume, 1.0f);
	}

	/** Shows a toast card (with optional item icon), then plays a sound. */
	public static void toast(Component title, Component description, ItemStack icon,
							 long durationMs, String soundName, float volume) {
		Minecraft client = Minecraft.getInstance();
		if (client.player == null) {
			return;
		}
		ToastRenderer.show(title, description, icon, durationMs);
		Sounds.play(soundName, volume, 1.0f);
	}
}
