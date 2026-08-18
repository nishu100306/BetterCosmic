package dev.nishu.bettercosmic.prisons.hud;

import dev.nishu.bettercosmic.prisons.client.BetterPrisonsClient;
import dev.nishu.bettercosmic.prisons.config.PrisonsConfig;
import dev.nishu.bettercosmic.prisons.notification.NotificationType;
import dev.nishu.bettercosmic.prisons.notification.Notifications;
import dev.nishu.bettercosmic.shared.hud.BaseHud;
import dev.nishu.bettercosmic.shared.util.NumberFormatUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;
import org.joml.Matrix3x2fStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Satchel HUD: detects satchels in the inventory (via Cosmic Prisons NBT, rename-proof), shows each
 * one's fill/capacity, and can combine same-type satchels, filter by fill threshold, show a
 * whitescroll indicator, and toast when a satchel fills. Ported from BetterPrisons (Yarn → Mojang);
 * extends the shared {@link BaseHud} and is drawn/ticked by the shared HUD framework.
 */
public class SatchelHud extends BaseHud {

	public List<SatchelInfo> foundSatchels = new ArrayList<>();
	private boolean scanning = false;
	/** Satchels of enabled types found this scan, before combining — the title's true count. */
	private int satchelCount = 0;
	/** Names of satchels currently full — edge-triggers the "satchel full" toast. */
	private final Set<String> fullNotified = new HashSet<>();

	private static final Map<String, ChatFormatting> SATCHEL_COLORS = new HashMap<>();

	static {
		SATCHEL_COLORS.put("Coal Ore Satchel", ChatFormatting.DARK_GRAY);
		SATCHEL_COLORS.put("Coal Satchel", ChatFormatting.DARK_GRAY);
		SATCHEL_COLORS.put("Iron Ore Satchel", ChatFormatting.GRAY);
		SATCHEL_COLORS.put("Iron Satchel", ChatFormatting.GRAY);
		SATCHEL_COLORS.put("Lapis Ore Satchel", ChatFormatting.BLUE);
		SATCHEL_COLORS.put("Lapis Satchel", ChatFormatting.BLUE);
		SATCHEL_COLORS.put("Redstone Ore Satchel", ChatFormatting.RED);
		SATCHEL_COLORS.put("Redstone Satchel", ChatFormatting.RED);
		SATCHEL_COLORS.put("Gold Ore Satchel", ChatFormatting.GOLD);
		SATCHEL_COLORS.put("Gold Satchel", ChatFormatting.GOLD);
		SATCHEL_COLORS.put("Diamond Ore Satchel", ChatFormatting.AQUA);
		SATCHEL_COLORS.put("Diamond Satchel", ChatFormatting.AQUA);
		SATCHEL_COLORS.put("Emerald Ore Satchel", ChatFormatting.GREEN);
		SATCHEL_COLORS.put("Emerald Satchel", ChatFormatting.GREEN);
	}

	private int count = 0;

	public SatchelHud() {
		super("satchel");
	}

	private static PrisonsConfig cfg() {
		return BetterPrisonsClient.config;
	}

	@Override
	public void tick(Minecraft client) {
		this.enabled = cfg().satchelHudEnabled;
		count++;
		if (!enabled || scanning) {
			return;
		}
		if (client.player == null) {
			return;
		}
		if (count % 5 == 0) {
			rescan(client.player.getInventory());
		}
	}

	private void rescan(Inventory inv) {
		scanning = true;
		foundSatchels.clear();
		for (int i = 0; i < inv.getContainerSize(); i++) {
			ItemStack stack = inv.getItem(i);
			if (isSatchel(stack)) {
				foundSatchels.add(parseSatchel(stack));
			} else if (isSatchelBackpack(stack)) {
				foundSatchels.addAll(parseBackpackSatchels(stack));
			}
		}
		filterDisabledTypes();
		satchelCount = foundSatchels.size();
		if (cfg().combineSimilarSatchels) {
			combineSimilarSatchels();
		}
		int threshold = parseThresholdPercent(cfg().satchelShowThreshold);
		if (threshold > 0) {
			filterBelowThreshold(threshold);
		}
		checkFullNotifications();
		scanning = false;
	}

