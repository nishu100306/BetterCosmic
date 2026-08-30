package dev.nishu.bettercosmic.shared.update;

import net.fabricmc.loader.api.SemanticVersion;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.VersionParsingException;

/**
 * Version comparison over Fabric's own {@link SemanticVersion} parser, so the updater orders versions
 * exactly the way the loader does (the mod versions are already declared as semver in
 * {@code gradle.properties}). Never hand-rolls string comparison.
 */
public final class VersionCompare {

	private VersionCompare() {}

	/**
	 * @return {@code true} iff {@code candidate} is a strictly newer version than {@code current}.
	 *     Falls back to Fabric's lenient {@link Version} ordering if either side isn't valid semver,
	 *     and to {@code false} if neither can be parsed (never throws).
	 */
	public static boolean isNewer(String candidate, String current) {
		if (candidate == null || current == null) {
			return false;
		}
		try {
			return SemanticVersion.parse(candidate).compareTo((Version) SemanticVersion.parse(current)) > 0;
		} catch (VersionParsingException e) {
			try {
				return Version.parse(candidate).compareTo(Version.parse(current)) > 0;
			} catch (VersionParsingException ex) {
				return false;
			}
		}
	}
}
