package dev.nishu.bettercosmic.shared.ui.widget;

import dev.nishu.bettercosmic.shared.ui.core.Theme;
import dev.nishu.bettercosmic.shared.ui.core.UiElement;
import dev.nishu.bettercosmic.shared.ui.model.Option;
import dev.nishu.bettercosmic.shared.ui.render.ColorUtils;
import dev.nishu.bettercosmic.shared.ui.render.RenderUtils;
import net.minecraft.client.gui.GuiGraphics;
import org.lwjgl.glfw.GLFW;

/**
 * A compact HSV color picker, ported from BetterPrisons and modernized: a saturation-value square, a
 * thin hue bar, a live hex field, and OK/Cancel. Floats in the screen's {@code OverlayLayer} above
 * everything.
 *
 * <p>Kept from BP: the SV-square cache (regenerated only when hue changes) and hue-bar cache with
 * block rendering for perf, drag on both, and typed hex. Changed: all the {@code System.out} debug
 * logging is gone; the compact sky styling (120px square, 8px hue bar, 1px borders); and it
 * <b>live-previews</b> — every change writes the bound {@link Option} immediately (so editing a theme
 * color repaints the whole UI, including this picker), and <b>Cancel</b>/Esc/click-away reverts to
 * the color the picker opened with. The option's original alpha byte is preserved on every write.
 */
public final class ColorPickerPopup extends UiElement {

	private static final int PAD = 8;
	private static final int SV = 120;
	private static final int HUE_W = 8;
	private static final int PIXEL = 4; // block size for cached gradient rendering
	private static final int PREVIEW_H = 14;
	private static final int FIELD_H = 14;
	private static final int BTN_H = 14;

	private final Option<Integer> option;
	private final int originalColor;
	private final int originalAlpha;
	private final Runnable onClose;

	private final int px, py, popupW, popupH;
	private final int svX, svY, hueX, hueY, previewY, hexY, btnY;
	private final int okX, cancelX, btnW;

	private float hue, saturation, value;
	private String hexInput;
	private boolean editingHex;
	private boolean draggingSV, draggingHue;

	// caches (regenerate SV only on hue change; hue bar once)
	private float cachedHue = -1;
	private int[][] svCache;
	private int[] hueCache;

	public ColorPickerPopup(Option<Integer> option, int screenW, int screenH, Runnable onClose) {
		this.option = option;
		this.originalColor = option.get();
		this.originalAlpha = (originalColor >>> 24) & 0xFF;
		this.onClose = onClose;

		float[] hsv = ColorUtils.rgbToHsv(originalColor & 0xFFFFFF);
		this.hue = hsv[0];
		this.saturation = hsv[1];
		this.value = hsv[2];
		this.hexInput = ColorUtils.toHex(originalColor, false);

		int contentW = SV + 6 + HUE_W;
		this.popupW = PAD + contentW + PAD;
		this.popupH = PAD + 12 + SV + 8 + PREVIEW_H + 8 + FIELD_H + 8 + BTN_H + PAD;
		this.px = (screenW - popupW) / 2;
		this.py = (screenH - popupH) / 2;

		int contentX = px + PAD;
		int contentY = py + PAD + 12;
		this.svX = contentX;
		this.svY = contentY;
		this.hueX = contentX + SV + 6;
		this.hueY = contentY;
		this.previewY = contentY + SV + 8;
		this.hexY = previewY + PREVIEW_H + 8;
		this.btnY = hexY + FIELD_H + 8;
		this.btnW = (contentW - 6) / 2;
		this.okX = contentX;
		this.cancelX = contentX + btnW + 6;

		bounds(px, py, popupW, popupH);
	}

	// ---- live preview / commit ----

	private int composed() {
		int rgb = ColorUtils.hsvToRgb(hue, saturation, value) & 0xFFFFFF;
		return (originalAlpha << 24) | rgb;
	}

	/** Push the current HSV to the bound option (persists + any side effects, e.g. theme repaint). */
	private void apply() {
		option.set(composed());
		if (!editingHex) {
			hexInput = ColorUtils.toHex(option.get(), false);
		}
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float dt) {
		int contentW = SV + 6 + HUE_W;

		RenderUtils.rect(g, px, py, popupW, popupH, ColorUtils.withAlpha(Theme.surfaceHover, 0xFF));
		RenderUtils.outline(g, px, py, popupW, popupH, Theme.accent);
		RenderUtils.text(g, "Pick color", px + PAD, py + PAD, Theme.text);

		renderSV(g);
		renderHue(g);

		// preview
		RenderUtils.rect(g, svX, previewY, contentW, PREVIEW_H, 0xFF000000 | (composed() & 0xFFFFFF));
		RenderUtils.outline(g, svX, previewY, contentW, PREVIEW_H, Theme.line);

		// hex field
		int fieldBg = editingHex ? Theme.surface : ColorUtils.withAlpha(Theme.ground, 0xFF);
		RenderUtils.rect(g, svX, hexY, contentW, FIELD_H, fieldBg);
		RenderUtils.outline(g, svX, hexY, contentW, FIELD_H, editingHex ? Theme.accent : Theme.line);
		boolean caretOn = editingHex && System.currentTimeMillis() % 1000 < 500;
		RenderUtils.text(g, "#" + hexInput + (caretOn ? "_" : ""), svX + 4, hexY + 3, Theme.text);

		// buttons
		boolean okHover = hit(mouseX, mouseY, okX, btnY, btnW, BTN_H);
		RenderUtils.rect(g, okX, btnY, btnW, BTN_H, okHover ? ColorUtils.withAlpha(Theme.accent, 0x55) : Theme.surface);
		RenderUtils.outline(g, okX, btnY, btnW, BTN_H, okHover ? Theme.accent : Theme.line);
		RenderUtils.textCentered(g, "OK", okX + btnW / 2, btnY + 3, Theme.text);

		boolean cxHover = hit(mouseX, mouseY, cancelX, btnY, btnW, BTN_H);
		RenderUtils.rect(g, cancelX, btnY, btnW, BTN_H, cxHover ? Theme.surfaceHover : Theme.surface);
		RenderUtils.outline(g, cancelX, btnY, btnW, BTN_H, cxHover ? Theme.accent : Theme.line);
		RenderUtils.textCentered(g, "Cancel", cancelX + btnW / 2, btnY + 3, cxHover ? Theme.text : Theme.muted);
	}

