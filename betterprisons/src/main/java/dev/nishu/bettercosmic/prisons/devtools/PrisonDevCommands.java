package dev.nishu.bettercosmic.prisons.devtools;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.nishu.bettercosmic.prisons.BetterPrisons;
import dev.nishu.bettercosmic.prisons.chestsearch.ClueScrollProvider;
import dev.nishu.bettercosmic.prisons.client.BetterPrisonsClient;
import dev.nishu.bettercosmic.prisons.hud.EventsHud;
import dev.nishu.bettercosmic.prisons.screen.WaypointsScreen;
import dev.nishu.bettercosmic.prisons.waypoint.CustomWaypoint;
import dev.nishu.bettercosmic.prisons.waypoint.WaypointManager;
import dev.nishu.bettercosmic.shared.command.DevCommands;
import dev.nishu.bettercosmic.shared.notification.ToastRenderer;
import dev.nishu.bettercosmic.shared.render.FloatingTextRenderer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

/**
 * BetterPrisons developer/debug commands (client-side, Brigadier), gated behind the shared developer
 * mode ({@code /bdev}) via the same {@link DevCommands#devModeEnabled} predicate the shared commands
 * use — so they are hidden from tab-completion until {@code /bdev on}, and the shared toggle's
 * command-tree refresh reveals them immediately.
 *
 * <p>Ported from BetterPrisons' {@code devtools/DevCommands} (Yarn → Mojang). Commands whose
 * dependencies are not yet ported are intentionally omitted: {@code /bpitem} (now the shared
 * {@code /bitem}), {@code /calc} (EnergyCalculator), {@code /bptest} (gang pings), and
 * {@code /bploadcmd}.
 */
public final class PrisonDevCommands {

	private PrisonDevCommands() {}

