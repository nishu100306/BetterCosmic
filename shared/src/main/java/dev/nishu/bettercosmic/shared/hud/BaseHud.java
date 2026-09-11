package dev.nishu.bettercosmic.shared.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Base class for a movable, scalable on-screen HUD element. Each mod subclasses this for its own
 * HUDs (satchel, stats, ...) and registers instances with {@link HudRegistry}; the shared
 * {@link HudRenderer} draws every enabled HUD and the shared {@link HudEditorScreen} repositions and
 * scales them.
 *
 * <p>Ported and de-hardcoded from BetterPrisons' {@code hud/BaseHud} (Yarn → Mojang). Position/scale
 * are live fields; the mod loads them from its config at construction and writes them back via the
 * {@code persist} callback it supplies to {@link HudRegistry#register}. The {@code default*} fields
 * are the "Reset Positions" targets.
 */
public abstract class BaseHud {

	public String id;
	public boolean enabled = true;

	/** Top-left position, in GUI-scaled pixels. */
	public int x = 10;
	public int y = 10;

	/** Render scale (1.0 = 100%). */
	public float scale = 1.0f;

	/** Defaults restored by the HUD editor's "Reset Positions". Set these when constructing. */
	public int defaultX = 10;
	public int defaultY = 10;
	public float defaultScale = 1.0f;

	public BaseHud(String id) {
		this.id = id;
	}

	/** Per-tick update hook (no client). */
	public void tick() {
	}

	/** Per-tick update hook with the client. */
	public void tick(Minecraft client) {
	}

	/** Draws the HUD at its current position/scale. */
	public abstract void render(GuiGraphics context, Minecraft client);

	/** Rendered width in GUI pixels, for the editor's bounding box (0 = use a default). */
	public int getWidth() {
		return 0;
	}

	/** Rendered height in GUI pixels, for the editor's bounding box (0 = use a default). */
	public int getHeight() {
		return 0;
	}

	/** Restores position and scale to the configured defaults. */
	public void resetToDefault() {
		this.x = defaultX;
		this.y = defaultY;
		this.scale = defaultScale;
	}

	/**
	 * Screen-clamped render X. Some HUDs grow with their content; when a HUD would extend past the
	 * screen edge this shifts where it draws so it stays fully on-screen, <b>without</b> changing the
	 * saved {@link #x} (its anchor) — so once the content shrinks again it returns to the anchor. When
	 * the width is unknown ({@link #getWidth()} returns 0) the anchor is used unchanged.
	 */
	public int clampedX(int screenWidth) {
		int w = getWidth();
		if (w <= 0) {
			return x;
		}
		return Math.max(0, Math.min(x, screenWidth - w));
	}

	/** Screen-clamped render Y; see {@link #clampedX(int)}. */
	public int clampedY(int screenHeight) {
		int h = getHeight();
		if (h <= 0) {
			return y;
		}
		return Math.max(0, Math.min(y, screenHeight - h));
	}

	// Helpers for subclasses to scale widths/heights/offsets in render().
	protected int scaled(int value) {
		return (int) (value * scale);
	}

	protected float scaled(float value) {
		return value * scale;
	}
}
