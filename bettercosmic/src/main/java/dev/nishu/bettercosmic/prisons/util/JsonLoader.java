package dev.nishu.bettercosmic.prisons.util;

import dev.nishu.bettercosmic.prisons.hud.CooldownHud.CommandDef;

import java.util.ArrayList;
import java.util.List;

/**
 * Supplies the built-in Cooldown HUD command definitions.
 *
 * <p>Ported from BetterPrisons' {@code JsonLoader}, which round-tripped these through
 * {@code commands.json} but <em>always</em> regenerated the file from these same defaults on load
 * (so it was never actually user-editable). The port drops the pointless file I/O and returns the
 * defaults directly — behavior is identical.
 */
public final class JsonLoader {

	private JsonLoader() {}

	public static List<CommandDef> loadCommands() {
		List<CommandDef> defs = new ArrayList<>();
		defs.add(def("/home", "startsWith", 60, "Home", null, null, "minecraft:red_bed"));
		defs.add(def("/jet", "exact", 30, "Jet", null, List.of("/jetpack"), "minecraft:blaze_powder"));
		defs.add(def("/feed", "startsWith", 180, "Feed", null, List.of("/eat"), "minecraft:cooked_beef"));
		defs.add(def("/fix", "startsWith", 180, "Fix", null, null, "minecraft:anvil"));
		defs.add(def("", "exact", 10, "Combat",
				"§c§l(!) §cYou have entered combat. Do not log out for 10s!", null, "minecraft:diamond_sword"));
		defs.add(def("/tpa ", "startsWith", 180, "tpa", null, null, "minecraft:experience_bottle"));
		defs.add(def("/tpahere ", "startsWith", 240, "tpahere", null, null, "minecraft:experience_bottle"));
		defs.add(def("/dangle", "exact", 30, "Dangle", "§aDangling your item!", null, "minecraft:fishing_rod"));
		defs.add(def("/adangle", "exact", 20, "Adangle", null, null, "minecraft:iron_chestplate"));
		return defs;
	}

	private static CommandDef def(String command, String matchType, int cooldown, String displayName,
								  String chatPattern, List<String> aliases, String icon) {
		CommandDef d = new CommandDef();
		d.command = command;
		d.matchType = matchType;
		d.cooldown = cooldown;
		d.displayName = displayName;
		d.chatPattern = chatPattern;
		d.aliases = aliases == null ? null : new ArrayList<>(aliases);
		d.icon = icon;
		return d;
	}
}
