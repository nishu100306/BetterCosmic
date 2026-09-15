package dev.nishu.bettercosmic.sky.feature;

import dev.nishu.bettercosmic.shared.easyview.Anchor;
import dev.nishu.bettercosmic.shared.easyview.ItemOverlayProvider;
import dev.nishu.bettercosmic.shared.easyview.SlotOverlay;
import dev.nishu.bettercosmic.shared.util.NumberFormatUtil;
import dev.nishu.bettercosmic.sky.client.BetterSkyClient;
import dev.nishu.bettercosmic.sky.config.SkyConfig;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.regex.Pattern;

/**
 * EasyView provider for Cosmic Sky quest-point notes: shows the note's point value compactly in the
 * top-left of its slot, like BetterPrisons' gang-point notes.
 *
 * <p>The value comes straight from NBT — {@code custom_data.points} (a double) — rather than the display
 * name, so it's exact regardless of formatting. It covers both note variants — regular
 * ({@code island_quest_point_note}) and adventure ({@code adventure_island_quest_point_note}) — matched
 * by {@link #COSMIC_ITEM_PATTERN}; both share the same config.
 */
public final class QuestPointProvider implements ItemOverlayProvider {

	/** Matches the {@code custom_data.cosmicItem} of either quest-point note variant. */
	static final Pattern COSMIC_ITEM_PATTERN = Pattern.compile("(?:adventure_)?island_quest_point_note");

	/** Whether {@code cosmicItem} identifies a quest-point note (regular or adventure). */
	static boolean isQuestPointNote(String cosmicItem) {
		return cosmicItem != null && COSMIC_ITEM_PATTERN.matcher(cosmicItem).matches();
	}

	@Override
	public SlotOverlay getOverlay(ItemStack stack) {
		SkyConfig cfg = BetterSkyClient.config;
		if (cfg == null || !cfg.questPointOverlayEnabled || stack.isEmpty()) {
			return null;
		}
		CustomData data = stack.get(DataComponents.CUSTOM_DATA);
		if (data == null) {
			return null;
		}
		CompoundTag nbt = data.copyTag();
		if (!isQuestPointNote(nbt.getStringOr("cosmicItem", ""))) {
			return null;
		}
		long points = (long) nbt.getDoubleOr("points", 0);
		if (points <= 0) {
			return null;
		}
		int color = 0xFF000000 | (cfg.questPointColor & 0xFFFFFF);
		return new SlotOverlay(NumberFormatUtil.compact(points), color,
				cfg.questPointScale / 100f, cfg.questPointBold, Anchor.TOP_LEFT);
	}
}
