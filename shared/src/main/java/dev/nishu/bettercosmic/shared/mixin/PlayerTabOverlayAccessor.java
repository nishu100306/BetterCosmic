package dev.nishu.bettercosmic.shared.mixin;

import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Exposes the tab-list header/footer, which {@link PlayerTabOverlay} keeps as private fields with no
 * getter. Read-only; used by the {@code /bctablist} dev command. Both may be {@code null} when the
 * server has not set a header/footer.
 */
@Mixin(PlayerTabOverlay.class)
public interface PlayerTabOverlayAccessor {

	@Accessor("header")
	Component bettercosmic$getHeader();

	@Accessor("footer")
	Component bettercosmic$getFooter();
}
