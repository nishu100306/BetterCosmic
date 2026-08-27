package dev.nishu.bettercosmic.prisons.feature;

import dev.nishu.bettercosmic.prisons.client.BetterPrisonsClient;
import dev.nishu.bettercosmic.prisons.config.PrisonsConfig;
import dev.nishu.bettercosmic.shared.peacefulmining.PeacefulMining;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MaceItem;

/**
 * BetterPrisons' peaceful-mining policy for the shared {@link PeacefulMining} engine: active while
 * holding a pickaxe/mace (each independently toggleable) or always in the PrisonBreak world, ghosting
 * other players within a configurable radius. Reads {@link PrisonsConfig}.
 */
public final class PrisonsPeacefulMiningPolicy implements PeacefulMining.Policy {

	private static PrisonsConfig cfg() {
		return BetterPrisonsClient.config;
	}

	@Override
	public boolean isActive() {
		PrisonsConfig c = cfg();
		if (c == null || !c.peacefulMiningEnabled) {
			return false;
		}
		Minecraft client = Minecraft.getInstance();
		if (client.player == null) {
			return false;
		}
		if (c.peacefulMiningAlwaysInPrisonbreak && inPrisonbreak(client)) {
			return true;
		}
		return isEnabledTool(client.player.getMainHandItem()) || isEnabledTool(client.player.getOffhandItem());
	}

	@Override
	public boolean isTarget(Player other) {
		Minecraft client = Minecraft.getInstance();
		if (client.player == null) {
			return false;
		}
		double distance = Math.sqrt(client.player.distanceToSqr(other));
		return distance <= cfg().peacefulMiningDistance;
	}

	@Override
	public int opacity() {
		return cfg().peacefulMiningOpacity;
	}

	private static boolean inPrisonbreak(Minecraft client) {
		return client.level != null
				&& dev.nishu.bettercosmic.prisons.PrisonWorlds.PRISONBREAK
						.equals(client.level.dimension().identifier().toString());
	}

	private static boolean isEnabledTool(ItemStack stack) {
		if (stack.isEmpty()) {
			return false;
		}
		PrisonsConfig c = cfg();
		if (c.peacefulMiningPickaxe && isPickaxe(stack)) {
			return true;
		}
		return c.peacefulMiningMace && isMace(stack);
	}

	private static boolean isPickaxe(ItemStack stack) {
		return stack.getItem().getDescriptionId().toLowerCase().contains("pickaxe");
	}

	private static boolean isMace(ItemStack stack) {
		return stack.getItem() instanceof MaceItem
				|| stack.getItem().getDescriptionId().toLowerCase().contains("mace");
	}
}
