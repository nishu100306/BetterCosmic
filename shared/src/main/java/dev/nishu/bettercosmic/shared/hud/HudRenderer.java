package dev.nishu.bettercosmic.shared.hud;

import dev.nishu.bettercosmic.shared.server.ServerContext;
import dev.nishu.bettercosmic.shared.ui.core.Theme;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;

/**
 * Drives every HUD in the {@link HudRegistry}: ticks them each client tick (so they refresh their
 * data and enabled state) and draws the enabled ones each frame, respecting F1 (vanilla "hide GUI").
 * Registered once from a mod's client init; the shared library owns the render/tick loops, the mods
 * own the HUD content.
 *
 * <p>Ported and de-hardcoded from BetterPrisons' {@code HudRenderer} (Yarn → Mojang).
 */
public final class HudRenderer {

	private HudRenderer() {}

	/** Hooks the HUD render + tick loops. Call once from a mod client init. */
	public static void register() {
		HudRenderCallback.EVENT.register((context, tickCounter) -> {
			Minecraft client = Minecraft.getInstance();
			if (client.player == null || client.options.hideGui) {
				return;
			}
			// Keep the shared tokens (and the server-following accent) fresh for in-world HUDs/toasts.
			Theme.load();
			int screenWidth = client.getWindow().getGuiScaledWidth();
			int screenHeight = client.getWindow().getGuiScaledHeight();
			for (HudRegistry.Entry entry : HudRegistry.entries()) {
				if (entry.hud.enabled && ServerContext.isActive(entry.network)) {
					BaseHud hud = entry.hud;
					// Draw at a screen-clamped position so content-resizing HUDs stay fully on-screen,
					// but keep the saved anchor (x/y) untouched — the config/editor position is always
					// the original, so the HUD returns to it once its content shrinks again.
					int anchorX = hud.x;
					int anchorY = hud.y;
					hud.x = hud.clampedX(screenWidth);
					hud.y = hud.clampedY(screenHeight);
					try {
						hud.render(context, client);
					} finally {
						hud.x = anchorX;
						hud.y = anchorY;
					}
				}
			}
		});

		// Tick every HUD (even disabled ones, so a HUD can sync its enabled flag from config), but only
		// while its owning network is active — so an off-server HUD never accumulates or draws state.
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			for (HudRegistry.Entry entry : HudRegistry.entries()) {
				if (!ServerContext.isActive(entry.network)) {
					continue;
				}
				entry.hud.tick();
				entry.hud.tick(client);
			}
		});
	}
}
