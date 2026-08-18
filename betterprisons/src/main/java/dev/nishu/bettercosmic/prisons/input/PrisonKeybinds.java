package dev.nishu.bettercosmic.prisons.input;

import dev.nishu.bettercosmic.prisons.client.BetterPrisonsClient;
import dev.nishu.bettercosmic.shared.input.KeyBinds;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

/**
 * BetterPrisons' key bindings, registered under a "betterprisons" category via the shared
 * {@link KeyBinds} helper. The shared config-UI already owns the "open config" key, so it isn't
 * re-registered here. More keys (gang ping, truce ping, ...) are added alongside their features.
 */
public final class PrisonKeybinds {

	public static KeyMapping resetStats;
	public static KeyMapping pauseStats;

	private PrisonKeybinds() {}

	/** Registers the keys and their poll loop. Call once from client init. */
	public static void register() {
		KeyMapping.Category category = KeyBinds.category("betterprisons", "betterprisons");
		resetStats = KeyBinds.register("key.betterprisons.reset_stats", GLFW.GLFW_KEY_R, category);
		pauseStats = KeyBinds.register("key.betterprisons.pause", GLFW.GLFW_KEY_B, category);

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (resetStats.consumeClick()) {
				if (BetterPrisonsClient.statsHud != null) {
					BetterPrisonsClient.statsHud.resetTracking();
				}
			}
			while (pauseStats.consumeClick()) {
				if (BetterPrisonsClient.statsHud != null) {
					BetterPrisonsClient.statsHud.togglePause();
				}
			}
		});
	}
}
