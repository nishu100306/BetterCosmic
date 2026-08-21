package dev.nishu.bettercosmic.prisons.enchantprocs;

import dev.nishu.bettercosmic.shared.render.FloatingTextRenderer;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central dispatch hub for Cosmic Prisons enchant procs. The API layer parses a raw hook into an
 * {@link EnchantProc} and calls {@link #handle(EnchantProc)}; this routes it to the handler registered
 * for that enchant id, or the default handler (a floating world-space label via the shared
 * {@link FloatingTextRenderer}). Adding bespoke behaviour for one enchant is a single {@link #register}
 * call in {@link #init()} — no API-layer changes. Ported from BetterPrisons' {@code EnchantProcManager}.
 */
public final class EnchantProcManager {

	/** How long a floating proc label lives, in milliseconds. */
	private static final long DISPLAY_MS = 1500L;

	/**
	 * The only enchants that are displayed; every other proc is ignored. Stored normalised (lowercase,
	 * alphanumeric only) so the id format the server sends — {@code perfect_strike}, {@code perfectStrike},
	 * {@code "Perfect Strike"} — all match the same entry.
	 */
	private static final Set<String> DISPLAYED_ENCHANTS = Set.of(
			"daze", "blaze", "electrocution", "poison", "scorch", "bleed", "pummel", "berserk",
			"lightning", "perfectstrike", "trap", "weakness", "silence", "thousandcuts", "fling",
			"obliterate", "lifesteal");

	private static final Map<String, EnchantProcHandler> handlers = new ConcurrentHashMap<>();

	/** Default presentation: a rising floating world-space label anchored on the player's target. */
	private static final EnchantProcHandler DEFAULT_HANDLER =
			proc -> FloatingTextRenderer.spawn(proc.displayName(), DISPLAY_MS);

	private EnchantProcManager() {}

	/** Registers built-in per-enchant overrides. Called once from client init. */
	public static void init() {
		// No per-enchant overrides yet — every enchant uses the default floating-text display.
		// To customise one enchant: register("enchant_id", proc -> { ... });
	}

	/** Registers a custom handler for a specific enchant id, replacing any prior registration. */
	public static void register(String id, EnchantProcHandler handler) {
		if (id == null || handler == null) {
			return;
		}
		handlers.put(id, handler);
	}

	/**
	 * Dispatches a proc to its registered handler, falling back to the default display. Procs for
	 * enchants outside {@link #DISPLAYED_ENCHANTS} are ignored.
	 */
	public static void handle(EnchantProc proc) {
		if (proc == null || proc.id() == null) {
			return;
		}
		if (!DISPLAYED_ENCHANTS.contains(normalize(proc.id()))) {
			return;
		}
		handlers.getOrDefault(proc.id(), DEFAULT_HANDLER).handle(proc);
	}

	/** Reduces an enchant id to lowercase alphanumerics so spelling/format variants compare equal. */
	private static String normalize(String id) {
		StringBuilder sb = new StringBuilder(id.length());
		for (int i = 0; i < id.length(); i++) {
			char c = Character.toLowerCase(id.charAt(i));
			if (c >= 'a' && c <= 'z' || c >= '0' && c <= '9') {
				sb.append(c);
			}
		}
		return sb.toString();
	}
}
