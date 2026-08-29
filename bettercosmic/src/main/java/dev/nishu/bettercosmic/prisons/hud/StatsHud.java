package dev.nishu.bettercosmic.prisons.hud;

import dev.nishu.bettercosmic.prisons.client.BetterPrisonsClient;
import dev.nishu.bettercosmic.prisons.config.PrisonsConfig;
import dev.nishu.bettercosmic.shared.hud.BaseHud;
import dev.nishu.bettercosmic.shared.util.NumberFormatUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import org.joml.Matrix3x2fStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Stats HUD: tracks XP and Cosmic Energy over a mining session — current values, per-hour/minute
 * rates, session totals, session duration, and time-to-next-level. Data comes from the scoreboard
 * sidebar (XP/level) and the held pickaxe's lore (energy), so it's fully client-local. Ported from
 * BetterPrisons (Yarn → Mojang); extends the shared {@link BaseHud}.
 */
public class StatsHud extends BaseHud {

	// Current values (parsed from the scoreboard / pickaxe).
	public long currentXP = 0;
	public long currentEnergy = 0;
	public long xpNeededForNextLevel = 0;
	public int targetLevel = 0;

	// Session tracking.
	public long sessionStartTime = 0;
	public boolean trackingActive = false;
	public boolean paused = false;
	public long totalPauseDuration = 0;
	public long pauseStartTime = 0;

	private final List<XPGain> xpGainHistory = new ArrayList<>();
	private long lastTickXP = 0;
	public long totalSessionXPGained = 0;

	private final List<EnergyReading> energyReadings = new ArrayList<>();
	private ItemStack lastPickaxe = ItemStack.EMPTY;
	public long totalSessionEnergyGained = 0;

	private long cachedXPPerHour = 0;
	private long cachedEnergyPerHour = 0;
	private long cachedXPPerMinute = 0;
	private long cachedEnergyPerMinute = 0;
	private long lastStatsUpdateTime = 0;

	public StatsHud() {
		super("stats");
	}

	private static PrisonsConfig cfg() {
		return BetterPrisonsClient.config;
	}

	@Override
	public void tick(Minecraft client) {
		this.enabled = cfg().statsHudEnabled;
		if (client.level == null || client.player == null) {
			return;
		}

		parseScoreboard(client.level.getScoreboard());

		ItemStack currentPickaxe = client.player.getMainHandItem();
		long energyFromPickaxe = parseEnergyFromPickaxe(currentPickaxe);
		if (energyFromPickaxe > 0) {
			currentEnergy = energyFromPickaxe;
		}

		if (!trackingActive && (currentXP > 0 || currentEnergy > 0)) {
			startTracking();
		}

		if (!trackingActive || paused) {
			return;
		}

		boolean pickaxeChanged = !ItemStack.matches(currentPickaxe, lastPickaxe);

		if (!pickaxeChanged && lastTickXP > 0 && currentXP > lastTickXP) {
			long xpGain = currentXP - lastTickXP;
			xpGainHistory.add(new XPGain(xpGain, System.currentTimeMillis()));
			totalSessionXPGained += xpGain;
		}

		if (energyFromPickaxe > 0) {
			long now = System.currentTimeMillis();
			boolean energyDecreased = false;
			if (!energyReadings.isEmpty()) {
				EnergyReading last = energyReadings.get(energyReadings.size() - 1);
				if (currentEnergy < last.energy) {
					energyDecreased = true;
					energyReadings.clear();
				}
			}
			if (!energyDecreased) {
				energyReadings.add(new EnergyReading(currentEnergy, now));
				long oneMinuteAgo = now - 60000;
				energyReadings.removeIf(r -> r.timestamp < oneMinuteAgo);
				if (energyReadings.size() >= 2) {
					EnergyReading prev = energyReadings.get(energyReadings.size() - 2);
					EnergyReading cur = energyReadings.get(energyReadings.size() - 1);
					if (cur.energy > prev.energy) {
						totalSessionEnergyGained += cur.energy - prev.energy;
					}
				}
			}
		}

		long oneMinuteAgo = System.currentTimeMillis() - 60000;
		xpGainHistory.removeIf(g -> g.timestamp < oneMinuteAgo);
		lastTickXP = currentXP;
		lastPickaxe = currentPickaxe.copy();
	}

