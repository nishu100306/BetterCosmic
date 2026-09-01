package dev.nishu.bettercosmic.prisons.misc;

import dev.nishu.bettercosmic.prisons.BetterPrisons;
import dev.nishu.bettercosmic.prisons.client.BetterPrisonsClient;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.ResourcePackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.repository.PackRepository;

import java.util.Objects;

/**
 * Bundles the PrisonBreak ore texture pack inside the mod jar and auto-applies it while the player is
 * in the {@code minecraft:prisonbreak} world, removing it on leave. Toggling a pack requires a full
 * {@link Minecraft#reloadResourcePacks()}, so there's a brief reload hitch on each transition. Ported from
 * BetterPrisons' {@code misc/PrisonbreakTexturePack} (Yarn → Mojang: {@code ResourcePackManager} →
 * {@code PackRepository}; the Yarn {@code enable}/{@code disable} helpers don't exist, so the selection
 * is driven through {@code options.resourcePacks}, which {@code reloadResources} applies).
 */
public final class PrisonbreakTexturePack {

	private static final Identifier PACK_ID = Identifier.fromNamespaceAndPath("betterprisons", "prisonbreak");

	/** Sentinel config value meaning "use the mod's built-in pack". */
	public static final String BUNDLED = "Bundled";

	private static String resolvedBundledId = null;
	/** The pack id currently applied, or null if none. */
	private static String appliedPackId = null;

	private PrisonbreakTexturePack() {}

	/** Registers the bundled pack so the game can load it from inside the jar. Call once at init. */
	public static void register() {
		ModContainer mod = FabricLoader.getInstance().getModContainer(BetterPrisons.FABRIC_MOD_ID).orElse(null);
		if (mod == null) {
			BetterPrisons.LOGGER.warn("Could not find mod container; PrisonBreak texture pack not registered");
			return;
		}
		ResourceManagerHelper.registerBuiltinResourcePack(PACK_ID, mod, ResourcePackActivationType.NORMAL);
	}

	/**
	 * Called each client tick with whether the player is in the prisonbreak world. Applies the selected
	 * pack while there and removes it on leave, only touching resources on an actual change.
	 */
	public static void update(boolean inPrisonbreak) {
		Minecraft client = Minecraft.getInstance();
		PackRepository rpm = client.getResourcePackRepository();

		String want = (inPrisonbreak && BetterPrisonsClient.config.prisonbreakTexturePackEnabled)
				? resolveSelectedId(rpm) : null;

		if (Objects.equals(want, appliedPackId)) {
			return;
		}

		boolean changed = false;
		if (appliedPackId != null && client.options.resourcePacks.remove(appliedPackId)) {
			changed = true;
		}
		if (want != null && rpm.getPack(want) != null && !client.options.resourcePacks.contains(want)) {
			client.options.resourcePacks.add(want); // end of list = highest priority
			changed = true;
		}
		appliedPackId = want;
		if (changed) {
			BetterPrisons.LOGGER.info("PrisonBreak texture pack -> {}", want == null ? "none" : want);
			client.reloadResourcePacks();
		}
	}

	/** Resolves the configured pack's profile id, falling back to the bundled pack. */
	private static String resolveSelectedId(PackRepository rpm) {
		String sel = BetterPrisonsClient.config.prisonbreakTexturePack;
		if (sel != null && !sel.isEmpty() && !sel.equals(BUNDLED) && rpm.getPack(sel) != null) {
			return sel;
		}
		return resolveBundledId(rpm);
	}

	/** Finds the bundled pack's profile id (its id contains "prisonbreak"). */
	private static String resolveBundledId(PackRepository rpm) {
		if (resolvedBundledId != null && rpm.getPack(resolvedBundledId) != null) {
			return resolvedBundledId;
		}
		for (String id : rpm.getAvailableIds()) {
			if (id.contains("prisonbreak")) {
				resolvedBundledId = id;
				return id;
			}
		}
		return null;
	}
}
