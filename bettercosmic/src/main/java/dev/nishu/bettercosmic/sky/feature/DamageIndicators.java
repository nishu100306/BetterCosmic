package dev.nishu.bettercosmic.sky.feature;

import dev.nishu.bettercosmic.shared.render.FloatingTextRenderer;
import dev.nishu.bettercosmic.shared.server.Network;
import dev.nishu.bettercosmic.shared.server.ServerContext;
import dev.nishu.bettercosmic.sky.client.BetterSkyClient;
import dev.nishu.bettercosmic.sky.config.SkyConfig;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.HashMap;
import java.util.Map;

/**
 * Damage indicators: floating combat text over nearby entities. Each client tick this samples the
 * health of every living entity within a radius of the player and, when one changes, spawns a rising
 * world-space number over it — red {@code -N} when it took damage, green {@code +N} when it healed —
 * via the shared {@link FloatingTextRenderer}.
 *
 * <p>It's inferred purely from client-visible health (the server syncs {@code getHealth()}), so it
 * covers any damage source, not just your own hits, and sends nothing to the server. A newly seen
 * entity is only baselined (no indicator) so entering render range doesn't fire a spurious number.
 * Gated to Cosmic Sky and a config toggle.
 */
public final class DamageIndicators {

	/** How long an indicator lives, in milliseconds. */
	private static final long DISPLAY_MS = 1000L;
	/** Ignore sub-tenth health wobble (float noise) so only real changes show. */
	private static final float MIN_DELTA = 0.05f;

	/** Last seen health per entity id, rebuilt each tick from the entities currently in range. */
	private static final Map<Integer, Float> lastHealth = new HashMap<>();
	private static final Map<Integer, Float> scratch = new HashMap<>();

	private DamageIndicators() {}

	public static void register() {
		ClientTickEvents.END_CLIENT_TICK.register(DamageIndicators::tick);
	}

	private static void tick(Minecraft client) {
		SkyConfig cfg = BetterSkyClient.config;
		if (client.level == null || client.player == null
				|| !ServerContext.isActive(Network.SKY) || !cfg.damageIndicatorsEnabled) {
			if (!lastHealth.isEmpty()) {
				lastHealth.clear(); // drop baselines so re-enabling doesn't fire stale deltas
			}
			return;
		}

		LocalPlayer player = client.player;
		double radius = Math.max(1, cfg.damageIndicatorRadius);
		double radiusSq = radius * radius;

		scratch.clear();
		for (Entity e : client.level.entitiesForRendering()) {
			if (!(e instanceof LivingEntity le) || le == player || !le.isAlive()) {
				continue;
			}
			if (le.distanceToSqr(player) > radiusSq) {
				continue;
			}
			int id = le.getId();
			float hp = le.getHealth();
			scratch.put(id, hp);

			Float prev = lastHealth.get(id);
			if (prev == null) {
				continue; // first sighting: baseline only, no indicator
			}
			float delta = hp - prev;
			if (Math.abs(delta) < MIN_DELTA) {
				continue;
			}
			boolean damage = delta < 0;
			String text = (damage ? "-" : "+") + format(Math.abs(delta));
			int rgb = damage ? cfg.damageIndicatorColor : cfg.healIndicatorColor;
			FloatingTextRenderer.spawn(
					Component.literal(text).setStyle(
							Style.EMPTY.withColor(TextColor.fromRgb(rgb)).withBold(true)),
					DISPLAY_MS, le);
		}

		lastHealth.clear();
		lastHealth.putAll(scratch);
	}

	/** Whole numbers render without a decimal (e.g. "3"); otherwise one decimal (e.g. "3.5"). */
	private static String format(float v) {
		if (Math.abs(v - Math.round(v)) < MIN_DELTA) {
			return Integer.toString(Math.round(v));
		}
		return String.format("%.1f", v);
	}
}
