package dev.nishu.bettercosmic.prisons.chestsearch;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.nishu.bettercosmic.prisons.BetterPrisons;
import dev.nishu.bettercosmic.prisons.client.BetterPrisonsClient;
import dev.nishu.bettercosmic.shared.easyview.Anchor;
import dev.nishu.bettercosmic.shared.easyview.ItemOverlayProvider;
import dev.nishu.bettercosmic.shared.easyview.SlotOverlay;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Clue Scroll Sorting: draws the clue's current (furthest) step number — or a green check when
 * finished — large and centered on the item, in containers and the hotbar. Implemented as a shared
 * EasyView {@link ItemOverlayProvider} (CENTER, outlined). Also contributes an "unmapped step"
 * tooltip warning via {@link #appendTooltip}. Ported from BetterPrisons' {@code ClueScrollOverlay}
 * (Yarn → Mojang; per-slot rendering now goes through EasyView instead of BP's mixins).
 */
public final class ClueScrollProvider implements ItemOverlayProvider {

	/** Clue step NBT type → displayed step number. Confirmed from real clue-scroll NBT. */
	private static final Map<String, Integer> TYPE_TO_STEP = new HashMap<>();

	static {
		TYPE_TO_STEP.put("TINKER_PICKAXE", 1);
		TYPE_TO_STEP.put("TINKER_GEAR", 2);
		TYPE_TO_STEP.put("TINKER_ENCHANT_PICKAXE", 3);
		TYPE_TO_STEP.put("TINKER_ENCHANT_GEAR", 4);
		TYPE_TO_STEP.put("MINE_ORE_REFINED", 5);
		TYPE_TO_STEP.put("MINE_ORE_REGULAR", 5);
		TYPE_TO_STEP.put("MINE_METEORITE_ORE", 6);
		TYPE_TO_STEP.put("MINE_METEOR_BLOCK", 7);
		TYPE_TO_STEP.put("MINE_METEOR_CONTRABAND", 8);
		TYPE_TO_STEP.put("DISCOVER_CONTRABAND", 8);
		TYPE_TO_STEP.put("DISCOVER_SHARD", 9);
		TYPE_TO_STEP.put("ITEM_USE_SHARD", 10);
		TYPE_TO_STEP.put("ITEM_USE_CONTRABAND", 11);
		TYPE_TO_STEP.put("ITEM_USE_MYSTERY_ENCHANT_GEAR", 12);
		TYPE_TO_STEP.put("AH_BUY", 13);
		TYPE_TO_STEP.put("AH_SELL", 14);
		TYPE_TO_STEP.put("JACKPOT_BUY", 15);
		TYPE_TO_STEP.put("CF_WIN", 16);
		TYPE_TO_STEP.put("CF_LOSE", 16);
		TYPE_TO_STEP.put("ENCHANTER_REPAIR_GEAR", 17);
		TYPE_TO_STEP.put("ENCHANT_DUST", 18);
		TYPE_TO_STEP.put("ENCHANTER_ENCHANT_PICKAXE", 19);
		TYPE_TO_STEP.put("ENCHANTER_LEVELUP_PICKAXE", 19);
		TYPE_TO_STEP.put("ENCHANTER_FAIL_PICKAXE", 20);
		TYPE_TO_STEP.put("TELEPORT_KOTH", 21);
		TYPE_TO_STEP.put("LOCATION_VISIT_ZONE", 22);
		TYPE_TO_STEP.put("TELEPORT_MINE", 22);
		TYPE_TO_STEP.put("ENCHANTER_ENCHANT_GEAR", 23);
		TYPE_TO_STEP.put("ENCHANTER_FAIL_GEAR", 24);
		TYPE_TO_STEP.put("ENCHANTER_DESTROY_GEAR", 24);
		TYPE_TO_STEP.put("ITEM_USE_BLACKSCROLL", 25);
		TYPE_TO_STEP.put("ITEM_USE_WHITESCROLL", 26);
		TYPE_TO_STEP.put("ITEM_USE_ABSORBER", 27);
		TYPE_TO_STEP.put("ITEM_USE_ENCHANT_PAGE", 28);
		TYPE_TO_STEP.put("ITEM_USE_CHARGEORB", 29);
		TYPE_TO_STEP.put("ITEM_USE_RANDOMIZATION", 30);
		TYPE_TO_STEP.put("ITEM_USE_LORECRYSTAL", 31);
		TYPE_TO_STEP.put("ITEM_USE_BOOSTER_XP", 32);
		TYPE_TO_STEP.put("ITEM_PICKAXE_PRESTIGE", 33);
		TYPE_TO_STEP.put("ENTITY_ATTACK_BANDIT", 34);
		TYPE_TO_STEP.put("ENTITY_KILL_BANDIT", 34);
		TYPE_TO_STEP.put("ENTITY_ATTACK_ESCAPEE_BANDIT", 35);
		TYPE_TO_STEP.put("ENTITY_KILL_ESCAPEE_BANDIT", 35);
		TYPE_TO_STEP.put("TRADE_GIVE", 36);
		TYPE_TO_STEP.put("TRADE_RECEIVE", 36);
		TYPE_TO_STEP.put("ITEM_USE_KOTH_LOOTBAG", 37);
		TYPE_TO_STEP.put("SHOP_BUY_COOKIE", 38);
	}

	/** Unmapped types already logged this session — avoids per-frame log spam. */
	private static final Set<String> LOGGED_UNMAPPED = new HashSet<>();

	/** The displayed step number for a clue NBT step type, or {@code null} if unmapped (used by dev tools). */
	public static Integer getStep(String type) {
		return TYPE_TO_STEP.get(type);
	}

	@Override
	public SlotOverlay getOverlay(ItemStack stack) {
		if (BetterPrisonsClient.config == null || !BetterPrisonsClient.config.clueScrollSortingEnabled) {
			return null;
		}
		ClueData data = parse(stack);
		if (data == null) {
			return null;
		}
		for (String type : data.allTypes) {
			if (!TYPE_TO_STEP.containsKey(type) && LOGGED_UNMAPPED.add(type)) {
				BetterPrisons.LOGGER.warn("Unmapped clue scroll step type: {}", type);
			}
		}

		String label;
		int rgb;
		if (data.completed) {
			label = "✓";
			rgb = 0x55FF55;
		} else {
			if (data.currentType == null) {
				return null;
			}
			Integer step = TYPE_TO_STEP.get(data.currentType);
			label = step != null ? Integer.toString(step) : "?";
			rgb = BetterPrisonsClient.config.clueScrollNumberColor;
		}

		Font font = Minecraft.getInstance().font;
		// Fixed scale sized for the widest 2-digit case so 1- and 2-digit numbers render the same size.
		float scale = 14f / Math.max(font.width("00"), font.lineHeight);
		boolean outline = BetterPrisonsClient.config.clueScrollNumberOutline;
		if (outline) {
			scale *= 0.9f; // shrink slightly so the outline still fits the slot
		}
		int color = 0xFF000000 | (rgb & 0xFFFFFF);
		return new SlotOverlay(label, color, scale, true, Anchor.CENTER, outline);
	}

	/** Adds an "unmapped clue step" tooltip warning so unknown types can be reported. */
	public static void appendTooltip(ItemStack stack, List<Component> lines) {
		if (!BetterPrisonsClient.config.clueScrollUnmappedTooltipEnabled) {
			return;
		}
		ClueData data = parse(stack);
		if (data == null) {
			return;
		}
		List<String> unmapped = new ArrayList<>();
		for (String type : data.allTypes) {
			if (!TYPE_TO_STEP.containsKey(type) && !unmapped.contains(type)) {
				unmapped.add(type);
			}
		}
		if (unmapped.isEmpty()) {
			return;
		}
		Style style = Style.EMPTY.withColor(TextColor.fromRgb(0xFF5555)).withItalic(false);
		lines.add(Component.literal("[BP] Unmapped clue step! Please report to nishu06 on Discord:").setStyle(style));
		for (String type : unmapped) {
			lines.add(Component.literal("[BP]  - " + type).setStyle(style));
		}
	}

	private record ClueData(String currentType, List<String> allTypes, boolean completed) {}

	private static ClueData parse(ItemStack stack) {
		try {
			if (stack == null || stack.isEmpty()) {
				return null;
			}
			CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
			if (customData == null) {
				return null;
			}
			CompoundTag bukkit = customData.copyTag().getCompound("PublicBukkitValues").orElse(null);
			if (bukkit == null || bukkit.isEmpty()) {
				return null;
			}
			if (!"clue_scroll".equals(bukkit.getString("cosmicprisons:custom_item_id").orElse(""))) {
				return null;
			}
			String json = bukkit.getString("cosmicprisons:clue_scroll_data").orElse("");
			if (json.isEmpty()) {
				return null;
			}
			JsonObject root = JsonParser.parseString(json).getAsJsonObject();
			JsonArray clues = root.getAsJsonArray("clues");
			if (clues == null || clues.isEmpty()) {
				return null;
			}
			int rawIdx = root.has("currentClueIndex") ? root.get("currentClueIndex").getAsInt() : 0;

			List<String> allTypes = new ArrayList<>();
			boolean allCompleted = true;
			for (int i = 0; i < clues.size(); i++) {
				JsonObject c = clues.get(i).getAsJsonObject();
				if (c.has("type")) {
					allTypes.add(c.get("type").getAsString());
				}
				if (!(c.has("completed") && c.get("completed").getAsBoolean())) {
					allCompleted = false;
				}
			}
			boolean completed = allCompleted || rawIdx >= clues.size();
			int idx = rawIdx;
			if (idx < 0 || idx >= clues.size()) {
				idx = clues.size() - 1;
			}
			String currentType = clues.get(idx).getAsJsonObject().get("type").getAsString();
			return new ClueData(currentType, allTypes, completed);
		} catch (Exception e) {
			return null;
		}
	}
}
