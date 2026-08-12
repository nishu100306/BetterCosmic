package dev.nishu.bettercosmic.shared.command;

import com.mojang.brigadier.context.CommandContext;
import dev.nishu.bettercosmic.shared.BetterCosmicShared;
import dev.nishu.bettercosmic.shared.config.SharedConfig;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.impl.command.client.ClientCommandInternals;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundCommandsPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;

/**
 * Shared developer/debug commands for the BetterCosmic mods (client-side, Brigadier).
 *
 * <p>Every dev command except {@code /bdev} is gated behind "developer mode"
 * ({@link SharedConfig#developerMode}) via Brigadier's {@code requires} predicate, so it is hidden
 * from tab-completion and unusable while developer mode is off. {@code /bdev} — the on/off switch —
 * is always available.
 *
 * <p>Making the gate reflect live: Fabric copies client commands into the command <em>suggestion</em>
 * tree once, when the server's command packet arrives (on world join), evaluating each
 * {@code requires} predicate at that moment. A command gated off at that point is physically absent
 * from tab-completion and would stay missing after {@code /bdev} enables developer mode. So whenever
 * developer mode is toggled we rebuild the command tree (see {@link #refreshCommandTree()}), which
 * re-evaluates the gates with the new value — making gated commands appear/disappear immediately.
 *
 * <p>Ported from BetterPrisons' {@code /bpitem} (now the shared {@code /bitem}), translated from
 * Yarn to Mojang mappings.
 */
public final class DevCommands {

	private DevCommands() {}

	/** Predicate used by every gated dev command; kept in one place for consistency. */
	public static boolean devModeEnabled(FabricClientCommandSource source) {
		return SharedConfig.get().developerMode;
	}

	/** Hooks client command registration. Call once from the shared library initializer. */
	public static void register() {
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
			// /bdev [on|off] — toggle developer mode. Ungated: it's the on/off switch itself.
			dispatcher.register(ClientCommandManager.literal("bdev")
					.executes(ctx -> setDevMode(ctx, !SharedConfig.get().developerMode))
					.then(ClientCommandManager.literal("on").executes(ctx -> setDevMode(ctx, true)))
					.then(ClientCommandManager.literal("off").executes(ctx -> setDevMode(ctx, false))));

			// /bitem — inspect the held item. Gated: hidden from tab-completion unless dev mode is on.
			dispatcher.register(ClientCommandManager.literal("bitem")
					.requires(DevCommands::devModeEnabled)
					.executes(DevCommands::inspectHeldItem));
		});
	}

	private static int setDevMode(CommandContext<FabricClientCommandSource> ctx, boolean enabled) {
		SharedConfig cfg = SharedConfig.get();
		cfg.developerMode = enabled;
		cfg.save();
		// Rebuild the command tree so the gated commands appear/disappear from tab-completion now,
		// rather than only after the next world join.
		refreshCommandTree();
		ctx.getSource().sendFeedback(Component.literal(enabled
				? "§aBetterCosmic developer mode §2enabled§a. Dev commands are now available."
				: "§7BetterCosmic developer mode §8disabled§7."));
		return 1;
	}

	/**
	 * Forces the client to rebuild its command-suggestion tree by re-processing the last command
	 * packet the server sent. This re-runs Fabric's client-command merge, which re-evaluates every
	 * {@code requires} gate against the current developer-mode value.
	 *
	 * <p>Uses Fabric's stored copy of that packet (exposed via an internal accessor on the network
	 * handler). Guarded so that if it is ever unavailable, the toggle still works — the gated
	 * commands simply won't refresh until the next world join.
	 */
	private static void refreshCommandTree() {
		ClientPacketListener conn = Minecraft.getInstance().getConnection();
		if (conn instanceof ClientCommandInternals.LastReceivedCommandsPacketAccessor accessor) {
			ClientboundCommandsPacket packet = accessor.fabric_api$getLastReceivedCommandsPacket();
			if (packet != null) {
				conn.handleCommands(packet);
			}
		}
	}

	/** Logs full details of the held item to the console — ported from BetterPrisons' /bpitem. */
	private static int inspectHeldItem(CommandContext<FabricClientCommandSource> context) {
		Minecraft client = context.getSource().getClient();
		var player = client.player;
		if (player == null) {
			BetterCosmicShared.LOGGER.info("[DevTools] No player found");
			return 0;
		}

		ItemStack stack = player.getMainHandItem();
		if (stack.isEmpty()) {
			context.getSource().sendFeedback(Component.literal("§cNo item in main hand"));
			return 0;
		}

		BetterCosmicShared.LOGGER.info("========== ITEM INSPECTION ==========");
		BetterCosmicShared.LOGGER.info("Item: {}", stack.getItem());
		BetterCosmicShared.LOGGER.info("Registry Name: {}", stack.getItem().getDescriptionId());
		BetterCosmicShared.LOGGER.info("Display Name: {}", stack.getHoverName().getString());
		BetterCosmicShared.LOGGER.info("Count: {}", stack.getCount());

		ItemLore lore = stack.get(DataComponents.LORE);
		if (lore != null && !lore.lines().isEmpty()) {
			BetterCosmicShared.LOGGER.info("Lore:");
			int lineNum = 1;
			for (Component line : lore.lines()) {
				BetterCosmicShared.LOGGER.info("  [{}] {}", lineNum, line.getString());
				lineNum++;
			}
		} else {
			BetterCosmicShared.LOGGER.info("Lore: (none)");
		}

		Component customName = stack.get(DataComponents.CUSTOM_NAME);
		if (customName != null) {
			BetterCosmicShared.LOGGER.info("Custom Name: {}", customName.getString());
		}

		BetterCosmicShared.LOGGER.info("Components: {}", stack.getComponents());
		BetterCosmicShared.LOGGER.info("====================================");

		context.getSource().sendFeedback(
				Component.literal("§aItem details logged to console. Check latest.log"));
		return 1;
	}
}