	/** Reads current energy from a pickaxe's lore: the value 2 lines below "Cosmic Energy". */
	private long parseEnergyFromPickaxe(ItemStack pickaxe) {
		if (pickaxe == null || pickaxe.isEmpty()) {
			return -1;
		}
		try {
			ItemLore lore = pickaxe.get(DataComponents.LORE);
			if (lore == null || lore.lines().isEmpty()) {
				return -1;
			}
			List<String> loreLines = new ArrayList<>();
			for (Component line : lore.lines()) {
				loreLines.add(line.getString().replaceAll("§.", ""));
			}
			for (int i = 0; i < loreLines.size(); i++) {
				if (loreLines.get(i).contains("Cosmic Energy")) {
					int energyLineIndex = i + 2;
					if (energyLineIndex < loreLines.size()) {
						String energyLine = loreLines.get(energyLineIndex);
						if (energyLine.contains("(") && energyLine.contains("/")) {
							int startIdx = energyLine.indexOf("(");
							int slashIdx = energyLine.indexOf("/");
							if (startIdx != -1 && slashIdx > startIdx) {
								String energyText = energyLine.substring(startIdx + 1, slashIdx).trim();
								long parsed = NumberFormatUtil.parse(energyText);
								if (parsed > 0) {
									return parsed;
								}
							}
						}
					}
					break;
				}
			}
		} catch (Exception e) {
			// ignore parse errors
		}
		return -1;
	}

	private void parseScoreboard(Scoreboard scoreboard) {
		if (scoreboard == null) {
			return;
		}
		Objective objective = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR);
		if (objective == null) {
			return;
		}
		Collection<PlayerScoreEntry> entries = scoreboard.listPlayerScores(objective);

		Integer levelValue = null;
		for (PlayerScoreEntry entry : entries) {
			try {
				String stripped = displayText(scoreboard, entry).getString().replaceAll("§.", "");
				if (stripped.trim().startsWith("Level")) {
					levelValue = entry.value();
					break;
				}
			} catch (Exception ignored) {
			}
		}

		for (PlayerScoreEntry entry : entries) {
			try {
				String stripped = displayText(scoreboard, entry).getString().replaceAll("§.", "");
				if (levelValue != null && entry.value() == levelValue - 1
						&& stripped.contains("(") && stripped.contains("XP")) {
					int startIdx = stripped.indexOf("(");
					int endIdx = stripped.indexOf("XP");
					if (startIdx != -1 && endIdx > startIdx) {
						long parsed = NumberFormatUtil.parse(stripped.substring(startIdx + 1, endIdx).trim());
						if (parsed > 0) {
							currentXP = parsed;
						}
					}
				}
			} catch (Exception ignored) {
			}
		}

		Integer progressValue = null;
		for (PlayerScoreEntry entry : entries) {
			try {
				String stripped = displayText(scoreboard, entry).getString().replaceAll("§.", "");
				if (stripped.trim().equals("Progress")) {
					progressValue = entry.value();
					break;
				}
			} catch (Exception ignored) {
			}
		}

