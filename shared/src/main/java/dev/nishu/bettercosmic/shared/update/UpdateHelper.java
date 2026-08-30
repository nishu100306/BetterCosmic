package dev.nishu.bettercosmic.shared.update;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Properties;

/**
 * Standalone update applier — runs as its <b>own short-lived JVM</b> spawned by {@link UpdateApplier}
 * when the game is closing, so it survives past the point where the OS releases the lock on the running
 * {@code bettercosmic.jar}. A running JVM cannot replace its own loaded jar (the file is locked for the
 * whole process lifetime on Windows — verified empirically), so the swap must happen from outside it.
 *
 * <p><b>Strict constraints — do not add dependencies.</b> This class is extracted from the mod jar as a
 * single {@code .class} and launched with {@code java -cp <tmp> …UpdateHelper <marker>}; only the JDK is
 * on its classpath. No Gson, no Minecraft, no Fabric, no other project classes, no inner/lambda-captured
 * types that would compile to separate class files. The marker is a plain {@link Properties} file for
 * exactly this reason.
 *
 * <p>Flow: verify the staged jar's SHA-256 → poll until the old jar becomes deletable (the parent JVM
 * has exited and released the lock) → delete the old jar → move the staged jar into {@code mods/} →
 * clean up. Everything is logged to the marker's {@code logFile} for post-mortem diagnosis. On timeout
 * or failure it leaves the staged jar and marker in place so the next launch can retry.
 */
public final class UpdateHelper {

	private static final long WAIT_TIMEOUT_MS = 120_000;
	private static final long POLL_MS = 250;

	private UpdateHelper() {}

	public static void main(String[] args) {
		if (args.length < 1) {
			return;
		}
		Path marker = Path.of(args[0]);
		Path log = null;
		try {
			Properties p = new Properties();
			try (InputStream in = Files.newInputStream(marker)) {
				p.load(in);
			}
			Path oldJar = Path.of(p.getProperty("oldJar"));
			Path staged = Path.of(p.getProperty("stagedJar"));
			Path finalJar = Path.of(p.getProperty("finalJar"));
			String sha256 = p.getProperty("sha256", "");
			log = Path.of(p.getProperty("logFile", marker.resolveSibling("apply.log").toString()));

			log(log, "helper started; target old jar = " + oldJar);

			if (!Files.exists(staged)) {
				log(log, "ABORT: staged jar missing: " + staged);
				return;
			}
			if (!sha256.isEmpty() && !sha256.equalsIgnoreCase(sha256Of(staged))) {
				log(log, "ABORT: staged jar hash mismatch; discarding staged jar");
				deleteQuietly(staged);
				deleteQuietly(marker);
				return;
			}

			// Wait for the parent JVM to exit and release the lock on the old jar. Deleting a locked
			// file throws on Windows; once the lock is gone the delete succeeds.
			long deadline = System.currentTimeMillis() + WAIT_TIMEOUT_MS;
			boolean removed = removeWhenUnlocked(oldJar, deadline, log);
			if (!removed) {
				log(log, "TIMEOUT: old jar still locked after " + (WAIT_TIMEOUT_MS / 1000)
						+ "s; leaving staged jar for next-launch retry");
				return;
			}

			// Old jar is gone — place the new one. Keep the staged copy until the move/copy succeeds so a
			// failure here never leaves the install with no jar.
			try {
				Files.createDirectories(finalJar.getParent());
				Files.move(staged, finalJar, StandardCopyOption.REPLACE_EXISTING);
			} catch (IOException moveErr) {
				log(log, "move failed (" + moveErr + "); trying copy");
				Files.copy(staged, finalJar, StandardCopyOption.REPLACE_EXISTING);
				deleteQuietly(staged);
			}
			log(log, "SUCCESS: installed " + finalJar.getFileName());

			deleteQuietly(marker);
			// Best-effort: remove the staging dir if now empty.
			try {
				Files.deleteIfExists(marker.getParent().resolve("apply.log"));
				Files.deleteIfExists(marker.getParent());
			} catch (IOException ignored) {
				// leftover staging dir is harmless
			}
		} catch (Exception e) {
			if (log != null) {
				log(log, "ERROR: " + e);
			}
		}
	}

	/** Polls until {@code jar} can be deleted (parent released the lock), or the deadline passes. */
	private static boolean removeWhenUnlocked(Path jar, long deadline, Path log) {
		boolean logged = false;
		while (System.currentTimeMillis() < deadline) {
			try {
				Files.deleteIfExists(jar);
				if (!Files.exists(jar)) {
					return true;
				}
			} catch (IOException locked) {
				if (!logged) {
					log(log, "old jar still locked; waiting for parent JVM to exit…");
					logged = true;
				}
			}
			try {
				Thread.sleep(POLL_MS);
			} catch (InterruptedException ie) {
				Thread.currentThread().interrupt();
				return false;
			}
		}
		return false;
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
		} catch (IOException ignored) {
			// best effort
		}
	}

	private static void log(Path log, String msg) {
		if (log == null) {
			return;
		}
		String line = "[" + LocalDateTime.now() + "] " + msg + System.lineSeparator();
		try {
			Files.createDirectories(log.getParent());
			try (OutputStream out = Files.newOutputStream(log,
					StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
				out.write(line.getBytes());
			}
		} catch (IOException ignored) {
			// logging is best-effort
		}
	}
}
