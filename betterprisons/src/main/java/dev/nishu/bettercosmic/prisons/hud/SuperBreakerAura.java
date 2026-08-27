package dev.nishu.bettercosmic.prisons.hud;

import dev.nishu.bettercosmic.prisons.client.BetterPrisonsClient;
import dev.nishu.bettercosmic.prisons.config.PrisonsConfig;
import dev.nishu.bettercosmic.prisons.enchants.BaseEnchant;
import dev.nishu.bettercosmic.shared.hud.BaseHud;
import dev.nishu.bettercosmic.shared.util.ItemUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.joml.Matrix3x2fStack;

/**
 * Super Breaker Aura: a centered ring (like a WoW WeakAura) around the crosshair that counts down the
 * Super Breaker duration. Registered as a non-draggable HUD (crosshair-anchored, positioned via
 * config X/Y offsets, not the drag editor). Shows while Super Breaker is active — detected via the
 * dragon-growl sound + nearby particles (see
 * {@link dev.nishu.bettercosmic.prisons.enchants.SuperBreakerDetector}). Ported from BetterPrisons
 * (Yarn → Mojang).
 */
public class SuperBreakerAura extends BaseHud {

	public SuperBreakerAura() {
		super("superbreaker_aura");
	}

	private static PrisonsConfig cfg() {
		return BetterPrisonsClient.config;
	}

	@Override
	public void tick() {
		this.enabled = cfg().superBreakerAuraEnabled;
	}

	@Override
	public void render(GuiGraphics ctx, Minecraft client) {
		if (!cfg().superBreakerAuraEnabled) {
			return;
		}
		BaseEnchant superBreaker = BetterPrisonsClient.enchantTracker.getEnchant("super_breaker");
		if (superBreaker == null || !superBreaker.isActive) {
			return;
		}
		if (!ItemUtils.isHoldingPickaxe() || ItemUtils.extractLoreLineFromHeldItem("Super Breaker") == null) {
			return;
		}

		float auraScale = cfg().superBreakerAuraScale / 100.0f;
		Matrix3x2fStack matrices = ctx.pose();

		int centerX = client.getWindow().getGuiScaledWidth() / 2;
		int centerY = client.getWindow().getGuiScaledHeight() / 2;

		double remainingSeconds = superBreaker.getRemainingSeconds();
		String timeText = String.format("%.1f", remainingSeconds);

		int baseColor = (cfg().superBreakerBaseOpacity << 24) | (cfg().superBreakerBaseColor & 0xFFFFFF);
		int lightColor = (cfg().superBreakerLightOpacity << 24) | (cfg().superBreakerLightColor & 0xFFFFFF);
		double progress = superBreaker.durationSeconds > 0 ? remainingSeconds / superBreaker.durationSeconds : 0;

		int radius = 100;
		int thickness = 10;

		matrices.pushMatrix();
		matrices.translate(centerX, centerY);
		matrices.scale(auraScale, auraScale);
		matrices.translate(-centerX, -centerY);

		drawSemicircle(ctx, centerX, centerY, radius, thickness, baseColor, true, 1.0);
		drawSemicircle(ctx, centerX, centerY, radius, thickness, baseColor, false, 1.0);
		drawSemicircle(ctx, centerX, centerY, radius, thickness, lightColor, true, progress);
		drawSemicircle(ctx, centerX, centerY, radius, thickness, lightColor, false, progress);

		if (cfg().superBreakerTimerEnabled) {
			int timerWidth = client.font.width(timeText);
			int timerX = centerX - timerWidth / 2 + cfg().superBreakerTimerOffsetX;
			int timerY = centerY - 4 + cfg().superBreakerTimerOffsetY;
			ctx.drawString(client.font, Component.literal(timeText), timerX, timerY, 0xFFFFFFFF, true);
		}

		matrices.popMatrix();
	}

	private void drawSemicircle(GuiGraphics ctx, int centerX, int centerY, int radius, int thickness,
								int color, boolean isLeft, double fillProgress) {
		for (int r = radius - thickness; r <= radius; r++) {
			for (int angle = 0; angle < 360; angle++) {
				boolean shouldDraw = false;
				if (isLeft) {
					if (angle >= 140 && angle <= 220) {
						if (fillProgress >= 1.0) {
							shouldDraw = true;
						} else {
							int angleInArc = angle - 120;
							shouldDraw = angleInArc <= (120 * fillProgress);
						}
					}
				} else {
					if (angle > 320 || angle < 40) {
						if (fillProgress >= 1.0) {
							shouldDraw = true;
						} else {
							int normalizedAngle = angle < 60 ? 60 - angle : 360 - angle + 60;
							shouldDraw = normalizedAngle <= (120 * fillProgress);
						}
					}
				}
				if (shouldDraw) {
					double radians = Math.toRadians(angle);
					int px = centerX + (int) (r * Math.cos(radians));
					int py = centerY + (int) (r * Math.sin(radians));
					ctx.fill(px, py, px + 2, py + 2, color);
				}
			}
		}
	}
}
