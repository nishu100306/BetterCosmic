package dev.nishu.bettercosmic.shared.update;

import dev.nishu.bettercosmic.shared.BetterCosmicShared;
import dev.nishu.bettercosmic.shared.config.SharedConfig;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * The update-check engine. On client init it asynchronously fetches the update manifest, compares the
 * newest published version against the installed one, and — if a newer, Minecraft-compatible build
 * exists — surfaces it three ways: a persistent, clickable in-game toast (a button {@link ToastRenderer}
 * toast), a row in the shared config screen (see {@code GeneralPanel}), and a ModMenu badge (see
 * {@code bettercosmic}'s {@code ModMenuUpdateChecker}). When the user has opted in
 * ({@code autoUpdateApply}), it also hands an available update to {@link UpdateApplier} to download.
 *
 * <p>All network I/O runs off the render thread; results are published to volatile state that the UI
 * reads. Any failure fails soft — the feature simply goes quiet and the game is unaffected.
 */
public final class UpdateChecker {

	public static final String MOD_ID = "bettercosmic";

	// Update host — GitHub Pages for the manifest, GitHub Releases for the download page.
	// NOTE: the repo path is case-sensitive ("BetterCosmic"); the mod id and jar name stay lowercase.
	public static final String MANIFEST_URL = "https://nishu100306.github.io/BetterCosmic/manifest.json";
	public static final String RELEASES_URL = "https://github.com/nishu100306/BetterCosmic/releases/latest";

	private static final Duration TIMEOUT = Duration.ofSeconds(5);
	private static final int MAX_BYTES = 64 * 1024; // a manifest is tiny; cap hostile/huge responses

	private static HttpClient httpClient; // lazy; created off-thread on first fetch

	private static volatile boolean initialized = false;
	private static volatile UpdateState state = null; // null until the first check completes
	private static volatile boolean shownThisSession = false; // notified once per session, on world join

	private UpdateChecker() {}

	/** The latest completed check result, or {@code null} if no check has finished yet. */
	public static UpdateState state() {
		return state;
	}

	/** A short status line for the config-screen row. */
	public static String statusLine() {
		UpdateState s = state;
		if (s == null) {
			return "Checking for updates…";
		}
		return s.available ? "Update available: " + s.latest : "Up to date (" + s.installed + ")";
	}

	/** Registers cleanup, the update toast, and the async check. Call once from the shared client init. */
	public static void init() {
		if (initialized) {
			return;
		}
		initialized = true;

		// Retire the old jar if a previous session installed a newer one that we're now running.
		UpdateApplier.performCleanup();

		// The update toast appears on world join (not at launch / the title screen).
		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> tryShowNotification());

		if (!SharedConfig.get().autoUpdateCheck) {
			BetterCosmicShared.LOGGER.info("Update check skipped (autoUpdateCheck is off).");
			return; // checks opted out
		}
		startCheck();
	}

	/**
	 * Re-runs the manifest check asynchronously and re-arms the session toast. Used by {@link #init}
	 * and by the {@code /bcupdate check} dev command; ignores the {@code autoUpdateCheck} opt-out so a
	 * developer can force a check.
	 */
	public static void recheck() {
		shownThisSession = false;
		startCheck();
	}

	/** Kicks off the async fetch. On completion, logs the outcome; notification happens on world join. */
	private static void startCheck() {
		CompletableFuture.supplyAsync(UpdateChecker::fetchNow).thenAccept(result -> {
			state = result;
			BetterCosmicShared.LOGGER.info(
					"Update check: installed={} latest={} available={} (manifest {})",
					result.installed, result.latest, result.available, MANIFEST_URL);
			// If the check finishes while already in a world (e.g. a re-check), notify now.
			Minecraft.getInstance().execute(UpdateChecker::tryShowNotification);
		});
	}

	/**
	 * Shows the update notification once per session, when in a world and the updater is enabled. When
	 * auto-install is on, starts the download; otherwise shows an "available" toast. A <em>mandatory</em>
	 * update re-shows on every world join. Client-thread only.
	 */
	private static void tryShowNotification() {
		if (shownThisSession) {
			return;
		}
		UpdateState s = state;
		if (s == null || !s.available) {
			return;
		}
		if (Minecraft.getInstance().player == null || !SharedConfig.get().autoUpdateCheck) {
			return; // only in a world, and only while the updater is enabled
		}
		shownThisSession = !s.mandatory; // mandatory updates keep nagging each join
		if (SharedConfig.get().autoUpdateApply && UpdateApplier.canSelfApply()) {
			startDownload(s);
		} else {
			UpdateNotifier.showAvailable(s.latest, s.mandatory, UpdateApplier.canSelfApply());
		}
	}

	/** Manually installs the available update (the "available" toast's Install button). */
	public static void installNow() {
		UpdateState s = state;
		if (s != null && s.available) {
			startDownload(s);
		}
	}

	/** Shows the downloading toast and kicks off the cancellable download, wiring result callbacks. */
	private static void startDownload(UpdateState s) {
		// If a prior trigger already finished the download (e.g. a mandatory re-show), reflect that.
		if (UpdateApplier.state() == UpdateApplier.State.DOWNLOADED) {
			UpdateNotifier.showDownloaded(s.mandatory);
			return;
		}
		UpdateNotifier.showDownloading(s.mandatory);
		UpdateApplier.installAsync(s,
				() -> UpdateNotifier.showDownloaded(s.mandatory),
				UpdateNotifier::showFailed);
	}

	/** Re-attempts the download after a failure (the failed toast's Retry button). */
	public static void retryDownload() {
		UpdateState s = state;
		if (s != null && s.available) {
			startDownload(s);
		}
	}

	/** Shows a sample toast for previewing in dev ({@code /bcupdate demo}). Does not download. */
	public static void demoToast() {
		UpdateNotifier.showDownloading(false);
	}

	/** Disables the updater, cancels any pending update, and confirms with a toast. */
	public static void disableUpdater() {
		SharedConfig cfg = SharedConfig.get();
		cfg.autoUpdateCheck = false;
		cfg.autoUpdateApply = false;
		cfg.save();
		UpdateApplier.cancel();
		UpdateNotifier.showDisabled();
	}

	/**
	 * Synchronously fetches the manifest, compares versions, and returns the resulting
	 * {@link UpdateState}. Blocking — safe to call from a background thread (the async {@link #init}
	 * path and ModMenu's own update-check thread both use it). Never throws; a failure resolves to an
	 * "up to date" state so the UI stays quiet.
	 */
	public static UpdateState fetchNow() {
		String installed = installedVersion();
		try {
			UpdateManifest m = fetchManifest();
			if (m == null) {
				return UpdateState.upToDate(installed);
			}
			// Ignore a manifest that isn't for this mod (guards against a misconfigured/wrong host).
			if (m.modId != null && !m.modId.isBlank() && !m.modId.equals(MOD_ID)) {
				BetterCosmicShared.LOGGER.warn("Manifest modId '{}' != '{}'; ignoring.", m.modId, MOD_ID);
				return UpdateState.upToDate(installed);
			}
			// Only offer a build made for the Minecraft version we're actually running.
			String mc = minecraftVersion();
			if (m.minecraft != null && !m.minecraft.isBlank() && !m.minecraft.equals(mc)) {
				return UpdateState.upToDate(installed);
			}
			if (VersionCompare.isNewer(m.latest, installed)) {
				return UpdateState.available(installed, m);
			}
			return UpdateState.upToDate(installed);
		} catch (Exception e) {
			BetterCosmicShared.LOGGER.debug("Update check failed (ignored): {}", e.toString());
			return UpdateState.upToDate(installed);
		}
	}

	private static UpdateManifest fetchManifest() throws Exception {
		HttpResponse<String> resp = client().send(
				HttpRequest.newBuilder(URI.create(MANIFEST_URL))
						.timeout(TIMEOUT)
						.header("Accept", "application/json")
						.GET()
						.build(),
				HttpResponse.BodyHandlers.ofString());
		if (resp.statusCode() != 200) {
			return null;
		}
		String body = resp.body();
		if (body == null || body.length() > MAX_BYTES) {
			return null; // reject oversized/hostile responses
		}
		return UpdateManifest.parse(body);
	}

	private static synchronized HttpClient client() {
		if (httpClient == null) {
			httpClient = HttpClient.newBuilder()
					.connectTimeout(TIMEOUT)
					// Manifest lives on GitHub Pages and doesn't redirect; never chase cross-host hops.
					.followRedirects(HttpClient.Redirect.NEVER)
					.build();
		}
		return httpClient;
	}

	private static String installedVersion() {
		return FabricLoader.getInstance().getModContainer(MOD_ID)
				.map(c -> c.getMetadata().getVersion().getFriendlyString())
				.orElse("unknown");
	}

	private static String minecraftVersion() {
		return FabricLoader.getInstance().getModContainer("minecraft")
				.map(c -> c.getMetadata().getVersion().getFriendlyString())
				.orElse("unknown");
	}
}
