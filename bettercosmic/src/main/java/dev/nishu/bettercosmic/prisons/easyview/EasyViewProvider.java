package dev.nishu.bettercosmic.prisons.easyview;

import dev.nishu.bettercosmic.prisons.client.BetterPrisonsClient;
import dev.nishu.bettercosmic.prisons.config.PrisonsConfig;
import dev.nishu.bettercosmic.shared.easyview.Anchor;
import dev.nishu.bettercosmic.shared.easyview.ItemOverlayProvider;
import dev.nishu.bettercosmic.shared.easyview.SlotOverlay;
import dev.nishu.bettercosmic.shared.util.NumberFormatUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * EasyView overlays for Cosmic Prisons items — the compact corner text that shows an item's value or
 * level without opening its tooltip. Ported from BetterPrisons' {@code misc/EasyView} (Yarn → Mojang)
 * and re-expressed as a single shared {@link ItemOverlayProvider}: it detects the item by its display
 * name / type, formats the value, and returns a top-left {@link SlotOverlay}. The shared EasyView
 * mixins draw it in both containers and the hotbar, so BetterPrisons' three EasyView render mixins are
 * no longer needed.
 *
 * <p>Each item type self-gates on its {@link PrisonsConfig} toggle; the whole provider is gated on
 * {@link PrisonsConfig#easyViewEnabled}.
 */
public final class EasyViewProvider implements ItemOverlayProvider {

	@Override
	public SlotOverlay getOverlay(ItemStack stack) {
		PrisonsConfig cfg = BetterPrisonsClient.config;
		if (cfg == null || !cfg.easyViewEnabled || stack.isEmpty()) {
			return null;
		}

		String name;
		try {
			name = stack.getHoverName().getString();
		} catch (Exception e) {
			return null;
		}
		String itemId = stack.getItem().getDescriptionId().toLowerCase();

		try {
			// ---- value notes (compact number) ----
			if (cfg.easyViewEnergyEnabled && name.endsWith(" Cosmic Energy")) {
				return overlay(compact(name.replace(" Cosmic Energy", "")),
						cfg.easyViewEnergyColor, cfg.easyViewEnergyScale / 100f, cfg.easyViewEnergyBold);
			}
			if (cfg.easyViewMoneyEnabled && name.startsWith("$")) {
				String money = name.replace("$", "").split("\\.")[0];
				return overlay(compact(money), cfg.easyViewMoneyColor, cfg.easyViewMoneyScale / 100f, cfg.easyViewMoneyBold);
			}
			if (cfg.easyViewGangPointsEnabled
					&& (name.toUpperCase().endsWith(" GANG POINTS") || name.endsWith(" Gang Points"))) {
				String pts = name.replace(" GANG POINTS", "").replace(" Gang Points", "");
				return overlay(compact(pts), cfg.easyViewGangPointsColor, cfg.easyViewGangPointsScale / 100f, cfg.easyViewGangPointsBold);
			}

			// ---- percent notes ----
			if (cfg.easyViewBlackScrollEnabled && name.startsWith("Black Scroll (")) {
				String pct = name.replace("Black Scroll (", "").replace("%)", "");
				return overlay(pct + "%", cfg.easyViewBlackScrollColor, cfg.easyViewBlackScrollScale / 100f, cfg.easyViewBlackScrollBold);
			}
			if (cfg.easyViewChargeOrbEnabled && name.endsWith("% Charge Orb")) {
				String pct = name.replace("% Charge Orb", "");
				return overlay(pct + "%", cfg.easyViewChargeOrbColor, cfg.easyViewChargeOrbScale / 100f, cfg.easyViewChargeOrbBold);
			}
			if (cfg.easyViewDustEnabled) {
				String pct = betweenPercent(name, " Dust (");
				if (pct != null) {
					return overlay(pct + "%", cfg.easyViewDustColor, cfg.easyViewDustScale / 100f, cfg.easyViewDustBold);
				}
			}
			if (cfg.easyViewPagesEnabled) {
				String pct = betweenPercent(name, " Page (");
				if (pct != null) {
					int color = cfg.easyViewPagesColor;
					if (cfg.easyViewPagesTierColor) {
						int tier = tierColor(stack);
						if (tier != -1) {
							color = tier;
						}
					}
					return overlay(pct + "%", color, cfg.easyViewPagesScale / 100f, cfg.easyViewPagesBold);
				}
			}

			// ---- level numbers (armor / weapon / pickaxe by item type) ----
			if (cfg.easyViewArmorEnabled && isArmor(itemId)) {
				String n = trailingNumber(name);
				if (n != null) {
					return overlay(n, cfg.easyViewArmorColor, cfg.easyViewArmorScale / 100f, cfg.easyViewArmorBold);
				}
			}
			if (cfg.easyViewWeaponsEnabled && isWeapon(itemId)) {
				String n = trailingNumber(name);
				if (n != null) {
					return overlay(n, cfg.easyViewWeaponsColor, cfg.easyViewWeaponsScale / 100f, cfg.easyViewWeaponsBold);
				}
			}
			if (cfg.easyViewPickaxesEnabled && itemId.contains("pickaxe")) {
				String n = trailingNumber(name);
				if (n != null) {
					return overlay(n, cfg.easyViewPickaxesColor, cfg.easyViewPickaxesScale / 100f, cfg.easyViewPickaxesBold);
				}
			}

			// ---- misc ----
			if (cfg.easyViewPrestigeTokenEnabled && name.startsWith("Pickaxe Prestige Token ")) {
				String level = name.replace("Pickaxe Prestige Token ", "").trim();
				if (!level.isEmpty()) {
					return overlay(level, cfg.easyViewPrestigeTokenColor, cfg.easyViewPrestigeTokenScale / 100f, cfg.easyViewPrestigeTokenBold);
				}
			}
			if (cfg.easyViewXpBottleEnabled
					&& (name.startsWith("XP Bottle (") || name.startsWith("Player XP Bottle (")) && name.endsWith(")")) {
				String amount = between(name, '(', ')');
				if (amount != null) {
					int color = cfg.easyViewXpBottleColor;
					if (cfg.easyViewXpBottleTierColor) {
						int tier = tierColor(stack);
						if (tier != -1) {
							color = tier;
						}
					}
					return overlay(compact(amount), color, cfg.easyViewXpBottleScale / 100f, cfg.easyViewXpBottleBold);
				}
			}
		} catch (Exception e) {
			// silently ignore parsing errors — a malformed name must not break slot rendering
		}
		return null;
	}

	private static SlotOverlay overlay(String text, int rgb, float scale, boolean bold) {
		return new SlotOverlay(text, 0xFF000000 | (rgb & 0xFFFFFF), scale, bold, Anchor.TOP_LEFT);
	}

	/** Parses a comma-grouped integer string and formats it compactly (1.2M etc.). */
	private static String compact(String raw) {
		long value = Long.parseLong(raw.replaceAll(",", "").trim());
		return NumberFormatUtil.compact(value);
	}

	private static boolean isArmor(String itemId) {
		return itemId.contains("helmet") || itemId.contains("chestplate")
				|| itemId.contains("leggings") || itemId.contains("boots");
	}

	private static boolean isWeapon(String itemId) {
		return itemId.contains("sword") || (itemId.contains("axe") && !itemId.contains("pickaxe"));
	}

	/** Extracts the trailing level number, tolerating a Roman-numeral prestige suffix. */
	private static String trailingNumber(String name) {
		String[] parts = name.trim().split("\\s+");
		if (parts.length == 0) {
			return null;
		}
		String last = parts[parts.length - 1];
		if (last.matches("\\d+")) {
			return last;
		}
		if (last.matches("[IVXLCDM]+") && parts.length >= 2) {
			String secondLast = parts[parts.length - 2];
			if (secondLast.matches("\\d+")) {
				return secondLast;
			}
		}
		return null;
	}

	/** Extracts {@code NN} from a name like {@code "Simple Dust (NN%)"} given the marker {@code " Dust ("}. */
	private static String betweenPercent(String name, String marker) {
		int start = name.indexOf(marker);
		if (start == -1) {
			return null;
		}
		int end = name.indexOf("%)", start);
		if (end == -1) {
			return null;
		}
		return name.substring(start + marker.length(), end);
	}

	private static String between(String name, char open, char close) {
		int start = name.indexOf(open);
		int end = start == -1 ? -1 : name.indexOf(close, start);
		return (start != -1 && end != -1) ? name.substring(start + 1, end).replaceAll(",", "") : null;
	}

	/** The tier color from the first sibling of the item's custom name, or {@code -1}. */
	private static int tierColor(ItemStack stack) {
		try {
			Component customName = stack.get(DataComponents.CUSTOM_NAME);
			if (customName == null) {
				return -1;
			}
			List<Component> siblings = customName.getSiblings();
			if (siblings.isEmpty()) {
				return -1;
			}
			TextColor color = siblings.get(0).getStyle().getColor();
			return color != null ? color.getValue() : -1;
		} catch (Exception e) {
			return -1;
		}
	}
}
