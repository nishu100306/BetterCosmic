package dev.nishu.bettercosmic.shared;

import dev.nishu.bettercosmic.shared.command.DevCommands;
import dev.nishu.bettercosmic.shared.config.SharedConfig;
import dev.nishu.bettercosmic.shared.ui.ConfigUi;
import dev.nishu.bettercosmic.shared.update.UpdateChecker;
import dev.nishu.bettercosmic.shared.ui.GeneralPanel;
import dev.nishu.bettercosmic.shared.ui.model.ConfigRegistry;
import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client-side initializer for the shared BetterCosmic library.
 *
 * <p>This module is a user-invisible <em>library mod</em>: it exposes reusable client
 * infrastructure (UI framework, HUD system, render utilities, config persistence, ...) that the
 * consuming mods — BetterSky and, later, BetterPrisons — build their features on top of. It must
 * never reference server-specific content; it provides mechanisms, the mods provide content.
 *
 * <p>See {@code ARCHITECTURE.md} at the repo root for the full design.
 */
public class BetterCosmicShared implements ClientModInitializer {
	public static final String MOD_ID = "bettercosmicshared";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitializeClient() {
		// Load (creating on first run) the shared config so config/bettercosmic/shared.json exists
		// and is available to every mod through SharedConfig.get().
		SharedConfig.get();

		// Register the shared dev commands (/bcdev toggle + gated /bcitem, ...).
		DevCommands.register();

		// Register the config-UI keybind (default I) that opens the shared config screen.
		ConfigUi.init();

		// Register the shared General panel once, as a global (all-profile) panel. Feature panels are
		// registered by each mod under its own Network.
		ConfigRegistry.register(GeneralPanel.create());

		// Auto-updater (phase 1: detect + notify). Async manifest check; surfaces via toast, the
		// General panel's Updates row, and the ModMenu badge. Fails soft.
		UpdateChecker.init();

		LOGGER.info("BetterCosmic Shared library initialized (config dir: {}).",
				SharedConfig.configDir());
	}
}
