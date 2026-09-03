package dev.nishu.bettercosmic.prisons.screen;

import dev.nishu.bettercosmic.prisons.client.BetterPrisonsClient;
import dev.nishu.bettercosmic.prisons.pvviewer.PvItemCodec;
import dev.nishu.bettercosmic.prisons.pvviewer.PvSnapshot;
import dev.nishu.bettercosmic.shared.easyview.EasyView;
import dev.nishu.bettercosmic.shared.ui.core.Theme;
import dev.nishu.bettercosmic.shared.ui.render.RenderUtils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;

import java.util.ArrayList;
import java.util.List;

/**
 * Read-only viewer for the player's cached vaults ({@link PvSnapshot}s captured by
 * {@code PvCapture}). A vault selector runs down the left; the selected vault's contents render as a
 * true-size item grid on the right, with hover tooltips and a "last seen" stamp. The search box dims
 * non-matching items across the open vault. Nothing here talks to the server — it only displays the
 * on-disk cache.
 *
 * <p>Built on the shared {@link Theme}/{@link RenderUtils} primitives with a vanilla {@link EditBox},
 * matching {@link WaypointsScreen}. Opened by the {@code pv_viewer} keybind.
 */
public class PvViewerScreen extends Screen {

	private static final int SLOT = 18;
	private static final int COLS = 9;
	private static final int LIST_W = 74;
	private static final int GAP = 10;
	private static final int PAD = 10;
	private static final int HEADER_H = 22;
	private static final int SEARCH_H = 16;
	private static final int LIST_ROW_H = 16;
	private static final int GRID_MAX_ROWS = 6;
	private static final int MATCH_TINT = 0x8032CD32; // 50% lime behind matches
	private static final int DIM = 0xC0101018;        // darken non-matches
	private static final int STAR_W = 11;             // clickable star button width in a list row
	private static final int STAR_COLOR = 0xFFFFD54A; // gold when favorited

	private final String profileKey;
	private List<Integer> vaultNumbers;

	private int panelX, panelY, panelW, panelH;
	private int gridX, gridY;

	private int selectedVault = -1;
	private PvSnapshot current;
	private List<ItemStack> decoded = new ArrayList<>();

	private EditBox searchField;
	private int listScroll = 0;

	public PvViewerScreen() {
		super(Component.literal("Vault Viewer"));
		this.profileKey = dev.nishu.bettercosmic.prisons.pvviewer.PvKey.current();
		this.vaultNumbers = BetterPrisonsClient.pvVaultStore.vaults(profileKey);
	}

	@Override
	protected void init() {
		int gridW = COLS * SLOT;
		panelW = PAD + LIST_W + GAP + gridW + PAD;
		panelH = HEADER_H + SEARCH_H + 6 + GRID_MAX_ROWS * SLOT + PAD;
		panelX = (width - panelW) / 2;
		panelY = (height - panelH) / 2;

		gridX = panelX + PAD + LIST_W + GAP;
		gridY = panelY + HEADER_H + SEARCH_H + 6;

		searchField = new EditBox(this.font, gridX, panelY + HEADER_H, gridW, SEARCH_H,
				Component.literal("Search"));
		searchField.setHint(Component.literal("Search items…"));
		searchField.setMaxLength(64);
		addRenderableWidget(searchField);

		if (selectedVault < 0 && !vaultNumbers.isEmpty()) {
			select(vaultNumbers.get(0));
		}
	}

	private void select(int vault) {
		selectedVault = vault;
		current = BetterPrisonsClient.pvVaultStore.get(profileKey, vault);
		decoded = new ArrayList<>();
		if (current != null && current.items != null && minecraft != null && minecraft.level != null) {
			for (var el : current.items) {
				decoded.add(PvItemCodec.decode(el, minecraft.level.registryAccess()));
			}
		}
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
		// Plain dim backdrop — do NOT call renderBackground(): super.render() already draws the screen
		// background, and blurring twice in a frame crashes ("Can only blur once per frame").
		g.fill(0, 0, width, height, Theme.ground);
		RenderUtils.panel(g, panelX, panelY, panelW, panelH, Theme.surface, Theme.line);

		// Header
		String title = selectedVault >= 0 ? "Player Vault #" + selectedVault : "Vault Viewer";
		RenderUtils.text(g, title, panelX + PAD, panelY + 7, Theme.text);
		if (current != null) {
			String stamp = current.capturedAt == 0
					? "not yet opened"
					: "last seen " + formatAgo(System.currentTimeMillis() - current.capturedAt);
			RenderUtils.textRight(g, stamp, panelX + panelW - PAD, panelY + 7, Theme.muted);
		}

		super.render(g, mouseX, mouseY, delta); // search EditBox

		renderVaultList(g, mouseX, mouseY);
		ItemStack hovered = renderGrid(g, mouseX, mouseY);

		if (vaultNumbers.isEmpty()) {
			RenderUtils.textCentered(g, "No vaults cached yet — open /pv <n> in-game.",
					gridX + COLS * SLOT / 2, gridY + 40, Theme.muted);
		}

		if (hovered != null && !hovered.isEmpty()) {
			g.setTooltipForNextFrame(this.font, hovered, mouseX, mouseY);
		}
	}

