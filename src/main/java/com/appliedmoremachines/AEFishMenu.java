package com.appliedmoremachines;

import com.appliedmoremachines.production.*;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class AEFishMenu extends AbstractContainerMenu {
    public static final int UPGRADE_X = 180, UPGRADE_Y = 5, SLOT_SPACING = 18, UPGRADE_PANEL_WIDTH = 24, UPGRADE_PANEL_HEIGHT = 80;
    public static final int IMAGE_WIDTH = 176, IMAGE_HEIGHT = 224, SETTINGS_COUNT = 6;
    public static final int ROD_MENU_SLOT = 27, PLAYER_START = 28, HOTBAR_START = 55, PLAYER_END = 64, UPGRADE_MENU_START = 64;
    public static final int TOGGLE_AUTO_EXPORT = 0, MAIN_PAGE = 100, CONFIGURATION_PAGE = 102, MODE_START = 200;
    public static final int TOGGLE_BLOCK_UNSTACKABLE = 204;
    private final Container machine;
    private final ContainerData settings;
    private boolean configurationOpen;

    public AEFishMenu(int id, Inventory inventory) { this(id, inventory, new SimpleContainer(AEFishBlockEntity.INVENTORY_SIZE)); }
    public AEFishMenu(int id, Inventory inventory, Container machine) {
        super(ModMenus.AE_FISH.get(), id);
        checkContainerSize(machine, AEFishBlockEntity.INVENTORY_SIZE);
        this.machine = machine;
        settings = machine instanceof AEFishBlockEntity fish ? fish.getSettings() : new SimpleContainerData(SETTINGS_COUNT);
        addDataSlots(settings); machine.startOpen(inventory.player);
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++) {
            addSlot(new PageSlot(machine, row * 9 + col, 8 + 18 * col, 18 + 18 * row) {
                @Override public boolean mayPlace(ItemStack stack) { return false; }
            });
        }
        addSlot(new PageSlot(machine, AEFishBlockEntity.ROD_SLOT, 8, 113) {
            @Override public boolean mayPlace(ItemStack stack) { return AEFishBlockEntity.acceptsRod(stack); }
            @Override public int getMaxStackSize() { return 1; }
        });
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++)
            addSlot(new PageSlot(inventory, col + row * 9 + 9, 8 + 18 * col, 140 + 18 * row));
        for (int col = 0; col < 9; col++) addSlot(new PageSlot(inventory, col, 8 + 18 * col, 198));
        for (int card = 0; card < 4; card++) addSlot(new PageSlot(machine, AEFishBlockEntity.UPGRADE_START + card, 180, 5 + 18 * card) {
            @Override public boolean mayPlace(ItemStack stack) { return InfusionUpgradeCards.accepts(stack); }
            @Override public int getMaxStackSize() { return 1; }
        });
        if (MultiplicationUpgrade.enabled()) addSlot(new MultiplicationSlot(machine, 180, 77, () -> !configurationOpen));
    }
    @Override public boolean stillValid(Player player) { return machine.stillValid(player); }
    public boolean isAutoExportEnabled() { return settings.get(0) != 0; }
    public boolean isOutputSideEnabled(InfusionOutputSide side) { return (settings.get(1) & side.mask()) != 0; }
    public int mode() { return settings.get(4); }
    public boolean isBlockingUnstackable() { return settings.get(5) != 0; }
    public double getProcessingProgress() { return settings.get(3) > 0 ? Math.clamp((double) settings.get(2) / settings.get(3), 0, 1) : 0; }
    public void setConfigurationOpen(boolean open) { configurationOpen = open; }
    @Override public boolean clickMenuButton(Player player, int button) {
        if (player.level().isClientSide || player.isSpectator() || player.containerMenu != this || !stillValid(player)) return false;
        if (button == MAIN_PAGE || button == CONFIGURATION_PAGE) {
            if (button == CONFIGURATION_PAGE && !isAutoExportEnabled()) return false;
            configurationOpen = button == CONFIGURATION_PAGE;
        } else if (button == TOGGLE_AUTO_EXPORT) {
            settings.set(0, isAutoExportEnabled() ? 0 : 1);
            if (!isAutoExportEnabled()) configurationOpen = false;
        } else if (button == TOGGLE_BLOCK_UNSTACKABLE && !configurationOpen) {
            settings.set(5, isBlockingUnstackable() ? 0 : 1);
        } else if (button >= MODE_START + 1 && button <= MODE_START + 3 && !configurationOpen) {
            int selected = button - MODE_START;
            settings.set(4, mode() == selected ? 0 : selected);
        } else {
            InfusionOutputSide side = InfusionOutputSide.fromButtonId(button);
            if (side == null || !isAutoExportEnabled()) return false;
            settings.set(1, settings.get(1) ^ side.mask());
        }
        broadcastChanges(); return true;
    }
    private class PageSlot extends Slot {
        PageSlot(Container inventory, int slot, int x, int y) { super(inventory, slot, x, y); }
        @Override public boolean isActive() { return !configurationOpen; }
    }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        ItemStack multiplicationMove = MultiplicationMenus.quickMove(this, player, index);
        if (multiplicationMove != null) return multiplicationMove;
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.isActive() || !slot.hasItem() || !slot.mayPickup(player)) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem(), before = stack.copy();
        boolean moved;
        if (index < PLAYER_START || index >= UPGRADE_MENU_START) moved = moveItemStackTo(stack, PLAYER_START, PLAYER_END, true);
        else {
            moved = InfusionUpgradeCards.accepts(stack) && moveItemStackTo(stack, UPGRADE_MENU_START, slots.size(), false);
            if (!moved && AEFishBlockEntity.acceptsRod(stack)) moved = moveItemStackTo(stack, ROD_MENU_SLOT, ROD_MENU_SLOT + 1, false);
            if (!moved) moved = index < HOTBAR_START ? moveItemStackTo(stack, HOTBAR_START, PLAYER_END, false)
                    : moveItemStackTo(stack, PLAYER_START, HOTBAR_START, false);
        }
        if (!moved || stack.getCount() == before.getCount()) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        slot.onTake(player, stack); return before;
    }
    @Override public void clicked(int index, int button, ClickType type, Player player) {
        if (configurationOpen) return;
        super.clicked(index, button, type, player);
    }
    @Override public boolean canDragTo(Slot slot) { return slot.isActive() && super.canDragTo(slot); }
    @Override public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) { return slot.isActive() && super.canTakeItemForPickAll(stack, slot); }
    @Override public void removed(Player player) { super.removed(player); machine.stopOpen(player); }
}
