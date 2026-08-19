package dev.nishu.bettercosmic.prisons.waypoint;

import dev.nishu.bettercosmic.prisons.client.BetterPrisonsClient;
import dev.nishu.bettercosmic.prisons.config.PrisonsConfig;
import dev.nishu.bettercosmic.prisons.hud.EventsHud;
import dev.nishu.bettercosmic.shared.render.BeaconBeamRenderer;
import dev.nishu.bettercosmic.shared.render.WaypointRenderer;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapts BetterPrisons event/waypoint data into the shared renderers' generic shapes: vertical
 * {@link BeaconBeamRenderer.Beam}s and screen-edge {@link WaypointRenderer.EdgeTarget}s. This is the
 * content half of the mechanism/policy split — the shared renderers know nothing about meteors or
 * merchants; this class maps them (and the per-type config gating and world checks BetterPrisons'
 * {@code BeaconBeamRenderer}/{@code WaypointRenderer} did inline) onto the registries.
 */
public final class WaypointSuppliers {

	/** Beams rise from world Y=0 for this many blocks, matching BetterPrisons (visible from any altitude). */
	private static final float BEAM_HEIGHT = 250f;

	private WaypointSuppliers() {}

	private static PrisonsConfig cfg() {
		return BetterPrisonsClient.config;
	}

	private static String worldKey() {
		Minecraft client = Minecraft.getInstance();
		return client.level != null ? client.level.dimension().identifier().toString() : "";
	}

	// -------------------------------------------------------------------------
	// Beacon beams
	// -------------------------------------------------------------------------

	public static List<BeaconBeamRenderer.Beam> beams() {
		List<BeaconBeamRenderer.Beam> beams = new ArrayList<>();
		PrisonsConfig c = cfg();
		if (c == null || !c.beaconBeamsEnabled || !c.waypointsEnabled) {
			return beams;
		}
		EventsHud events = BetterPrisonsClient.eventsHud;
		boolean throughWalls = c.beaconBeamThroughWalls;
		String world = worldKey();
		boolean inOverworld = "minecraft:overworld".equals(world);
		boolean inBadlands = "minecraft:badlands".equals(world);

		if (inOverworld && c.waypointMeteorsEnabled) {
			for (EventsHud.MeteorInfo m : events.getActiveMeteors()) {
				int color = m.type == EventsHud.MeteorType.NATURAL
						? c.eventsNaturalHeadingColor : c.eventsSummonedHeadingColor;
				beams.add(beam(m.x, m.z, color, c.meteorBeamOpacity, throughWalls));
			}
		}
		if (inOverworld && c.waypointMerchantsEnabled) {
			for (EventsHud.MerchantInfo m : events.getVisibleMerchantsForWaypoints()) {
				beams.add(beam(m.x, m.z, m.type.getHeadingColor(c), c.merchantBeamOpacity, throughWalls));
			}
		}
		if (inOverworld && c.waypointMeteoriteShowerEnabled) {
			for (EventsHud.MeteoriteShowerInfo s : events.getVisibleMeteoriteShowers()) {
				beams.add(beam(s.x, s.z, c.meteoriteShowerHeadingColor, c.meteoriteShowerBeamOpacity, throughWalls));
			}
		}
		if (inBadlands && c.waypointBanditRushEnabled) {
			for (EventsHud.BanditRushInfo b : events.getVisibleBanditRushes()) {
				beams.add(beam(b.x, b.z, c.banditRushHeadingColor, c.banditRushBeamOpacity, throughWalls));
			}
		}
		if (c.waypointCustomEnabled) {
			for (CustomWaypoint wp : BetterPrisonsClient.waypointManager.getEnabled()) {
				beams.add(beam(wp.x, wp.z, wp.color, wp.opacity, throughWalls));
			}
		}
		return beams;
	}

	private static BeaconBeamRenderer.Beam beam(int x, int z, int rgb, int opacity, boolean throughWalls) {
		return new BeaconBeamRenderer.Beam(x + 0.5, 0, z + 0.5, BEAM_HEIGHT, rgb, opacity, throughWalls);
	}

	// -------------------------------------------------------------------------
	// Screen-edge markers
	// -------------------------------------------------------------------------

	public static List<WaypointRenderer.EdgeTarget> edgeTargets() {
		List<WaypointRenderer.EdgeTarget> targets = new ArrayList<>();
		PrisonsConfig c = cfg();
		if (c == null || !c.waypointsEnabled) {
			return targets;
		}
		EventsHud events = BetterPrisonsClient.eventsHud;
		String world = worldKey();
		boolean inOverworld = "minecraft:overworld".equals(world);
		boolean inBadlands = "minecraft:badlands".equals(world);

		if (inOverworld && c.waypointMeteorsEnabled) {
			for (EventsHud.MeteorInfo m : events.getActiveMeteors()) {
				int color = m.type == EventsHud.MeteorType.NATURAL
						? c.eventsNaturalHeadingColor : c.eventsSummonedHeadingColor;
				WaypointRenderer.EdgeTarget t =
						new WaypointRenderer.EdgeTarget(m.x + 0.5, m.y, m.z + 0.5, color, m.iconStack);
				t.edgeEnabled = c.waypointMeteorsEdgeEnabled;
				targets.add(t);
			}
		}
		if (inOverworld && c.waypointMerchantsEnabled) {
			for (EventsHud.MerchantInfo m : events.getVisibleMerchantsForWaypoints()) {
				WaypointRenderer.EdgeTarget t = new WaypointRenderer.EdgeTarget(
						m.x + 0.5, m.y, m.z + 0.5, m.type.getHeadingColor(c), m.iconStack);
				t.edgeEnabled = c.waypointMerchantsEdgeEnabled;
				targets.add(t);
			}
		}
		if (inOverworld && c.waypointMeteoriteShowerEnabled) {
			for (EventsHud.MeteoriteShowerInfo s : events.getVisibleMeteoriteShowers()) {
				WaypointRenderer.EdgeTarget t = new WaypointRenderer.EdgeTarget(
						s.x + 0.5, s.y, s.z + 0.5, c.meteoriteShowerHeadingColor, s.iconStack);
				t.edgeEnabled = c.waypointMeteoriteShowerEdgeEnabled;
				targets.add(t);
			}
		}
		if (inBadlands && c.waypointBanditRushEnabled) {
			for (EventsHud.BanditRushInfo b : events.getVisibleBanditRushes()) {
				WaypointRenderer.EdgeTarget t = new WaypointRenderer.EdgeTarget(
						b.x + 0.5, b.y, b.z + 0.5, c.banditRushHeadingColor, b.iconStack);
				t.edgeEnabled = c.waypointBanditRushEdgeEnabled;
				targets.add(t);
			}
		}
		if (c.waypointCustomEnabled) {
			Minecraft client = Minecraft.getInstance();
			for (CustomWaypoint wp : BetterPrisonsClient.waypointManager.getEnabled()) {
				WaypointRenderer.EdgeTarget t = new WaypointRenderer.EdgeTarget(
						wp.x + 0.5, wp.y, wp.z + 0.5, wp.color, null);
				int dist = client.player != null
						? (int) Math.sqrt(client.player.distanceToSqr(wp.x + 0.5, wp.y, wp.z + 0.5)) : 0;
				t.label = wp.name + " " + dist + "m";
				t.onScreenScale = Math.max(0.1f, wp.onScreenScale);
				t.offScreenScale = Math.max(0.1f, wp.offScreenScale);
				t.edgeEnabled = c.waypointCustomEdgeEnabled;
				targets.add(t);
			}
		}
		return targets;
	}
}
