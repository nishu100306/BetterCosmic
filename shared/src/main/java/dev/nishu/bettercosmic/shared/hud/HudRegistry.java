package dev.nishu.bettercosmic.shared.hud;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The set of {@link BaseHud}s the shared {@link HudRenderer} draws and the {@link HudEditorScreen}
 * edits. Each mod registers its HUD instances at client init, pairing each with a {@code persist}
 * callback the editor invokes after a move/scale so the mod writes the new position back to its own
 * config.
 *
 * <p>Replaces BetterPrisons' hardcoded HUD list — the shared code no longer knows any specific HUD.
 */
public final class HudRegistry {

	/** One registered HUD: the instance, how to persist its position, and whether it's draggable. */
	public static final class Entry {
		public final BaseHud hud;
		public final Runnable persist;
		public final boolean draggable;

		Entry(BaseHud hud, Runnable persist, boolean draggable) {
			this.hud = hud;
			this.persist = persist == null ? () -> {} : persist;
			this.draggable = draggable;
		}
	}

	private static final List<Entry> ENTRIES = new ArrayList<>();

	private HudRegistry() {}

	/** Registers a draggable HUD with a callback that saves its position/scale to config. */
	public static void register(BaseHud hud, Runnable persist) {
		register(hud, persist, true);
	}

	/**
	 * Registers a HUD.
	 *
	 * @param draggable whether the HUD editor can move/scale it (false for crosshair-anchored HUDs
	 *                  like a centered aura that shouldn't be dragged)
	 */
	public static void register(BaseHud hud, Runnable persist, boolean draggable) {
		ENTRIES.add(new Entry(hud, persist, draggable));
	}

	/** All registered entries, in registration order (unmodifiable). */
	public static List<Entry> entries() {
		return Collections.unmodifiableList(ENTRIES);
	}

	/** Whether any HUDs are registered (drives showing the HUD-editor button in the config screen). */
	public static boolean isEmpty() {
		return ENTRIES.isEmpty();
	}

	/** Test/util hook. */
	public static void clear() {
		ENTRIES.clear();
	}
}
