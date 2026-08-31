package dev.nishu.bettercosmic.shared.update;

import dev.nishu.bettercosmic.shared.notification.ToastRenderer;
import dev.nishu.bettercosmic.shared.notification.ToastRenderer.ToastButton;
import dev.nishu.bettercosmic.shared.ui.ConfigUi;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.network.chat.Component;

import java.net.URI;
import java.util.List;

/**
 * Builds the auto-updater's toasts — the notification UI, kept separate from {@link UpdateChecker}'s
 * orchestration so the check engine stays free of layout/button wiring. Every toast shares one key so
 * a new one replaces the previous (downloading → downloaded → …). All update toasts carry a dimmed
 * "Disable Updater" (destructive, de-emphasized) alongside their primary actions.
 */
final class UpdateNotifier {

	static final String KEY = "bettercosmic-update";

	private UpdateNotifier() {}

	/** Sticky toast with a live progress readout; stays until it becomes {@link #showDownloaded} or fails. */
	static void showDownloading(boolean mandatory) {
		ToastRenderer.showButtons(KEY,
				title("A new BetterCosmic update is available", mandatory),
				() -> Component.literal(progressText()), // resolved live each frame
				0L,
				List.of(cancel("Cancel"), ok(), disable()));
	}

	static void showDownloaded(boolean mandatory) {
		ToastRenderer.showButtons(KEY,
				title("BetterCosmic update downloaded", mandatory),
				Component.literal("Restart your game to install"), 0L,
				List.of(notes(), undo(), ok(), disable()));
	}

	static void showAvailable(String version, String changelog, boolean mandatory) {
		Component desc = (changelog != null && !changelog.isBlank())
				? Component.literal(changelog) : Component.literal("A new version is available.");
		ToastRenderer.showButtons(KEY,
				title("BetterCosmic " + version + " available", mandatory), desc, 0L,
				List.of(notes(), config(), ok(), disable()));
	}

	static void showFailed() {
		ToastRenderer.showButtons(KEY,
				Component.literal("BetterCosmic update failed"),
				Component.literal("Couldn't download — try again later"), 0L,
				List.of(retry(), ok(), disable()));
	}

	static void showDisabled() {
		ToastRenderer.show(
				Component.literal("Auto-Updater disabled"),
				Component.literal("Re-enable it in the config menu"), null, 8000L);
	}

	// ---- helpers ----

	private static Component title(String base, boolean mandatory) {
		return Component.literal(mandatory ? "Required: " + base : base);
	}

	private static String progressText() {
		double p = UpdateApplier.downloadProgress();
		return p < 0 ? "Downloading now…" : "Downloading now… " + (int) Math.round(p * 100) + "%";
	}

	private static ToastButton ok() {
		return ToastButton.primary(Component.literal("OK"), () -> {});
	}

	/** Cancels the in-progress download. */
	private static ToastButton cancel(String label) {
		return ToastButton.primary(Component.literal(label), UpdateApplier::cancel);
	}

	/** Removes an already-downloaded update so it won't install on restart. */
	private static ToastButton undo() {
		return ToastButton.primary(Component.literal("Undo"), UpdateApplier::cancel);
	}

	private static ToastButton retry() {
		return ToastButton.primary(Component.literal("Retry"), UpdateChecker::retryDownload);
	}

	private static ToastButton config() {
		return ToastButton.primary(Component.literal("Config"), () -> {
			Minecraft c = Minecraft.getInstance();
			c.setScreen(ConfigUi.create(c.screen));
		});
	}

	/** Opens the release page (full changelog) behind the vanilla link confirmation. */
	private static ToastButton notes() {
		return ToastButton.primary(Component.literal("Notes"), () ->
				ConfirmLinkScreen.confirmLinkNow(Minecraft.getInstance().screen, URI.create(UpdateChecker.RELEASES_URL)));
	}

	/** Destructive, de-emphasized: turns the updater off entirely. */
	private static ToastButton disable() {
		return ToastButton.secondary(Component.literal("Disable Updater"), UpdateChecker::disableUpdater);
	}
}
