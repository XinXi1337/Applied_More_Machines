package com.appliedmoremachines.automation;

import com.appliedmoremachines.InfusionPedestaBlockEntity;
import com.appliedmoremachines.processing.AltarInputs;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

/** All faces accept recipe materials into nine inputs and expose only the product for extraction. */
public final class AltarItemHandler implements IItemHandler {
    private final Container inventory;

    public AltarItemHandler(Container inventory) {
        this.inventory = inventory;
    }

    private int storageSlot(int slot) {
        if (slot < 0 || slot >= getSlots()) throw new IndexOutOfBoundsException(slot);
        return slot == AltarInputs.COUNT ? InfusionPedestaBlockEntity.OUTPUT_SLOT : AltarInputs.slot(slot);
    }

    @Override public int getSlots() { return AltarInputs.COUNT + 1; }

    @Override public ItemStack getStackInSlot(int slot) { return inventory.getItem(storageSlot(slot)); }

    @Override public int getSlotLimit(int slot) {
        storageSlot(slot);
        return inventory.getMaxStackSize();
    }

    @Override public boolean isItemValid(int slot, ItemStack stack) {
        int storage = storageSlot(slot);
        return slot < AltarInputs.COUNT && !stack.isEmpty() && inventory.canPlaceItem(storage, stack);
    }

    @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        int storage = storageSlot(slot);
        if (!isItemValid(slot, stack)) return stack;
        ItemStack existing = inventory.getItem(storage);
        if (!existing.isEmpty() && !ItemStack.isSameItemSameComponents(existing, stack)) return stack;
        int limit = Math.min(getSlotLimit(slot), stack.getMaxStackSize());
        int moved = Math.max(0, Math.min(stack.getCount(), limit - existing.getCount()));
        if (moved == 0) return stack;
        if (!simulate) {
            inventory.setItem(storage, stack.copyWithCount(existing.getCount() + moved));
        }
        return stack.copyWithCount(stack.getCount() - moved);
    }

    @Override public ItemStack extractItem(int slot, int amount, boolean simulate) {
        int storage = storageSlot(slot);
        if (slot != AltarInputs.COUNT || amount <= 0) return ItemStack.EMPTY;
        ItemStack existing = inventory.getItem(storage);
        if (existing.isEmpty()) return ItemStack.EMPTY;
        int moved = Math.min(amount, Math.min(existing.getCount(), existing.getMaxStackSize()));
        ItemStack extracted = existing.copyWithCount(moved);
        if (!simulate) inventory.setItem(storage, existing.copyWithCount(existing.getCount() - moved));
        return extracted;
    }
}
