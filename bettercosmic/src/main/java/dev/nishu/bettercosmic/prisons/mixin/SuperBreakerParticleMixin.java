package dev.nishu.bettercosmic.prisons.mixin;

import dev.nishu.bettercosmic.prisons.PrisonsGate;
import dev.nishu.bettercosmic.prisons.enchants.SuperBreakerDetector;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.FlameParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.SpellParticle;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Reports flame/spell particles spawned within a block of the player to {@link SuperBreakerDetector},
 * which correlates them with the dragon-growl sound to detect a Super Breaker proc. Ported from
 * BetterPrisons' {@code ParticleDebugMixin} (its real purpose is Super Breaker detection).
 */
@Mixin(ParticleEngine.class)
public class SuperBreakerParticleMixin {

	@Inject(method = "add", at = @At("HEAD"))
	private void betterprisons$detectSuperBreakerParticle(Particle particle, CallbackInfo ci) {
		if (!PrisonsGate.active()) {
			return;
		}
		if (!(particle instanceof FlameParticle || particle instanceof SpellParticle)) {
			return;
		}
		Minecraft client = Minecraft.getInstance();
		if (client.player == null) {
			return;
		}
		Vec3 center = particle.getBoundingBox().getCenter();
		double dx = center.x - client.player.getX();
		double dz = center.z - client.player.getZ();
		double distance = Math.sqrt(dx * dx + dz * dz); // horizontal distance, matching BetterPrisons
		if (distance <= 1.0) {
			SuperBreakerDetector.considerParticle(distance);
		}
	}
}
