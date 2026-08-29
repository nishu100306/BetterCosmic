package dev.nishu.bettercosmic.prisons.hud;

import dev.nishu.bettercosmic.prisons.BetterPrisons;
import dev.nishu.bettercosmic.prisons.client.BetterPrisonsClient;
import dev.nishu.bettercosmic.prisons.config.PrisonsConfig;
import dev.nishu.bettercosmic.shared.hud.BaseHud;
import dev.nishu.bettercosmic.shared.notification.Sounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3x2fStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Events HUD: tracks Cosmic Prisons world events parsed from chat — falling/crashed meteors, ore
 * merchants, badlands bandit rushes, and meteorite showers — and renders a stacked list with a
 * live countdown, icon, and coordinates for each. It also mirrors every event into the
 * {@link dev.nishu.bettercosmic.prisons.waypoint.WaypointManager} so beams and screen-edge markers
 * can point at them, and (via the {@code updateScheduledEvent} hook) shows single-line countdowns for
 * Cosmic-API scheduled events.
 *
 * <p>Ported from BetterPrisons' {@code hud/EventsHud} (Yarn → Mojang: {@code DrawContext} →
 * {@code GuiGraphics}, {@code Text} → {@code Component}, {@code textRenderer} → {@code font},
 * {@code Registries.ITEM.get} → {@code BuiltInRegistries.ITEM.getValue}, sounds routed through the
 * shared {@link Sounds} catalog). Chat detection now arrives through {@code ClientReceiveMessageEvents}
 * (see {@code BetterPrisonsClient}) instead of BetterPrisons' {@code ChatReceiveMixin}.
 */
public class EventsHud extends BaseHud {

	// --- Meteors ---
	private final List<MeteorInfo> activeMeteors = new ArrayList<>();
	private static final long METEOR_TIMEOUT_MS = 20 * 60 * 1000L;
	private static final long NATURAL_METEOR_DURATION_MS = 7 * 60 * 1000L;
	private static final long SUMMONED_METEOR_DURATION_MS = 60 * 1000L;
	private static final long IMMINENT_GRACE_MS = 60 * 1000L;
	private static final Pattern COORDS_PATTERN = Pattern.compile("(-?\\d+)x,\\s*(-?\\d+)y,\\s*(-?\\d+)z");

	// --- Merchants ---
	private final List<MerchantInfo> activeMerchants = new ArrayList<>();

	// --- Bandit Rushes ---
	private final List<BanditRushInfo> activeBanditRushes = new ArrayList<>();

	// --- Scheduled events (Cosmic API: server.event.schedule.changed) ---
	private final Map<String, ScheduledEventInfo> scheduledEvents = new LinkedHashMap<>();
	private static final List<String> SCHEDULED_EVENT_ORDER = List.of();

	// --- Meteorite Showers ---
	private final List<MeteoriteShowerInfo> activeMeteoriteShowers = new ArrayList<>();
	private static final long METEORITE_SHOWER_WARNING_MS = 60 * 1000L;
	private static final long METEORITE_SHOWER_TIMEOUT_MS = 15 * 60 * 1000L;
	private static final Pattern SHOWER_COORDS_PATTERN =
			Pattern.compile("(-?\\d+)x,\\s*(-?\\d+)y,\\s*(-?\\d+)z(?:\\s*\\(([^)]+)\\))?");

	// -------------------------------------------------------------------------
	// Enums
	// -------------------------------------------------------------------------

	public enum MeteorType {
		NATURAL, SUMMONED
	}

	public enum MerchantType {
		COAL, IRON, LAPIS, REDSTONE, GOLD, DIAMOND, EMERALD, UNKNOWN;

		public static MerchantType fromString(String tierName) {
			switch (tierName.toUpperCase()) {
				case "COAL": return COAL;
				case "IRON": return IRON;
				case "LAPIS": return LAPIS;
				case "REDSTONE": return REDSTONE;
				case "GOLD": return GOLD;
				case "DIAMOND": return DIAMOND;
				case "EMERALD": return EMERALD;
				default: return UNKNOWN;
			}
		}

		public String getDefaultIconId() {
			switch (this) {
				case COAL: return "coal";
				case IRON: return "iron_ingot";
				case LAPIS: return "lapis_lazuli";
				case REDSTONE: return "redstone";
				case GOLD: return "gold_ingot";
				case DIAMOND: return "diamond";
				case EMERALD: return "emerald";
				default: return "nether_quartz_ore";
			}
		}

		public String getDisplayName() {
			String name = this.name().charAt(0) + this.name().substring(1).toLowerCase();
			return name + " Ore Merchant";
		}

		public boolean isEnabled(PrisonsConfig config) {
			switch (this) {
				case COAL: return config.coalMerchantEnabled;
				case IRON: return config.ironMerchantEnabled;
				case LAPIS: return config.lapisMerchantEnabled;
				case REDSTONE: return config.redstoneMerchantEnabled;
				case GOLD: return config.goldMerchantEnabled;
				case DIAMOND: return config.diamondMerchantEnabled;
				case EMERALD: return config.emeraldMerchantEnabled;
				default: return true;
			}
		}

		public int getHeadingColor(PrisonsConfig config) {
			switch (this) {
				case COAL: return config.coalMerchantHeadingColor;
				case IRON: return config.ironMerchantHeadingColor;
				case LAPIS: return config.lapisMerchantHeadingColor;
				case REDSTONE: return config.redstoneMerchantHeadingColor;
				case GOLD: return config.goldMerchantHeadingColor;
				case DIAMOND: return config.diamondMerchantHeadingColor;
				case EMERALD: return config.emeraldMerchantHeadingColor;
				default: return 0xFFFFFF;
			}
		}
	}

