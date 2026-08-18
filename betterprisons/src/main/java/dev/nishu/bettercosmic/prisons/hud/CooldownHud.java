package dev.nishu.bettercosmic.prisons.hud;

import dev.nishu.bettercosmic.prisons.BetterPrisons;
import dev.nishu.bettercosmic.prisons.client.BetterPrisonsClient;
import dev.nishu.bettercosmic.prisons.config.PrisonsConfig;
import dev.nishu.bettercosmic.prisons.util.JsonLoader;
import dev.nishu.bettercosmic.shared.hud.BaseHud;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3x2fStack;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Cooldown HUD: tracks active command/ability cooldowns with timers and icons. Cooldowns start
 * either when a command is sent (command-triggered) or when a confirming server chat message arrives
 * (chat-triggered), and count down until they expire. Ported from BetterPrisons (Yarn → Mojang);
 * fed by the client's {@code ClientSendMessageEvents.COMMAND} / {@code ClientReceiveMessageEvents.GAME}
 * listeners rather than BP's chat mixins.
 */
public class CooldownHud extends BaseHud {

	private static final Pattern NEAR_PATTERN =
			Pattern.compile("There are \\d+ player\\(s\\) to a \\d+ block radius of you");
	private static final Pattern PULSE_PATTERN =
			Pattern.compile("Meteorite pulse found \\d+ meteorites? within \\d+ blocks");

	public List<CommandDef> definitions = new ArrayList<>();
	public List<ActiveCooldown> activeCooldowns = new ArrayList<>();

	private boolean peacefulMiningDisabledByCombat = false;
	private boolean peacefulMiningStateBeforeCombat = false;

	public CooldownHud() {
		super("cooldown");
	}

	private static PrisonsConfig cfg() {
		return BetterPrisonsClient.config;
	}

	public void loadFromDefinitions() {
		definitions = JsonLoader.loadCommands();
	}

	private boolean isCommandEnabled(String displayName) {
		return switch (displayName) {
			case "Home" -> cfg().homeEnabled;
			case "Jet" -> cfg().jetEnabled;
			case "Feed" -> cfg().feedEnabled;
			case "Fix" -> cfg().fixEnabled;
			case "Combat" -> cfg().combatEnabled;
			case "tpa" -> cfg().tpaEnabled;
			case "tpahere" -> cfg().tpahereEnabled;
			case "Dangle" -> cfg().dangleEnabled;
			case "Adangle" -> cfg().adangleEnabled;
			default -> true;
		};
	}

	private int getCommandColor(String displayName) {
		return switch (displayName) {
			case "Home" -> cfg().homeColor;
			case "Jet" -> cfg().jetColor;
			case "Feed" -> cfg().feedColor;
			case "Fix" -> cfg().fixColor;
			case "Combat" -> cfg().combatColor;
			case "tpa" -> cfg().tpaColor;
			case "tpahere" -> cfg().tpahereColor;
			case "Dangle" -> cfg().dangleColor;
			case "Adangle" -> cfg().adangleColor;
			default -> 0xFFFFFF;
		};
	}

