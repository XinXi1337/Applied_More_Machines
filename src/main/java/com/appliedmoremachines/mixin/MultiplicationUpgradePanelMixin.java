package com.appliedmoremachines.mixin;

import appeng.client.gui.implementations.UpgradeableScreen;
import appeng.menu.SlotSemantic;
import appeng.menu.implementations.UpgradeableMenu;
import com.appliedmoremachines.production.MultiplicationSlot;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Include the dedicated slot in the native visual panel without changing menu semantics. */
@Mixin(value = UpgradeableScreen.class, remap = false)
public abstract class MultiplicationUpgradePanelMixin {
    @WrapOperation(method = "<init>", at = @At(value = "INVOKE", target =
            "Lappeng/menu/implementations/UpgradeableMenu;getSlots(Lappeng/menu/SlotSemantic;)Ljava/util/List;"), require = 1, allow = 1)
    private List<Slot> amm$includeDedicatedSlot(UpgradeableMenu<?> menu, SlotSemantic semantic,
            Operation<List<Slot>> original) {
        List<Slot> slots = original.call(menu, semantic);
        for (Slot slot : menu.slots) {
            if (slot instanceof MultiplicationSlot) {
                var extended = new ArrayList<>(slots);
                extended.add(slot);
                return extended;
            }
        }
        return slots;
    }
}