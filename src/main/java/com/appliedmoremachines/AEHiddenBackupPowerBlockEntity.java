package com.appliedmoremachines;

import appeng.api.config.*;
import appeng.api.ids.AEComponents;
import appeng.api.networking.*;
import appeng.api.networking.energy.IAEPowerStorage;
import appeng.api.networking.events.GridPowerStorageStateChanged;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEKey;
import appeng.api.storage.*;
import appeng.api.storage.cells.StorageCell;
import appeng.me.helpers.*;
import com.appliedmoremachines.power.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;

/** One hidden FE disk and a 64M AE reservoir, with channel-controlled six-face power export. */
public final class AEHiddenBackupPowerBlockEntity extends BaseContainerBlockEntity
        implements IGridConnectedBlockEntity, IAEPowerStorage, WorldlyContainer, IStorageProvider {
    public static final int FE_PER_FACE = 1_000_000;
    private NonNullList<ItemStack> items = NonNullList.withSize(1, ItemStack.EMPTY);
    private final HiddenPowerEnergy energy = new HiddenPowerEnergy(this::energyChanged);
    private StorageCell disk;
    private ItemStack diskStack = ItemStack.EMPTY;
    private boolean storageRefresh = true, energyRefresh = true;
    private long outputTick = Long.MIN_VALUE;
    private final int[] exported = new int[7];
    private boolean pushing;
    private final IEnergyStorage[] ports = new IEnergyStorage[7];
    private final IManagedGridNode mainNode = GridHelper.createManagedNode(this, BlockEntityNodeListener.INSTANCE)
            .setInWorldNode(true).setFlags(GridFlags.REQUIRE_CHANNEL).setTagName("MainNode")
            .setIdlePowerUsage(0).setExposedOnSides(EnumSet.allOf(Direction.class))
            .setVisualRepresentation(ModBlocks.AE_HIDDEN_BACKUP_POWER.get())
            .addService(IAEPowerStorage.class, this).addService(IStorageProvider.class, this);
    private final MEStorage hiddenInput = new MEStorage() {
        @Override public long insert(AEKey key, long amount, Actionable mode, IActionSource source) {
            if (amount <= 0 || !hasChannel() || !key.equals(HiddenPowerCell.key())) return 0;
            StorageCell current = disk();
            if (current == null) return 0;
            long accepted = Math.clamp(current.insert(key, amount, mode, source), 0, amount);
            if (accepted > 0 && mode == Actionable.MODULATE) { flushDisk(); setChanged(); }
            return accepted;
        }
        // No extract or getAvailableStacks: the disk never appears in terminals or ordinary FE extraction.
        @Override public Component getDescription() { return getDisplayName(); }
    };

    public AEHiddenBackupPowerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.AE_HIDDEN_BACKUP_POWER.get(), pos, state);
        for (int i = 0; i < ports.length; i++) ports[i] = new Port(i);
    }
    public HiddenPowerEnergy energy() { return energy; }
    public boolean hasChannel() {
        var node = mainNode.getNode();
        // Deliberately independent of POWER: an energy source must be able to restart an unpowered grid.
        return node != null && node.meetsChannelRequirements();
    }
    @Override public IManagedGridNode getMainNode() { return mainNode; }
    @Override public void saveChanges() { setChanged(); }
    @Override public void onMainNodeStateChanged(IGridNodeListener.State reason) {
        storageRefresh = true; energyRefresh = true;
    }
    private void energyChanged() { energyRefresh = true; setChanged(); }
    public void setOwner(Player player) { mainNode.setOwningPlayer(player); }
    @Override public void mountInventories(IStorageMounts mounts) {
        if (hasChannel()) mounts.mount(hiddenInput, Integer.MIN_VALUE);
    }

    private StorageCell disk() {
        ItemStack current = items.getFirst();
        if (current != diskStack) {
            flushDisk(); diskStack = current;
            disk = current.isEmpty() ? null : StorageCells.getCellInventory(current, this::setChanged);
        }
        return disk;
    }
    public void flushDisk() { if (disk != null) disk.persist(); }
    public long extractDisk(long amount, Actionable mode) {
        StorageCell current = disk();
        if (current == null || amount <= 0) return 0;
        long taken = Math.clamp(current.extract(HiddenPowerCell.key(), amount, mode, IActionSource.ofMachine(this)), 0, amount);
        if (taken > 0 && mode == Actionable.MODULATE) { flushDisk(); setChanged(); }
        return taken;
    }
    @Override public int getContainerSize() { return 1; }
    @Override public int getMaxStackSize() { return 1; }
    @Override protected NonNullList<ItemStack> getItems() { flushDisk(); return items; }
    @Override protected void setItems(NonNullList<ItemStack> value) {
        flushDisk(); disk = null; diskStack = ItemStack.EMPTY; items = value; storageRefresh = true;
    }
    @Override public ItemStack getItem(int index) { flushDisk(); return items.get(index); }
    @Override public void setItem(int index, ItemStack stack) {
        flushDisk(); disk = null; diskStack = ItemStack.EMPTY;
        items.set(index, stack); storageRefresh = true; setChanged();
    }
    @Override public boolean canPlaceItem(int slot, ItemStack stack) { return slot == 0 && HiddenPowerCell.accepts(stack); }
    @Override public ItemStack removeItem(int slot, int count) {
        flushDisk(); ItemStack result = ContainerHelper.removeItem(items, slot, count);
        disk = null; diskStack = ItemStack.EMPTY; storageRefresh = true; setChanged(); return result;
    }
    @Override public ItemStack removeItemNoUpdate(int slot) {
        flushDisk(); ItemStack result = ContainerHelper.takeItem(items, slot);
        disk = null; diskStack = ItemStack.EMPTY; storageRefresh = true; setChanged(); return result;
    }
    @Override public void clearContent() { setItem(0, ItemStack.EMPTY); }
    @Override public int[] getSlotsForFace(Direction side) { return new int[0]; }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) { return false; }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) { return false; }
    @Override protected Component getDefaultName() {
        return Component.translatable("block.applied_more_machines.ae_hidden_backup_power");
    }
    @Override protected AbstractContainerMenu createMenu(int id, Inventory inventory) {
        return new AEHiddenBackupPowerMenu(id, inventory, this);
    }

    public void serverTick() {
        if (level == null || level.isClientSide) return;
        if (storageRefresh) { storageRefresh = false; IStorageProvider.requestUpdate(mainNode); }
        if (energyRefresh) {
            energyRefresh = false;
            // AE2 removes full requesters. Re-arm on the next tick, never mutate its set during insertion.
            mainNode.ifPresent(grid -> grid.postEvent(new GridPowerStorageStateChanged(this,
                    GridPowerStorageStateChanged.PowerEventType.RECEIVE_POWER)));
        }
        if (hasChannel()) charge();
        else pushAdjacent();
    }
    private void charge() {
        var node = mainNode.getNode();
        if (node == null) return;
        double room = HiddenPowerEnergy.CAPACITY - energy.stored();
        if (room <= 0) return;
        var grid = node.getGrid();
        var draw = energy.draw(Actionable.MODULATE, PowerUnit.FE.convertTo(PowerUnit.AE, 1));
        double fromNetwork = HiddenPowerNetwork.bypass(() -> draw.take(room,
                n -> grid.getStorageService().getInventory().extract(HiddenPowerCell.key(), n,
                        Actionable.MODULATE, IActionSource.ofMachine(this))));
        energy.insert(fromNetwork, Actionable.MODULATE);
        double fromDisk = draw.take(HiddenPowerEnergy.CAPACITY - energy.stored(),
                n -> extractDisk(n, Actionable.MODULATE));
        energy.insert(fromDisk, Actionable.MODULATE);
        // AE charges through the public WRITE-only storage service. Do not drain other batteries into this one.
    }

    private void beginOutputTick() {
        long now = level == null ? 0 : level.getGameTime();
        if (now != outputTick) { outputTick = now; Arrays.fill(exported, 0); }
    }
    private int availableFE(int request) {
        double ratio = PowerUnit.FE.convertTo(PowerUnit.AE, 1);
        if (request <= 0 || !Double.isFinite(ratio) || ratio <= 0) return 0;
        long diskAmount = extractDisk(request, Actionable.SIMULATE);
        long aeAmount = (long) Math.floor((energy.stored() + energy.remainder()) / ratio);
        return (int) Math.min(request, diskAmount + Math.min(request, aeAmount));
    }
    private int takeFE(int request, Actionable mode) {
        double ratio = PowerUnit.FE.convertTo(PowerUnit.AE, 1);
        int available = availableFE(request);
        if (mode == Actionable.SIMULATE) return available;
        long fromDisk = extractDisk(available, mode);
        int missing = (int) Math.min(available - fromDisk,
                Math.floor((energy.stored() + energy.remainder()) / ratio));
        // Conversion remainder was already paid for; keep it alongside the disk's first-priority energy.
        var draw = energy.draw(mode, ratio);
        double fromRemainder = draw.take(missing * ratio, n -> 0);
        double fromBuffer = energy.extract(missing * ratio - fromRemainder, mode);
        return (int) fromDisk + (int) Math.round((fromRemainder + fromBuffer) / ratio);
    }
    private void pushAdjacent() {
        beginOutputTick();
        if (pushing) return;
        pushing = true;
        try {
            for (Direction side : Direction.values()) {
                if (hasChannel()) break;
                BlockPos targetPos = worldPosition.relative(side);
                if (!level.hasChunkAt(targetPos)) continue;
                var target = level.getCapability(Capabilities.EnergyStorage.BLOCK, targetPos, side.getOpposite());
                if (target == null || !target.canReceive()) continue;
                int port = side.ordinal(), budget = FE_PER_FACE - exported[port];
                int offer = availableFE(budget);
                int planned = Math.clamp(target.receiveEnergy(offer, true), 0, offer);
                if (planned <= 0) continue;
                // Reserve before the callback so a receiver cannot re-enter and spend this energy twice.
                int taken = takeFE(planned, Actionable.MODULATE);
                exported[port] += taken;
                int accepted = 0;
                try { accepted = Math.clamp(target.receiveEnergy(taken, false), 0, taken); }
                finally {
                    int refund = taken - accepted;
                    exported[port] -= refund;
                    if (refund > 0) refundFE(refund);
                }
            }
        } finally { pushing = false; }
    }
    private void refundFE(int amount) {
        // Restore real storage first. Normal receiver rejection must not build an oversized AE cache.
        StorageCell current = disk();
        long restored = current == null ? 0 : Math.clamp(current.insert(HiddenPowerCell.key(), amount,
                Actionable.MODULATE, IActionSource.ofMachine(this)), 0, amount);
        if (restored > 0) flushDisk();
        double left = energy.insert(PowerUnit.FE.convertTo(PowerUnit.AE, amount - restored), Actionable.MODULATE);
        // Only a concurrent disk change / receiver callback filling the buffer can leave a refund pending.
        if (left > 0) energy.load(energy.stored(), energy.remainder() + left);
        energyChanged();
    }
    public IEnergyStorage getForgeEnergyStorage(Direction side) { return ports[side == null ? 6 : side.ordinal()]; }
    private final class Port implements IEnergyStorage {
        private final int face;
        private Port(int face) { this.face = face; }
        @Override public int receiveEnergy(int offered, boolean simulate) {
            double ratio = PowerUnit.FE.convertTo(PowerUnit.AE, 1);
            if (offered <= 0 || !Double.isFinite(ratio) || ratio <= 0) return 0;
            int accepted = (int) Math.min(offered, Math.floor((HiddenPowerEnergy.CAPACITY - energy.stored()) / ratio));
            if (!simulate && accepted > 0) energy.insert(accepted * ratio, Actionable.MODULATE);
            return accepted;
        }
        @Override public int extractEnergy(int amount, boolean simulate) {
            if (hasChannel() || pushing || amount <= 0) return 0;
            beginOutputTick();
            // Null is input-only: it must not create a seventh independent output face.
            if (face == 6) return 0;
            int taken = takeFE(Math.min(amount, FE_PER_FACE - exported[face]),
                    simulate ? Actionable.SIMULATE : Actionable.MODULATE);
            if (!simulate) exported[face] += taken;
            return taken;
        }
        @Override public int getEnergyStored() { return availableFE(Integer.MAX_VALUE); }
        @Override public int getMaxEnergyStored() {
            return Math.max(getEnergyStored(), (int) Math.min(Integer.MAX_VALUE,
                    PowerUnit.AE.convertTo(PowerUnit.FE, HiddenPowerEnergy.CAPACITY)));
        }
        @Override public boolean canExtract() { return face != 6 && !hasChannel(); }
        @Override public boolean canReceive() { return true; }
    }
    @Override public double injectAEPower(double amount, Actionable mode) {
        return hasChannel() ? energy.insert(amount, mode) : amount;
    }
    @Override public double extractAEPower(double amount, Actionable mode, PowerMultiplier multiplier) { return 0; }
    @Override public double getAECurrentPower() { return energy.stored(); }
    @Override public double getAEMaxPower() { return HiddenPowerEnergy.CAPACITY; }
    @Override public boolean isAEPublicPowerStorage() { return true; }
    @Override public AccessRestriction getPowerFlow() { return AccessRestriction.WRITE; }
    @Override public int getPriority() { return Integer.MIN_VALUE; }

    public void restoreItemEnergy(ItemStack stack) {
        var data = stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
        energy.load(stack.getOrDefault(AEComponents.STORED_ENERGY, 0.0), data.getDouble("HiddenPowerRemainderAE"));
        energyChanged();
    }
    public void storeItemEnergy(ItemStack stack) {
        stack.set(AEComponents.STORED_ENERGY, energy.stored());
        if (energy.remainder() > 0) net.minecraft.world.item.component.CustomData.update(
                net.minecraft.core.component.DataComponents.CUSTOM_DATA, stack,
                tag -> tag.putDouble("HiddenPowerRemainderAE", energy.remainder()));
    }
    @Override public void clearRemoved() {
        super.clearRemoved();
        GridHelper.onFirstTick(this, host -> {
            if (!host.isRemoved() && host.level != null && !host.level.isClientSide && !host.mainNode.isReady())
                host.mainNode.create(host.level, host.worldPosition);
        });
    }
    @Override public void setRemoved() { flushDisk(); mainNode.destroy(); super.setRemoved(); }
    @Override public void onChunkUnloaded() { flushDisk(); mainNode.destroy(); super.onChunkUnloaded(); }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
        super.saveAdditional(tag, lookup); flushDisk(); ContainerHelper.saveAllItems(tag, items, lookup);
        tag.putDouble("StoredAE", energy.stored()); tag.putDouble("RemainderAE", energy.remainder()); mainNode.saveToNBT(tag);
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
        super.loadAdditional(tag, lookup); disk = null; diskStack = ItemStack.EMPTY;
        items = NonNullList.withSize(1, ItemStack.EMPTY); ContainerHelper.loadAllItems(tag, items, lookup);
        energy.load(tag.getDouble("StoredAE"), tag.getDouble("RemainderAE")); mainNode.loadFromNBT(tag);
        storageRefresh = true; energyRefresh = true;
    }
}
