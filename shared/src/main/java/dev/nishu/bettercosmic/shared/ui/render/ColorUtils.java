package dev.nishu.bettercosmic.shared.ui.render;

/**
 * Pure color math for the config UI: HSV↔RGB conversion, hex parse/format, ARGB packing, blending.
 *
 * <p>Ported verbatim from BetterPrisons (it depends on no Minecraft types, so it needs no mapping
 * translation). Colors are packed {@code 0xAARRGGBB} ints throughout, matching {@code GuiGraphics}.
 */
public final class ColorUtils {

	private ColorUtils() {}

	/**
	 * Converts HSV to an opaque {@code 0xFFRRGGBB} color.
	 *
	 * @param hue        hue in degrees, 0–360
	 * @param saturation saturation, 0–1
	 * @param value      value/brightness, 0–1
	 */
	public static int hsvToRgb(float hue, float saturation, float value) {
		int h = (int) (hue / 60);
		float f = hue / 60 - h;
		float p = value * (1 - saturation);
		float q = value * (1 - f * saturation);
		float t = value * (1 - (1 - f) * saturation);

		float r, g, b;
		switch (h % 6) {
			case 0 -> { r = value; g = t;     b = p;     }
			case 1 -> { r = q;     g = value; b = p;     }
			case 2 -> { r = p;     g = value; b = t;     }
			case 3 -> { r = p;     g = q;     b = value; }
			case 4 -> { r = t;     g = p;     b = value; }
			default -> { r = value; g = p;    b = q;     }
		}

		int ri = (int) (r * 255);
		int gi = (int) (g * 255);
		int bi = (int) (b * 255);
		return 0xFF000000 | (ri << 16) | (gi << 8) | bi;
	}

	/**
	 * Converts an RGB color to HSV.
	 *
	 * @param rgb packed {@code 0xAARRGGBB} (alpha ignored)
	 * @return {@code {hue 0–360, saturation 0–1, value 0–1}}
	 */
	public static float[] rgbToHsv(int rgb) {
		int r = (rgb >> 16) & 0xFF;
		int g = (rgb >> 8) & 0xFF;
		int b = rgb & 0xFF;

		float rf = r / 255.0f;
		float gf = g / 255.0f;
		float bf = b / 255.0f;

		float max = Math.max(rf, Math.max(gf, bf));
		float min = Math.min(rf, Math.min(gf, bf));
		float delta = max - min;

		float hue = 0;
		if (delta > 0) {
			if (max == rf) {
				hue = 60 * (((gf - bf) / delta) % 6);
			} else if (max == gf) {
				hue = 60 * (((bf - rf) / delta) + 2);
			} else {
				hue = 60 * (((rf - gf) / delta) + 4);
			}
		}
		if (hue < 0) {
			hue += 360;
		}

		float saturation = max == 0 ? 0 : delta / max;
		float value = max;
		return new float[] { hue, saturation, value };
	}

	/**
	 * Parses a hex color string into a packed color. Accepts {@code RRGGBB}, {@code #RRGGBB},
	 * {@code AARRGGBB}, {@code #AARRGGBB}; returns opaque black on anything unparseable.
	 */
	public static int parseHex(String hex) {
		hex = hex.trim();
		if (hex.startsWith("#")) {
			hex = hex.substring(1);
		}
		try {
			if (hex.length() == 6) {
				return 0xFF000000 | Integer.parseInt(hex, 16);
			} else if (hex.length() == 8) {
				return (int) Long.parseLong(hex, 16);
			}
		} catch (NumberFormatException e) {
			return 0xFF000000;
		}
		return 0xFF000000;
	}

	/** Formats a color as an uppercase hex string, with or without the alpha byte. */
	public static String toHex(int rgb, boolean includeAlpha) {
		return includeAlpha ? String.format("%08X", rgb) : String.format("%06X", rgb & 0xFFFFFF);
	}

	/** Packs an {@code 0xAARRGGBB} color from 0–255 components. */
	public static int argb(int alpha, int red, int green, int blue) {
		return ((alpha & 0xFF) << 24) | ((red & 0xFF) << 16) | ((green & 0xFF) << 8) | (blue & 0xFF);
	}

	/** Packs an opaque {@code 0xFFRRGGBB} color from 0–255 components. */
	public static int rgb(int red, int green, int blue) {
		return argb(255, red, green, blue);
	}

	/**
	 * Linearly blends two packed colors, component-wise including alpha.
	 *
	 * @param ratio 0 → {@code color1}, 1 → {@code color2} (clamped)
	 */
	public static int blend(int color1, int color2, float ratio) {
		ratio = Math.max(0, Math.min(1, ratio));

		int a1 = (color1 >> 24) & 0xFF, r1 = (color1 >> 16) & 0xFF, g1 = (color1 >> 8) & 0xFF, b1 = color1 & 0xFF;
		int a2 = (color2 >> 24) & 0xFF, r2 = (color2 >> 16) & 0xFF, g2 = (color2 >> 8) & 0xFF, b2 = color2 & 0xFF;

		int a = (int) (a1 + (a2 - a1) * ratio);
		int r = (int) (r1 + (r2 - r1) * ratio);
		int g = (int) (g1 + (g2 - g1) * ratio);
		int b = (int) (b1 + (b2 - b1) * ratio);
		return (a << 24) | (r << 16) | (g << 8) | b;
	}

	/** Replaces a color's alpha byte (0–255), keeping RGB. */
	public static int withAlpha(int color, int alpha) {
		return (color & 0x00FFFFFF) | ((alpha & 0xFF) << 24);
	}

	/** Scales a color's existing alpha by {@code factor} (0–1). */
	public static int scaleAlpha(int color, float factor) {
		int a = (int) (((color >> 24) & 0xFF) * Math.max(0, Math.min(1, factor)));
		return (color & 0x00FFFFFF) | (a << 24);
	}

	public static int alpha(int color) { return (color >> 24) & 0xFF; }

	public static int red(int color)   { return (color >> 16) & 0xFF; }

	public static int green(int color) { return (color >> 8) & 0xFF; }

	public static int blue(int color)  { return color & 0xFF; }
}
