package com.appliedmoremachines;

import com.appliedmoremachines.latex.LatexUpgrades;
import com.appliedmoremachines.processing.AltarTransfers;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.*;

public final class LatexExtractionProcessorMenu extends AbstractContainerMenu {
    public static final int DATA_COUNT = 15, PLAYER_START = 2, HOTBAR_START = 29, PLAYER_END = 38, UPGRADE_START = 38;
    public static final int MAIN_PAGE = 100, CONFIG_PAGE = 101;
    private final Container machine;
    private final ContainerData data;
    private boolean configurationOpen;
    public LatexExtractionProcessorMenu(int id, Inventory inventory) {
        this(id, inventory, new SimpleContainer(7));
    }
    public LatexExtractionProcessorMenu(int id, Inventory inventory, Container machine) {
        super(ModMenus.AE_LATEX_EXTRACTION_PROCESSOR.get(), id);
        this.machine = machine;
        data = machine instanceof LatexExtractionProcessorBlockEntity be ? be.getSettings() : new SimpleContainerData(DATA_COUNT);
        checkContainerSize(machine, 7); checkContainerDataCount(data, DATA_COUNT); addDataSlots(data);
        addSlot(new PageSlot(machine, 0, 16, 113) {
            @Override public boolean mayPlace(ItemStack stack) {
                return !configurationOpen && stack.is(net.minecraft.tags.ItemTags.LOGS)
                        && (!locksLogs() || ItemStack.isSameItemSameComponents(getItem(), stack));
            }
            @Override public boolean mayPickup(Player player) { return !locksLogs() && super.mayPickup(player); }
        });
        addSlot(new PageSlot(machine, 1, 138, 57) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
        });
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++)
            addSlot(new PageSlot(inventory, col + row * 9 + 9, 8 + col * 18, 140 + row * 18));
        for (int col = 0; col < 9; col++) addSlot(new PageSlot(inventory, col, 8 + col * 18, 198));
        for (int i = 2; i < 7; i++) {
            final int slot = i;
            addSlot(new PageSlot(machine, slot, 180, 5 + (slot - 2) * 18) {
                @Override public boolean mayPlace(ItemStack stack) { return !configurationOpen && LatexUpgrades.accepts(machine, slot, stack); }
                @Override public int getMaxStackSize() { return 4; }
                @Override public int getMaxStackSize(ItemStack stack) { return Math.min(stack.getMaxStackSize(), LatexUpgrades.limit(stack)); }
            });
        }
        machine.startOpen(inventory.player);
    }
    private class PageSlot extends Slot {
        PageSlot(Container container, int slot, int x, int y) { super(container, slot, x, y); }
        @Override public boolean isActive() { return !configurationOpen; }
        @Override public boolean mayPickup(Player player) { return !configurationOpen && super.mayPickup(player); }
        @Override public int getMaxStackSize(ItemStack stack) { return Math.min(64, stack.getMaxStackSize()); }
    }
    public boolean autoExport() { return data.get(0) != 0; }
    public int mode() { return data.get(1); }
    public int amount(int tank) { return data.get(tank == 0 ? 2 : 3); }
    public int energyPercent() { return Math.clamp(data.get(8), 0, 100); }
    public boolean locksLogs() { return data.get(14) != 0; }
    public double progress(boolean rubber) {
        int time = data.get(rubber ? 7 : 5);
        return time > 0 ? Math.clamp((double) data.get(rubber ? 6 : 4) / time, 0, 1) : 0;
    }
    public boolean isOutputSideEnabled(InfusionOutputSide side) {
        return ((data.get(12) | data.get(13)) & side.mask()) != 0;
    }
    public void setConfigurationOpen(boolean open) { configurationOpen = open; }
    @Override public void broadcastChanges() {
        if (!autoExport()) configurationOpen = false;
        super.broadcastChanges();
    }
    @Override public boolean stillValid(Player player) { return machine.stillValid(player); }
    @Override public boolean clickMenuButton(Player player, int id) {
        if (player.level().isClientSide || player.isSpectator() || player.containerMenu != this || !stillValid(player)) return false;
        if (id == MAIN_PAGE || id == CONFIG_PAGE) {
            if (id == CONFIG_PAGE && !autoExport()) return false;
            configurationOpen = id == CONFIG_PAGE;
        } else if (id == 0) {
            data.set(0, autoExport() ? 0 : 1);
            if (!autoExport()) configurationOpen = false;
        } else if (id >= 1 && id <= 6) {
            if (!configurationOpen || !autoExport()) return false;
            InfusionOutputSide side = InfusionOutputSide.fromButtonId(id);
            int mask = (data.get(12) | data.get(13)) ^ side.mask();
            data.set(12, mask);
            data.set(13, mask);
        } else if (id >= 10 && id <= 12) {
            if (configurationOpen) return false;
            int mode = id - 9; data.set(1, mode() == mode ? 0 : mode);
        } else if (id == 120 || id == 121) {
            if (configurationOpen || !(machine instanceof LatexExtractionProcessorBlockEntity be)) return false;
            ItemStack carried = getCarried();
            // A single carried container makes the exchange atomic without inventory spill or partial stack loss.
            if (carried.isEmpty() || carried.getCount() != 1) return false;
            var handler = be.getGuiTank(id - 120);
            var action = FluidUtil.tryEmptyContainer(carried, handler, Integer.MAX_VALUE, player, true);
            if (!action.isSuccess()) action = FluidUtil.tryFillContainer(carried, handler, Integer.MAX_VALUE, player, true);
            if (action.isSuccess()) setCarried(action.getResult());
        } else return false;
        broadcastChanges(); return true;
    }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (configurationOpen || index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.isActive() || !slot.hasItem() || !slot.mayPickup(player)) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem(), original = stack.copy();
        if (index < PLAYER_START || index >= UPGRADE_START) {
            AltarTransfers.move(stack, slots.subList(PLAYER_START, PLAYER_END).reversed());
        } else if (LatexUpgrades.kind(stack) >= 0) {
            AltarTransfers.move(stack, slots.subList(UPGRADE_START, UPGRADE_START + 5));
        } else if (!AltarTransfers.move(stack, slots.subList(0, 1))) {
            AltarTransfers.move(stack, index < HOTBAR_START ? slots.subList(HOTBAR_START, PLAYER_END) : slots.subList(PLAYER_START, HOTBAR_START));
        }
        if (stack.getCount() == original.getCount()) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        slot.onTake(player, stack); return original;
    }
    @Override public void clicked(int index, int button, ClickType type, Player player) {
        if (configurationOpen) return;
        if (index >= 0 && index < slots.size() && !slots.get(index).isActive()) return;
        super.clicked(index, button, type, player);
    }
    @Override public boolean canDragTo(Slot slot) { return slot.isActive() && super.canDragTo(slot); }
    @Override public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return slot.isActive() && (slot.index != 0 || !locksLogs()) && super.canTakeItemForPickAll(stack, slot);
    }
    @Override public void removed(Player player) { super.removed(player); machine.stopOpen(player); }
}
