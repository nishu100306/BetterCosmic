package dev.nishu.bettercosmic.prisons.mixin;

import dev.nishu.bettercosmic.prisons.pvviewer.PvSidebar;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Draws the PV preview sidebar at the container's own render layer — the tail of
 * {@link AbstractContainerScreen}'s {@code render}, which runs after the slots/carried item but
 * <em>before</em> the deferred tooltip is flushed (that flush happens in {@code Screen.renderWithTooltip}
 * after {@code render} returns). Rendering here, rather than in a {@code ScreenEvents.afterRender} pass,
 * is what lets vanilla item tooltips draw on top of the sidebar instead of behind it. The sidebar's own
 * preview-item tooltips likewise use {@code setTooltipForNextFrame}, so they ride the same flush.
 */
@Mixin(AbstractContainerScreen.class)
public abstract class PvSidebarRenderMixin {

	@Inject(method = "render", at = @At("TAIL"))
	private void betterprisons$pvSidebar(GuiGraphics graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
		PvSidebar.render(graphics, (Screen) (Object) this, mouseX, mouseY);
	}
}