	/** The four badlands sub-worlds within {@code minecraft:badlands}, identified by coordinate bounds. */
	public enum BadlandsRegion {
		CHAIN(1073, -127, 1295, 95),
		GOLD(641, -127, 863, 95),
		IRON(641, 289, 863, 511),
		DIAMOND(1073, 289, 1295, 511);

		public final int minX, minZ, maxX, maxZ;

		BadlandsRegion(int x1, int z1, int x2, int z2) {
			this.minX = Math.min(x1, x2);
			this.minZ = Math.min(z1, z2);
			this.maxX = Math.max(x1, x2);
			this.maxZ = Math.max(z1, z2);
		}

		public boolean contains(int x, int z) {
			return x >= minX && x <= maxX && z >= minZ && z <= maxZ;
		}

		public static BadlandsRegion fromCoords(int x, int z) {
			for (BadlandsRegion r : values()) {
				if (r.contains(x, z)) {
					return r;
				}
			}
			return null;
		}

		public static BadlandsRegion getPlayerRegion() {
			Minecraft client = Minecraft.getInstance();
			if (client.player == null) {
				return null;
			}
			return fromCoords((int) client.player.getX(), (int) client.player.getZ());
		}
	}

	// -------------------------------------------------------------------------
	// Constructor
	// -------------------------------------------------------------------------

	public EventsHud() {
		super("events");
	}

	private static PrisonsConfig cfg() {
		return BetterPrisonsClient.config;
	}

	// -------------------------------------------------------------------------
	// Meteor API
	// -------------------------------------------------------------------------

	public void onMeteorFalling(String coordsLine, MeteorType type) {
		Matcher matcher = COORDS_PATTERN.matcher(coordsLine);
		if (matcher.find()) {
			try {
				int x = Integer.parseInt(matcher.group(1));
				int y = Integer.parseInt(matcher.group(2));
				int z = Integer.parseInt(matcher.group(3));
				for (MeteorInfo m : activeMeteors) {
					if (m.x == x && m.y == y && m.z == z) {
						return;
					}
				}
				long now = System.currentTimeMillis();
				long duration = (type == MeteorType.NATURAL) ? NATURAL_METEOR_DURATION_MS : SUMMONED_METEOR_DURATION_MS;
				activeMeteors.add(new MeteorInfo(x, y, z, now, now + duration, createMeteorIcon(), type));
				BetterPrisons.LOGGER.info("Meteor detected at: {}, {}, {} (type: {})", x, y, z, type);
				String name = (type == MeteorType.NATURAL) ? "Natural Meteor" : "Summoned Meteor";
				int color = (type == MeteorType.NATURAL)
						? cfg().eventsNaturalHeadingColor : cfg().eventsSummonedHeadingColor;
				String eventKey = (type == MeteorType.NATURAL) ? "METEOR_NATURAL" : "METEOR_SUMMONED";
				BetterPrisonsClient.waypointManager.addEventWaypoint(x, y, z, color, name, eventKey);
			} catch (NumberFormatException e) {
				BetterPrisons.LOGGER.warn("Failed to parse meteor coordinates: {}", coordsLine);
			}
		}
	}

	public void onMeteorCrashed(String coordsLine) {
		Matcher matcher = COORDS_PATTERN.matcher(coordsLine);
		if (matcher.find()) {
			try {
				int x = Integer.parseInt(matcher.group(1));
				int y = Integer.parseInt(matcher.group(2));
				int z = Integer.parseInt(matcher.group(3));
				for (MeteorInfo m : activeMeteors) {
					if (m.x == x && m.y == y && m.z == z && m.crashTime == null) {
						m.crashTime = System.currentTimeMillis();
						BetterPrisons.LOGGER.info("Meteor marked as crashed at: {}, {}, {}", x, y, z);
						return;
					}
				}
				MeteorType type = coordsLine.contains("Summoned by") ? MeteorType.SUMMONED : MeteorType.NATURAL;
				long now = System.currentTimeMillis();
				MeteorInfo crashed = new MeteorInfo(x, y, z, now, now, createMeteorIcon(), type);
				crashed.crashTime = now;
				activeMeteors.add(crashed);
				BetterPrisons.LOGGER.info("Meteor crash registered (no prior falling) at: {}, {}, {} (type: {})", x, y, z, type);
				String name = (type == MeteorType.NATURAL) ? "Natural Meteor" : "Summoned Meteor";
				int color = (type == MeteorType.NATURAL)
						? cfg().eventsNaturalHeadingColor : cfg().eventsSummonedHeadingColor;
				String eventKey = (type == MeteorType.NATURAL) ? "METEOR_NATURAL" : "METEOR_SUMMONED";
				BetterPrisonsClient.waypointManager.addEventWaypoint(x, y, z, color, name, eventKey);
			} catch (NumberFormatException e) {
				BetterPrisons.LOGGER.warn("Failed to parse meteor crash coordinates: {}", coordsLine);
			}
		}
	}

	private ItemStack createMeteorIcon() {
		ItemStack stack = new ItemStack(itemOrDefault(cfg().eventsIconItemId, "nether_quartz_ore"));
		stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
		return stack;
	}

	// -------------------------------------------------------------------------
	// Merchant API
	// -------------------------------------------------------------------------

