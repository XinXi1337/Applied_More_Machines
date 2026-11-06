package com.appliedmoremachines;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Single-page 9x4 encoded-pattern inventory and the player's normal inventory. */
public final class FactoryPatternMenu extends AbstractContainerMenu {
    public static final int IMAGE_WIDTH = 176;
    public static final int IMAGE_HEIGHT = 186;
    public static final int SLOT_SPACING = 18;
    public static final int PATTERN_SLOT_COUNT = FactoryPatternBlockEntity.SLOT_COUNT;
    public static final int PLAYER_START = PATTERN_SLOT_COUNT;
    public static final int HOTBAR_START = PLAYER_START + 27;
    public static final int PLAYER_END = HOTBAR_START + 9;

    private final Container patternInventory;

    public FactoryPatternMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, new SimpleContainer(PATTERN_SLOT_COUNT));
    }

    public FactoryPatternMenu(int containerId, Inventory playerInventory, Container patternInventory) {
        super(ModMenus.AE_FACTORY_PATTERN.get(), containerId);
        checkContainerSize(patternInventory, PATTERN_SLOT_COUNT);
        this.patternInventory = patternInventory;
        patternInventory.startOpen(playerInventory.player);

        for (int row = 0; row < 4; row++) {
            for (int column = 0; column < 9; column++) {
                int slot = row * 9 + column;
                addSlot(new Slot(patternInventory, slot,
                        8 + column * SLOT_SPACING, 18 + row * SLOT_SPACING) {
                    @Override public boolean mayPlace(ItemStack stack) {
                        return FactoryPatternBlockEntity.acceptsPattern(stack);
                    }
                    @Override public int getMaxStackSize() { return 1; }
                });
            }
        }

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(playerInventory, column + row * 9 + 9,
                        8 + column * SLOT_SPACING, 104 + row * SLOT_SPACING));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(playerInventory, column, 8 + column * SLOT_SPACING, 162));
        }
    }

    @Override public boolean stillValid(Player player) { return patternInventory.stillValid(player); }

    @Override public ItemStack quickMoveStack(Player player, int slotIndex) {
        if (slotIndex < 0 || slotIndex >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(slotIndex);
        if (!slot.hasItem() || !slot.mayPickup(player)) return ItemStack.EMPTY;

        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        boolean moved;
        if (slotIndex < PLAYER_START) {
            moved = moveItemStackTo(stack, PLAYER_START, PLAYER_END, true);
        } else {
            moved = slot.mayPlace(stack) && moveItemStackTo(stack, 0, PATTERN_SLOT_COUNT, false);
            if (!moved) {
                moved = slotIndex < HOTBAR_START
                        ? moveItemStackTo(stack, HOTBAR_START, PLAYER_END, false)
                        : moveItemStackTo(stack, PLAYER_START, HOTBAR_START, false);
            }
        }

        if (!moved || stack.getCount() == original.getCount()) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        slot.onTake(player, stack);
        return original;
    }

    @Override public void removed(Player player) {
        super.removed(player);
        patternInventory.stopOpen(player);
    }
}
