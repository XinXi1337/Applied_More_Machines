package com.appliedmoremachines.mixin;

import com.appliedmoremachines.production.*;

import appeng.blockentity.misc.InscriberBlockEntity;
import appeng.util.inv.AppEngInternalInventory;
import com.appliedmoremachines.ParallelInscriberBlockEntity;
import com.appliedmoremachines.inscriber.InscriberBatch;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = InscriberBlockEntity.class, remap = false)
public abstract class ParallelInscriberMixin {
    @WrapOperation(method = "<init>", at = @At(value = "NEW", target =
            "(Lappeng/util/inv/InternalInventoryHost;IILappeng/util/inv/filter/IAEItemFilter;)Lappeng/util/inv/AppEngInternalInventory;"), require = 3, allow = 3)
    private AppEngInternalInventory amm$bulkOutput(appeng.util.inv.InternalInventoryHost host, int size,
            int limit, appeng.util.inv.filter.IAEItemFilter filter, Operation<AppEngInternalInventory> original) {
        if (host instanceof ParallelInscriberBlockEntity machine && size == 2) {
            return new com.appliedmoremachines.inscriber.InscriberSideInventory(host, size, limit, filter,
                    machine::outputCapacity);
        }
        return original.call(host, size, limit, filter);
    }

    // Ordinal zero is the real insertion at smash step 8; ordinal one is only a preflight.
    @WrapOperation(method = "tickingRequest", at = @At(value = "INVOKE", target =
            "Lappeng/util/inv/AppEngInternalInventory;insertItem(ILnet/minecraft/world/item/ItemStack;Z)Lnet/minecraft/world/item/ItemStack;",
            ordinal = 0), require = 1, allow = 1)
    private ItemStack amm$completeBatch(AppEngInternalInventory output, int slot, ItemStack result,
            boolean simulate, Operation<ItemStack> original) {
        if ((Object) this instanceof ParallelInscriberBlockEntity machine && !simulate) {
            if (MultiplicationUpgrade.active(machine)) return InscriberBatch.insertMultiplied(machine, machine.getTask(), result, false);
            return InscriberBatch.insert(machine.getInternalInventory(), machine.getTask(), result,
                    machine.parallelLimit(), stack -> original.call(output, slot, stack, false));
        }
        return original.call(output, slot, result, simulate);
    }
    @WrapOperation(method = {"hasCraftWork", "tickingRequest"}, at = @At(value = "INVOKE", target =
            "Lappeng/util/inv/AppEngInternalInventory;insertItem(ILnet/minecraft/world/item/ItemStack;Z)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack amm$dedicatedPreflight(AppEngInternalInventory output, int slot, ItemStack result,
            boolean simulate, Operation<ItemStack> original) {
        if (simulate && (Object) this instanceof ParallelInscriberBlockEntity machine && MultiplicationUpgrade.active(machine))
            return InscriberBatch.insertMultiplied(machine, machine.getTask(), result, true);
        return original.call(output, slot, result, simulate);
    }
}