	public void onMerchantSpawned(String tierName, int x, int y, int z) {
		MerchantType type = MerchantType.fromString(tierName);
		for (MerchantInfo m : activeMerchants) {
			if (m.x == x && m.y == y && m.z == z) {
				return;
			}
		}
		ItemStack icon = new ItemStack(itemOrDefault("minecraft:" + type.getDefaultIconId(), "coal"));
		activeMerchants.add(new MerchantInfo(x, y, z, System.currentTimeMillis(), icon, type));
		BetterPrisons.LOGGER.info("Merchant detected: {} at {}, {}, {}", type, x, y, z);
		String name = type.getDisplayName();
		int color = type.getHeadingColor(cfg());
		String eventKey = "MERCHANT_" + type.name();
		BetterPrisonsClient.waypointManager.addEventWaypoint(x, y, z, color, name, eventKey);
	}

	public void onMerchantSlain(String tierName, int x, int y, int z) {
		for (MerchantInfo m : activeMerchants) {
			if (m.x == x && m.z == z && m.slainTime == null) {
				m.slainTime = System.currentTimeMillis();
				BetterPrisons.LOGGER.info("Merchant marked as slain: {} at {}, {}, {} (spawn Y was {})",
						tierName, x, y, z, m.y);
				return;
			}
		}
		BetterPrisons.LOGGER.warn("Merchant slain but no matching entry found: {} at {}, {}, {}", tierName, x, y, z);
	}

	// -------------------------------------------------------------------------
	// Bandit Rush API
	// -------------------------------------------------------------------------

	public void onBanditRushSpawned(String tier, int x, int y, int z) {
		if (!cfg().banditRushEnabled) {
			return;
		}
		BadlandsRegion rushRegion = BadlandsRegion.fromCoords(x, z);
		BadlandsRegion playerRegion = BadlandsRegion.getPlayerRegion();
		if (rushRegion == null || playerRegion == null || rushRegion != playerRegion) {
			BetterPrisons.LOGGER.info("Bandit rush at {}, {}, {} ignored (different badlands region)", x, y, z);
			return;
		}
		for (BanditRushInfo b : activeBanditRushes) {
			if (b.x == x && b.y == y && b.z == z) {
				return;
			}
		}
		ItemStack icon = new ItemStack(itemOrDefault(cfg().banditRushIconItemId, "iron_sword"));
		activeBanditRushes.add(new BanditRushInfo(x, y, z, System.currentTimeMillis(), icon, tier));
		BetterPrisons.LOGGER.info("Bandit rush detected: {} at {}, {}, {}", tier, x, y, z);

		int color = cfg().banditRushHeadingColor;
		String name = tier + " Bandit Rush";
		BetterPrisonsClient.waypointManager.addEventWaypoint(x, y, z, color, name, "BANDIT_RUSH_" + tier.toUpperCase());

		if (cfg().banditRushSoundEnabled) {
			Sounds.play(cfg().banditRushSound, cfg().banditRushSoundVolume / 100.0f, 1.0f);
		}
	}

	public List<BanditRushInfo> getActiveBanditRushes() {
		return activeBanditRushes;
	}

	public List<BanditRushInfo> getVisibleBanditRushes() {
		if (!cfg().banditRushEnabled) {
			return new ArrayList<>();
		}
		return new ArrayList<>(activeBanditRushes);
	}

	public void onBanditRushWon(String tier, int x, int z) {
		BadlandsRegion wonRegion = BadlandsRegion.fromCoords(x, z);
		activeBanditRushes.removeIf(b -> {
			if (b.tier.equalsIgnoreCase(tier)) {
				BadlandsRegion rushRegion = BadlandsRegion.fromCoords(b.x, b.z);
				if (wonRegion != null && wonRegion == rushRegion) {
					BetterPrisonsClient.waypointManager.removeEventWaypoint(b.x, b.y, b.z);
					BetterPrisons.LOGGER.info("Bandit rush {} won in {} region — removed from {}, {}, {}",
							tier, wonRegion, b.x, b.y, b.z);
					return true;
				}
			}
			return false;
		});
	}

	public void clearBanditRushes() {
		for (BanditRushInfo b : activeBanditRushes) {
			BetterPrisonsClient.waypointManager.removeEventWaypoint(b.x, b.y, b.z);
		}
		activeBanditRushes.clear();
	}

	// -------------------------------------------------------------------------
	// Meteorite Shower API
	// -------------------------------------------------------------------------

	public void onMeteoriteShower(String coordsLine, boolean crashed) {
		if (!cfg().meteoriteShowerEnabled) {
			return;
		}
		Matcher matcher = SHOWER_COORDS_PATTERN.matcher(coordsLine);
		if (!matcher.find()) {
			return;
		}
		try {
			int x = Integer.parseInt(matcher.group(1));
			int y = Integer.parseInt(matcher.group(2));
			int z = Integer.parseInt(matcher.group(3));
			String zone = matcher.group(4);
			long now = System.currentTimeMillis();

			for (MeteoriteShowerInfo s : activeMeteoriteShowers) {
				if (s.x == x && s.y == y && s.z == z) {
					if (crashed && s.crashTime == null) {
						s.crashTime = now;
						BetterPrisons.LOGGER.info("Meteorite shower crashed at {}, {}, {}", x, y, z);
					}
					return;
				}
			}
			MeteoriteShowerInfo info = new MeteoriteShowerInfo(
					x, y, z, now, now + METEORITE_SHOWER_WARNING_MS, createMeteoriteShowerIcon(), zone);
			if (crashed) {
				info.crashTime = now;
			}
			activeMeteoriteShowers.add(info);
			BetterPrisons.LOGGER.info("Meteorite shower detected at {}, {}, {} (zone: {}, crashed: {})", x, y, z, zone, crashed);

			int color = cfg().meteoriteShowerHeadingColor;
			BetterPrisonsClient.waypointManager.addEventWaypoint(x, y, z, color, "Meteorite Shower", "METEORITE_SHOWER");

			if (cfg().meteoriteShowerSoundEnabled) {
				Sounds.play(cfg().meteoriteShowerSound, cfg().meteoriteShowerSoundVolume / 100.0f, 1.0f);
			}
		} catch (NumberFormatException e) {
			BetterPrisons.LOGGER.warn("Failed to parse meteorite shower coordinates: {}", coordsLine);
		}
	}

