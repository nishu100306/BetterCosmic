package dev.nishu.bettercosmic.prisons.notification;

import dev.nishu.bettercosmic.prisons.client.BetterPrisonsClient;
import dev.nishu.bettercosmic.prisons.config.PrisonsConfig;
import dev.nishu.bettercosmic.shared.notification.Sounds;
import dev.nishu.bettercosmic.shared.ui.model.ConfigPanel;
import dev.nishu.bettercosmic.shared.ui.model.Option;
import dev.nishu.bettercosmic.shared.ui.model.OptionGroup;
import dev.nishu.bettercosmic.shared.ui.model.Options;
import dev.nishu.bettercosmic.shared.ui.model.PanelIcon;

import java.util.ArrayList;
import java.util.List;

/**
 * Config panel for notifications: an enable toggle, sound, and volume per {@link NotificationType}
 * (message received, name mentioned, powerball ready, satchel full). Bound to the {@code notification*}
 * maps on {@link PrisonsConfig} via the shared {@code Options} lambdas.
 */
public final class NotificationsPanel {

	private NotificationsPanel() {}

	public static ConfigPanel create() {
		PrisonsConfig c = BetterPrisonsClient.config;
		List<OptionGroup> groups = new ArrayList<>();

		for (NotificationType type : NotificationType.values()) {
			String id = type.id;
			groups.add(new OptionGroup(type.displayName, List.<Option>of(
					Options.toggle("Enabled", type.defaultEnabled,
							() -> c.notificationEnabled.getOrDefault(id, type.defaultEnabled),
							v -> { c.notificationEnabled.put(id, v); c.save(); }),
					Options.dropdown("Sound", type.defaultSound, Sounds.OPTIONS,
							() -> c.notificationSound.getOrDefault(id, type.defaultSound),
							v -> { c.notificationSound.put(id, v); c.save(); }),
					Options.intSlider("Volume", type.defaultVolume, 0, 100, 5,
							() -> c.notificationVolume.getOrDefault(id, type.defaultVolume),
							v -> { c.notificationVolume.put(id, v); c.save(); }))));
		}

		return ConfigPanel.of("prisons-notifications", "Notifications",
				"Sounds & alerts for messages, mentions, and more", PanelIcon.BELL, groups);
	}
}
