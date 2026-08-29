package dev.nishu.bettercosmic.prisons.mixin;

import dev.nishu.bettercosmic.prisons.PrisonsGate;
import dev.nishu.bettercosmic.prisons.client.BetterPrisonsClient;
import net.minecraft.client.gui.Gui;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Bolds server-sent title / subtitle popups that mention "XP" or "Energy" (e.g. "+1.0 XP",
 * "+18.0 Energy") when the QoL setting is on. Ported from BetterPrisons' {@code TitleBoldMixin}
 * (Yarn → Mojang: {@code InGameHud} → {@code Gui}, {@code MutableText.of(getContent())} →
 * {@code MutableComponent.create(getContents())}).
 */
@Mixin(Gui.class)
public class TitleBoldMixin {

	@ModifyVariable(method = "setTitle", at = @At("HEAD"), argsOnly = true)
	private Component bettercosmic$boldTitle(Component title) {
		return maybeBold(title);
	}

	@ModifyVariable(method = "setSubtitle", at = @At("HEAD"), argsOnly = true)
	private Component bettercosmic$boldSubtitle(Component subtitle) {
		return maybeBold(subtitle);
	}

	private static Component maybeBold(Component text) {
		if (text == null) {
			return null;
		}
		if (BetterPrisonsClient.config == null || !BetterPrisonsClient.config.boldXpEnergyTitles
				|| !PrisonsGate.active()) {
			return text;
		}
		String s = text.getString();
		if (s.contains("XP") || s.contains("Energy")) {
			return deepBold(text);
		}
		return text;
	}

	/** Rebuilds the text with bold forced on every component (not just the root). */
	private static Component deepBold(Component text) {
		MutableComponent result = MutableComponent.create(text.getContents())
				.setStyle(text.getStyle().withBold(true));
		for (Component sibling : text.getSiblings()) {
			result.append(deepBold(sibling));
		}
		return result;
	}
}
