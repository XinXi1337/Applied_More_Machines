package com.appliedmoremachines.mixin;

import appeng.api.inventories.InternalInventory;
import com.appliedmoremachines.inscriber.InscriberSideInventory;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** DE restores rejected auto-export stacks even without a card; its 99 cap cannot hold our bulk output. */
@Pseudo
@Mixin(targets = "com.fish_dan_.data_energistics.common.upgrade.production.MultiplicationCardOutput", remap = false)
public abstract class ParallelInscriberDataOutputMixin {
    @Inject(method = "restoreExternalOutput", at = @At("HEAD"), cancellable = true, require = 0)
    private static void amm$restoreBulkOutput(InternalInventory inventory, int slot, ItemStack stack,
            boolean simulate, CallbackInfoReturnable<ItemStack> cir) {
        if (inventory instanceof InscriberSideInventory || com.appliedmoremachines.nativeeight.EightInventories.expanded(inventory)) cir.setReturnValue(inventory.insertItem(slot, stack, simulate));
    }
}
