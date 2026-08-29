package dev.nishu.bettercosmic.prisons.hud;

import dev.nishu.bettercosmic.prisons.client.BetterPrisonsClient;
import dev.nishu.bettercosmic.prisons.config.PrisonsConfig;
import dev.nishu.bettercosmic.prisons.enchants.BaseEnchant;
import dev.nishu.bettercosmic.shared.hud.BaseHud;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.joml.Matrix3x2fStack;

import java.util.List;

/**
 * Enchant HUD: lists active enchants/effects (from {@link dev.nishu.bettercosmic.prisons.enchants.EnchantTracker})
 * with their remaining time. Ported from BetterPrisons (Yarn → Mojang); extends the shared
 * {@link BaseHud}.
 */
public class EnchantHud extends BaseHud {

	private static final Component TITLE =
			Component.literal("Enchant HUD").setStyle(Style.EMPTY.withUnderlined(true).withBold(true));

	public EnchantHud() {
		super("enchant");
	}

	private static PrisonsConfig cfg() {
		return BetterPrisonsClient.config;
	}

	@Override
	public void tick() {
		this.enabled = cfg().enchantHudEnabled;
	}

	private List<BaseEnchant> activeEnchants() {
		return BetterPrisonsClient.enchantTracker.getActiveEnchants();
	}

	@Override
	public void render(GuiGraphics ctx, Minecraft client) {
		if (!enabled) {
			return;
		}
		this.scale = cfg().enchantHudScale / 100.0f;

		boolean showTitle = cfg().showEnchantHudTitle;
		List<BaseEnchant> active = activeEnchants();
		boolean hasContent = !active.isEmpty();
		if (!showTitle && !hasContent) {
			return;
		}

		int titleHeight = 0;
		int titleWidth = 0;
		if (showTitle) {
			titleWidth = (int) (client.font.width(TITLE) * scale);
			titleHeight = scaled(12);
		}

		int maxWidth = titleWidth;
		for (BaseEnchant enchant : active) {
			Component nameText = enchant.displayText != null ? enchant.displayText : Component.literal(enchant.displayName);
			String timeString = String.format("%.1f", enchant.getRemainingSeconds()) + "s";
			int totalWidth = (int) ((client.font.width(nameText) + 10 + client.font.width(timeString)) * scale);
			maxWidth = Math.max(maxWidth, totalWidth);
		}

		int bgWidth = hasContent ? scaled((int) (maxWidth / scale)) : titleWidth;
		int contentHeight = hasContent ? scaled(active.size() * 14) : 0;
		int bgHeight = titleHeight + contentHeight;

		int bgColor = (cfg().enchantBgOpacity << 24) | (cfg().enchantBgColor & 0xFFFFFF);
		int borderColor = (cfg().enchantBorderOpacity << 24) | (cfg().enchantBorderColor & 0xFFFFFF);
		int thickness = cfg().enchantBorderThickness;

		ctx.fill(x - 2, y - 2, x + bgWidth + 2, y + bgHeight + 2, bgColor);
		ctx.fill(x - 2, y - 2 - thickness, x + bgWidth + 2, y - 2, borderColor);
		ctx.fill(x - 2, y + bgHeight + 2, x + bgWidth + 2, y + bgHeight + 2 + thickness, borderColor);
		ctx.fill(x - 2 - thickness, y - 2 - thickness, x - 2, y + bgHeight + 2 + thickness, borderColor);
		ctx.fill(x + bgWidth + 2, y - 2 - thickness, x + bgWidth + 2 + thickness, y + bgHeight + 2 + thickness, borderColor);

		Matrix3x2fStack matrices = ctx.pose();

		if (showTitle) {
			int titleColor = 0xFF000000 | cfg().enchantHudTitleColor;
			matrices.pushMatrix();
			matrices.scale(scale, scale);
			matrices.translate(x / scale, y / scale);
			ctx.drawString(client.font, TITLE, 0, 0, titleColor, true);
			matrices.popMatrix();
		}

		int yOffset = titleHeight;
		for (BaseEnchant enchant : active) {
			Component nameText = enchant.displayText != null ? enchant.displayText : Component.literal(enchant.displayName);
			int timeColor = 0xFF000000 | cfg().enchantTimeColor;
			String timeString = String.format("%.1f", enchant.getRemainingSeconds()) + "s";
			int nameWidth = client.font.width(nameText);

			matrices.pushMatrix();
			matrices.scale(scale, scale);
			matrices.translate(x / scale, (y + yOffset) / scale);
			ctx.drawString(client.font, nameText, 0, 0, 0xFFFFFFFF, true);
			ctx.drawString(client.font, Component.literal(timeString), nameWidth + 10, 0, timeColor, true);
			matrices.popMatrix();
			yOffset += scaled(14);
		}
	}

	@Override
	public int getWidth() {
		Minecraft client = Minecraft.getInstance();
		if (client == null || client.font == null) {
			return scaled(120);
		}
		int titleWidth = cfg().showEnchantHudTitle ? (int) (client.font.width(TITLE) * scale) : 0;
		List<BaseEnchant> active = activeEnchants();
		if (active.isEmpty()) {
			return titleWidth;
		}
		int maxWidth = titleWidth;
		for (BaseEnchant enchant : active) {
			Component nameText = enchant.displayText != null ? enchant.displayText : Component.literal(enchant.displayName);
			String timeText = String.format("%.1f", enchant.getRemainingSeconds()) + "s";
			int totalWidth = (int) ((client.font.width(nameText) + 10 + client.font.width(timeText)) * scale);
			maxWidth = Math.max(maxWidth, totalWidth);
		}
		return scaled((int) (maxWidth / scale)) + 4;
	}

	@Override
	public int getHeight() {
		int titleHeight = cfg().showEnchantHudTitle ? scaled(10) : 0;
		return titleHeight + activeEnchants().size() * scaled(14);
	}
}
