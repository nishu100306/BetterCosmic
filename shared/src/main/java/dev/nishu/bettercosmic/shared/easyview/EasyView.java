package dev.nishu.bettercosmic.shared.easyview;

import dev.nishu.bettercosmic.shared.server.Network;
import dev.nishu.bettercosmic.shared.server.ServerContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3x2fStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared "EasyView" system: draws small text overlays in the corners of inventory slots, for both
 * open container/inventory screens and the hotbar.
 *
 * <p>Ported and generalized from BetterPrisons. The rendering here is content-agnostic — what to
 * show for a given item is decided by registered {@link ItemOverlayProvider}s, so server-specific
 * item logic lives in the mods (e.g. Cosmic Sky trinket charges in BetterSky).
 *
 * <p>Rendering is driven by mixins at the item-decoration layer (right after each slot's item is
 * drawn) rather than a post-render event, so overlays sit <em>directly above the item texture</em>
 * and below tooltips — see {@code AbstractContainerScreenMixin} and {@code GuiMixin}. Those mixins
 * call {@link #renderSlotOverlays}.
 */
public final class EasyView {

	/** A registered provider paired with the network it belongs to ({@code null} = always active). */
	private record OverlayEntry(ItemOverlayProvider provider, Network network) {}

	private record TintEntry(SlotTintProvider provider, Network network) {}

	private static final List<OverlayEntry> PROVIDERS = new ArrayList<>();
	private static final List<TintEntry> TINTS = new ArrayList<>();

	private EasyView() {}

	/** Registers a provider. Multiple providers can draw to different corners of the same slot. */
	public static void register(ItemOverlayProvider provider) {
		register(provider, null);
	}

	/** Registers a provider owned by {@code network}; it draws only while that network is active. */
	public static void register(ItemOverlayProvider provider, Network network) {
		PROVIDERS.add(new OverlayEntry(provider, network));
	}

	/** Registers a slot-background tint provider (e.g. a search-match highlight). */
	public static void registerTint(SlotTintProvider provider) {
		registerTint(provider, null);
	}

	/** Registers a tint provider owned by {@code network}; it draws only while that network is active. */
	public static void registerTint(SlotTintProvider provider, Network network) {
		TINTS.add(new TintEntry(provider, network));
	}

	/**
	 * Fills the slot at ({@code slotX},{@code slotY}) with each registered tint provider's color (a
	 * translucent highlight over the item). Call this from the same render context as the item, right
	 * after it is drawn and before {@link #renderSlotOverlays} so text overlays stay on top.
	 */
	public static void renderSlotTints(GuiGraphics graphics, int slotX, int slotY, ItemStack stack) {
		if (TINTS.isEmpty() || stack.isEmpty()) {
			return;
		}
		for (TintEntry entry : TINTS) {
			if (!ServerContext.isActive(entry.network())) {
				continue;
			}
			int tint;
			try {
				tint = entry.provider().tint(stack);
			} catch (Exception e) {
				tint = 0; // a misbehaving provider must never break slot rendering
			}
			if (tint != 0) {
				graphics.fill(slotX, slotY, slotX + 16, slotY + 16, tint);
			}
		}
	}

	/**
	 * Draws every registered provider's overlay for {@code stack}, anchored within the slot at
	 * ({@code slotX}, {@code slotY}). These coordinates must be in the same space {@code graphics}
	 * is currently using to draw the item (i.e. call this from the same render context as the item),
	 * which is what keeps the overlay pinned to the item texture.
	 */
	public static void renderSlotOverlays(GuiGraphics graphics, int slotX, int slotY, ItemStack stack) {
		if (PROVIDERS.isEmpty() || stack.isEmpty()) {
			return;
		}
		Font font = Minecraft.getInstance().font;
		for (OverlayEntry entry : PROVIDERS) {
			if (!ServerContext.isActive(entry.network())) {
				continue;
			}
			SlotOverlay overlay;
			try {
				overlay = entry.provider().getOverlay(stack);
			} catch (Exception e) {
				overlay = null; // a misbehaving provider must never break slot rendering
			}
			if (overlay == null || overlay.text() == null || overlay.text().isEmpty()) {
				continue;
			}
			drawOverlay(graphics, font, overlay, slotX, slotY);
		}
	}

	private static void drawOverlay(GuiGraphics graphics, Font font, SlotOverlay overlay, int slotX, int slotY) {
		String text = overlay.text();
		float scale = overlay.scale();
		float width = font.width(text) * scale;
		float height = font.lineHeight * scale;

		float px;
		float py;
		switch (overlay.anchor()) {
			case TOP_RIGHT -> {
				px = slotX + 15 - width;
				py = slotY + 1;
			}
			case BOTTOM_LEFT -> {
				px = slotX + 1;
				py = slotY + 15 - height;
			}
			case BOTTOM_RIGHT -> {
				px = slotX + 15 - width;
				py = slotY + 15 - height;
			}
			case CENTER -> {
				px = slotX + 8 - width / 2f;
				py = slotY + 8 - height / 2f;
			}
			default -> { // TOP_LEFT
				px = slotX + 1;
				py = slotY + 1;
			}
		}

		Component component = Component.literal(text).withStyle(style -> style.withBold(overlay.bold()));

		Matrix3x2fStack pose = graphics.pose();
		pose.pushMatrix();
		pose.translate(px, py);
		pose.scale(scale, scale);
		if (overlay.outline()) {
			// Black 1px outline in all 8 directions, then the colored glyph on top (no drop shadow).
			int outlineColor = 0xFF000000;
			for (int dx = -1; dx <= 1; dx++) {
				for (int dy = -1; dy <= 1; dy++) {
					if (dx != 0 || dy != 0) {
						graphics.drawString(font, component, dx, dy, outlineColor, false);
					}
				}
			}
			graphics.drawString(font, component, 0, 0, overlay.color(), false);
		} else {
			graphics.drawString(font, component, 0, 0, overlay.color(), true);
		}
		pose.popMatrix();
	}
}
