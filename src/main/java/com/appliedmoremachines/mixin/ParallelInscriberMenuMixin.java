package com.appliedmoremachines.mixin;

import com.appliedmoremachines.production.*;

import appeng.api.inventories.InternalInventory;
import appeng.client.gui.Icon;
import appeng.menu.implementations.InscriberMenu;
import appeng.menu.slot.OutputSlot;
import com.appliedmoremachines.inscriber.InscriberOutputSlot;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = InscriberMenu.class, remap = false)
public abstract class ParallelInscriberMenuMixin {
    @WrapOperation(method = "<init>", at = @At(value = "NEW", target =
            "(Lappeng/api/inventories/InternalInventory;ILappeng/client/gui/Icon;)Lappeng/menu/slot/OutputSlot;"), require = 1, allow = 1)
    private OutputSlot amm$bulkOutputSlot(InternalInventory inventory, int slot, Icon icon, Operation<OutputSlot> original) {
        return ((InscriberMenu) (Object) this).getHost() instanceof com.appliedmoremachines.ParallelInscriberBlockEntity
                ? new InscriberOutputSlot(inventory, slot, icon)
                : original.call(inventory, slot, icon);
    }
    @org.spongepowered.asm.mixin.injection.Inject(method = "<init>", at = @At("RETURN"))
    private void amm$dedicatedSlot(int id, net.minecraft.world.entity.player.Inventory inventory,
            appeng.blockentity.misc.InscriberBlockEntity host, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        if (host instanceof com.appliedmoremachines.ParallelInscriberBlockEntity && MultiplicationUpgrade.enabled())
            ((MultiplicationMenuAccess) this).amm$addSlot(new MultiplicationSlot(host, 180, 77, () -> true));
    }
}
