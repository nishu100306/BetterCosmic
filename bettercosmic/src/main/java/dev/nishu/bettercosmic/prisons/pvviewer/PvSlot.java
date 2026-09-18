package dev.nishu.bettercosmic.prisons.pvviewer;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/**
 * Draws a single vanilla container slot background, so the viewer and the preview grids look like a
 * real Minecraft inventory (and scale cleanly when drawn under a {@code pose().scale(...)}).
 *
 * <p>Uses the vanilla {@code generic_54} chest texture: every slot cell in it is identical, an
 * {@value #CELL}×{@value #CELL} region whose top-left sits at texture ({@value #U},{@value #V}); the
 * 16×16 item area is inset by 1px, so items render at {@code (x+1, y+1)}.
 */
public final class PvSlot {

	/** Vanilla slot cell size (item is 16px inset by 1px on each side). */
	public static final int CELL = 18;
	public static final int ITEM_INSET = 1;

	private static final Identifier TEXTURE =
			Identifier.fromNamespaceAndPath("minecraft", "textures/gui/container/generic_54.png");
	private static final int U = 7;
	private static final int V = 17;
	private static final int TEX_SIZE = 256;

	private PvSlot() {}

	/** Draws one vanilla slot cell at ({@code x},{@code y}). Render the item at {@code (x+1, y+1)}. */
	public static void render(GuiGraphics g, int x, int y) {
		g.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, U, V, CELL, CELL, TEX_SIZE, TEX_SIZE);
	}
}
