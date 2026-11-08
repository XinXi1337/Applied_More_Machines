package com.appliedmoremachines;

import com.appliedmoremachines.production.*;

import appeng.api.config.*;
import appeng.api.networking.*;
import appeng.api.networking.energy.IAEPowerStorage;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.*;
import appeng.api.storage.MEStorage;
import appeng.me.helpers.BlockEntityNodeListener;
import appeng.me.helpers.IGridConnectedBlockEntity;
import com.appliedmoremachines.latex.*;
import java.util.EnumSet;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.*;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.*;

public final class LatexExtractionProcessorBlockEntity extends BaseContainerBlockEntity
        implements IGridConnectedBlockEntity, IAEPowerStorage, WorldlyContainer, LatexProcessor.Host, MultiplicationHost {
    private final MultiplicationUpgrade multiplicationUpgrade = new MultiplicationUpgrade(this);
    @Override public MultiplicationUpgrade multiplicationUpgrade() { return multiplicationUpgrade; }

    public static final int SIZE = 7, UPGRADE_START = 2, UPGRADE_COUNT = 5;
    public static final int LOG_INPUT = 0, WATER_INPUT = 1, LATEX_INPUT = 2, LATEX_OUTPUT = 3, RUBBER_OUTPUT = 4;
    private NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
    private int latex, water, mode;
    private boolean autoExport;
    private final int[] sides = {63, 63, 63, 0, 0};
    private final LatexProcessor processing = new LatexProcessor();
    private final InfusionEnergyStorage energy = new InfusionEnergyStorage(this::setChanged);
    private final IEnergyStorage forgeEnergy = new InfusionForgeEnergyStorage(energy);
    private final IManagedGridNode mainNode = GridHelper.createManagedNode(this, BlockEntityNodeListener.INSTANCE)
            .setInWorldNode(true).setExposedOnSides(EnumSet.allOf(Direction.class))
            .setFlags(GridFlags.REQUIRE_CHANNEL).setTagName("MainNode").setIdlePowerUsage(0)
            .setVisualRepresentation(ModBlocks.AE_LATEX_EXTRACTION_PROCESSOR.get()).addService(IAEPowerStorage.class, this);
    private final ContainerData settings = new ContainerData() {
        @Override public int get(int i) {
            return switch (i) {
                case 0 -> autoExport ? 1 : 0;
                case 1 -> mode;
                case 2 -> latex;
                case 3 -> water;
                case 4 -> processing.extractionTicks();
                case 5 -> processing.extractionDuration();
                case 6 -> processing.rubberTicks();
                case 7 -> processing.rubberDuration();
                case 8 -> (int) Math.floor(100 * getAECurrentPower() / getAEMaxPower());
                case 9, 10, 11, 12, 13 -> sides[i - 9];
                case 14 -> processing.locksLogs() ? 1 : 0;
                default -> 0;
            };
        }
        @Override public void set(int i, int value) {
            if (i == 0) autoExport = value != 0;
            else if (i == 1) mode = Math.clamp(value, 0, 3);
            else if (i >= 9 && i < 12) sides[i - 9] = 63;
            else if (i == 12 || i == 13) sides[LATEX_OUTPUT] = sides[RUBBER_OUTPUT] = value & 63;
            else return;
            setChanged();
        }
        @Override public int getCount() { return LatexExtractionProcessorMenu.DATA_COUNT; }
    };
    public LatexExtractionProcessorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.AE_LATEX_EXTRACTION_PROCESSOR.get(), pos, state);
    }
    @Override public int getContainerSize() { return SIZE; }
    @Override protected NonNullList<ItemStack> getItems() { return items; }
    @Override protected void setItems(NonNullList<ItemStack> value) { items = value; }
    @Override protected Component getDefaultName() { return Component.translatable("block.applied_more_machines.ae_latex_extraction_processor"); }
    @Override protected AbstractContainerMenu createMenu(int id, Inventory inventory) { return new LatexExtractionProcessorMenu(id, inventory, this); }
    public ContainerData getSettings() { return settings; }
    @Override public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == 0 ? stack.is(ItemTags.LOGS) && (!processing.locksLogs()
                || ItemStack.isSameItemSameComponents(input(), stack)) : LatexUpgrades.accepts(this, slot, stack);
    }
    @Override public void setItem(int slot, ItemStack stack) {
        // Vanilla menu limits insertions; do not truncate legacy or command-created stacks on extraction.
        items.set(slot, stack); setChanged();
    }
    @Override public void clearContent() { super.clearContent(); setChanged(); }
    public boolean locksLogs() { return processing.locksLogs(); }
    public int installUpgradeCards(ItemStack offered) {
        if (level == null || level.isClientSide || LatexUpgrades.kind(offered) < 0) return 0;
        for (int i = 2; i < SIZE; i++) if (LatexUpgrades.accepts(this, i, offered)) {
            ItemStack present = getItem(i);
            if (!present.isEmpty() && !ItemStack.isSameItemSameComponents(present, offered)) continue;
            int count = Math.min(offered.getCount(), Math.min(offered.getMaxStackSize(), LatexUpgrades.limit(offered)) - present.getCount());
            if (count > 0) { setItem(i, offered.copyWithCount(present.getCount() + count)); return count; }
        }
        return 0;
    }
    private boolean enabled(int type, Direction direction) {
        if (direction == null) return sides[type] != 0;
        Direction front = getBlockState().getValue(LatexExtractionProcessorBlock.FACING);
        for (var side : InfusionOutputSide.values())
            if (side.worldDirection(front) == direction) return (sides[type] & side.mask()) != 0;
        return false;
    }
    @Override public int[] getSlotsForFace(Direction side) { return new int[]{0, 1}; }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == 0 && enabled(LOG_INPUT, side) && canPlaceItem(0, stack);
    }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == 1 && enabled(RUBBER_OUTPUT, side);
    }
    public IItemHandler getAutomationInventory(Direction side) {
        return new IItemHandler() {
            private void check(int slot) { if (slot < 0 || slot > 1) throw new IndexOutOfBoundsException(slot); }
            @Override public int getSlots() { return 2; }
            @Override public ItemStack getStackInSlot(int slot) { check(slot); return getItem(slot).copy(); }
            @Override public boolean isItemValid(int slot, ItemStack stack) { check(slot); return canPlaceItemThroughFace(slot, stack, side); }
            @Override public int getSlotLimit(int slot) { check(slot); return 64; }
            @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                check(slot);
                if (stack.isEmpty() || !isItemValid(slot, stack)) return stack;
                ItemStack present = getItem(slot);
                if (!present.isEmpty() && !ItemStack.isSameItemSameComponents(present, stack)) return stack;
                int count = Math.min(stack.getCount(), Math.max(0, Math.min(64, stack.getMaxStackSize()) - present.getCount()));
                if (!simulate && count > 0) setItem(slot, stack.copyWithCount(present.getCount() + count));
                return stack.copyWithCount(stack.getCount() - count);
            }
            @Override public ItemStack extractItem(int slot, int count, boolean simulate) {
                check(slot);
                if (count <= 0 || !canTakeItemThroughFace(slot, getItem(slot), side)) return ItemStack.EMPTY;
                count = Math.min(count, Math.min(getItem(slot).getCount(), getItem(slot).getMaxStackSize()));
                return simulate ? getItem(slot).copyWithCount(count) : removeItem(slot, count);
            }
        };
    }
    public static Fluid latexFluid() { return BuiltInRegistries.FLUID.getOptional(LatexRecipes.LATEX).orElse(Fluids.EMPTY); }
    public FluidStack tankContents(int tank) {
        int amount = tank == 0 ? latex : water;
        Fluid fluid = tank == 0 ? latexFluid() : Fluids.WATER;
        return amount == 0 || fluid == Fluids.EMPTY ? FluidStack.EMPTY : new FluidStack(fluid, amount);
    }
    public IFluidHandler getFluidHandler(Direction side) { return fluidHandler(side, -1, false); }
    public IFluidHandler getGuiTank(int tank) { return fluidHandler(null, tank, true); }
    private IFluidHandler fluidHandler(Direction side, int only, boolean gui) {
        return new IFluidHandler() {
            private int index(int index) {
                if (index < 0 || index >= getTanks()) throw new IndexOutOfBoundsException(index);
                return only < 0 ? index : only;
            }
            @Override public int getTanks() { return only < 0 ? 2 : 1; }
            @Override public FluidStack getFluidInTank(int tank) { return tankContents(index(tank)); }
            @Override public int getTankCapacity(int tank) { index(tank); return 16000; }
            @Override public boolean isFluidValid(int tank, FluidStack stack) {
                int i = index(tank);
                return !stack.isEmpty() && stack.is(i == 0 ? latexFluid() : Fluids.WATER)
                        && (gui || enabled(i == 0 ? LATEX_INPUT : WATER_INPUT, side));
            }
            @Override public int fill(FluidStack stack, FluidAction action) {
                for (int tank = 0; tank < getTanks(); tank++) if (isFluidValid(tank, stack)) {
                    int i = index(tank), count = Math.min(stack.getAmount(), 16000 - (i == 0 ? latex : water));
                    if (count > 0 && action.execute()) { if (i == 0) latex += count; else water += count; setChanged(); }
                    return count;
                }
                return 0;
            }
            @Override public FluidStack drain(FluidStack stack, FluidAction action) {
                if (stack.isEmpty() || !stack.is(latexFluid())) return FluidStack.EMPTY;
                return drain(stack.getAmount(), action);
            }
            @Override public FluidStack drain(int max, FluidAction action) {
                // Water is an input; reserved rubber ingredients cannot be removed by buckets or pipes.
                if (max <= 0 || only == 1 || mode == LatexProcessor.RUBBER
                        || !(gui || enabled(LATEX_OUTPUT, side))) return FluidStack.EMPTY;
                int count = Math.min(max, Math.max(0, latex - processing.reservedLatex()));
                if (count == 0 || latexFluid() == Fluids.EMPTY) return FluidStack.EMPTY;
                if (action.execute()) { latex -= count; setChanged(); }
                return new FluidStack(latexFluid(), count);
            }
        };
    }
    private MEStorage network() {
        var grid = mainNode.getGrid();
        return mainNode.isActive() && grid != null ? grid.getStorageService().getInventory() : null;
    }
    @Override public long sendLatex(long count, boolean simulate) {
        MEStorage storage = network();
        if (storage == null || count <= 0 || latexFluid() == Fluids.EMPTY) return 0;
        return storage.insert(AEFluidKey.of(latexFluid()), count, simulate ? Actionable.SIMULATE : Actionable.MODULATE, IActionSource.ofMachine(this));
    }
    private void pullInputs() {
        MEStorage storage = network(); if (storage == null) return;
        if (water < 16000) {
            int count = (int) storage.extract(AEFluidKey.of(Fluids.WATER), 16000 - water, Actionable.MODULATE, IActionSource.ofMachine(this));
            if (count > 0) { water += count; setChanged(); }
        }
        ItemStack present = input();
        int space = (present.isEmpty() ? 64 : Math.min(64, present.getMaxStackSize())) - present.getCount();
        if (space <= 0) return;
        AEItemKey best = LatexNetwork.bestLog(storage, LatexRecipes.all(level), present);
        if (best != null) {
            int count = (int) storage.extract(best, Math.min(space, best.getMaxStackSize()), Actionable.MODULATE, IActionSource.ofMachine(this));
            if (count > 0) setItem(0, best.toStack(present.getCount() + count));
        }
    }
    private void exportToNetwork() {
        MEStorage storage = network(); if (storage == null) return;
        if (mode == LatexProcessor.PASSIVE && !output().isEmpty()) {
            int count = (int) storage.insert(AEItemKey.of(output()), output().getCount(), Actionable.MODULATE, IActionSource.ofMachine(this));
            if (count > 0) removeItem(1, count);
        }
        if (mode == LatexProcessor.LATEX) {
            int available = Math.max(0, latex - processing.reservedLatex());
            int count = (int) sendLatex(available, false);
            if (count > 0) { latex -= count; setChanged(); }
        }
    }
    private void pushAdjacent() {
        if (!autoExport) return;
        Direction front = getBlockState().getValue(LatexExtractionProcessorBlock.FACING);
        for (var side : InfusionOutputSide.values()) {
            Direction direction = side.worldDirection(front); BlockPos pos = worldPosition.relative(direction);
            if (!level.hasChunkAt(pos)) continue;
            if (enabled(RUBBER_OUTPUT, direction) && !output().isEmpty()) {
                var target = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, direction.getOpposite());
                if (target != null) {
                    ItemStack rest = ItemHandlerHelper.insertItemStacked(target, output().copy(), false);
                    if (rest.getCount() < output().getCount()) setItem(1, rest);
                }
            }
            if (mode != LatexProcessor.RUBBER && enabled(LATEX_OUTPUT, direction)) {
                var source = getFluidHandler(direction);
                FluidStack offered = source.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.SIMULATE);
                var target = level.getCapability(Capabilities.FluidHandler.BLOCK, pos, direction.getOpposite());
                if (target != null && !offered.isEmpty()) {
                    int count = target.fill(offered, IFluidHandler.FluidAction.EXECUTE);
                    if (count > 0) source.drain(count, IFluidHandler.FluidAction.EXECUTE);
                }
            }
        }
    }
    public void serverTick() {
        if (!multiplicationUpgrade.tick()) return;
        if (level == null || level.isClientSide) return;
        if (mainNode.isActive()) {
            double demand = getAEMaxPower() - getAECurrentPower();
            if (demand > 0) mainNode.ifPresent(grid -> injectAEPower(grid.getEnergyService()
                    .extractAEPower(demand, Actionable.MODULATE, PowerMultiplier.ONE), Actionable.MODULATE));
            if (mode == LatexProcessor.PASSIVE && level.getGameTime() % 20 == 0) pullInputs();
            exportToNetwork();
        }
        processing.tick(this, mainNode.isActive(), LatexUpgrades.stats(this),
                LatexRecipes.find(LatexRecipes.all(level), input()), LatexRecipes.rubber());
        if (mainNode.isActive()) exportToNetwork();
        if (level.getGameTime() % 4 == 0) pushAdjacent();
    }
    @Override public ItemStack input() { return getItem(0); }
    @Override public ItemStack output() { return getItem(1); }
    @Override public int latex() { return latex; }
    @Override public int water() { return water; }
    @Override public int mode() { return mode; }
    @Override public boolean powered() { return getAECurrentPower() >= LatexProcessor.COST; }
    @Override public void pay() { extractAEPower(LatexProcessor.COST, Actionable.MODULATE, PowerMultiplier.ONE); }
    @Override public void consumeLogs(int count) { removeItem(0, count); }
    @Override public void consumeFluids(int l, int w) { latex -= l; water -= w; setChanged(); }
    @Override public void addLatex(int count) { latex += count; if (count > 0) setChanged(); }
    @Override public void addRubber(ItemStack result) {
        MultiplicationOutputs.deliver(this, new int[]{1}, java.util.List.of(result));
    }
    @Override public void dirty() { setChanged(); }
    @Override public IManagedGridNode getMainNode() { return mainNode; }
    @Override public void saveChanges() { setChanged(); }
    @Override public void setOwner(Player player) { mainNode.setOwningPlayer(player); }
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
    @Override public double injectAEPower(double amount, Actionable action) { return energy.injectAEPower(amount, action); }
    @Override public double extractAEPower(double amount, Actionable action, PowerMultiplier multiplier) { return energy.extractAEPower(amount, action, multiplier); }
    @Override public double getAECurrentPower() { return energy.getAECurrentPower(); }
    @Override public double getAEMaxPower() { return energy.getAEMaxPower(); }
    @Override public boolean isAEPublicPowerStorage() { return false; }
    @Override public AccessRestriction getPowerFlow() { return energy.getPowerFlow(); }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
        super.saveAdditional(tag, lookup);
        multiplicationUpgrade.save(tag, lookup); ContainerHelper.saveAllItems(tag, items, lookup);
        tag.putInt("Latex", latex); tag.putInt("Water", water); tag.putInt("LatexMode", mode);
        tag.putBoolean("AutoExport", autoExport); tag.putIntArray("TransferSides", sides);
        tag.put("LatexWork", processing.save(lookup)); tag.putDouble("StoredAE", getAECurrentPower()); mainNode.saveToNBT(tag);
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
        super.loadAdditional(tag, lookup);
        multiplicationUpgrade.load(tag, lookup); items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items, lookup);
        latex = Math.clamp(tag.getInt("Latex"), 0, 16000); water = Math.clamp(tag.getInt("Water"), 0, 16000);
        mode = Math.clamp(tag.getInt("LatexMode"), 0, 3); autoExport = tag.getBoolean("AutoExport");
        int[] saved = tag.getIntArray("TransferSides");
        // Migrate the old per-resource page to the template's single six-face output setting.
        for (int i = 0; i < 3; i++) sides[i] = 63;
        int outputSides = saved.length == 5 ? (saved[LATEX_OUTPUT] | saved[RUBBER_OUTPUT]) & 63 : 0;
        sides[LATEX_OUTPUT] = sides[RUBBER_OUTPUT] = outputSides;
        processing.load(tag.getCompound("LatexWork"), lookup); energy.load(tag.getDouble("StoredAE")); mainNode.loadFromNBT(tag);
    }
}