	public List<MeteoriteShowerInfo> getVisibleMeteoriteShowers() {
		if (!cfg().meteoriteShowerEnabled) {
			return new ArrayList<>();
		}
		return new ArrayList<>(activeMeteoriteShowers);
	}

	public void clearMeteoriteShowers() {
		for (MeteoriteShowerInfo s : activeMeteoriteShowers) {
			BetterPrisonsClient.waypointManager.removeEventWaypoint(s.x, s.y, s.z);
		}
		activeMeteoriteShowers.clear();
	}

	private ItemStack createMeteoriteShowerIcon() {
		return new ItemStack(itemOrDefault(cfg().meteoriteShowerIconItemId, "magma_block"));
	}

	private String buildShowerHeading(MeteoriteShowerInfo s, long now) {
		String name = "Meteorite Shower";
		if (s.crashTime != null) {
			return name + " [Mineable]";
		}
		long remaining = s.landingTime - now;
		if (remaining <= 0) {
			return name + " (Imminent)";
		}
		long totalSecs = remaining / 1000;
		return String.format("%s (%d:%02d)", name, totalSecs / 60, totalSecs % 60);
	}

	private String buildShowerCoords(MeteoriteShowerInfo s, Vec3 playerPos) {
		return coordsWithDist(s.x, s.y, s.z, playerPos, cfg().meteoriteShowerShowDistance);
	}

	// -------------------------------------------------------------------------
	// Getters used by the beam / edge suppliers
	// -------------------------------------------------------------------------

	public List<MeteorInfo> getActiveMeteors() {
		return activeMeteors;
	}

	public List<MerchantInfo> getActiveMerchants() {
		return activeMerchants;
	}

	public List<MerchantInfo> getVisibleMerchantsForWaypoints() {
		PrisonsConfig config = cfg();
		if (!config.merchantsEnabled) {
			return new ArrayList<>();
		}
		List<MerchantInfo> result = new ArrayList<>();
		for (MerchantInfo m : activeMerchants) {
			if (m.slainTime == null && m.type.isEnabled(config)) {
				result.add(m);
			}
		}
		return result;
	}

	public void clearMeteors() {
		for (MeteorInfo m : activeMeteors) {
			BetterPrisonsClient.waypointManager.removeEventWaypoint(m.x, m.y, m.z);
		}
		activeMeteors.clear();
	}

	public void clearMerchants() {
		for (MerchantInfo m : activeMerchants) {
			BetterPrisonsClient.waypointManager.removeEventWaypoint(m.x, m.y, m.z);
		}
		activeMerchants.clear();
	}

	private List<MerchantInfo> getVisibleMerchants() {
		PrisonsConfig config = cfg();
		if (!config.merchantsEnabled) {
			return new ArrayList<>();
		}
		List<MerchantInfo> result = new ArrayList<>();
		for (MerchantInfo m : activeMerchants) {
			if (m.type.isEnabled(config)) {
				result.add(m);
			}
		}
		return result;
	}

	private List<MeteorInfo> getVisibleMeteors() {
		PrisonsConfig config = cfg();
		List<MeteorInfo> result = new ArrayList<>();
		for (MeteorInfo m : activeMeteors) {
			boolean enabled = m.type == MeteorType.NATURAL
					? config.naturalMeteorsEnabled : config.summonedMeteorsEnabled;
			if (enabled) {
				result.add(m);
			}
		}
		return result;
	}

	private String buildMeteorHeading(MeteorInfo m, long now) {
		String name = m.type == MeteorType.NATURAL ? "Natural Meteor" : "Summoned Meteor";
		if (m.crashTime != null) {
			return name + " [Crashed]";
		}
		long remaining = m.landingTime - now;
		if (remaining <= 0) {
			return name + " (Imminent)";
		}
		long totalSecs = remaining / 1000;
		return String.format("%s (%d:%02d)", name, totalSecs / 60, totalSecs % 60);
	}

	private String coordsWithDist(int x, int y, int z, Vec3 playerPos, boolean showDist) {
		String base = String.format("%dx, %dy, %dz", x, y, z);
		if (!showDist || playerPos == null) {
			return base;
		}
		int dist = (int) playerPos.distanceTo(new Vec3(x + 0.5, y, z + 0.5));
		return base + " (" + dist + "m)";
	}

	// -------------------------------------------------------------------------
	// Tick
	// -------------------------------------------------------------------------

