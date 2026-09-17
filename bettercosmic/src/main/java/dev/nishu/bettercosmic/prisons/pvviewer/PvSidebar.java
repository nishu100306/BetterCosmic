package dev.nishu.bettercosmic.prisons.pvviewer;

import com.google.gson.JsonElement;
import com.mojang.blaze3d.platform.InputConstants;
import dev.nishu.bettercosmic.prisons.client.BetterPrisonsClient;
import dev.nishu.bettercosmic.shared.easyview.EasyView;
import dev.nishu.bettercosmic.shared.notification.ToastRenderer;
import dev.nishu.bettercosmic.shared.ui.core.Theme;
import dev.nishu.bettercosmic.shared.ui.render.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Cached-vault preview overlay for any {@code /pv} screen (the {@code Vaults} selector and each
 * {@code Player Vault #X} window — see {@link PvScreens}). Vaults are split across two single-column
 * sidebars — one docked at the left edge, one at the right — that share a single scroll position, so
 * row N on the left lines up with row N on the right. Grids are full-size vanilla slots and hovering an
 * item shows its tooltip.
 *
 * <p>Interaction depends on whether the vault is the one currently open in the real GUI (the
 * {@code Player Vault #X} the player is viewing):
 * <ul>
 *   <li><b>The open vault's</b> preview renders <em>live</em> from the container and its slots are
 *       interactive — clicking one forwards the click to the real menu
 *       ({@code handleInventoryMouseClick}), so the player can pick up / move / place items through
 *       the preview (shift = quick-move, right-click = the vanilla right-click).
 *   <li><b>Other vaults'</b> previews are read-only cached snapshots; left-clicking one runs
 *       {@code /pv <n>} to select (open) it, after which it becomes the interactive one.
 * </ul>
 * Favoriting is via the star button (left-click) on each header. Registered by {@link PvScreenHooks}.
 */
public final class PvSidebar {

	private static final int COLUMNS = 2;        // one column per side (left, right)
	private static final int SLOT = PvSlot.CELL; // full-size vanilla slot cell
	private static final int GRID_W = 9 * SLOT;
	private static final int ENTRY_W = GRID_W;
	private static final int PAD = 6;
	private static final int PANEL_W = ENTRY_W + PAD * 2;
	private static final int ENTRY_GAP = 10;
	private static final int ENTRY_HEADER_H = 12;
	private static final int TITLE_H = 15;
	private static final int MARGIN = 6;
	private static final int STAR_W = 10;             // clickable star button width
	private static final int STAR_COLOR = 0xFFFFD54A; // gold when favorited

	// Shared scroll, persisted across /pv screens (selecting a vault opens a new screen but shouldn't
	// reset the sidebar); clampScroll keeps it valid if the vault list changes.
	private static int scroll = 0;

	// True while the item on the cursor was picked up from a preview (a vault). Used to block moving an
	// item from one preview into another; an inventory-sourced item stays freely movable.
	private static boolean carriedFromPreview = false;

	// Deposit-into-an-unopened-vault flow: clicking an unselected preview while carrying an inventory
	// item runs /pv <n> (which the server processes by returning the carried item to the inventory), so
	// we remember it and re-grab it onto the cursor once that vault opens — the click "keeps" the item.
	private static ItemStack pendingItem = ItemStack.EMPTY;
	private static int pendingVault = -1;
	private static int pendingTicks = 0;

	// Anti-spam: a minimum gap between auto-opens, AND an in-flight guard so a second /pv can't be sent
	// until the vault the last one requested has actually opened (opening takes longer than the gap, so
	// the gap alone doesn't stop click-spam).
	private static final long OPEN_COOLDOWN_MS = 300;
	private static final long AWAIT_TIMEOUT_MS = 2000;
	private static long lastAutoOpen = 0;
	private static int awaitingVault = -1;
	private static long awaitingSince = 0;

	/** Decode cache: "profileKey#vault" -> (capturedAt, decoded stacks). */
	private static final Map<String, Cached> DECODE_CACHE = new HashMap<>();

	private PvSidebar() {}

	private record Cached(long capturedAt, List<ItemStack> items) {}

	private record Entry(int vault, PvSnapshot snapshot, int height) {}

	/** An entry placed in content space (before scroll): which side (0=left, 1=right) and its row's y. */
	private record Placed(Entry entry, int column, int relY) {}

	private static boolean enabledOn(Screen screen) {
		return dev.nishu.bettercosmic.prisons.PrisonsGate.active()
				&& BetterPrisonsClient.config != null
				&& BetterPrisonsClient.config.pvViewerEnabled
				&& Minecraft.getInstance().player != null
				&& PvScreens.isPvScreen(screen);
	}

	private static String profileKey() {
		return PvKey.current();
	}

	private static int gridRows(PvSnapshot snap) {
		return Math.max(1, snap.rows);
	}

	private static int entryHeight(PvSnapshot snap) {
		return ENTRY_HEADER_H + gridRows(snap) * SLOT;
	}

	// ---- Layout (shared by render / click / scroll) ----

	private static int leftPanelX() {
		return MARGIN;
	}

	private static int rightPanelX(Screen screen) {
		return screen.width - MARGIN - PANEL_W;
	}

	/** Screen x of a column's grid content: left side or right side. */
	private static int contentX(Screen screen, int column) {
		return (column == 0 ? leftPanelX() : rightPanelX(screen)) + PAD;
	}

	private static int viewportTop() {
		return MARGIN + TITLE_H + 2;
	}

	private static int viewportHeight(Screen screen) {
		return screen.height - viewportTop() - MARGIN;
	}

	private static List<Entry> entries(String key) {
		List<Entry> out = new ArrayList<>();
		for (int vault : BetterPrisonsClient.pvVaultStore.vaults(key)) {
			PvSnapshot snap = BetterPrisonsClient.pvVaultStore.get(key, vault);
			if (snap != null) {
				out.add(new Entry(vault, snap, entryHeight(snap)));
			}
		}
		return out;
	}

	/** Flows entries into rows of {@value #COLUMNS} (left then right); each row is as tall as its taller entry. */
	private static List<Placed> place(List<Entry> entries) {
		List<Placed> placed = new ArrayList<>();
		int y = 0;
		for (int i = 0; i < entries.size(); i += COLUMNS) {
			int rowH = 0;
			for (int j = 0; j < COLUMNS && i + j < entries.size(); j++) {
				rowH = Math.max(rowH, entries.get(i + j).height());
			}
			for (int j = 0; j < COLUMNS && i + j < entries.size(); j++) {
				placed.add(new Placed(entries.get(i + j), j, y));
			}
			y += rowH + ENTRY_GAP;
		}
		return placed;
	}

	private static int contentHeight(List<Placed> placed) {
		int max = 0;
		for (Placed p : placed) {
			max = Math.max(max, p.relY() + p.entry().height());
		}
		return max;
	}

	private static void clampScroll(Screen screen, int contentHeight) {
		int max = Math.max(0, contentHeight - viewportHeight(screen));
		scroll = Math.max(0, Math.min(max, scroll));
	}

	// ---- Render ----

	public static void render(GuiGraphics g, Screen screen, int mouseX, int mouseY) {
		if (!enabledOn(screen)) {
			return;
		}
		String key = profileKey();
		List<Entry> entries = entries(key);
		if (entries.isEmpty()) {
			return;
		}
		List<Placed> placed = place(entries);
		clampScroll(screen, contentHeight(placed));

		int currentVault = PvScreens.vaultContentsNumber(screen);
		int panelH = screen.height - MARGIN * 2;
		drawPanelFrame(g, leftPanelX(), panelH);
		drawPanelFrame(g, rightPanelX(screen), panelH);

		ItemStack hovered = renderColumn(g, screen, placed, 0, leftPanelX(), mouseX, mouseY, currentVault, key);
		ItemStack right = renderColumn(g, screen, placed, 1, rightPanelX(screen), mouseX, mouseY, currentVault, key);
		if (hovered == null) {
			hovered = right;
		}

		// The cursor-carried item is drawn by vanilla before this (afterRender) pass, so the panels
		// cover it. Re-draw it on top where it would be hidden, so it floats above the sidebar just like
		// it floats above the real slots. No tooltip while carrying (matches vanilla).
		int vTop = viewportTop();
		int vH = viewportHeight(screen);
		ItemStack carried = screen instanceof AbstractContainerScreen<?> cs ? cs.getMenu().getCarried() : ItemStack.EMPTY;
		if (!carried.isEmpty()) {
			Placed under = entryAt(screen, placed, mouseX, mouseY, vTop, vH);
			boolean overUnselectedGrid = under != null
					&& under.entry().vault() != currentVault
					&& mouseY >= vTop - scroll + under.relY() + ENTRY_HEADER_H;
			// Only a preview-sourced item is blocked here: you can't move an item from one preview into
			// another. An inventory-sourced item may go anywhere (clicking opens that vault to deposit).
			if (overUnselectedGrid && carriedFromPreview) {
				renderBarrierCursor(g, mouseX, mouseY);
			} else if (overSidebar(screen, mouseX, mouseY)) {
				g.renderItem(carried, mouseX - 8, mouseY - 8);
				g.renderItemDecorations(Minecraft.getInstance().font, carried, mouseX - 8, mouseY - 8);
			}
		} else {
			carriedFromPreview = false; // nothing on the cursor
			if (hovered != null && !hovered.isEmpty()) {
				// Deferred: this render runs before the screen's tooltip flush (see PvSidebarRenderMixin),
				// so it draws on top — and vanilla real-slot tooltips draw on top of the sidebar.
				g.setTooltipForNextFrame(Minecraft.getInstance().font, hovered, mouseX, mouseY);
			}
		}
	}

	/** The entry under the cursor within the scroll viewport, or null. */
	private static Placed entryAt(Screen screen, List<Placed> placed, int mx, int my, int vTop, int vH) {
		if (my < vTop || my > vTop + vH) {
			return null;
		}
		for (Placed p : placed) {
			int ex = contentX(screen, p.column());
			int ey = vTop - scroll + p.relY();
			if (mx >= ex && mx < ex + ENTRY_W && my >= ey && my < ey + p.entry().height()) {
				return p;
			}
		}
		return null;
	}

	/** Draws a 0.3× barrier at the cursor, meaning "can't move an item here". */
	private static void renderBarrierCursor(GuiGraphics g, int mx, int my) {
		var pose = g.pose();
		pose.pushMatrix();
		pose.translate(mx, my);
		pose.scale(0.3f, 0.3f);
		g.renderItem(new ItemStack(Items.BARRIER), -8, -8);
		pose.popMatrix();
	}

	private static void drawPanelFrame(GuiGraphics g, int panelX, int panelH) {
		RenderUtils.panel(g, panelX, MARGIN, PANEL_W, panelH, Theme.surface, Theme.line);
		RenderUtils.text(g, "Vaults", panelX + PAD, MARGIN + 4, Theme.muted);
		RenderUtils.hLine(g, panelX + PAD, MARGIN + TITLE_H, ENTRY_W, Theme.line);
	}

	/** Draws one side's entries (clipped to its viewport) and returns the item under the cursor, or null. */
	private static ItemStack renderColumn(GuiGraphics g, Screen screen, List<Placed> placed, int column,
										  int panelX, int mouseX, int mouseY, int currentVault, String key) {
		int vTop = viewportTop();
		int vH = viewportHeight(screen);
		int ex = panelX + PAD;
		ItemStack hovered = null;

		RenderUtils.pushScissor(g, panelX, vTop, PANEL_W, vH);
		for (Placed p : placed) {
			if (p.column() != column) {
				continue;
			}
			int ey = vTop - scroll + p.relY();
			int eh = p.entry().height();
			if (ey + eh < vTop || ey > vTop + vH) {
				continue;
			}
			PvSnapshot snap = p.entry().snapshot();
			boolean current = p.entry().vault() == currentVault;
			boolean hover = RenderUtils.hit(mouseX, mouseY, ex, ey, ENTRY_W, eh)
					&& mouseY >= vTop && mouseY <= vTop + vH;
			boolean placeholder = snap.capturedAt == 0;

			int headerColor = current || hover ? Theme.accent : placeholder ? Theme.faint : Theme.text;
			RenderUtils.text(g, "Vault " + p.entry().vault(), ex, ey + 1, headerColor);

			// Star button (top-right of the header) — click to favorite.
			int starX = ex + ENTRY_W - STAR_W;
			boolean starHover = RenderUtils.hit(mouseX, mouseY, starX, ey, STAR_W, ENTRY_HEADER_H)
					&& mouseY >= vTop && mouseY <= vTop + vH;
			RenderUtils.text(g, snap.favorite ? "★" : "☆", starX, ey + 1,
					snap.favorite ? STAR_COLOR : starHover ? Theme.text : Theme.muted);

			int gy = ey + ENTRY_HEADER_H;
			// The open vault renders live (in sync with clicks); the rest render their cached snapshot.
			List<ItemStack> stacks = current ? liveVaultStacks(screen) : decoded(key, snap);
			ItemStack h = renderGrid(g, ex, gy, stacks, gridRows(snap) * 9, mouseX, mouseY);
			if (h != null) {
				hovered = h;
			}
			if (current || hover) {
				RenderUtils.outline(g, ex - 1, gy - 1, ENTRY_W + 2, gridRows(snap) * SLOT + 2, Theme.accent);
			}
		}
		RenderUtils.popScissor(g);
		return hovered;
	}

	/** Draws a vault's full-size grid ({@code cap} slots); returns the stack under the cursor, or null. */
	private static ItemStack renderGrid(GuiGraphics g, int gx, int gy, List<ItemStack> stacks, int cap,
										int mouseX, int mouseY) {
		ItemStack hovered = null;
		for (int i = 0; i < cap; i++) {
			int x = gx + (i % 9) * SLOT;
			int y = gy + (i / 9) * SLOT;
			PvSlot.render(g, x, y);
			ItemStack st = i < stacks.size() ? stacks.get(i) : ItemStack.EMPTY;
			if (!st.isEmpty()) {
				int ix = x + PvSlot.ITEM_INSET;
				int iy = y + PvSlot.ITEM_INSET;
				g.renderItem(st, ix, iy);
				g.renderItemDecorations(Minecraft.getInstance().font, st, ix, iy);
				// EasyView overlays + chest-search highlight (ChestSearchTintProvider is an EasyView tint),
				// drawn in the same order as the vanilla slot mixin: item → tints → overlays.
				EasyView.renderSlotTints(g, ix, iy, st);
				EasyView.renderSlotOverlays(g, ix, iy, st);
				if (RenderUtils.hit(mouseX, mouseY, x, y, SLOT, SLOT)) {
					hovered = st;
				}
			}
		}
		return hovered;
	}

	/** The live vault-slot contents of the open container (player-inventory slots excluded), in order. */
	private static List<ItemStack> liveVaultStacks(Screen screen) {
		List<ItemStack> out = new ArrayList<>();
		Minecraft mc = Minecraft.getInstance();
		if (screen instanceof AbstractContainerScreen<?> cs && mc.player != null) {
			Container inv = mc.player.getInventory();
			for (Slot slot : cs.getMenu().slots) {
				if (slot.container != inv) {
					out.add(slot.getItem());
				}
			}
		}
		return out;
	}

	/** Forwards a click on the {@code previewIndex}-th vault slot to the real open menu. */
	private static void forwardClick(Screen screen, int previewIndex, int button) {
		Minecraft mc = Minecraft.getInstance();
		if (!(screen instanceof AbstractContainerScreen<?> cs) || mc.player == null || mc.gameMode == null) {
			return;
		}
		AbstractContainerMenu menu = cs.getMenu();
		Container inv = mc.player.getInventory();
		int seen = 0;
		for (Slot slot : menu.slots) {
			if (slot.container == inv) {
				continue;
			}
			if (seen == previewIndex) {
				boolean shift = InputConstants.isKeyDown(mc.getWindow(), org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_SHIFT)
						|| InputConstants.isKeyDown(mc.getWindow(), org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_SHIFT);
				ClickType type = shift ? ClickType.QUICK_MOVE : ClickType.PICKUP;
				mc.gameMode.handleInventoryMouseClick(menu.containerId, slot.index, button, type, mc.player);
				// If the cursor now holds an item, it came out of this vault preview.
				carriedFromPreview = !menu.getCarried().isEmpty();
				return;
			}
			seen++;
		}
	}

	private static List<ItemStack> decoded(String profileKey, PvSnapshot snap) {
		String key = profileKey + "#" + snap.vault;
		Cached cached = DECODE_CACHE.get(key);
		if (cached != null && cached.capturedAt() == snap.capturedAt) {
			return cached.items();
		}
		List<ItemStack> items = new ArrayList<>();
		Minecraft mc = Minecraft.getInstance();
		if (snap.items != null && mc.level != null) {
			for (JsonElement el : snap.items) {
				items.add(PvItemCodec.decode(el, mc.level.registryAccess()));
			}
		}
		DECODE_CACHE.put(key, new Cached(snap.capturedAt, items));
		return items;
	}

	// ---- Input ----

	/**
	 * Handles a click on an entry. Left-click on the star toggles favorite. For the open vault, a click
	 * on a slot is forwarded to the live container; for any other vault, a left-click selects (opens)
	 * it. Returns true if it landed on an entry.
	 */
	public static boolean handleClick(Screen screen, double mx, double my, int button) {
		if ((button != 0 && button != 1) || !enabledOn(screen)) {
			return false;
		}
		String key = profileKey();
		List<Entry> entries = entries(key);
		if (entries.isEmpty()) {
			return false;
		}
		List<Placed> placed = place(entries);
		clampScroll(screen, contentHeight(placed));

		int vTop = viewportTop();
		int vH = viewportHeight(screen);
		if (my < vTop || my > vTop + vH) {
			return false;
		}
		int openNumber = PvScreens.vaultContentsNumber(screen);
		for (Placed p : placed) {
			int ex = contentX(screen, p.column());
			int ey = vTop - scroll + p.relY();
			int eh = p.entry().height();
			if (!(mx >= ex && mx < ex + ENTRY_W && my >= ey && my < ey + eh)) {
				continue;
			}
			// Star button (top-right of the header), left-click only.
			if (button == 0 && mx >= ex + ENTRY_W - STAR_W && my < ey + ENTRY_HEADER_H) {
				BetterPrisonsClient.pvVaultStore.toggleFavorite(key, p.entry().vault());
				return true;
			}
			if (p.entry().vault() == openNumber) {
				// Interactive: forward a click on the matching slot of the live vault.
				int gy = ey + ENTRY_HEADER_H;
				if (my >= gy) {
					int col = (int) ((mx - ex) / SLOT);
					int row = (int) ((my - gy) / SLOT);
					int idx = row * 9 + col;
					if (col >= 0 && col < 9 && idx >= 0 && idx < gridRows(p.entry().snapshot()) * 9) {
						forwardClick(screen, idx, button);
					}
				}
				return true;
			}
			// A different vault. Block moving an item from one preview into another; but a carried
			// inventory item is fine — open that vault (keeping the item) so it can be deposited.
			ItemStack carried = screen instanceof AbstractContainerScreen<?> cs ? cs.getMenu().getCarried() : ItemStack.EMPTY;
			if (button == 0) {
				if (!carried.isEmpty()) {
					if (carriedFromPreview) {
						return true; // blocked (barrier cursor shows why)
					}
					// Carrying an inventory item: open the vault and re-grab the item once it's open,
					// but only arm that if the open wasn't throttled away.
					if (openVault(p.entry().vault())) {
						pendingItem = carried.copy();
						pendingVault = p.entry().vault();
						pendingTicks = 60;
					}
					return true;
				}
				openVault(p.entry().vault());
			}
			return true;
		}
		return false;
	}

	/** Adjusts the shared scroll when the cursor is over either sidebar. */
	public static void handleScroll(Screen screen, double mx, double my, double verticalAmount) {
		if (!enabledOn(screen)) {
			return;
		}
		int vTop = viewportTop();
		int vH = viewportHeight(screen);
		boolean overLeft = mx >= leftPanelX() && mx <= leftPanelX() + PANEL_W;
		boolean overRight = mx >= rightPanelX(screen) && mx <= rightPanelX(screen) + PANEL_W;
		if ((!overLeft && !overRight) || my < vTop || my > vTop + vH) {
			return;
		}
		scroll -= (int) (verticalAmount * SLOT);
		clampScroll(screen, contentHeight(place(entries(profileKey()))));
	}

	/**
	 * Whether the point is over either sidebar panel. Used to cancel vanilla mouse handling there — the
	 * panels sit outside the container window, where vanilla press/release would "drop outside" a carried
	 * item; the sidebar does its own forwarding instead.
	 */
	public static boolean overSidebar(Screen screen, double mx, double my) {
		if (!enabledOn(screen) || entries(profileKey()).isEmpty()) {
			return false;
		}
		if (my < MARGIN || my > screen.height - MARGIN) {
			return false;
		}
		boolean left = mx >= leftPanelX() && mx <= leftPanelX() + PANEL_W;
		boolean right = mx >= rightPanelX(screen) && mx <= rightPanelX(screen) + PANEL_W;
		return left || right;
	}

	/**
	 * Whether the mod currently has an auto-{@code /pv} open in flight (a sidebar click asked the server
	 * to open a vault and that vault hasn't appeared yet). The PV API reader uses this to distinguish a
	 * mod-driven open from a genuine player open, so it doesn't refresh on our own navigation.
	 */
	public static boolean isAutoOpening() {
		return awaitingVault >= 0;
	}

	/** Runs {@code /pv <n>}, throttled to one open per {@value #OPEN_COOLDOWN_MS}ms. Returns whether it fired. */
	private static boolean openVault(int vault) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.getConnection() == null) {
			return false;
		}
		long now = System.currentTimeMillis();
		boolean inFlight = awaitingVault >= 0 && now - awaitingSince < AWAIT_TIMEOUT_MS;
		if (inFlight || now - lastAutoOpen < OPEN_COOLDOWN_MS) {
			ToastRenderer.show(Component.literal("§cVault open cancelled"),
					Component.literal("Opening vaults too quickly."));
			return false;
		}
		lastAutoOpen = now;
		awaitingVault = vault;
		awaitingSince = now;
		mc.getConnection().sendCommand("pv " + vault);
		return true;
	}

	/**
	 * Once the vault requested by a carried-item click has opened, re-grabs the returned item onto the
	 * cursor so it looks like the click never dropped it. Called each client tick.
	 */
	public static void tickPendingDeposit() {
		Minecraft mc = Minecraft.getInstance();

		// Clear the in-flight open guard once the requested vault has actually opened (or it times out),
		// so the next auto-open is allowed again.
		if (awaitingVault >= 0) {
			int open = mc.screen != null ? PvScreens.vaultContentsNumber(mc.screen) : -1;
			if (open == awaitingVault || System.currentTimeMillis() - awaitingSince > AWAIT_TIMEOUT_MS) {
				awaitingVault = -1;
			}
		}

		if (pendingVault < 0) {
			return;
		}
		if (--pendingTicks <= 0) {
			clearPending();
			return;
		}
		if (mc.player == null || mc.gameMode == null
				|| !(mc.screen instanceof AbstractContainerScreen<?> cs)
				|| PvScreens.vaultContentsNumber(mc.screen) != pendingVault) {
			return; // the target vault isn't open yet — keep waiting
		}
		AbstractContainerMenu menu = cs.getMenu();
		if (!menu.getCarried().isEmpty()) {
			clearPending(); // the server kept the item on the cursor — nothing to restore
			return;
		}
		Container inv = mc.player.getInventory();
		Slot anyMatch = null;
		Slot exactMatch = null;
		for (Slot slot : menu.slots) {
			ItemStack in = slot.getItem();
			if (slot.container == inv && !in.isEmpty() && ItemStack.isSameItemSameComponents(in, pendingItem)) {
				if (anyMatch == null) {
					anyMatch = slot;
				}
				if (in.getCount() == pendingItem.getCount()) {
					exactMatch = slot; // the returned stack landed in its own slot, un-merged — prefer it
					break;
				}
			}
		}
		Slot target = exactMatch != null ? exactMatch : anyMatch;
		if (target != null) {
			mc.gameMode.handleInventoryMouseClick(menu.containerId, target.index, 0, ClickType.PICKUP, mc.player);
			carriedFromPreview = false; // still an inventory-sourced item — free to move on
			clearPending();
		}
	}

	private static void clearPending() {
		pendingItem = ItemStack.EMPTY;
		pendingVault = -1;
		pendingTicks = 0;
	}
}
