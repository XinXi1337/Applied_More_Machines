package com.appliedmoremachines;

import com.appliedmoremachines.production.*;

import com.appliedmoremachines.energizing.EnergizingUpgrades;
import com.appliedmoremachines.energizing.EnergizingRecipes;
import com.appliedmoremachines.processing.AltarTransfers;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

public final class AEEnergizingMenu extends AbstractContainerMenu {
    public static final int DATA_COUNT = 5, PLAYER_START = 26, HOTBAR_START = 53, PLAYER_END = 62, UPGRADE_START = 62;
    public static final int MAIN_PAGE = 100, CONFIG_PAGE = 102;
    public static int[][] positions(boolean outer) {
        java.util.List<int[]> result=new java.util.ArrayList<>();
        for(int y=0;y<5;y++)for(int x=0;x<5;x++)if((x==0||x==4||y==0||y==4)==outer)result.add(new int[]{8+18*x,6+18*y});
        return result.toArray(int[][]::new);
    }
    private final Container machine;
    private final ContainerData data;
    private boolean configurationOpen;
    public AEEnergizingMenu(int id, Inventory inventory) { this(id, inventory, new SimpleContainer(27)); }
    public AEEnergizingMenu(int id, Inventory inventory, Container machine) {
        super(ModMenus.AE_ENERGIZING_ORB.get(), id); this.machine = machine;
        data = machine instanceof AEEnergizingBlockEntity be ? be.getSettings() : new SimpleContainerData(DATA_COUNT);
        checkContainerSize(machine, 27); checkContainerDataCount(data, DATA_COUNT); addDataSlots(data);
        int[][] rods=positions(true), inputs=positions(false);
        for(int i=0;i<25;i++){
            final int index=i;int[] pos=i<16?rods[i]:inputs[i-16];
            addSlot(new PageSlot(machine,i,pos[0],pos[1]){
                @Override public boolean mayPlace(ItemStack stack){return !configurationOpen && (index<16?EnergizingRecipes.rod(stack):EnergizingRecipes.material(inventory.player.level(),stack));}
                @Override public int getMaxStackSize(){return index<16?1:64;}
                @Override public int getMaxStackSize(ItemStack stack){return Math.min(getMaxStackSize(),stack.getMaxStackSize());}
            });
        }
        addSlot(new PageSlot(machine,25,148,42){@Override public boolean mayPlace(ItemStack stack){return false;}});
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++)
            addSlot(new PageSlot(inventory, col + row * 9 + 9, 8 + col * 18, 100 + row * 18));
        for (int col = 0; col < 9; col++) addSlot(new PageSlot(inventory, col, 8 + col * 18, 158));
        for (int i = 26; i < 27; i++) {
            final int slot = i;
            addSlot(new PageSlot(machine, slot, 180, 5 + (slot - 26) * 18) {
                @Override public boolean mayPlace(ItemStack stack) { return !configurationOpen && EnergizingUpgrades.accepts(machine, slot, stack); }
                @Override public int getMaxStackSize() { return 4; }
                @Override public int getMaxStackSize(ItemStack stack) { return Math.min(stack.getMaxStackSize(), 4); }
            });
        }
        machine.startOpen(inventory.player);
        if (MultiplicationUpgrade.enabled()) addSlot(new MultiplicationSlot(machine, 180, 23, () -> !configurationOpen));
    }
    private class PageSlot extends Slot {
        PageSlot(Container container, int slot, int x, int y) { super(container, slot, x, y); }
        @Override public boolean isActive() { return !configurationOpen; }
        @Override public boolean mayPickup(Player player) { return !configurationOpen && super.mayPickup(player); }
        @Override public int getMaxStackSize(ItemStack stack) { return Math.min(64, stack.getMaxStackSize()); }
    }
    public boolean autoExport() { return data.get(0) != 0; }
    public int energyPercent() { return Math.clamp(data.get(4), 0, 100); }
    public double progress() { return data.get(3) > 0 ? Math.clamp((double) data.get(2) / data.get(3), 0, 1) : 0; }
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
        else if (EnergizingUpgrades.isCard(stack)) AltarTransfers.move(stack, slots.subList(UPGRADE_START, UPGRADE_START + 1));
        else if (!AltarTransfers.move(stack, slots.subList(EnergizingRecipes.rod(stack)?0:16, EnergizingRecipes.rod(stack)?16:25)))
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
