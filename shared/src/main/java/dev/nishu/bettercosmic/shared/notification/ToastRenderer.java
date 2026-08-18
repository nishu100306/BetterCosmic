package dev.nishu.bettercosmic.shared.notification;

import dev.nishu.bettercosmic.shared.ui.core.Theme;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Supplier;

/**
 * A lightweight custom toast system: small notification cards that slide in from a screen corner,
 * stack along that edge, linger, then slide back out. Each card is aligned to its corner — right
 * corners are right-aligned and slide from the right; bottom corners stack upward from the bottom.
 *
 * <p>Ported from BetterPrisons (Yarn → Mojang) and made content-agnostic: the corner is supplied by
 * the consuming mod via {@link #setCornerSupplier} (BetterPrisons feeds its {@code toastCorner}
 * setting), and the card colors come from the shared {@link Theme}. All access is on the
 * render/client thread, so no synchronization is required.
 */
public final class ToastRenderer {

	private static final long SLIDE_MS = 300;         // slide-in / slide-out duration
	private static final long DEFAULT_DURATION = 4000; // total on-screen lifetime (incl. slides)
	private static final int EDGE_MARGIN = 8;
	private static final int GAP = 4;
	private static final int PAD = 6;
	private static final int LINE_H = 10;
	private static final int ICON = 16;
	private static final int MIN_W = 70;
	private static final int MAX_W = 240;
	private static final int MAX_TOASTS = 6;

	private static final List<ToastEntry> active = new ArrayList<>();

	/** Which corner toasts appear in; one of "Top Left", "Top Right", "Bottom Left", "Bottom Right". */
	private static Supplier<String> cornerSupplier = () -> "Bottom Right";

	private ToastRenderer() {}

	private static final class ToastEntry {
		final Component title;
		final Component description; // may be null
		final ItemStack icon;       // may be null
		final long spawnTime;
		final long durationMs;

		ToastEntry(Component title, Component description, ItemStack icon, long durationMs) {
			this.title = title;
			this.description = description;
			this.icon = icon;
			this.spawnTime = System.currentTimeMillis();
			this.durationMs = durationMs;
		}
	}

	/** Registers the HUD render hook. Call once from a shared/mod client init. */
	public static void register() {
		HudRenderCallback.EVENT.register((ctx, tickCounter) -> render(ctx));
	}

	/** Sets the corner source (e.g. a config getter). {@code null} restores the default corner. */
	public static void setCornerSupplier(Supplier<String> supplier) {
		cornerSupplier = supplier != null ? supplier : () -> "Bottom Right";
	}

	public static void show(Component title, Component description) {
		show(title, description, null, DEFAULT_DURATION);
	}

	public static void show(Component title, Component description, ItemStack icon, long durationMs) {
		if (title == null) {
			return;
		}
		if (active.size() >= MAX_TOASTS) {
			active.remove(0);
		}
		active.add(new ToastEntry(title, description, icon, durationMs));
	}

	private static void render(GuiGraphics ctx) {
		if (active.isEmpty()) {
			return;
		}

		Minecraft client = Minecraft.getInstance();
		if (client.options.hideGui) {
			return;
		}

		String corner = cornerSupplier.get();
		if (corner == null) {
			corner = "Bottom Right";
		}
		corner = corner.toLowerCase();
		boolean right = corner.contains("right");
		boolean bottom = corner.contains("bottom");

		Font font = client.font;
		int screenW = ctx.guiWidth();
		int screenH = ctx.guiHeight();
		long now = System.currentTimeMillis();

		// Running edge cursor: top corners grow down from the top, bottom corners up from the bottom.
		int cursor = bottom ? screenH - EDGE_MARGIN : EDGE_MARGIN;

		Iterator<ToastEntry> it = active.iterator();
		while (it.hasNext()) {
			ToastEntry t = it.next();
			long elapsed = now - t.spawnTime;
			if (elapsed >= t.durationMs) {
				it.remove();
				continue;
			}

			int boxW = boxWidth(font, t);
			int boxH = boxHeight(t);

			float slideIn = Math.min(1f, elapsed / (float) SLIDE_MS);
			float slideOut = elapsed > t.durationMs - SLIDE_MS
					? (t.durationMs - elapsed) / (float) SLIDE_MS : 1f;
			float visible = Math.max(0f, Math.min(slideIn, slideOut));
			int slide = (int) ((1f - visible) * (boxW + EDGE_MARGIN));

			int restX = right ? (screenW - EDGE_MARGIN - boxW) : EDGE_MARGIN;
			int x = right ? restX + slide : restX - slide;
			int y = bottom ? cursor - boxH : cursor;

			drawToast(ctx, font, t, x, y, boxW, boxH, right);

			cursor = bottom ? (y - GAP) : (y + boxH + GAP);
		}
	}

	private static int boxWidth(Font font, ToastEntry t) {
		int iconSpace = t.icon != null ? ICON + 4 : 0;
		int descW = t.description != null ? font.width(t.description) : 0;
		int contentW = Math.max(font.width(t.title), descW);
		return clamp(PAD + iconSpace + contentW + PAD, MIN_W, MAX_W);
	}

	private static int boxHeight(ToastEntry t) {
		int textLines = t.description != null ? 2 : 1;
		int contentH = Math.max(t.icon != null ? ICON : 0, textLines * LINE_H);
		return contentH + PAD * 2;
	}

	/** Draws a single toast card, aligned left or right within its box. */
	private static void drawToast(GuiGraphics ctx, Font font, ToastEntry t,
								  int x, int y, int boxW, int boxH, boolean right) {
		ctx.fill(x, y, x + boxW, y + boxH, Theme.surface);
		ctx.fill(x, y, x + boxW, y + 1, Theme.line);
		ctx.fill(x, y + boxH - 1, x + boxW, y + boxH, Theme.line);
		ctx.fill(x, y, x + 1, y + boxH, Theme.line);
		ctx.fill(x + boxW - 1, y, x + boxW, y + boxH, Theme.line);

		int iconSpace = t.icon != null ? ICON + 4 : 0;
		if (t.icon != null) {
			int iconX = right ? x + boxW - PAD - ICON : x + PAD;
			ctx.renderItem(t.icon, iconX, y + (boxH - ICON) / 2);
		}

		int textLines = t.description != null ? 2 : 1;
		int ty = y + (boxH - textLines * LINE_H) / 2;

		if (right) {
			int rightEdge = x + boxW - PAD - iconSpace;
			ctx.drawString(font, t.title, rightEdge - font.width(t.title), ty, Theme.text, true);
			if (t.description != null) {
				ctx.drawString(font, t.description,
						rightEdge - font.width(t.description), ty + LINE_H, Theme.muted, true);
			}
		} else {
			int leftEdge = x + PAD + iconSpace;
			ctx.drawString(font, t.title, leftEdge, ty, Theme.text, true);
			if (t.description != null) {
				ctx.drawString(font, t.description, leftEdge, ty + LINE_H, Theme.muted, true);
			}
		}
	}

	private static int clamp(int v, int min, int max) {
		return Math.max(min, Math.min(max, v));
	}
}
