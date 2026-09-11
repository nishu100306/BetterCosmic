package dev.nishu.bettercosmic.sky.feature;

import dev.nishu.bettercosmic.shared.easyview.Anchor;
import dev.nishu.bettercosmic.shared.easyview.ItemOverlayProvider;
import dev.nishu.bettercosmic.shared.easyview.SlotOverlay;
import dev.nishu.bettercosmic.sky.client.BetterSkyClient;
import dev.nishu.bettercosmic.sky.config.SkyConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * EasyView provider that draws a centered {@code m:ss} timer on Cosmic Sky pets — green while the
 * ability's effect is active, red while the pet is on cooldown. Rendered through the shared EasyView
 * framework (CENTER), so it shows in containers and the hotbar for free.
 *
 * <p>Cosmic Sky pets are {@code minecraft:player_head}s whose {@code custom_data} carries
 * {@code persistentItem:"inventory_pet"} (plus {@code petType}, {@code level}, {@code exp}, and a
 * {@code lastUsed} epoch-ms timestamp). Unlike BetterPrisons pets, they have <b>no</b>
 * {@code use_cooldown} component and store nothing under {@code PublicBukkitValues}, so the timing is:
 * <ul>
 *   <li><b>NBT (authoritative):</b> identity from {@code custom_data.persistentItem} and the last-use
 *       instant from {@code custom_data.lastUsed} (survives relogs/deaths).</li>
 *   <li><b>Lore (no NBT equivalent):</b> the cooldown length is only printed under the "Cooldown"
 *       header (e.g. {@code "1 Hour"}), and the active-effect length only in the ability text
 *       (e.g. {@code "...for 15m."}). Both are parsed from lore because the item exposes them nowhere
 *       else.</li>
 * </ul>
 */
public final class PetCooldownProvider implements ItemOverlayProvider {

	/** Text scale for the centered timer (matches the vanilla-count-sized 0.5 used elsewhere). */
	private static final float SCALE = 0.5f;

	private static final String PERSISTENT_ITEM_KEY = "persistentItem";
	private static final String PET_PERSISTENT_ITEM = "inventory_pet";
	private static final String LAST_USED_KEY = "lastUsed";

	/** Cooldown length, printed as "<n> Hour(s)/Minute(s)/Second(s)" on the line after "Cooldown". */
	private static final Pattern COOLDOWN_HOURS = Pattern.compile("(\\d+)\\s*Hour", Pattern.CASE_INSENSITIVE);
	private static final Pattern COOLDOWN_MINUTES = Pattern.compile("(\\d+)\\s*Minute", Pattern.CASE_INSENSITIVE);
	private static final Pattern COOLDOWN_SECONDS = Pattern.compile("(\\d+)\\s*Second", Pattern.CASE_INSENSITIVE);

	/** Active-effect length from the ability text, e.g. "for 15m" / "for 1h 30m". */
	private static final Pattern ACTIVE_SPAN =
			Pattern.compile("for\\s+((?:\\d+\\s*[hms]\\s*)+)", Pattern.CASE_INSENSITIVE);
	private static final Pattern SPAN_HOURS = Pattern.compile("(\\d+)\\s*h", Pattern.CASE_INSENSITIVE);
	private static final Pattern SPAN_MINUTES = Pattern.compile("(\\d+)\\s*m", Pattern.CASE_INSENSITIVE);
	private static final Pattern SPAN_SECONDS = Pattern.compile("(\\d+)\\s*s", Pattern.CASE_INSENSITIVE);

