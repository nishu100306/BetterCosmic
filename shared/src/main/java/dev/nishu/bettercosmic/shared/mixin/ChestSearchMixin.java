package dev.nishu.bettercosmic.shared.mixin;

import dev.nishu.bettercosmic.shared.chestsearch.ChestSearchRegistry;
import dev.nishu.bettercosmic.shared.chestsearch.ChestSearchState;
import dev.nishu.bettercosmic.shared.chestsearch.FilterRule;
import dev.nishu.bettercosmic.shared.chestsearch.FilterState;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Adds the chest-search bar and no-code filter-rule sidebar to container screens for whichever
 * BetterCosmic mod is active (see {@link ChestSearchRegistry}). The per-slot match highlight is drawn
 * by the shared {@code ChestSearchTintProvider} through EasyView, so this mixin only injects the
 * widgets and their focus/typing handling. Ported from BetterPrisons' {@code ContainerSearchMixin}
 * and de-hardcoded to run per active network.
 */
@Mixin(AbstractContainerScreen.class)
public abstract class ChestSearchMixin extends Screen {

	@Shadow protected int leftPos;
	@Shadow protected int topPos;
	@Shadow protected int imageWidth;
	@Shadow protected int imageHeight;

	@Unique private EditBox bettercosmicshared$searchField;

	protected ChestSearchMixin(Component title) {
		super(title);
	}

	@Inject(method = "init", at = @At("TAIL"))
	private void bettercosmicshared$init(CallbackInfo ci) {
		if (!ChestSearchRegistry.enabled()) {
			return;
		}
		bettercosmicshared$buildSearchBar();
		bettercosmicshared$buildSidebar();
	}

	@Unique
	private void bettercosmicshared$buildSearchBar() {
		int fieldW = 120, fieldH = 16, btnW = 36, gap = 4;
		int totalW = fieldW + gap + btnW;
		int barX = this.leftPos + (this.imageWidth - totalW) / 2;
		int barY = this.topPos + this.imageHeight + 4;

		bettercosmicshared$searchField = new EditBox(this.font, barX, barY, fieldW, fieldH, Component.literal("Search"));
		bettercosmicshared$searchField.setMaxLength(1024);
		bettercosmicshared$searchField.setValue(ChestSearchState.query == null ? "" : ChestSearchState.query);
		bettercosmicshared$searchField.setResponder(s -> ChestSearchState.query = s);
		this.addRenderableWidget(bettercosmicshared$searchField);

		Button filterToggle = Button.builder(
				Component.literal(FilterState.sidebarOpen ? "Filt." : "Filt+"),
				btn -> {
					FilterState.sidebarOpen = !FilterState.sidebarOpen;
					if (FilterState.sidebarOpen && FilterState.rules.isEmpty()) {
						FilterState.addRule();
					}
					this.rebuildWidgets();
				}
		).bounds(barX + fieldW + gap, barY, btnW, fieldH).build();
		this.addRenderableWidget(filterToggle);
	}

	@Unique
	private void bettercosmicshared$buildSidebar() {
		if (!FilterState.sidebarOpen) {
			return;
		}
		int sidebarW = 140;
		int sx = this.leftPos + this.imageWidth + 8;
		if (sx + sidebarW > this.width) {
			sx = this.leftPos - sidebarW - 8;
		}
		int sy = Math.max(8, this.topPos - 75);
		int rowY = sy + 14;

		Button modeBtn = Button.builder(
				Component.literal(FilterState.matchAll ? "Match: All" : "Match: Any"),
				btn -> {
					FilterState.matchAll = !FilterState.matchAll;
					btn.setMessage(Component.literal(FilterState.matchAll ? "Match: All" : "Match: Any"));
				}
		).bounds(sx, rowY, 140, 18).build();
		this.addRenderableWidget(modeBtn);
		rowY += 22;

		for (int i = 0; i < FilterState.rules.size(); i++) {
			final int idx = i;
			FilterRule rule = FilterState.rules.get(i);

			EditBox valField = new EditBox(this.font, sx, rowY, 140, 16, Component.literal("value"));
			valField.setMaxLength(64);
			valField.setValue(rule.value);
			valField.setResponder(s -> rule.value = s);
			this.addRenderableWidget(valField);
			rowY += 18;

			Button typeBtn = Button.builder(
					Component.literal(rule.type.label()),
					btn -> {
						rule.type = ChestSearchRegistry.nextType(rule.type);
						btn.setMessage(Component.literal(rule.type.label()));
					}
			).bounds(sx, rowY, 60, 18).build();
			this.addRenderableWidget(typeBtn);

			Button colorBtn = Button.builder(
					Component.literal(FilterState.colorName(rule.color)),
					btn -> {
						rule.color = FilterState.nextColor(rule.color);
						btn.setMessage(Component.literal(FilterState.colorName(rule.color)));
					}
			).bounds(sx + 62, rowY, 60, 18).build();
			this.addRenderableWidget(colorBtn);

			Button delBtn = Button.builder(
					Component.literal("X"),
					btn -> {
						FilterState.removeRule(idx);
						this.rebuildWidgets();
					}
			).bounds(sx + 124, rowY, 16, 18).build();
			this.addRenderableWidget(delBtn);
			rowY += 22;
		}

		if (FilterState.rules.size() < FilterState.MAX_RULES) {
			Button addBtn = Button.builder(
					Component.literal("+ Add Rule"),
					btn -> {
						FilterState.addRule();
						this.rebuildWidgets();
					}
			).bounds(sx, rowY, 140, 18).build();
			this.addRenderableWidget(addBtn);
		}
	}

	@Inject(method = "render", at = @At("TAIL"))
	private void bettercosmicshared$renderSidebarBackdrop(GuiGraphics context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
		if (!ChestSearchRegistry.enabled() || !FilterState.sidebarOpen) {
			return;
		}
		int sidebarW = 140;
		int sx = this.leftPos + this.imageWidth + 8;
		if (sx + sidebarW > this.width) {
			sx = this.leftPos - sidebarW - 8;
		}
		int sy = Math.max(8, this.topPos - 75);
		context.fill(sx - 4, sy - 4, sx + sidebarW + 4,
				sy + 14 + 22 + FilterState.rules.size() * 40 + 22, 0x80000000);
		context.drawString(this.font, Component.literal("Filter Rules"), sx, sy, 0xFFFFFFFF, true);
	}

	@Inject(method = "mouseClicked", at = @At("HEAD"))
	private void bettercosmicshared$clearFocusOnOutsideClick(MouseButtonEvent event, boolean doubled, CallbackInfoReturnable<Boolean> cir) {
		if (!ChestSearchRegistry.enabled()) {
			return;
		}
		if (this.getFocused() instanceof EditBox field && !field.isMouseOver(event.x(), event.y())) {
			field.setFocused(false);
			this.setFocused((GuiEventListener) null);
		}
	}

	@Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
	private void bettercosmicshared$keyPressed(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
		if (!ChestSearchRegistry.enabled()) {
			return;
		}
		if (event.key() == 256) { // GLFW_KEY_ESCAPE — let escape close the screen normally
			return;
		}
		// If a text field is focused, dispatch via Screen and short-circuit so the container's
		// "close on inventory key" branch doesn't fire mid-typing.
		if (this.getFocused() instanceof EditBox) {
			super.keyPressed(event);
			cir.setReturnValue(true);
		}
	}
}
