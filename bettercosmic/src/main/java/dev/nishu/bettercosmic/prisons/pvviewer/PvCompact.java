package dev.nishu.bettercosmic.prisons.pvviewer;

import dev.nishu.bettercosmic.prisons.client.BetterPrisonsClient;

/**
 * Shared "compact mode" scale for the PV preview sidebars, driven by {@code pvCompactScalePercent}.
 * Lowering it shrinks the {@link PvSidebar} previews so more vaults fit on screen; it does <b>not</b>
 * touch the vanilla vault window or inventory. A percent of {@code 100} means off (previews render at
 * full vanilla-slot size).
 */
public final class PvCompact {

	/** Clamp bounds so preview layout stays sane. */
	private static final float MIN_SCALE = 0.5f;
	private static final float MAX_SCALE = 1.0f;

	private PvCompact() {}

	/** The configured preview scale in [0.5, 1.0], or 1.0 when off / config missing. */
	public static float scale() {
		if (BetterPrisonsClient.config == null) {
			return 1.0f;
		}
		float s = BetterPrisonsClient.config.pvCompactScalePercent / 100.0f;
		return Math.max(MIN_SCALE, Math.min(MAX_SCALE, s));
	}
}