	/** Called when the player sends a command (via {@code ClientSendMessageEvents.COMMAND}). */
	public void onCommandSent(String command) {
		for (CommandDef def : definitions) {
			if (!matches(command, def)) {
				continue;
			}
			if (!isCommandEnabled(def.displayName)) {
				return;
			}
			// Commands whose confirmation is a chat message set their pattern dynamically here;
			// skip if this cooldown is already ticking.
			if (command.startsWith("/home ")) {
				if (hasActive(def.displayName)) {
					return;
				}
				String homeName = command.split(" ")[1].toLowerCase();
				if (homeName.isEmpty() || homeName.contains(" ") || homeName.equals("delete")
						|| homeName.equals("list") || homeName.equals("set")) {
					return;
				}
				def.chatPattern = String.format("§a§l(!) §aTeleported to §a§n%s§a!", homeName);
			}
			if (def.command.equals("/fix")) {
				if (hasActive(def.displayName)) {
					return;
				}
				def.chatPattern = "§a§l(!) §aYour item has been restored!";
			}
			if (def.command.equals("/jet")) {
				if (hasActive(def.displayName)) {
					return;
				}
				def.chatPattern = "§a§lJETPACK ENGAGED: §f§lprepare for launch!";
			}
			if (def.command.equals("/feed")) {
				if (hasActive(def.displayName)) {
					return;
				}
				def.chatPattern = "§a§l(!) §aYou have been satiated.";
			}
			if (command.startsWith("/tpa ")) {
				if (hasActive(def.displayName)) {
					return;
				}
				String[] parts = command.split(" ");
				if (parts.length >= 2) {
					def.chatPattern = String.format("§a§l(!) §aSent a teleport request to §a§n%s§a!", parts[1]);
				}
			}
			if (command.startsWith("/tpahere ")) {
				if (hasActive(def.displayName)) {
					return;
				}
				String[] parts = command.split(" ");
				if (parts.length >= 2) {
					def.chatPattern = String.format("§a§l(!) §aAsked §a§n%s§a to teleport to you!", parts[1]);
				}
			}

			// Command-triggered (no confirming chat message): start the cooldown immediately.
			if (def.chatPattern == null || def.chatPattern.isEmpty()) {
				addCooldown(def.displayName, def.cooldown, def.icon, getCommandColor(def.displayName));
			}
			break;
		}
	}

	/** Called for each received chat message (via {@code ClientReceiveMessageEvents.GAME}). */
	public void onChatReceived(String message) {
		if (message.equals("§c§l(!) §c/jet cancelled.")) {
			activeCooldowns.removeIf(cd -> cd.name.equals("Jet"));
			return;
		}
		if (message.startsWith("§cYou must wait §e") && message.contains("§cbefore armor dangling again")
				|| message.equals("§cYou already have items dangling!")) {
			activeCooldowns.removeIf(cd -> cd.name.equals("Adangle"));
			return;
		}
		if (cfg().nearEnabled && NEAR_PATTERN.matcher(message.replaceAll("§.", "")).find()) {
			addCooldown("Near", 45, "minecraft:compass", cfg().nearColor);
			return;
		}
		if (cfg().pulseEnabled && PULSE_PATTERN.matcher(message.replaceAll("§.", "")).find()) {
			addCooldown("Pulse", 300, "minecraft:redstone_torch", cfg().pulseColor);
			return;
		}
		if (message.contains("§a§l(!) §aUpdated your teleport request to §a§n") && message.endsWith("§a.")) {
			triggerByCommand("/tpa ");
			return;
		}
		if (message.contains("§a§l(!) §aUpdated your teleport request for §a§n")
				&& message.endsWith("§a to come to you.")) {
			triggerByCommand("/tpahere ");
			return;
		}

		String strippedMessage = message.replaceAll("§.", "");
		for (CommandDef def : definitions) {
			if (def.chatPattern == null || def.chatPattern.isEmpty()) {
				continue;
			}
			if (strippedMessage.equals(def.chatPattern.replaceAll("§.", ""))) {
				if (isCommandEnabled(def.displayName)) {
					addCooldown(def.displayName, def.cooldown, def.icon, getCommandColor(def.displayName));
				}
				if (def.command.equals("/fix") || def.command.equals("/home") || def.command.equals("/jet")
						|| def.command.equals("/feed") || def.command.equals("/tpa ") || def.command.equals("/tpahere ")) {
					def.chatPattern = null; // one-shot dynamic pattern
				}
				break;
			}
		}
	}

	private void triggerByCommand(String command) {
		for (CommandDef def : definitions) {
			if (def.command.equals(command)) {
				if (isCommandEnabled(def.displayName)) {
					addCooldown(def.displayName, def.cooldown, def.icon, getCommandColor(def.displayName));
				}
				break;
			}
		}
	}

	private boolean hasActive(String name) {
		for (ActiveCooldown cd : activeCooldowns) {
			if (cd.name.equals(name)) {
				return true;
			}
		}
		return false;
	}

