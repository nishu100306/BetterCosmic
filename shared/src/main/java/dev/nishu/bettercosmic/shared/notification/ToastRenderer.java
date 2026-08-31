package dev.nishu.bettercosmic.shared.notification;

import dev.nishu.bettercosmic.shared.ui.core.Theme;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
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
 * <p>Two flavors:
 * <ul>
 *   <li><b>Auto-dismiss</b> ({@link #show}) — fades out after its duration. No interaction.</li>
 *   <li><b>Button toast</b> ({@link #showButtons}) — with one or more {@link ToastButton}s; sticky by
 *       default (stays until a button is clicked). Its description may be a live {@link Supplier} so it
 *       can show changing state (e.g. download progress). The HUD can't take clicks while the cursor is
 *       grabbed, so toasts are also drawn over any open screen and clicks are routed via Fabric's
 *       {@link ScreenMouseEvents} — click a button with chat/any menu open to act + dismiss.</li>
 * </ul>
 *
 * <p>Ported from BetterPrisons (Yarn → Mojang) and made content-agnostic: the corner is supplied by
 * the consuming mod via {@link #setCornerSupplier}, and card colors come from the shared {@link Theme}.
 */
public final class ToastRenderer {

	private static final long SLIDE_MS = 300;          // slide-in / slide-out duration
	private static final long DEFAULT_DURATION = 4000; // auto-dismiss lifetime (incl. slides)
	private static final int EDGE_MARGIN = 8;
	private static final int GAP = 4;
	private static final int PAD = 6;
	private static final int LINE_H = 10;
	private static final int ICON = 16;
	private static final int MIN_W = 70;
	private static final int MAX_W = 240;
	private static final int MAX_TOASTS = 6;

	// Button metrics.
	private static final int BTN_H = 14;
	private static final int BTN_HPAD = 8; // horizontal padding each side of a button's label
	private static final int BTN_GAP = 4;

	private static final List<ToastEntry> active = new ArrayList<>();

	/** Which corner toasts appear in; one of "Top Left", "Top Right", "Bottom Left", "Bottom Right". */
	private static Supplier<String> cornerSupplier = () -> "Bottom Right";
	private static boolean registered = false;

	private ToastRenderer() {}

	/**
	 * A labeled button on a toast; {@code onClick} runs when clicked, then the toast is dismissed. A
	 * {@code secondary} button is drawn dimmer to de-emphasize it (e.g. a destructive "Disable").
	 */
	public record ToastButton(Component label, Runnable onClick, boolean secondary) {
		public static ToastButton primary(Component label, Runnable onClick) {
			return new ToastButton(label, onClick, false);
		}

		public static ToastButton secondary(Component label, Runnable onClick) {
			return new ToastButton(label, onClick, true);
		}
	}

	private static final class ToastEntry {
		final String key;                       // dedup key for keyed toasts; null otherwise
		final Component title;
		final Supplier<Component> description;   // resolved each frame; may be null
		final ItemStack icon;                    // may be null
		final long spawnTime;
		final long durationMs;                   // <= 0 means sticky (stays until a button is clicked)
		final List<ToastButton> buttons;         // empty for a plain toast

		// Absolute button hit-rects, recomputed each render (parallel to `buttons`).
		int[] bx, by, bw;

		ToastEntry(String key, Component title, Supplier<Component> description, ItemStack icon,
				   long durationMs, List<ToastButton> buttons) {
			this.key = key;
			this.title = title;
			this.description = description;
			this.icon = icon;
			this.spawnTime = System.currentTimeMillis();
			this.durationMs = durationMs;
			this.buttons = buttons;
		}

		boolean sticky() {
			return durationMs <= 0;
		}

		Component desc() {
			return description != null ? description.get() : null;
		}
	}

	/** Registers the HUD render, the over-screen render, and button-click handling. Idempotent. */
	public static void register() {
		if (registered) {
			return;
		}
		registered = true;

		// In-game with no screen: draw on the HUD (cursor grabbed, so buttons aren't clickable yet).
		HudRenderCallback.EVENT.register((ctx, tickCounter) -> {
			Minecraft client = Minecraft.getInstance();
			if (client.screen == null && !client.options.hideGui) {
				render(ctx, -1, -1);
			}
		});
		// Any screen open (cursor free): draw on top and accept clicks on toast buttons.
		ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
			ScreenEvents.afterRender(screen).register((scr, g, mx, my, dt) -> render(g, mx, my));
			ScreenMouseEvents.afterMouseClick(screen).register((scr, context, consumed) ->
					context.button() == 0 && handleClick(context.x(), context.y()));
		});
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
		add(new ToastEntry(null, title, fixed(description), icon, durationMs, List.of()));
	}

	/** Sticky button toast (stays until a button is clicked). */
	public static void showButtons(String key, Component title, Component description, List<ToastButton> buttons) {
		showButtons(key, title, description, 0L, buttons);
	}

	/**
	 * Shows a toast with buttons. {@code durationMs <= 0} is sticky (stays until a button is clicked);
	 * a positive duration auto-dismisses after that long. Re-showing with the same {@code key} replaces
	 * the existing one rather than stacking a duplicate.
	 */
	public static void showButtons(String key, Component title, Component description,
								   long durationMs, List<ToastButton> buttons) {
		showButtons(key, title, fixed(description), durationMs, buttons);
	}

	/** Button toast whose description is resolved live each frame (e.g. download progress). */
	public static void showButtons(String key, Component title, Supplier<Component> description,
								   long durationMs, List<ToastButton> buttons) {
		if (title == null) {
			return;
		}
		if (key != null) {
			active.removeIf(e -> key.equals(e.key));
		}
		add(new ToastEntry(key, title, description, null, durationMs, buttons));
	}

	private static Supplier<Component> fixed(Component c) {
		return c == null ? null : () -> c;
	}

	private static void add(ToastEntry entry) {
		if (active.size() >= MAX_TOASTS) {
			active.remove(0);
		}
		active.add(entry);
	}

	private static void render(GuiGraphics ctx, int mouseX, int mouseY) {
		if (active.isEmpty()) {
			return;
		}
		Minecraft client = Minecraft.getInstance();
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

		int cursor = bottom ? screenH - EDGE_MARGIN : EDGE_MARGIN;

		Iterator<ToastEntry> it = active.iterator();
		while (it.hasNext()) {
			ToastEntry t = it.next();
			long elapsed = now - t.spawnTime;
			if (!t.sticky() && elapsed >= t.durationMs) {
				it.remove();
				continue;
			}

			int boxW = boxWidth(font, t);
			int boxH = boxHeight(t);

			float slideIn = Math.min(1f, elapsed / (float) SLIDE_MS);
			float slideOut = (!t.sticky() && elapsed > t.durationMs - SLIDE_MS)
					? (t.durationMs - elapsed) / (float) SLIDE_MS : 1f;
			float visible = Math.max(0f, Math.min(slideIn, slideOut));
			int slide = (int) ((1f - visible) * (boxW + EDGE_MARGIN));

			int restX = right ? (screenW - EDGE_MARGIN - boxW) : EDGE_MARGIN;
			int x = right ? restX + slide : restX - slide;
			int y = bottom ? cursor - boxH : cursor;

			drawToast(ctx, font, t, x, y, boxW, boxH, right, mouseX, mouseY);

			cursor = bottom ? (y - GAP) : (y + boxH + GAP);
		}
	}

	private static int boxWidth(Font font, ToastEntry t) {
		Component desc = t.desc();
		int iconSpace = t.icon != null ? ICON + 4 : 0;
		int descW = desc != null ? font.width(desc) : 0;
		int contentW = Math.max(font.width(t.title), descW);
		int w = PAD + iconSpace + contentW + PAD;
		int max = MAX_W;
		if (!t.buttons.isEmpty()) {
			int btnRow = PAD + buttonsRowWidth(font, t) + PAD;
			w = Math.max(w, btnRow);
			max = Math.max(MAX_W, btnRow); // let a wide button row expand the card past the normal cap
		}
		return clamp(w, MIN_W, max);
	}

	private static int boxHeight(ToastEntry t) {
		int textLines = t.desc() != null ? 2 : 1;
		int contentH = Math.max(t.icon != null ? ICON : 0, textLines * LINE_H);
		int h = contentH + PAD * 2;
		if (!t.buttons.isEmpty()) {
			h += BTN_GAP + BTN_H;
		}
		return h;
	}

	private static int buttonsRowWidth(Font font, ToastEntry t) {
		int w = 0;
		for (int i = 0; i < t.buttons.size(); i++) {
			w += font.width(t.buttons.get(i).label()) + BTN_HPAD * 2;
			if (i > 0) {
				w += BTN_GAP;
			}
		}
		return w;
	}

	/** Draws a single toast card, aligned left or right within its box. */
	private static void drawToast(GuiGraphics ctx, Font font, ToastEntry t, int x, int y,
								  int boxW, int boxH, boolean right, int mouseX, int mouseY) {
		Component desc = t.desc();
		ctx.fill(x, y, x + boxW, y + boxH, Theme.surface);
		int border = t.sticky() ? Theme.accent : Theme.line;
		ctx.fill(x, y, x + boxW, y + 1, border);
		ctx.fill(x, y + boxH - 1, x + boxW, y + boxH, border);
		ctx.fill(x, y, x + 1, y + boxH, border);
		ctx.fill(x + boxW - 1, y, x + boxW, y + boxH, border);

		int iconSpace = t.icon != null ? ICON + 4 : 0;
		if (t.icon != null) {
			int iconX = right ? x + boxW - PAD - ICON : x + PAD;
			ctx.renderItem(t.icon, iconX, y + PAD);
		}

		int ty = y + PAD;
		if (right) {
			int rightEdge = x + boxW - PAD - iconSpace;
			ctx.drawString(font, t.title, rightEdge - font.width(t.title), ty, Theme.text, true);
			if (desc != null) {
				ctx.drawString(font, desc, rightEdge - font.width(desc), ty + LINE_H, Theme.muted, true);
			}
		} else {
			int leftEdge = x + PAD + iconSpace;
			ctx.drawString(font, t.title, leftEdge, ty, Theme.text, true);
			if (desc != null) {
				ctx.drawString(font, desc, leftEdge, ty + LINE_H, Theme.muted, true);
			}
		}

		if (!t.buttons.isEmpty()) {
			drawButtons(ctx, font, t, x, y, boxW, boxH, mouseX, mouseY);
		}
	}

	private static void drawButtons(GuiGraphics ctx, Font font, ToastEntry t, int x, int y,
									int boxW, int boxH, int mouseX, int mouseY) {
		int n = t.buttons.size();
		t.bx = new int[n];
		t.by = new int[n];
		t.bw = new int[n];

		int rowW = buttonsRowWidth(font, t);
		int bx = x + boxW - PAD - rowW; // right-aligned button row
		int by = y + boxH - PAD - BTN_H;
		for (int i = 0; i < n; i++) {
			ToastButton btn = t.buttons.get(i);
			int bw = font.width(btn.label()) + BTN_HPAD * 2;
			boolean hover = mouseX >= bx && mouseX < bx + bw && mouseY >= by && mouseY < by + BTN_H;

			// Secondary buttons are drawn dimmer (faint border / muted text) to de-emphasize them.
			int borderIdle = btn.secondary() ? Theme.faint : Theme.line;
			int borderColor = hover ? (btn.secondary() ? Theme.muted : Theme.accent) : borderIdle;
			int textColor = btn.secondary()
					? (hover ? Theme.muted : Theme.faint)
					: (hover ? Theme.text : Theme.muted);

			ctx.fill(bx, by, bx + bw, by + BTN_H, Theme.surfaceHover);
			ctx.fill(bx, by, bx + bw, by + 1, borderColor);
			ctx.fill(bx, by + BTN_H - 1, bx + bw, by + BTN_H, borderColor);
			ctx.fill(bx, by, bx + 1, by + BTN_H, borderColor);
			ctx.fill(bx + bw - 1, by, bx + bw, by + BTN_H, borderColor);
			int lx = bx + (bw - font.width(btn.label())) / 2;
			ctx.drawString(font, btn.label(), lx, by + (BTN_H - 8) / 2, textColor, false);

			t.bx[i] = bx;
			t.by[i] = by;
			t.bw[i] = bw;
			bx += bw + BTN_GAP;
		}
	}

	/** Routes a click to a toast button, running its action and dismissing that toast. */
	private static boolean handleClick(double mx, double my) {
		for (ToastEntry t : new ArrayList<>(active)) {
			if (t.buttons.isEmpty() || t.bx == null) {
				continue;
			}
			for (int i = 0; i < t.buttons.size(); i++) {
				if (mx >= t.bx[i] && mx < t.bx[i] + t.bw[i] && my >= t.by[i] && my < t.by[i] + BTN_H) {
					active.remove(t);
					Runnable action = t.buttons.get(i).onClick();
					if (action != null) {
						action.run();
					}
					return true;
				}
			}
		}
		return false;
	}

	private static int clamp(int v, int min, int max) {
		return Math.max(min, Math.min(max, v));
	}
}
