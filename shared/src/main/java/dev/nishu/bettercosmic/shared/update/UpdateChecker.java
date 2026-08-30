package dev.nishu.bettercosmic.shared.update;

import dev.nishu.bettercosmic.shared.BetterCosmicShared;
import dev.nishu.bettercosmic.shared.config.SharedConfig;
import dev.nishu.bettercosmic.shared.notification.Notifier;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * The update-check engine. On client init it asynchronously fetches the update manifest, compares the
 * newest published version against the installed one, and — if a newer, Minecraft-compatible build
 * exists — surfaces it three ways: an in-game toast on world join, a row in the shared config screen
 * (see {@code GeneralPanel}), and a ModMenu "update available" badge (see {@code bettercosmic}'s
 * {@code ModMenuUpdateChecker}). When the user has opted in ({@code autoUpdateApply}), it also hands
 * an available update to {@link UpdateApplier} to download and install.
 *
 * <p>All network I/O runs off the render thread; results are marshalled back via
 * {@link Minecraft#execute}. Any failure fails soft — the feature simply goes quiet and the game is
 * unaffected.
 *
 * <p>See {@code planning/AUTO_UPDATER_PLAN.md} for the full design and locked decisions.
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
	private static volatile boolean toastShown = false; // one toast per session

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

	/** Registers the async check and the join-toast hook. Call once from the shared client init. */
	public static void init() {
		if (initialized) {
			return;
		}
		initialized = true;

		// Retire the old jar if a previous session installed a newer one that we're now running.
		UpdateApplier.performCleanup();

		// Show the toast when the player joins a world (the earliest point the HUD-based toast can
		// render — the title screen has no HUD). Guarded so it fires at most once per session.
		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> maybeShowToast(client));

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
		toastShown = false;
		startCheck();
	}

	/** Kicks off the async fetch + notify. On completion, logs the outcome and surfaces the toast. */
	private static void startCheck() {
		CompletableFuture.supplyAsync(UpdateChecker::fetchNow).thenAccept(result -> {
			state = result;
			BetterCosmicShared.LOGGER.info(
					"Update check: installed={} latest={} available={} (manifest {})",
					result.installed, result.latest, result.available, MANIFEST_URL);
			Minecraft.getInstance().execute(() -> maybeShowToast(Minecraft.getInstance()));

			// Opt-in self-apply: download + verify + drop the new jar into mods/ for the next launch.
			if (result.available && SharedConfig.get().autoUpdateApply) {
				UpdateApplier.installAsync(result);
			}
		});
	}

	/**
	 * Shows a sample update toast immediately, bypassing the version/availability guards. For manually
	 * previewing the toast's look in dev ({@code /bcupdate demo}) without a published manifest.
	 */
	public static void demoToast() {
		Notifier.toast(
				Component.literal("BetterCosmic " + installedVersion() + "+1 available (demo)"),
				Component.literal("Open config (I) to update"), null, 6000L, "note_pling", 0.5f);
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

	/** Shows the update toast once per session, if an update is available and the player is in-game. */
	private static void maybeShowToast(Minecraft client) {
		if (toastShown || client == null || client.player == null) {
			return;
		}
		UpdateState s = state;
		if (s == null || !s.available) {
			return;
		}
		toastShown = true;

		Component title = Component.literal("BetterCosmic " + s.latest + " available");
		String descText = s.mandatory
				? "Important update — open config (I) to update"
				: (s.changelog != null && !s.changelog.isBlank()
						? trim(s.changelog, 48)
						: "Open config (I) to update");
		long duration = s.mandatory ? 8000L : 5000L;
		Notifier.toast(title, Component.literal(descText), null, duration, "note_pling", 0.5f);
	}

	private static String trim(String s, int max) {
		s = s.strip();
		return s.length() <= max ? s : s.substring(0, max - 1) + "…";
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
