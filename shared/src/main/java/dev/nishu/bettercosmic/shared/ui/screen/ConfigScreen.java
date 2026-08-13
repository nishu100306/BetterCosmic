package dev.nishu.bettercosmic.shared.ui.screen;

import dev.nishu.bettercosmic.shared.ui.core.Theme;
import dev.nishu.bettercosmic.shared.ui.model.ConfigPanel;
import dev.nishu.bettercosmic.shared.ui.model.ConfigRegistry;
import dev.nishu.bettercosmic.shared.ui.render.RenderUtils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/**
 * Host screen for the config UI: a centered translucent window over the blurred world, with a
 * branded header, the paginated {@link PanelGrid}, and a footer {@link Pager} + Done button. Opening
 * a real panel is wired in Phase 2 (the {@link FeaturePopup}); for now a click is routed to
 * {@link #openPanel}, which no-ops until popups exist.
 *
 * <p>Render passes run in order — window → grid → {@link OverlayLayer} → tooltip — so floating
 * overlays (dropdowns, color picker) always paint on top and take input first.
 */
public final class ConfigScreen extends Screen {

	private static final int W = 420;
	private static final int H = 246;
	private static final int PAD = 13;
	private static final int HEADER = 26;
	private static final int FOOTER = 26;

	private final Screen parent;
	private final String subtitle;

	private final OverlayLayer overlay = new OverlayLayer();
	private PanelGrid grid;
	private Pager pager;

	private int x0, y0;
	private int resetX, resetW, doneX, doneW; // header/footer button hit-rects (y derived)

	public ConfigScreen(Screen parent, String subtitle) {
		super(Component.literal("BetterCosmic"));
		this.parent = parent;
		this.subtitle = subtitle == null ? "" : subtitle;
	}

	@Override
	protected void init() {
		Theme.load();
		x0 = (this.width - W) / 2;
		y0 = (this.height - H) / 2;

		grid = new PanelGrid(ConfigRegistry.panels(), this::openPanel);
		grid.layout(x0 + PAD, y0 + HEADER + PAD, W - 2 * PAD, H - HEADER - FOOTER - 2 * PAD);
		pager = new Pager(() -> grid.prevPage(), () -> grid.nextPage());

		resetW = RenderUtils.textWidth("Reset all") + 12;
		resetX = x0 + W - PAD - resetW;
		doneW = RenderUtils.textWidth("Done") + 16;
		doneX = x0 + W - PAD - doneW;
	}

	private void openPanel(ConfigPanel panel) {
		// Phase 2 opens the FeaturePopup here. Until then, real-panel clicks are a no-op.
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float dt) {
		this.renderBackground(g, mouseX, mouseY, dt); // blurred world + dim

		// window
		RenderUtils.rect(g, x0, y0, W, H, Theme.surface);
		RenderUtils.hLine(g, x0, y0 + HEADER, W, Theme.line);
		RenderUtils.hLine(g, x0, y0 + H - FOOTER, W, Theme.line);
		RenderUtils.outline(g, x0, y0, W, H, Theme.line);

		renderHeader(g, mouseX, mouseY);

		grid.render(g, mouseX, mouseY, dt);

		renderFooter(g, mouseX, mouseY);

		overlay.render(g, mouseX, mouseY, dt);
	}

	private void renderHeader(GuiGraphics g, int mouseX, int mouseY) {
		int cy = y0 + HEADER / 2;
		int textY = cy - 4;

		// accent diamond mark
		diamond(g, x0 + PAD + 3, cy, 5, Theme.accent);

		int nameX = x0 + PAD + 12;
		RenderUtils.text(g, "BetterCosmic", nameX, textY, Theme.text);
		if (!subtitle.isEmpty()) {
			int subX = nameX + RenderUtils.textWidth("BetterCosmic") + 6;
			RenderUtils.text(g, subtitle.toUpperCase(), subX, textY, Theme.muted);
		}

		// "Reset all" (faint → text on hover). No options to reset yet (wired in Phase 6).
		boolean rHover = hit(mouseX, mouseY, resetX, y0 + 6, resetW, 14);
		RenderUtils.text(g, "Reset all", resetX + 6, textY, rHover ? Theme.text : Theme.faint);
	}

	private void renderFooter(GuiGraphics g, int mouseX, int mouseY) {
		int cy = y0 + H - FOOTER / 2;
		pager.update(grid.page(), grid.pageCount());
		pager.render(g, x0 + PAD, cy, mouseX, mouseY);

		int doneY = cy - 8;
		boolean dHover = hit(mouseX, mouseY, doneX, doneY, doneW, 16);
		RenderUtils.panel(g, doneX, doneY, doneW, 16, Theme.surface, dHover ? Theme.accent : Theme.line);
		RenderUtils.textCentered(g, "Done", doneX + doneW / 2, doneY + 4, dHover ? Theme.text : Theme.muted);
	}

	// Input arrives as 1.21.11 event objects; we unpack to primitives and forward to the element
	// tree (which uses simple, mapping-independent signatures).

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		double mx = event.x();
		double my = event.y();
		int button = event.button();
		if (overlay.mouseClicked(mx, my, button)) {
			return true;
		}
		if (grid.mouseClicked(mx, my, button)) {
			return true;
		}
		if (pager.mouseClicked(mx, my, button)) {
			return true;
		}
		if (button == 0 && hit(mx, my, doneX, y0 + H - FOOTER / 2 - 8, doneW, 16)) {
			onClose();
			return true;
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		if (overlay.mouseReleased(event.x(), event.y(), event.button())) {
			return true;
		}
		return super.mouseReleased(event);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
		if (overlay.mouseDragged(event.x(), event.y(), event.button(), dragX, dragY)) {
			return true;
		}
		return super.mouseDragged(event, dragX, dragY);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (overlay.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) {
			return true;
		}
		// wheel over the grid pages
		if (scrollY < 0) {
			grid.nextPage();
		} else if (scrollY > 0) {
			grid.prevPage();
		}
		return true;
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (overlay.keyPressed(event.key(), event.scancode(), event.modifiers())) {
			return true;
		}
		switch (event.key()) {
			case GLFW.GLFW_KEY_LEFT -> {
				grid.prevPage();
				return true;
			}
			case GLFW.GLFW_KEY_RIGHT -> {
				grid.nextPage();
				return true;
			}
			default -> {
				return super.keyPressed(event);
			}
		}
	}

	@Override
	public boolean charTyped(CharacterEvent event) {
		if (overlay.charTyped((char) event.codepoint(), event.modifiers())) {
			return true;
		}
		return super.charTyped(event);
	}

	@Override
	public void onClose() {
		this.minecraft.setScreen(parent);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	// ---- helpers ----

	private static boolean hit(double mx, double my, int x, int y, int w, int h) {
		return mx >= x && mx < x + w && my >= y && my < y + h;
	}

	private static void diamond(GuiGraphics g, int cx, int cy, int r, int color) {
		for (int i = -r; i <= r; i++) {
			int half = r - Math.abs(i);
			g.fill(cx - half, cy + i, cx + half + 1, cy + i + 1, color);
		}
	}
}
