package dev.nishu.bettercosmic.prisons.mixin;

import dev.nishu.bettercosmic.prisons.client.BetterPrisonsClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundDamageEventPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Refreshes the Cooldown HUD's Combat cooldown when the local player is damaged by another player. The
 * damage packet carries the attacker's entity id, so player-sourced hits can be told apart. Ported from
 * BetterPrisons' {@code LocalPlayerHurtMixin} (Yarn → Mojang: {@code ClientPlayNetworkHandler.
 * onEntityDamage(EntityDamageS2CPacket)} → {@code ClientPacketListener.handleDamageEvent(
 * ClientboundDamageEventPacket)}). Injected at TAIL so it runs on the main client thread, after the
 * packet handler's thread reschedule.
 */
@Mixin(ClientPacketListener.class)
public class LocalPlayerHurtMixin {

	@Inject(method = "handleDamageEvent", at = @At("TAIL"))
	private void bettercosmic$resetCombatOnHurt(ClientboundDamageEventPacket packet, CallbackInfo ci) {
		Minecraft client = Minecraft.getInstance();
		if (client.player == null || client.level == null || BetterPrisonsClient.cooldownHud == null) {
			return;
		}
		if (packet.entityId() != client.player.getId()) {
			return;
		}
		int causeId = packet.sourceCauseId();
		if (causeId > 0) {
			Entity cause = client.level.getEntity(causeId);
			if (cause instanceof Player && cause != client.player) {
				BetterPrisonsClient.cooldownHud.resetCombatCooldown();
			}
		}
	}
}