	@Override
	public void tick() {
		long now = System.currentTimeMillis();
		PrisonsConfig config = cfg();
		long crashedDisplayDurationMs = config.eventsCrashedDisplayDuration * 1000L;
		long merchantTimeoutMs = config.merchantTimeoutMinutes * 60 * 1000L;
		long merchantSlainDisplayMs = config.merchantSlainDisplayDuration * 1000L;

		activeMeteors.removeIf(m -> {
			if (!BetterPrisonsClient.waypointManager.hasEventWaypoint(m.x, m.y, m.z)) {
				return true;
			}
			if (now - m.spawnTime > METEOR_TIMEOUT_MS) {
				BetterPrisonsClient.waypointManager.removeEventWaypoint(m.x, m.y, m.z);
				return true;
			}
			if (m.crashTime != null && now - m.crashTime > crashedDisplayDurationMs) {
				BetterPrisonsClient.waypointManager.removeEventWaypoint(m.x, m.y, m.z);
				return true;
			}
			if (m.crashTime == null && now - m.landingTime > IMMINENT_GRACE_MS) {
				BetterPrisonsClient.waypointManager.removeEventWaypoint(m.x, m.y, m.z);
				return true;
			}
			return false;
		});

		activeMerchants.removeIf(m -> {
			if (!BetterPrisonsClient.waypointManager.hasEventWaypoint(m.x, m.y, m.z)) {
				return true;
			}
			if (m.slainTime != null && now - m.slainTime > merchantSlainDisplayMs) {
				BetterPrisonsClient.waypointManager.removeEventWaypoint(m.x, m.y, m.z);
				return true;
			}
			if (m.slainTime == null && now - m.spawnTime > merchantTimeoutMs) {
				BetterPrisonsClient.waypointManager.removeEventWaypoint(m.x, m.y, m.z);
				return true;
			}
			return false;
		});

		long banditRushDurationMs = config.banditRushTimeoutSeconds * 1000L;
		activeBanditRushes.removeIf(b -> {
			if (!BetterPrisonsClient.waypointManager.hasEventWaypoint(b.x, b.y, b.z)) {
				return true;
			}
			if (now - b.spawnTime > banditRushDurationMs) {
				BetterPrisonsClient.waypointManager.removeEventWaypoint(b.x, b.y, b.z);
				return true;
			}
			return false;
		});

		long showerMineableMs = config.meteoriteShowerTimeoutSeconds * 1000L;
		activeMeteoriteShowers.removeIf(s -> {
			if (!BetterPrisonsClient.waypointManager.hasEventWaypoint(s.x, s.y, s.z)) {
				return true;
			}
			if (s.crashTime != null && now - s.crashTime > showerMineableMs) {
				BetterPrisonsClient.waypointManager.removeEventWaypoint(s.x, s.y, s.z);
				return true;
			}
			if (s.crashTime == null && now - s.landingTime > IMMINENT_GRACE_MS) {
				BetterPrisonsClient.waypointManager.removeEventWaypoint(s.x, s.y, s.z);
				return true;
			}
			if (now - s.spawnTime > METEORITE_SHOWER_TIMEOUT_MS) {
				BetterPrisonsClient.waypointManager.removeEventWaypoint(s.x, s.y, s.z);
				return true;
			}
			return false;
		});
	}

	// -------------------------------------------------------------------------
	// Scheduled events (Cosmic API)
	// -------------------------------------------------------------------------

	public void updateScheduledEvent(String id, String name, boolean enabled, boolean active, long displayUntil) {
		if (!SCHEDULED_EVENT_ORDER.contains(id)) {
			return;
		}
		ScheduledEventInfo info = scheduledEvents.computeIfAbsent(id, k -> new ScheduledEventInfo());
		info.id = id;
		info.name = name;
		info.enabled = enabled;
		info.active = active;
		info.displayUntil = displayUntil;
	}

	private List<ScheduledEventInfo> getVisibleScheduledEvents() {
		List<ScheduledEventInfo> out = new ArrayList<>();
		for (String id : SCHEDULED_EVENT_ORDER) {
			ScheduledEventInfo info = scheduledEvents.get(id);
			if (info != null) {
				out.add(info);
			}
		}
		return out;
	}

	private String buildScheduledHeading(ScheduledEventInfo e, long now) {
		if (!e.enabled) {
			return e.name + " [Disabled]";
		}
		if (e.active || e.displayUntil <= 0) {
			return e.name + " [Active]";
		}
		long remaining = e.displayUntil - now;
		if (remaining <= 0) {
			return e.name + " (Soon)";
		}
		long totalSecs = remaining / 1000;
		long hours = totalSecs / 3600;
		long mins = (totalSecs % 3600) / 60;
		long secs = totalSecs % 60;
		if (hours > 0) {
			return String.format("%s (%d:%02d:%02d)", e.name, hours, mins, secs);
		}
		return String.format("%s (%d:%02d)", e.name, mins, secs);
	}

	// -------------------------------------------------------------------------
	// Render
	// -------------------------------------------------------------------------

