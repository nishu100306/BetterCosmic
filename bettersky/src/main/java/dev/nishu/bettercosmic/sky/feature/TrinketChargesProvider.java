package dev.nishu.bettercosmic.sky.feature;

import dev.nishu.bettercosmic.shared.easyview.Anchor;
import dev.nishu.bettercosmic.shared.easyview.ItemOverlayProvider;
import dev.nishu.bettercosmic.shared.easyview.SlotOverlay;
import dev.nishu.bettercosmic.sky.client.BetterSkyClient;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.component.CustomData;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * EasyView provider for Cosmic Sky potion trinkets: shows the trinket's remaining usable count in
 * the bottom-left of its slot, colored to match the trinket's potion.
 *
 * <p>The number comes from the trinket's display name, which the server suffixes with the current
 * usable count in parentheses, e.g. {@code "Healing Trinket II (39)"} → {@code 39} — capped at the
 * item's "Max Uses Per World" ({@code custom_data.maxUses}) when that is lower. (The name count is
 * the authoritative usable value; it differs from the raw {@code charges} NBT, which was 517 for
 * that item while the real remaining uses was 39.)
 *
 * <p>The color comes straight from vanilla via {@link PotionContents#getColor()} (red for Healing,
 * light blue for Speed, orange for Fire Resistance, ...).
 *
 * <p>Trinkets are identified by their {@code custom_data} NBT
 * ({@code cosmicItem: "potion_trinket"}), so this works for every potion trinket variant regardless
 * of display name.
 */
public final class TrinketChargesProvider implements ItemOverlayProvider {

	private static final String COSMIC_ITEM_KEY = "cosmicItem";
	private static final String POTION_TRINKET = "potion_trinket";

	/** Matches the trailing "(<digits>)" the server appends to a trinket's name. */
	private static final Pattern TRAILING_COUNT = Pattern.compile("\\((\\d+)\\)\\s*$");

	@Override
	public SlotOverlay getOverlay(ItemStack stack) {
		if (!BetterSkyClient.config.trinketChargesOverlay) {
			return null;
		}

		CustomData data = stack.get(DataComponents.CUSTOM_DATA);
		if (data == null) {
			return null;
		}

		CompoundTag nbt = data.copyTag();
		if (!POTION_TRINKET.equals(nbt.getStringOr(COSMIC_ITEM_KEY, ""))) {
			return null;
		}

		// Remaining usable count, read from the "(N)" suffix on the trinket's name...
		Integer nameCount = trailingCount(stack.getHoverName().getString());
		if (nameCount == null) {
			return null;
		}
		// ...but never show more than the Max Uses Per World cap.
		int maxUses = nbt.getIntOr("maxUses", Integer.MAX_VALUE);
		int shown = Math.min(nameCount, maxUses);

		// Color: either the vanilla potion color (default) or the user's custom color, per config.
		// The potion source falls back to the custom color if the item has no potion_contents.
		int rgb;
		if ("Custom".equals(BetterSkyClient.config.trinketColorSource)) {
			rgb = BetterSkyClient.config.trinketChargesColor;
		} else {
			PotionContents potion = stack.get(DataComponents.POTION_CONTENTS);
			rgb = potion != null ? potion.getColor() : BetterSkyClient.config.trinketChargesColor;
		}

		int color = 0xFF000000 | (rgb & 0xFFFFFF);
		float scale = (float) BetterSkyClient.config.trinketChargesScale;
		Anchor anchor = anchorFromLabel(BetterSkyClient.config.trinketChargesAnchor);
		return new SlotOverlay(String.valueOf(shown), color, scale, true, anchor);
	}

	/** Maps a friendly position label from config to a {@link Anchor} (defaults to bottom-left). */
	private static Anchor anchorFromLabel(String label) {
		return switch (label) {
			case "Top-left" -> Anchor.TOP_LEFT;
			case "Top-right" -> Anchor.TOP_RIGHT;
			case "Bottom-right" -> Anchor.BOTTOM_RIGHT;
			case "Center" -> Anchor.CENTER;
			default -> Anchor.BOTTOM_LEFT;
		};
	}

	/** @return the number inside the name's trailing "(...)", or {@code null} if none/unparseable. */
	private static Integer trailingCount(String name) {
		if (name == null) {
			return null;
		}
		Matcher matcher = TRAILING_COUNT.matcher(name);
		if (!matcher.find()) {
			return null;
		}
		try {
			return Integer.parseInt(matcher.group(1));
		} catch (NumberFormatException e) {
			return null; // absurdly large count — ignore rather than crash
		}
	}
}