		if (progressValue != null) {
			for (PlayerScoreEntry entry : entries) {
				try {
					if (entry.value() == progressValue - 1) {
						String stripped = displayText(scoreboard, entry).getString().replaceAll("§.", "").strip();
						String[] parts = stripped.split(" ");
						if (parts.length > 0) {
							long xpNeeded = NumberFormatUtil.parse(parts[0]);
							if (xpNeeded > 0) {
								xpNeededForNextLevel = xpNeeded;
							}
						}
						if (stripped.contains("to")) {
							String afterTo = stripped.substring(stripped.indexOf("to") + 2).trim();
							String levelStr = afterTo.replaceAll("[^0-9]", "");
							if (!levelStr.isEmpty()) {
								targetLevel = Integer.parseInt(levelStr);
							}
						}
						break;
					}
				} catch (Exception ignored) {
				}
			}
		}
	}

	private Component displayText(Scoreboard scoreboard, PlayerScoreEntry entry) {
		Component direct = entry.display();
		if (direct != null) {
			return direct;
		}
		PlayerTeam team = scoreboard.getPlayersTeam(entry.owner());
		if (team != null) {
			return PlayerTeam.formatNameForTeam(team, Component.literal(entry.owner()));
		}
		return Component.literal(entry.owner());
	}

	public void startTracking() {
		sessionStartTime = System.currentTimeMillis();
		xpGainHistory.clear();
		energyReadings.clear();
		lastTickXP = currentXP;
		totalSessionXPGained = 0;
		totalSessionEnergyGained = 0;
		lastStatsUpdateTime = 0;
		cachedXPPerHour = cachedEnergyPerHour = cachedXPPerMinute = cachedEnergyPerMinute = 0;
		totalPauseDuration = 0;
		pauseStartTime = 0;
		trackingActive = true;
	}

	public void resetTracking() {
		sessionStartTime = System.currentTimeMillis();
		xpGainHistory.clear();
		energyReadings.clear();
		lastTickXP = currentXP;
		totalSessionXPGained = 0;
		totalSessionEnergyGained = 0;
		lastStatsUpdateTime = sessionStartTime;
		cachedXPPerHour = cachedEnergyPerHour = cachedXPPerMinute = cachedEnergyPerMinute = 0;
		totalPauseDuration = 0;
		pauseStartTime = 0;
	}

	public long getXPPerHour() {
		return getXPPerMinute() * 60;
	}

	public long getXPPerMinute() {
		if (!trackingActive || xpGainHistory.isEmpty()) {
			return 0;
		}
		long totalGains = 0;
		for (XPGain gain : xpGainHistory) {
			totalGains += gain.amount;
		}
		if (totalGains == 0) {
			return 0;
		}
		double elapsedSeconds = (System.currentTimeMillis() - xpGainHistory.get(0).timestamp) / 1000.0;
		if (elapsedSeconds < 5.0) {
			return 0;
		}
		return (long) ((totalGains / elapsedSeconds) * 60.0);
	}

	public long getEnergyPerHour() {
		return getEnergyPerMinute() * 60;
	}

	public long getEnergyPerMinute() {
		if (!trackingActive || energyReadings.size() < 2) {
			return 0;
		}
		EnergyReading oldest = energyReadings.get(0);
		EnergyReading newest = energyReadings.get(energyReadings.size() - 1);
		long energyGained = newest.energy - oldest.energy;
		if (energyGained <= 0) {
			return 0;
		}
		double elapsedSeconds = (newest.timestamp - oldest.timestamp) / 1000.0;
		if (elapsedSeconds < 5.0) {
			return 0;
		}
		return (long) ((energyGained / elapsedSeconds) * 60.0);
	}

	public String getSessionDuration() {
		if (sessionStartTime == 0) {
			return "0:00:00";
		}
		long elapsed = System.currentTimeMillis() - sessionStartTime - totalPauseDuration;
		if (paused && pauseStartTime > 0) {
			elapsed -= System.currentTimeMillis() - pauseStartTime;
		}
		long millis = elapsed % 1000;
		long seconds = (elapsed / 1000) % 60;
		long minutes = (elapsed / 60000) % 60;
		long hours = elapsed / 3600000;
		if (cfg().statsShowMillisOnSessionDuration) {
			return String.format("%d:%02d:%02d.%03d", hours, minutes, seconds, millis);
		}
		return String.format("%d:%02d:%02d", hours, minutes, seconds);
	}

	public void togglePause() {
		if (paused) {
			if (pauseStartTime > 0) {
				totalPauseDuration += System.currentTimeMillis() - pauseStartTime;
				pauseStartTime = 0;
			}
			paused = false;
		} else {
			pauseStartTime = System.currentTimeMillis();
			paused = true;
		}
	}

	public String getTimeTillLevelUp() {
		if (!trackingActive || cachedXPPerMinute == 0 || xpNeededForNextLevel == 0) {
			return "Time until lvl " + targetLevel + ": --:--";
		}
		double minutesNeeded = (double) xpNeededForNextLevel / cachedXPPerMinute;
		long totalSeconds = (long) (minutesNeeded * 60);
		long hours = totalSeconds / 3600;
		long minutes = (totalSeconds % 3600) / 60;
		long seconds = totalSeconds % 60;
		if (hours >= 1) {
			return String.format("Time until lvl %d: %d:%02d:%02d", targetLevel, hours, minutes, seconds);
		}
		return String.format("Time until lvl %d: %02d:%02d", targetLevel, minutes, seconds);
	}

	/** Builds the ordered list of visible stat lines (used by render and getWidth/Height). */
	private List<String> visibleElements() {
		List<String> el = new ArrayList<>();
		PrisonsConfig c = cfg();
		if (c.statsShowCurrentXP) {
			el.add("XP: " + NumberFormatUtil.format(currentXP));
		}
		if (c.statsShowTimeTillLevelUp) {
			el.add(getTimeTillLevelUp());
		}
		if (c.statsShowXPPerHour) {
			el.add("XP/hr: " + NumberFormatUtil.format(cachedXPPerHour));
		}
		if (c.statsShowXPPerMinute) {
			el.add("XP/min: " + NumberFormatUtil.format(cachedXPPerMinute));
		}
		if (c.statsShowSessionXP) {
			el.add("Session XP: " + NumberFormatUtil.format(totalSessionXPGained));
		}
		if (c.statsShowCurrentCE) {
			el.add("CE: " + NumberFormatUtil.format(currentEnergy));
		}
		if (c.statsShowCEPerHour) {
			el.add("CE/hr: " + NumberFormatUtil.format(cachedEnergyPerHour));
		}
		if (c.statsShowCEPerMinute) {
			el.add("CE/min: " + NumberFormatUtil.format(cachedEnergyPerMinute));
		}
		if (c.statsShowSessionCE) {
			el.add("Session CE: " + NumberFormatUtil.format(totalSessionEnergyGained));
		}
		if (c.statsShowSessionDuration) {
			el.add("Session: " + (paused ? "(P) " : "") + getSessionDuration());
		}
		return el;
	}

	private int elementColor(String text) {
		PrisonsConfig c = cfg();
		int rgb;
		if (text.startsWith("XP:")) {
			rgb = c.statsCurrentXPColor;
		} else if (text.startsWith("XP/hr:")) {
			rgb = c.statsXPPerHourColor;
		} else if (text.startsWith("XP/min:")) {
			rgb = c.statsXPPerMinuteColor;
		} else if (text.startsWith("Session XP:")) {
			rgb = c.statsSessionXPColor;
		} else if (text.startsWith("CE:")) {
			rgb = c.statsCurrentCEColor;
		} else if (text.startsWith("CE/hr:")) {
			rgb = c.statsCEPerHourColor;
		} else if (text.startsWith("CE/min:")) {
			rgb = c.statsCEPerMinuteColor;
		} else if (text.startsWith("Session CE:")) {
			rgb = c.statsSessionCEColor;
		} else if (text.startsWith("Session:")) {
			rgb = c.statsSessionDurationColor;
		} else if (text.startsWith("Time until lvl")) {
			rgb = c.statsTimeTillLevelUpColor;
		} else {
			return 0xFFFFFFFF;
		}
		return 0xFF000000 | rgb;
	}

	private static final Component TITLE =
			Component.literal("Stats HUD").setStyle(Style.EMPTY.withUnderlined(true).withBold(true));

	@Override
	public void render(GuiGraphics ctx, Minecraft client) {
		if (!enabled || client.player == null) {
			return;
		}
		this.scale = cfg().statsHudScale / 100.0f;

		// Only display while holding a pickaxe.
		if (!client.player.getMainHandItem().getItem().getDescriptionId().contains("pickaxe")) {
			return;
		}

		long now = System.currentTimeMillis();
		if (now - lastStatsUpdateTime >= 1000 && !paused) {
			cachedXPPerHour = getXPPerHour();
			cachedEnergyPerHour = getEnergyPerHour();
			cachedXPPerMinute = getXPPerMinute();
			cachedEnergyPerMinute = getEnergyPerMinute();
			lastStatsUpdateTime = now;
		}

		boolean showTitle = cfg().showStatsHudTitle;
		List<String> visible = visibleElements();
		boolean hasContent = !visible.isEmpty();
		if (!showTitle && !hasContent) {
			return;
		}

		int titleHeight = 0;
		int titleWidth = 0;
		if (showTitle) {
			titleWidth = (int) (client.font.width(TITLE) * scale);
			titleHeight = scaled(12);
		}

		int maxWidth = titleWidth;
		for (String text : visible) {
			maxWidth = Math.max(maxWidth, (int) (client.font.width(text) * scale));
		}

		int bgWidth = scaled((int) (maxWidth / scale) + 4);
		int contentHeight = hasContent ? scaled(visible.size() * 12 - 2) : 0;
		int bgHeight = titleHeight + contentHeight;

		int bgColor = (cfg().statsBgOpacity << 24) | (cfg().statsBgColor & 0xFFFFFF);
		int borderColor = (cfg().statsBorderOpacity << 24) | (cfg().statsBorderColor & 0xFFFFFF);
		int thickness = cfg().statsBorderThickness;

		ctx.fill(x - 2, y - 2, x + bgWidth + 2, y + bgHeight, bgColor);
		ctx.fill(x - 2, y - 2 - thickness, x + bgWidth + 2, y - 2, borderColor);
		ctx.fill(x - 2, y + bgHeight, x + bgWidth + 2, y + bgHeight + thickness, borderColor);
		ctx.fill(x - 2 - thickness, y - 2 - thickness, x - 2, y + bgHeight + thickness, borderColor);
		ctx.fill(x + bgWidth + 2, y - 2 - thickness, x + bgWidth + 2 + thickness, y + bgHeight + thickness, borderColor);

		Matrix3x2fStack matrices = ctx.pose();
		int yOffset = 0;

		if (showTitle) {
			int titleColor = 0xFF000000 | cfg().statsHudTitleColor;
			matrices.pushMatrix();
			matrices.scale(scale, scale);
			matrices.translate(x / scale, y / scale);
			ctx.drawString(client.font, TITLE, 0, 0, titleColor, true);
			matrices.popMatrix();
			yOffset += titleHeight;
		}

		for (String text : visible) {
			int color = elementColor(text);
			matrices.pushMatrix();
			matrices.scale(scale, scale);
			matrices.translate(x / scale, (y + yOffset) / scale);
			ctx.drawString(client.font, Component.literal(text), 0, 0, color, true);
			matrices.popMatrix();
			yOffset += scaled(12);
		}
	}

	@Override
	public int getWidth() {
		Minecraft client = Minecraft.getInstance();
		if (client == null || client.font == null) {
			return scaled(150);
		}
		int titleWidth = cfg().showStatsHudTitle ? (int) (client.font.width(TITLE) * scale) : 0;
		int maxWidth = titleWidth;
		for (String text : visibleElements()) {
			maxWidth = Math.max(maxWidth, (int) (client.font.width(text) * scale));
		}
		int bgWidth = scaled((int) (maxWidth / scale) + 4);
		return bgWidth + 4;
	}

	@Override
	public int getHeight() {
		int titleHeight = cfg().showStatsHudTitle ? scaled(10) : 0;
		return titleHeight + visibleElements().size() * 12;
	}

	private static class XPGain {
		final long amount;
		final long timestamp;

		XPGain(long amount, long timestamp) {
			this.amount = amount;
			this.timestamp = timestamp;
		}
	}

	private static class EnergyReading {
		final long energy;
		final long timestamp;

		EnergyReading(long energy, long timestamp) {
			this.energy = energy;
			this.timestamp = timestamp;
		}
	}
}
