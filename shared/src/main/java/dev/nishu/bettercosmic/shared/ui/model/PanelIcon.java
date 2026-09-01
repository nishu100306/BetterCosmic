package dev.nishu.bettercosmic.shared.ui.model;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.gui.GuiGraphics;

import java.io.IOException;
import java.io.InputStream;

/**
 * The small glyph shown on a {@link ConfigPanel} card. Each value maps to a 16×16 PNG under
 * {@code assets/bettercosmicshared/textures/gui/icons/}. The PNG is a white-on-transparent mask: the
 * alpha channel is the shape, which is tinted to the passed {@code color} (so icons follow the profile
 * accent) and nearest-neighbor scaled into the {@code size}×{@code size} box at ({@code x},{@code y}).
 *
 * <p>Pixels are read once (lazily, on first draw) via {@link NativeImage} and cached; drawing is the
 * same per-pixel fill the framework already uses, so there is no atlas or blit-scaling to manage. To
 * change an icon, replace its PNG file — no code change needed.
 */
public enum PanelIcon {
	FLASK("flask"),
	LOCK("lock"),
	EYE("eye"),
	SATCHEL("satchel"),
	CHART("chart"),
	SPARKLE("sparkle"),
	POTION("splash_potion"),
	BUNDLE("bundle"),
	BEACON("beacon"),
	METEOR("meteor"),
	MAGNIFIER("magnifier"),
	PICKAXE("pickaxe"),
	SWORD("sword"),
	CLOCK("clock"),
	BELL("bell"),
	MARKER("marker"),
	BUBBLE("bubble"),
	SLIDERS("sliders"),
	GEAR("gear"),
	BOOK("book"),
	EXCLAMATION("exclamation");

	private static final String DIR = "/assets/bettercosmicshared/textures/gui/icons/";

	private final String file;
	private int[] argb;   // row-major pixels; only the alpha channel is used (tint mask)
	private int w, h;
	private boolean loaded;

	PanelIcon(String file) {
		this.file = file;
	}

	/** Draws the icon tinted to {@code color}, nearest-neighbor scaled into the size×size box. */
	public void draw(GuiGraphics g, int x, int y, int size, int color) {
		if (!loaded) {
			load();
		}
		if (argb == null || w == 0 || h == 0) {
			return;
		}
		for (int dy = 0; dy < size; dy++) {
			int sy = dy * h / size;
			for (int dx = 0; dx < size; dx++) {
				int sx = dx * w / size;
				if ((argb[sy * w + sx] >>> 24) >= 128) {
					g.fill(x + dx, y + dy, x + dx + 1, y + dy + 1, color);
				}
			}
		}
	}

	/** Reads the PNG's pixels once and caches them; leaves the icon empty (drawn as nothing) on failure. */
	private void load() {
		loaded = true;
		try (InputStream in = PanelIcon.class.getResourceAsStream(DIR + file + ".png")) {
			if (in == null) {
				return;
			}
			try (NativeImage img = NativeImage.read(in)) {
				w = img.getWidth();
				h = img.getHeight();
				argb = new int[w * h];
				for (int yy = 0; yy < h; yy++) {
					for (int xx = 0; xx < w; xx++) {
						// NativeImage packs ABGR; only the alpha (top byte) is read as the tint mask.
						argb[yy * w + xx] = img.getPixel(xx, yy);
					}
				}
			}
		} catch (IOException e) {
			argb = null;
		}
	}
}
