package dev.nishu.bettercosmic.prisons.pvviewer;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.ItemStack;

/**
 * Serializes {@link ItemStack}s to and from JSON for the PV viewer's on-disk cache, using the vanilla
 * {@link ItemStack#CODEC} so custom Cosmic components (lore, enchants, custom model data, …) survive a
 * restart intact.
 *
 * <p>Encoding needs the level's {@link RegistryAccess} (item/enchantment registries are looked up by
 * id), so callers pass it in from {@code client.level.registryAccess()} at capture time. Decoding uses
 * the same registry access from whatever level is current when the viewer is opened.
 */
public final class PvItemCodec {

	private PvItemCodec() {}

	/** Encodes a stack to a JSON element, or {@link JsonNull} for an empty/null stack. Never throws. */
	public static JsonElement encode(ItemStack stack, RegistryAccess registries) {
		if (stack == null || stack.isEmpty()) {
			return JsonNull.INSTANCE;
		}
		try {
			RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, registries);
			return ItemStack.CODEC.encodeStart(ops, stack).result().orElse(JsonNull.INSTANCE);
		} catch (Exception e) {
			return JsonNull.INSTANCE;
		}
	}

	/** Decodes a JSON element back to a stack, or {@link ItemStack#EMPTY} if absent/malformed. Never throws. */
	public static ItemStack decode(JsonElement element, RegistryAccess registries) {
		if (element == null || element.isJsonNull()) {
			return ItemStack.EMPTY;
		}
		try {
			RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, registries);
			return ItemStack.CODEC.parse(ops, element).result().orElse(ItemStack.EMPTY);
		} catch (Exception e) {
			return ItemStack.EMPTY;
		}
	}
}
