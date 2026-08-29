package dev.nishu.bettercosmic.prisons.enchantprocs;

import net.minecraft.network.chat.Component;

/**
 * A single enchant activation reported by the Cosmic API ({@code player.enchant_proc}).
 *
 * @param id          the enchant identifier (e.g. {@code "maneuver"}); the dispatch key
 * @param displayName the coloured display text (legacy {@code &}/{@code §} codes already normalised)
 * @param level       the enchant level, or {@code 0} if unknown
 * @param source      where the proc originated (e.g. {@code "combat"}), or {@code ""} if unknown
 * @param playerName  the player the proc belongs to, or {@code ""} if unknown
 */
public record EnchantProc(String id, Component displayName, int level, String source, String playerName) {
}
