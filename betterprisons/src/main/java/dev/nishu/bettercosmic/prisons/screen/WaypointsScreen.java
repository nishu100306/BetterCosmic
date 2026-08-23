package dev.nishu.bettercosmic.prisons.screen;

import dev.nishu.bettercosmic.prisons.client.BetterPrisonsClient;
import dev.nishu.bettercosmic.prisons.waypoint.CustomWaypoint;
import dev.nishu.bettercosmic.shared.ui.core.Theme;
import dev.nishu.bettercosmic.shared.ui.render.RenderUtils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * Management screen for user-created waypoints — a list mode (per-world scrollable table with inline
 * enable / edit / delete) and an edit mode (name, coords, color, opacity, per-marker scales). Rebuilt
 * on the shared UI primitives ({@link Theme} palette, {@link RenderUtils}) with vanilla
 * {@link EditBox}/{@link Button} widgets, per the port plan — BetterPrisons' bespoke {@code ui/custom}
 * widget framework is not carried over. Backs onto the shared {@code WaypointManager}. Opened by the
 * {@code waypoints} keybind and the {@code /bpwaypoints} dev command.
 */
public class WaypointsScreen extends Screen {

	private static final int PANEL_W = 540;
	private static final int ROW_H = 24;
	private static final int LIST_TOP = 58;
	private static final int BOTTOM_BAR_H = 36;

	private static final String[] PRESET_NAMES = {
			"White", "Orange", "Magenta", "Light Blue", "Yellow", "Lime",
			"Pink", "Gray", "Light Gray", "Cyan", "Purple", "Blue",
			"Brown", "Green", "Red", "Black"};
	private static final int[] PRESET_COLORS = {
			0xF9FFFE, 0xF9801D, 0xC74EBD, 0x3AB3DA, 0xFED83D, 0x80C71F,
			0xF38BAA, 0x474F52, 0x9D9D97, 0x169C9C, 0x8932B8, 0x3C44AA,
			0x835432, 0x5E7C16, 0xB02E26, 0x1D1D21};

	private static final int SWATCH_SIZE = 14;
	private static final int SWATCH_GAP = 3;
	private static final int SWATCH_COLS = 4;
	private static final int FORM_W = 300;
	private static final int FORM_H = 300;

	private int scrollOffset = 0;
	private int maxScroll = 0;

	private boolean editMode = false;
	private int editIndex = -1;
	private int pendingX, pendingY, pendingZ;
	private int pendingColor = 0xFFFFFF;

	private String viewedWorld;

	private EditBox nameField;
	private EditBox xField, yField, zField;
	private EditBox opacityField;
	private EditBox onScaleField, offScaleField;
	private EditBox hexField;

	public WaypointsScreen() {
		super(Component.literal("Waypoints"));
		this.viewedWorld = BetterPrisonsClient.waypointManager.getCurrentWorld();
	}

	private void rebuild() {
		clearWidgets();
		init();
	}

	@Override
	protected void init() {
		if (editMode) {
			initEditForm();
		} else {
			initListButtons();
		}
	}

	// ---- List mode ----

