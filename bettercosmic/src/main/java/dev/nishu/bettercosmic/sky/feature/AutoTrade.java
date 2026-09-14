package dev.nishu.bettercosmic.sky.feature;

import dev.nishu.bettercosmic.shared.server.Network;
import dev.nishu.bettercosmic.shared.server.ServerContext;
import dev.nishu.bettercosmic.sky.client.BetterSkyClient;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;

/**
 * Auto-trade: shift-right-clicking another player sends {@code /trade <name>}. Ported from
 * BetterPrisons' {@code feature.AutoTrade}, gated on the Sky network instead of Prisons. Returns
 * {@code PASS} so it never consumes the interaction. Both mods register their own copy on the shared
 * {@link UseEntityCallback}; each self-gates on its network, so only the active one sends.
 */
public final class AutoTrade {

	private AutoTrade() {}

	public static void register() {
		UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
			if (!world.isClientSide()
					|| hand != InteractionHand.MAIN_HAND
					|| !(entity instanceof Player target)
					|| !player.isShiftKeyDown()) {
				return InteractionResult.PASS;
			}
			if (!ServerContext.isActive(Network.SKY)
					|| !BetterSkyClient.config.autoTradeEnabled) {
				return InteractionResult.PASS;
			}
			Minecraft client = Minecraft.getInstance();
			if (client.getConnection() != null) {
				client.getConnection().sendCommand("trade " + target.getGameProfile().name());
			}
			return InteractionResult.PASS;
		});
	}
}
