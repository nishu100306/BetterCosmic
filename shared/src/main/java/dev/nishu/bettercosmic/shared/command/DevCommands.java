package dev.nishu.bettercosmic.shared.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.nishu.bettercosmic.shared.BetterCosmicShared;
import dev.nishu.bettercosmic.shared.config.SharedConfig;
import dev.nishu.bettercosmic.shared.notification.ToastRenderer;
import dev.nishu.bettercosmic.shared.render.FloatingTextRenderer;
import dev.nishu.bettercosmic.shared.server.Network;
import dev.nishu.bettercosmic.shared.server.ServerContext;
import dev.nishu.bettercosmic.shared.update.UpdateChecker;
import dev.nishu.bettercosmic.shared.update.UpdateState;
import dev.nishu.bettercosmic.shared.util.TabListUtil;
import dev.nishu.bettercosmic.shared.util.WorldUtil;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.impl.command.client.ClientCommandInternals;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundCommandsPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

import java.net.SocketAddress;
import java.util.Collection;

/**
 * Shared developer/debug commands for the BetterCosmic mods (client-side, Brigadier).
 *
 * <p>Every dev command except {@code /bcdev} is gated behind "developer mode"
 * ({@link SharedConfig#developerMode}) via Brigadier's {@code requires} predicate, so it is hidden
 * from tab-completion and unusable while developer mode is off. {@code /bcdev} — the on/off switch —
 * is always available.
 *
 * <p>Making the gate reflect live: Fabric copies client commands into the command <em>suggestion</em>
 * tree once, when the server's command packet arrives (on world join), evaluating each
 * {@code requires} predicate at that moment. A command gated off at that point is physically absent
 * from tab-completion and would stay missing after {@code /bcdev} enables developer mode. So whenever
 * developer mode is toggled we rebuild the command tree (see {@link #refreshCommandTree()}), which
 * re-evaluates the gates with the new value — making gated commands appear/disappear immediately.
 *
 * <p>Shared/cross-mod dev commands use the {@code bc} prefix (BetterCosmic), reserving {@code bp}
 * for BetterPrisons and {@code bs} for BetterSky. Ported from BetterPrisons' {@code /bpitem} (now the
 * shared {@code /bcitem}), translated from Yarn to Mojang mappings.
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
			// /bcdev [on|off] — toggle developer mode. Ungated: it's the on/off switch itself.
			dispatcher.register(ClientCommandManager.literal("bcdev")
					.executes(ctx -> setDevMode(ctx, !SharedConfig.get().developerMode))
					.then(ClientCommandManager.literal("on").executes(ctx -> setDevMode(ctx, true)))
					.then(ClientCommandManager.literal("off").executes(ctx -> setDevMode(ctx, false))));

			// /bcitem — inspect the held item. Gated: hidden from tab-completion unless dev mode is on.
			dispatcher.register(ClientCommandManager.literal("bcitem")
					.requires(DevCommands::devModeEnabled)
					.executes(DevCommands::inspectHeldItem));

			// /bcfloat [message] — spawn test world-space floating text at the player's target.
			dispatcher.register(ClientCommandManager.literal("bcfloat")
					.requires(DevCommands::devModeEnabled)
					.executes(ctx -> {
						FloatingTextRenderer.spawn(
								Component.literal("Test Proc").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD), 1500L);
						return 1;
					})
					.then(ClientCommandManager.argument("message", StringArgumentType.greedyString())
							.executes(ctx -> {
								FloatingTextRenderer.spawn(
										Component.literal(StringArgumentType.getString(ctx, "message")), 1500L);
								return 1;
							})));

			// /bctoast [message] — show a test toast.
			dispatcher.register(ClientCommandManager.literal("bctoast")
					.requires(DevCommands::devModeEnabled)
					.executes(ctx -> {
						ToastRenderer.show(
								Component.literal("Test Toast").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD),
								Component.literal("This is a sample notification toast."));
						return 1;
					})
					.then(ClientCommandManager.argument("message", StringArgumentType.greedyString())
							.executes(ctx -> {
								ToastRenderer.show(
										Component.literal("BetterCosmic").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD),
										Component.literal(StringArgumentType.getString(ctx, "message")));
								return 1;
							})));

			// /bcupdate [check|demo] — inspect the auto-updater, force a re-check, or preview the toast.
			dispatcher.register(ClientCommandManager.literal("bcupdate")
					.requires(DevCommands::devModeEnabled)
					.executes(DevCommands::reportUpdate)
					.then(ClientCommandManager.literal("check").executes(ctx -> {
						UpdateChecker.recheck();
						ctx.getSource().sendFeedback(Component.literal(
								"§7Re-checking for updates — see latest.log and §f/bcupdate§7."));
						return 1;
					}))
					.then(ClientCommandManager.literal("demo").executes(ctx -> {
						UpdateChecker.demoToast();
						return 1;
					})));

			// /bcblock — coordinates and type of the block under the crosshair.
			dispatcher.register(ClientCommandManager.literal("bcblock")
					.requires(DevCommands::devModeEnabled)
					.executes(DevCommands::getBlockLookingAt));

			// /bcscoreboard — dump all scoreboard state (display slots, objectives + scores, teams).
			dispatcher.register(ClientCommandManager.literal("bcscoreboard")
					.requires(DevCommands::devModeEnabled)
					.executes(DevCommands::dumpScoreboard));

			// /bcworld — the dimension key of the world the client is currently in.
			dispatcher.register(ClientCommandManager.literal("bcworld")
					.requires(DevCommands::devModeEnabled)
					.executes(ctx -> {
						ctx.getSource().sendFeedback(
								Component.literal("§7Current world: §f" + WorldUtil.detectWorldKey()));
						return 1;
					}));

			// /bcserver — the address the client is actually connected to right now.
			dispatcher.register(ClientCommandManager.literal("bcserver")
					.requires(DevCommands::devModeEnabled)
					.executes(DevCommands::reportServerAddress));

			// /bctablist — dump the tab-list header/footer and listed players.
			dispatcher.register(ClientCommandManager.literal("bctablist")
					.requires(DevCommands::devModeEnabled)
					.executes(DevCommands::dumpTabList));

			// /bcforce <prisons|sky|none|clear> — override the active network for testing off-server.
			dispatcher.register(ClientCommandManager.literal("bcforce")
					.requires(DevCommands::devModeEnabled)
					.then(ClientCommandManager.literal("prisons").executes(ctx -> forceNetwork(ctx, Network.PRISONS)))
					.then(ClientCommandManager.literal("sky").executes(ctx -> forceNetwork(ctx, Network.SKY)))
					.then(ClientCommandManager.literal("none").executes(ctx -> forceNetwork(ctx, null)))
					.then(ClientCommandManager.literal("clear").executes(ctx -> {
						ServerContext.clearOverride();
						ctx.getSource().sendFeedback(Component.literal(
								"§7Network override cleared — using detection (§f" + ServerContext.detected() + "§7)."));
						return 1;
					})));
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

	/** Logs the coordinates and type of the block under the crosshair — ported from BetterPrisons. */
	private static int getBlockLookingAt(CommandContext<FabricClientCommandSource> ctx) {
		Minecraft client = ctx.getSource().getClient();
		if (client.player == null || client.level == null) {
			return 0;
		}
		HitResult hit = client.hitResult;
		if (hit == null || hit.getType() != HitResult.Type.BLOCK) {
			ctx.getSource().sendFeedback(Component.literal("§cNot looking at a block"));
			return 0;
		}
		BlockPos pos = ((BlockHitResult) hit).getBlockPos();
		BetterCosmicShared.LOGGER.info("Block at {}, {}, {}: {}", pos.getX(), pos.getY(), pos.getZ(),
				client.level.getBlockState(pos).getBlock());
		ctx.getSource().sendFeedback(Component.literal("§aBlock: §f" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ()));
		return 1;
	}

	/**
	 * Reports the server address the client is connected to. Prints two values:
	 *
	 * <ul>
	 *   <li><b>connected</b> — the live socket's remote address ({@link Connection#getRemoteAddress()}).
	 *       After a server transfer ({@code ClientboundTransferPacket}) the client reconnects, so this
	 *       reflects the <em>rerouted</em> backend, not wherever the session started.</li>
	 *   <li><b>list entry</b> — the address the player originally selected/typed in the multiplayer
	 *       list ({@link Minecraft#getCurrentServer()}). It does not change on a reroute, so comparing
	 *       the two makes a transfer visible.</li>
	 * </ul>
	 *
	 * <p>Caveat: a BungeeCord/Velocity-style proxy that switches backends server-side (no transfer
	 * packet) keeps the client's socket pointed at the proxy, so "connected" will read as the proxy
	 * address in that case — the client has no address-level view of the backend behind it.
	 */
	private static int reportServerAddress(CommandContext<FabricClientCommandSource> ctx) {
		Minecraft client = ctx.getSource().getClient();
		ClientPacketListener conn = client.getConnection();
		if (conn == null) {
			ctx.getSource().sendFeedback(Component.literal("§cNot connected to a server."));
			return 0;
		}

		SocketAddress remote = conn.getConnection().getRemoteAddress();
		ServerData data = client.getCurrentServer();
		String listed = data != null ? data.ip : "(direct / single-player)";

		Network detected = ServerContext.detected();
		Network override = ServerContext.override();
		String scope = !SharedConfig.get().restrictFeaturesToServer
				? "§eunrestricted (features on everywhere)"
				: override != null
						? "§b" + override.name().toLowerCase() + " §7(dev override)"
						: detected != null ? "§a" + detected.name().toLowerCase() : "§7none";

		BetterCosmicShared.LOGGER.info("[DevTools] Connected socket: {} | server-list entry: {} | network: {}",
				remote, listed, detected);
		ctx.getSource().sendFeedback(Component.literal("§6Server address:"));
		ctx.getSource().sendFeedback(Component.literal("§7  connected: §f" + remote));
		ctx.getSource().sendFeedback(Component.literal("§7  list entry: §f" + listed));
		ctx.getSource().sendFeedback(Component.literal("§7  network:   " + scope));
		return 1;
	}

	/**
	 * Dumps the full client-side scoreboard state to the console: which objective occupies each
	 * display slot, every objective with its criteria/render type and player scores, and every team
	 * with its prefix/suffix/colour/members. The client mirrors whatever the server sends over the
	 * scoreboard packets, so this shows exactly what is visible on the current backend — handy for
	 * spotting a server/planet identifier that the sidebar or tab-list carries. Ported from
	 * BetterPrisons (originally sidebar-only).
	 */
	private static int dumpScoreboard(CommandContext<FabricClientCommandSource> ctx) {
		Minecraft client = ctx.getSource().getClient();
		if (client.level == null) {
			ctx.getSource().sendFeedback(Component.literal("§cNo world loaded"));
			return 0;
		}
		Scoreboard scoreboard = client.level.getScoreboard();

		BetterCosmicShared.LOGGER.info("========== SCOREBOARD DUMP ==========");

		// Display slots -> the objective shown in each (sidebar, list, below-name, per-team sidebars).
		BetterCosmicShared.LOGGER.info("-- Display slots --");
		for (DisplaySlot slot : DisplaySlot.values()) {
			Objective shown = scoreboard.getDisplayObjective(slot);
			if (shown != null) {
				BetterCosmicShared.LOGGER.info("  {} -> {}", slot.getSerializedName(), shown.getName());
			}
		}

		// Every objective, with its scores (regardless of whether it is currently displayed).
		Collection<Objective> objectives = scoreboard.getObjectives();
		BetterCosmicShared.LOGGER.info("-- Objectives ({}) --", objectives.size());
		for (Objective objective : objectives) {
			BetterCosmicShared.LOGGER.info("Objective '{}' | display: \"{}\" | criteria: {} | render: {}",
					objective.getName(), objective.getDisplayName().getString(),
					objective.getCriteria().getName(), objective.getRenderType());
			int line = 1;
			for (PlayerScoreEntry entry : scoreboard.listPlayerScores(objective)) {
				String owner = entry.owner();
				PlayerTeam team = scoreboard.getPlayersTeam(owner);
				Component display = team != null
						? PlayerTeam.formatNameForTeam(team, Component.literal(owner)) : Component.literal(owner);
				String full = display.getString();
				BetterCosmicShared.LOGGER.info("  [{}] \"{}\" | stripped: \"{}\" | value: {}",
						line, full, full.replaceAll("§.", ""), entry.value());
				line++;
			}
		}

		// Every team, with formatting and membership.
		Collection<PlayerTeam> teams = scoreboard.getPlayerTeams();
		BetterCosmicShared.LOGGER.info("-- Teams ({}) --", teams.size());
		for (PlayerTeam team : teams) {
			BetterCosmicShared.LOGGER.info(
					"Team '{}' | display: \"{}\" | prefix: \"{}\" | suffix: \"{}\" | color: {} | members: {}",
					team.getName(), team.getDisplayName().getString(),
					team.getPlayerPrefix().getString(), team.getPlayerSuffix().getString(),
					team.getColor(), team.getPlayers());
		}

		BetterCosmicShared.LOGGER.info("=====================================");
		ctx.getSource().sendFeedback(Component.literal(String.format(
				"§aScoreboard dumped: §f%d§a objectives, §f%d§a teams. Check latest.log",
				objectives.size(), teams.size())));
		return 1;
	}

	/**
	 * Dumps the tab-list header/footer and the listed players to the console. The header/footer are the
	 * usual carriers of a server/planet identifier (Cosmic often prints the backend name there), so they
	 * are also echoed to chat. Header/footer are read via {@link TabListUtil}; the listed players come
	 * from the connection's player list.
	 */
	private static int dumpTabList(CommandContext<FabricClientCommandSource> ctx) {
		Minecraft client = ctx.getSource().getClient();
		ClientPacketListener conn = client.getConnection();
		if (conn == null) {
			ctx.getSource().sendFeedback(Component.literal("§cNot connected to a server."));
			return 0;
		}

		Component header = TabListUtil.header();
		Component footer = TabListUtil.footer();

		BetterCosmicShared.LOGGER.info("========== TAB LIST DUMP ==========");
		logComponent("Header", header);
		logComponent("Footer", footer);

		Collection<PlayerInfo> players = conn.getListedOnlinePlayers();
		BetterCosmicShared.LOGGER.info("-- Listed players ({}) --", players.size());
		for (PlayerInfo info : players) {
			String name = info.getProfile().name();
			Component displayName = info.getTabListDisplayName();
			String display = displayName != null ? displayName.getString() : name;
			BetterCosmicShared.LOGGER.info("  {} | display: \"{}\" | stripped: \"{}\" | ping: {}ms | mode: {}",
					name, display, display.replaceAll("§.", ""), info.getLatency(), info.getGameMode());
		}
		BetterCosmicShared.LOGGER.info("===================================");

		// Header/footer are the likely planet carriers, so surface them inline; the player list can be long.
		ctx.getSource().sendFeedback(Component.literal("§6Tab list:"));
		ctx.getSource().sendFeedback(Component.literal("§7  header: §f" + previewComponent(header)));
		ctx.getSource().sendFeedback(Component.literal("§7  footer: §f" + previewComponent(footer)));
		ctx.getSource().sendFeedback(Component.literal(
				"§7  " + players.size() + " listed players (full dump in latest.log)."));
		return 1;
	}

	/** Reports the current auto-updater state (installed / latest / available) to chat. */
	private static int reportUpdate(CommandContext<FabricClientCommandSource> ctx) {
		UpdateState s = UpdateChecker.state();
		if (s == null) {
			ctx.getSource().sendFeedback(Component.literal(
					"§7Update check: §fstill running or checks are off§7. Try §f/bcupdate check§7."));
			return 1;
		}
		ctx.getSource().sendFeedback(Component.literal("§6Auto-updater:"));
		ctx.getSource().sendFeedback(Component.literal("§7  installed: §f" + s.installed));
		ctx.getSource().sendFeedback(Component.literal("§7  latest:    §f" + s.latest));
		ctx.getSource().sendFeedback(Component.literal(s.available
				? "§7  status:    §aupdate available" + (s.mandatory ? " §c(mandatory)" : "")
				: "§7  status:    §aup to date"));
		ctx.getSource().sendFeedback(Component.literal(
				"§8  (§f/bcupdate check§8 re-checks, §f/bcupdate demo§8 previews the toast)"));
		return 1;
	}

	/** Sets (or clears, when {@code network} is null but forced) a dev network override for testing. */
	private static int forceNetwork(CommandContext<FabricClientCommandSource> ctx, Network network) {
		ServerContext.setOverride(network);
		ctx.getSource().sendFeedback(Component.literal("§aForced network override: §f"
				+ (network == null ? "none" : network.name().toLowerCase())
				+ " §7(use §f/bcforce clear§7 to restore detection)."));
		return 1;
	}

	/** Logs a tab-list component with both raw (§) and stripped forms, or "(none)" when null. */
	private static void logComponent(String label, Component component) {
		if (component == null) {
			BetterCosmicShared.LOGGER.info("{}: (none)", label);
			return;
		}
		String raw = component.getString();
		BetterCosmicShared.LOGGER.info("{}: \"{}\" | stripped: \"{}\"", label, raw, raw.replaceAll("§.", ""));
	}

	/** One-line, colour-stripped preview of a (possibly multi-line/null) component for chat feedback. */
	private static String previewComponent(Component component) {
		if (component == null) {
			return "(none)";
		}
		String s = component.getString().replaceAll("§.", "").replace("\n", " / ");
		return s.length() > 80 ? s.substring(0, 80) + "…" : s;
	}
}
