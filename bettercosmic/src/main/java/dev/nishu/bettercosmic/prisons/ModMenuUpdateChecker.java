package dev.nishu.bettercosmic.prisons;

import com.terraformersmc.modmenu.api.UpdateChannel;
import com.terraformersmc.modmenu.api.UpdateInfo;
import dev.nishu.bettercosmic.shared.config.SharedConfig;
import dev.nishu.bettercosmic.shared.update.UpdateChecker;
import dev.nishu.bettercosmic.shared.update.UpdateState;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

/**
 * ModMenu adapter for the shared auto-updater: surfaces an "update available" badge on BetterCosmic's
 * mod-list entry, reusing the same {@link UpdateChecker} result as the toast and config-screen row.
 * Registered via {@link ModMenuIntegration#getUpdateChecker()}; only active when ModMenu is installed.
 *
 * <p>ModMenu calls {@link #checkForUpdates()} on its own background thread (blocking is allowed), so
 * we reuse the cached {@link UpdateState} when the shared check has already run, else fetch
 * synchronously. Respects the user's opt-out — no badge when {@code autoUpdateCheck} is off.
 */
public class ModMenuUpdateChecker implements com.terraformersmc.modmenu.api.UpdateChecker {

	@Nullable
	@Override
	public UpdateInfo checkForUpdates() {
		if (!SharedConfig.get().autoUpdateCheck) {
			return null; // user opted out — no badge
		}
		UpdateState s = UpdateChecker.state();
		if (s == null) {
			s = UpdateChecker.fetchNow(); // shared check hasn't finished yet; do it here (off-thread)
		}
		if (!s.available) {
			return null; // ModMenu shows no badge
		}
		return new Info(s);
	}

	/** Immutable {@link UpdateInfo} view over an available {@link UpdateState}. */
	private record Info(UpdateState state) implements UpdateInfo {
		@Override
		public boolean isUpdateAvailable() {
			return state.available;
		}

		@Nullable
		@Override
		public Component getUpdateMessage() {
			return Component.literal("BetterCosmic " + state.latest + " available");
		}

		@Override
		public String getDownloadLink() {
			return state.url != null ? state.url : UpdateChecker.RELEASES_URL;
		}

		@Override
		public UpdateChannel getUpdateChannel() {
			return UpdateChannel.RELEASE; // single release channel for now
		}
	}
}
