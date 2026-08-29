package dev.nishu.bettercosmic.prisons;

import dev.nishu.bettercosmic.shared.server.Network;
import dev.nishu.bettercosmic.shared.server.ServerContext;

/**
 * Convenience gate for BetterPrisons feature code: {@code active()} is true only while the client is
 * on the Cosmic Prisons network (or a dev override forces it, or the global restrict toggle is off).
 * Feature callbacks and mixins early-return on {@code !active()} so nothing runs off-server except the
 * shared config UI.
 */
public final class PrisonsGate {

	private PrisonsGate() {}

	/** Whether BetterPrisons features may run right now. */
	public static boolean active() {
		return ServerContext.isActive(Network.PRISONS);
	}
}
