package dev.nishu.bettercosmic.shared.update;

import dev.nishu.bettercosmic.shared.BetterCosmicShared;
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

/**
 * Phase 2 — opt-in staged self-apply. Downloads the new jar, verifies its SHA-256, stages it under
 * {@code mods/.bettercosmic-updates/}, and — because a running JVM can never replace its own loaded jar
 * (the file is OS-locked for the whole process lifetime; verified) — arms a shutdown hook that spawns
 * {@link UpdateHelper} as a <b>separate, detached JVM</b> on game close. That helper waits for this
 * process to exit and release the lock, then swaps the jar so the update is live on the next launch.
 *
 * <p>Gated behind {@code SharedConfig.autoUpdateApply} (default off). Integrity is mandatory: it
 * refuses to stage a jar the manifest doesn't provide a matching SHA-256 for. No-ops in dev / when the
 * mod isn't loaded from a single jar (nothing to swap). See {@code planning/AUTO_UPDATER_PLAN.md} §5B.
 */
public final class UpdateApplier {

	private static final String MOD_ID = "bettercosmic";
	private static final String STAGING_DIR = ".bettercosmic-updates";
	private static final String MARKER = "pending-update.properties";

	private static HttpClient downloadClient;
	private static volatile boolean staging = false;
	private static volatile boolean hookArmed = false;

	private UpdateApplier() {}

	/** True when the mod is a single jar in a writable dir — i.e., a swap is physically possible. */
	public static boolean canSelfApply() {
		Path jar = ownJar();
		return jar != null && jar.getFileName().toString().endsWith(".jar") && Files.isRegularFile(jar);
	}

	/**
	 * On client init: reconcile any previously staged update. If it was applied (or is stale), clean up;
	 * if it's still pending and valid, re-arm the shutdown hook so it applies when the game next closes.
	 */
	public static void resumePending() {
		try {
			Path marker = markerPath();
			if (marker == null || !Files.exists(marker)) {
				return;
			}
			Properties p = load(marker);
			String version = p.getProperty("version", "");
			Path staged = Path.of(p.getProperty("stagedJar", ""));
			String sha = p.getProperty("sha256", "");

			if (!VersionCompare.isNewer(version, installedVersion())) {
				cleanupStaging(); // already applied, or stale
				BetterCosmicShared.LOGGER.info("Cleaned up applied/stale pending update ({}).", version);
				return;
			}
			if (Files.exists(staged) && !sha.isBlank() && sha.equalsIgnoreCase(sha256Of(staged))) {
				armShutdownHook();
				BetterCosmicShared.LOGGER.info("Pending update {} staged; will install on exit.", version);
			} else {
				BetterCosmicShared.LOGGER.warn("Pending update {} staged jar missing/corrupt; discarding.", version);
				cleanupStaging();
			}
		} catch (Exception e) {
			BetterCosmicShared.LOGGER.error("resumePending failed", e);
		}
	}

