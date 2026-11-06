package com.appliedmoremachines.fusion;

import com.appliedmoremachines.CraftingCoreBlockEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

/** Center first, then16 material slots. Equipment/upgrades never exposed to pipes. */
public final class CoreItemHandler implements IItemHandler {
    private final CraftingCoreBlockEntity core;
    public CoreItemHandler(CraftingCoreBlockEntity core) { this.core = core; }
    private int storage(int slot) {
        if (slot < 0 || slot >= 17) throw new IndexOutOfBoundsException(slot);
        return slot == 0 ? CraftingCoreBlockEntity.CENTER_SLOT : slot - 1;
    }
    @Override public int getSlots() { return 17; }
    @Override public ItemStack getStackInSlot(int slot) { return core.getItem(storage(slot)); }
    @Override public int getSlotLimit(int slot) { storage(slot); return 64; }
    @Override public boolean isItemValid(int slot, ItemStack stack) {
        int target = storage(slot);
        if (stack.isEmpty()) return false;
        if (slot == 0) return !core.isResult() && CoreRecipes.catalystLimit(core.getLevel(), stack) > 0;
        if (core.getItem(CraftingCoreBlockEntity.CENTER_SLOT).isEmpty()
                && CoreRecipes.catalystLimit(core.getLevel(), stack) > 0) return false;
        return target < core.unlockedSlots() && (CoreRecipes.material(core.getLevel(), stack)
                || !core.isResult() && CoreRecipes.catalystLimit(core.getLevel(), stack) > 0);
    }
    @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        int target = storage(slot);
        if (!isItemValid(slot, stack) || !core.canPlaceItem(target, stack)) return stack;
        ItemStack existing = core.getItem(target);
        if (!existing.isEmpty() && !ItemStack.isSameItemSameComponents(existing, stack)) return stack;
        int count = Math.max(0, Math.min(stack.getCount(), core.slotLimit(target, stack) - existing.getCount()));
        if (count > 0 && !simulate) {
            core.setItem(target, stack.copyWithCount(existing.getCount() + count));
            core.arrangeAutomaticInputs();
        }
        return stack.copyWithCount(stack.getCount() - count);
    }
    @Override public ItemStack extractItem(int slot, int amount, boolean simulate) {
        int target = storage(slot);
        if (slot != 0 || !core.isResult() || amount <= 0) return ItemStack.EMPTY;
        ItemStack output = core.getItem(target);
        int count = Math.min(amount, Math.min(output.getCount(), output.getMaxStackSize()));
        if (count <= 0) return ItemStack.EMPTY;
        ItemStack taken = output.copyWithCount(count);
        if (!simulate) core.setItem(target, output.copyWithCount(output.getCount() - count));
        return taken;
    }
}