	/** Refreshes the Combat cooldown to full, if active. Called when hitting/being hit by a player. */
	public void resetCombatCooldown() {
		for (ActiveCooldown cd : activeCooldowns) {
			if (cd.name.equals("Combat")) {
				cd.startTime = System.currentTimeMillis();
				return;
			}
		}
	}

	public void addCooldown(String name, int durationSeconds) {
		addCooldown(name, durationSeconds, null, 0xFFFFFF);
	}

	public void addCooldown(String name, int durationSeconds, String icon, int color) {
		if (hasActive(name)) {
			return;
		}
		activeCooldowns.add(new ActiveCooldown(name, durationSeconds, System.currentTimeMillis(), icon, color));

		if (name.equals("Combat") && cfg().peacefulMiningDisableOnCombat) {
			peacefulMiningStateBeforeCombat = cfg().peacefulMiningEnabled;
			if (peacefulMiningStateBeforeCombat) {
				cfg().peacefulMiningEnabled = false;
				cfg().save();
				peacefulMiningDisabledByCombat = true;
			}
		}
	}

	@Override
	public void tick() {
		this.enabled = cfg().cooldownHudEnabled;
		boolean combatWasActive = hasActive("Combat");
		long now = System.currentTimeMillis();
		activeCooldowns.removeIf(cd -> cd.isExpired(now));
		boolean combatIsActive = hasActive("Combat");

		if (combatWasActive && !combatIsActive && peacefulMiningDisabledByCombat) {
			cfg().peacefulMiningEnabled = peacefulMiningStateBeforeCombat;
			cfg().save();
			peacefulMiningDisabledByCombat = false;
		}
	}

	private static final Component TITLE =
			Component.literal("Cooldown HUD").setStyle(Style.EMPTY.withUnderlined(true).withBold(true));

	@Override
	public void render(GuiGraphics ctx, Minecraft client) {
		this.scale = cfg().cooldownHudScale / 100.0f;

		boolean showTitle = cfg().showCooldownHudTitle;
		boolean hasContent = !activeCooldowns.isEmpty();
		if (!enabled || (!showTitle && !hasContent)) {
			return;
		}

		int titleHeight = 0;
		int titleWidth = 0;
		if (showTitle) {
			titleWidth = (int) (client.font.width(TITLE) * scale);
			titleHeight = scaled(12);
		}

		int iconSpace = scaled(20);
		int maxWidth = titleWidth;
		if (hasContent) {
			for (ActiveCooldown cd : activeCooldowns) {
				int nameWidth = (int) (client.font.width(cd.name) * scale);
				int timeWidth = (int) (client.font.width(cd.getRemainingSeconds() + "s") * scale);
				maxWidth = Math.max(maxWidth, iconSpace + nameWidth + scaled(10) + timeWidth);
			}
		}

		int bgWidth = maxWidth;
		int contentHeight = hasContent ? activeCooldowns.size() * scaled(18) : 0;
		int bgHeight = titleHeight + contentHeight;

		int bgColor = (cfg().cooldownBgOpacity << 24) | (cfg().cooldownBgColor & 0xFFFFFF);
		int borderColor = (cfg().cooldownBorderOpacity << 24) | (cfg().cooldownBorderColor & 0xFFFFFF);
		int thickness = scaled(cfg().cooldownBorderThickness);
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
			int titleColor = 0xFF000000 | cfg().cooldownHudTitleColor;
			matrices.pushMatrix();
			matrices.scale(scale, scale);
			matrices.translate(x / scale, y / scale);
			ctx.drawString(client.font, TITLE, 0, 0, titleColor, true);
			matrices.popMatrix();
			yOffset += titleHeight;
		}

