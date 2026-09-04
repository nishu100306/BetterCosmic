package dev.nishu.bettercosmic.prisons.misc;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * Calculates Cosmic Energy costs for pickaxe level-ups on CosmicPrisons.
 *
 * <p>Ported from BetterPrisons' {@code misc/EnergyCalculator}, trimmed to pickaxes only
 * (Stone / Iron / Diamond) and updated to the current prestige-dependent formula.
 *
 * <p>Cost of a single upgrade <em>to</em> level {@code L} at prestige stage {@code P}:
 * <pre>
 *   cost(L, P) = (startCost + (L - 2) × increasePerLevel) × (1 + P × 0.08)
 * </pre>
 * where {@code startCost} / {@code increasePerLevel} are the P0 numbers below.
 * {@code L} is the level being upgraded to (2–100); {@code P} is the prestige stage (0–10).
 * At P0, level 2 costs exactly {@code startCost} and each further level adds
 * {@code increasePerLevel}; the prestige multiplier then scales the whole thing.
 */
public final class EnergyCalculator {

    /** Lowest level that can be upgraded to (level 1 is the starting level, so it has no cost). */
    public static final int MIN_LEVEL = 2;
    /** Highest reachable pickaxe level. */
    public static final int MAX_LEVEL = 100;
    /** Prestige stage range. */
    public static final int MIN_PRESTIGE = 0;
    public static final int MAX_PRESTIGE = 10;
    /** Each prestige stage adds 8% to every level-up cost. */
    private static final double PRESTIGE_STEP = 0.08;

    private static final NumberFormat NUMBER_FORMAT = NumberFormat.getNumberInstance(Locale.US);

    /** P0 cost of level 2 ({@code startCost}) and the per-level increase, for each supported pickaxe. */
    public enum PickType {
        STONE(864, 1_728),
        IRON(4_536, 7_128),
        DIAMOND(6_048, 9_072);

        public final long startCost;
        public final long increasePerLevel;

        PickType(long startCost, long increasePerLevel) {
            this.startCost = startCost;
            this.increasePerLevel = increasePerLevel;
        }
    }

    private EnergyCalculator() {}

    /**
     * Energy cost of the single upgrade that raises the pickaxe <em>to</em> {@code level}, at the
     * given prestige stage. Returns 0 for levels below {@link #MIN_LEVEL}.
     */
    public static long singleLevelCost(PickType type, int level, int prestige) {
        if (level < MIN_LEVEL) {
            return 0;
        }
        long p0Cost = type.startCost + (long) (level - 2) * type.increasePerLevel;
        double prestigeMultiplier = 1.0 + prestige * PRESTIGE_STEP;
        return Math.round(p0Cost * prestigeMultiplier);
    }

    /**
     * Total energy to go from {@code startLevel} up to {@code endLevel} at the given prestige stage,
     * i.e. the sum of every individual level-up in between (each rounded on its own, matching the
     * per-level cost the game shows). {@code startLevel}'s own level has already been paid for, so it
     * is not included; the range is the upgrades producing levels {@code startLevel + 1 … endLevel}.
     */
    public static long rangeCost(PickType type, int startLevel, int endLevel, int prestige) {
        long total = 0;
        for (int level = startLevel + 1; level <= endLevel; level++) {
            total += singleLevelCost(type, level, prestige);
        }
        return total;
    }

    public static String formatEnergy(long energy) {
        return NUMBER_FORMAT.format(energy);
    }
}
