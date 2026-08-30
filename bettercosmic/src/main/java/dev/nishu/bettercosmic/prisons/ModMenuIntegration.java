package dev.nishu.bettercosmic.prisons;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import com.terraformersmc.modmenu.api.UpdateChecker;
import dev.nishu.bettercosmic.shared.ui.ConfigUi;

/**
 * ModMenu integration: makes BetterPrisons' entry in the mod list open the shared BetterCosmic config
 * screen. Optional — this entrypoint is only loaded when ModMenu is installed; without it the config
 * screen still opens via the shared keybind (default I). Ported from BetterPrisons'
 * {@code ModMenuIntegration} (now targeting the shared {@link ConfigUi} instead of BP's screen).
 */
public class ModMenuIntegration implements ModMenuApi {

	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return ConfigUi::create;
	}

	@Override
	public UpdateChecker getUpdateChecker() {
		return new ModMenuUpdateChecker();
	}
}
