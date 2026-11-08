package com.appliedmoremachines;

import com.appliedmoremachines.production.*;

import com.appliedmoremachines.laser.FluidLaserSlots;
import com.appliedmoremachines.processing.AltarTransfers;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.*;

public final class FluidLaserBaseMenu extends AbstractContainerMenu {
    public static final int DATA_COUNT = 17, PLAYER_START = 25, HOTBAR_START = 52, PLAYER_END = 61, UPGRADE_START = 61;
    public static final int MAIN_PAGE = 100, CONFIG_PAGE = 101;
    public static final int[][] EQUIPMENT_POSITIONS = {
        {8,17},{26,17},{44,17},{8,35},{26,35},{44,35},{26,53},
        {80,17},{80,35},{80,53},
        {116,17},{134,17},{152,17},{116,35},{134,35},{152,35},{116,53},{134,53},{152,53},
        {8,93},{26,93},{44,93},{8,111},{26,111},{44,111}
    };
    private final Container machine;
    private final ContainerData data;
    private boolean configurationOpen;
    public FluidLaserBaseMenu(int id, Inventory inventory) { this(id, inventory, new SimpleContainer(FluidLaserSlots.SIZE)); }
    public FluidLaserBaseMenu(int id, Inventory inventory, Container machine) {
        super(ModMenus.AE_FLUID_LASER_BASE.get(), id); this.machine = machine;
        data = machine instanceof FluidLaserBaseBlockEntity be ? be.getSettings() : new SimpleContainerData(DATA_COUNT);
        checkContainerSize(machine, FluidLaserSlots.SIZE); checkContainerDataCount(data, DATA_COUNT); addDataSlots(data);
        for (int i=0;i<FluidLaserSlots.UPGRADE;i++) {
            final int slot=i;
            addSlot(new PageSlot(machine,slot,EQUIPMENT_POSITIONS[i][0],EQUIPMENT_POSITIONS[i][1]) {
                @Override public boolean mayPlace(ItemStack stack) { return !configurationOpen && FluidLaserSlots.accepts(machine,slot,stack); }
                @Override public int getMaxStackSize() { return slot>=FluidLaserSlots.DRILL?36:1; }
                @Override public int getMaxStackSize(ItemStack stack) { return Math.min(stack.getMaxStackSize(),FluidLaserSlots.limit(slot,stack)); }
            });
        }
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++)
            addSlot(new PageSlot(inventory, col + row * 9 + 9, 8 + col * 18, 140 + row * 18));
        for (int col = 0; col < 9; col++) addSlot(new PageSlot(inventory, col, 8 + col * 18, 198));
        for (int i = FluidLaserSlots.UPGRADE; i < FluidLaserSlots.SIZE; i++) {
            final int slot = i;
            addSlot(new PageSlot(machine, slot, 180, 5 + (slot - FluidLaserSlots.UPGRADE) * 18) {
                @Override public boolean mayPlace(ItemStack stack) { return !configurationOpen && FluidLaserSlots.accepts(machine, slot, stack); }
                @Override public int getMaxStackSize() { return slot == FluidLaserSlots.HEAL ? 1 : 4; }
                @Override public int getMaxStackSize(ItemStack stack) { return Math.min(stack.getMaxStackSize(), FluidLaserSlots.limit(slot, stack)); }
            });
        }
        machine.startOpen(inventory.player);
        if (MultiplicationUpgrade.enabled()) addSlot(new MultiplicationSlot(machine, 180, 113, () -> !configurationOpen));
    }
    private class PageSlot extends Slot {
        PageSlot(Container container, int slot, int x, int y) { super(container, slot, x, y); }
        @Override public boolean isActive() { return !configurationOpen; }
        @Override public boolean mayPickup(Player player) { return !configurationOpen && super.mayPickup(player); }
        @Override public int getMaxStackSize(ItemStack stack) { return Math.min(64, stack.getMaxStackSize()); }
    }
    public boolean autoExport() { return data.get(0) != 0; }
    private int wordPair(int index) { return (data.get(index) & 65535) | (data.get(index + 1) & 65535) << 16; }
    public int amount(int tank) { return Math.clamp(wordPair(4 + 4 * tank), 0, 64000); }
    public FluidStack fluid(int tank) {
        var fluid = BuiltInRegistries.FLUID.byId(wordPair(2 + 4 * tank));
        return fluid == null || fluid == Fluids.EMPTY || amount(tank) == 0 ? FluidStack.EMPTY : new FluidStack(fluid, amount(tank));
    }
    public int energyPercent() { return Math.clamp(data.get(16), 0, 100); }
    public double progress() { int time = data.get(15); return time > 0 ? Math.clamp((double) data.get(14) / time, 0, 1) : 0; }
    public boolean isOutputSideEnabled(InfusionOutputSide side) { return (data.get(1) & side.mask()) != 0; }
    public void setConfigurationOpen(boolean open) { configurationOpen = open; }
    @Override public void broadcastChanges() { if (!autoExport()) configurationOpen = false; super.broadcastChanges(); }
    @Override public boolean stillValid(Player player) { return machine.stillValid(player); }
    @Override public boolean clickMenuButton(Player player, int id) {
        if (player.level().isClientSide || player.isSpectator() || player.containerMenu != this || !stillValid(player)) return false;
        if (id == MAIN_PAGE || id == CONFIG_PAGE) {
            if (id == CONFIG_PAGE && !autoExport()) return false;
            configurationOpen = id == CONFIG_PAGE;
        } else if (id == 0) {
            data.set(0, autoExport() ? 0 : 1); if (!autoExport()) configurationOpen = false;
        } else if (id >= 1 && id <= 6) {
            if (!configurationOpen || !autoExport()) return false;
            data.set(1, data.get(1) ^ InfusionOutputSide.fromButtonId(id).mask());
        } else if (id >= 120 && id <= 122) {
            if (configurationOpen || !(machine instanceof FluidLaserBaseBlockEntity be)) return false;
            ItemStack carried = getCarried();
            if (carried.isEmpty() || carried.getCount() != 1) return false;
            var handler = be.getGuiTank(id - 120);
            var action = FluidUtil.tryFillContainer(carried, handler, Integer.MAX_VALUE, player, true);
            if (action.isSuccess()) setCarried(action.getResult());
        } else return false;
        broadcastChanges(); return true;
    }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        ItemStack multiplicationMove = MultiplicationMenus.quickMove(this, player, index);
        if (multiplicationMove != null) return multiplicationMove;
        if (configurationOpen || index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.isActive() || !slot.hasItem() || !slot.mayPickup(player)) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem(), original = stack.copy();
        if (index < PLAYER_START || index >= UPGRADE_START) AltarTransfers.move(stack, slots.subList(PLAYER_START, PLAYER_END).reversed());
        else if (com.appliedmoremachines.latex.LatexUpgrades.kind(stack) >= 0 || FluidLaserSlots.healingTier(stack)>0)
            AltarTransfers.move(stack, slots.subList(UPGRADE_START, UPGRADE_START + 6));
        else if (!AltarTransfers.move(stack, slots.subList(0, PLAYER_START)))
            AltarTransfers.move(stack, index < HOTBAR_START ? slots.subList(HOTBAR_START, PLAYER_END) : slots.subList(PLAYER_START, HOTBAR_START));
        if (stack.getCount() == original.getCount()) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        slot.onTake(player, stack); return original;
    }
    @Override public void clicked(int index, int button, ClickType type, Player player) {
        if (configurationOpen || index >= 0 && index < slots.size() && !slots.get(index).isActive()) return;
        super.clicked(index, button, type, player);
    }
    @Override public boolean canDragTo(Slot slot) { return slot.isActive() && super.canDragTo(slot); }
    @Override public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) { return slot.isActive() && super.canTakeItemForPickAll(stack, slot); }
    @Override public void removed(Player player) { super.removed(player); machine.stopOpen(player); }
}
