package dev.nishu.bettercosmic.prisons.easyview;

import dev.nishu.bettercosmic.prisons.client.BetterPrisonsClient;
import dev.nishu.bettercosmic.prisons.config.PrisonsConfig;
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
 * Item Cooldown overlay: draws a live {@code m:ss} timer centered on Cosmic Prisons pets, trinkets,
 * and bandit boxes so their cooldown / active-effect / unlock time is visible without opening the
 * tooltip. Implemented as a shared EasyView {@link ItemOverlayProvider} (CENTER), so it renders in
 * containers and the hotbar for free. Ported from BetterPrisons' {@code misc/ItemCooldownOverlay}
 * (Yarn → Mojang; per-slot rendering now goes through EasyView instead of BP's three screen mixins).
 *
 * <p>Pet/trinket cooldowns are computed from the item's {@code use_cooldown} component plus a
 * {@code *_last_use_ms} timestamp stashed in custom data (persists across world switches and death);
 * pets additionally show a green active-effect timer parsed from their lore. Bandit boxes parse the
 * remaining unlock time from lore while unlocking.
 */
public final class ItemCooldownProvider implements ItemOverlayProvider {

	/** Text scale for the centered timer (matches BetterPrisons' vanilla-count-sized 0.5). */
	private static final float SCALE = 0.5f;

	/** Matches pet-lore lines like " 1m duration", " 30s duration", " 1m 30s duration". */
	private static final Pattern DURATION_PATTERN =
			Pattern.compile("^\\s*(?:(\\d+)m)?\\s*(?:(\\d+)s)?\\s+duration$");

	/** Matches "Time Left: 6m 57s" / "Time to Unlock: 8m 0s" in bandit-box lore. */
	private static final Pattern TIME_LEFT_PATTERN =
			Pattern.compile("Time Left:\\s*(?:(\\d+)m)?\\s*(?:(\\d+)s)?");

	@Override
	public SlotOverlay getOverlay(ItemStack stack) {
		PrisonsConfig cfg = BetterPrisonsClient.config;
		if (cfg == null || !cfg.itemCooldownsEnabled || stack.isEmpty()) {
			return null;
		}
		if (Minecraft.getInstance().player == null) {
			return null;
		}

		try {
			String name = stack.getHoverName().getString();

			if (isPet(name)) {
				return cfg.itemCooldownsPetEnabled ? petOverlay(stack, cfg) : null;
			}
			if (isTrinket(name)) {
				return cfg.itemCooldownsTrinketEnabled ? trinketOverlay(stack, cfg) : null;
			}
			if (isBanditBox(name)) {
				return cfg.itemCooldownsBanditBoxEnabled ? banditBoxOverlay(stack, name, cfg) : null;
			}
		} catch (Exception e) {
			// a malformed item must never break slot rendering
		}
		return null;
	}

	private static boolean isPet(String name) {
		return name.contains(" Pet [LVL ");
	}

	private static boolean isTrinket(String name) {
		return name.contains(" Trinket (");
	}

	private static boolean isBanditBox(String name) {
		return name.startsWith("Bandit Box:");
	}

	/**
	 * Pet: while the effect is active, show a green duration timer; otherwise show the cooldown timer.
	 * Both are anchored off {@code pet_last_use_ms} so they survive relogs/deaths.
	 */
	private static SlotOverlay petOverlay(ItemStack stack, PrisonsConfig cfg) {
		UseCooldown cooldown = stack.get(DataComponents.USE_COOLDOWN);
		if (cooldown == null) {
			return null;
		}
		long lastUseMs = getLastUseMs(stack, "cosmicprisons:pet_last_use_ms");
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
				// Effect still active — green active timer.
				return overlay(formatTime(remainingDuration),
						cfg.itemCooldownsPetActiveColor, cfg.itemCooldownsPetBold);
			}
		}
		// Effect expired (or no duration) — cooldown timer.
		return overlay(formatTime(remainingCooldown),
				cfg.itemCooldownsPetCooldownColor, cfg.itemCooldownsPetBold);
	}

	private static SlotOverlay trinketOverlay(ItemStack stack, PrisonsConfig cfg) {
		UseCooldown cooldown = stack.get(DataComponents.USE_COOLDOWN);
		if (cooldown == null) {
			return null;
		}
		long lastUseMs = getLastUseMs(stack, "cosmicprisons:trinket_last_use_ms");
		if (lastUseMs <= 0) {
			return null;
		}

		float remainingCooldown =
				(lastUseMs + (long) (cooldown.seconds() * 1000) - System.currentTimeMillis()) / 1000.0f;
		if (remainingCooldown <= 0) {
			return null;
		}
		return overlay(formatTime(remainingCooldown),
				cfg.itemCooldownsTrinketColor, cfg.itemCooldownsTrinketBold);
	}

	private static SlotOverlay banditBoxOverlay(ItemStack stack, String name, PrisonsConfig cfg) {
		if (!name.contains("(Unlocking")) {
			return null; // locked or already unlocked — no timer
		}
		int timeLeft = parseTimeLeftFromLore(stack);
		if (timeLeft <= 0) {
			return null;
		}
		return overlay(formatTime(timeLeft),
				cfg.itemCooldownsBanditBoxColor, cfg.itemCooldownsBanditBoxBold);
	}

	private static SlotOverlay overlay(String text, int rgb, boolean bold) {
		return new SlotOverlay(text, 0xFF000000 | (rgb & 0xFFFFFF), SCALE, bold, Anchor.CENTER);
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

	/** Remaining seconds parsed from a bandit box's "Time Left: Xm Ys" lore, or -1 if absent. */
	private static int parseTimeLeftFromLore(ItemStack stack) {
		ItemLore lore = stack.get(DataComponents.LORE);
		if (lore == null) {
			return -1;
		}
		for (Component line : lore.lines()) {
			Matcher matcher = TIME_LEFT_PATTERN.matcher(line.getString());
			if (matcher.find()) {
				return minutes(matcher.group(1)) + seconds(matcher.group(2));
			}
		}
		return -1;
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
