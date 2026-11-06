package com.appliedmoremachines;

import com.appliedmoremachines.processing.AltarInputs;
import com.appliedmoremachines.processing.AltarTransfers;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class InfusionPedestaMenu extends AbstractContainerMenu {
    public static final int IMAGE_WIDTH = 176;
    public static final int IMAGE_HEIGHT = 178;
    public static final int INPUT_X = 53;
    public static final int INPUT_Y = 22;
    public static final int OUTPUT_X = 109;
    public static final int OUTPUT_Y = 41;
    public static final int PLAYER_X = 8;
    public static final int PLAYER_Y = 94;
    public static final int HOTBAR_Y = 152;
    public static final int SLOT_SPACING = 18;
    public static final int SETTINGS_COUNT = 4;
    public static final int TOGGLE_AUTO_EXPORT = 0;
    public static final int MAIN_PAGE = 100;
    public static final int CONFIGURATION_PAGE = 102;
    public static final int UPGRADE_X = IMAGE_WIDTH + 4;
    public static final int UPGRADE_Y = 5;
    public static final int UPGRADE_PANEL_WIDTH = 24;
    public static final int UPGRADE_PANEL_HEIGHT = 80;

    private static final int PLAYER_START = InfusionPedestaBlockEntity.UPGRADE_START;
    private static final int HOTBAR_START = PLAYER_START + 27;
    private static final int PLAYER_END = HOTBAR_START + 9;
    public static final int UPGRADE_MENU_START = PLAYER_END;
    private static final int UPGRADE_MENU_END = UPGRADE_MENU_START + InfusionPedestaBlockEntity.UPGRADE_COUNT;

    private final Container altar;
    private final ContainerData outputSettings;
    private boolean configurationOpen;
    private final Player owner;

    /** Preserve legacy oversized counts during synchronization; input slots enforce native limits. */
    public InfusionPedestaMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, new SimpleContainer(InfusionPedestaBlockEntity.INVENTORY_SIZE) {
            @Override
            public int getMaxStackSize(ItemStack stack) { return 64; }
        });
    }

    public InfusionPedestaMenu(int containerId, Inventory playerInventory, Container altar) {
        super(ModMenus.AE_INFUSION_PEDESTA.get(), containerId);
        checkContainerSize(altar, InfusionPedestaBlockEntity.INVENTORY_SIZE);
        this.altar = altar;
        this.owner = playerInventory.player;
        this.outputSettings = altar instanceof InfusionPedestaBlockEntity blockEntity
                ? blockEntity.getOutputSettings() : new SimpleContainerData(SETTINGS_COUNT);
        checkContainerDataCount(outputSettings, SETTINGS_COUNT);
        addDataSlots(outputSettings);
        altar.startOpen(playerInventory.player);

        for (int input = 0; input < 3; input++) {
            addSlot(new InputSlot(input, INPUT_X, INPUT_Y + input * SLOT_SPACING));
        }
        addSlot(new PageSlot(altar, InfusionPedestaBlockEntity.OUTPUT_SLOT, OUTPUT_X, OUTPUT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new PageSlot(playerInventory, column + row * 9 + 9,
                        PLAYER_X + column * SLOT_SPACING, PLAYER_Y + row * SLOT_SPACING));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new PageSlot(playerInventory, column,
                    PLAYER_X + column * SLOT_SPACING, HOTBAR_Y));
        }
        // Append menu slots so existing machine/player menu indices remain stable.
        for (int upgrade = 0; upgrade < InfusionPedestaBlockEntity.UPGRADE_COUNT; upgrade++) {
            addSlot(new PageSlot(altar, InfusionPedestaBlockEntity.UPGRADE_START + upgrade,
                    UPGRADE_X, UPGRADE_Y + upgrade * SLOT_SPACING) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return InfusionUpgradeCards.accepts(stack);
                }

                @Override
                public int getMaxStackSize() {
                    return 1;
                }
            });
        }
        for (int extra = 0; extra < 6; extra++) {
            addSlot(new InputSlot(AltarInputs.slot(extra + 3),
                    INPUT_X - (extra < 3 ? 2 * SLOT_SPACING : SLOT_SPACING),
                    INPUT_Y + extra % 3 * SLOT_SPACING));
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return altar.stillValid(player);
    }

    public boolean isAutoExportEnabled() {
        return outputSettings.get(0) != 0;
    }

    public double getProcessingProgress() {
        int duration = outputSettings.get(3);
        return duration > 0 ? Math.clamp((double) outputSettings.get(2) / duration, 0, 1) : 0;
    }

    public boolean isOutputSideEnabled(InfusionOutputSide side) {
        return (outputSettings.get(1) & side.mask()) != 0;
    }

    /** Server-owned export settings and page controls. */
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
            if (side == null || !isAutoExportEnabled()) {
                return false;
            }
            outputSettings.set(1, outputSettings.get(1) ^ side.mask());
        }
        broadcastChanges();
        return true;
    }

    /** Client presentation state; changing pages keeps the same server container open. */
    public void setConfigurationOpen(boolean configurationOpen) {
        this.configurationOpen = configurationOpen;
    }

    private class InputSlot extends PageSlot {
        InputSlot(int index, int x, int y) {
            super(altar, index, x, y);
        }

        @Override public boolean mayPlace(ItemStack stack) { return AltarInputs.accepts(owner.level(), stack); }
        @Override public int getMaxStackSize(ItemStack stack) {
            return Math.min(getMaxStackSize(), stack.getMaxStackSize());
        }
        @Override public ItemStack remove(int amount) {
            return super.remove(Math.min(amount, getItem().getMaxStackSize()));
        }
        @Override public boolean allowModification(Player player) {
            // A removed recipe must never prevent taking existing material out of the machine.
            return mayPickup(player);
        }
    }

    private class PageSlot extends Slot {
        PageSlot(Container inventory, int index, int x, int y) {
            super(inventory, index, x, y);
        }

        @Override
        public boolean isActive() {
            return !configurationOpen;
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        if (slotIndex < 0 || slotIndex >= slots.size()) {
            return ItemStack.EMPTY;
        }
        Slot slot = slots.get(slotIndex);
        if (!slot.isActive() || !slot.hasItem() || !slot.mayPickup(player)) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (slotIndex < PLAYER_START || slotIndex >= UPGRADE_MENU_START) {
            if (!AltarTransfers.move(stack, slots.subList(PLAYER_START, PLAYER_END).reversed())) {
                return ItemStack.EMPTY;
            }
        } else if (InfusionUpgradeCards.accepts(stack)) {
            if (!moveItemStackTo(stack, UPGRADE_MENU_START, UPGRADE_MENU_END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!AltarTransfers.move(stack, slots.stream().filter(target -> target instanceof InputSlot).toList())) {
            // If the inputs are full, retain normal backpack/hotbar transfers.
            if (slotIndex < HOTBAR_START) {
                if (!moveItemStackTo(stack, HOTBAR_START, PLAYER_END, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!moveItemStackTo(stack, PLAYER_START, HOTBAR_START, false)) {
                return ItemStack.EMPTY;
            }
        }

        if (stack.getCount() == original.getCount()) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        slot.onTake(player, stack);
        return original;
    }

    @Override
    public boolean canDragTo(Slot slot) {
        // Protect oversized stacks retained from old saves until they have been extracted.
        return slot.isActive() && slot.getItem().getCount() <= slot.getItem().getMaxStackSize()
                && super.canDragTo(slot);
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return slot.isActive() && super.canTakeItemForPickAll(stack, slot);
    }

    @Override
    public void clicked(int index, int button, ClickType type, Player player) {
        if (index >= 0 && index < slots.size()) {
            Slot slot = slots.get(index);
            if (!slot.isActive()) return;
            ItemStack existing = slot.getItem();
            if (slot instanceof InputSlot && existing.getCount() > existing.getMaxStackSize()) {
                if (type == ClickType.PICKUP) {
                    if (getCarried().isEmpty()) {
                        ItemStack taken = slot.safeTake(button == 0 ? existing.getCount()
                                : (existing.getCount() + 1) / 2, existing.getMaxStackSize(), player);
                        setCarried(taken);
                    }
                    return;
                }
                if (type == ClickType.SWAP) {
                    if ((button >= 0 && button < 9 || button == 40)
                            && player.getInventory().getItem(button).isEmpty()) {
                        player.getInventory().setItem(button,
                                slot.safeTake(existing.getMaxStackSize(), existing.getMaxStackSize(), player));
                    }
                    return;
                }
            }
        }
        super.clicked(index, button, type, player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        altar.stopOpen(player);
    }
}