		if (hasContent) {
			int rowHeight = scaled(18);
			int iconYOffset = scaled(1);
			int textYOffset = scaled(4);

			for (ActiveCooldown cd : activeCooldowns) {
				int textColor = 0xFF000000 | cd.color;

				if (cd.icon != null && !cd.icon.isEmpty()) {
					try {
						ItemStack iconStack = new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse(cd.icon)));
						if (!iconStack.isEmpty()) {
							matrices.pushMatrix();
							matrices.scale(scale, scale);
							matrices.translate(x / scale, (y + yOffset + iconYOffset) / scale);
							ctx.renderItem(iconStack, 0, 0);
							matrices.popMatrix();
						}
					} catch (Exception e) {
						BetterPrisons.LOGGER.warn("Failed to render cooldown icon: {}", cd.icon);
					}
				}

				matrices.pushMatrix();
				matrices.scale(scale, scale);
				matrices.translate((x + iconSpace) / scale, (y + yOffset + textYOffset) / scale);
				ctx.drawString(client.font, Component.literal(cd.name), 0, 0, textColor, true);
				matrices.popMatrix();

				int nameWidth = client.font.width(cd.name);
				int timerX = iconSpace + (int) (nameWidth * scale) + scaled(10);
				matrices.pushMatrix();
				matrices.scale(scale, scale);
				matrices.translate((x + timerX) / scale, (y + yOffset + textYOffset) / scale);
				ctx.drawString(client.font, Component.literal(cd.getRemainingSeconds() + "s"), 0, 0, textColor, true);
				matrices.popMatrix();

				yOffset += rowHeight;
			}
		}
	}

	@Override
	public int getWidth() {
		Minecraft client = Minecraft.getInstance();
		if (client == null || client.font == null) {
			return scaled(120);
		}
		int titleWidth = cfg().showCooldownHudTitle ? (int) (client.font.width("Cooldown HUD") * scale) : 0;
		int iconSpace = scaled(20);
		int maxWidth = titleWidth;
		for (ActiveCooldown cd : activeCooldowns) {
			int nameWidth = (int) (client.font.width(cd.name) * scale);
			int timeWidth = (int) (client.font.width(cd.getRemainingSeconds() + "s") * scale);
			maxWidth = Math.max(maxWidth, iconSpace + nameWidth + scaled(10) + timeWidth);
		}
		int padding = 4;
		if (scale < 1) {
			padding = scaled(padding);
		}
		return maxWidth + (padding * 2);
	}

	@Override
	public int getHeight() {
		int titleHeight = cfg().showCooldownHudTitle ? scaled(10) : 0;
		return titleHeight + activeCooldowns.size() * scaled(18);
	}

	/** A configured command whose use starts a cooldown. */
	public static class CommandDef {
		public String command;
		public String matchType; // "exact" or "startsWith"
		public int cooldown;     // seconds
		public String displayName;
		public String chatPattern; // optional confirming server message
		public List<String> aliases;
		public String icon;      // optional item id, e.g. "minecraft:blaze_powder"
	}

	/** A currently-ticking cooldown. */
	public static class ActiveCooldown {
		public String name;
		public int duration;
		public long startTime;
		public String icon;
		public int color;

		public ActiveCooldown(String name, int duration, long startTime, String icon, int color) {
			this.name = name;
			this.duration = duration;
			this.startTime = startTime;
			this.icon = icon;
			this.color = color;
		}

		public int getRemainingSeconds() {
			long elapsed = System.currentTimeMillis() - startTime;
			return Math.max(0, duration - (int) (elapsed / 1000));
		}

		public boolean isExpired(long now) {
			return now > startTime + (duration * 1000L);
		}
	}

	private boolean matches(String command, CommandDef def) {
		if (def.command != null && !def.command.isEmpty()) {
			if ("exact".equals(def.matchType)) {
				if (command.equalsIgnoreCase(def.command)) {
					return true;
				}
			} else if (command.toLowerCase().startsWith(def.command.toLowerCase())) {
				return true;
			}
		}
		if (def.aliases != null) {
			for (String alias : def.aliases) {
				if ("exact".equals(def.matchType)) {
					if (command.equalsIgnoreCase(alias)) {
						return true;
					}
				} else if (command.toLowerCase().startsWith(alias.toLowerCase())) {
					return true;
				}
			}
		}
		return false;
	}
}
