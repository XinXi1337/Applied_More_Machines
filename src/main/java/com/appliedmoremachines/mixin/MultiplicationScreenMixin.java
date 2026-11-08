package com.appliedmoremachines.mixin;

import com.appliedmoremachines.client.MultiplicationScreen;
import com.appliedmoremachines.production.MultiplicationSlot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

/** Paint with existing AE sprites before rendering the actual slot item. */
@Mixin(AbstractContainerScreen.class)
public abstract class MultiplicationScreenMixin {
    @Shadow protected int leftPos;
    @Shadow protected int topPos;
    @Shadow @Final protected AbstractContainerMenu menu;
    @Inject(method = "renderSlot", at = @At("HEAD"))
    private void amm$dedicatedBackground(GuiGraphics graphics, Slot slot, CallbackInfo ci) {
        if (slot instanceof MultiplicationSlot && slot.isActive()) MultiplicationScreen.background(graphics, slot);
    }
    @Inject(method = "hasClickedOutside", at = @At("RETURN"), cancellable = true)
    private void amm$dedicatedInside(double x, double y, int left, int top, int button, CallbackInfoReturnable<Boolean> cir) {
        for (Slot slot : menu.slots) if (slot instanceof MultiplicationSlot && slot.isActive()
                && x >= leftPos + slot.x - 4 && x < leftPos + slot.x + 20
                && y >= topPos + slot.y - 1 && y < topPos + slot.y + 21) cir.setReturnValue(false);
    }
    @Shadow protected Slot hoveredSlot;
    @Shadow protected abstract java.util.List<net.minecraft.network.chat.Component> getTooltipFromContainerItem(net.minecraft.world.item.ItemStack stack);
    @Inject(method = "renderTooltip", at = @At("HEAD"), cancellable = true)
    private void amm$dedicatedTooltip(GuiGraphics graphics, int x, int y, CallbackInfo ci) {
        if (!(hoveredSlot instanceof MultiplicationSlot)) return;
        var font = net.minecraft.client.Minecraft.getInstance().font;
        if (MultiplicationScreen.tooltip(graphics, font, hoveredSlot, menu.getCarried(), x, y, this::getTooltipFromContainerItem)) ci.cancel();
    }
}
