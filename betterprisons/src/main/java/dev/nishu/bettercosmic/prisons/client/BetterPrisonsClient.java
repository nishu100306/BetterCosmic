package dev.nishu.bettercosmic.prisons.client;

import dev.nishu.bettercosmic.prisons.BetterPrisons;
import dev.nishu.bettercosmic.prisons.config.PrisonsConfig;
import dev.nishu.bettercosmic.prisons.easyview.EasyViewPanel;
import dev.nishu.bettercosmic.prisons.easyview.EasyViewProvider;
import dev.nishu.bettercosmic.shared.config.BetterCosmicConfig;
import dev.nishu.bettercosmic.shared.config.SharedConfig;
import dev.nishu.bettercosmic.shared.easyview.EasyView;
import dev.nishu.bettercosmic.shared.hud.HudRenderer;
import dev.nishu.bettercosmic.shared.notification.ToastRenderer;
import dev.nishu.bettercosmic.shared.ui.ConfigUi;
import dev.nishu.bettercosmic.shared.ui.model.ConfigPanel;
import dev.nishu.bettercosmic.shared.ui.model.ConfigRegistry;
import dev.nishu.bettercosmic.shared.ui.model.OptionGroup;
import dev.nishu.bettercosmic.shared.ui.model.Options;
import dev.nishu.bettercosmic.shared.ui.model.PanelIcon;
import net.fabricmc.api.ClientModInitializer;

import java.util.List;

/**
 * Client entrypoint for BetterPrisons.
 *
 * <p>Loads configs and stands up the shared services BetterPrisons builds on ({@link HudRenderer},
 * {@link ToastRenderer}). Feature systems (HUDs, EasyView providers, waypoints, CosmicApi, ...) are
 * registered here as they are ported in during Phase C; today this wires the config pipeline and one
 * real feature panel so the shared config screen shows a BetterPrisons tab.
 */
public class BetterPrisonsClient implements ClientModInitializer {

	/** Shared config (config/bettercosmic/shared.json) — the same instance every mod uses. */
	public static SharedConfig sharedConfig;

	/** BetterPrisons' own config (config/bettercosmic/betterprisons.json). */
	public static PrisonsConfig config;

	@Override
	public void onInitializeClient() {
		sharedConfig = SharedConfig.get();
		config = BetterCosmicConfig.load(PrisonsConfig.class);

		// Brand the shared config screen and route toasts to the configured corner.
		ConfigUi.setSubtitle("Prisons");
		ToastRenderer.setCornerSupplier(() -> config.toastCorner);
		ToastRenderer.register();

		// Draw registered HUDs (populated as HUDs are ported in Phase C).
		HudRenderer.register();

		// EasyView inventory/hotbar overlays (drawn by the shared EasyView mixins).
		EasyView.register(new EasyViewProvider());

		registerPanels();

		BetterPrisons.LOGGER.info("BetterPrisons initialized. Configs: {} and {}",
				sharedConfig.configPath(), config.configPath());
	}

	/**
	 * Registers the BetterPrisons config panels. Only "Misc" is wired so far; the per-feature panels
	 * (HUDs, EasyView, Waypoints, ...) are added alongside their features in Phase C.
	 */
	private void registerPanels() {
		PrisonsConfig def = new PrisonsConfig(); // code defaults, so reset restores these

		OptionGroup qol = new OptionGroup("Quality of life", List.of(
				Options.toggle("Auto-trade", def.autoTradeEnabled,
						() -> config.autoTradeEnabled,
						v -> { config.autoTradeEnabled = v; config.save(); })
						.tooltip("Shift-right-click a player to send /trade <name>."),
				Options.toggle("Bold XP/Energy popups", def.boldXpEnergyTitles,
						() -> config.boldXpEnergyTitles,
						v -> { config.boldXpEnergyTitles = v; config.save(); })
						.tooltip("Bold the server's +XP / +Energy title popups."),
				Options.toggle("PrisonBreak texture pack", def.prisonbreakTexturePackEnabled,
						() -> config.prisonbreakTexturePackEnabled,
						v -> { config.prisonbreakTexturePackEnabled = v; config.save(); })
						.tooltip("Auto-apply the bundled ore pack in the PrisonBreak world.")));

		OptionGroup search = new OptionGroup("Search", List.of(
				Options.toggle("Chest search", def.chestSearchEnabled,
						() -> config.chestSearchEnabled,
						v -> { config.chestSearchEnabled = v; config.save(); })
						.tooltip("Search bar + filter sidebar in containers."),
				Options.toggle("Clue scroll sorting", def.clueScrollSortingEnabled,
						() -> config.clueScrollSortingEnabled,
						v -> { config.clueScrollSortingEnabled = v; config.save(); })
						.tooltip("Show each clue scroll's step number on the item.")));

		ConfigRegistry.register(ConfigPanel.of("prisons-misc", "Misc",
				"General BetterPrisons features", PanelIcon.GEAR, List.of(qol, search)));

		ConfigRegistry.register(EasyViewPanel.create());
	}
}