	/** Hooks client command registration. Call once from the BetterPrisons client init. */
	public static void register() {
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
			// /bpblock — coordinates of the block under the crosshair.
			dispatcher.register(ClientCommandManager.literal("bpblock")
					.requires(DevCommands::devModeEnabled)
					.executes(PrisonDevCommands::getBlockLookingAt));

			// /bpworld — the current dimension key.
			dispatcher.register(ClientCommandManager.literal("bpworld")
					.requires(DevCommands::devModeEnabled)
					.executes(ctx -> {
						ctx.getSource().sendFeedback(Component.literal("§7Current world: §f" + WaypointManager.detectWorldKey()));
						return 1;
					}));

			// /bpwaypoints — open the waypoints management screen.
			dispatcher.register(ClientCommandManager.literal("bpwaypoints")
					.requires(DevCommands::devModeEnabled)
					.executes(ctx -> {
						ctx.getSource().getClient().execute(() ->
								ctx.getSource().getClient().setScreen(new WaypointsScreen()));
						return 1;
					}));

			// /bpscoreboard — dump the sidebar objective to the console.
			dispatcher.register(ClientCommandManager.literal("bpscoreboard")
					.requires(DevCommands::devModeEnabled)
					.executes(PrisonDevCommands::dumpScoreboard));

			// /bpclue — list the NBT step types of the held clue scroll.
			dispatcher.register(ClientCommandManager.literal("bpclue")
					.requires(DevCommands::devModeEnabled)
					.executes(PrisonDevCommands::inspectClueScroll));

			// /bpfloat [message] — spawn test world-space floating text at the player's target.
			dispatcher.register(ClientCommandManager.literal("bpfloat")
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

			// /bptoast [message] — show a test toast.
			dispatcher.register(ClientCommandManager.literal("bptoast")
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
										Component.literal("BetterPrisons").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD),
										Component.literal(StringArgumentType.getString(ctx, "message")));
								return 1;
							})));

			registerEvents(dispatcher);
			registerWaypoint(dispatcher);
		});
	}

	// -------------------------------------------------------------------------
	// /bpevents
	// -------------------------------------------------------------------------

	private static void registerEvents(com.mojang.brigadier.CommandDispatcher<FabricClientCommandSource> dispatcher) {
		dispatcher.register(ClientCommandManager.literal("bpevents")
				.requires(DevCommands::devModeEnabled)
				.then(ClientCommandManager.literal("clear")
						.executes(ctx -> {
							BetterPrisonsClient.eventsHud.clearMeteors();
							BetterPrisonsClient.eventsHud.clearMerchants();
							ctx.getSource().sendFeedback(Component.literal("§aCleared all meteors and merchants."));
							return 1;
						}))
				.then(ClientCommandManager.literal("meteor")
						.then(ClientCommandManager.literal("add")
								.then(ClientCommandManager.argument("x", IntegerArgumentType.integer())
								.then(ClientCommandManager.argument("y", IntegerArgumentType.integer())
								.then(ClientCommandManager.argument("z", IntegerArgumentType.integer())
										.executes(ctx -> addMeteor(ctx, EventsHud.MeteorType.NATURAL))
										.then(ClientCommandManager.argument("type", StringArgumentType.word())
												.suggests((ctx, builder) -> {
													builder.suggest("natural");
													builder.suggest("summoned");
													return builder.buildFuture();
												})
												.executes(ctx -> {
													String type = StringArgumentType.getString(ctx, "type");
													EventsHud.MeteorType meteorType = type.equalsIgnoreCase("summoned")
															? EventsHud.MeteorType.SUMMONED : EventsHud.MeteorType.NATURAL;
													return addMeteor(ctx, meteorType);
												}))))))
						.then(ClientCommandManager.literal("crash")
								.then(ClientCommandManager.argument("x", IntegerArgumentType.integer())
								.then(ClientCommandManager.argument("y", IntegerArgumentType.integer())
								.then(ClientCommandManager.argument("z", IntegerArgumentType.integer())
										.executes(ctx -> {
											String coordsLine = coords(ctx);
											BetterPrisonsClient.eventsHud.onMeteorCrashed(coordsLine);
											ctx.getSource().sendFeedback(Component.literal("§aMeteor crashed at " + coordsLine));
											return 1;
										})))))
						.then(ClientCommandManager.literal("clear")
								.executes(ctx -> {
									BetterPrisonsClient.eventsHud.clearMeteors();
									ctx.getSource().sendFeedback(Component.literal("§aCleared all meteors."));
									return 1;
								})))
				.then(ClientCommandManager.literal("merchant")
						.then(ClientCommandManager.literal("add")
								.then(ClientCommandManager.argument("tier", StringArgumentType.word())
										.suggests((ctx, builder) -> {
											for (EventsHud.MerchantType t : EventsHud.MerchantType.values()) {
												if (t != EventsHud.MerchantType.UNKNOWN) {
													builder.suggest(t.name().toLowerCase());
												}
											}
											return builder.buildFuture();
										})
										.then(ClientCommandManager.argument("x", IntegerArgumentType.integer())
										.then(ClientCommandManager.argument("y", IntegerArgumentType.integer())
										.then(ClientCommandManager.argument("z", IntegerArgumentType.integer())
												.executes(ctx -> {
													String tier = StringArgumentType.getString(ctx, "tier");
													int x = IntegerArgumentType.getInteger(ctx, "x");
													int y = IntegerArgumentType.getInteger(ctx, "y");
													int z = IntegerArgumentType.getInteger(ctx, "z");
													BetterPrisonsClient.eventsHud.onMerchantSpawned(tier, x, y, z);
													ctx.getSource().sendFeedback(Component.literal(
															"§aAdded " + tier + " merchant at " + x + ", " + y + ", " + z));
													return 1;
												}))))))
						.then(ClientCommandManager.literal("kill")
								.then(ClientCommandManager.argument("x", IntegerArgumentType.integer())
								.then(ClientCommandManager.argument("y", IntegerArgumentType.integer())
								.then(ClientCommandManager.argument("z", IntegerArgumentType.integer())
										.executes(ctx -> {
											int x = IntegerArgumentType.getInteger(ctx, "x");
											int y = IntegerArgumentType.getInteger(ctx, "y");
											int z = IntegerArgumentType.getInteger(ctx, "z");
											BetterPrisonsClient.eventsHud.onMerchantSlain("unknown", x, y, z);
											ctx.getSource().sendFeedback(Component.literal(
													"§aMerchant killed at " + x + ", " + y + ", " + z));
											return 1;
										})))))
						.then(ClientCommandManager.literal("clear")
								.executes(ctx -> {
									BetterPrisonsClient.eventsHud.clearMerchants();
									ctx.getSource().sendFeedback(Component.literal("§aCleared all merchants."));
									return 1;
								}))));
	}

	// -------------------------------------------------------------------------
	// /bpwaypoint
	// -------------------------------------------------------------------------

	private static void registerWaypoint(com.mojang.brigadier.CommandDispatcher<FabricClientCommandSource> dispatcher) {
		dispatcher.register(ClientCommandManager.literal("bpwaypoint")
				.requires(DevCommands::devModeEnabled)
				.then(ClientCommandManager.literal("add")
						.then(ClientCommandManager.argument("name", StringArgumentType.word())
						.then(ClientCommandManager.argument("x", IntegerArgumentType.integer())
						.then(ClientCommandManager.argument("y", IntegerArgumentType.integer())
						.then(ClientCommandManager.argument("z", IntegerArgumentType.integer())
								.executes(ctx -> {
									String name = StringArgumentType.getString(ctx, "name");
									int x = IntegerArgumentType.getInteger(ctx, "x");
									int y = IntegerArgumentType.getInteger(ctx, "y");
									int z = IntegerArgumentType.getInteger(ctx, "z");
									BetterPrisonsClient.waypointManager.add(new CustomWaypoint(name, x, y, z, 0xFFFFFF));
									ctx.getSource().sendFeedback(Component.literal("§aWaypoint '" + name + "' added at " + x + ", " + y + ", " + z));
									return 1;
								})
								.then(ClientCommandManager.argument("color", StringArgumentType.word())
										.executes(ctx -> {
											String name = StringArgumentType.getString(ctx, "name");
											int x = IntegerArgumentType.getInteger(ctx, "x");
											int y = IntegerArgumentType.getInteger(ctx, "y");
											int z = IntegerArgumentType.getInteger(ctx, "z");
											int color;
											try {
												color = (int) Long.parseLong(
														StringArgumentType.getString(ctx, "color").replace("#", ""), 16) & 0xFFFFFF;
											} catch (NumberFormatException e) {
												color = 0xFFFFFF;
											}
											BetterPrisonsClient.waypointManager.add(new CustomWaypoint(name, x, y, z, color));
											ctx.getSource().sendFeedback(Component.literal("§aWaypoint '" + name + "' added."));
											return 1;
										})))))))
				.then(ClientCommandManager.literal("here")
						.then(ClientCommandManager.argument("name", StringArgumentType.word())
								.executes(ctx -> {
									var player = ctx.getSource().getClient().player;
									if (player == null) {
										return 0;
									}
									String name = StringArgumentType.getString(ctx, "name");
									int x = (int) player.getX(), y = (int) player.getY(), z = (int) player.getZ();
									BetterPrisonsClient.waypointManager.add(new CustomWaypoint(name, x, y, z, 0xFFFFFF));
									ctx.getSource().sendFeedback(Component.literal("§aWaypoint '" + name + "' added at your position."));
									return 1;
								})))
				.then(ClientCommandManager.literal("remove")
						.then(ClientCommandManager.argument("name", StringArgumentType.greedyString())
								.executes(ctx -> {
									String name = StringArgumentType.getString(ctx, "name");
									var wps = BetterPrisonsClient.waypointManager.getAll();
									for (int i = 0; i < wps.size(); i++) {
										if (wps.get(i).name.equalsIgnoreCase(name)) {
											BetterPrisonsClient.waypointManager.remove(i);
											ctx.getSource().sendFeedback(Component.literal("§aRemoved waypoint '" + name + "'."));
											return 1;
										}
									}
									ctx.getSource().sendFeedback(Component.literal("§cNo waypoint named '" + name + "'."));
									return 0;
								})))
				.then(ClientCommandManager.literal("list")
						.executes(ctx -> {
							var wps = BetterPrisonsClient.waypointManager.getAll();
							if (wps.isEmpty()) {
								ctx.getSource().sendFeedback(Component.literal("§7No waypoints."));
								return 1;
							}
							ctx.getSource().sendFeedback(Component.literal("§eWaypoints:"));
							for (CustomWaypoint wp : wps) {
								String status = wp.enabled ? "§a[ON]" : "§c[OFF]";
								ctx.getSource().sendFeedback(Component.literal(
										status + " §f" + wp.name + " §7" + wp.x + ", " + wp.y + ", " + wp.z));
							}
							return 1;
						}))
				.then(ClientCommandManager.literal("clear")
						.executes(ctx -> {
							int count = BetterPrisonsClient.waypointManager.getAll().size();
							while (!BetterPrisonsClient.waypointManager.getAll().isEmpty()) {
								BetterPrisonsClient.waypointManager.remove(0);
							}
							ctx.getSource().sendFeedback(Component.literal("§aCleared " + count + " waypoints."));
							return 1;
						})));
	}

	// -------------------------------------------------------------------------
	// Handlers
	// -------------------------------------------------------------------------

	private static int addMeteor(CommandContext<FabricClientCommandSource> ctx, EventsHud.MeteorType type) {
		int x = IntegerArgumentType.getInteger(ctx, "x");
		int y = IntegerArgumentType.getInteger(ctx, "y");
		int z = IntegerArgumentType.getInteger(ctx, "z");
		BetterPrisonsClient.eventsHud.onMeteorFalling(x + "x, " + y + "y, " + z + "z", type);
		ctx.getSource().sendFeedback(Component.literal(
				"§aAdded " + type.name().toLowerCase() + " meteor at " + x + ", " + y + ", " + z));
		return 1;
	}

	private static String coords(CommandContext<FabricClientCommandSource> ctx) {
		return IntegerArgumentType.getInteger(ctx, "x") + "x, "
				+ IntegerArgumentType.getInteger(ctx, "y") + "y, "
				+ IntegerArgumentType.getInteger(ctx, "z") + "z";
	}

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
		BetterPrisons.LOGGER.info("Block at {}, {}, {}: {}", pos.getX(), pos.getY(), pos.getZ(),
				client.level.getBlockState(pos).getBlock());
		ctx.getSource().sendFeedback(Component.literal("§aBlock: §f" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ()));
		return 1;
	}

	private static int dumpScoreboard(CommandContext<FabricClientCommandSource> ctx) {
		Minecraft client = ctx.getSource().getClient();
		if (client.level == null) {
			ctx.getSource().sendFeedback(Component.literal("§cNo world loaded"));
			return 0;
		}
		Scoreboard scoreboard = client.level.getScoreboard();
		Objective sidebar = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR);
		BetterPrisons.LOGGER.info("========== SIDEBAR DUMP ==========");
		if (sidebar == null) {
			BetterPrisons.LOGGER.info("(no sidebar objective)");
			ctx.getSource().sendFeedback(Component.literal("§7No sidebar objective."));
			return 1;
		}
		BetterPrisons.LOGGER.info("Objective: {} ({})", sidebar.getName(), sidebar.getDisplayName().getString());
		int line = 1;
		for (PlayerScoreEntry entry : scoreboard.listPlayerScores(sidebar)) {
			String owner = entry.owner();
			PlayerTeam team = scoreboard.getPlayersTeam(owner);
			Component display = team != null
					? PlayerTeam.formatNameForTeam(team, Component.literal(owner)) : Component.literal(owner);
			String full = display.getString();
			BetterPrisons.LOGGER.info("  [{}] \"{}\" | stripped: \"{}\" | value: {}",
					line, full, full.replaceAll("§.", ""), entry.value());
			line++;
		}
		BetterPrisons.LOGGER.info("==================================");
		ctx.getSource().sendFeedback(Component.literal("§aSidebar dumped to console. Check latest.log"));
		return 1;
	}

	private static int inspectClueScroll(CommandContext<FabricClientCommandSource> ctx) {
		Minecraft client = ctx.getSource().getClient();
		if (client.player == null) {
			return 0;
		}
		ItemStack stack = client.player.getMainHandItem();
		if (stack.isEmpty()) {
			ctx.getSource().sendFeedback(Component.literal("§cNo item in main hand"));
			return 0;
		}
		try {
			CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
			CompoundTag bukkit = customData == null ? null
					: customData.copyTag().getCompound("PublicBukkitValues").orElse(null);
			if (bukkit == null || !"clue_scroll".equals(bukkit.getString("cosmicprisons:custom_item_id").orElse(""))) {
				ctx.getSource().sendFeedback(Component.literal("§cHeld item is not a clue scroll"));
				return 0;
			}
			String json = bukkit.getString("cosmicprisons:clue_scroll_data").orElse("");
			JsonObject root = JsonParser.parseString(json).getAsJsonObject();
			JsonArray clues = root.getAsJsonArray("clues");
			int currentIdx = root.has("currentClueIndex") ? root.get("currentClueIndex").getAsInt() : -1;
			String tier = root.has("tier") ? root.get("tier").getAsString() : "?";

			ctx.getSource().sendFeedback(Component.literal(
					"§6===== Clue Scroll (§e" + tier + "§6) — current index §e" + currentIdx + " §6====="));
			BetterPrisons.LOGGER.info("===== CLUE SCROLL (tier={}, currentIndex={}) =====", tier, currentIdx);

			for (int i = 0; i < clues.size(); i++) {
				JsonObject clue = clues.get(i).getAsJsonObject();
				String type = clue.has("type") ? clue.get("type").getAsString() : "?";
				boolean completed = clue.has("completed") && clue.get("completed").getAsBoolean();
				Integer step = ClueScrollProvider.getStep(type);
				String stepStr = step != null ? "step " + step : "§cUNMAPPED";
				boolean active = (i == currentIdx);
				ctx.getSource().sendFeedback(Component.literal(String.format("§7[%d]%s §f%s §7→ %s§7%s",
						i, active ? " §a*" : "", type, stepStr, completed ? " §8(done)" : "")));
				BetterPrisons.LOGGER.info("[{}]{} type={} -> {}{}",
						i, active ? " *" : "", type, stepStr.replace("§c", ""), completed ? " (done)" : "");
			}
			return 1;
		} catch (Exception e) {
			ctx.getSource().sendFeedback(Component.literal("§cFailed to parse clue scroll: " + e.getMessage()));
			BetterPrisons.LOGGER.warn("Failed to parse clue scroll: {}", e.getMessage());
			return 0;
		}
	}
}
