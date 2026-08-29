package dev.nishu.bettercosmic.prisons.mixin;

import dev.nishu.bettercosmic.prisons.PrisonsGate;
import dev.nishu.bettercosmic.prisons.misc.EnchantBookTooltip;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Cosmic Prisons sends enchant-book chat hovers as {@code SHOW_TEXT} (pre-rendered text, no item), so
 * the normal item-tooltip callback can't touch them. This redirects the {@code ShowText.value()} lookup
 * inside {@code renderComponentHoverEffect} and appends the upgrade-cost lines when the hovered text
 * looks like an enchant book. Ported from BetterPrisons' {@code ChatBookTooltipMixin} (Yarn → Mojang:
 * {@code DrawContext.drawHoverEvent} → {@code GuiGraphics.renderComponentHoverEffect}).
 */
@Mixin(GuiGraphics.class)
public class ChatBookTooltipMixin {

	@Redirect(method = "renderComponentHoverEffect", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/network/chat/HoverEvent$ShowText;value()Lnet/minecraft/network/chat/Component;"))
	private Component bettercosmic$appendBookCost(HoverEvent.ShowText showText) {
		if (!PrisonsGate.active()) {
			return showText.value(); // off-server: leave the hover text untouched
		}
		return EnchantBookTooltip.appendChatHoverCost(showText.value());
	}
}
