package dev.nishu.bettercosmic.prisons.pvviewer;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;

/**
 * Attaches the {@link PvSidebar} preview overlay to every screen: an after-render pass draws it, and
 * mouse passes route input to it. All are no-ops unless the screen is a {@code /pv} GUI and the feature
 * is enabled (checked inside {@link PvSidebar}).
 *
 * <p>Both mouse press and release use the <em>allow</em> (pre-vanilla) phase and are cancelled while
 * the cursor is over a sidebar panel. The panels sit outside the container window, where a vanilla
 * press <em>or release</em> while carrying an item would "drop outside" — so vanilla must not see
 * either; the sidebar forwards the click to the right slot itself (on press).
 */
public final class PvScreenHooks {

	private PvScreenHooks() {}

	public static void register() {
		// Rendering is done by PvSidebarRenderMixin (at the container's own render layer, so vanilla
		// tooltips draw on top of the sidebar); here we only wire the mouse input.
		ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
			ScreenMouseEvents.allowMouseClick(screen).register((scr, event) -> {
				PvSidebar.handleClick(scr, event.x(), event.y(), event.button());
				return !PvSidebar.overSidebar(scr, event.x(), event.y()); // cancel vanilla over the panels
			});
			ScreenMouseEvents.allowMouseRelease(screen).register((scr, event) ->
					!PvSidebar.overSidebar(scr, event.x(), event.y()));
			ScreenMouseEvents.afterMouseScroll(screen).register((scr, mouseX, mouseY, hAmount, vAmount, consumed) -> {
				PvSidebar.handleScroll(scr, mouseX, mouseY, vAmount);
				return false;
			});
		});
		// Completes a deposit-into-an-unopened-vault once that vault's GUI has opened.
		ClientTickEvents.END_CLIENT_TICK.register(client -> PvSidebar.tickPendingDeposit());
	}
}