	private void initListButtons() {
		int panelX = (width - PANEL_W) / 2;
		int btnY = height - BOTTOM_BAR_H + 8;

		addRenderableWidget(Button.builder(Component.literal("Add Waypoint"), b -> {
			if (minecraft != null && minecraft.player != null) {
				BlockPos pos = minecraft.player.blockPosition();
				pendingX = pos.getX();
				pendingY = pos.getY();
				pendingZ = pos.getZ();
			} else {
				pendingX = pendingY = pendingZ = 0;
			}
			editIndex = -1;
			editMode = true;
			rebuild();
		}).bounds(panelX, btnY, 130, 20).build());

		addRenderableWidget(Button.builder(Component.literal("Delete World"), b -> {
			String worldToDelete = viewedWorld;
			minecraft.setScreen(new ConfirmScreen(confirmed -> {
				if (confirmed) {
					BetterPrisonsClient.waypointManager.removeWorld(worldToDelete);
					List<String> remaining = BetterPrisonsClient.waypointManager.getWorlds();
					String current = BetterPrisonsClient.waypointManager.getCurrentWorld();
					if (remaining.contains(current) || remaining.isEmpty()) {
						viewedWorld = current;
					} else {
						viewedWorld = remaining.get(0);
					}
					scrollOffset = 0;
				}
				minecraft.setScreen(WaypointsScreen.this);
			}, Component.literal("Delete World?"),
					Component.literal("Delete all waypoints for \"" + worldToDelete + "\"? This cannot be undone.")));
		}).bounds(panelX + PANEL_W - 196, btnY, 110, 20).build());

		addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose())
				.bounds(panelX + PANEL_W - 80, btnY, 80, 20).build());

		List<String> worlds = worldsList();
		if (!worlds.contains(viewedWorld)) {
			viewedWorld = worlds.get(0);
		}
		int listH = height - LIST_TOP - BOTTOM_BAR_H;
		maxScroll = Math.max(0, waypoints().size() * ROW_H - listH);
		scrollOffset = Math.min(scrollOffset, maxScroll);
	}

	// ---- Edit mode ----

	private void initEditForm() {
		int formX = (width - FORM_W) / 2;
		int formY = (height - FORM_H) / 2;

		nameField = editBox(formX, formY + 26, FORM_W, 32);
		xField = editBox(formX, formY + 68, 90, 8);
		yField = editBox(formX + 95, formY + 68, 90, 8);
		zField = editBox(formX + 190, formY + 68, 90, 8);
		hexField = editBox(formX + 22, formY + 96, 80, 7);
		opacityField = editBox(formX + 130, formY + 205, 45, 3);
		onScaleField = editBox(formX + 130, formY + 226, 45, 5);
		offScaleField = editBox(formX + 130, formY + 247, 45, 5);

		if (editIndex >= 0) {
			List<CustomWaypoint> wps = waypoints();
			if (editIndex < wps.size()) {
				CustomWaypoint wp = wps.get(editIndex);
				nameField.setValue(wp.name);
				xField.setValue(String.valueOf(wp.x));
				yField.setValue(String.valueOf(wp.y));
				zField.setValue(String.valueOf(wp.z));
				opacityField.setValue(String.valueOf(wp.opacity));
				onScaleField.setValue(String.valueOf(wp.onScreenScale));
				offScaleField.setValue(String.valueOf(wp.offScreenScale));
				pendingColor = wp.color & 0xFFFFFF;
			}
		} else {
			xField.setValue(String.valueOf(pendingX));
			yField.setValue(String.valueOf(pendingY));
			zField.setValue(String.valueOf(pendingZ));
			opacityField.setValue(String.valueOf(BetterPrisonsClient.config.customWaypointDefaultOpacity));
			onScaleField.setValue(String.valueOf(BetterPrisonsClient.config.customWaypointOnScreenScale));
			offScaleField.setValue(String.valueOf(BetterPrisonsClient.config.customWaypointOffScreenScale));
			pendingColor = 0xFFFFFF;
		}
		hexField.setValue(String.format("%06X", pendingColor & 0xFFFFFF));
		hexField.setResponder(s -> {
			try {
				pendingColor = (int) Long.parseLong(s.replace("#", "").trim(), 16) & 0xFFFFFF;
			} catch (NumberFormatException ignored) {
				// keep the previous color until a valid hex is typed
			}
		});

		addRenderableWidget(Button.builder(Component.literal("Save"), b -> saveEdit())
				.bounds(formX, formY + 268, 145, 20).build());
		addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> cancelEdit())
				.bounds(formX + 150, formY + 268, 150, 20).build());
	}

	private EditBox editBox(int x, int y, int w, int maxLen) {
		EditBox box = new EditBox(this.font, x, y, w, 18, Component.empty());
		box.setMaxLength(maxLen);
		return addRenderableWidget(box);
	}

	private void saveEdit() {
		if (nameField == null || nameField.getValue().trim().isEmpty()) {
			return;
		}
		int x, y, z;
		try {
			x = Integer.parseInt(xField.getValue().trim());
			y = Integer.parseInt(yField.getValue().trim());
			z = Integer.parseInt(zField.getValue().trim());
		} catch (NumberFormatException e) {
			return;
		}
		int opacity = clampInt(opacityField.getValue(), 255, 0, 255);
		float onScale = clampFloat(onScaleField.getValue(), 1.0f);
		float offScale = clampFloat(offScaleField.getValue(), 1.0f);

		CustomWaypoint wp = new CustomWaypoint(nameField.getValue().trim(), x, y, z, pendingColor & 0xFFFFFF);
		wp.opacity = opacity;
		wp.onScreenScale = onScale;
		wp.offScreenScale = offScale;
		if (editIndex >= 0) {
			List<CustomWaypoint> wps = waypoints();
			if (editIndex < wps.size()) {
				wp.enabled = wps.get(editIndex).enabled;
			}
			BetterPrisonsClient.waypointManager.update(editIndex, wp, viewedWorld);
		} else {
			BetterPrisonsClient.waypointManager.add(wp);
		}
		cancelEdit();
	}

	private void cancelEdit() {
		editMode = false;
		editIndex = -1;
		rebuild();
	}

	private static int clampInt(String s, int def, int min, int max) {
		try {
			return Math.max(min, Math.min(max, Integer.parseInt(s.trim())));
		} catch (NumberFormatException e) {
			return def;
		}
	}

	private static float clampFloat(String s, float def) {
		try {
			return Math.max(0.1f, Math.min(10f, Float.parseFloat(s.trim())));
		} catch (NumberFormatException e) {
			return def;
		}
	}

	// ---- Render ----

	@Override
	public void render(GuiGraphics ctx, int mouseX, int mouseY, float delta) {
		ctx.fill(0, 0, width, height, Theme.ground);

		String title = editMode ? (editIndex >= 0 ? "Edit Waypoint" : "New Waypoint") : "Waypoints";
		RenderUtils.textCentered(ctx, title, width / 2, 16, Theme.text);

		if (!editMode) {
			renderList(ctx, mouseX, mouseY);
		} else {
			renderEditForm(ctx, mouseX, mouseY);
		}
		super.render(ctx, mouseX, mouseY, delta);
	}

	private void renderList(GuiGraphics ctx, int mouseX, int mouseY) {
		int panelX = (width - PANEL_W) / 2;
		int listH = height - LIST_TOP - BOTTOM_BAR_H;

		RenderUtils.panel(ctx, panelX - 4, LIST_TOP - 6, PANEL_W + 8, listH + 10, Theme.surface, Theme.line);

		// World selector: "◂ World: <name> ▸" (arrows cycle through worlds).
		List<String> worlds = worldsList();
		String worldText = "World: " + viewedWorld;
		int wtX = panelX;
		int wtY = LIST_TOP - 34;
		if (worlds.size() > 1) {
			RenderUtils.triLeft(ctx, wtX, wtY, 6, 8, Theme.accent);
			ctx.drawString(this.font, Component.literal(worldText), wtX + 12, wtY, Theme.text, false);
			int rightArrowX = wtX + 12 + this.font.width(worldText) + 4;
			RenderUtils.triRight(ctx, rightArrowX, wtY, 6, 8, Theme.accent);
		} else {
			ctx.drawString(this.font, Component.literal(worldText), wtX, wtY, Theme.muted, false);
		}

		ctx.drawString(this.font, Component.literal("Name"), panelX + 24, LIST_TOP - 17, Theme.muted, false);
		ctx.drawString(this.font, Component.literal("Coords"), panelX + 200, LIST_TOP - 17, Theme.muted, false);
		RenderUtils.hLine(ctx, panelX, LIST_TOP - 4, PANEL_W, Theme.line);

		RenderUtils.pushScissor(ctx, panelX, LIST_TOP, PANEL_W, listH);
		List<CustomWaypoint> wps = waypoints();
		for (int i = 0; i < wps.size(); i++) {
			CustomWaypoint wp = wps.get(i);
			int rowY = LIST_TOP + i * ROW_H - scrollOffset;
			if (rowY + ROW_H < LIST_TOP || rowY > LIST_TOP + listH) {
				continue;
			}
			if (i % 2 == 0) {
				ctx.fill(panelX, rowY, panelX + PANEL_W, rowY + ROW_H, 0x15FFFFFF);
			}
			if (mouseY >= rowY && mouseY < rowY + ROW_H && mouseX >= panelX && mouseX <= panelX + PANEL_W) {
				ctx.fill(panelX, rowY, panelX + PANEL_W, rowY + ROW_H, Theme.surfaceHover);
			}
			ctx.fill(panelX + 5, rowY + 5, panelX + 19, rowY + 19, 0xFF000000 | (wp.color & 0xFFFFFF));
			ctx.drawString(this.font, Component.literal(wp.name), panelX + 24, rowY + 8,
					wp.enabled ? Theme.text : Theme.faint, false);
			ctx.drawString(this.font, Component.literal(wp.x + ", " + wp.y + ", " + wp.z),
					panelX + 200, rowY + 8, Theme.muted, false);

			int rx = panelX + PANEL_W;
			ctx.drawString(this.font, Component.literal(wp.enabled ? "§aON " : "§cOFF"), rx - 102, rowY + 8, Theme.text, false);
			ctx.drawString(this.font, Component.literal("§7Edit"), rx - 60, rowY + 8, Theme.text, false);
			ctx.drawString(this.font, Component.literal("§c✕"), rx - 16, rowY + 8, Theme.text, false);
		}
		RenderUtils.popScissor(ctx);

		if (maxScroll > 0 && !wps.isEmpty()) {
			int sbH = listH - 4;
			int thumbH = Math.max(20, (int) (sbH * (float) listH / (wps.size() * ROW_H)));
			int thumbY = LIST_TOP + 2 + (int) ((sbH - thumbH) * (float) scrollOffset / maxScroll);
			ctx.fill(panelX + PANEL_W + 4, LIST_TOP + 2, panelX + PANEL_W + 8, LIST_TOP + 2 + sbH, 0x30FFFFFF);
			ctx.fill(panelX + PANEL_W + 4, thumbY, panelX + PANEL_W + 8, thumbY + thumbH, 0x90FFFFFF);
		}

		if (wps.isEmpty()) {
			RenderUtils.textCentered(ctx, "No waypoints for this world.", width / 2, LIST_TOP + listH / 2 - 10, Theme.faint);
			RenderUtils.textCentered(ctx, "Use 'Add Waypoint' below to add one.", width / 2, LIST_TOP + listH / 2 + 4, Theme.faint);
		}
	}

	private void renderEditForm(GuiGraphics ctx, int mouseX, int mouseY) {
		int formX = (width - FORM_W) / 2;
		int formY = (height - FORM_H) / 2;
		RenderUtils.panel(ctx, formX - 12, formY - 12, FORM_W + 24, FORM_H, Theme.surface, Theme.line);

		ctx.drawString(this.font, Component.literal("Name:"), formX, formY + 14, Theme.muted, false);
		ctx.drawString(this.font, Component.literal("X:"), formX, formY + 56, Theme.muted, false);
		ctx.drawString(this.font, Component.literal("Y:"), formX + 95, formY + 56, Theme.muted, false);
		ctx.drawString(this.font, Component.literal("Z:"), formX + 190, formY + 56, Theme.muted, false);
		ctx.drawString(this.font, Component.literal("Color:"), formX, formY + 99, Theme.muted, false);
		ctx.drawString(this.font, Component.literal("Opacity (0-255):"), formX, formY + 210, Theme.muted, false);
		ctx.drawString(this.font, Component.literal("Scale on-screen:"), formX, formY + 231, Theme.muted, false);
		ctx.drawString(this.font, Component.literal("Scale off-screen:"), formX, formY + 252, Theme.muted, false);

		// Current-color preview swatch next to the hex field.
		ctx.fill(formX + 106, formY + 96, formX + 124, formY + 114, 0xFF000000 | (pendingColor & 0xFFFFFF));
		RenderUtils.outline(ctx, formX + 106, formY + 96, 18, 18, Theme.line);

		renderColorSwatches(ctx, mouseX, mouseY, formX, formY + 120);
	}

	private void renderColorSwatches(GuiGraphics ctx, int mouseX, int mouseY, int startX, int startY) {
		ctx.drawString(this.font, Component.literal("Preset:"), startX, startY, Theme.muted, false);
		int gridY = startY + 12;
		int hovered = -1;
		for (int i = 0; i < PRESET_COLORS.length; i++) {
			int sx = startX + (i % SWATCH_COLS) * (SWATCH_SIZE + SWATCH_GAP);
			int sy = gridY + (i / SWATCH_COLS) * (SWATCH_SIZE + SWATCH_GAP);
			boolean over = mouseX >= sx && mouseX < sx + SWATCH_SIZE && mouseY >= sy && mouseY < sy + SWATCH_SIZE;
			if (over) {
				hovered = i;
			}
			ctx.fill(sx, sy, sx + SWATCH_SIZE, sy + SWATCH_SIZE, 0xFF000000 | (PRESET_COLORS[i] & 0xFFFFFF));
			RenderUtils.outline(ctx, sx, sy, SWATCH_SIZE, SWATCH_SIZE, over ? 0xFFFFFFFF : Theme.line);
		}
		if (hovered >= 0) {
			int gridW = SWATCH_COLS * (SWATCH_SIZE + SWATCH_GAP) - SWATCH_GAP;
			ctx.drawString(this.font, Component.literal(PRESET_NAMES[hovered]),
					startX + gridW + 8, gridY + 4, 0xFF000000 | (PRESET_COLORS[hovered] & 0xFFFFFF), false);
		}
	}

	private int getSwatchAt(double mouseX, double mouseY, int startX, int startY) {
		int gridY = startY + 12;
		for (int i = 0; i < PRESET_COLORS.length; i++) {
			int sx = startX + (i % SWATCH_COLS) * (SWATCH_SIZE + SWATCH_GAP);
			int sy = gridY + (i / SWATCH_COLS) * (SWATCH_SIZE + SWATCH_GAP);
			if (mouseX >= sx && mouseX < sx + SWATCH_SIZE && mouseY >= sy && mouseY < sy + SWATCH_SIZE) {
				return i;
			}
		}
		return -1;
	}

	// ---- Input ----

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		double mx = event.x(), my = event.y();

		if (editMode) {
			int formX = (width - FORM_W) / 2;
			int formY = (height - FORM_H) / 2;
			int idx = getSwatchAt(mx, my, formX, formY + 120);
			if (idx >= 0) {
				pendingColor = PRESET_COLORS[idx];
				hexField.setValue(String.format("%06X", pendingColor & 0xFFFFFF));
				return true;
			}
			return super.mouseClicked(event, doubleClick);
		}

		int panelX = (width - PANEL_W) / 2;

		// World cycle arrows.
		List<String> worlds = worldsList();
		if (worlds.size() > 1) {
			int wtY = LIST_TOP - 34;
			String worldText = "World: " + viewedWorld;
			if (RenderUtils.hit(mx, my, panelX, wtY, 8, 8)) {
				cycleWorld(worlds, -1);
				return true;
			}
			int rightArrowX = panelX + 12 + this.font.width(worldText) + 4;
			if (RenderUtils.hit(mx, my, rightArrowX, wtY, 8, 8)) {
				cycleWorld(worlds, 1);
				return true;
			}
		}

		// List row actions.
		int listH = height - LIST_TOP - BOTTOM_BAR_H;
		if (mx >= panelX && mx <= panelX + PANEL_W && my >= LIST_TOP && my < LIST_TOP + listH) {
			int i = (int) ((my + scrollOffset - LIST_TOP) / ROW_H);
			List<CustomWaypoint> wps = waypoints();
			if (i >= 0 && i < wps.size()) {
				int rx = panelX + PANEL_W;
				if (mx >= rx - 20 && mx <= rx) {
					BetterPrisonsClient.waypointManager.remove(i, viewedWorld);
					rebuild();
					return true;
				}
				if (mx >= rx - 65 && mx < rx - 20) {
					editIndex = i;
					editMode = true;
					rebuild();
					return true;
				}
				if (mx >= rx - 110 && mx < rx - 65) {
					wps.get(i).enabled = !wps.get(i).enabled;
					BetterPrisonsClient.waypointManager.save();
					return true;
				}
			}
		}
		return super.mouseClicked(event, doubleClick);
	}

	private void cycleWorld(List<String> worlds, int dir) {
		int idx = Math.max(0, worlds.indexOf(viewedWorld));
		viewedWorld = worlds.get((idx + dir + worlds.size()) % worlds.size());
		scrollOffset = 0;
		int listH = height - LIST_TOP - BOTTOM_BAR_H;
		maxScroll = Math.max(0, waypoints().size() * ROW_H - listH);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (!editMode) {
			scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - (int) (scrollY * ROW_H)));
		}
		return true;
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (editMode && event.key() == GLFW.GLFW_KEY_ESCAPE) {
			cancelEdit();
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public boolean charTyped(CharacterEvent event) {
		return super.charTyped(event);
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return !editMode;
	}

	// ---- Helpers ----

	private List<String> worldsList() {
		List<String> worlds = new ArrayList<>(BetterPrisonsClient.waypointManager.getWorlds());
		if (worlds.isEmpty()) {
			worlds.add(viewedWorld);
		}
		return worlds;
	}

	private List<CustomWaypoint> waypoints() {
		return BetterPrisonsClient.waypointManager.getAll(viewedWorld);
	}
}
