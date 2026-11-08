package com.appliedmoremachines;

import com.appliedmoremachines.production.*;

import appeng.api.config.*;
import appeng.api.networking.*;
import appeng.api.networking.energy.IAEPowerStorage;
import appeng.api.networking.security.IActionSource;
import appeng.me.helpers.*;
import com.appliedmoremachines.dissolution.*;
import com.appliedmoremachines.factory.FactoryEnergy;
import com.appliedmoremachines.latex.LatexUpgrades;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.*;

public final class DissolutionChamberBlockEntity extends BaseContainerBlockEntity
        implements IGridConnectedBlockEntity, IAEPowerStorage, WorldlyContainer, DissolutionProcessor.Host, MultiplicationHost {
    private final MultiplicationUpgrade multiplicationUpgrade = new MultiplicationUpgrade(this);
    @Override public MultiplicationUpgrade multiplicationUpgrade() { return multiplicationUpgrade; }

    public static final int INPUT_COUNT = 8, OUTPUT_START = 8, UPGRADE_START = 11, SIZE = 16;
    private NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
    private FluidStack inputFluid = FluidStack.EMPTY, outputFluid = FluidStack.EMPTY;
    private boolean autoExport;
    private int outputSides;
    private final DissolutionProcessor processing = new DissolutionProcessor();
    private final DissolutionEnergyStorage energy = new DissolutionEnergyStorage(this::setChanged);
    private final IEnergyStorage forgeEnergy = new InfusionForgeEnergyStorage(energy);
    private final IManagedGridNode mainNode = GridHelper.createManagedNode(this, BlockEntityNodeListener.INSTANCE)
            .setInWorldNode(true).setFlags(GridFlags.REQUIRE_CHANNEL).setTagName("MainNode")
            .setIdlePowerUsage(0).setExposedOnSides(EnumSet.allOf(Direction.class))
            .setVisualRepresentation(ModBlocks.AE_DISSOLUTION_CHAMBER_FRONT.get()).addService(IAEPowerStorage.class, this);
    private final ContainerData settings = new ContainerData() {
        @Override public int get(int i) {
            if (i >= 2 && i < 10) {
                FluidStack fluid = i < 6 ? inputFluid : outputFluid;
                int offset = (i - 2) % 4;
                int value = offset < 2 ? BuiltInRegistries.FLUID.getId(fluid.getFluid()) : fluid.getAmount();
                return value >>> (16 * (offset % 2)) & 65535;
            }
            return switch (i) {
                case 0 -> autoExport ? 1 : 0; case 1 -> outputSides;
                case 10 -> processing.ticks(); case 11 -> processing.duration();
                case 12 -> (int) Math.floor(100 * getAECurrentPower() / getAEMaxPower());
                default -> 0;
            };
        }
        @Override public void set(int i, int value) {
            if (i == 0) autoExport = value != 0;
            else if (i == 1) outputSides = value & 63;
            else return;
            setChanged();
        }
        @Override public int getCount() { return DissolutionChamberMenu.DATA_COUNT; }
    };
    public DissolutionChamberBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.AE_DISSOLUTION_CHAMBER_FRONT.get(), pos, state);
    }
    @Override public int getContainerSize() { return SIZE; }
    @Override protected NonNullList<ItemStack> getItems() { return items; }
    @Override protected void setItems(NonNullList<ItemStack> value) { items = value; }
    @Override protected Component getDefaultName() { return Component.translatable("block.applied_more_machines.ae_dissolution_chamber_front"); }
    @Override protected AbstractContainerMenu createMenu(int id, Inventory inventory) { return new DissolutionChamberMenu(id, inventory, this); }
    public ContainerData getSettings() { return settings; }
    @Override public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot >= 0 && slot < INPUT_COUNT ? !stack.isEmpty() : LatexUpgrades.accepts(this, slot, stack, UPGRADE_START);
    }
    @Override public void setItem(int slot, ItemStack stack) { items.set(slot, stack); setChanged(); }
    @Override public void clearContent() { super.clearContent(); setChanged(); }
    public int installUpgradeCards(ItemStack offered) {
        if (level == null || level.isClientSide || LatexUpgrades.kind(offered) < 0) return 0;
        for (int i = UPGRADE_START; i < SIZE; i++) if (LatexUpgrades.accepts(this, i, offered, UPGRADE_START)) {
            ItemStack present = getItem(i);
            if (!present.isEmpty() && !ItemStack.isSameItemSameComponents(present, offered)) continue;
            int count = Math.min(offered.getCount(), Math.min(offered.getMaxStackSize(), LatexUpgrades.limit(offered)) - present.getCount());
            if (count > 0) { setItem(i, offered.copyWithCount(present.getCount() + count)); return count; }
        }
        return 0;
    }
    private boolean outputEnabled(Direction direction) {
        if (direction == null) return outputSides != 0;
        Direction front = getBlockState().getValue(DissolutionChamberBlock.FACING);
        for (var side : InfusionOutputSide.values()) if (side.worldDirection(front) == direction) return (outputSides & side.mask()) != 0;
        return false;
    }
    @Override public int[] getSlotsForFace(Direction side) { return java.util.stream.IntStream.range(0, UPGRADE_START).toArray(); }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) { return slot >= 0 && slot < 8 && canPlaceItem(slot, stack); }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) { return slot >= 8 && slot < 11 && outputEnabled(side); }
    public IItemHandler getAutomationInventory(Direction side) {
        return new IItemHandler() {
            private void check(int slot) { if (slot < 0 || slot >= 11) throw new IndexOutOfBoundsException(slot); }
            @Override public int getSlots() { return 11; }
            @Override public ItemStack getStackInSlot(int slot) { check(slot); return getItem(slot).copy(); }
            @Override public boolean isItemValid(int slot, ItemStack stack) { check(slot); return canPlaceItemThroughFace(slot, stack, side); }
            @Override public int getSlotLimit(int slot) { check(slot); return 64; }
            @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                check(slot); if (stack.isEmpty() || !isItemValid(slot, stack)) return stack;
                ItemStack present = getItem(slot);
                if (!present.isEmpty() && !ItemStack.isSameItemSameComponents(present, stack)) return stack;
                int count = Math.min(stack.getCount(), Math.max(0, Math.min(64, stack.getMaxStackSize()) - present.getCount()));
                if (!simulate && count > 0) setItem(slot, stack.copyWithCount(present.getCount() + count));
                return stack.copyWithCount(stack.getCount() - count);
            }
            @Override public ItemStack extractItem(int slot, int count, boolean simulate) {
                check(slot); if (count <= 0 || !canTakeItemThroughFace(slot, getItem(slot), side)) return ItemStack.EMPTY;
                count = Math.min(count, Math.min(getItem(slot).getCount(), getItem(slot).getMaxStackSize()));
                return simulate ? getItem(slot).copyWithCount(count) : removeItem(slot, count);
            }
        };
    }
    public FluidStack tankContents(int tank) {
        if (tank < 0 || tank > 1) throw new IndexOutOfBoundsException(tank);
        return (tank == 0 ? inputFluid : outputFluid).copy();
    }
    public IFluidHandler getFluidHandler(Direction side) { return fluidHandler(side, -1, false); }
    public IFluidHandler getGuiTank(int tank) {
        if (tank < 0 || tank > 1) throw new IndexOutOfBoundsException(tank);
        return fluidHandler(null, tank, true);
    }
    private IFluidHandler fluidHandler(Direction side, int only, boolean gui) {
        return new IFluidHandler() {
            private int index(int tank) {
                if (tank < 0 || tank >= getTanks()) throw new IndexOutOfBoundsException(tank);
                return only < 0 ? tank : only;
            }
            @Override public int getTanks() { return only < 0 ? 2 : 1; }
            @Override public FluidStack getFluidInTank(int tank) { return tankContents(index(tank)); }
            @Override public int getTankCapacity(int tank) { index(tank); return DissolutionProcessor.CAPACITY; }
            @Override public boolean isFluidValid(int tank, FluidStack fluid) {
                return index(tank) == 0 && !fluid.isEmpty() && (inputFluid.isEmpty() || FluidStack.isSameFluidSameComponents(inputFluid, fluid));
            }
            @Override public int fill(FluidStack fluid, FluidAction action) {
                if (only == 1 || !isFluidValid(0, fluid)) return 0;
                int count = Math.min(fluid.getAmount(), Math.max(0, DissolutionProcessor.CAPACITY - inputFluid.getAmount()));
                if (count > 0 && action.execute()) { inputFluid = fluid.copyWithAmount(inputFluid.getAmount() + count); setChanged(); }
                return count;
            }
            private int drainTank() { return gui ? only : 1; }
            @Override public FluidStack drain(FluidStack fluid, FluidAction action) {
                if (fluid.isEmpty() || !FluidStack.isSameFluidSameComponents(tankContents(drainTank()), fluid)) return FluidStack.EMPTY;
                return drain(fluid.getAmount(), action);
            }
            @Override public FluidStack drain(int max, FluidAction action) {
                int tank = drainTank();
                if (max <= 0 || !gui && !outputEnabled(side)) return FluidStack.EMPTY;
                FluidStack source = tank == 0 ? inputFluid : outputFluid;
                int count = Math.min(max, source.getAmount());
                if (count <= 0) return FluidStack.EMPTY;
                FluidStack result = source.copyWithAmount(count);
                if (action.execute()) { source.shrink(count); setChanged(); }
                return result;
            }
        };
    }
    private void pushAdjacent() {
        if (!autoExport) return;
        Direction front = getBlockState().getValue(DissolutionChamberBlock.FACING);
        for (var side : InfusionOutputSide.values()) {
            Direction direction = side.worldDirection(front); BlockPos pos = worldPosition.relative(direction);
            if (!outputEnabled(direction) || !level.hasChunkAt(pos)) continue;
            var target = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, direction.getOpposite());
            if (target != null) for (int i = 8; i < 11; i++) if (!getItem(i).isEmpty()) {
                ItemStack rest = ItemHandlerHelper.insertItemStacked(target, getItem(i).copy(), false);
                if (rest.getCount() < getItem(i).getCount()) setItem(i, rest);
            }
            var tank = level.getCapability(Capabilities.FluidHandler.BLOCK, pos, direction.getOpposite());
            if (tank != null && !outputFluid.isEmpty()) {
                int count = Math.clamp(tank.fill(outputFluid.copy(), IFluidHandler.FluidAction.EXECUTE), 0, outputFluid.getAmount());
                if (count > 0) { outputFluid.shrink(count); setChanged(); }
            }
        }
    }
    public void serverTick() {
        if (!multiplicationUpgrade.tick()) return;
        if (level == null || level.isClientSide) return;
        if (mainNode.isActive() && getAECurrentPower() < getAEMaxPower()) mainNode.ifPresent(grid ->
                DissolutionCharging.charge(energy, FactoryEnergy.source(grid, IActionSource.ofMachine(this))));
        processing.tick(this, mainNode.isActive(), LatexUpgrades.stats(this, UPGRADE_START), DissolutionRecipes.all(level), level.registryAccess());
        if (level.getGameTime() % 4 == 0) pushAdjacent();
    }
    @Override public List<ItemStack> inputs() { return items.subList(0, 8); }
    @Override public List<ItemStack> outputs() { return items.subList(8, 11); }
    @Override public FluidStack inputFluid() { return inputFluid; }
    @Override public FluidStack outputFluid() { return outputFluid; }
    @Override public boolean powered() { return getAECurrentPower() >= DissolutionProcessor.COST; }
    @Override public void commit(DissolutionProcessor.Plan plan) {
        extractAEPower(DissolutionProcessor.COST, Actionable.MODULATE, PowerMultiplier.ONE);
        for (int i = 0; i < 8; i++) items.get(i).shrink(plan.consumed()[i]);
        inputFluid.shrink(plan.fluidConsumed());
        for (int i = 0; i < 3; i++) items.set(8 + i, plan.outputs().get(i).copy());
        outputFluid = plan.fluidOutput().copy();
        MultiplicationOutputs.retain(this, plan.overflow()); setChanged();
    }
    @Override public void dirty() { setChanged(); }
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
        tag.put("InputFluid", inputFluid.saveOptional(lookup)); tag.put("OutputFluid", outputFluid.saveOptional(lookup));
        tag.putBoolean("AutoExport", autoExport); tag.putInt("OutputSides", outputSides);
        tag.put("DissolutionWork", processing.save(lookup));
        tag.putDouble("StoredAE", getAECurrentPower()); mainNode.saveToNBT(tag);
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
        super.loadAdditional(tag, lookup);
        multiplicationUpgrade.load(tag, lookup); items = NonNullList.withSize(SIZE, ItemStack.EMPTY); ContainerHelper.loadAllItems(tag, items, lookup);
        inputFluid = FluidStack.parseOptional(lookup, tag.getCompound("InputFluid")); outputFluid = FluidStack.parseOptional(lookup, tag.getCompound("OutputFluid"));
        inputFluid.setAmount(Math.min(DissolutionProcessor.CAPACITY, inputFluid.getAmount())); outputFluid.setAmount(Math.min(DissolutionProcessor.CAPACITY, outputFluid.getAmount()));
        autoExport = tag.getBoolean("AutoExport"); outputSides = tag.getInt("OutputSides") & 63;
        processing.load(tag.getCompound("DissolutionWork"), lookup); energy.load(tag.getDouble("StoredAE")); mainNode.loadFromNBT(tag);
    }
}
