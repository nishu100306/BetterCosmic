package dev.nishu.bettercosmic.prisons.feature;

import dev.nishu.bettercosmic.prisons.client.BetterPrisonsClient;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;

/**
 * Auto-trade: shift-right-clicking another player sends {@code /trade <name>}. Ported from the
 * auto-trade branch of BetterPrisons' {@code ClientPlayerInteractionMixin}, re-expressed as a Fabric
 * {@link UseEntityCallback} (the plan prefers events over mixins). Returns {@code PASS} so it never
 * consumes the interaction — the shared peaceful-mining block (also a {@code UseEntityCallback}) still
 * applies when it's active.
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
			if (!BetterPrisonsClient.config.autoTradeEnabled) {
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
