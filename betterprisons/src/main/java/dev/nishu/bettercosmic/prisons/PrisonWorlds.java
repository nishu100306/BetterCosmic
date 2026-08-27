package dev.nishu.bettercosmic.prisons;

/**
 * Cosmic Prisons dimension keys, in one place. These are the server's own world identifiers (the
 * server reuses vanilla-looking {@code minecraft:} namespaces for custom worlds), observed from
 * {@code level.dimension().identifier()} — not authoritative registry values, so if the server ever
 * renames a world this is the single spot to update (and {@code /bpworld} prints the live key).
 */
public final class PrisonWorlds {

	public static final String OVERWORLD = "minecraft:overworld";
	public static final String BADLANDS = "minecraft:badlands";
	public static final String PRISONBREAK = "minecraft:prisonbreak";

	private PrisonWorlds() {}
}
