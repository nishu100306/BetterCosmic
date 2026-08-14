package dev.nishu.bettercosmic.shared.ui.model;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

/**
 * A client key binding, edited by a {@code KeybindButton}. Wraps a {@link KeyMapping}: the current
 * key isn't read directly (no public getter), so display and default-checks go through the mapping.
 */
public final class KeybindOption extends Option {

	public final KeyMapping mapping;

	KeybindOption(String label, KeyMapping mapping) {
		super(label);
		this.mapping = mapping;
	}

	/** Binds {@code key} (use {@link InputConstants#UNKNOWN} to unbind), refreshes and persists. */
	public void bind(InputConstants.Key key) {
		mapping.setKey(key);
		KeyMapping.resetMapping();
		Minecraft.getInstance().options.save();
	}

	@Override
	public boolean isDefault() {
		return mapping.isDefault();
	}

	@Override
	public void reset() {
		bind(mapping.getDefaultKey());
	}

	@Override
	public String displayValue() {
		return mapping.isUnbound() ? "Unbound" : mapping.getTranslatedKeyMessage().getString();
	}
}
