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
import com.appliedmoremachines.fishing.FishingLoot;
import com.appliedmoremachines.fishing.FishingProcessor;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

public final class AEFishBlockEntity extends BaseContainerBlockEntity implements IGridConnectedBlockEntity, IAEPowerStorage, WorldlyContainer {
    public static final int OUTPUT_COUNT = 27, ROD_SLOT = 27, UPGRADE_START = 28, UPGRADE_COUNT = 4, INVENTORY_SIZE = 32;
    private NonNullList<ItemStack> items = NonNullList.withSize(INVENTORY_SIZE, ItemStack.EMPTY);
    private boolean autoExport, blockUnstackable;
    private int outputSides, mode;
    private final FishingProcessor processing = new FishingProcessor();
    private final EnchantingEnergyStorage energy = new EnchantingEnergyStorage(this::setChanged);
    private final IEnergyStorage forgeEnergy = new InfusionForgeEnergyStorage(energy);
    private final IManagedGridNode mainNode = GridHelper.createManagedNode(this, BlockEntityNodeListener.INSTANCE)
            .setInWorldNode(true).setFlags(GridFlags.REQUIRE_CHANNEL).setTagName("MainNode").setIdlePowerUsage(0)
            .setVisualRepresentation(ModBlocks.AE_FISH.get()).addService(IAEPowerStorage.class, this);
    private final IItemHandler automation = new IItemHandler() {
        @Override public int getSlots() { return OUTPUT_COUNT; }
        @Override public ItemStack getStackInSlot(int slot) { validate(slot); return getItem(slot); }
        @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) { validate(slot); return stack; }
        @Override public ItemStack extractItem(int slot, int amount, boolean simulate) {
            validate(slot);
            ItemStack stack = getItem(slot);
            if (amount <= 0 || stack.isEmpty()) return ItemStack.EMPTY;
            int count = Math.min(Math.min(amount, stack.getCount()), stack.getMaxStackSize());
            return simulate ? stack.copyWithCount(count) : removeItem(slot, count);
        }
        @Override public int getSlotLimit(int slot) { validate(slot); return 64; }
        @Override public boolean isItemValid(int slot, ItemStack stack) { validate(slot); return false; }
        private void validate(int slot) { if (slot < 0 || slot >= OUTPUT_COUNT) throw new IndexOutOfBoundsException(slot); }
    };
    private final ContainerData settings = new ContainerData() {
        @Override public int get(int index) {
            return switch (index) {
                case 0 -> autoExport ? 1 : 0;
                case 1 -> outputSides;
                case 2 -> processing.ticks();
                case 3 -> processing.duration();
                case 4 -> mode;
                case 5 -> blockUnstackable ? 1 : 0;
                default -> 0;
            };
        }
        @Override public void set(int index, int value) {
            switch (index) {
                case 0 -> autoExport = value != 0;
                case 1 -> outputSides = value & InfusionOutputSide.ALL_SIDES;
                case 4 -> mode = Math.clamp(value, 0, 3);
                case 5 -> blockUnstackable = value != 0;
                default -> { return; }
            }
            setChanged();
        }
        @Override public int getCount() { return AEFishMenu.SETTINGS_COUNT; }
    };

    public AEFishBlockEntity(BlockPos pos, BlockState state) { super(ModBlockEntities.AE_FISH.get(), pos, state); }
    @Override public int getContainerSize() { return INVENTORY_SIZE; }
    public static boolean acceptsRod(ItemStack stack) { return stack.is(Items.FISHING_ROD); }
    @Override public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == ROD_SLOT && acceptsRod(stack) || slot >= UPGRADE_START && slot < INVENTORY_SIZE && InfusionUpgradeCards.accepts(stack);
    }
    @Override public void setItem(int slot, ItemStack stack) {
        stack.limitSize(slot >= ROD_SLOT ? 1 : stack.getMaxStackSize());
        items.set(slot, stack); setChanged();
    }
    @Override public void clearContent() { super.clearContent(); setChanged(); }
    @Override protected NonNullList<ItemStack> getItems() { return items; }
    @Override protected void setItems(NonNullList<ItemStack> value) { items = value; }
    @Override protected Component getDefaultName() { return Component.translatable("block.applied_more_machines.ae_fish"); }
    @Override protected AbstractContainerMenu createMenu(int id, Inventory inventory) { return new AEFishMenu(id, inventory, this); }
    public ContainerData getSettings() { return settings; }
    public IItemHandler getAutomationInventory() { return automation; }
    @Override public int[] getSlotsForFace(Direction face) { return java.util.stream.IntStream.range(0, OUTPUT_COUNT).toArray(); }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction face) { return false; }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction face) { return slot >= 0 && slot < OUTPUT_COUNT; }
    public IEnergyStorage getForgeEnergyStorage() { return forgeEnergy; }
    public List<ItemStack> paidDrops() { return processing.paidDrops(); }
    @Override public IManagedGridNode getMainNode() { return mainNode; }
    @Override public void saveChanges() { setChanged(); }
    public void setOwner(Player player) { mainNode.setOwningPlayer(player); }
    @Override public void clearRemoved() {
        super.clearRemoved();
        GridHelper.onFirstTick(this, machine -> {
            if (!machine.isRemoved() && machine.level != null && !machine.mainNode.isReady()) machine.mainNode.create(machine.level, machine.worldPosition);
        });
    }
    @Override public void setRemoved() { mainNode.destroy(); super.setRemoved(); }
    @Override public void onChunkUnloaded() { mainNode.destroy(); super.onChunkUnloaded(); }

    public void serverTick() {
        if (!(level instanceof ServerLevel server)) return;
        if (mainNode.isActive()) {
            double demand = getAEMaxPower() - getAECurrentPower();
            if (demand > 0) mainNode.ifPresent(grid -> injectAEPower(grid.getEnergyService().extractAEPower(
                    demand, Actionable.MODULATE, PowerMultiplier.ONE), Actionable.MODULATE));
        }
        int speed = 0, overload = 0;
        for (int slot = UPGRADE_START; slot < INVENTORY_SIZE; slot++) {
            if (InfusionUpgradeCards.isOverload(getItem(slot))) overload++;
            else if (InfusionUpgradeCards.accepts(getItem(slot))) speed++;
        }
        int selection = mode | (blockUnstackable ? FishingLoot.BLOCK_UNSTACKABLE : 0);
        processing.tick(this, energy, mainNode.isActive(), acceptsRod(getItem(ROD_SLOT)), speed, overload, selection,
                selected -> FishingLoot.roll(server, worldPosition, selected), this::setChanged);
        if (autoExport && outputSides != 0) {
            var front = getBlockState().getValue(AEFishBlock.FACING);
            for (var side : InfusionOutputSide.values()) {
                if ((outputSides & side.mask()) == 0) continue;
                var direction = side.worldDirection(front);
                var pos = worldPosition.relative(direction);
                var target = level.hasChunkAt(pos) ? level.getCapability(Capabilities.ItemHandler.BLOCK, pos, direction.getOpposite()) : null;
                if (target == null) continue;
                for (int slot = 0; slot < OUTPUT_COUNT; slot++) {
                    ItemStack stack = getItem(slot);
                    if (stack.isEmpty()) continue;
                    ItemStack rest = ItemHandlerHelper.insertItemStacked(target, stack.copy(), false);
                    if (rest.getCount() < stack.getCount()) setItem(slot, rest);
                }
            }
        }
    }

    public int installUpgradeCards(ItemStack held) {
        if (level == null || level.isClientSide || !InfusionUpgradeCards.accepts(held)) return 0;
        int installed = 0;
        for (int slot = UPGRADE_START; slot < INVENTORY_SIZE && installed < held.getCount(); slot++) {
            if (getItem(slot).isEmpty()) { setItem(slot, held.copyWithCount(1)); installed++; }
        }
        return installed;
    }
    @Override public double injectAEPower(double value, Actionable action) { return energy.injectAEPower(value, action); }
    @Override public double extractAEPower(double value, Actionable action, PowerMultiplier multiplier) { return energy.extractAEPower(value, action, multiplier); }
    @Override public double getAEMaxPower() { return energy.getAEMaxPower(); }
    @Override public double getAECurrentPower() { return energy.getAECurrentPower(); }
    @Override public boolean isAEPublicPowerStorage() { return false; }
    @Override public AccessRestriction getPowerFlow() { return energy.getPowerFlow(); }

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, registries);
        tag.putBoolean("AutoExport", autoExport); tag.putInt("OutputSides", outputSides); tag.putInt("FishingMode", mode);
        tag.putBoolean("BlockUnstackable", blockUnstackable);
        tag.putDouble("StoredAE", getAECurrentPower()); tag.put("FishingWork", processing.save(registries)); mainNode.saveToNBT(tag);
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items = NonNullList.withSize(INVENTORY_SIZE, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items, registries);
        autoExport = tag.getBoolean("AutoExport"); outputSides = tag.getInt("OutputSides") & InfusionOutputSide.ALL_SIDES;
        mode = Math.clamp(tag.getInt("FishingMode"), 0, 3);
        blockUnstackable = tag.getBoolean("BlockUnstackable");
        energy.load(tag.getDouble("StoredAE")); processing.load(tag.getCompound("FishingWork"), registries); mainNode.loadFromNBT(tag);
    }
}
