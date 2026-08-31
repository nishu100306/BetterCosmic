package dev.nishu.bettercosmic.shared.ui.screen;

import dev.nishu.bettercosmic.shared.config.SharedConfig;
import dev.nishu.bettercosmic.shared.hud.HudEditorScreen;
import dev.nishu.bettercosmic.shared.hud.HudRegistry;
import dev.nishu.bettercosmic.shared.server.Network;
import dev.nishu.bettercosmic.shared.server.ServerContext;
import dev.nishu.bettercosmic.shared.ui.core.OverlayLayer;
import dev.nishu.bettercosmic.shared.ui.core.Theme;
import dev.nishu.bettercosmic.shared.ui.core.UiSounds;
import dev.nishu.bettercosmic.shared.ui.model.ConfigPanel;
import dev.nishu.bettercosmic.shared.ui.model.ConfigRegistry;
import dev.nishu.bettercosmic.shared.ui.model.Option;
import dev.nishu.bettercosmic.shared.ui.model.OptionGroup;
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
 * branded header, the paginated {@link PanelGrid}, and a footer {@link Pager} + Done button. Clicking
 * a real panel routes to {@link #openPanel}, which opens its {@link FeaturePopup} in the
 * {@link OverlayLayer}.
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

	private final OverlayLayer overlay = new OverlayLayer();
	private PanelGrid grid;
	private Pager pager;

	/** The network profile whose panels are shown (plus global panels). See {@link #initialProfile()}. */
	private Network profile;

	private int x0, y0;
	private int resetX, resetW, doneX, doneW; // header/footer button hit-rects (y derived)
	private int hudX, hudW; // footer "HUD editor" button (only shown when HUDs are registered)
	private int prisonsX, prisonsW, skyX, skyW; // header profile-selector hit-rects (y derived)
	private boolean resetArmed; // "Reset all" needs a confirming second click

	public ConfigScreen(Screen parent) {
		super(Component.literal("BetterCosmic"));
		this.parent = parent;
		this.profile = initialProfile();
	}

	/**
	 * The profile to open on: the connected network if recognised, else the last one the player viewed
	 * (persisted in {@link SharedConfig#lastConfigProfile}), else {@link Network#PRISONS}. This lets the
	 * screen default to where you are while still allowing off-server editing of either profile.
	 */
	private static Network initialProfile() {
		Network detected = ServerContext.detected();
		if (detected != null) {
			return detected;
		}
		String saved = SharedConfig.get().lastConfigProfile;
		if (saved != null) {
			try {
				return Network.valueOf(saved);
			} catch (IllegalArgumentException ignored) {
				// stale/renamed value — fall through to the default
			}
		}
		return Network.PRISONS;
	}

	@Override
	protected void init() {
		Theme.setProfile(profile); // pin accent to the viewed profile (orange = Prisons, yellow = Sky)
		overlay.clear(); // drop any popup left open across a resize
		x0 = (this.width - W) / 2;
		y0 = (this.height - H) / 2;

		buildGrid();

		// Header profile selector ("Prisons" / "Sky"), just right of the brand name.
		int selX = x0 + PAD + 12 + RenderUtils.textWidth("BetterCosmic") + 10;
		prisonsX = selX;
		prisonsW = RenderUtils.textWidth(Network.PRISONS.displayName());
		skyX = prisonsX + prisonsW + 12; // gap holds the divider dot
		skyW = RenderUtils.textWidth(Network.SKY.displayName());

		resetW = RenderUtils.textWidth("Reset all") + 12;
		resetX = x0 + W - PAD - resetW;
		doneW = RenderUtils.textWidth("Done") + 16;
		doneX = x0 + W - PAD - doneW;

		// "HUD editor" sits just left of Done, and only when a mod has registered HUDs.
		hudW = RenderUtils.textWidth("HUD editor") + 16;
		hudX = doneX - 6 - hudW;
	}

	/** (Re)builds the panel grid for the current {@link #profile}, scoping to its panels plus globals. */
	private void buildGrid() {
		grid = new PanelGrid(ConfigRegistry.panels(profile), this::openPanel);
		grid.layout(x0 + PAD, y0 + HEADER + PAD, W - 2 * PAD, H - HEADER - FOOTER - 2 * PAD);
		pager = new Pager(() -> grid.prevPage(), () -> grid.nextPage());
	}

	/** Switches the viewed profile, persisting it and rebuilding the grid from page one. */
	private void switchProfile(Network network) {
		if (network == profile) {
			return;
		}
		UiSounds.click();
		profile = network;
		Theme.setProfile(profile); // repaint in the new profile's accent
		SharedConfig c = SharedConfig.get();
		c.lastConfigProfile = network.name();
		c.save();
		resetArmed = false;
		buildGrid();
	}

	private void openPanel(ConfigPanel panel) {
		if (panel == null || panel.placeholder || panel.groups.isEmpty()) {
			return;
		}
		FeaturePopup popup = new FeaturePopup(panel, this.width, this.height);
		popup.setOnClose(() -> overlay.remove(popup));
		overlay.add(popup);
	}

	/** Restores every editable option across all registered panels to its default (theme repaints live). */
	private void resetAll() {
		for (ConfigPanel panel : ConfigRegistry.panels()) {
			for (OptionGroup group : panel.groups) {
				for (Option option : group.options) {
					if (option.editable()) {
						option.reset();
					}
				}
			}
		}
		Theme.load();
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float dt) {
		// Dim the world behind us. Deliberately NOT renderBackground(): that applies the GUI blur
		// post-effect, which throws "Can only blur once per frame" when something (Iris/Sodium in the
		// user's modpack) has already blurred this frame. renderTransparentBackground draws the
		// vanilla translucent darkening gradient with no blur, so it's crash-proof everywhere.
		this.renderTransparentBackground(g);

		// window
		RenderUtils.rect(g, x0, y0, W, H, Theme.surface);
		RenderUtils.hLine(g, x0, y0 + HEADER, W, Theme.line);
		RenderUtils.hLine(g, x0, y0 + H - FOOTER, W, Theme.line);
		RenderUtils.outline(g, x0, y0, W, H, Theme.line);

		// While a popup is open it owns the cursor: feed the base layer an off-screen mouse so
		// nothing behind the popup shows a hover state.
		boolean blocked = overlay.isActive();
		int bmx = blocked ? -1 : mouseX;
		int bmy = blocked ? -1 : mouseY;

		renderHeader(g, bmx, bmy);

		grid.render(g, bmx, bmy, dt);

		renderFooter(g, bmx, bmy);

		overlay.render(g, mouseX, mouseY, dt);
	}

	private void renderHeader(GuiGraphics g, int mouseX, int mouseY) {
		int cy = y0 + HEADER / 2;
		int textY = cy - 4;

		// accent diamond mark
		diamond(g, x0 + PAD + 3, cy, 5, Theme.accent);

		int nameX = x0 + PAD + 12;
		RenderUtils.text(g, "BetterCosmic", nameX, textY, Theme.text);

		// Profile selector: the active network in accent, the other muted/hovered. Clicking switches.
		boolean pHover = RenderUtils.hit(mouseX, mouseY, prisonsX, y0 + 6, prisonsW, 14);
		boolean sHover = RenderUtils.hit(mouseX, mouseY, skyX, y0 + 6, skyW, 14);
		RenderUtils.text(g, Network.PRISONS.displayName(), prisonsX, textY,
			profile == Network.PRISONS ? Theme.accent : (pHover ? Theme.text : Theme.muted));
		RenderUtils.text(g, "·", prisonsX + prisonsW + 4, textY, Theme.faint);
		RenderUtils.text(g, Network.SKY.displayName(), skyX, textY,
			profile == Network.SKY ? Theme.accent : (sHover ? Theme.text : Theme.muted));

		// "Reset all" — two-click arm: first click shows "Confirm?", second resets everything.
		boolean rHover = RenderUtils.hit(mouseX, mouseY, resetX, y0 + 6, resetW, 14);
		String label = resetArmed ? "Confirm?" : "Reset all";
		int color = resetArmed ? Theme.accent : (rHover ? Theme.text : Theme.faint);
		RenderUtils.text(g, label, resetX + 6, textY, color);
	}

	private void renderFooter(GuiGraphics g, int mouseX, int mouseY) {
		int cy = y0 + H - FOOTER / 2;
		pager.update(grid.page(), grid.pageCount());
		pager.render(g, x0 + PAD, cy, mouseX, mouseY);

		int doneY = cy - 8;
		boolean dHover = RenderUtils.hit(mouseX, mouseY, doneX, doneY, doneW, 16);
		RenderUtils.panel(g, doneX, doneY, doneW, 16, Theme.surface, dHover ? Theme.accent : Theme.line);
		RenderUtils.textCentered(g, "Done", doneX + doneW / 2, doneY + 4, dHover ? Theme.text : Theme.muted);

		if (!HudRegistry.isEmpty()) {
			boolean hHover = RenderUtils.hit(mouseX, mouseY, hudX, doneY, hudW, 16);
			RenderUtils.panel(g, hudX, doneY, hudW, 16, Theme.surface, hHover ? Theme.accent : Theme.line);
			RenderUtils.textCentered(g, "HUD editor", hudX + hudW / 2, doneY + 4, hHover ? Theme.text : Theme.muted);
		}
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
		// Profile selector — switch which network's panels the grid shows.
		if (button == 0 && RenderUtils.hit(mx, my, prisonsX, y0 + 6, prisonsW, 14)) {
			switchProfile(Network.PRISONS);
			return true;
		}
		if (button == 0 && RenderUtils.hit(mx, my, skyX, y0 + 6, skyW, 14)) {
			switchProfile(Network.SKY);
			return true;
		}
		// "Reset all" — arm on first click, reset on the confirming second click.
		if (button == 0 && RenderUtils.hit(mx, my, resetX, y0 + 6, resetW, 14)) {
			UiSounds.click();
			if (resetArmed) {
				resetAll();
				resetArmed = false;
			} else {
				resetArmed = true;
			}
			return true;
		}
		resetArmed = false; // any other click disarms
		if (grid.mouseClicked(mx, my, button)) {
			return true;
		}
		if (pager.mouseClicked(mx, my, button)) {
			return true;
		}
		if (button == 0 && RenderUtils.hit(mx, my, doneX, y0 + H - FOOTER / 2 - 8, doneW, 16)) {
			UiSounds.click();
			onClose();
			return true;
		}
		if (button == 0 && !HudRegistry.isEmpty()
				&& RenderUtils.hit(mx, my, hudX, y0 + H - FOOTER / 2 - 8, hudW, 16)) {
			UiSounds.click();
			if (this.minecraft != null) {
				this.minecraft.setScreen(new HudEditorScreen(this));
			}
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
		Theme.setProfile(null); // release the pin so the in-world accent follows the connected server
		this.minecraft.setScreen(parent);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	// ---- helpers ----

	private static void diamond(GuiGraphics g, int cx, int cy, int r, int color) {
		for (int i = -r; i <= r; i++) {
			int half = r - Math.abs(i);
			g.fill(cx - half, cy + i, cx + half + 1, cy + i + 1, color);
		}
	}
}
