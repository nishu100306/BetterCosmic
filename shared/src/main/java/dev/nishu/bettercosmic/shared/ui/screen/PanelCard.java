package dev.nishu.bettercosmic.shared.ui.screen;

import dev.nishu.bettercosmic.shared.ui.core.Theme;
import dev.nishu.bettercosmic.shared.ui.core.UiElement;
import dev.nishu.bettercosmic.shared.ui.model.ConfigPanel;
import dev.nishu.bettercosmic.shared.ui.model.PanelIcon;
import dev.nishu.bettercosmic.shared.ui.render.ColorUtils;
import dev.nishu.bettercosmic.shared.ui.render.RenderUtils;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;
import java.util.function.Consumer;

/**
 * A single grid tile. Renders one {@link ConfigPanel} — icon, title, wrapped description, and a
 * bottom meta line (accent dot + "N settings" for a real panel, a "Coming soon" chip for a
 * placeholder or an empty cell). Real panels hover (accent border + 1px lift) and open on click; a
 * {@code null} panel is a generic empty "coming soon" cell.
 */
public final class PanelCard extends UiElement {

	private static final int PAD = 9;
	private static final int ICON = 14;

	private final ConfigPanel panel;         // null → generic empty cell
	private final Consumer<ConfigPanel> onOpen;
	private final boolean locked;            // placeholder or empty → non-interactive

	public PanelCard(ConfigPanel panel, int x, int y, int w, int h, Consumer<ConfigPanel> onOpen) {
		super(x, y, w, h);
		this.panel = panel;
		this.onOpen = onOpen;
		this.locked = panel == null || panel.placeholder;
		this.enabled = !locked;
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float dt) {
		hovered = enabled && isMouseOver(mouseX, mouseY);
		int dy = hovered ? y - 1 : y; // subtle lift on hover

		int bg = locked ? ColorUtils.scaleAlpha(Theme.surface, 0.55f)
			: hovered ? Theme.surfaceHover : Theme.surface;
		int border = hovered ? Theme.accent : Theme.line;
		RenderUtils.panel(g, x, dy, w, h, bg, border);

		int iconColor = locked ? Theme.faint : Theme.accent;
		PanelIcon icon = panel != null ? panel.icon : PanelIcon.LOCK;
		icon.draw(g, x + PAD, dy + PAD, ICON, iconColor);

		Font font = RenderUtils.font();
		int textX = x + PAD;
		int titleY = dy + PAD + ICON + 5;
		String title = panel != null ? panel.title : "Coming soon";
		RenderUtils.text(g, title, textX, titleY, locked ? Theme.faint : Theme.text);

		// wrapped description (up to 2 lines)
		String desc = panel != null ? panel.description : "Not yet available";
		int descY = titleY + font.lineHeight + 3;
		List<FormattedCharSequence> lines = font.split(Component.literal(desc), w - 2 * PAD);
		int descColor = locked ? Theme.faint : Theme.muted;
		for (int i = 0; i < Math.min(2, lines.size()); i++) {
			g.drawString(font, lines.get(i), textX, descY + i * (font.lineHeight + 1), descColor, true);
		}

		// bottom meta line
		int metaY = dy + h - PAD - font.lineHeight + 2;
		if (locked) {
			// "Coming soon" chip
			String chip = "COMING SOON";
			int cw = font.width(chip) + 8;
			RenderUtils.outline(g, textX, metaY - 3, cw, font.lineHeight + 4, Theme.line);
			RenderUtils.text(g, chip, textX + 4, metaY, Theme.faint);
		} else {
			int dot = 4;
			RenderUtils.rect(g, textX, metaY + 2, dot, dot, Theme.accent);
			RenderUtils.text(g, panel.settingsCount() + " SETTINGS", textX + dot + 5, metaY, Theme.faint);
		}
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (button == 0 && enabled && isMouseOver(mouseX, mouseY)) {
			onOpen.accept(panel);
			return true;
		}
		return false;
	}
}
