package dev.nishu.bettercosmic.shared.peacefulmining;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;

import java.util.HashSet;
import java.util.Set;

/**
 * Shared "peaceful mining" mechanism: while active, nearby players are drawn as translucent ghosts
 * you can see and mine through, and interaction/attacks on entities are suppressed. This is the
 * content-agnostic engine — <em>when</em> it is active, <em>which</em> players are targets, and the
 * ghost opacity are decided by a mod-supplied {@link Policy}, so both BetterPrisons and BetterSky can
 * use it with their own rules.
 *
 * <p>Ported and generalized from BetterPrisons. The shared render mixins
 * ({@code LivingEntityRendererPeacefulMiningMixin}, {@code GameRendererPeacefulMiningMixin}) read the
 * {@link #TARGETS} set and {@link #opacity()}; this class recomputes the target set each client tick
 * from the registered policy and blocks entity interaction/attacks via Fabric events while active.
 */
public final class PeacefulMining {

	/** Per-mod policy deciding activation, targeting, and ghost opacity. */
	public interface Policy {
		/** Whether peaceful mining is globally active right now (e.g. holding a mining tool). */
		boolean isActive();

		/** Whether {@code other} (never the local player) should be ghosted — typically a distance test. */
		boolean isTarget(Player other);

		/** Ghost opacity, 0–255. */
		int opacity();
	}

	/** Entity network IDs to render as ghosts this frame. Written on the client thread each tick. */
	public static final Set<Integer> TARGETS = new HashSet<>();

	private static Policy policy;

	private PeacefulMining() {}

	/** Registers the mod's policy. The last registration wins (one active policy per game). */
	public static void setPolicy(Policy p) {
		policy = p;
	}

	/** Whether peaceful mining is globally active (per the registered policy). */
	public static boolean isActive() {
		return policy != null && policy.isActive();
	}

	/** Ghost opacity (0–255), or fully opaque if no policy is set. */
	public static int opacity() {
		return policy != null ? policy.opacity() : 255;
	}

	/** Whether the given entity id is a current ghost target. */
	public static boolean isTarget(int entityId) {
		return TARGETS.contains(entityId);
	}

	/**
	 * Hooks the per-tick target computation and the interaction/attack blocking. Call once from a
	 * shared/mod client init.
	 */
	public static void init() {
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			TARGETS.clear();
			if (policy == null || !policy.isActive() || client.level == null || client.player == null) {
				return;
			}
			for (Player other : client.level.players()) {
				if (other != client.player && policy.isTarget(other)) {
					TARGETS.add(other.getId());
				}
			}
		});

		// While active, suppress all entity interaction and attacks so you can mine undisturbed.
		UseEntityCallback.EVENT.register((player, level, hand, entity, hit) ->
				isActive() ? InteractionResult.FAIL : InteractionResult.PASS);
		AttackEntityCallback.EVENT.register((player, level, hand, entity, hit) ->
				isActive() ? InteractionResult.FAIL : InteractionResult.PASS);
	}
}
