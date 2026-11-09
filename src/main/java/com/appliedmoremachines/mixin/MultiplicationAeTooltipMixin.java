package com.appliedmoremachines.mixin;

import appeng.client.gui.AEBaseScreen;
import appeng.menu.AEBaseMenu;
import com.appliedmoremachines.client.MultiplicationScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Give the dedicated slot sole ownership before AE dispatches panel tooltips. */
@Mixin(value = AEBaseScreen.class, remap = false)
public abstract class MultiplicationAeTooltipMixin extends AbstractContainerScreen<AEBaseMenu> {
    protected MultiplicationAeTooltipMixin(AEBaseMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Inject(method = "renderTooltips", at = @At("HEAD"), cancellable = true)
    private void amm$dedicatedTooltip(GuiGraphics graphics, int x, int y, CallbackInfo ci) {
        if (MultiplicationScreen.tooltip(graphics, font, hoveredSlot, menu.getCarried(), x, y,
                this::getTooltipFromContainerItem)) {
            ci.cancel();
        }
    }
}