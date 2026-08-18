package dev.nishu.bettercosmic.shared.hud;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;

/**
 * Draws every enabled HUD in the {@link HudRegistry} each frame, respecting F1 (vanilla "hide GUI").
 * Registered once from a mod's client init; the shared library owns the render loop, the mods own the
 * HUD content.
 *
 * <p>Ported and de-hardcoded from BetterPrisons' {@code HudRenderer} (Yarn → Mojang).
 */
public final class HudRenderer {

	private HudRenderer() {}

	/** Hooks the HUD render callback. Call once from a mod client init. */
	public static void register() {
		HudRenderCallback.EVENT.register((context, tickCounter) -> {
			Minecraft client = Minecraft.getInstance();
			if (client.player == null || client.options.hideGui) {
				return;
			}
			for (HudRegistry.Entry entry : HudRegistry.entries()) {
				if (entry.hud.enabled) {
					entry.hud.render(context, client);
				}
			}
		});
	}
}
