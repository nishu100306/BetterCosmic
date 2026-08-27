package dev.nishu.bettercosmic.prisons.input;

import dev.nishu.bettercosmic.prisons.client.BetterPrisonsClient;
import dev.nishu.bettercosmic.prisons.screen.WaypointsScreen;
import dev.nishu.bettercosmic.shared.input.KeyBinds;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.lwjgl.glfw.GLFW;

/**
 * BetterPrisons' key bindings, registered under a "betterprisons" category via the shared
 * {@link KeyBinds} helper. The shared config-UI already owns the "open config" key, so it isn't
 * re-registered here. More keys (gang ping, ...) are added alongside their features.
 */
public final class PrisonKeybinds {

	public static KeyMapping resetStats;
	public static KeyMapping pauseStats;
	public static KeyMapping gangPing;
	public static KeyMapping gangPingBlock;
	public static KeyMapping waypoints;

	private PrisonKeybinds() {}

	/** Registers the keys and their poll loop. Call once from client init. */
	public static void register() {
		KeyMapping.Category category = KeyBinds.category("betterprisons", "betterprisons");
		resetStats = KeyBinds.register("key.betterprisons.reset_stats", GLFW.GLFW_KEY_R, category);
		pauseStats = KeyBinds.register("key.betterprisons.pause", GLFW.GLFW_KEY_B, category);
		gangPing = KeyBinds.register("key.betterprisons.gang_ping", GLFW.GLFW_KEY_G, category);
		gangPingBlock = KeyBinds.register("key.betterprisons.gang_ping_block", GLFW.GLFW_KEY_UNKNOWN, category);
		waypoints = KeyBinds.register("key.betterprisons.waypoints", GLFW.GLFW_KEY_UNKNOWN, category);

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (resetStats.consumeClick()) {
				if (BetterPrisonsClient.statsHud != null) {
					BetterPrisonsClient.statsHud.resetTracking();
				}
			}
			while (pauseStats.consumeClick()) {
				if (BetterPrisonsClient.statsHud != null) {
					BetterPrisonsClient.statsHud.togglePause();
				}
			}
			while (gangPing.consumeClick()) {
				if (BetterPrisonsClient.config.gangPingEnabled) {
					BetterPrisonsClient.gangPingManager.sendPing(client);
				}
			}
			while (waypoints.consumeClick()) {
				client.setScreen(new WaypointsScreen());
			}
			while (gangPingBlock.consumeClick()) {
				if (BetterPrisonsClient.config.gangPingEnabled) {
					BlockPos target = raycastTargetBlock(client);
					if (target != null) {
						BetterPrisonsClient.gangPingManager.sendPingAtBlock(client, target);
					} else if (client.player != null) {
						client.player.displayClientMessage(
								Component.literal("§c[BetterPrisons] No block in sight to ping"), false);
					}
				}
			}
		});
	}

	/** The block position of the first solid block within 200 blocks of the player's crosshair, or null. */
	private static BlockPos raycastTargetBlock(Minecraft client) {
		if (client.player == null) {
			return null;
		}
		HitResult hit = client.player.pick(200.0, 1.0f, false);
		if (hit.getType() == HitResult.Type.BLOCK && hit instanceof BlockHitResult bhr) {
			return bhr.getBlockPos();
		}
		return null;
	}
}
