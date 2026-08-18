package dev.nishu.bettercosmic.prisons.mixin;

import dev.nishu.bettercosmic.prisons.chestsearch.ChestSearchFilterRule;
import dev.nishu.bettercosmic.prisons.chestsearch.ChestSearchFilterState;
import dev.nishu.bettercosmic.prisons.chestsearch.ChestSearchState;
import dev.nishu.bettercosmic.prisons.client.BetterPrisonsClient;
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
 * Adds the chest-search bar and no-code filter-rule sidebar to container screens. The per-slot match
 * highlight and clue-scroll number are drawn by the shared EasyView providers (see
 * {@code ChestSearchTintProvider} / {@code ClueScrollProvider}), so this mixin only injects the
 * widgets and their focus/typing handling — much slimmer than BetterPrisons' original. Ported from
 * BetterPrisons' {@code ContainerSearchMixin} (Yarn → Mojang).
 */
@Mixin(AbstractContainerScreen.class)
public abstract class ContainerSearchMixin extends Screen {

	@Shadow protected int leftPos;
	@Shadow protected int topPos;
	@Shadow protected int imageWidth;
	@Shadow protected int imageHeight;

	@Unique private EditBox betterprisons$searchField;

	protected ContainerSearchMixin(Component title) {
		super(title);
	}

	@Inject(method = "init", at = @At("TAIL"))
	private void betterprisons$init(CallbackInfo ci) {
		if (!BetterPrisonsClient.config.chestSearchEnabled) {
			return;
		}
		betterprisons$buildSearchBar();
		betterprisons$buildSidebar();
	}

	@Unique
	private void betterprisons$buildSearchBar() {
		int fieldW = 120, fieldH = 16, btnW = 36, gap = 4;
		int totalW = fieldW + gap + btnW;
		int barX = this.leftPos + (this.imageWidth - totalW) / 2;
		int barY = this.topPos + this.imageHeight + 4;

		betterprisons$searchField = new EditBox(this.font, barX, barY, fieldW, fieldH, Component.literal("Search"));
		betterprisons$searchField.setMaxLength(1024);
		betterprisons$searchField.setValue(ChestSearchState.query == null ? "" : ChestSearchState.query);
		betterprisons$searchField.setResponder(s -> ChestSearchState.query = s);
		this.addRenderableWidget(betterprisons$searchField);

		Button filterToggle = Button.builder(
				Component.literal(ChestSearchFilterState.sidebarOpen ? "Filt." : "Filt+"),
				btn -> {
					ChestSearchFilterState.sidebarOpen = !ChestSearchFilterState.sidebarOpen;
					if (ChestSearchFilterState.sidebarOpen && ChestSearchFilterState.rules.isEmpty()) {
						ChestSearchFilterState.addRule();
					}
					this.rebuildWidgets();
				}
		).bounds(barX + fieldW + gap, barY, btnW, fieldH).build();
		this.addRenderableWidget(filterToggle);
	}

	@Unique
	private void betterprisons$buildSidebar() {
		if (!ChestSearchFilterState.sidebarOpen) {
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
				Component.literal(ChestSearchFilterState.matchAll ? "Match: All" : "Match: Any"),
				btn -> {
					ChestSearchFilterState.matchAll = !ChestSearchFilterState.matchAll;
					btn.setMessage(Component.literal(ChestSearchFilterState.matchAll ? "Match: All" : "Match: Any"));
				}
		).bounds(sx, rowY, 140, 18).build();
		this.addRenderableWidget(modeBtn);
		rowY += 22;

		for (int i = 0; i < ChestSearchFilterState.rules.size(); i++) {
			final int idx = i;
			ChestSearchFilterRule rule = ChestSearchFilterState.rules.get(i);

			EditBox valField = new EditBox(this.font, sx, rowY, 140, 16, Component.literal("name"));
			valField.setMaxLength(64);
			valField.setValue(rule.value);
			valField.setResponder(s -> rule.value = s);
			this.addRenderableWidget(valField);
			rowY += 18;

			Button typeBtn = Button.builder(
					Component.literal(rule.type.label),
					btn -> {
						rule.type = rule.type.next();
						btn.setMessage(Component.literal(rule.type.label));
					}
			).bounds(sx, rowY, 60, 18).build();
			this.addRenderableWidget(typeBtn);

			Button colorBtn = Button.builder(
					Component.literal(ChestSearchFilterState.colorName(rule.color)),
					btn -> {
						rule.color = ChestSearchFilterState.nextColor(rule.color);
						btn.setMessage(Component.literal(ChestSearchFilterState.colorName(rule.color)));
					}
			).bounds(sx + 62, rowY, 60, 18).build();
			this.addRenderableWidget(colorBtn);

			Button delBtn = Button.builder(
					Component.literal("X"),
					btn -> {
						ChestSearchFilterState.removeRule(idx);
						this.rebuildWidgets();
					}
			).bounds(sx + 124, rowY, 16, 18).build();
			this.addRenderableWidget(delBtn);
			rowY += 22;
		}

		if (ChestSearchFilterState.rules.size() < ChestSearchFilterState.MAX_RULES) {
			Button addBtn = Button.builder(
					Component.literal("+ Add Rule"),
					btn -> {
						ChestSearchFilterState.addRule();
						this.rebuildWidgets();
					}
			).bounds(sx, rowY, 140, 18).build();
			this.addRenderableWidget(addBtn);
		}
	}

	@Inject(method = "render", at = @At("TAIL"))
	private void betterprisons$renderSidebarBackdrop(GuiGraphics context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
		if (!BetterPrisonsClient.config.chestSearchEnabled || !ChestSearchFilterState.sidebarOpen) {
			return;
		}
		int sidebarW = 140;
		int sx = this.leftPos + this.imageWidth + 8;
		if (sx + sidebarW > this.width) {
			sx = this.leftPos - sidebarW - 8;
		}
		int sy = Math.max(8, this.topPos - 75);
		context.fill(sx - 4, sy - 4, sx + sidebarW + 4,
				sy + 14 + 22 + ChestSearchFilterState.rules.size() * 40 + 22, 0x80000000);
		context.drawString(this.font, Component.literal("Filter Rules"), sx, sy, 0xFFFFFFFF, true);
	}

	@Inject(method = "mouseClicked", at = @At("HEAD"))
	private void betterprisons$clearFocusOnOutsideClick(MouseButtonEvent event, boolean doubled, CallbackInfoReturnable<Boolean> cir) {
		if (!BetterPrisonsClient.config.chestSearchEnabled) {
			return;
		}
		if (this.getFocused() instanceof EditBox field && !field.isMouseOver(event.x(), event.y())) {
			field.setFocused(false);
			this.setFocused((GuiEventListener) null);
		}
	}

	@Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
	private void betterprisons$keyPressed(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
		if (!BetterPrisonsClient.config.chestSearchEnabled) {
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
