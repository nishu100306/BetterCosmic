package dev.nishu.bettercosmic.prisons.gangping;

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
 * Config panel for gang / truce pings: enable + colors, distance-based icon scaling, beacon beams,
 * sound, and the per-line info display. Bound to {@link PrisonsConfig} via the shared {@code Options}
 * lambdas. Sending is bound to the gang-ping (G) / truce-ping (H) / block-ping (unbound) keys.
 */
public final class GangPingsPanel {

	private GangPingsPanel() {}

	public static ConfigPanel create() {
		PrisonsConfig d = new PrisonsConfig();
		PrisonsConfig c = BetterPrisonsClient.config;

		OptionGroup general = new OptionGroup("General", List.<Option>of(
				Options.toggle("Gang pings", d.gangPingEnabled,
						() -> c.gangPingEnabled, v -> { c.gangPingEnabled = v; c.save(); }),
				PrisonOptions.colorRgb("Gang color", d.gangPingColor,
						() -> c.gangPingColor, v -> { c.gangPingColor = v; c.save(); }),
				Options.toggle("Truce pings", d.trucePingEnabled,
						() -> c.trucePingEnabled, v -> { c.trucePingEnabled = v; c.save(); }),
				PrisonOptions.colorRgb("Truce color", d.trucePingColor,
						() -> c.trucePingColor, v -> { c.trucePingColor = v; c.save(); }),
				Options.toggle("Show non-gang pings", d.gangPingShowNonGang,
						() -> c.gangPingShowNonGang, v -> { c.gangPingShowNonGang = v; c.save(); })
						.tooltip("Also show pings received outside gang/truce chat.")));

		OptionGroup icon = new OptionGroup("Icon", List.<Option>of(
				Options.toggle("Distance scaling", d.gangPingDistanceScaling,
						() -> c.gangPingDistanceScaling, v -> { c.gangPingDistanceScaling = v; c.save(); })
						.tooltip("Scale the head icon with distance (larger when farther)."),
				Options.slider("Min icon scale", d.gangPingIconMinScale, 0.1, 3.0, 0.1,
						() -> (double) c.gangPingIconMinScale, v -> { c.gangPingIconMinScale = v.floatValue(); c.save(); }),
				Options.slider("Max icon scale", d.gangPingIconMaxScale, 0.1, 3.0, 0.1,
						() -> (double) c.gangPingIconMaxScale, v -> { c.gangPingIconMaxScale = v.floatValue(); c.save(); }),
				Options.intSlider("Base opacity", d.gangPingBaseOpacity, 0, 255, 5,
						() -> c.gangPingBaseOpacity, v -> { c.gangPingBaseOpacity = v; c.save(); }),
				Options.toggle("Edge arrow when off-screen", d.gangPingEdgeEnabled,
						() -> c.gangPingEdgeEnabled, v -> { c.gangPingEdgeEnabled = v; c.save(); })));

		OptionGroup beams = new OptionGroup("Beacon Beams", List.<Option>of(
				Options.toggle("Beam", d.gangPingBeamEnabled,
						() -> c.gangPingBeamEnabled, v -> { c.gangPingBeamEnabled = v; c.save(); }),
				Options.intSlider("Beam opacity", d.gangPingBeamOpacity, 0, 255, 5,
						() -> c.gangPingBeamOpacity, v -> { c.gangPingBeamOpacity = v; c.save(); })));

		OptionGroup sound = new OptionGroup("Sound", List.<Option>of(
				Options.toggle("Sound", d.gangPingSoundEnabled,
						() -> c.gangPingSoundEnabled, v -> { c.gangPingSoundEnabled = v; c.save(); }),
				Options.intSlider("Volume", d.gangPingSoundVolume, 0, 100, 5,
						() -> c.gangPingSoundVolume, v -> { c.gangPingSoundVolume = v; c.save(); })));

		OptionGroup text = new OptionGroup("Text Display", List.<Option>of(
				Options.toggle("Name", d.gangPingShowName,
						() -> c.gangPingShowName, v -> { c.gangPingShowName = v; c.save(); }),
				Options.toggle("Timer", d.gangPingShowTimer,
						() -> c.gangPingShowTimer, v -> { c.gangPingShowTimer = v; c.save(); }),
				Options.toggle("Coordinates", d.gangPingShowCoords,
						() -> c.gangPingShowCoords, v -> { c.gangPingShowCoords = v; c.save(); }),
				Options.toggle("HP", d.gangPingShowHp,
						() -> c.gangPingShowHp, v -> { c.gangPingShowHp = v; c.save(); }),
				Options.toggle("Facing", d.gangPingShowFacing,
						() -> c.gangPingShowFacing, v -> { c.gangPingShowFacing = v; c.save(); }),
				Options.slider("Text scale", d.gangPingTextScale, 0.5, 3.0, 0.1,
						() -> (double) c.gangPingTextScale, v -> { c.gangPingTextScale = v.floatValue(); c.save(); })));

		return ConfigPanel.of("prisons-gangpings", "Gang Pings",
				"Gang & truce ping markers", PanelIcon.SPARKLE,
				List.of(general, icon, beams, sound, text));
	}
}
