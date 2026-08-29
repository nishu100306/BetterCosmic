package dev.nishu.bettercosmic.shared.server;

/**
 * A Cosmic network that a consuming mod scopes its features to. Each carries the host suffix(es) that
 * identify it in the multiplayer server address; {@link ServerContext} matches the connected server
 * against these to decide which mod's features may run.
 *
 * <p>Matching is suffix-based (see {@link ServerContext}), so region/lobby subdomains like
 * {@code play.cosmicprisons.com} or {@code us.cosmicsky.net} all resolve to the right network.
 */
public enum Network {

	PRISONS("Prisons", "cosmicprisons.com"),
	SKY("Sky", "cosmicsky.net");

	private final String displayName;
	private final String[] hosts;

	Network(String displayName, String... hosts) {
		this.displayName = displayName;
		this.hosts = hosts;
	}

	/** Human-readable name for the config UI (e.g. "Prisons"). */
	public String displayName() {
		return displayName;
	}

	/** The host suffixes that identify this network (lower-case, no port). */
	public String[] hosts() {
		return hosts;
	}
}
