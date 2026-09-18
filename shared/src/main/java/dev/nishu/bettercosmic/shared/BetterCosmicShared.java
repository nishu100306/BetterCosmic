package dev.nishu.bettercosmic.shared;

import dev.nishu.bettercosmic.shared.chestsearch.ChestSearchTintProvider;
import dev.nishu.bettercosmic.shared.command.DevCommands;
import dev.nishu.bettercosmic.shared.config.SharedConfig;
import dev.nishu.bettercosmic.shared.easyview.EasyView;
import dev.nishu.bettercosmic.shared.notification.ToastRenderer;
import dev.nishu.bettercosmic.shared.ui.ConfigUi;
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
 * consuming feature sets — BetterSky and BetterPrisons — build their features on top of. It must
 * never reference server-specific content; it provides mechanisms, the mods provide content.
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

		// Chest search: one slot-highlight tint provider for every mod. It's network-agnostic and
		// gates itself on the active network's ChestSearchRegistry entry; the search-bar/sidebar UI is
		// added by the shared ChestSearchMixin. Each mod registers its network + filter types.
		EasyView.registerTint(new ChestSearchTintProvider());

		// Register the config-UI keybind (default I) that opens the shared config screen.
		ConfigUi.init();

		// Register the shared General panel once, as a global (all-profile) panel. Feature panels are
		// registered by each mod under its own Network.
		ConfigRegistry.register(GeneralPanel.create());

		// Toast system (HUD + over-screen render + button clicks). Idempotent — the mods may also call it.
		ToastRenderer.register();

		// Auto-updates are handled by Cosmic's official nested updater (com.cosmicprisons:cosmic-updater,
		// bundled jar-in-jar and player-controlled via /cosmicupdater) — the sanctioned replacement for
		// the old self-updater, which has been removed. Nothing to wire here.

		LOGGER.info("BetterCosmic Shared library initialized (config dir: {}).",
				SharedConfig.configDir());
	}
}
