package dev.nishu.bettercosmic.prisons.pvviewer;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Title detection for Cosmic's player-vault GUIs, shared by {@link PvCapture} (which snapshots the
 * contents screens) and {@link PvSidebar} (which overlays cached previews on any of them).
 *
 * <ul>
 *   <li>{@code Player Vault #X} — a specific vault's contents; {@link #vaultContentsNumber} returns X.
 *   <li>{@code Vaults} — the generic selector/index menu; {@link #isVaultSelector} is true.
 * </ul>
 */
public final class PvScreens {

	private static final Pattern CONTENTS = Pattern.compile("^Player Vault #(\\d+)$");
	/** Selector icon labels like {@code "Vault 3"} / {@code "Vault #15"}. */
	private static final Pattern SELECTOR_ICON = Pattern.compile("\\bVault\\s+#?(\\d+)\\b");

	private PvScreens() {}

	/** The vault number named by a selector icon's display name (e.g. {@code "Vault 3"}), or -1. */
	public static int vaultNumberFromLabel(String name) {
		if (name == null) {
			return -1;
		}
		Matcher m = SELECTOR_ICON.matcher(name);
		if (!m.find()) {
			return -1;
		}
		try {
			return Integer.parseInt(m.group(1));
		} catch (NumberFormatException e) {
			return -1;
		}
	}

	/** The vault number of a {@code Player Vault #X} contents screen, or -1 if the screen isn't one. */
	public static int vaultContentsNumber(Screen screen) {
		if (!(screen instanceof AbstractContainerScreen<?> s)) {
			return -1;
		}
		Matcher m = CONTENTS.matcher(s.getTitle().getString().trim());
		if (!m.matches()) {
			return -1;
		}
		try {
			return Integer.parseInt(m.group(1));
		} catch (NumberFormatException e) {
			return -1;
		}
	}

	/** Whether the screen is the generic {@code Vaults} selector menu. */
	public static boolean isVaultSelector(Screen screen) {
		return screen instanceof AbstractContainerScreen<?> s
				&& "Vaults".equals(s.getTitle().getString().trim());
	}

	/** Whether the screen is any player-vault GUI (a specific vault or the selector). */
	public static boolean isPvScreen(Screen screen) {
		return vaultContentsNumber(screen) >= 0 || isVaultSelector(screen);
	}
}
