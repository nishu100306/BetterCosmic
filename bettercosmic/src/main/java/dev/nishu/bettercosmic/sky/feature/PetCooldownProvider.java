package dev.nishu.bettercosmic.sky.feature;

import dev.nishu.bettercosmic.shared.easyview.Anchor;
import dev.nishu.bettercosmic.shared.easyview.ItemOverlayProvider;
import dev.nishu.bettercosmic.shared.easyview.SlotOverlay;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.UseCooldown;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <b>STUB.</b> EasyView provider that draws a centered {@code m:ss} cooldown timer on Cosmic Sky
 * pets — green while the pet's effect is active, red while it's on cooldown.
 *
 * <p>Templated from BetterPrisons' pet path in
 * {@link dev.nishu.bettercosmic.prisons.easyview.ItemCooldownProvider} (the {@code isPet} /
 * {@code petOverlay} logic): identify pets by name, read the {@code use_cooldown} component plus a
 * {@code *_last_use_ms} timestamp stashed in custom data (which survives relogs/deaths), and render a
 * live timer through the shared EasyView framework (so it works in containers and the hotbar for
 * free). The mechanics below are ported verbatim; only the Cosmic-Sky-specific <em>values</em> are
 * unknown.
 *
 * <p><b>Inert until confirmed + wired.</b> {@link #getOverlay} short-circuits on {@link #STUB_ENABLED}
 * (currently {@code false}) and this provider is not registered anywhere, so it does nothing in-game
 * yet. To finish it:
 * <ol>
 *   <li><b>Confirm Sky's pet identity.</b> {@link #isPet} uses BetterPrisons' {@code " Pet [LVL "}
 *       name marker as a placeholder — verify Cosmic Sky's pet display-name format (or a
 *       {@code custom_data} tag, the way {@link TrinketChargesProvider} keys off
 *       {@code cosmicItem:"potion_trinket"}) and update the check.</li>
 *   <li><b>Confirm the NBT key.</b> {@link #PET_LAST_USE_KEY} guesses {@code cosmicsky:pet_last_use_ms}
 *       under {@code PublicBukkitValues} — verify against a real Sky pet's NBT dump. Also confirm Sky
 *       pets actually carry a {@code use_cooldown} component and that the active-effect duration is in
 *       lore in the {@code "1m 30s duration"} shape {@link #DURATION_PATTERN} expects.</li>
 *   <li><b>Add config + colors.</b> Replace the {@code COOLDOWN_COLOR} / {@code ACTIVE_COLOR} /
 *       {@code BOLD} placeholders with {@code SkyConfig} fields + a Trinkets-style panel group
 *       (mirroring {@code itemCooldownsPet*} in {@code PrisonsConfig} / {@code EasyViewPanel}).</li>
 *   <li><b>Register it.</b> Add {@code EasyView.register(new PetCooldownProvider(), Network.SKY);} in
 *       {@code BetterSkyClient#onInitializeClient} (next to the TrinketChargesProvider registration),
 *       then flip {@link #STUB_ENABLED} (or drop it for the real config gate).</li>
 * </ol>
 */
public final class PetCooldownProvider implements ItemOverlayProvider {

	/** While {@code false} the provider is inert. Flip once Sky specifics are confirmed + wired. */
	private static final boolean STUB_ENABLED = false;

	/** Text scale for the centered timer (matches BetterPrisons' vanilla-count-sized 0.5). */
	private static final float SCALE = 0.5f;

	// TODO(sky): move these to SkyConfig once the feature is implemented (see class javadoc).
	private static final int COOLDOWN_COLOR = 0xFF5555; // red while on cooldown
	private static final int ACTIVE_COLOR = 0x00FF00;   // green while the effect is active
	private static final boolean BOLD = true;

	// TODO(sky): verify against a real Cosmic Sky pet — display-name marker and custom-data key.
	private static final String PET_NAME_MARKER = " Pet [LVL ";
	private static final String PET_LAST_USE_KEY = "cosmicsky:pet_last_use_ms";

	/** Matches pet-lore lines like " 1m duration", " 30s duration", " 1m 30s duration". */
	private static final Pattern DURATION_PATTERN =
			Pattern.compile("^\\s*(?:(\\d+)m)?\\s*(?:(\\d+)s)?\\s+duration$");

	@Override
	public SlotOverlay getOverlay(ItemStack stack) {
		if (!STUB_ENABLED || stack.isEmpty() || Minecraft.getInstance().player == null) {
			return null;
		}
		try {
			String name = stack.getHoverName().getString();
			if (isPet(name)) {
				return petOverlay(stack);
			}
		} catch (Exception e) {
			// a malformed item must never break slot rendering
		}
		return null;
	}

	private static boolean isPet(String name) {
		return name.contains(PET_NAME_MARKER);
	}

	/**
	 * While the effect is active, show a green duration timer; otherwise show the cooldown timer. Both
	 * are anchored off {@code pet_last_use_ms} so they survive relogs/deaths.
	 */
	private static SlotOverlay petOverlay(ItemStack stack) {
		UseCooldown cooldown = stack.get(DataComponents.USE_COOLDOWN);
		if (cooldown == null) {
			return null;
		}
		long lastUseMs = getLastUseMs(stack, PET_LAST_USE_KEY);
		if (lastUseMs <= 0) {
			return null;
		}

		long now = System.currentTimeMillis();
		float remainingCooldown = (lastUseMs + (long) (cooldown.seconds() * 1000) - now) / 1000.0f;
		if (remainingCooldown <= 0) {
			return null; // cooldown finished
		}

		int durationSeconds = parseDurationFromLore(stack);
		if (durationSeconds > 0) {
			float remainingDuration = durationSeconds - (now - lastUseMs) / 1000.0f;
			if (remainingDuration > 0) {
				return overlay(formatTime(remainingDuration), ACTIVE_COLOR); // effect still active
			}
		}
		return overlay(formatTime(remainingCooldown), COOLDOWN_COLOR); // expired / no duration
	}

	private static SlotOverlay overlay(String text, int rgb) {
		return new SlotOverlay(text, 0xFF000000 | (rgb & 0xFFFFFF), SCALE, BOLD, Anchor.CENTER);
	}

	/** Formats seconds remaining as {@code m:ss}. */
	private static String formatTime(float seconds) {
		int total = (int) seconds;
		return String.format("%d:%02d", total / 60, total % 60);
	}

	/** Active duration in seconds from a pet's lore ({@code " 1m 30s duration"}), or 0 if absent. */
	private static int parseDurationFromLore(ItemStack stack) {
		ItemLore lore = stack.get(DataComponents.LORE);
		if (lore == null) {
			return 0;
		}
		for (Component line : lore.lines()) {
			Matcher matcher = DURATION_PATTERN.matcher(line.getString());
			if (matcher.matches()) {
				return minutes(matcher.group(1)) + seconds(matcher.group(2));
			}
		}
		return 0;
	}

	private static int minutes(String group) {
		return group != null ? Integer.parseInt(group) * 60 : 0;
	}

	private static int seconds(String group) {
		return group != null ? Integer.parseInt(group) : 0;
	}

	/**
	 * Reads a {@code *_last_use_ms} timestamp from the item's custom data
	 * ({@code custom_data → PublicBukkitValues → key}). Returns 0 if absent.
	 */
	private static long getLastUseMs(ItemStack stack, String key) {
		CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
		if (customData == null) {
			return 0;
		}
		CompoundTag bukkit = customData.copyTag().getCompound("PublicBukkitValues").orElse(null);
		if (bukkit == null || bukkit.isEmpty()) {
			return 0;
		}
		return bukkit.getLongOr(key, 0L);
	}
}