	@Override
	public SlotOverlay getOverlay(ItemStack stack) {
		SkyConfig cfg = BetterSkyClient.config;
		if (cfg == null || !cfg.petCooldownOverlay || stack.isEmpty()) {
			return null;
		}
		if (Minecraft.getInstance().player == null) {
			return null;
		}

		try {
			CompoundTag data = customData(stack);
			if (data == null || !PET_PERSISTENT_ITEM.equals(data.getStringOr(PERSISTENT_ITEM_KEY, ""))) {
				return null; // not an inventory pet
			}

			long lastUsed = data.getLongOr(LAST_USED_KEY, 0L);
			if (lastUsed <= 0) {
				return null;
			}

			int cooldownSeconds = parseCooldownFromLore(stack);
			if (cooldownSeconds <= 0) {
				return null; // no cooldown printed — nothing to time
			}

			long now = System.currentTimeMillis();
			float remainingCooldown = (lastUsed + cooldownSeconds * 1000L - now) / 1000.0f;
			if (remainingCooldown <= 0) {
				return null; // ready
			}

			// While the ability effect is still running, show a green active timer instead.
			int activeSeconds = parseActiveFromLore(stack);
			if (activeSeconds > 0) {
				float remainingActive = activeSeconds - (now - lastUsed) / 1000.0f;
				if (remainingActive > 0) {
					return overlay(formatTime(remainingActive), cfg.petActiveColor, cfg.petCooldownBold);
				}
			}
			return overlay(formatTime(remainingCooldown), cfg.petCooldownColor, cfg.petCooldownBold);
		} catch (Exception e) {
			// a malformed item must never break slot rendering
			return null;
		}
	}

	private static SlotOverlay overlay(String text, int rgb, boolean bold) {
		return new SlotOverlay(text, 0xFF000000 | (rgb & 0xFFFFFF), SCALE, bold, Anchor.CENTER);
	}

	/** Formats seconds remaining as {@code m:ss} (minutes grow past 60 for hour-long cooldowns). */
	private static String formatTime(float seconds) {
		int total = (int) seconds;
		return String.format("%d:%02d", total / 60, total % 60);
	}

	/** Cooldown length in seconds, from the lore line following the "Cooldown" header. 0 if absent. */
	private static int parseCooldownFromLore(ItemStack stack) {
		ItemLore lore = stack.get(DataComponents.LORE);
		if (lore == null) {
			return 0;
		}
		List<Component> lines = lore.lines();
		for (int i = 0; i < lines.size() - 1; i++) {
			if (lines.get(i).getString().trim().equalsIgnoreCase("Cooldown")) {
				return parseWordyDuration(lines.get(i + 1).getString());
			}
		}
		return 0;
	}

	/** Sums "<n> Hour/Minute/Second" tokens in a line into seconds. */
	private static int parseWordyDuration(String text) {
		int total = 0;
		Matcher h = COOLDOWN_HOURS.matcher(text);
		if (h.find()) {
			total += Integer.parseInt(h.group(1)) * 3600;
		}
		Matcher m = COOLDOWN_MINUTES.matcher(text);
		if (m.find()) {
			total += Integer.parseInt(m.group(1)) * 60;
		}
		Matcher s = COOLDOWN_SECONDS.matcher(text);
		if (s.find()) {
			total += Integer.parseInt(s.group(1));
		}
		return total;
	}

	/** Active-effect length in seconds, parsed from the ability's "for <dur>" text. 0 if absent. */
	private static int parseActiveFromLore(ItemStack stack) {
		ItemLore lore = stack.get(DataComponents.LORE);
		if (lore == null) {
			return 0;
		}
		for (Component line : lore.lines()) {
			Matcher span = ACTIVE_SPAN.matcher(line.getString());
			if (span.find()) {
				String dur = span.group(1);
				int total = 0;
				Matcher h = SPAN_HOURS.matcher(dur);
				if (h.find()) {
					total += Integer.parseInt(h.group(1)) * 3600;
				}
				Matcher m = SPAN_MINUTES.matcher(dur);
				if (m.find()) {
					total += Integer.parseInt(m.group(1)) * 60;
				}
				Matcher s = SPAN_SECONDS.matcher(dur);
				if (s.find()) {
					total += Integer.parseInt(s.group(1));
				}
				if (total > 0) {
					return total;
				}
			}
		}
		return 0;
	}

	/** The item's root {@code custom_data} tag, or {@code null} if absent. */
	private static CompoundTag customData(ItemStack stack) {
		CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
		return customData == null ? null : customData.copyTag();
	}
}
