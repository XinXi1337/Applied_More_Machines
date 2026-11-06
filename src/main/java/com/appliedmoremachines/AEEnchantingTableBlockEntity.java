package com.appliedmoremachines;

import appeng.api.config.AccessRestriction;
import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.networking.GridFlags;
import appeng.api.networking.GridHelper;
import appeng.api.networking.IManagedGridNode;
import appeng.api.networking.energy.IAEPowerStorage;
import appeng.me.helpers.BlockEntityNodeListener;
import appeng.me.helpers.IGridConnectedBlockEntity;
import com.appliedmoremachines.enchanting.EnchantingEnergyStorage;
import com.appliedmoremachines.enchanting.EnchantingRules;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.EnchantingTableBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.IEnergyStorage;

/** Single manual slot, private AE buffer and persistent, channel-gated enchantment payments. */
public final class AEEnchantingTableBlockEntity extends EnchantingTableBlockEntity
        implements WorldlyContainer, MenuProvider, IGridConnectedBlockEntity, IAEPowerStorage {
    private NonNullList<ItemStack> items = NonNullList.withSize(1, ItemStack.EMPTY);
    private final Map<ResourceLocation, Integer> targets = new LinkedHashMap<>();
    private ItemStack selectionInput = ItemStack.EMPTY;
    private ItemStack pendingInput = ItemStack.EMPTY;
    private ItemStack pendingResult = ItemStack.EMPTY;
    private boolean working;
    private long totalCost;
    private double paid;
    private final EnchantingEnergyStorage energy = new EnchantingEnergyStorage(this::setChanged);
    private final IEnergyStorage forgeEnergy = new InfusionForgeEnergyStorage(energy);
    private final IManagedGridNode mainNode = GridHelper.createManagedNode(this, BlockEntityNodeListener.INSTANCE)
            .setInWorldNode(true).setFlags(GridFlags.REQUIRE_CHANNEL).setTagName("MainNode")
            .setIdlePowerUsage(0).setVisualRepresentation(ModBlocks.AE_ENCHANTING_TABLE.get())
            .addService(IAEPowerStorage.class, this);

    public AEEnchantingTableBlockEntity(BlockPos pos, BlockState state) { super(pos, state); }
    @Override public BlockEntityType<?> getType() { return ModBlockEntities.AE_ENCHANTING_TABLE.get(); }
    @Override public Component getName() {
        Component name = getCustomName();
        return name != null ? name : Component.translatable("block.applied_more_machines.ae_enchanting_table");
    }
    @Override public Component getDisplayName() { return getName(); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new AEEnchantingTableMenu(id, inventory, this);
    }
    @Override public int[] getSlotsForFace(Direction side) { return new int[0]; }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) { return false; }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) { return false; }
    @Override public int getContainerSize() { return 1; }
    @Override public int getMaxStackSize() { return 1; }
    @Override public boolean isEmpty() { return items.getFirst().isEmpty(); }
    @Override public ItemStack getItem(int slot) { return slot == 0 ? items.getFirst() : ItemStack.EMPTY; }
    @Override public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == 0 && !working && EnchantingRules.accepts(stack);
    }
    @Override public ItemStack removeItem(int slot, int amount) {
        if (working || slot != 0) return ItemStack.EMPTY;
        ItemStack taken = ContainerHelper.removeItem(items, slot, amount);
        if (!taken.isEmpty()) setChanged();
        return taken;
    }
    @Override public ItemStack removeItemNoUpdate(int slot) {
        if (working || slot != 0) return ItemStack.EMPTY;
        ItemStack taken = ContainerHelper.takeItem(items, slot);
        if (!taken.isEmpty()) setChanged();
        return taken;
    }
    @Override public void setItem(int slot, ItemStack stack) {
        if (working || slot != 0) return;
        items.set(0, stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1));
        setChanged();
    }
    @Override public void clearContent() {
        if (!working) { items.set(0, ItemStack.EMPTY); setChanged(); }
    }
    @Override public boolean stillValid(Player player) { return Container.stillValidBlockEntity(this, player); }
    @Override public void setChanged() {
        if (targets != null && !working && items != null && !same(selectionInput, getItem(0))) {
            targets.clear();
            selectionInput = getItem(0).copy();
        }
        super.setChanged();
    }
    private static boolean same(ItemStack first, ItemStack second) {
        return first.getCount() == second.getCount() && ItemStack.isSameItemSameComponents(first, second);
    }
    public Map<ResourceLocation, Integer> targets() { return Map.copyOf(targets); }
    public int targetLevel(Holder<Enchantment> enchantment) { return targets.getOrDefault(EnchantingRules.id(enchantment), 0); }
    public boolean isWorking() { return working; }
    public boolean hasChannel() { return mainNode.isActive(); }
    public long paidEnergy() { return Math.max(0, (long) Math.floor(paid)); }
    public long energyCost(List<Holder<Enchantment>> catalog) {
        return working ? totalCost : EnchantingRules.cost(getItem(0), targets, catalog);
    }
    public boolean changeTarget(Holder<Enchantment> enchantment, int action, List<Holder<Enchantment>> catalog) {
        if (working || !EnchantingRules.applicable(getItem(0), enchantment)) return false;
        int existing = EnchantingRules.existingLevel(getItem(0), enchantment);
        int maximum = EnchantingRules.maxLevel(enchantment);
        ResourceLocation key = EnchantingRules.id(enchantment);
        int current = targets.getOrDefault(key, 0);
        if (current == 0 && (existing >= maximum || EnchantingRules.blocked(getItem(0), enchantment, targets, catalog))) return false;
        int minimum = existing + 1;
        int next = switch (action) {
            case AEEnchantingTableMenu.SELECT -> current > 0 ? current : minimum;
            case AEEnchantingTableMenu.DESELECT -> 0;
            case AEEnchantingTableMenu.DECREASE -> current > minimum ? current - 1 : 0;
            case AEEnchantingTableMenu.INCREASE -> current == 0 ? minimum : Math.min(maximum, current + 1);
            case AEEnchantingTableMenu.MINIMUM -> current > minimum ? minimum : 0;
            case AEEnchantingTableMenu.MAXIMUM -> maximum;
            default -> -1;
        };
        if (next < 0 || next == current || next > maximum) return false;
        if (next == 0) targets.remove(key); else targets.put(key, next);
        setChanged();
        return true;
    }
    public boolean confirm() {
        if (level == null || level.isClientSide || working || !hasChannel()) return false;
        List<Holder<Enchantment>> catalog = EnchantingRules.catalog(level);
        long cost = EnchantingRules.cost(getItem(0), targets, catalog);
        if (cost <= 0) return false;
        ItemStack result = EnchantingRules.result(getItem(0), targets, catalog);
        if (result.isEmpty()) return false;
        pendingInput = getItem(0).copy();
        pendingResult = result.copy();
        totalCost = cost;
        paid = 0;
        working = true;
        setChanged();
        payFromBuffer();
        return true;
    }
    private void payFromBuffer() {
        if (!working || !hasChannel() || !same(pendingInput, getItem(0))) return;
        List<Holder<Enchantment>> catalog = EnchantingRules.catalog(level);
        // A datapack may change limits or compatibility while this paid job is saved.
        // Pause without discarding its ledger; never apply the item hook a second time.
        if (!EnchantingRules.validTargets(getItem(0), targets, catalog)
                || EnchantingRules.cost(getItem(0), targets, catalog) != totalCost) return;
        double taken = energy.extractAEPower(Math.max(0, totalCost - paid), Actionable.MODULATE, PowerMultiplier.ONE);
        paid += taken;
        if (paid >= totalCost) {
            items.set(0, pendingResult.copy());
            working = false;
            pendingInput = ItemStack.EMPTY;
            pendingResult = ItemStack.EMPTY;
            paid = 0;
            totalCost = 0;
            targets.clear();
            selectionInput = getItem(0).copy();
        }
        if (taken > 0 || !working) setChanged();
    }
    public void serverTick() {
        if (level == null || level.isClientSide || !hasChannel()) return;
        double demand = getAEMaxPower() - getAECurrentPower();
        if (demand > 0) mainNode.ifPresent(grid -> injectAEPower(grid.getEnergyService()
                .extractAEPower(demand, Actionable.MODULATE, PowerMultiplier.ONE), Actionable.MODULATE));
        payFromBuffer();
    }
    @Override public IManagedGridNode getMainNode() { return mainNode; }
    @Override public void saveChanges() { setChanged(); }
    public void setOwner(Player player) { mainNode.setOwningPlayer(player); }
    public IEnergyStorage getForgeEnergyStorage() { return forgeEnergy; }
    @Override public void clearRemoved() {
        super.clearRemoved();
        GridHelper.onFirstTick(this, table -> {
            if (!table.isRemoved() && table.level != null && !table.mainNode.isReady()) table.mainNode.create(table.level, table.worldPosition);
        });
    }
    @Override public void setRemoved() { mainNode.destroy(); super.setRemoved(); }
    @Override public void onChunkUnloaded() { mainNode.destroy(); super.onChunkUnloaded(); }
    @Override public double injectAEPower(double amount, Actionable mode) { return energy.injectAEPower(amount, mode); }
    @Override public double extractAEPower(double amount, Actionable mode, PowerMultiplier multiplier) { return energy.extractAEPower(amount, mode, multiplier); }
    @Override public double getAECurrentPower() { return energy.getAECurrentPower(); }
    @Override public double getAEMaxPower() { return energy.getAEMaxPower(); }
    @Override public boolean isAEPublicPowerStorage() { return false; }
    @Override public AccessRestriction getPowerFlow() { return energy.getPowerFlow(); }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, registries);
        tag.putDouble("StoredAE", getAECurrentPower());
        CompoundTag selected = new CompoundTag();
        targets.forEach((key, value) -> selected.putInt(key.toString(), value));
        tag.put("SelectedEnchantments", selected);
        if (working) {
            CompoundTag work = new CompoundTag();
            work.putLong("Cost", totalCost);
            work.putDouble("Paid", paid);
            work.put("Input", pendingInput.save(registries));
            work.put("Result", pendingResult.save(registries));
            tag.put("EnchantingWork", work);
        }
        mainNode.saveToNBT(tag);
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items = NonNullList.withSize(1, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items, registries);
        energy.load(tag.getDouble("StoredAE"));
        targets.clear();
        CompoundTag selected = tag.getCompound("SelectedEnchantments");
        for (String name : selected.getAllKeys()) {
            ResourceLocation key = ResourceLocation.tryParse(name);
            int value = selected.getInt(name);
            if (key != null && value > 0 && value <= 255) targets.put(key, value);
        }
        selectionInput = getItem(0).copy();
        CompoundTag work = tag.getCompound("EnchantingWork");
        pendingInput = ItemStack.parseOptional(registries, work.getCompound("Input"));
        pendingResult = ItemStack.parseOptional(registries, work.getCompound("Result"));
        totalCost = Math.max(0, work.getLong("Cost"));
        paid = work.getDouble("Paid");
        if (!Double.isFinite(paid)) paid = 0;
        paid = Math.clamp(paid, 0, totalCost);
        working = totalCost > 0 && !pendingResult.isEmpty() && pendingResult.getCount() == 1 && same(pendingInput, getItem(0));
        if (!working) { totalCost = 0; paid = 0; pendingInput = ItemStack.EMPTY; pendingResult = ItemStack.EMPTY; }
        mainNode.loadFromNBT(tag);
    }
}
