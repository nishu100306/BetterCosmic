package dev.nishu.bettercosmic.prisons.enchants;

/**
 * A player effect surfaced on the Enchant HUD, driven by the Cosmic API
 * {@code player.effects.changed} hook (wired via {@code CosmicApi}, when present). The active-effect
 * set is replaced wholesale on each hook; each entry counts down on its own. Ported from BetterPrisons.
 */
public class ApiEffect extends BaseEnchant {

	public ApiEffect(String id, String displayName) {
		super(id, displayName);
	}
}
