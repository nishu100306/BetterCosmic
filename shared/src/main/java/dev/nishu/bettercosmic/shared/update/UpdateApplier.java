package dev.nishu.bettercosmic.shared.update;

import dev.nishu.bettercosmic.shared.BetterCosmicShared;
import dev.nishu.bettercosmic.shared.config.BetterCosmicConfig;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;

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
import java.util.concurrent.atomic.AtomicReference;

/**
 * Opt-in staged self-apply using an in-session-move model.
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
 * Integrity is mandatory: it refuses to install a jar the manifest has no matching SHA-256 for.
 */
public final class UpdateApplier {

	private static final String TMP_DIR = ".bettercosmic-updates"; // download scratch (not scanned by Fabric)
	private static final String MARKER = "pending-cleanup.properties";
	private static final String BACKUPS = "backups";

	/** The install lifecycle. {@code FAILED} is retryable; {@code DOWNLOADED} awaits a restart. */
	public enum State { IDLE, DOWNLOADING, DOWNLOADED, FAILED }

	private static HttpClient downloadClient;
	private static final AtomicReference<State> state = new AtomicReference<>(State.IDLE);
	private static volatile boolean cancelRequested = false;
	private static volatile CompletableFuture<HttpResponse<Path>> downloadFuture = null;
	private static volatile Path installedJar = null; // new jar placed in mods/, kept for uninstall
	private static volatile Path partFile = null;     // in-progress download file (for progress readout)
	private static volatile long contentLength = -1;  // total download size in bytes, -1 if unknown

	private UpdateApplier() {}

	/** Current install lifecycle state. */
	public static State state() {
		return state.get();
	}

	/** Download progress in [0,1], or {@code -1} if not downloading / size unknown. */
	public static double downloadProgress() {
		if (state.get() != State.DOWNLOADING) {
			return -1;
		}
		Path p = partFile;
		long total = contentLength;
		if (p == null || total <= 0) {
			return -1;
		}
		try {
			return Math.min(1.0, (double) Files.size(p) / total);
		} catch (Exception e) {
			return -1;
		}
	}

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

	/**
	 * Downloads + verifies the update off-thread, then drops it into {@code mods/} for the next launch.
	 * The download is cancellable via {@link #cancel()}. {@code onDownloaded} runs on the client thread
	 * once the verified jar is in place.
	 */
	public static void installAsync(UpdateState st, Runnable onDownloaded, Runnable onFailed) {
		if (!canSelfApply()) {
			BetterCosmicShared.LOGGER.info("Self-apply unavailable (dev / non-jar install); not installing.");
			return;
		}
		if (st.url == null || st.url.isBlank()) {
			BetterCosmicShared.LOGGER.warn("Manifest has no download URL; cannot self-apply.");
			return;
		}
		if (st.sha256 == null || st.sha256.isBlank()) {
			BetterCosmicShared.LOGGER.warn("Manifest has no sha256; refusing to auto-install an unverified jar.");
			return;
		}
		// Start only from a clean/failed state; DOWNLOADING/DOWNLOADED already have this in hand.
		if (!(state.compareAndSet(State.IDLE, State.DOWNLOADING)
				|| state.compareAndSet(State.FAILED, State.DOWNLOADING))) {
			return;
		}
		cancelRequested = false;
		contentLength = -1;
		CompletableFuture.runAsync(() -> {
			try {
				doInstall(st, onDownloaded, onFailed);
			} finally {
				downloadFuture = null;
				partFile = null;
			}
		});
	}

	/**
	 * Cancels an in-progress download, or — if the new jar was already placed in {@code mods/} — removes
	 * it and the cleanup marker so the update won't install on restart (a rollback to the running jar).
	 */
	public static void cancel() {
		cancelRequested = true;
		CompletableFuture<HttpResponse<Path>> f = downloadFuture;
		if (f != null) {
			f.cancel(true);
		}
		Path jar = installedJar;
		if (jar != null) {
			deleteQuietly(jar);
			installedJar = null;
		}
		deleteQuietly(markerPath());
		state.set(State.IDLE);
		BetterCosmicShared.LOGGER.info("Update download cancelled / pending install removed.");
	}

