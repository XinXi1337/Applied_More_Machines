package com.appliedmoremachines;

import com.appliedmoremachines.production.*;

import appeng.api.config.*;
import appeng.api.stacks.AEItemKey;
import appeng.api.networking.*;
import appeng.api.networking.energy.IAEPowerStorage;
import appeng.api.networking.security.IActionSource;
import appeng.me.helpers.*;
import com.appliedmoremachines.brewing.*;
import com.appliedmoremachines.dissolution.*;
import com.appliedmoremachines.factory.FactoryEnergy;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.*;

public final class AEBrewingBlockEntity extends BaseContainerBlockEntity
        implements IGridConnectedBlockEntity, IAEPowerStorage, WorldlyContainer, MultiplicationHost {
    private final MultiplicationUpgrade multiplicationUpgrade = new MultiplicationUpgrade(this);
    @Override public MultiplicationUpgrade multiplicationUpgrade() { return multiplicationUpgrade; }

    public static final int INPUT_COUNT = 4, OUTPUT_START = 4, UPGRADE_START = 7, SIZE = 9, WATER_CAPACITY = 8000;
    private NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
    private boolean autoExport;
    private int outputSides;
    private final BrewingProcessor processing = new BrewingProcessor();
    private final DissolutionEnergyStorage energy = new DissolutionEnergyStorage(this::setChanged);
    private final IEnergyStorage forgeEnergy = new InfusionForgeEnergyStorage(energy);
    private final IManagedGridNode mainNode = GridHelper.createManagedNode(this, BlockEntityNodeListener.INSTANCE)
            .setInWorldNode(true).setFlags(GridFlags.REQUIRE_CHANNEL).setTagName("MainNode")
            .setIdlePowerUsage(0).setExposedOnSides(EnumSet.allOf(Direction.class))
            .setVisualRepresentation(ModBlocks.AE_BREWING.get()).addService(IAEPowerStorage.class, this);
    private final ContainerData settings = new ContainerData() {
        @Override public int get(int i) { return switch (i) {
            case 0 -> autoExport ? 1 : 0; case 1 -> outputSides;
            case 2 -> processing.ticks(); case 3 -> processing.duration();
            case 4 -> (int) (100 * getAECurrentPower() / getAEMaxPower()); default -> 0;
        }; }
        @Override public void set(int i, int value) {
            if (i == 0) autoExport = value != 0; else if (i == 1) outputSides = value & 63; else return;
            setChanged();
        }
        @Override public int getCount() { return AEBrewingMenu.DATA_COUNT; }
    };
    public AEBrewingBlockEntity(BlockPos pos, BlockState state) { super(ModBlockEntities.AE_BREWING.get(), pos, state); }
    @Override public int getContainerSize() { return SIZE; }
    @Override protected NonNullList<ItemStack> getItems() { return items; }
    @Override protected void setItems(NonNullList<ItemStack> value) { items = value; }
    @Override protected Component getDefaultName() { return Component.translatable("block.applied_more_machines.ae_brewing"); }
    @Override protected AbstractContainerMenu createMenu(int id, Inventory inventory) { return new AEBrewingMenu(id, inventory, this); }
    public ContainerData getSettings() { return settings; }
    @Override public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot >= UPGRADE_START ? BrewingUpgrades.accepts(this, slot, stack)
                : level != null && BrewingRecipes.accepts(level.potionBrewing(), slot, stack);
    }
    // Callers plan bounded insertions; preserve saved/legacy stacks rather than silently deleting excess items.
    @Override public void setItem(int slot, ItemStack stack) { items.set(slot, stack); setChanged(); }
    @Override public void clearContent() { super.clearContent(); processing.reset(); processing.takePendingReturn(); setChanged(); }
    public int installUpgradeCards(ItemStack offered) {
        if (level == null || level.isClientSide || !BrewingUpgrades.isCard(offered)) return 0;
        for (int i = UPGRADE_START; i < SIZE; i++) if (BrewingUpgrades.accepts(this, i, offered)) {
            ItemStack present = getItem(i);
            if (!present.isEmpty() && !ItemStack.isSameItemSameComponents(present, offered)) continue;
            int count = Math.min(offered.getCount(), Math.max(0, Math.min(4, offered.getMaxStackSize()) - present.getCount()));
            if (count > 0) { setItem(i, offered.copyWithCount(present.getCount() + count)); return count; }
        }
        return 0;
    }
    @Override public int[] getSlotsForFace(Direction side) { return new int[]{0,1,2,3,4,5,6}; }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot >= 0 && slot < 4 && stack.getCount() == 1 && getItem(slot).isEmpty() && canPlaceItem(slot, stack);
    }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) { return slot >= 4 && slot < 7; }
    private final IItemHandler automation = new IItemHandler() {
        private void check(int slot) { if (slot < 0 || slot >= 7) throw new IndexOutOfBoundsException(slot); }
        @Override public int getSlots() { return 7; }
        @Override public ItemStack getStackInSlot(int slot) { check(slot); return getItem(slot).copy(); }
        @Override public boolean isItemValid(int slot, ItemStack stack) { check(slot); return slot < 4 && canPlaceItem(slot, stack); }
        @Override public int getSlotLimit(int slot) { check(slot); return slot < 4 ? 1 : 64; }
        @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            check(slot); if (stack.isEmpty() || !isItemValid(slot, stack) || !getItem(slot).isEmpty()) return stack;
            if (!simulate) setItem(slot, stack.copyWithCount(1));
            return stack.copyWithCount(stack.getCount() - 1);
        }
        @Override public ItemStack extractItem(int slot, int count, boolean simulate) {
            check(slot); if (count <= 0 || !canTakeItemThroughFace(slot, getItem(slot), null)) return ItemStack.EMPTY;
            count = Math.min(count, Math.min(getItem(slot).getCount(), getItem(slot).getMaxStackSize()));
            return simulate ? getItem(slot).copyWithCount(count) : removeItem(slot, count);
        }
    };
    public IItemHandler getAutomationInventory() { return automation; }
    /** This is an immutable 8 B water reservoir, not a source for external fluid networks. */
    private final IFluidHandler waterDisplay = new IFluidHandler() {
        private void check(int tank) { if (tank != 0) throw new IndexOutOfBoundsException(tank); }
        @Override public int getTanks() { return 1; }
        @Override public FluidStack getFluidInTank(int tank) { check(tank); return new FluidStack(Fluids.WATER, WATER_CAPACITY); }
        @Override public int getTankCapacity(int tank) { check(tank); return WATER_CAPACITY; }
        @Override public boolean isFluidValid(int tank, FluidStack stack) { check(tank); return false; }
        @Override public int fill(FluidStack stack, FluidAction action) { return 0; }
        @Override public FluidStack drain(FluidStack stack, FluidAction action) { return FluidStack.EMPTY; }
        @Override public FluidStack drain(int amount, FluidAction action) { return FluidStack.EMPTY; }
    };
    public IFluidHandler getWaterDisplay() { return waterDisplay; }
    private void pushAdjacent() {
        if (!autoExport || outputSides == 0) return;
        Direction front = getBlockState().getValue(AEBrewingBlock.FACING);
        for (var side : InfusionOutputSide.values()) {
            if ((outputSides & side.mask()) == 0) continue;
            Direction direction = side.worldDirection(front); BlockPos pos = worldPosition.relative(direction);
            if (!level.hasChunkAt(pos)) continue;
            var target = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, direction.getOpposite());
            if (target != null) for (int i = 4; i < 7; i++) if (!getItem(i).isEmpty()) {
                ItemStack rest = ItemHandlerHelper.insertItemStacked(target, getItem(i).copy(), false);
                if (rest.getCount() < getItem(i).getCount()) setItem(i, rest);
            }
        }
    }
    public ItemStack takePendingReturn() {
        ItemStack result = processing.takePendingReturn();
        if (!result.isEmpty()) setChanged();
        return result;
    }
    private long insertReturn(ItemStack stack, Actionable action) {
        if (stack.isEmpty() || !mainNode.isActive()) return 0;
        long[] accepted = {0};
        mainNode.ifPresent(grid -> accepted[0] = grid.getStorageService().getInventory().insert(
                AEItemKey.of(stack), stack.getCount(), action, IActionSource.ofMachine(this)));
        return accepted[0];
    }
    public void serverTick() {
        if (!multiplicationUpgrade.tick()) return;
        if (level == null || level.isClientSide) return;
        if (mainNode.isActive() && getAECurrentPower() < getAEMaxPower()) mainNode.ifPresent(grid ->
                DissolutionCharging.charge(energy, FactoryEnergy.source(grid, IActionSource.ofMachine(this))));
        processing.tick(this, energy, level.potionBrewing(), mainNode.isActive(), BrewingUpgrades.duration(this), this::insertReturn);
        if (level.getGameTime() % 4 == 0) pushAdjacent();
    }
    @Override public IManagedGridNode getMainNode() { return mainNode; }
    @Override public void saveChanges() { setChanged(); }
    public void setOwner(Player player) { mainNode.setOwningPlayer(player); }
    public IEnergyStorage getForgeEnergyStorage() { return forgeEnergy; }
    @Override public void clearRemoved() {
        super.clearRemoved();
        GridHelper.onFirstTick(this, machine -> {
            if (!machine.isRemoved() && machine.level != null && !machine.level.isClientSide && !machine.mainNode.isReady())
                machine.mainNode.create(machine.level, machine.worldPosition);
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
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
        super.saveAdditional(tag, lookup);
        multiplicationUpgrade.save(tag, lookup); ContainerHelper.saveAllItems(tag, items, lookup);
        tag.put("BrewingWork", processing.save(lookup)); tag.putBoolean("AutoExport", autoExport); tag.putInt("OutputSides", outputSides);
        tag.putDouble("StoredAE", getAECurrentPower()); mainNode.saveToNBT(tag);
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
        super.loadAdditional(tag, lookup);
        multiplicationUpgrade.load(tag, lookup); items = NonNullList.withSize(SIZE, ItemStack.EMPTY); ContainerHelper.loadAllItems(tag, items, lookup);
        processing.load(tag.getCompound("BrewingWork"), lookup); autoExport = tag.getBoolean("AutoExport"); outputSides = tag.getInt("OutputSides") & 63;
        energy.load(tag.getDouble("StoredAE")); mainNode.loadFromNBT(tag);
    }
}