	private void renderSV(GuiGraphics g) {
		int n = SV / PIXEL;
		if (svCache == null || Math.abs(cachedHue - hue) > 0.1f) {
			cachedHue = hue;
			svCache = new int[n][n];
			for (int row = 0; row < n; row++) {
				float v = 1.0f - (float) row / n;
				for (int col = 0; col < n; col++) {
					svCache[row][col] = ColorUtils.hsvToRgb(hue, (float) col / n, v);
				}
			}
		}
		for (int row = 0; row < n; row++) {
			for (int col = 0; col < n; col++) {
				RenderUtils.rect(g, svX + col * PIXEL, svY + row * PIXEL, PIXEL, PIXEL, svCache[row][col]);
			}
		}
		RenderUtils.outline(g, svX, svY, SV, SV, Theme.line);

		int selX = svX + (int) (saturation * SV);
		int selY = svY + (int) ((1.0f - value) * SV);
		RenderUtils.outline(g, selX - 3, selY - 3, 6, 6, 0xFFFFFFFF);
		RenderUtils.outline(g, selX - 2, selY - 2, 4, 4, 0xFF000000);
	}

	private void renderHue(GuiGraphics g) {
		int n = SV / PIXEL;
		if (hueCache == null) {
			hueCache = new int[n];
			for (int row = 0; row < n; row++) {
				hueCache[row] = ColorUtils.hsvToRgb((float) row / n * 360, 1, 1);
			}
		}
		for (int row = 0; row < n; row++) {
			RenderUtils.rect(g, hueX, hueY + row * PIXEL, HUE_W, PIXEL, hueCache[row]);
		}
		RenderUtils.outline(g, hueX, hueY, HUE_W, SV, Theme.line);

		int selY = hueY + (int) (hue / 360 * SV);
		RenderUtils.rect(g, hueX - 2, selY - 1, HUE_W + 4, 2, 0xFFFFFFFF);
	}

	// ---- input ----

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (button != 0) {
			return true;
		}
		if (hit(mouseX, mouseY, svX, svY, SV, SV)) {
			draggingSV = true;
			updateSV(mouseX, mouseY);
			return true;
		}
		if (hit(mouseX, mouseY, hueX, hueY, HUE_W, SV)) {
			draggingHue = true;
			updateHue(mouseY);
			return true;
		}
		editingHex = hit(mouseX, mouseY, svX, hexY, SV + 6 + HUE_W, FIELD_H);
		if (editingHex) {
			return true;
		}
		if (hit(mouseX, mouseY, okX, btnY, btnW, BTN_H)) {
			onClose.run(); // value already live
			return true;
		}
		if (hit(mouseX, mouseY, cancelX, btnY, btnW, BTN_H) || !isMouseOver(mouseX, mouseY)) {
			option.set(originalColor); // revert
			onClose.run();
			return true;
		}
		return true; // modal
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		draggingSV = false;
		draggingHue = false;
		return true;
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
		if (draggingSV) {
			updateSV(mouseX, mouseY);
			return true;
		}
		if (draggingHue) {
			updateHue(mouseY);
			return true;
		}
		return true;
	}

	@Override
	public boolean charTyped(char chr, int modifiers) {
		if (editingHex && hexInput.length() < 6
			&& (Character.isDigit(chr) || (chr >= 'a' && chr <= 'f') || (chr >= 'A' && chr <= 'F'))) {
			hexInput += Character.toUpperCase(chr);
			commitHex();
			return true;
		}
		return true; // modal
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (editingHex) {
			if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !hexInput.isEmpty()) {
				hexInput = hexInput.substring(0, hexInput.length() - 1);
				commitHex();
				return true;
			}
			if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_ESCAPE) {
				editingHex = false;
				return true;
			}
		} else if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
			option.set(originalColor);
			onClose.run();
			return true;
		}
		return true; // modal
	}

	private void updateSV(double mouseX, double mouseY) {
		saturation = clamp01((float) (mouseX - svX) / SV);
		value = clamp01(1.0f - (float) (mouseY - svY) / SV);
		apply();
	}

	private void updateHue(double mouseY) {
		hue = Math.max(0, Math.min(360, (float) (mouseY - hueY) / SV * 360));
		apply();
	}

	private void commitHex() {
		if (hexInput.length() == 6) {
			int rgb = ColorUtils.parseHex(hexInput) & 0xFFFFFF;
			float[] hsv = ColorUtils.rgbToHsv(rgb);
			hue = hsv[0];
			saturation = hsv[1];
			value = hsv[2];
			option.set((originalAlpha << 24) | rgb);
		}
	}

	private static float clamp01(float v) {
		return Math.max(0, Math.min(1, v));
	}

	private static boolean hit(double mx, double my, int x, int y, int w, int h) {
		return mx >= x && mx < x + w && my >= y && my < y + h;
	}
}