	private void checkFullNotifications() {
		boolean notifyEnabled = Notifications.isEnabled(NotificationType.SATCHEL_FULL);

		Map<String, SatchelInfo> full = new LinkedHashMap<>();
		for (SatchelInfo s : foundSatchels) {
			if (s.max > 0 && s.current >= s.max) {
				full.putIfAbsent(s.name, s);
			}
		}

		if (notifyEnabled) {
			for (Map.Entry<String, SatchelInfo> e : full.entrySet()) {
				if (fullNotified.contains(e.getKey())) {
					continue;
				}
				SatchelInfo s = e.getValue();
				Notifications.toast(NotificationType.SATCHEL_FULL,
						Component.literal("Satchel Full")
								.setStyle(Style.EMPTY.withColor(ChatFormatting.GREEN).withBold(true)),
						Component.literal(s.name), s.itemStack);
			}
		}

		fullNotified.clear();
		fullNotified.addAll(full.keySet());
	}

	private void combineSimilarSatchels() {
		try {
			List<SatchelInfo> combined = new ArrayList<>();
			while (!foundSatchels.isEmpty()) {
				SatchelInfo satchel = foundSatchels.remove(0);
				combined.add(satchel);
				for (int j = 0; j < foundSatchels.size(); j++) {
					SatchelInfo other = foundSatchels.get(j);
					if (satchel.name.equals(other.name)) {
						satchel.current += other.current;
						satchel.max += other.max;
						satchel.whitescrolled = satchel.whitescrolled && other.whitescrolled;
						foundSatchels.remove(j);
						j--;
					}
				}
			}
			this.foundSatchels = combined;
		} catch (Exception e) {
			dev.nishu.bettercosmic.prisons.BetterPrisons.LOGGER.warn("Error combining satchels", e);
		}
	}

	private static final String[] ORE_KEYS =
			{"coal", "iron", "lapis", "redstone", "gold", "diamond", "emerald", "prismarine"};

	/** Maps a satchel display name to its config toggle key, or null if unrecognized (always shown). */
	private String satchelTypeKey(String name) {
		if (name == null) {
			return null;
		}
		String lower = name.toLowerCase();
		if (lower.contains("clue scroll")) {
			return "clue_scroll";
		}
		if (lower.contains("contraband")) {
			return "contraband";
		}
		if (lower.contains("shard")) {
			return "shard";
		}
		for (String ore : ORE_KEYS) {
			if (lower.contains(ore)) {
				String variant;
				if (lower.contains("deepslate")) {
					variant = "deepslate";
				} else if (lower.startsWith("block of")) {
					variant = "block";
				} else if (lower.contains("ore satchel")) {
					variant = "ore";
				} else {
					variant = "refined";
				}
				return ore + "_" + variant;
			}
		}
		return null;
	}

	private void filterDisabledTypes() {
		Map<String, Boolean> enabledMap = cfg().satchelTypeEnabled;
		if (enabledMap == null || enabledMap.isEmpty()) {
			return;
		}
		foundSatchels.removeIf(s -> {
			String key = satchelTypeKey(s.name);
			return key != null && !enabledMap.getOrDefault(key, true);
		});
	}

