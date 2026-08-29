package dev.nishu.bettercosmic.shared.server;

import dev.nishu.bettercosmic.shared.config.SharedConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;

import java.util.Locale;

/**
 * Tracks which {@link Network} the client is currently connected to, so each mod can scope its
 * features to its own server. BetterPrisons features run only on Cosmic Prisons, BetterSky only on
 * Cosmic Sky; everywhere else (other servers, single-player) their features stay dormant and only the
 * shared config UI is available.
 *
 * <p><b>Which address.</b> Detection uses the server-list entry the player connected to
 * ({@link Minecraft#getCurrentServer()}), matched by host suffix. This is stable across the network's
 * internal backend switches (a Prisons planet swap keeps you on {@code cosmicprisons.com}), which is
 * exactly the network-level scope we want. The match is computed live on each query (cheap, and free
 * of event-ordering pitfalls); {@code getCurrentServer()} is null off a multiplayer server, so
 * single-player and other servers resolve to no network.
 *
 * <p><b>Gating.</b> Feature code asks {@link #isActive(Network)}. A {@code null} network means
 * "ungated" (always active) — used for shared-internal content that belongs to no single mod. The
 * global {@link SharedConfig#restrictFeaturesToServer} toggle disables the gate entirely (everything
 * on everywhere, the legacy behaviour), and a dev override ({@link #setOverride}) forces a network for
 * testing off-server.
 */
public final class ServerContext {

	/** Dev-forced network (via {@code /bcforce}); {@code overrideActive} says whether it applies. */
	private static Network override;
	private static boolean overrideActive;

	private ServerContext() {}

	/** Detects the network from the current server address, or {@code null} when on none. */
	private static Network detect() {
		ServerData data = Minecraft.getInstance().getCurrentServer();
		if (data == null || data.ip == null) {
			return null;
		}
		String host = data.ip.trim().toLowerCase(Locale.ROOT);
		int colon = host.indexOf(':');
		if (colon >= 0) {
			host = host.substring(0, colon);
		}
		for (Network network : Network.values()) {
			for (String suffix : network.hosts()) {
				if (host.equals(suffix) || host.endsWith("." + suffix)) {
					return network;
				}
			}
		}
		return null;
	}

	/**
	 * Whether features owned by {@code network} may run right now. {@code null} is always active
	 * (ungated). Honours the global restrict toggle and any dev override.
	 */
	public static boolean isActive(Network network) {
		if (network == null) {
			return true;
		}
		if (!SharedConfig.get().restrictFeaturesToServer) {
			return true;
		}
		if (overrideActive) {
			return override == network;
		}
		return detect() == network;
	}

	/** The network detected from the connection, ignoring the toggle/override. May be {@code null}. */
	public static Network detected() {
		return detect();
	}

	/** The dev override target, or {@code null} if none is set. */
	public static Network override() {
		return overrideActive ? override : null;
	}

	/** Forces {@code network} as active for testing (e.g. off-server). {@code null} forces "no network". */
	public static void setOverride(Network network) {
		override = network;
		overrideActive = true;
	}

	/** Clears any dev override, restoring connection-based detection. */
	public static void clearOverride() {
		override = null;
		overrideActive = false;
	}

	/** Whether a dev override is currently in effect. */
	public static boolean hasOverride() {
		return overrideActive;
	}
}
