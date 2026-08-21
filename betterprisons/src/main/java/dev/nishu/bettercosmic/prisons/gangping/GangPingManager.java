package dev.nishu.bettercosmic.prisons.gangping;

import dev.nishu.bettercosmic.prisons.BetterPrisons;
import dev.nishu.bettercosmic.prisons.waypoint.WaypointManager;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages gang-ping waypoints — transient waypoints triggered by chat pings (one per player; a new
 * ping replaces the old one; auto-expires). Sends this client's own pings (gang / truce / block-target)
 * through the server's gang-chat command, subject to a client-side cooldown. Ported from BetterPrisons'
 * {@code gangping/GangPingManager} (Yarn → Mojang: {@code getNetworkHandler().sendChatCommand/
 * sendChatMessage} → {@code getConnection().sendCommand/sendChat}, {@code getBlockPos} →
 * {@code blockPosition}, {@code getHorizontalFacing().asString} → {@code getDirection().getName}).
 */
public class GangPingManager {

	private static final long PING_TIMEOUT_MS = 60_000L;
	private static final long SEND_COOLDOWN_MS = 3_000L;

	/** Active pings keyed by player name (one per player). */
	private final Map<String, GangPingInfo> activePings = new LinkedHashMap<>();
	private long lastSendTime = 0;

	public static class GangPingInfo {
		public final String playerName;
		public final int x, y, z;
		public final String world;
		public final float hp, maxHp;
		public final String facing;
		public final long createdAt;
		public final boolean isTruce;

		public GangPingInfo(String playerName, int x, int y, int z, String world,
				float hp, float maxHp, String facing, boolean isTruce) {
			this.playerName = playerName;
			this.x = x;
			this.y = y;
			this.z = z;
			this.world = world;
			this.hp = hp;
			this.maxHp = maxHp;
			this.facing = facing;
			this.isTruce = isTruce;
			this.createdAt = System.currentTimeMillis();
		}
	}

	// ---- Send ----

	public void sendPing(Minecraft client) {
		sendPingInternal(client, false, null);
	}

	public void sendTrucePing(Minecraft client) {
		sendPingInternal(client, true, null);
	}

	/** Sends a gang ping at the given block position instead of the player's position. */
	public void sendPingAtBlock(Minecraft client, BlockPos pos) {
		sendPingInternal(client, false, pos);
	}

	private void sendPingInternal(Minecraft client, boolean truce, BlockPos overridePos) {
		if (client.player == null || client.level == null || client.getConnection() == null) {
			return;
		}
		long now = System.currentTimeMillis();
		long remaining = SEND_COOLDOWN_MS - (now - lastSendTime);
		if (remaining > 0) {
			String seconds = String.format("%.1f", remaining / 1000.0);
			client.player.displayClientMessage(
					Component.literal("§c[BetterPrisons] Ping on cooldown! Wait " + seconds + "s"), false);
			return;
		}
		lastSendTime = now;

		int x, y, z;
		if (overridePos != null) {
			x = overridePos.getX();
			y = overridePos.getY();
			z = overridePos.getZ();
		} else {
			x = client.player.blockPosition().getX();
			y = (int) Math.round(client.player.getEyeY());
			z = client.player.blockPosition().getZ();
		}
		String world = WaypointManager.detectWorldKey();
		float hp = client.player.getHealth();
		float maxHp = client.player.getMaxHealth();

		Direction dir = client.player.getDirection();
		String facing = dir.getName().substring(0, 1).toUpperCase() + dir.getName().substring(1);

		String prefix = truce ? "[T!]" : "[!]";
		String msg = String.format("%s %s has pinged at %dx %dy %dz %s | HP %.0f/%.0f | Facing %s",
				prefix, client.player.getGameProfile().name(), x, y, z, world, hp, maxHp, facing);

		client.getConnection().sendCommand(truce ? "g c t" : "g c g");
		client.getConnection().sendChat(msg);
	}

	// ---- Receive ----

	public void onGangPingReceived(String playerName, int x, int y, int z, String world,
			float hp, float maxHp, String facing, boolean isTruce) {
		GangPingInfo info = new GangPingInfo(playerName, x, y, z, world, hp, maxHp, facing, isTruce);
		activePings.put(playerName, info);
		BetterPrisons.LOGGER.info("{} ping from {} at {}, {}, {} ({})",
				isTruce ? "Truce" : "Gang", playerName, x, y, z, world);
	}

	// ---- Tick — expire old pings ----

	public void tick() {
		long now = System.currentTimeMillis();
		activePings.values().removeIf(p -> now - p.createdAt > PING_TIMEOUT_MS);
	}

	// ---- Accessors ----

	public List<GangPingInfo> getActivePings() {
		return new ArrayList<>(activePings.values());
	}

	public void clear() {
		activePings.clear();
	}

	/** Distance-based opacity: starts at base, fades toward a floor as the ping gets very close. */
	public static float calculateOpacity(float distance, float baseOpacity) {
		float fadeStart = 10f;
		float minOpacity = baseOpacity * 0.3f;
		if (distance <= 0) {
			return minOpacity;
		}
		if (distance >= fadeStart) {
			return baseOpacity;
		}
		return minOpacity + (baseOpacity - minOpacity) * (distance / fadeStart);
	}

	/** Distance-based scale: grows to {@code maxScale} at ~75 blocks, then holds. */
	public static float calculateScale(float distance, float minScale, float maxScale, boolean distanceScaling) {
		if (!distanceScaling) {
			return minScale;
		}
		float peakDistance = 75f;
		if (distance <= peakDistance) {
			return minScale + (maxScale - minScale) * (distance / peakDistance);
		}
		return maxScale;
	}
}
