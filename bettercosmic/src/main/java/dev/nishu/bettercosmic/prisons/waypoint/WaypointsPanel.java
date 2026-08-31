package dev.nishu.bettercosmic.prisons.waypoint;

import dev.nishu.bettercosmic.prisons.client.BetterPrisonsClient;
import dev.nishu.bettercosmic.prisons.config.PrisonsConfig;
import dev.nishu.bettercosmic.prisons.ui.PrisonOptions;
import dev.nishu.bettercosmic.shared.ui.model.ConfigPanel;
import dev.nishu.bettercosmic.shared.ui.model.Option;
import dev.nishu.bettercosmic.shared.ui.model.OptionGroup;
import dev.nishu.bettercosmic.shared.ui.model.Options;
import dev.nishu.bettercosmic.shared.ui.model.PanelIcon;

import java.util.List;

/**
 * Config panel for waypoints and beacon beams: per-type on-screen / edge-indicator toggles, beam
 * appearance, and custom-waypoint defaults. Bound to {@link PrisonsConfig} via the shared
 * {@code Options} lambdas. (Managing individual custom waypoints uses the dedicated Waypoints screen,
 * ported separately.)
 */
public final class WaypointsPanel {

	private WaypointsPanel() {}

	public static ConfigPanel create() {
		PrisonsConfig d = new PrisonsConfig();
		PrisonsConfig c = BetterPrisonsClient.config;

		OptionGroup general = new OptionGroup("General", List.<Option>of(
				Options.toggle("Waypoints", d.waypointsEnabled,
						() -> c.waypointsEnabled, v -> { c.waypointsEnabled = v; c.save(); })
						.tooltip("Master toggle for on-screen waypoint markers and edge indicators.")));

		OptionGroup types = new OptionGroup("Types", List.<Option>of(
				Options.toggle("Meteors", d.waypointMeteorsEnabled,
						() -> c.waypointMeteorsEnabled, v -> { c.waypointMeteorsEnabled = v; c.save(); }),
				Options.toggle("Meteors: edge arrow", d.waypointMeteorsEdgeEnabled,
						() -> c.waypointMeteorsEdgeEnabled, v -> { c.waypointMeteorsEdgeEnabled = v; c.save(); }),
				Options.toggle("Merchants", d.waypointMerchantsEnabled,
						() -> c.waypointMerchantsEnabled, v -> { c.waypointMerchantsEnabled = v; c.save(); }),
				Options.toggle("Merchants: edge arrow", d.waypointMerchantsEdgeEnabled,
						() -> c.waypointMerchantsEdgeEnabled, v -> { c.waypointMerchantsEdgeEnabled = v; c.save(); }),
				Options.toggle("Meteorite showers", d.waypointMeteoriteShowerEnabled,
						() -> c.waypointMeteoriteShowerEnabled, v -> { c.waypointMeteoriteShowerEnabled = v; c.save(); }),
				Options.toggle("Meteorite showers: edge arrow", d.waypointMeteoriteShowerEdgeEnabled,
						() -> c.waypointMeteoriteShowerEdgeEnabled, v -> { c.waypointMeteoriteShowerEdgeEnabled = v; c.save(); }),
				Options.toggle("Bandit rushes", d.waypointBanditRushEnabled,
						() -> c.waypointBanditRushEnabled, v -> { c.waypointBanditRushEnabled = v; c.save(); }),
				Options.toggle("Bandit rushes: edge arrow", d.waypointBanditRushEdgeEnabled,
						() -> c.waypointBanditRushEdgeEnabled, v -> { c.waypointBanditRushEdgeEnabled = v; c.save(); }),
				Options.toggle("Custom waypoints", d.waypointCustomEnabled,
						() -> c.waypointCustomEnabled, v -> { c.waypointCustomEnabled = v; c.save(); }),
				Options.toggle("Custom: edge arrow", d.waypointCustomEdgeEnabled,
						() -> c.waypointCustomEdgeEnabled, v -> { c.waypointCustomEdgeEnabled = v; c.save(); })));

		OptionGroup beams = new OptionGroup("Beacon Beams", List.<Option>of(
				Options.toggle("Beacon beams", d.beaconBeamsEnabled,
						() -> c.beaconBeamsEnabled, v -> { c.beaconBeamsEnabled = v; c.save(); }),
				Options.toggle("Through walls", d.beaconBeamThroughWalls,
						() -> c.beaconBeamThroughWalls, v -> { c.beaconBeamThroughWalls = v; c.save(); })
						.tooltip("Render beams visible through terrain (capped to the fog-free zone)."),
				Options.intSlider("Meteor beam opacity", d.meteorBeamOpacity, 0, 255, 5,
						() -> c.meteorBeamOpacity, v -> { c.meteorBeamOpacity = v; c.save(); }),
				Options.intSlider("Merchant beam opacity", d.merchantBeamOpacity, 0, 255, 5,
						() -> c.merchantBeamOpacity, v -> { c.merchantBeamOpacity = v; c.save(); }),
				Options.intSlider("Bandit rush beam opacity", d.banditRushBeamOpacity, 0, 255, 5,
						() -> c.banditRushBeamOpacity, v -> { c.banditRushBeamOpacity = v; c.save(); }),
				Options.intSlider("Meteorite shower beam opacity", d.meteoriteShowerBeamOpacity, 0, 255, 5,
						() -> c.meteoriteShowerBeamOpacity, v -> { c.meteoriteShowerBeamOpacity = v; c.save(); })));

		OptionGroup custom = new OptionGroup("Custom Waypoint Defaults", List.<Option>of(
				Options.intSlider("Default beam opacity", d.customWaypointDefaultOpacity, 0, 255, 5,
						() -> c.customWaypointDefaultOpacity, v -> { c.customWaypointDefaultOpacity = v; c.save(); }),
				Options.slider("On-screen icon scale", d.customWaypointOnScreenScale, 0.1, 3.0, 0.1,
						() -> (double) c.customWaypointOnScreenScale, v -> { c.customWaypointOnScreenScale = v.floatValue(); c.save(); }),
				Options.slider("Off-screen icon scale", d.customWaypointOffScreenScale, 0.1, 3.0, 0.1,
						() -> (double) c.customWaypointOffScreenScale, v -> { c.customWaypointOffScreenScale = v.floatValue(); c.save(); })));

		return ConfigPanel.of("prisons-waypoints", "Waypoints & beams",
				"Screen markers & beacon beams", PanelIcon.BEACON,
				List.of(general, types, beams, custom));
	}
}