	private void renderVaultList(GuiGraphics g, int mouseX, int mouseY) {
		int lx = panelX + PAD;
		int ly = panelY + HEADER_H;
		int lh = panelH - HEADER_H - PAD;
		RenderUtils.panel(g, lx, ly, LIST_W, lh, Theme.ground, Theme.line);

		RenderUtils.pushScissor(g, lx, ly, LIST_W, lh);
		int y = ly + 2 - listScroll;
		for (int vault : vaultNumbers) {
			if (y + LIST_ROW_H >= ly && y <= ly + lh) {
				boolean selected = vault == selectedVault;
				boolean hover = RenderUtils.hit(mouseX, mouseY, lx, y, LIST_W, LIST_ROW_H)
						&& mouseY >= ly && mouseY <= ly + lh;
				if (selected) {
					RenderUtils.rect(g, lx + 1, y, LIST_W - 2, LIST_ROW_H, Theme.accent);
				} else if (hover) {
					RenderUtils.rect(g, lx + 1, y, LIST_W - 2, LIST_ROW_H, Theme.surfaceHover);
				}
				RenderUtils.text(g, "Vault " + vault, lx + 6, y + 4, selected ? 0xFF101018 : Theme.text);

				// Star button at the row's right edge — click to favorite.
				boolean favorite = BetterPrisonsClient.pvVaultStore.isFavorite(profileKey, vault);
				int starX = lx + LIST_W - STAR_W;
				boolean starHover = RenderUtils.hit(mouseX, mouseY, starX, y, STAR_W, LIST_ROW_H)
						&& mouseY >= ly && mouseY <= ly + lh;
				int starColor = favorite ? STAR_COLOR : selected ? 0xFF101018 : starHover ? Theme.text : Theme.muted;
				RenderUtils.text(g, favorite ? "★" : "☆", starX, y + 4, starColor);
			}
			y += LIST_ROW_H;
		}
		RenderUtils.popScissor(g);
	}

	/** Draws the selected vault's grid and returns the stack under the cursor, or null. */
	private ItemStack renderGrid(GuiGraphics g, int mouseX, int mouseY) {
		if (current == null) {
			return null;
		}
		String query = searchField.getValue().trim().toLowerCase();
		boolean filtering = !query.isEmpty();
		ItemStack hovered = null;

		for (int i = 0; i < decoded.size(); i++) {
			int col = i % COLS;
			int row = i / COLS;
			int x = gridX + col * SLOT;
			int y = gridY + row * SLOT;

			ItemStack stack = decoded.get(i);
			boolean match = filtering && !stack.isEmpty() && matches(stack, query);

			dev.nishu.bettercosmic.prisons.pvviewer.PvSlot.render(g, x, y);
			if (match) {
				RenderUtils.rect(g, x + 1, y + 1, SLOT - 2, SLOT - 2, MATCH_TINT);
			}

			if (!stack.isEmpty()) {
				g.renderItem(stack, x + 1, y + 1);
				g.renderItemDecorations(this.font, stack, x + 1, y + 1);
				EasyView.renderSlotOverlays(g, x + 1, y + 1, stack); // clue numbers, cooldowns, charges, …
				if (filtering && !match) {
					RenderUtils.rect(g, x + 1, y + 1, SLOT - 2, SLOT - 2, DIM);
				}
				if (RenderUtils.hit(mouseX, mouseY, x, y, SLOT, SLOT)) {
					hovered = stack;
				}
			}
		}
		return hovered;
	}

	private static boolean matches(ItemStack stack, String query) {
		if (stack.getHoverName().getString().toLowerCase().contains(query)) {
			return true;
		}
		ItemLore lore = stack.get(DataComponents.LORE);
		if (lore != null) {
			for (Component line : lore.lines()) {
				if (line.getString().toLowerCase().contains(query)) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubled) {
		int lx = panelX + PAD;
		int ly = panelY + HEADER_H;
		int lh = panelH - HEADER_H - PAD;
		if (RenderUtils.hit(event.x(), event.y(), lx, ly, LIST_W, lh)) {
			int idx = (int) ((event.y() - (ly + 2) + listScroll) / LIST_ROW_H);
			if (idx >= 0 && idx < vaultNumbers.size()) {
				int vault = vaultNumbers.get(idx);
				boolean onStar = event.x() >= lx + LIST_W - STAR_W;
				if (event.button() == 1 || onStar) {
					// Star button / right click: star-unstar and re-sort (favorites first).
					BetterPrisonsClient.pvVaultStore.toggleFavorite(profileKey, vault);
					vaultNumbers = BetterPrisonsClient.pvVaultStore.vaults(profileKey);
				} else {
					select(vault);
				}
				return true;
			}
		}
		return super.mouseClicked(event, doubled);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double dx, double dy) {
		int ly = panelY + HEADER_H;
		int lh = panelH - HEADER_H - PAD;
		if (RenderUtils.hit(mouseX, mouseY, panelX + PAD, ly, LIST_W, lh)) {
			int contentH = vaultNumbers.size() * LIST_ROW_H;
			int maxScroll = Math.max(0, contentH - (lh - 4));
			listScroll = Math.max(0, Math.min(maxScroll, listScroll - (int) (dy * LIST_ROW_H)));
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, dx, dy);
	}

	/** Compact "3h", "5m", "just now" style age string. */
	private static String formatAgo(long millis) {
		long s = Math.max(0, millis / 1000);
		if (s < 60) return "just now";
		long m = s / 60;
		if (m < 60) return m + "m ago";
		long h = m / 60;
		if (h < 24) return h + "h ago";
		return (h / 24) + "d ago";
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
