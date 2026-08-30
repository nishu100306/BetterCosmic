package dev.nishu.bettercosmic.shared.update;

import dev.nishu.bettercosmic.shared.BetterCosmicShared;
import dev.nishu.bettercosmic.shared.config.BetterCosmicConfig;
import dev.nishu.bettercosmic.shared.notification.Notifier;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Phase 2 — opt-in staged self-apply, in-session-move model (as used by ModMenuUpdater).
 *
 * <p>Fabric Loader loads the <em>newest</em> of two same-id jars in {@code mods/} and leaves the older
 * one unselected — it does not crash on the duplicate. So the update can be dropped in live:
 * <ol>
 *   <li><b>This session:</b> download the new jar, verify its SHA-256, and {@code Files.move} it
 *       straight into {@code mods/} under its versioned filename. The running (old) jar is left
 *       untouched — no attempt to replace a file the OS has locked. Its path is recorded in a marker.</li>
 *   <li><b>Next launch:</b> Fabric loads the new jar; the old one is never opened, so it isn't locked.
 *       {@link #performCleanup()} (run at client init) moves the marker's old jar into
 *       {@code config/bettercosmic/backups/} as a rollback point, leaving just the new jar.</li>
 * </ol>
 *
 * <p>No detached process, no shutdown hook, no file-lock fight. Gated behind
 * {@code SharedConfig.autoUpdateApply} (default off); no-ops in dev / when the mod isn't a single jar.
 * Integrity is mandatory: it refuses to install a jar the manifest has no matching SHA-256 for. See
 * {@code planning/AUTO_UPDATER_PLAN.md} §5B.
 */
public final class UpdateApplier {

	private static final String MOD_ID = "bettercosmic";
	private static final String TMP_DIR = ".bettercosmic-updates"; // download scratch (not scanned by Fabric)
	private static final String MARKER = "pending-cleanup.properties";
	private static final String BACKUPS = "backups";

	private static HttpClient downloadClient;
	private static final AtomicBoolean installing = new AtomicBoolean(false);

	private UpdateApplier() {}

	/** True when the mod is a single jar in a writable dir — i.e., an in-place install is possible. */
	public static boolean canSelfApply() {
		Path jar = ownJar();
		return jar != null && jar.getFileName().toString().endsWith(".jar") && Files.isRegularFile(jar);
	}

	/**
	 * Run at client init: retire the jar a previous session upgraded away from. Identifies the old jar
	 * by the marker (not a version-string match) and removes it whenever it isn't the jar we're actually
	 * running now — so a single {@code bettercosmic} jar remains. Safe to call always; no-ops when
	 * there's nothing pending.
	 */
	public static void performCleanup() {
		try {
			Path marker = markerPath();
			if (marker == null || !Files.exists(marker)) {
				return;
			}
			Path oldJar = Path.of(load(marker).getProperty("oldJar", ""));
			Path own = ownJar();
			if (own == null) {
				return; // can't tell what we're running (dev / non-jar) — leave the marker for a real launch
			}
			if (oldJar.toString().isEmpty() || !Files.exists(oldJar)) {
				deleteQuietly(marker); // already gone
				return;
			}
			if (isSameFile(oldJar, own)) {
				// Fabric loaded the old jar (the new one hasn't taken yet) — keep the marker and retry.
				BetterCosmicShared.LOGGER.info("Update pending: still running old jar {}; will retry next launch.",
						oldJar.getFileName());
				return;
			}
			backup(oldJar);
			BetterCosmicShared.LOGGER.info("Update applied; retired old jar {} to backups/.", oldJar.getFileName());
			deleteQuietly(marker);
		} catch (Exception e) {
			BetterCosmicShared.LOGGER.error("performCleanup failed", e);
		}
	}

	/** Downloads + verifies the update off-thread, then drops it into {@code mods/} for the next launch. */
	public static void installAsync(UpdateState state) {
		if (!canSelfApply()) {
			BetterCosmicShared.LOGGER.info("Self-apply unavailable (dev / non-jar install); not installing.");
			return;
		}
		if (state.url == null || state.url.isBlank()) {
			BetterCosmicShared.LOGGER.warn("Manifest has no download URL; cannot self-apply.");
			return;
		}
		if (state.sha256 == null || state.sha256.isBlank()) {
			BetterCosmicShared.LOGGER.warn("Manifest has no sha256; refusing to auto-install an unverified jar.");
			return;
		}
		if (!installing.compareAndSet(false, true)) {
			return; // an install is already in flight
		}
		CompletableFuture.runAsync(() -> {
			try {
				doInstall(state);
			} finally {
				installing.set(false);
			}
		});
	}

	private static void doInstall(UpdateState state) {
		try {
			Path target = newJarPath(state.latest); // mods/bettercosmic-<latest>.jar
			if (target == null) {
				return;
			}
			// Already dropped in this or a prior session (and verified)? Just (re)write the marker + notify.
			if (Files.exists(target) && state.sha256.equalsIgnoreCase(sha256Of(target))) {
				writeMarker();
				notifyDownloaded(state.latest);
				return;
			}

			Path tmpDir = tmpDir();
			Files.createDirectories(tmpDir);
			Path part = tmpDir.resolve("bettercosmic-" + state.latest + ".jar.part");

			HttpResponse<Path> resp = downloadClient().send(
					HttpRequest.newBuilder(URI.create(state.url)).timeout(Duration.ofMinutes(5)).GET().build(),
					HttpResponse.BodyHandlers.ofFile(part, StandardOpenOption.CREATE,
							StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING));
			if (resp.statusCode() != 200) {
				BetterCosmicShared.LOGGER.warn("Update download failed (HTTP {}).", resp.statusCode());
				deleteQuietly(part);
				return;
			}

			String actual = sha256Of(part);
			if (!state.sha256.equalsIgnoreCase(actual)) {
				BetterCosmicShared.LOGGER.error(
						"Downloaded jar hash mismatch (expected {}, got {}); discarding.", state.sha256, actual);
				deleteQuietly(part);
				return;
			}

			// Drop the verified jar into mods/ under its versioned name. Fabric will load it (newest) next
			// launch; the running old jar is left alone and retired by performCleanup() then.
			Files.move(part, target, StandardCopyOption.REPLACE_EXISTING);
			writeMarker();
			BetterCosmicShared.LOGGER.info("Installed update {} into mods/ (active next launch).", state.latest);
			notifyDownloaded(state.latest);
		} catch (Exception e) {
			BetterCosmicShared.LOGGER.error("Failed to install update", e);
		}
	}

	// ---- helpers ----

	/** Moves the retired jar into {@code config/bettercosmic/backups/} (one rollback copy per version). */
	private static void backup(Path oldJar) throws Exception {
		Path backups = configDir().resolve(BACKUPS);
		Files.createDirectories(backups);
		Files.move(oldJar, backups.resolve(oldJar.getFileName()), StandardCopyOption.REPLACE_EXISTING);
	}

	/** Records the currently-running jar as the one to retire once the new jar is loaded next launch. */
	private static void writeMarker() throws Exception {
		Properties p = new Properties();
		p.setProperty("oldJar", ownJar().toString());
		Files.createDirectories(configDir());
		try (OutputStream out = Files.newOutputStream(markerPath(),
				StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
			p.store(out, "BetterCosmic pending cleanup — old jar retired by performCleanup() next launch");
		}
	}

	private static void notifyDownloaded(String version) {
		Minecraft.getInstance().execute(() -> Notifier.toast(
				Component.literal("BetterCosmic " + version + " downloaded"),
				Component.literal("Restart to finish installing"), null, 8000L, "note_pling", 0.5f));
	}

	private static Properties load(Path marker) throws Exception {
		Properties p = new Properties();
		try (InputStream in = Files.newInputStream(marker)) {
			p.load(in);
		}
		return p;
	}

	private static boolean isSameFile(Path a, Path b) {
		try {
			return a != null && b != null && Files.isSameFile(a, b);
		} catch (Exception e) {
			return a != null && b != null
					&& a.toAbsolutePath().normalize().equals(b.toAbsolutePath().normalize());
		}
	}

	private static Path ownJar() {
		return FabricLoader.getInstance().getModContainer(MOD_ID)
				.map(c -> c.getOrigin().getPaths())
				.filter(paths -> paths.size() == 1)
				.map(List::getFirst)
				.orElse(null);
	}

	private static Path modsDir() {
		Path jar = ownJar();
		return jar != null ? jar.getParent() : null;
	}

	private static Path newJarPath(String version) {
		Path mods = modsDir();
		return mods != null ? mods.resolve("bettercosmic-" + version + ".jar") : null;
	}

	private static Path tmpDir() {
		Path mods = modsDir();
		return mods != null ? mods.resolve(TMP_DIR) : null;
	}

	private static Path configDir() {
		return BetterCosmicConfig.configDir();
	}

	private static Path markerPath() {
		return configDir().resolve(MARKER);
	}

	private static synchronized HttpClient downloadClient() {
		if (downloadClient == null) {
			downloadClient = HttpClient.newBuilder()
					.connectTimeout(Duration.ofSeconds(10))
					// The release-asset URL 302-redirects to objects.githubusercontent.com; follow it.
					// Integrity is still guaranteed by the SHA-256 check on the downloaded bytes.
					.followRedirects(HttpClient.Redirect.NORMAL)
					.build();
		}
		return downloadClient;
	}

	private static String sha256Of(Path file) throws Exception {
		MessageDigest md = MessageDigest.getInstance("SHA-256");
		try (InputStream in = Files.newInputStream(file)) {
			byte[] buf = new byte[1 << 16];
			int n;
			while ((n = in.read(buf)) > 0) {
				md.update(buf, 0, n);
			}
		}
		StringBuilder sb = new StringBuilder();
		for (byte b : md.digest()) {
			sb.append(Character.forDigit((b >> 4) & 0xF, 16)).append(Character.forDigit(b & 0xF, 16));
		}
		return sb.toString();
	}

	private static void deleteQuietly(Path p) {
		try {
			Files.deleteIfExists(p);
		} catch (Exception ignored) {
			// best-effort
		}
	}
}