	private static void doInstall(UpdateState st, Runnable onDownloaded, Runnable onFailed) {
		try {
			Path target = newJarPath(st.latest); // mods/bettercosmic-<latest>.jar
			if (target == null) {
				fail(onFailed);
				return;
			}
			// Already dropped in this or a prior session (and verified)? Just (re)write the marker + notify.
			if (Files.exists(target) && st.sha256.equalsIgnoreCase(sha256Of(target))) {
				writeMarker();
				installedJar = target;
				state.set(State.DOWNLOADED);
				fire(onDownloaded);
				return;
			}

			Path tmpDir = tmpDir();
			Files.createDirectories(tmpDir);
			Path part = tmpDir.resolve(jarName(st.latest) + ".part");
			partFile = part;

			// Custom BodyHandler: capture Content-Length (for the progress readout) at response start,
			// then stream the body straight to the file with the built-in file subscriber.
			downloadFuture = downloadClient().sendAsync(
					HttpRequest.newBuilder(URI.create(st.url)).timeout(Duration.ofMinutes(5)).GET().build(),
					info -> {
						contentLength = info.headers().firstValueAsLong("Content-Length").orElse(-1L);
						return HttpResponse.BodySubscribers.ofFile(part, StandardOpenOption.CREATE,
								StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
					});
			HttpResponse<Path> resp;
			try {
				resp = downloadFuture.join();
			} catch (Exception cancelledOrFailed) {
				deleteQuietly(part);
				if (cancelRequested) {
					state.set(State.IDLE); // user cancelled — quiet
				} else {
					BetterCosmicShared.LOGGER.warn("Update download error: {}", cancelledOrFailed.toString());
					fail(onFailed);
				}
				return;
			}
			if (cancelRequested) {
				deleteQuietly(part);
				state.set(State.IDLE);
				return;
			}
			if (resp.statusCode() != 200) {
				BetterCosmicShared.LOGGER.warn("Update download failed (HTTP {}).", resp.statusCode());
				deleteQuietly(part);
				fail(onFailed);
				return;
			}

			String actual = sha256Of(part);
			if (!st.sha256.equalsIgnoreCase(actual)) {
				BetterCosmicShared.LOGGER.error(
						"Downloaded jar hash mismatch (expected {}, got {}); discarding.", st.sha256, actual);
				deleteQuietly(part);
				fail(onFailed);
				return;
			}
			if (cancelRequested) {
				deleteQuietly(part);
				state.set(State.IDLE);
				return;
			}

			// Drop the verified jar into mods/ under its versioned name. Fabric will load it (newest) next
			// launch; the running old jar is left alone and retired by performCleanup() then.
			Files.move(part, target, StandardCopyOption.REPLACE_EXISTING);
			writeMarker();
			installedJar = target;
			if (cancelRequested) { // cancelled during the final move — undo it
				deleteQuietly(target);
				deleteQuietly(markerPath());
				installedJar = null;
				state.set(State.IDLE);
				return;
			}
			state.set(State.DOWNLOADED);
			BetterCosmicShared.LOGGER.info("Installed update {} into mods/ (active next launch).", st.latest);
			fire(onDownloaded);
		} catch (Exception e) {
			BetterCosmicShared.LOGGER.error("Failed to install update", e);
			fail(onFailed);
		}
	}

	private static void fail(Runnable onFailed) {
		state.set(State.FAILED);
		fire(onFailed);
	}

	private static void fire(Runnable action) {
		if (action != null) {
			Minecraft.getInstance().execute(action);
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
		return FabricLoader.getInstance().getModContainer(UpdateChecker.MOD_ID)
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
		return mods != null ? mods.resolve(jarName(version)) : null;
	}

	/** The versioned jar filename for {@code version}, derived from the mod id (single source of truth). */
	private static String jarName(String version) {
		return UpdateChecker.MOD_ID + "-" + version + ".jar";
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
