package dev.nishu.bettercosmic.shared.input;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

/**
 * Thin helpers for registering client key bindings, so each mod doesn't repeat the Fabric
 * boilerplate. A mod creates its own {@link KeyMapping.Category} (via {@link #category}) and
 * registers its keys through {@link #register}; the shared config-UI keybind is handled separately
 * by {@code ui.ConfigUi}.
 */
public final class KeyBinds {

	private KeyBinds() {}

	/** Creates (or returns) a named keybinding category, e.g. {@code category("betterprisons", "betterprisons")}. */
	public static KeyMapping.Category category(String namespace, String path) {
		return KeyMapping.Category.register(Identifier.fromNamespaceAndPath(namespace, path));
	}

	/**
	 * Registers a client keybinding bound to a GLFW key in the given category.
	 *
	 * @param translationKey the {@code key.*} translation key
	 * @param glfwKey        a {@code GLFW_KEY_*} code, or {@link GLFW#GLFW_KEY_UNKNOWN} for unbound
	 */
	public static KeyMapping register(String translationKey, int glfwKey, KeyMapping.Category category) {
		return KeyBindingHelper.registerKeyBinding(new KeyMapping(translationKey, glfwKey, category));
	}

	/** Registers an unbound client keybinding (the player assigns a key in Controls). */
	public static KeyMapping registerUnbound(String translationKey, KeyMapping.Category category) {
		return register(translationKey, GLFW.GLFW_KEY_UNKNOWN, category);
	}
}