	@Override
	public void render(GuiGraphics ctx, Minecraft client) {
		PrisonsConfig config = cfg();
		this.scale = config.eventsHudScale / 100.0f;

		boolean showTitle = config.showEventsHudTitle;
		Vec3 playerPos = client.player != null
				? new Vec3(client.player.getX(), client.player.getY(), client.player.getZ()) : null;
		List<MeteorInfo> visibleMeteors = getVisibleMeteors();
		List<MerchantInfo> visibleMerchants = getVisibleMerchants();
		List<BanditRushInfo> visibleBanditRushes = getVisibleBanditRushes();
		List<MeteoriteShowerInfo> visibleShowers = getVisibleMeteoriteShowers();
		List<ScheduledEventInfo> visibleScheduled = getVisibleScheduledEvents();
		boolean hasContent = !visibleMeteors.isEmpty() || !visibleMerchants.isEmpty()
				|| !visibleBanditRushes.isEmpty() || !visibleShowers.isEmpty()
				|| !visibleScheduled.isEmpty();

		if (!enabled || (!showTitle && !hasContent)) {
			return;
		}

		int titleHeight = 0, titleWidth = 0;
		if (showTitle) {
			Component titleText = Component.literal("Events HUD").setStyle(Style.EMPTY.withUnderlined(true).withBold(true));
			titleWidth = (int) (client.font.width(titleText) * scale);
			titleHeight = scaled(12);
		}

		long renderNow = System.currentTimeMillis();

		int maxTextWidth = titleWidth;
		for (MeteorInfo m : visibleMeteors) {
			maxTextWidth = Math.max(maxTextWidth, (int) (client.font.width(buildMeteorHeading(m, renderNow)) * scale));
			String coords = coordsWithDist(m.x, m.y, m.z, playerPos, config.meteorShowDistance);
			maxTextWidth = Math.max(maxTextWidth, scaled(20) + (int) (client.font.width(coords) * scale));
		}
		for (MerchantInfo m : visibleMerchants) {
			maxTextWidth = Math.max(maxTextWidth, (int) (client.font.width(m.type.getDisplayName()) * scale));
			String coords = coordsWithDist(m.x, m.y, m.z, playerPos, config.merchantShowDistance);
			maxTextWidth = Math.max(maxTextWidth, scaled(20) + (int) (client.font.width(coords) * scale));
		}
		for (BanditRushInfo b : visibleBanditRushes) {
			maxTextWidth = Math.max(maxTextWidth, (int) (client.font.width(b.getDisplayName()) * scale));
			String coords = coordsWithDist(b.x, b.y, b.z, playerPos, config.banditRushShowDistance);
			maxTextWidth = Math.max(maxTextWidth, scaled(20) + (int) (client.font.width(coords) * scale));
		}
		for (MeteoriteShowerInfo s : visibleShowers) {
			maxTextWidth = Math.max(maxTextWidth, (int) (client.font.width(buildShowerHeading(s, renderNow)) * scale));
			String coords = buildShowerCoords(s, playerPos);
			maxTextWidth = Math.max(maxTextWidth, scaled(20) + (int) (client.font.width(coords) * scale));
		}
		for (ScheduledEventInfo e : visibleScheduled) {
			maxTextWidth = Math.max(maxTextWidth, (int) (client.font.width(buildScheduledHeading(e, renderNow)) * scale));
		}

		int bgWidth = maxTextWidth;
		int contentHeight = (visibleMeteors.size() + visibleMerchants.size()
				+ visibleBanditRushes.size() + visibleShowers.size()) * scaled(32)
				+ visibleScheduled.size() * scaled(12);
		int bgHeight = titleHeight + contentHeight;

		int bgColor = (config.eventsBgOpacity << 24) | (config.eventsBgColor & 0xFFFFFF);
		int borderColor = (config.eventsBorderOpacity << 24) | (config.eventsBorderColor & 0xFFFFFF);
		int thickness = scaled(config.eventsBorderThickness);
		int padding = scale < 1 ? scaled(4) : 4;

		ctx.fill(x - padding, y - padding, x + bgWidth + padding, y + bgHeight + padding, bgColor);
		ctx.fill(x - padding, y - padding - thickness, x + bgWidth + padding, y - padding, borderColor);
		ctx.fill(x - padding, y + bgHeight + padding, x + bgWidth + padding, y + bgHeight + padding + thickness, borderColor);
		ctx.fill(x - padding - thickness, y - padding - thickness, x - padding, y + bgHeight + padding + thickness, borderColor);
		ctx.fill(x + bgWidth + padding, y - padding - thickness, x + bgWidth + padding + thickness, y + bgHeight + padding + thickness, borderColor);

		Matrix3x2fStack matrices = ctx.pose();
		int yOffset = 0;

		if (showTitle) {
			Component titleText = Component.literal("Events HUD").setStyle(Style.EMPTY.withUnderlined(true).withBold(true));
			int titleColor = 0xFF000000 | config.eventsHudTitleColor;
			matrices.pushMatrix();
			matrices.scale(scale, scale);
			matrices.translate(x / scale, y / scale);
			ctx.drawString(client.font, titleText, 0, 0, titleColor, true);
			matrices.popMatrix();
			yOffset += titleHeight;
		}

		int iconSpacing = scaled(20);

		// --- Merchants ---
		for (MerchantInfo m : visibleMerchants) {
			boolean slain = m.slainTime != null;
			int headingColor = 0xFF000000 | m.type.getHeadingColor(config);
			if (slain) {
				headingColor = (headingColor & 0x00FFFFFF) | 0x80000000;
			}
			String displayName = slain ? m.type.getDisplayName() + " §c[Slain]" : m.type.getDisplayName();

			matrices.pushMatrix();
			matrices.scale(scale, scale);
			matrices.translate(x / scale, (y + yOffset) / scale);
			ctx.drawString(client.font, Component.literal(displayName).setStyle(Style.EMPTY.withItalic(true)), 0, 0, headingColor, true);
			matrices.popMatrix();
			yOffset += scaled(12);

			if (m.iconStack != null) {
				matrices.pushMatrix();
				matrices.scale(scale, scale);
				matrices.translate(x / scale, (y + yOffset) / scale);
				ctx.renderItem(m.iconStack, 0, 0);
				matrices.popMatrix();
			}

			String coords = coordsWithDist(m.x, m.y, m.z, playerPos, config.merchantShowDistance);
			matrices.pushMatrix();
			matrices.scale(scale, scale);
			matrices.translate((x + iconSpacing) / scale, (y + yOffset + scaled(4)) / scale);
			ctx.drawString(client.font, Component.literal(coords), 0, 0, headingColor, true);
			matrices.popMatrix();
			yOffset += scaled(20);
		}

		// --- Bandit Rushes ---
		int rushCoordColor = 0xFF000000 | config.banditRushTextColor;
		for (BanditRushInfo b : visibleBanditRushes) {
			int rushHeadingColor = 0xFF000000 | config.banditRushHeadingColor;

			matrices.pushMatrix();
			matrices.scale(scale, scale);
			matrices.translate(x / scale, (y + yOffset) / scale);
			ctx.drawString(client.font, Component.literal(b.getDisplayName()).setStyle(Style.EMPTY.withItalic(true)), 0, 0, rushHeadingColor, true);
			matrices.popMatrix();
			yOffset += scaled(12);

			if (b.iconStack != null) {
				matrices.pushMatrix();
				matrices.scale(scale, scale);
				matrices.translate(x / scale, (y + yOffset) / scale);
				ctx.renderItem(b.iconStack, 0, 0);
				matrices.popMatrix();
			}

			String rushCoords = coordsWithDist(b.x, b.y, b.z, playerPos, config.banditRushShowDistance);
			matrices.pushMatrix();
			matrices.scale(scale, scale);
			matrices.translate((x + iconSpacing) / scale, (y + yOffset + scaled(4)) / scale);
			ctx.drawString(client.font, Component.literal(rushCoords), 0, 0, rushCoordColor, true);
			matrices.popMatrix();
			yOffset += scaled(20);
		}

		// --- Meteors ---
		int meteorCoordColor = 0xFF000000 | config.eventsTextColor;
		for (MeteorInfo m : visibleMeteors) {
			String heading = buildMeteorHeading(m, renderNow);
			int headingColor = 0xFF000000 | (m.type == MeteorType.NATURAL
					? config.eventsNaturalHeadingColor : config.eventsSummonedHeadingColor);

			matrices.pushMatrix();
			matrices.scale(scale, scale);
			matrices.translate(x / scale, (y + yOffset) / scale);
			ctx.drawString(client.font, Component.literal(heading).setStyle(Style.EMPTY.withItalic(true)), 0, 0, headingColor, true);
			matrices.popMatrix();
			yOffset += scaled(12);

			if (m.iconStack != null) {
				matrices.pushMatrix();
				matrices.scale(scale, scale);
				matrices.translate(x / scale, (y + yOffset) / scale);
				ctx.renderItem(m.iconStack, 0, 0);
				matrices.popMatrix();
			}

			String coords = coordsWithDist(m.x, m.y, m.z, playerPos, config.meteorShowDistance);
			matrices.pushMatrix();
			matrices.scale(scale, scale);
			matrices.translate((x + iconSpacing) / scale, (y + yOffset + scaled(4)) / scale);
			ctx.drawString(client.font, Component.literal(coords), 0, 0, meteorCoordColor, true);
			matrices.popMatrix();
			yOffset += scaled(20);
		}

		// --- Meteorite Showers ---
		int showerCoordColor = 0xFF000000 | config.meteoriteShowerTextColor;
		for (MeteoriteShowerInfo s : visibleShowers) {
			int showerHeadingColor = 0xFF000000 | config.meteoriteShowerHeadingColor;
			String displayName = buildShowerHeading(s, renderNow);

			matrices.pushMatrix();
			matrices.scale(scale, scale);
			matrices.translate(x / scale, (y + yOffset) / scale);
			ctx.drawString(client.font, Component.literal(displayName).setStyle(Style.EMPTY.withItalic(true)), 0, 0, showerHeadingColor, true);
			matrices.popMatrix();
			yOffset += scaled(12);

			if (s.iconStack != null) {
				matrices.pushMatrix();
				matrices.scale(scale, scale);
				matrices.translate(x / scale, (y + yOffset) / scale);
				ctx.renderItem(s.iconStack, 0, 0);
				matrices.popMatrix();
			}

			String showerCoords = buildShowerCoords(s, playerPos);
			matrices.pushMatrix();
			matrices.scale(scale, scale);
			matrices.translate((x + iconSpacing) / scale, (y + yOffset + scaled(4)) / scale);
			ctx.drawString(client.font, Component.literal(showerCoords), 0, 0, showerCoordColor, true);
			matrices.popMatrix();
			yOffset += scaled(20);
		}

		// --- Scheduled events (Cosmic API) — single-line countdowns, no coords ---
		int scheduledColor = 0xFF000000 | config.eventsTextColor;
		for (ScheduledEventInfo e : visibleScheduled) {
			String heading = buildScheduledHeading(e, renderNow);
			matrices.pushMatrix();
			matrices.scale(scale, scale);
			matrices.translate(x / scale, (y + yOffset) / scale);
			ctx.drawString(client.font, Component.literal(heading).setStyle(Style.EMPTY.withItalic(true)), 0, 0, scheduledColor, true);
			matrices.popMatrix();
			yOffset += scaled(12);
		}
	}

