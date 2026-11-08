package com.appliedmoremachines.inscriber;

import com.appliedmoremachines.production.*;

import appeng.api.inventories.InternalInventory;
import appeng.recipes.handlers.InscriberProcessType;
import appeng.recipes.handlers.InscriberRecipe;
import java.util.function.Function;
import net.minecraft.world.item.ItemStack;

/** Expands AE2's completed single recipe; native code still consumes the first unit. */
public final class InscriberBatch {
    private InscriberBatch() {}

    public static int size(InternalInventory inv, InscriberRecipe recipe, ItemStack result, int limit) {
        if (recipe == null || result.isEmpty()) return 0;
        int batch = Math.min(limit, inv.getStackInSlot(2).getCount());
        if (recipe.getProcessType() == InscriberProcessType.PRESS) {
            // Empty optional ingredients are legal, including flipped recipes.
            for (int slot = 0; slot < 2; slot++) {
                var ingredient = inv.getStackInSlot(slot);
                if (!ingredient.isEmpty()) batch = Math.min(batch, ingredient.getCount());
            }
        }
        int unit = result.getCount();
        batch = Math.min(batch, inv.getSlotLimit(3) / unit);
        if (batch <= 0) return 0;
        int amount = batch * unit;
        int remainder = inv.insertItem(3, result.copyWithCount(amount), true).getCount();
        return (amount - remainder) / unit;
    }

    public static ItemStack insert(InternalInventory inv, InscriberRecipe recipe, ItemStack result,
            int limit, Function<ItemStack, ItemStack> originalInsertion) {
        int batch = size(inv, recipe, result, limit);
        if (batch == 0) return result; // Native code must not consume even one unit.
        var remainder = originalInsertion.apply(result.copyWithCount(result.getCount() * batch));
        if (remainder.isEmpty() && batch > 1) {
            // Preserve the native completion, animation, dirty flag and auto-export path.
            int extra = batch - 1;
            if (recipe.getProcessType() == InscriberProcessType.PRESS) {
                inv.extractItem(0, extra, false);
                inv.extractItem(1, extra, false);
            }
            inv.extractItem(2, extra, false);
        }
        return remainder;
    }
    /** Dedicated production bypasses DE's native 99-count insertion while retaining AE's first-unit completion. */
    public static ItemStack insertMultiplied(com.appliedmoremachines.ParallelInscriberBlockEntity machine,
            InscriberRecipe recipe, ItemStack unit, boolean simulate) {
        var inv = machine.getInternalInventory();
        if (recipe == null || unit.isEmpty() || machine.multiplicationUpgrade().hasPending()) return unit;
        int copies = Math.min(machine.parallelLimit(), inv.getStackInSlot(2).getCount());
        if (recipe.getProcessType() == InscriberProcessType.PRESS) for (int slot = 0; slot < 2; slot++) {
            var input = inv.getStackInSlot(slot);
            if (!input.isEmpty()) copies = Math.min(copies, input.getCount());
        }
        if (copies < 1) return unit;
        if (simulate) return ItemStack.EMPTY;
        long produced = MultiplicationUpgrade.amount(machine, unit.getCount(), copies);
        ItemStack present = inv.getStackInSlot(3);
        int local = present.isEmpty() || ItemStack.isSameItemSameComponents(present, unit)
                ? (int) Math.min(produced, Math.max(0, machine.outputCapacity() - present.getCount())) : 0;
        if (local > 0) inv.setItemDirect(3, unit.copyWithCount(present.getCount() + local));
        if (produced > local) machine.multiplicationUpgrade().retain(appeng.api.stacks.AEItemKey.of(unit), produced - local);
        int extra = copies - 1;
        if (extra > 0) {
            if (recipe.getProcessType() == InscriberProcessType.PRESS) {
                inv.extractItem(0, extra, false); inv.extractItem(1, extra, false);
            }
            inv.extractItem(2, extra, false);
        }
        return ItemStack.EMPTY;
    }
}
