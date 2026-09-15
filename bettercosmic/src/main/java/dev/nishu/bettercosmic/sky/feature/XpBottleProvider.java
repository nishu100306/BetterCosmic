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
 * EasyView provider for Cosmic Sky EXP bottles: shows the bottle's XP value compactly in the top-left
 * of its slot.
 *
 * <p>The value comes straight from NBT — {@code custom_data.expValue} (a long) — so it's exact
 * regardless of the display name. Bottles are identified by {@code custom_data.cosmicItem == "exp_bottle"}.
 */
public final class XpBottleProvider implements ItemOverlayProvider {

	/** {@code custom_data.cosmicItem} value that marks an EXP bottle. */
	static final String COSMIC_ITEM = "exp_bottle";

	@Override
	public SlotOverlay getOverlay(ItemStack stack) {
		SkyConfig cfg = BetterSkyClient.config;
		if (cfg == null || !cfg.xpBottleOverlayEnabled || stack.isEmpty()) {
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
		long value = nbt.getLongOr("expValue", 0L);
		if (value <= 0) {
			return null;
		}
		int color = 0xFF000000 | (cfg.xpBottleColor & 0xFFFFFF);
		return new SlotOverlay(NumberFormatUtil.compact(value), color,
				cfg.xpBottleScale / 100f, cfg.xpBottleBold, Anchor.TOP_LEFT);
	}
}
