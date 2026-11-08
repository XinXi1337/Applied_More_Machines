package com.appliedmoremachines.inscriber;

import appeng.util.inv.AppEngInternalInventory;
import appeng.util.inv.InternalInventoryHost;
import appeng.util.inv.filter.IAEItemFilter;
import java.util.function.IntSupplier;
import net.minecraft.world.item.ItemStack;

/** Native input slot plus a bulk output slot; extraction retains native item stack limits. */
public final class InscriberSideInventory extends AppEngInternalInventory {
    private final IntSupplier outputCapacity;

    public InscriberSideInventory(InternalInventoryHost host, int size, int limit, IAEItemFilter filter,
            IntSupplier outputCapacity) {
        super(host, size, limit, filter);
        this.outputCapacity = outputCapacity;
    }

    @Override public int getSlotLimit(int slot) {
        return slot == 1 ? outputCapacity.getAsInt() : super.getSlotLimit(slot);
    }

    @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (slot != 1) return super.insertItem(slot, stack, simulate);
        if (stack.isEmpty() || !isItemValid(slot, stack)) return stack;
        var current = getStackInSlot(slot);
        if (!current.isEmpty() && !ItemStack.isSameItemSameComponents(current, stack)) return stack;
        int accepted = Math.min(stack.getCount(), Math.max(0, getSlotLimit(slot) - current.getCount()));
        if (accepted == 0) return stack;
        if (!simulate) setItemDirect(slot, stack.copyWithCount(current.getCount() + accepted));
        return accepted == stack.getCount() ? ItemStack.EMPTY : stack.copyWithCount(stack.getCount() - accepted);
    }
}
