package dev.nishu.bettercosmic.shared.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.nishu.bettercosmic.shared.BetterCosmicShared;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Base class for all BetterCosmic config files.
 *
 * <p>Every config is a plain object of public fields (each with a sensible default) serialized to
 * JSON under a single {@code bettercosmic/} directory inside Minecraft's config folder:
 * <pre>
 *   config/bettercosmic/shared.json          shared across BetterSky and BetterPrisons
 *   config/bettercosmic/bettersky.json        BetterSky only
 *   config/bettercosmic/betterprisons.json    BetterPrisons only
 * </pre>
 *
 * <p>Loading is forgiving: fields present in the file are applied, fields removed from the code are
 * ignored, and any field missing from the file keeps its in-code default — so adding a new setting
 * never breaks an existing config. A corrupt file is backed up to {@code <name>.bak} and replaced
 * with defaults rather than crashing the client.
 *
 * <p>Unlike BetterPrisons' original single 800-field config that copied every field by hand on
 * load, subclasses here declare only their fields; (de)serialization and file management are
 * handled generically.
 */
public abstract class BetterCosmicConfig {

	protected static final Gson GSON = new GsonBuilder()
			.setPrettyPrinting()
			.disableHtmlEscaping()
			.create();

	/** Directory holding every BetterCosmic config file: {@code <mcconfig>/bettercosmic}. */
	public static Path configDir() {
		return FabricLoader.getInstance().getConfigDir().resolve("bettercosmic");
	}

	/** File name for this config within {@link #configDir()}, e.g. {@code "shared.json"}. */
	public abstract String fileName();

	/** Absolute path to this config's file on disk. */
	public final Path configPath() {
		return configDir().resolve(fileName());
	}

	/**
	 * Loads a config of the given type from disk. Creates it with defaults if absent, backs up and
	 * replaces a corrupt file, and re-saves after a successful load so any newly-added fields are
	 * written back. Never returns {@code null}.
	 *
	 * @param type the concrete config class; must have a public no-arg constructor
	 */
	public static <T extends BetterCosmicConfig> T load(Class<T> type) {
		T config;
		try {
			config = type.getDeclaredConstructor().newInstance();
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException(
					"Config " + type.getName() + " must have a public no-arg constructor", e);
		}

		Path path = config.configPath();
		if (Files.notExists(path)) {
			config.save(); // first run — write defaults
			return config;
		}

		try (Reader reader = Files.newBufferedReader(path)) {
			T loaded = GSON.fromJson(reader, type);
			if (loaded != null) {
				config = loaded; // fields absent from the file keep their constructor defaults
			}
		} catch (Exception e) {
			BetterCosmicShared.LOGGER.error(
					"Failed to read config {} — backing it up and using defaults", path, e);
			backup(path);
			config.save();
			return config;
		}

		config.save(); // persist any fields added since the file was last written
		return config;
	}

	/**
	 * Serializes this config to its file, creating the {@code bettercosmic/} directory if needed.
	 *
	 * <p>The write is done to a sibling {@code .tmp} file which is then atomically moved into place,
	 * so a crash mid-write can never leave a half-written (corrupt) config on disk. Filesystems that
	 * can't perform an atomic move fall back to a plain replace.
	 */
	public void save() {
		Path target = configPath();
		Path tmp = target.resolveSibling(target.getFileName() + ".tmp");
		try {
			Files.createDirectories(configDir());
			try (Writer writer = Files.newBufferedWriter(tmp)) {
				GSON.toJson(this, writer);
			}
			try {
				Files.move(tmp, target,
						StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
			} catch (AtomicMoveNotSupportedException e) {
				Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
			}
		} catch (IOException e) {
			BetterCosmicShared.LOGGER.error("Failed to save config {}", target, e);
			try {
				Files.deleteIfExists(tmp);
			} catch (IOException ignored) {
				// best-effort cleanup of the temp file
			}
		}
	}

	private static void backup(Path path) {
		try {
			Path bak = path.resolveSibling(path.getFileName() + ".bak");
			Files.move(path, bak, StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException ex) {
			BetterCosmicShared.LOGGER.error("Failed to back up corrupt config {}", path, ex);
		}
	}
}
