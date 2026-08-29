package dev.nishu.bettercosmic.prisons.hud;

import dev.nishu.bettercosmic.prisons.client.BetterPrisonsClient;
import dev.nishu.bettercosmic.prisons.config.PrisonsConfig;
import dev.nishu.bettercosmic.prisons.ui.PrisonOptions;
import dev.nishu.bettercosmic.shared.notification.Sounds;
import dev.nishu.bettercosmic.shared.ui.model.ConfigPanel;
import dev.nishu.bettercosmic.shared.ui.model.Option;
import dev.nishu.bettercosmic.shared.ui.model.OptionGroup;
import dev.nishu.bettercosmic.shared.ui.model.Options;
import dev.nishu.bettercosmic.shared.ui.model.PanelIcon;

import java.util.List;

/**
 * Config panel for the Events HUD (meteors, ore merchants, bandit rushes, meteorite showers). Bound to
 * {@link PrisonsConfig} via the shared {@code Options} lambdas.
 */
public final class EventsHudPanel {

	private EventsHudPanel() {}

	public static ConfigPanel create() {
		PrisonsConfig d = new PrisonsConfig();
		PrisonsConfig c = BetterPrisonsClient.config;

		OptionGroup general = new OptionGroup("Toggle & Title", List.<Option>of(
				Options.toggle("Events HUD", d.eventsHudEnabled,
						() -> c.eventsHudEnabled, v -> { c.eventsHudEnabled = v; BetterPrisonsClient.eventsHud.enabled = v; c.save(); }),
				Options.toggle("Show title", d.showEventsHudTitle,
						() -> c.showEventsHudTitle, v -> { c.showEventsHudTitle = v; c.save(); }),
				PrisonOptions.colorRgb("Title color", d.eventsHudTitleColor,
						() -> c.eventsHudTitleColor, v -> { c.eventsHudTitleColor = v; c.save(); }),
				Options.intSlider("Scale", d.eventsHudScale, 50, 150, 5,
						() -> c.eventsHudScale, v -> { c.eventsHudScale = v; c.save(); }),
				PrisonOptions.colorRgb("Coordinate text color", d.eventsTextColor,
						() -> c.eventsTextColor, v -> { c.eventsTextColor = v; c.save(); })));

		OptionGroup meteors = new OptionGroup("Meteors", List.<Option>of(
				Options.toggle("Natural meteors", d.naturalMeteorsEnabled,
						() -> c.naturalMeteorsEnabled, v -> { c.naturalMeteorsEnabled = v; c.save(); }),
				PrisonOptions.colorRgb("Natural color", d.eventsNaturalHeadingColor,
						() -> c.eventsNaturalHeadingColor, v -> { c.eventsNaturalHeadingColor = v; c.save(); }),
				Options.toggle("Summoned meteors", d.summonedMeteorsEnabled,
						() -> c.summonedMeteorsEnabled, v -> { c.summonedMeteorsEnabled = v; c.save(); }),
				PrisonOptions.colorRgb("Summoned color", d.eventsSummonedHeadingColor,
						() -> c.eventsSummonedHeadingColor, v -> { c.eventsSummonedHeadingColor = v; c.save(); }),
				Options.toggle("Show distance", d.meteorShowDistance,
						() -> c.meteorShowDistance, v -> { c.meteorShowDistance = v; c.save(); }),
				Options.text("Icon item id", d.eventsIconItemId,
						() -> c.eventsIconItemId, v -> { c.eventsIconItemId = v; c.save(); }),
				Options.intSlider("Crashed display (s)", d.eventsCrashedDisplayDuration, 1, 120, 1,
						() -> c.eventsCrashedDisplayDuration, v -> { c.eventsCrashedDisplayDuration = v; c.save(); })));

		OptionGroup merchants = new OptionGroup("Merchants", List.<Option>of(
				Options.toggle("Merchants", d.merchantsEnabled,
						() -> c.merchantsEnabled, v -> { c.merchantsEnabled = v; c.save(); }),
				Options.toggle("Show distance", d.merchantShowDistance,
						() -> c.merchantShowDistance, v -> { c.merchantShowDistance = v; c.save(); }),
				Options.intSlider("Timeout (min)", d.merchantTimeoutMinutes, 1, 60, 1,
						() -> c.merchantTimeoutMinutes, v -> { c.merchantTimeoutMinutes = v; c.save(); }),
				Options.toggle("Coal", d.coalMerchantEnabled, () -> c.coalMerchantEnabled, v -> { c.coalMerchantEnabled = v; c.save(); }),
				Options.toggle("Iron", d.ironMerchantEnabled, () -> c.ironMerchantEnabled, v -> { c.ironMerchantEnabled = v; c.save(); }),
				Options.toggle("Lapis", d.lapisMerchantEnabled, () -> c.lapisMerchantEnabled, v -> { c.lapisMerchantEnabled = v; c.save(); }),
				Options.toggle("Redstone", d.redstoneMerchantEnabled, () -> c.redstoneMerchantEnabled, v -> { c.redstoneMerchantEnabled = v; c.save(); }),
				Options.toggle("Gold", d.goldMerchantEnabled, () -> c.goldMerchantEnabled, v -> { c.goldMerchantEnabled = v; c.save(); }),
				Options.toggle("Diamond", d.diamondMerchantEnabled, () -> c.diamondMerchantEnabled, v -> { c.diamondMerchantEnabled = v; c.save(); }),
				Options.toggle("Emerald", d.emeraldMerchantEnabled, () -> c.emeraldMerchantEnabled, v -> { c.emeraldMerchantEnabled = v; c.save(); })));

		OptionGroup bandit = new OptionGroup("Bandit Rush", List.<Option>of(
				Options.toggle("Bandit rush", d.banditRushEnabled,
						() -> c.banditRushEnabled, v -> { c.banditRushEnabled = v; c.save(); }),
				PrisonOptions.colorRgb("Heading color", d.banditRushHeadingColor,
						() -> c.banditRushHeadingColor, v -> { c.banditRushHeadingColor = v; c.save(); }),
				PrisonOptions.colorRgb("Text color", d.banditRushTextColor,
						() -> c.banditRushTextColor, v -> { c.banditRushTextColor = v; c.save(); }),
				Options.intSlider("Timeout (s)", d.banditRushTimeoutSeconds, 10, 300, 5,
						() -> c.banditRushTimeoutSeconds, v -> { c.banditRushTimeoutSeconds = v; c.save(); }),
				Options.toggle("Sound", d.banditRushSoundEnabled,
						() -> c.banditRushSoundEnabled, v -> { c.banditRushSoundEnabled = v; c.save(); }),
				Options.dropdown("Sound type", d.banditRushSound, Sounds.OPTIONS,
						() -> c.banditRushSound, v -> { c.banditRushSound = v; c.save(); }),
				Options.intSlider("Sound volume", d.banditRushSoundVolume, 0, 100, 5,
						() -> c.banditRushSoundVolume, v -> { c.banditRushSoundVolume = v; c.save(); })));

		OptionGroup shower = new OptionGroup("Meteorite Shower", List.<Option>of(
				Options.toggle("Meteorite shower", d.meteoriteShowerEnabled,
						() -> c.meteoriteShowerEnabled, v -> { c.meteoriteShowerEnabled = v; c.save(); }),
				PrisonOptions.colorRgb("Heading color", d.meteoriteShowerHeadingColor,
						() -> c.meteoriteShowerHeadingColor, v -> { c.meteoriteShowerHeadingColor = v; c.save(); }),
				PrisonOptions.colorRgb("Text color", d.meteoriteShowerTextColor,
						() -> c.meteoriteShowerTextColor, v -> { c.meteoriteShowerTextColor = v; c.save(); }),
				Options.intSlider("Mineable display (s)", d.meteoriteShowerTimeoutSeconds, 10, 600, 5,
						() -> c.meteoriteShowerTimeoutSeconds, v -> { c.meteoriteShowerTimeoutSeconds = v; c.save(); }),
				Options.toggle("Sound", d.meteoriteShowerSoundEnabled,
						() -> c.meteoriteShowerSoundEnabled, v -> { c.meteoriteShowerSoundEnabled = v; c.save(); }),
				Options.dropdown("Sound type", d.meteoriteShowerSound, Sounds.OPTIONS,
						() -> c.meteoriteShowerSound, v -> { c.meteoriteShowerSound = v; c.save(); }),
				Options.intSlider("Sound volume", d.meteoriteShowerSoundVolume, 0, 100, 5,
						() -> c.meteoriteShowerSoundVolume, v -> { c.meteoriteShowerSoundVolume = v; c.save(); })));

		OptionGroup box = new OptionGroup("Background & Border", List.<Option>of(
				PrisonOptions.colorRgb("Background", d.eventsBgColor,
						() -> c.eventsBgColor, v -> { c.eventsBgColor = v; c.save(); }),
				Options.intSlider("Background opacity", d.eventsBgOpacity, 0, 255, 5,
						() -> c.eventsBgOpacity, v -> { c.eventsBgOpacity = v; c.save(); }),
				PrisonOptions.colorRgb("Border", d.eventsBorderColor,
						() -> c.eventsBorderColor, v -> { c.eventsBorderColor = v; c.save(); }),
				Options.intSlider("Border opacity", d.eventsBorderOpacity, 0, 255, 5,
						() -> c.eventsBorderOpacity, v -> { c.eventsBorderOpacity = v; c.save(); }),
				Options.intSlider("Border thickness", d.eventsBorderThickness, 0, 6, 1,
						() -> c.eventsBorderThickness, v -> { c.eventsBorderThickness = v; c.save(); })));

		return ConfigPanel.of("prisons-events", "Events HUD",
				"Meteors, merchants, bandit rushes & showers", PanelIcon.SPARKLE,
				List.of(general, meteors, merchants, bandit, shower, box));
	}
}
