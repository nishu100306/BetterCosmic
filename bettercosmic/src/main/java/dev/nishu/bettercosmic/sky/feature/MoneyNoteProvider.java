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

/**
 * EasyView provider for Cosmic Sky money notes: shows the note's dollar value compactly in the top-left
 * of its slot.
 *
 * <p>The value comes straight from NBT — {@code custom_data.noteValue} (a double) — so it's exact
 * regardless of the display name. Notes are identified by {@code custom_data.cosmicItem == "money_note"}.
 */
public final class MoneyNoteProvider implements ItemOverlayProvider {

	/** {@code custom_data.cosmicItem} value that marks a money note. */
	static final String COSMIC_ITEM = "money_note";

	@Override
	public SlotOverlay getOverlay(ItemStack stack) {
		SkyConfig cfg = BetterSkyClient.config;
		if (cfg == null || !cfg.moneyNoteOverlayEnabled || stack.isEmpty()) {
			return null;
		}
		CustomData data = stack.get(DataComponents.CUSTOM_DATA);
		if (data == null) {
			return null;
		}
		CompoundTag nbt = data.copyTag();
		if (!COSMIC_ITEM.equals(nbt.getStringOr("cosmicItem", ""))) {
			return null;
		}
		long value = (long) nbt.getDoubleOr("noteValue", 0);
		if (value <= 0) {
			return null;
		}
		int color = 0xFF000000 | (cfg.moneyNoteColor & 0xFFFFFF);
		return new SlotOverlay("$" + NumberFormatUtil.compact(value), color,
				cfg.moneyNoteScale / 100f, cfg.moneyNoteBold, Anchor.TOP_LEFT);
	}
}