	private int parseThresholdPercent(String setting) {
		if (setting == null) {
			return 0;
		}
		String s = setting.trim();
		if (s.isEmpty() || s.equalsIgnoreCase("Off")) {
			return 0;
		}
		try {
			return Integer.parseInt(s.replace("%", "").trim());
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private void filterBelowThreshold(int thresholdPercent) {
		foundSatchels.removeIf(s -> {
			double percent = s.max > 0 ? (s.current * 100.0) / s.max : 0.0;
			return percent < thresholdPercent;
		});
	}

	private static final Pattern CAPACITY_PATTERN =
			Pattern.compile("\\((\\d[\\d,]*)\\s*/\\s*(\\d[\\d,]*)\\s*[A-Za-z]+\\)");

	private static final Pattern BACKPACK_SATCHEL_PATTERN =
			Pattern.compile("^\\s*\\d+\\.\\s*(.+?)\\s*\\((\\d[\\d,]*)\\s*/\\s*(\\d[\\d,]*)\\s*[A-Za-z]+\\)");

	/** Returns the cosmicprisons PublicBukkitValues NBT compound, or null. */
	private CompoundTag getBukkit(ItemStack stack) {
		CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
		if (customData == null) {
			return null;
		}
		CompoundTag bukkit = customData.copyTag().getCompound("PublicBukkitValues").orElse(null);
		return (bukkit == null || bukkit.isEmpty()) ? null : bukkit;
	}

	private boolean isSatchelBackpack(ItemStack stack) {
		if (stack.isEmpty()) {
			return false;
		}
		CompoundTag bukkit = getBukkit(stack);
		if (bukkit == null) {
			return false;
		}
		return "satchel_backpack".equals(bukkit.getString("cosmicprisons:custom_item_id").orElse(""));
	}

	private List<SatchelInfo> parseBackpackSatchels(ItemStack stack) {
		List<SatchelInfo> result = new ArrayList<>();
		ItemLore lore = stack.get(DataComponents.LORE);
		if (lore == null) {
			return result;
		}

		ItemStack icon = stack.copy();
		for (Component line : lore.lines()) {
			String lineStr = line.getString();
			Matcher m = BACKPACK_SATCHEL_PATTERN.matcher(lineStr);
			if (!m.find()) {
				continue;
			}
			long cur, max;
			try {
				cur = Long.parseLong(m.group(2).replace(",", ""));
				max = Long.parseLong(m.group(3).replace(",", ""));
			} catch (NumberFormatException e) {
				continue;
			}
			if (max <= 0) {
				max = 1;
			}

			SatchelInfo info = new SatchelInfo();
			info.name = m.group(1).trim();
			info.current = cur;
			info.max = max;
			info.itemStack = icon;
			info.whitescrolled = lineStr.toUpperCase().contains("WHITESCROLL");

			ChatFormatting color = resolveSatchelColor(info.name);
			Style style = Style.EMPTY.withBold(true);
			if (color != null) {
				style = style.withColor(color);
			}
			info.displayName = Component.literal(info.name).setStyle(style);
			result.add(info);
		}
		return result;
	}

	private ChatFormatting resolveSatchelColor(String name) {
		ChatFormatting c = SATCHEL_COLORS.get(name);
		if (c != null) {
			return c;
		}
		String[] words = name.split("\\s+");
		for (int start = 1; start < words.length; start++) {
			String candidate = String.join(" ", Arrays.copyOfRange(words, start, words.length));
			ChatFormatting f = SATCHEL_COLORS.get(candidate);
			if (f != null) {
				return f;
			}
		}
		return null;
	}

	private boolean isSatchel(ItemStack stack) {
		if (stack.isEmpty()) {
			return false;
		}
		CompoundTag bukkit = getBukkit(stack);
		if (bukkit == null) {
			return false;
		}
		if (bukkit.getString("cosmicprisons:custom_item_id").orElse("").endsWith("_satchel")) {
			return true;
		}
		return !bukkit.getString("cosmicprisons:satchel_ore").orElse("").isEmpty();
	}

	private SatchelInfo parseSatchel(ItemStack stack) {
		SatchelInfo info = new SatchelInfo();
		info.itemStack = stack.copy();

		CompoundTag bukkit = getBukkit(stack);
		String ore = bukkit != null ? bukkit.getString("cosmicprisons:satchel_ore").orElse("") : "";
		String customId = bukkit != null ? bukkit.getString("cosmicprisons:custom_item_id").orElse("") : "";
		boolean renamed = bukkit != null
				&& !bukkit.getString("cosmicprisons:custom_display_name").orElse("").isEmpty();

		long[] cap = parseCapacity(stack);

		String collected = ore.isEmpty() ? null : parseCollectedBlock(stack);
		if (collected != null) {
			info.name = collected + " Satchel";
		} else {
			info.name = renamed ? prettyName(customId) : stripCapacitySuffix(stack.getHoverName().getString());
		}

		String colorKey;
		if (!ore.isEmpty()) {
			boolean refined = bukkit.getBoolean("cosmicprisons:satchel_refined").orElse(false);
			long collectedCount = bukkit.getInt("cosmicprisons:satchel_count").orElse(0);
			String oreName = ore.charAt(0) + ore.substring(1).toLowerCase();
			colorKey = refined ? oreName + " Satchel" : oreName + " Ore Satchel";
			info.current = collectedCount;
		} else {
			colorKey = info.name;
			info.current = cap != null ? cap[0] : 0;
		}

		info.max = cap != null ? cap[1] : 1;
		if (info.max <= 0) {
			info.max = 1;
		}

		ChatFormatting color = SATCHEL_COLORS.get(colorKey);
		Style style = Style.EMPTY.withBold(true);
		if (color != null) {
			style = style.withColor(color);
		}
		info.displayName = Component.literal(info.name).setStyle(style);
		info.whitescrolled = detectWhitescroll(stack, bukkit);
		return info;
	}

	private boolean detectWhitescroll(ItemStack stack, CompoundTag bukkit) {
		if (bukkit != null && bukkit.getBoolean("cosmicprisons:white_scroll").orElse(false)) {
			return true;
		}
		ItemLore lore = stack.get(DataComponents.LORE);
		if (lore != null) {
			for (Component line : lore.lines()) {
				if (line.getString().toUpperCase().contains("WHITESCROLL")) {
					return true;
				}
			}
		}
		return false;
	}

	private long[] parseCapacity(ItemStack stack) {
		long[] fromName = matchCapacity(stack.getHoverName().getString());
		if (fromName != null) {
			return fromName;
		}
		ItemLore lore = stack.get(DataComponents.LORE);
		if (lore != null) {
			for (Component line : lore.lines()) {
				long[] r = matchCapacity(line.getString());
				if (r != null) {
					return r;
				}
			}
		}
		return null;
	}

	private long[] matchCapacity(String s) {
		Matcher m = CAPACITY_PATTERN.matcher(s);
		if (m.find()) {
			try {
				long cur = Long.parseLong(m.group(1).replace(",", ""));
				long max = Long.parseLong(m.group(2).replace(",", ""));
				return new long[]{cur, max};
			} catch (NumberFormatException e) {
				return null;
			}
		}
		return null;
	}

	private String parseCollectedBlock(ItemStack stack) {
		ItemLore lore = stack.get(DataComponents.LORE);
		if (lore == null) {
			return null;
		}
		String marker = "Automatically collects ";
		for (Component line : lore.lines()) {
			String s = line.getString();
			int idx = s.indexOf(marker);
			if (idx >= 0) {
				String rest = s.substring(idx + marker.length()).replace("while mining.", "").trim();
				if (!rest.isEmpty()) {
					return rest;
				}
			}
		}
		return null;
	}

	private String stripCapacitySuffix(String name) {
		int idx = name.indexOf('(');
		return idx >= 0 ? name.substring(0, idx).trim() : name.trim();
	}

	private String prettyName(String customId) {
		if (customId == null || customId.isEmpty()) {
			return "Satchel";
		}
		StringBuilder sb = new StringBuilder();
		for (String part : customId.split("_")) {
			if (part.isEmpty()) {
				continue;
			}
			if (sb.length() > 0) {
				sb.append(' ');
			}
			sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
		}
		return sb.length() == 0 ? "Satchel" : sb.toString();
	}

	private int indicatorRawWidth(Minecraft client) {
		if (!cfg().satchelWhitescrollIndicators) {
			return 0;
		}
		return 4 + client.font.width(Component.literal("W").setStyle(Style.EMPTY.withBold(true)));
	}

	private Component whitescrollIndicator(SatchelInfo satchel) {
		return satchel.whitescrolled
				? Component.literal("W").setStyle(Style.EMPTY.withBold(true).withColor(ChatFormatting.WHITE))
				: Component.literal("X").setStyle(Style.EMPTY.withBold(true).withColor(ChatFormatting.RED));
	}

	private MutableComponent buildTitleText() {
		MutableComponent title = Component.literal("Satchel HUD")
				.setStyle(Style.EMPTY.withUnderlined(true).withBold(true));
		if (cfg().satchelShowCount) {
			title.append(Component.literal(" (" + satchelCount + ")").setStyle(
					Style.EMPTY.withColor(ChatFormatting.GRAY).withBold(false).withUnderlined(false)));
		}
		return title;
	}

	@Override
	public void render(GuiGraphics ctx, Minecraft client) {
		this.scale = cfg().satchelHudScale / 100.0f;

		boolean showTitle = cfg().showSatchelHudTitle;
		boolean hasContent = !foundSatchels.isEmpty();
		if (!enabled || (!showTitle && !hasContent)) {
			return;
		}

		int titleHeight = 0;
		int titleWidth = 0;
		if (showTitle) {
			MutableComponent titleText = buildTitleText();
			titleWidth = (int) (client.font.width(titleText) * scale);
			titleHeight = scaled(12);
		}

		int maxTextWidth = titleWidth;
		if (hasContent) {
			for (SatchelInfo satchel : foundSatchels) {
				if (satchel.displayName != null) {
					int nameWidth = (int) (client.font.width(satchel.displayName) * scale);
					maxTextWidth = Math.max(maxTextWidth, nameWidth);
				}
				String fillText = fillText(satchel);
				int fillTextWidth = (int) ((client.font.width(Component.literal(fillText)) + indicatorRawWidth(client)) * scale);
				maxTextWidth = Math.max(maxTextWidth, fillTextWidth);
			}
		}

		int bgWidth = hasContent ? (scaled(16 + 4) + maxTextWidth) : maxTextWidth;
		int contentHeight = hasContent ? foundSatchels.size() * scaled(18) : 0;
		int bgHeight = titleHeight + contentHeight;

		int bgColor = (cfg().satchelBgOpacity << 24) | (cfg().satchelBgColor & 0xFFFFFF);
		int borderColor = (cfg().satchelBorderOpacity << 24) | (cfg().satchelBorderColor & 0xFFFFFF);
		int thickness = scaled(cfg().satchelBorderThickness);
		int padding = 4;
		if (scale < 1) {
			padding = scaled(padding);
		}

		ctx.fill(x - padding, y - padding, x + bgWidth + padding, y + bgHeight + padding, bgColor);
		ctx.fill(x - padding, y - padding - thickness, x + bgWidth + padding, y - padding, borderColor);
		ctx.fill(x - padding, y + bgHeight + padding, x + bgWidth + padding, y + bgHeight + padding + thickness, borderColor);
		ctx.fill(x - padding - thickness, y - padding - thickness, x - padding, y + bgHeight + padding + thickness, borderColor);
		ctx.fill(x + bgWidth + padding, y - padding - thickness, x + bgWidth + padding + thickness, y + bgHeight + padding + thickness, borderColor);

		Matrix3x2fStack matrices = ctx.pose();
		int yOffset = 0;

		if (showTitle) {
			MutableComponent titleText = buildTitleText();
			int titleColor = 0xFF000000 | cfg().satchelHudTitleColor;
			matrices.pushMatrix();
			matrices.scale(scale, scale);
			matrices.translate(x / scale, y / scale);
			ctx.drawString(client.font, titleText, 0, 0, titleColor, true);
			matrices.popMatrix();
			yOffset += titleHeight;
		}

		if (hasContent) {
			int rowHeight = scaled(18);
			int iconSpacing = scaled(20);
			int textLineSpacing = scaled(10);

			for (SatchelInfo satchel : foundSatchels) {
				if (satchel.itemStack != null) {
					matrices.pushMatrix();
					matrices.scale(scale, scale);
					matrices.translate(x / scale, (y + yOffset) / scale);
					ctx.renderItem(satchel.itemStack, 0, 0);
					matrices.popMatrix();
				}

				if (satchel.displayName != null) {
					matrices.pushMatrix();
					matrices.scale(scale, scale);
					matrices.translate((x + iconSpacing) / scale, (y + yOffset) / scale);
					ctx.drawString(client.font, satchel.displayName, 0, 0, 0xFFFFFFFF, true);
					matrices.popMatrix();
				}

				double percentage = (satchel.current * 100.0) / satchel.max;
				String fillText = fillText(satchel);
				int capacityColor;
				if (percentage < 20.0) {
					capacityColor = 0xFF000000 | cfg().satchelColorUnder20;
				} else if (percentage < 60.0) {
					capacityColor = 0xFF000000 | cfg().satchelColor20to60;
				} else if (percentage < 95.0) {
					capacityColor = 0xFF000000 | cfg().satchelColor60to95;
				} else {
					capacityColor = 0xFF000000 | cfg().satchelColor95Plus;
				}

				matrices.pushMatrix();
				matrices.scale(scale, scale);
				matrices.translate((x + iconSpacing) / scale, (y + yOffset + textLineSpacing) / scale);
				ctx.drawString(client.font, Component.literal(fillText), 0, 0, capacityColor, true);
				if (cfg().satchelWhitescrollIndicators) {
					int fillW = client.font.width(Component.literal(fillText));
					ctx.drawString(client.font, whitescrollIndicator(satchel), fillW + 4, 0, 0xFFFFFFFF, true);
				}
				matrices.popMatrix();

				yOffset += rowHeight;
			}
		}
	}

	private String fillText(SatchelInfo satchel) {
		if (cfg().satchelShowPercentage) {
			double percentage = (satchel.current * 100.0) / satchel.max;
			return String.format("%.1f%%", percentage);
		}
		return NumberFormatUtil.withCommas(satchel.current) + " / " + NumberFormatUtil.withCommas(satchel.max);
	}

	@Override
	public int getWidth() {
		Minecraft client = Minecraft.getInstance();
		if (client == null || client.font == null) {
			return scaled(140);
		}
		boolean showTitle = cfg().showSatchelHudTitle;
		boolean hasContent = !foundSatchels.isEmpty();

		int titleWidth = showTitle ? (int) (client.font.width(buildTitleText()) * scale) : 0;
		int maxTextWidth = titleWidth;
		if (hasContent) {
			for (SatchelInfo satchel : foundSatchels) {
				if (satchel.displayName != null) {
					maxTextWidth = Math.max(maxTextWidth, (int) (client.font.width(satchel.displayName) * scale));
				}
				String fillText = fillText(satchel);
				int fillTextWidth = (int) ((client.font.width(Component.literal(fillText)) + indicatorRawWidth(client)) * scale);
				maxTextWidth = Math.max(maxTextWidth, fillTextWidth);
			}
		}
		int bgWidth = hasContent ? (scaled(16 + 4) + maxTextWidth) : maxTextWidth;
		int padding = 4;
		if (scale < 1) {
			padding = scaled(padding);
		}
		return bgWidth + (padding * 2);
	}

	@Override
	public int getHeight() {
		int titleHeight = cfg().showSatchelHudTitle ? scaled(10) : 0;
		int contentHeight = foundSatchels.size() * scaled(18);
		return titleHeight + contentHeight;
	}

	public static class SatchelInfo {
		public String name = "";
		public Component displayName;
		public ItemStack itemStack;
		public long current = 0;
		public long max = 1;
		public boolean whitescrolled = false;
	}
}
