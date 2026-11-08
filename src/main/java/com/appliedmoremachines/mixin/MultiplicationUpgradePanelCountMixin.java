package com.appliedmoremachines.mixin;

import appeng.client.gui.widgets.UpgradesPanel;
import com.appliedmoremachines.production.MultiplicationSlot;
import java.util.List;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** AE counts AppEngSlots only; include our plain dedicated slot in the frame and bounds. */
@Mixin(value = UpgradesPanel.class, remap = false)
public abstract class MultiplicationUpgradePanelCountMixin {
    @Shadow @Final private List<Slot> slots;

    @Inject(method = "getUpgradeSlotCount", at = @At("RETURN"), cancellable = true)
    private void amm$countDedicatedSlot(CallbackInfoReturnable<Integer> cir) {
        int count = cir.getReturnValue();
        for (Slot slot : slots) {
            if (slot instanceof MultiplicationSlot && slot.isActive()) count++;
        }
        cir.setReturnValue(count);
    }
}