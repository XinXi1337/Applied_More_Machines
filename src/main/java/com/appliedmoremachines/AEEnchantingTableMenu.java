package com.appliedmoremachines;

import com.appliedmoremachines.enchanting.EnchantingRules;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
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
import net.minecraft.world.item.enchantment.Enchantment;

/** Vanilla slot synchronization plus small, validated server-owned enchantment selections. */
public final class AEEnchantingTableMenu extends AbstractContainerMenu {
    public static final int IMAGE_WIDTH = 176, IMAGE_HEIGHT = 170;
    public static final int CONFIRM = 0, SELECT = 0, DECREASE = 1, INCREASE = 2, MINIMUM = 3, MAXIMUM = 4, DESELECT = 5;
    private static final int HEADER = 12, ACTION_BASE = 100, ACTION_COUNT = 6;
    private final Container table;
    private final List<Holder<Enchantment>> catalog;
    private final ContainerData data;

    public AEEnchantingTableMenu(int id, Inventory inventory) { this(id, inventory, new SimpleContainer(1)); }
    public AEEnchantingTableMenu(int id, Inventory inventory, Container table) {
        super(ModMenus.AE_ENCHANTING_TABLE.get(), id);
        checkContainerSize(table, 1);
        this.table = table;
        catalog = EnchantingRules.catalog(inventory.player.level());
        data = table instanceof AEEnchantingTableBlockEntity entity ? new ContainerData() {
            @Override public int getCount() { return HEADER + catalog.size(); }
            @Override public int get(int index) {
                if (index == 0) return (int) entity.getAECurrentPower() & 65535;
                if (index == 1) return (int) entity.getAECurrentPower() >>> 16;
                if (index == 2) return entity.hasChannel() ? 1 : 0;
                if (index == 3) return entity.isWorking() ? 1 : 0;
                if (index >= 4 && index < 8) return part(entity.paidEnergy(), index - 4);
                if (index >= 8 && index < 12) return part(entity.energyCost(catalog), index - 8);
                return index >= HEADER && index < getCount() ? entity.targetLevel(catalog.get(index - HEADER)) : 0;
            }
            @Override public void set(int index, int value) { }
        } : new SimpleContainerData(HEADER + catalog.size());
        addDataSlots(data);
        addSlot(new Slot(table, 0, 8, 38) {
            @Override public int getMaxStackSize() { return 1; }
            @Override public int getMaxStackSize(ItemStack stack) { return 1; }
            @Override public boolean mayPlace(ItemStack stack) { return !isWorking() && EnchantingRules.accepts(stack) && table.canPlaceItem(0, stack); }
            @Override public boolean mayPickup(Player player) { return !isWorking() && super.mayPickup(player); }
        });
        for (int row = 0; row < 3; row++) for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, 88 + row * 18));
        }
        for (int column = 0; column < 9; column++) addSlot(new Slot(inventory, column, 8 + column * 18, 146));
        table.startOpen(inventory.player);
    }
    private static int part(long value, int index) { return (int) (value >>> (index * 16)) & 65535; }
    private long value(int start) {
        long value = 0;
        for (int i = 0; i < 4; i++) value |= ((long) data.get(start + i) & 65535) << (i * 16);
        return Math.max(0, value);
    }
    public ItemStack input() { return table.getItem(0); }
    public List<Holder<Enchantment>> availableEnchantments() {
        return catalog.stream().filter(enchantment -> EnchantingRules.applicable(input(), enchantment)).toList();
    }
    public int targetLevel(Holder<Enchantment> enchantment) {
        int index = catalog.indexOf(enchantment);
        return index >= 0 ? data.get(HEADER + index) : 0;
    }
    public int existingLevel(Holder<Enchantment> enchantment) { return EnchantingRules.existingLevel(input(), enchantment); }
    public int maxLevel(Holder<Enchantment> enchantment) { return EnchantingRules.maxLevel(enchantment); }
    private Map<ResourceLocation, Integer> selections() {
        Map<ResourceLocation, Integer> selected = new LinkedHashMap<>();
        for (Holder<Enchantment> enchantment : catalog) {
            int target = targetLevel(enchantment);
            if (target > 0) selected.put(EnchantingRules.id(enchantment), target);
        }
        return selected;
    }
    public boolean isBlocked(Holder<Enchantment> enchantment) {
        return EnchantingRules.blocked(input(), enchantment, selections(), catalog);
    }
    public boolean isWorking() { return data.get(3) != 0; }
    public boolean isWorkValid() {
        return !isWorking() || EnchantingRules.validTargets(input(), selections(), catalog)
                && EnchantingRules.cost(input(), selections(), catalog) == energyCost();
    }
    public boolean hasChannel() { return data.get(2) != 0; }
    public long energyCost() { return value(8); }
    public long paidEnergy() { return value(4); }
    public int storedEnergy() { return (data.get(0) & 65535) | ((data.get(1) & 65535) << 16); }
    public boolean canConfirm() { return !isWorking() && hasChannel() && energyCost() > 0 && !input().isEmpty(); }
    public int actionId(Holder<Enchantment> enchantment, int action) {
        int index = catalog.indexOf(enchantment);
        return index < 0 || action < SELECT || action > DESELECT ? -1 : ACTION_BASE + index * ACTION_COUNT + action;
    }
    @Override public boolean clickMenuButton(Player player, int button) {
        if (!(table instanceof AEEnchantingTableBlockEntity entity) || player.level().isClientSide
                || player.isSpectator() || player.containerMenu != this || !stillValid(player)) return false;
        boolean changed;
        if (button == CONFIRM) changed = entity.confirm();
        else {
            int encoded = button - ACTION_BASE;
            if (encoded < 0 || encoded / ACTION_COUNT >= catalog.size()) return false;
            changed = entity.changeTarget(catalog.get(encoded / ACTION_COUNT), encoded % ACTION_COUNT, catalog);
        }
        if (changed) broadcastChanges();
        return changed;
    }
    @Override public boolean stillValid(Player player) { return table.stillValid(player); }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem() || !slot.mayPickup(player)) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem(), original = stack.copy();
        if (index == 0) {
            if (!moveItemStackTo(stack, 1, 37, true)) return ItemStack.EMPTY;
        } else {
            if (slots.getFirst().mayPlace(stack)) moveItemStackTo(stack, 0, 1, false);
            if (stack.getCount() == original.getCount()) {
                if (index < 28) { if (!moveItemStackTo(stack, 28, 37, false)) return ItemStack.EMPTY; }
                else if (!moveItemStackTo(stack, 1, 28, false)) return ItemStack.EMPTY;
            }
        }
        if (stack.getCount() == original.getCount()) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        slot.onTake(player, stack);
        return original;
    }
    @Override public void clicked(int slot, int button, ClickType type, Player player) {
        if (slot == 0 && isWorking()) return;
        super.clicked(slot, button, type, player);
    }
    @Override public boolean canDragTo(Slot slot) { return !(slot.index == 0 && isWorking()) && super.canDragTo(slot); }
    @Override public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return !(slot.index == 0 && isWorking()) && super.canTakeItemForPickAll(stack, slot);
    }
    @Override public void removed(Player player) { super.removed(player); table.stopOpen(player); }
}
