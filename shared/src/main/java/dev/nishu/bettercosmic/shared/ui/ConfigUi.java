package dev.nishu.bettercosmic.shared.ui;

import dev.nishu.bettercosmic.shared.ui.screen.ConfigScreen;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.lwjgl.glfw.GLFW;

/**
 * Entry point to the shared config UI: registers the "Open config" keybind (default <kbd>I</kbd>)
 * and opens the single {@link ConfigScreen} that lists every mod's registered panels.
 *
 * <p>Lives in {@code :shared} so BetterSky and BetterPrisons share one keybind and one screen. The
 * opening mod sets its brand tag via {@link #setSubtitle} (BetterSky → "Sky"). ModMenu, when present,
 * opens the same screen through {@link #create}.
 */
public final class ConfigUi {

	public static final String KEY_OPEN = "key.bettercosmic.open_config";

	private static KeyMapping openKey;
	private static String subtitle = "Cosmic";

	private ConfigUi() {}

	/** Sets the header brand tag (e.g. "Sky"). Call before/at client init. */
	public static void setSubtitle(String s) {
		subtitle = s == null ? "" : s;
	}

	/**
	 * The "Open config" key binding, registering it on first access. Client-mod entrypoints aren't
	 * invoked in dependency order (Fabric gathers their exceptions and continues), so a mod's panel
	 * that references this keybind (the General panel) may run before {@link #init}; lazy registration
	 * makes that order-independent. Registration is idempotent — whichever runs first wins.
	 */
	public static KeyMapping openKeyMapping() {
		if (openKey == null) {
			openKey = KeyBindingHelper.registerKeyBinding(
				new KeyMapping(KEY_OPEN, GLFW.GLFW_KEY_I, KeyMapping.Category.MISC));
		}
		return openKey;
	}

	/** Registers the keybind (if not already) and its poll loop. Call once from a shared client init. */
	public static void init() {
		openKeyMapping(); // ensure the keybind is registered
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (openKey.consumeClick()) {
				if (client.screen == null) {
					open(client, null);
				}
			}
		});
	}

	/** Opens the config screen, returning to {@code parent} on close. */
	public static void open(Minecraft client, Screen parent) {
		client.setScreen(create(parent));
	}

	/** Builds the config screen (used by the keybind and by ModMenu). */
	public static Screen create(Screen parent) {
		return new ConfigScreen(parent, subtitle);
	}
}
