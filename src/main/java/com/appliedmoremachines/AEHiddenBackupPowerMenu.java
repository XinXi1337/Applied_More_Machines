package com.appliedmoremachines;

import com.appliedmoremachines.power.HiddenPowerCell;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

/** Single hidden disk at (80,42), with the user's unmodified backpack and hotbar. */
public final class AEHiddenBackupPowerMenu extends AbstractContainerMenu {
    private final Container inventory;
    public AEHiddenBackupPowerMenu(int id, Inventory player) { this(id, player, new SimpleContainer(1)); }
    public AEHiddenBackupPowerMenu(int id, Inventory player, Container inventory) {
        super(ModMenus.AE_HIDDEN_BACKUP_POWER.get(), id);
        checkContainerSize(inventory, 1); this.inventory = inventory; inventory.startOpen(player.player);
        addSlot(new Slot(inventory, 0, 80, 42) {
            @Override public boolean mayPlace(ItemStack stack) { return HiddenPowerCell.accepts(stack); }
            @Override public int getMaxStackSize() { return 1; }
        });
        for (int row = 0; row < 3; row++) for (int column = 0; column < 9; column++)
            addSlot(new Slot(player, column + row * 9 + 9, 8 + column * 18, 100 + row * 18));
        for (int column = 0; column < 9; column++) addSlot(new Slot(player, column, 8 + column * 18, 158));
    }
    @Override public boolean stillValid(Player player) { return inventory.stillValid(player); }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem() || !slot.mayPickup(player)) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem(), original = stack.copy();
        boolean moved;
        if (index == 0) moved = moveItemStackTo(stack, 1, 37, true);
        else {
            moved = HiddenPowerCell.accepts(stack) && moveItemStackTo(stack, 0, 1, false);
            if (!moved) moved = index < 28 ? moveItemStackTo(stack, 28, 37, false) : moveItemStackTo(stack, 1, 28, false);
        }
        if (!moved || stack.getCount() == original.getCount()) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        slot.onTake(player, stack); return original;
    }
    @Override public void removed(Player player) { super.removed(player); inventory.stopOpen(player); }
}
