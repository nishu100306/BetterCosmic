package dev.nishu.bettercosmic.shared.ui.screen;

import dev.nishu.bettercosmic.shared.ui.model.Option;

/**
 * Implemented by the {@link FeaturePopup} so a {@code ColorSwatch} row can ask it to open the color
 * picker as an attached sidebar (on the popup's own layer) rather than a separate overlay.
 */
public interface ColorPickerHost {
	void openColorPicker(Option<Integer> option);
}
