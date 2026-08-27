package dev.nishu.bettercosmic.shared.notification;

import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

import java.util.List;

/**
 * The shared catalog of selectable notification sounds. Each option is a stable name (used in config
 * and dropdowns) mapped to a vanilla {@link SoundEvent}; {@code "none"} plays nothing.
 *
 * <p>Content-agnostic: the <em>set</em> of notification types and their per-type config live in each
 * mod (e.g. BetterPrisons' {@code NotificationType}); this just resolves a chosen sound name and
 * plays it. Ported from BetterPrisons' {@code Notifications} sound handling (Yarn → Mojang).
 */
public final class Sounds {

	/** Sound options offered in config dropdowns, in display order. {@code "none"} is silent. */
	public static final List<String> OPTIONS =
			List.of("none", "anvil", "bell", "xp_orb", "note_pling", "enchant", "level_up", "ender_eye");

	private Sounds() {}

	/** Resolves an option name to its {@link SoundEvent}, or {@code null} for {@code "none"}/unknown. */
	public static SoundEvent byName(String name) {
		if (name == null) {
			return null;
		}
		return switch (name) {
			case "anvil" -> SoundEvents.ANVIL_LAND;
			case "bell" -> SoundEvents.BELL_BLOCK;
			case "xp_orb" -> SoundEvents.EXPERIENCE_ORB_PICKUP;
			case "note_pling" -> SoundEvents.NOTE_BLOCK_PLING.value();
			case "enchant" -> SoundEvents.ENCHANTMENT_TABLE_USE;
			case "level_up" -> SoundEvents.PLAYER_LEVELUP;
			case "ender_eye" -> SoundEvents.ENDER_EYE_DEATH;
			default -> null; // "none" and anything unrecognized
		};
	}

	/**
	 * Plays the named sound at the given volume (no-op for {@code "none"} or when no player exists).
	 *
	 * @param name    a catalog name from {@link #OPTIONS}
	 * @param volume  linear volume, {@code 1.0f} = 100%
	 * @param pitch   playback pitch, {@code 1.0f} = normal
	 */
	public static void play(String name, float volume, float pitch) {
		Minecraft client = Minecraft.getInstance();
		if (client.player == null) {
			return;
		}
		SoundEvent event = byName(name);
		if (event != null) {
			client.player.playSound(event, volume, pitch);
		}
	}
}