	// -------------------------------------------------------------------------
	// Size helpers
	// -------------------------------------------------------------------------

	@Override
	public int getWidth() {
		Minecraft client = Minecraft.getInstance();
		if (client == null || client.font == null) {
			return scaled(130);
		}
		PrisonsConfig config = cfg();
		List<MerchantInfo> visibleMerchants = getVisibleMerchants();

		int maxTextWidth = 0;
		if (config.showEventsHudTitle) {
			Component titleText = Component.literal("Events HUD").setStyle(Style.EMPTY.withUnderlined(true).withBold(true));
			maxTextWidth = (int) (client.font.width(titleText) * scale);
		}
		Vec3 playerPos = client.player != null
				? new Vec3(client.player.getX(), client.player.getY(), client.player.getZ()) : null;
		long widthNow = System.currentTimeMillis();
		for (MeteorInfo m : getVisibleMeteors()) {
			maxTextWidth = Math.max(maxTextWidth, (int) (client.font.width(buildMeteorHeading(m, widthNow)) * scale));
			String coords = coordsWithDist(m.x, m.y, m.z, playerPos, config.meteorShowDistance);
			maxTextWidth = Math.max(maxTextWidth, scaled(20) + (int) (client.font.width(coords) * scale));
		}
		for (MerchantInfo m : visibleMerchants) {
			maxTextWidth = Math.max(maxTextWidth, (int) (client.font.width(m.type.getDisplayName()) * scale));
			String coords = coordsWithDist(m.x, m.y, m.z, playerPos, config.merchantShowDistance);
			maxTextWidth = Math.max(maxTextWidth, scaled(20) + (int) (client.font.width(coords) * scale));
		}
		for (BanditRushInfo b : getVisibleBanditRushes()) {
			maxTextWidth = Math.max(maxTextWidth, (int) (client.font.width(b.getDisplayName()) * scale));
			String coords = coordsWithDist(b.x, b.y, b.z, playerPos, config.banditRushShowDistance);
			maxTextWidth = Math.max(maxTextWidth, scaled(20) + (int) (client.font.width(coords) * scale));
		}
		for (MeteoriteShowerInfo s : getVisibleMeteoriteShowers()) {
			maxTextWidth = Math.max(maxTextWidth, (int) (client.font.width(buildShowerHeading(s, widthNow)) * scale));
			String coords = buildShowerCoords(s, playerPos);
			maxTextWidth = Math.max(maxTextWidth, scaled(20) + (int) (client.font.width(coords) * scale));
		}
		for (ScheduledEventInfo e : getVisibleScheduledEvents()) {
			maxTextWidth = Math.max(maxTextWidth, (int) (client.font.width(buildScheduledHeading(e, widthNow)) * scale));
		}

		int padding = scale < 1 ? scaled(4) : 4;
		return maxTextWidth + (padding * 2);
	}

