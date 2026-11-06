package com.appliedmoremachines;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** One local upgrade slot per controller and four actions shared by the structure. */
public final class ReactorControllerMenu extends AbstractContainerMenu {
    public static final int IMAGE_WIDTH = 176;
    public static final int BASE_HEIGHT = 119;
    public static final int IMAGE_HEIGHT = 204;
    public static final int UPGRADE_X = IMAGE_WIDTH + 4;
    public static final int UPGRADE_Y = 8;
    public static final int SLOT_SPACING = 18;
    public static final int UPGRADE_PANEL_WIDTH = 24;
    public static final int UPGRADE_PANEL_HEIGHT = 26;
    public static final int PLAYER_X = 8;
    public static final int PLAYER_Y = 126;
    public static final int HOTBAR_Y = 184;
    public static final int PLAYER_START = ReactorControllerBlockEntity.UPGRADE_COUNT;
    public static final int PLAYER_END = PLAYER_START + 36;
    public static final int DATA_COUNT = 24;
    public static final int TOGGLE_PROJECTION = 201;
    public static final int PLACE_STRUCTURE = 202;
    public static final int DISMANTLE_STRUCTURE = 203;

    private final Container controllerInventory;
    private final ContainerData data;
    private final ReactorControllerBlockEntity controller;
    private final Inventory playerInventory;

    public ReactorControllerMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, new SimpleContainer(ReactorControllerBlockEntity.UPGRADE_COUNT),
                new SimpleContainerData(DATA_COUNT), null);
    }

    public ReactorControllerMenu(int containerId, Inventory inventory, ReactorControllerBlockEntity controller) {
        this(containerId, inventory, controller, controller.getMenuData(), controller);
    }

    private ReactorControllerMenu(int containerId, Inventory inventory, Container controllerInventory,
            ContainerData data, ReactorControllerBlockEntity controller) {
        super(ModMenus.AE_REACTORS_CONTROLLER.get(), containerId);
        checkContainerSize(controllerInventory, ReactorControllerBlockEntity.UPGRADE_COUNT);
        checkContainerDataCount(data, DATA_COUNT);
        this.controllerInventory = controllerInventory;
        this.data = data;
        this.controller = controller;
        this.playerInventory = inventory;
        addDataSlots(data);
        controllerInventory.startOpen(inventory.player);

        for (int slot = 0; slot < ReactorControllerBlockEntity.UPGRADE_COUNT; slot++) {
            addSlot(new Slot(controllerInventory, slot, UPGRADE_X, UPGRADE_Y + slot * SLOT_SPACING) {
                @Override public boolean mayPlace(ItemStack stack) {
                    ReactorUpgradeCards.Kind kind = ReactorUpgradeCards.type(stack);
                    return kind != null && (controller == null || controller.canPlaceItem(getContainerSlot(), stack));
                }

                @Override public int getMaxStackSize(ItemStack stack) {
                    ReactorUpgradeCards.Kind kind = ReactorUpgradeCards.type(stack);
                    return kind == null ? 0 : kind.limit();
                }
            });
        }
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9,
                        PLAYER_X + column * SLOT_SPACING, PLAYER_Y + row * SLOT_SPACING));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, PLAYER_X + column * SLOT_SPACING, HOTBAR_Y));
        }
    }

    public boolean isFormed() { return data.get(0) != 0; }
    public int parallel() { return data.get(1); }
    public int durationTicks() { return data.get(2); }
    public boolean isAeConnected() { return data.get(3) != 0; }
    public boolean projectionEnabled() { return data.get(4) != 0; }
    public int dismantleClicks() { return data.get(5); }
    public int actionStatus() { return data.get(6); }
    public int actionAmount() { return data.get(7); }
    public int combinedCount(ReactorUpgradeCards.Kind kind) { return data.get(8 + kind.ordinal()); }
    public int progress() { return data.get(12); }
    public int processingState() { return data.get(13); }
    public int illegalPatterns() { return data.get(14); }
    public int queuedTasks() { return (data.get(15) & 0xFFFF) | ((data.get(22) & 0xFFFF) << 16); }
    public int activeTasks() { return (data.get(16) & 0xFFFF) | ((data.get(23) & 0xFFFF) << 16); }
    public int chemicalPaused() { return data.get(17); }
    public java.util.List<appeng.api.stacks.GenericStack> taskDisplay() {
        if (controller != null) return controller.taskDisplay();
        var entity = playerInventory.player.level().getBlockEntity(controllerPosition());
        return entity instanceof ReactorControllerBlockEntity factory ? factory.taskDisplay() : java.util.List.of();
    }
    public net.minecraft.core.BlockPos controllerPosition() {
        if (controller != null) return controller.getBlockPos();
        long position = 0;
        for (int index = 0; index < 4; index++) position |= ((long) data.get(18 + index) & 65535) << (index * 16);
        return net.minecraft.core.BlockPos.of(position);
    }

    @Override public boolean stillValid(Player player) {
        return controller == null || controller.stillValid(player);
    }

    @Override public boolean clickMenuButton(Player player, int buttonId) {
        if (player.level().isClientSide || controller == null || player.isSpectator()
                || player.containerMenu != this || !stillValid(player)) {
            return false;
        }
        if (buttonId == TOGGLE_PROJECTION) {
            controller.toggleProjection();
        } else if (buttonId == PLACE_STRUCTURE) {
            ReactorOperations.place(controller, player);
        } else if (buttonId == DISMANTLE_STRUCTURE) {
            controller.recordDismantleClick(player);
        } else {
            return false;
        }
        broadcastChanges();
        return true;
    }

    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem() || !slot.mayPickup(player)) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < PLAYER_START) {
            if (!moveItemStackTo(stack, PLAYER_START, PLAYER_END, true)) return ItemStack.EMPTY;
        } else if (ReactorUpgradeCards.type(stack) != null
                && moveItemStackTo(stack, 0, PLAYER_START, false)) {
            // The remainder stays in the player inventory.
        } else if (index < PLAYER_END - 9) {
            if (!moveItemStackTo(stack, PLAYER_END - 9, PLAYER_END, false)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, PLAYER_START, PLAYER_END - 9, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.getCount() == original.getCount()) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        slot.onTake(player, stack);
        return original;
    }

    /** Vanilla skips mayPlace for occupied slots, which would bypass controller-wide type limits. */
    @Override
    protected boolean moveItemStackTo(ItemStack stack, int start, int end, boolean reverse) {
        int before = stack.getCount();
        for (int pass = 0; pass < 2 && !stack.isEmpty(); pass++) {
            for (int step = 0; step < end - start && !stack.isEmpty(); step++) {
                Slot target = slots.get(reverse ? end - 1 - step : start + step);
                if (!target.mayPlace(stack)) continue;
                ItemStack existing = target.getItem();
                if (pass == 0 && (existing.isEmpty()
                        || !ItemStack.isSameItemSameComponents(existing, stack))) continue;
                if (pass == 1 && !existing.isEmpty()) continue;
                int count = Math.max(0, Math.min(stack.getCount(),
                        target.getMaxStackSize(stack) - existing.getCount()));
                if (count <= 0) continue;
                target.setByPlayer(stack.copyWithCount(existing.getCount() + count));
                stack.shrink(count);
            }
        }
        return before != stack.getCount();
    }

    @Override public void removed(Player player) {
        super.removed(player);
        controllerInventory.stopOpen(player);
    }
}
