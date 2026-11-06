package com.appliedmoremachines;

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
import com.appliedmoremachines.fusion.CoreInjectors;
import com.appliedmoremachines.fusion.CoreRecipes;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public final class CraftingCoreMenu extends AbstractContainerMenu {
    public static final int IMAGE_WIDTH = 176;
    public static final int IMAGE_HEIGHT = 210;
    public static final int SLOT_SPACING = 18;
    public static final int PLAYER_X = 8;
    public static final int PLAYER_Y = 126;
    public static final int HOTBAR_Y = 184;
    public static final int UPGRADE_X = IMAGE_WIDTH + 4;
    public static final int UPGRADE_Y = 5;
    public static final int UPGRADE_PANEL_WIDTH = 24;
    public static final int UPGRADE_PANEL_HEIGHT = 80;
    public static final int SETTINGS_COUNT = 6;
    public static final int TOGGLE_AUTO_EXPORT = 0;
    public static final int MAIN_PAGE = 100;
    public static final int CONFIGURATION_PAGE = 102;
    public static final int PLAYER_START = CraftingCoreBlockEntity.UPGRADE_START;
    public static final int HOTBAR_START = PLAYER_START + 27;
    public static final int PLAYER_END = HOTBAR_START + 9;
    public static final int UPGRADE_MENU_START = PLAYER_END;
    public static final int UPGRADE_MENU_END = UPGRADE_MENU_START + CraftingCoreBlockEntity.UPGRADE_COUNT;

    private final Container core;
    private final ContainerData outputSettings;
    private final Inventory playerInventory;
    private boolean configurationOpen;
    private boolean changingInventory;

    public CraftingCoreMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, new SimpleContainer(CraftingCoreBlockEntity.INVENTORY_SIZE));
    }

    public CraftingCoreMenu(int containerId, Inventory inventory, Container core) {
        super(ModMenus.AE_CRAFTING_CORE.get(), containerId);
        checkContainerSize(core, CraftingCoreBlockEntity.INVENTORY_SIZE);
        this.core = core;
        playerInventory = inventory;
        outputSettings = core instanceof CraftingCoreBlockEntity blockEntity
                ? blockEntity.getOutputSettings() : new SimpleContainerData(SETTINGS_COUNT);
        checkContainerDataCount(outputSettings, SETTINGS_COUNT);
        addDataSlots(outputSettings);
        core.startOpen(inventory.player);

        for (int group = 0; group < 2; group++) {
            for (int row = 0; row < 4; row++) {
                for (int column = 0; column < 2; column++) {
                    addSlot(new PageSlot(core, group * 8 + row * 2 + column,
                            (group == 0 ? 18 : 124) + column * SLOT_SPACING, 26 + row * SLOT_SPACING) {
                        @Override public boolean isActive() {
                            return super.isActive() && (getContainerSlot() < unlockedSlots() || hasItem());
                        }
                        @Override public boolean mayPlace(ItemStack stack) {
                            return isActive() && getContainerSlot() < unlockedSlots() && super.mayPlace(stack);
                        }
                    });
                }
            }
        }
        addSlot(new PageSlot(core, CraftingCoreBlockEntity.INJECTOR_SLOT, 80, 26) {
            @Override public boolean mayPlace(ItemStack stack) {
                return !isWorking() && CoreInjectors.tier(stack) >= 0 && super.mayPlace(stack);
            }
            @Override public boolean mayPickup(Player player) { return !isWorking() && super.mayPickup(player); }
            @Override public int getMaxStackSize() { return CoreInjectors.MAX_COUNT; }
        });
        addSlot(new PageSlot(core, CraftingCoreBlockEntity.CENTER_SLOT, 80, 80) {
            @Override public boolean mayPlace(ItemStack stack) { return !isResult() && super.mayPlace(stack); }
            @Override public int getMaxStackSize(ItemStack stack) {
                return isResult() ? stack.getMaxStackSize() : Math.min(stack.getMaxStackSize(),
                        Math.max(1, CoreRecipes.catalystLimit(playerInventory.player.level(), stack)));
            }
        });
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new PageSlot(inventory, column + row * 9 + 9,
                        PLAYER_X + column * SLOT_SPACING, PLAYER_Y + row * SLOT_SPACING));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new PageSlot(inventory, column, PLAYER_X + column * SLOT_SPACING, HOTBAR_Y));
        }
        for (int upgrade = 0; upgrade < CraftingCoreBlockEntity.UPGRADE_COUNT; upgrade++) {
            addSlot(new PageSlot(core, CraftingCoreBlockEntity.UPGRADE_START + upgrade,
                    UPGRADE_X, UPGRADE_Y + upgrade * SLOT_SPACING) {
                @Override public boolean mayPlace(ItemStack stack) { return InfusionUpgradeCards.accepts(stack); }
                @Override public int getMaxStackSize() { return 1; }
            });
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return core.stillValid(player);
    }

    public boolean isAutoExportEnabled() {
        return outputSettings.get(0) != 0;
    }

    public int unlockedSlots() { return CoreInjectors.unlocked(core.getItem(CraftingCoreBlockEntity.INJECTOR_SLOT)); }
    public int injectorTier() { return CoreInjectors.tier(core.getItem(CraftingCoreBlockEntity.INJECTOR_SLOT)); }
    public boolean isWorking() { return outputSettings.get(4) != 0; }
    public boolean isResult() { return outputSettings.get(5) != 0; }
    public int progressPercent() {
        return outputSettings.get(3) > 0 ? Math.clamp(outputSettings.get(2) * 100 / outputSettings.get(3), 0, 100) : 0;
    }

    public boolean isOutputSideEnabled(InfusionOutputSide side) {
        return (outputSettings.get(1) & side.mask()) != 0;
    }

    public boolean isConfigurationOpen() {
        return configurationOpen && isAutoExportEnabled();
    }

    public void setConfigurationOpen(boolean open) {
        configurationOpen = open && isAutoExportEnabled();
    }

    @Override
    public boolean clickMenuButton(Player player, int buttonId) {
        if (player.level().isClientSide || player.isSpectator()
                || player.containerMenu != this || !stillValid(player)) {
            return false;
        }
        if (buttonId == MAIN_PAGE || buttonId == CONFIGURATION_PAGE) {
            if (buttonId == CONFIGURATION_PAGE && !isAutoExportEnabled()) return false;
            setConfigurationOpen(buttonId == CONFIGURATION_PAGE);
        } else if (buttonId == TOGGLE_AUTO_EXPORT) {
            outputSettings.set(0, isAutoExportEnabled() ? 0 : 1);
            if (!isAutoExportEnabled()) configurationOpen = false;
        } else {
            InfusionOutputSide side = InfusionOutputSide.fromButtonId(buttonId);
            if (side == null || !isConfigurationOpen()) return false;
            outputSettings.set(1, outputSettings.get(1) ^ side.mask());
        }
        broadcastChanges();
        return true;
    }

    private class PageSlot extends Slot {
        PageSlot(Container inventory, int index, int x, int y) {
            super(inventory, index, x, y);
        }

        @Override public boolean isActive() { return !isConfigurationOpen(); }
        @Override public boolean mayPlace(ItemStack stack) { return isActive() && container.canPlaceItem(getContainerSlot(), stack); }
        @Override public boolean mayPickup(Player player) { return isActive(); }
        @Override public int getMaxStackSize(ItemStack stack) {
            return Math.min(getMaxStackSize(), stack.getMaxStackSize());
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        return inventoryChange(player, () -> quickMove(player, slotIndex), ItemStack.EMPTY);
    }

    private ItemStack quickMove(Player player, int slotIndex) {
        if (isConfigurationOpen() || slotIndex < 0 || slotIndex >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(slotIndex);
        if (!slot.hasItem() || !slot.mayPickup(player)) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (slotIndex < PLAYER_START || slotIndex >= UPGRADE_MENU_START) {
            if (!moveItemStackTo(stack, PLAYER_START, PLAYER_END, true)) return ItemStack.EMPTY;
        } else if (InfusionUpgradeCards.accepts(stack)) {
            if (!moveItemStackTo(stack, UPGRADE_MENU_START, UPGRADE_MENU_END, false)) return ItemStack.EMPTY;
        } else {
            if (CoreInjectors.tier(stack) >= 0) {
                moveItemStackTo(stack, CraftingCoreBlockEntity.INJECTOR_SLOT, CraftingCoreBlockEntity.INJECTOR_SLOT + 1, false);
            }
            if (!stack.isEmpty() && CoreRecipes.catalystLimit(player.level(), stack) > 0) {
                moveItemStackTo(stack, CraftingCoreBlockEntity.CENTER_SLOT, CraftingCoreBlockEntity.CENTER_SLOT + 1, false);
            }
            if (!stack.isEmpty()) moveItemStackTo(stack, 0, CraftingCoreBlockEntity.INPUT_COUNT, false);
            if (stack.getCount() != original.getCount()) {
                if (core instanceof CraftingCoreBlockEntity blockEntity) blockEntity.arrangeAutomaticInputs();
                if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
                slot.onTake(player, stack);
                return original;
            }
            if (slotIndex < HOTBAR_START) {
                if (!moveItemStackTo(stack, HOTBAR_START, PLAYER_END, false)) return ItemStack.EMPTY;
            } else if (!moveItemStackTo(stack, PLAYER_START, HOTBAR_START, false)) {
                return ItemStack.EMPTY;
            }
        }
        if (stack.getCount() == original.getCount()) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        slot.onTake(player, stack);
        return original;
    }

    @Override
    public void clicked(int index, int button, ClickType type, Player player) {
        if (isConfigurationOpen()) return;
        // Vanilla can drop displaced SWAP stacks before a transaction can be rolled back.
        if (index == CraftingCoreBlockEntity.INJECTOR_SLOT) {
            if (type == ClickType.THROW) return;
            if (type == ClickType.SWAP && (button >= 0 && button < 9 || button == 40)
                    && player.getInventory().getItem(button).getCount() > CoreInjectors.MAX_COUNT) return;
        }
        inventoryChange(player, () -> { super.clicked(index, button, type, player); return true; }, false);
    }

    /** All Shift merges must respect filters too; vanilla skips mayPlace on occupied slots. */
    @Override
    protected boolean moveItemStackTo(ItemStack stack, int start, int end, boolean reverse) {
        int before = stack.getCount();
        for (int pass = 0; pass < 2 && !stack.isEmpty(); pass++) {
            for (int step = 0; step < end - start && !stack.isEmpty(); step++) {
                Slot target = slots.get(reverse ? end - 1 - step : start + step);
                if (!target.isActive() || !target.mayPlace(stack)) continue;
                ItemStack existing = target.getItem();
                if (pass == 0 && (existing.isEmpty() || !ItemStack.isSameItemSameComponents(existing, stack))) continue;
                if (pass == 1 && !existing.isEmpty()) continue;
                int count = Math.max(0, Math.min(stack.getCount(), target.getMaxStackSize(stack) - existing.getCount()));
                if (count == 0) continue;
                target.setByPlayer(stack.copyWithCount(existing.getCount() + count));
                stack.shrink(count);
            }
        }
        return before != stack.getCount();
    }

    /** Shrinking the rack and returning the newly locked materials are one reversible operation. */
    private <T> T inventoryChange(Player player, Supplier<T> operation, T rejected) {
        if (changingInventory) return operation.get();
        changingInventory = true;
        List<ItemStack> machine = snapshot(core), inventory = snapshot(player.getInventory());
        ItemStack cursor = getCarried().copy();
        boolean wasResult = isResult();
        int oldUnlocked = unlockedSlots();
        try {
            T value = operation.get();
            if (unlockedSlots() < oldUnlocked) {
                for (int slot = unlockedSlots(); slot < CraftingCoreBlockEntity.INPUT_COUNT; slot++) {
                    ItemStack remaining = core.getItem(slot).copy();
                    if (remaining.isEmpty()) continue;
                    returnToInventory(player.getInventory(), remaining);
                    if (!remaining.isEmpty()) {
                        if (core instanceof CraftingCoreBlockEntity blockEntity) blockEntity.restoreResultFlag(wasResult);
                        restore(core, machine); restore(player.getInventory(), inventory); setCarried(cursor);
                        return rejected;
                    }
                    core.setItem(slot, ItemStack.EMPTY);
                }
            }
            return value;
        } finally { changingInventory = false; }
    }
    private static List<ItemStack> snapshot(Container container) {
        List<ItemStack> snapshot = new ArrayList<>();
        for (int slot = 0; slot < container.getContainerSize(); slot++) snapshot.add(container.getItem(slot).copy());
        return snapshot;
    }
    private static void restore(Container container, List<ItemStack> snapshot) {
        for (int slot = 0; slot < snapshot.size(); slot++) container.setItem(slot, snapshot.get(slot).copy());
        container.setChanged();
    }
    private static void returnToInventory(Inventory inventory, ItemStack remaining) {
        // Inventory.add discards excess in creative mode; returns must conserve items in every mode.
        for (int pass = 0; pass < 2 && !remaining.isEmpty(); pass++) {
            for (int slot = 0; slot < inventory.items.size() && !remaining.isEmpty(); slot++) {
                ItemStack existing = inventory.getItem(slot);
                if (pass == 0 && (existing.isEmpty() || !ItemStack.isSameItemSameComponents(existing, remaining))) continue;
                if (pass == 1 && !existing.isEmpty()) continue;
                int count = Math.max(0, Math.min(remaining.getCount(),
                        Math.min(remaining.getMaxStackSize(), inventory.getMaxStackSize()) - existing.getCount()));
                if (count > 0) {
                    inventory.setItem(slot, remaining.copyWithCount(existing.getCount() + count));
                    remaining.shrink(count);
                }
            }
        }
        inventory.setChanged();
    }

    @Override
    public boolean canDragTo(Slot slot) {
        return slot.isActive() && !(slot.index == CraftingCoreBlockEntity.INJECTOR_SLOT && isWorking()) && super.canDragTo(slot);
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return slot.isActive() && !(slot.index == CraftingCoreBlockEntity.INJECTOR_SLOT && isWorking()) && super.canTakeItemForPickAll(stack, slot);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        core.stopOpen(player);
    }
}
