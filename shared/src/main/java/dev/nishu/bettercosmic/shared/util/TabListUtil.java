package dev.nishu.bettercosmic.shared.util;

import dev.nishu.bettercosmic.shared.mixin.PlayerTabOverlayAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/**
 * Content-agnostic access to the player-list (tab) header and footer. The server sets these over the
 * tab-list packets and the client mirrors them on {@code PlayerTabOverlay}, which keeps them as
 * private fields; this reads them via {@link PlayerTabOverlayAccessor}. Nothing here is
 * server-specific, so it lives in the shared library — callers that want to parse the text for a
 * particular network (e.g. a planet name) build on top of it.
 */
public final class TabListUtil {

	private TabListUtil() {}

	/** The tab-list header component, or {@code null} when unavailable or unset by the server. */
	public static Component header() {
		Minecraft client = Minecraft.getInstance();
		if (client.gui == null) {
			return null;
		}
		return ((PlayerTabOverlayAccessor) client.gui.getTabList()).bettercosmic$getHeader();
	}

	/** The tab-list footer component, or {@code null} when unavailable or unset by the server. */
	public static Component footer() {
		Minecraft client = Minecraft.getInstance();
		if (client.gui == null) {
			return null;
		}
		return ((PlayerTabOverlayAccessor) client.gui.getTabList()).bettercosmic$getFooter();
	}

	/** The header as plain text with colour codes removed, or an empty string when unset. */
	public static String headerText() {
		return strip(header());
	}

	/** The footer as plain text with colour codes removed, or an empty string when unset. */
	public static String footerText() {
		return strip(footer());
	}

	private static String strip(Component component) {
		return component == null ? "" : component.getString().replaceAll("§.", "");
	}
}