	/** Downloads + verifies + stages the update off-thread, then arms the on-exit swap. */
	public static void stageAsync(UpdateState state) {
		if (!canSelfApply()) {
			BetterCosmicShared.LOGGER.info("Self-apply unavailable (dev / non-jar install); not staging.");
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
		if (staging) {
			return;
		}
		CompletableFuture.runAsync(() -> doStage(state));
	}

	private static void doStage(UpdateState state) {
		staging = true;
		try {
			if (hasValidPendingFor(state.latest)) {
				armShutdownHook();
				notifyStaged(state.latest);
				return; // already downloaded this version
			}
			Path stagingDir = stagingDir();
			Files.createDirectories(stagingDir);
			Path part = stagingDir.resolve("bettercosmic-" + state.latest + ".jar.part");

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

			Path staged = stagingDir.resolve("bettercosmic-" + state.latest + ".jar");
			Files.move(part, staged, StandardCopyOption.REPLACE_EXISTING);
			writeMarker(state, staged);
			armShutdownHook();
			BetterCosmicShared.LOGGER.info("Staged verified update {} for install on exit.", state.latest);
			notifyStaged(state.latest);
		} catch (Exception e) {
			BetterCosmicShared.LOGGER.error("Failed to stage update", e);
		} finally {
			staging = false;
		}
	}

	// ---- shutdown hook / helper spawn ----

	private static synchronized void armShutdownHook() {
		if (hookArmed) {
			return;
		}
		hookArmed = true;
		Runtime.getRuntime().addShutdownHook(new Thread(UpdateApplier::spawnHelper, "bettercosmic-update-spawn"));
	}

	/**
	 * Extracts {@link UpdateHelper} as a standalone class and launches it in its own JVM. Runs from the
	 * shutdown hook, while our jar is still readable but about to be released. The helper must not depend
	 * on anything but the JDK (see its javadoc), so a single extracted class file with no classpath runs.
	 */
	private static void spawnHelper() {
		try {
			Path marker = markerPath();
			if (marker == null || !Files.exists(marker)) {
				return;
			}
			Path tmp = Files.createTempDirectory("bc-updater");
			Path clsDir = tmp.resolve("dev/nishu/bettercosmic/shared/update");
			Files.createDirectories(clsDir);
			try (InputStream in = UpdateApplier.class.getResourceAsStream(
					"/dev/nishu/bettercosmic/shared/update/UpdateHelper.class")) {
				if (in == null) {
					BetterCosmicShared.LOGGER.error("Could not extract UpdateHelper.class; aborting self-apply.");
					return;
				}
				Files.copy(in, clsDir.resolve("UpdateHelper.class"), StandardCopyOption.REPLACE_EXISTING);
			}

			String javaBin = Path.of(System.getProperty("java.home"), "bin",
					isWindows() ? "java.exe" : "java").toString();
			ProcessBuilder pb = new ProcessBuilder(javaBin, "-cp", tmp.toString(),
					"dev.nishu.bettercosmic.shared.update.UpdateHelper", marker.toString());
			pb.redirectErrorStream(true);
			pb.redirectOutput(ProcessBuilder.Redirect.appendTo(logPath().toFile()));
			pb.start();
			BetterCosmicShared.LOGGER.info("Launched detached update helper; it will install the new jar after exit.");
		} catch (Exception e) {
			BetterCosmicShared.LOGGER.error("Failed to spawn update helper", e);
		}
	}

	// ---- helpers ----

	private static void writeMarker(UpdateState state, Path staged) throws Exception {
		Properties p = new Properties();
		p.setProperty("oldJar", ownJar().toString());
		p.setProperty("stagedJar", staged.toString());
		p.setProperty("finalJar", finalJar(state.latest).toString());
		p.setProperty("sha256", state.sha256);
		p.setProperty("version", state.latest);
		p.setProperty("logFile", logPath().toString());
		try (OutputStream out = Files.newOutputStream(markerPath(),
				StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
			p.store(out, "BetterCosmic pending update — applied by UpdateHelper on next exit");
		}
	}

	private static boolean hasValidPendingFor(String version) {
		try {
			Path marker = markerPath();
			if (marker == null || !Files.exists(marker)) {
				return false;
			}
			Properties p = load(marker);
			if (!version.equals(p.getProperty("version"))) {
				return false;
			}
			Path staged = Path.of(p.getProperty("stagedJar", ""));
			String sha = p.getProperty("sha256", "");
			return Files.exists(staged) && !sha.isBlank() && sha.equalsIgnoreCase(sha256Of(staged));
		} catch (Exception e) {
			return false;
		}
	}

	private static void cleanupStaging() {
		Path dir = stagingDir();
		if (dir == null) {
			return;
		}
		try {
			if (Files.isDirectory(dir)) {
				try (var stream = Files.newDirectoryStream(dir)) {
					for (Path child : stream) {
						deleteQuietly(child);
					}
				}
			}
			deleteQuietly(dir);
		} catch (Exception ignored) {
			// best-effort
		}
	}

	private static void notifyStaged(String version) {
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

	private static Path stagingDir() {
		Path mods = modsDir();
		return mods != null ? mods.resolve(STAGING_DIR) : null;
	}

	private static Path markerPath() {
		Path dir = stagingDir();
		return dir != null ? dir.resolve(MARKER) : null;
	}

	private static Path logPath() {
		Path dir = stagingDir();
		return dir != null ? dir.resolve("apply.log") : null;
	}

	private static Path finalJar(String version) {
		Path mods = modsDir();
		return mods != null ? mods.resolve("bettercosmic-" + version + ".jar") : null;
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

	private static boolean isWindows() {
		return System.getProperty("os.name", "").toLowerCase().contains("win");
	}

	private static String installedVersion() {
		return FabricLoader.getInstance().getModContainer(MOD_ID)
				.map(c -> c.getMetadata().getVersion().getFriendlyString())
				.orElse("unknown");
	}
}
