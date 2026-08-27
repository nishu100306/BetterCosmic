package dev.nishu.bettercosmic.shared.hud;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * Drag-and-drop editor for repositioning the registered {@link BaseHud}s. Opened from the config
 * screen's "HUD editor" button. Each draggable HUD renders live with a bounding box (green normally,
 * yellow while dragging); "Reset Positions" restores every HUD to its default, and closing persists
 * each HUD via its registered callback.
 *
 * <p>Ported and de-hardcoded from BetterPrisons' {@code HudEditorScreen} (Yarn → Mojang) — it now
 * iterates {@link HudRegistry} instead of a fixed HUD list.
 */
public final class HudEditorScreen extends Screen {

	private static final int DEFAULT_W = 100;
	private static final int DEFAULT_H = 50;

	private final Screen parent;
	private BaseHud draggedHud = null;
	private int dragOffsetX = 0;
	private int dragOffsetY = 0;
	private boolean isDragging = false;

	public HudEditorScreen(Screen parent) {
		super(Component.literal("HUD Editor"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		this.addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose())
				.bounds(this.width / 2 - 100, this.height - 30, 200, 20).build());
		this.addRenderableWidget(Button.builder(Component.literal("Reset Positions"), b -> resetPositions())
				.bounds(this.width / 2 - 100, this.height - 55, 200, 20).build());
	}

	@Override
	public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
		super.render(context, mouseX, mouseY, delta);

		context.drawCenteredString(this.font, this.title, this.width / 2, 10, 0xFFFFFFFF);
		context.drawCenteredString(this.font, "Click and drag HUD elements to reposition them",
				this.width / 2, 25, 0xFFAAAAAA);

		for (HudRegistry.Entry entry : HudRegistry.entries()) {
			if (!entry.draggable || !entry.hud.enabled) {
				continue;
			}
			BaseHud hud = entry.hud;
			hud.render(context, this.minecraft);

			int w = hud.getWidth() > 0 ? hud.getWidth() : DEFAULT_W;
			int h = hud.getHeight() > 0 ? hud.getHeight() : DEFAULT_H;
			int borderColor = (hud == draggedHud) ? 0xFFFFFF00 : 0x8000FF00;
			drawBorder(context, hud.x - 2, hud.y - 2, w + 4, h + 4, borderColor);
		}
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (event.button() == 0) {
			double mouseX = event.x();
			double mouseY = event.y();
			for (HudRegistry.Entry entry : HudRegistry.entries()) {
				if (!entry.draggable || !entry.hud.enabled) {
					continue;
				}
				BaseHud hud = entry.hud;
				int w = hud.getWidth() > 0 ? hud.getWidth() : DEFAULT_W;
				int h = hud.getHeight() > 0 ? hud.getHeight() : DEFAULT_H;
				if (mouseX >= hud.x && mouseX <= hud.x + w && mouseY >= hud.y && mouseY <= hud.y + h) {
					draggedHud = hud;
					dragOffsetX = (int) (mouseX - hud.x);
					dragOffsetY = (int) (mouseY - hud.y);
					isDragging = true;
					return true;
				}
			}
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
		if (isDragging && draggedHud != null) {
			draggedHud.x = (int) (event.x() - dragOffsetX);
			draggedHud.y = (int) (event.y() - dragOffsetY);
			int w = draggedHud.getWidth() > 0 ? draggedHud.getWidth() : DEFAULT_W;
			int h = draggedHud.getHeight() > 0 ? draggedHud.getHeight() : DEFAULT_H;
			draggedHud.x = Math.max(0, Math.min(draggedHud.x, this.width - w));
			draggedHud.y = Math.max(0, Math.min(draggedHud.y, this.height - h));
			return true;
		}
		return super.mouseDragged(event, dragX, dragY);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		if (event.button() == 0 && isDragging) {
			isDragging = false;
			draggedHud = null;
			return true;
		}
		return super.mouseReleased(event);
	}

	private void resetPositions() {
		for (HudRegistry.Entry entry : HudRegistry.entries()) {
			entry.hud.resetToDefault();
			entry.persist.run();
		}
	}

	@Override
	public void onClose() {
		// Persist every HUD's current position/scale, then return to the parent screen.
		for (HudRegistry.Entry entry : HudRegistry.entries()) {
			entry.persist.run();
		}
		if (this.minecraft != null) {
			this.minecraft.setScreen(parent);
		}
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	private static void drawBorder(GuiGraphics ctx, int x, int y, int width, int height, int color) {
		ctx.fill(x, y, x + width, y + 1, color);
		ctx.fill(x, y + height - 1, x + width, y + height, color);
		ctx.fill(x, y, x + 1, y + height, color);
		ctx.fill(x + width - 1, y, x + width, y + height, color);
	}
}