	@Override
	public int getHeight() {
		PrisonsConfig config = cfg();
		int titleHeight = config.showEventsHudTitle ? scaled(10) : 0;
		int visibleMerchantCount = getVisibleMerchants().size();
		int visibleBanditRushCount = getVisibleBanditRushes().size();
		int visibleShowerCount = getVisibleMeteoriteShowers().size();
		int visibleScheduledCount = getVisibleScheduledEvents().size();
		return titleHeight + ((getVisibleMeteors().size() + visibleMerchantCount
				+ visibleBanditRushCount + visibleShowerCount) * scaled(32))
				+ (visibleScheduledCount * scaled(12));
	}

	// -------------------------------------------------------------------------
	// Helpers
	// -------------------------------------------------------------------------

	/** Resolves an item id (namespaced or bare) to an {@link Item}, falling back to {@code fallbackPath}. */
	private static Item itemOrDefault(String itemId, String fallbackPath) {
		try {
			String id = itemId.contains(":") ? itemId : "minecraft:" + itemId;
			Identifier identifier = Identifier.tryParse(id);
			if (identifier != null) {
				Item item = BuiltInRegistries.ITEM.getValue(identifier);
				// DefaultedRegistry returns AIR for unknown ids — fall back so the icon stays visible.
				if (item != Items.AIR) {
					return item;
				}
			}
		} catch (Exception e) {
			BetterPrisons.LOGGER.warn("Failed to resolve item icon '{}': {}", itemId, e.getMessage());
		}
		return BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("minecraft", fallbackPath));
	}

	// -------------------------------------------------------------------------
	// Data classes
	// -------------------------------------------------------------------------

	public static class ScheduledEventInfo {
		public String id;
		public String name;
		public boolean enabled;
		public boolean active;
		public long displayUntil;
	}

	public static class MeteorInfo {
		public int x, y, z;
		public long spawnTime;
		public long landingTime;
		public Long crashTime;
		public ItemStack iconStack;
		public MeteorType type;

		public MeteorInfo(int x, int y, int z, long spawnTime, long landingTime, ItemStack iconStack, MeteorType type) {
			this.x = x;
			this.y = y;
			this.z = z;
			this.spawnTime = spawnTime;
			this.landingTime = landingTime;
			this.crashTime = null;
			this.iconStack = iconStack;
			this.type = type;
		}
	}

	public static class MerchantInfo {
		public int x, y, z;
		public long spawnTime;
		public Long slainTime;
		public ItemStack iconStack;
		public MerchantType type;

		public MerchantInfo(int x, int y, int z, long spawnTime, ItemStack iconStack, MerchantType type) {
			this.x = x;
			this.y = y;
			this.z = z;
			this.spawnTime = spawnTime;
			this.slainTime = null;
			this.iconStack = iconStack;
			this.type = type;
		}
	}

	public static class BanditRushInfo {
		public int x, y, z;
		public long spawnTime;
		public ItemStack iconStack;
		public String tier;

		public BanditRushInfo(int x, int y, int z, long spawnTime, ItemStack iconStack, String tier) {
			this.x = x;
			this.y = y;
			this.z = z;
			this.spawnTime = spawnTime;
			this.iconStack = iconStack;
			this.tier = tier;
		}

		public String getDisplayName() {
			String name = tier.charAt(0) + tier.substring(1).toLowerCase();
			return name + " Bandit Rush";
		}
	}

	public static class MeteoriteShowerInfo {
		public int x, y, z;
		public long spawnTime;
		public long landingTime;
		public Long crashTime;
		public ItemStack iconStack;
		public String zone;

		public MeteoriteShowerInfo(int x, int y, int z, long spawnTime, long landingTime, ItemStack iconStack, String zone) {
			this.x = x;
			this.y = y;
			this.z = z;
			this.spawnTime = spawnTime;
			this.landingTime = landingTime;
			this.crashTime = null;
			this.iconStack = iconStack;
			this.zone = zone;
		}
	}
}
