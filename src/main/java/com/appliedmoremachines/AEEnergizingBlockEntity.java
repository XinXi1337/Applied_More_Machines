package com.appliedmoremachines;

import com.appliedmoremachines.production.*;

import appeng.api.config.*;
import appeng.api.networking.*;
import appeng.api.networking.energy.IAEPowerStorage;
import appeng.api.networking.security.IActionSource;
import appeng.me.helpers.*;
import com.appliedmoremachines.energizing.*;
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
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.*;

public final class AEEnergizingBlockEntity extends BaseContainerBlockEntity
        implements IGridConnectedBlockEntity, IAEPowerStorage, WorldlyContainer, MultiplicationHost {
    private final MultiplicationUpgrade multiplicationUpgrade = new MultiplicationUpgrade(this);
    @Override public MultiplicationUpgrade multiplicationUpgrade() { return multiplicationUpgrade; }

    public static final int INPUT_COUNT = 9, OUTPUT_START = 25, UPGRADE_START = 26, SIZE = 27;
    private NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
    // Separate visual state: never overwrite the real inventory/energy with a render-only packet.
    private NonNullList<ItemStack> displayedInputs = NonNullList.withSize(INPUT_COUNT, ItemStack.EMPTY);
    private boolean autoExport;
    private int outputSides;
    private final EnergizingProcessor processing = new EnergizingProcessor();
    private final EnergizingEnergy energy = new EnergizingEnergy(() -> PowerUnit.FE.convertTo(PowerUnit.AE, EnergizingRecipes.totals(this).capacity()), this::setChanged);
    private final IEnergyStorage forgeEnergy = new InfusionForgeEnergyStorage(energy);
    private final IManagedGridNode mainNode = GridHelper.createManagedNode(this, BlockEntityNodeListener.INSTANCE)
            .setInWorldNode(true).setFlags(GridFlags.REQUIRE_CHANNEL).setTagName("MainNode")
            .setIdlePowerUsage(0).setExposedOnSides(EnumSet.allOf(Direction.class))
            .setVisualRepresentation(ModBlocks.AE_ENERGIZING_ORB.get()).addService(IAEPowerStorage.class, this);
    private final ContainerData settings = new ContainerData() {
        @Override public int get(int i) { return switch (i) {
            case 0 -> autoExport ? 1 : 0; case 1 -> outputSides;
            case 2 -> processing.progress(); case 3 -> 10000;
            case 4 -> getAEMaxPower() > 0 ? (int) (100 * getAECurrentPower() / getAEMaxPower()) : 0; default -> 0;
        }; }
        @Override public void set(int i, int value) {
            if (i == 0) autoExport = value != 0; else if (i == 1) outputSides = value & 63; else return;
            setChanged();
        }
        @Override public int getCount() { return AEEnergizingMenu.DATA_COUNT; }
    };
    public AEEnergizingBlockEntity(BlockPos pos, BlockState state) { super(ModBlockEntities.AE_ENERGIZING_ORB.get(), pos, state); }
    @Override public int getContainerSize() { return SIZE; }
    @Override protected NonNullList<ItemStack> getItems() { return items; }
    @Override protected void setItems(NonNullList<ItemStack> value) { items = value; }
    @Override protected Component getDefaultName() { return Component.translatable("block.applied_more_machines.ae_energizing_orb"); }
    @Override protected AbstractContainerMenu createMenu(int id, Inventory inventory) { return new AEEnergizingMenu(id, inventory, this); }
    public ContainerData getSettings() { return settings; }
    @Override public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot>=0 && slot<16 ? EnergizingRecipes.rod(stack)
                : slot>=16 && slot<25 ? level!=null && EnergizingRecipes.material(level,stack)
                : slot==26 && EnergizingUpgrades.isCard(stack);
    }
    // Callers plan bounded insertions; preserve saved/legacy stacks rather than silently deleting excess items.
    @Override public void setItem(int slot, ItemStack stack) { items.set(slot, stack); if (slot < 16) energy.clamp(); setChanged(); }
    @Override public void clearContent() { super.clearContent(); energy.clamp(); processing.reset(); setChanged(); }
    public int installUpgradeCards(ItemStack offered) {
        if (level == null || level.isClientSide || !EnergizingUpgrades.isCard(offered)) return 0;
        for (int i = UPGRADE_START; i < SIZE; i++) if (EnergizingUpgrades.accepts(this, i, offered)) {
            ItemStack present = getItem(i);
            if (!present.isEmpty() && !ItemStack.isSameItemSameComponents(present, offered)) continue;
            int count = Math.min(offered.getCount(), Math.max(0, Math.min(4, offered.getMaxStackSize()) - present.getCount()));
            if (count > 0) { setItem(i, offered.copyWithCount(present.getCount() + count)); return count; }
        }
        return 0;
    }
    @Override public int[] getSlotsForFace(Direction side) { return new int[]{16,17,18,19,20,21,22,23,24,25}; }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot >= 16 && slot < 25 && canPlaceItem(slot, stack);
    }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) { return slot == 25; }
    private final IItemHandler automation = new IItemHandler() {
        private int map(int slot){if(slot<0||slot>=10)throw new IndexOutOfBoundsException(slot);return slot+16;}
        public int getSlots(){return 10;}
        public ItemStack getStackInSlot(int slot){return getItem(map(slot)).copy();}
        public boolean isItemValid(int slot,ItemStack stack){return map(slot)<25 && canPlaceItem(map(slot),stack);}
        public int getSlotLimit(int slot){map(slot);return 64;}
        public ItemStack insertItem(int slot,ItemStack stack,boolean simulate){
            int index=map(slot);if(stack.isEmpty()||!isItemValid(slot,stack))return stack;
            ItemStack present=getItem(index);
            if(!present.isEmpty()&&!ItemStack.isSameItemSameComponents(present,stack))return stack;
            int n=Math.min(stack.getCount(),Math.max(0,Math.min(64,stack.getMaxStackSize())-present.getCount()));
            if(n>0&&!simulate)setItem(index,stack.copyWithCount(present.getCount()+n));
            return stack.copyWithCount(stack.getCount()-n);
        }
        public ItemStack extractItem(int slot,int amount,boolean simulate){
            int index=map(slot);if(index!=25||amount<=0)return ItemStack.EMPTY;
            int n=Math.min(amount,Math.min(getItem(index).getCount(),getItem(index).getMaxStackSize()));
            return simulate?getItem(index).copyWithCount(n):removeItem(index,n);
        }
    };
    public IItemHandler getAutomationInventory(){return automation;}
    @Override public ItemStack removeItem(int slot,int count){ItemStack result=super.removeItem(slot,count);if(slot<16)energy.clamp();return result;}
    @Override public ItemStack removeItemNoUpdate(int slot){ItemStack result=super.removeItemNoUpdate(slot);if(slot<16)energy.clamp();return result;}
    private void pushAdjacent() {
        if (!autoExport || outputSides == 0) return;
        Direction front = getBlockState().getValue(AEEnergizingBlock.FACING);
        for (var side : InfusionOutputSide.values()) {
            if ((outputSides & side.mask()) == 0) continue;
            Direction direction = side.worldDirection(front); BlockPos pos = worldPosition.relative(direction);
            if (!level.hasChunkAt(pos)) continue;
            var target = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, direction.getOpposite());
            if (target != null) for (int i = 25; i < 26; i++) if (!getItem(i).isEmpty()) {
                ItemStack rest = ItemHandlerHelper.insertItemStacked(target, getItem(i).copy(), false);
                if (rest.getCount() < getItem(i).getCount()) setItem(i, rest);
            }
        }
    }
    private NonNullList<ItemStack> materialDisplaySnapshot() {
        NonNullList<ItemStack> snapshot = NonNullList.withSize(INPUT_COUNT, ItemStack.EMPTY);
        for (int i = 0; i < INPUT_COUNT; i++) snapshot.set(i, getItem(16 + i).copyWithCount(1));
        return snapshot;
    }

    /** One rendered model per occupied material slot, irrespective of stack count. */
    public List<ItemStack> getDisplayedMaterials() {
        return displayedInputs.stream().filter(stack -> !stack.isEmpty()).map(ItemStack::copy).toList();
    }

    private void syncDisplayedInputs() {
        var snapshot = materialDisplaySnapshot();
        for (int i = 0; i < INPUT_COUNT; i++) {
            if (!ItemStack.matches(snapshot.get(i), displayedInputs.get(i))) {
                displayedInputs = snapshot;
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(),
                        net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
                return;
            }
        }
    }

    @Override public CompoundTag getUpdateTag(HolderLookup.Provider lookup) {
        CompoundTag tag = new CompoundTag();
        CompoundTag display = new CompoundTag();
        ContainerHelper.saveAllItems(display, materialDisplaySnapshot(), lookup);
        tag.put("DisplayedInputs", display);
        return tag;
    }

    @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    @Override public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider lookup) {
        displayedInputs = NonNullList.withSize(INPUT_COUNT, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag.getCompound("DisplayedInputs"), displayedInputs, lookup);
    }

    @Override public void onDataPacket(net.minecraft.network.Connection connection,
            net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket packet, HolderLookup.Provider lookup) {
        handleUpdateTag(packet.getTag(), lookup);
    }

    public void serverTick() {
        if (!multiplicationUpgrade.tick()) return;
        if (level == null || level.isClientSide) return;
        energy.clamp();
        if (mainNode.isActive() && getAECurrentPower() < getAEMaxPower()) mainNode.ifPresent(grid ->
                DissolutionCharging.charge(energy, FactoryEnergy.source(grid, IActionSource.ofMachine(this))));
        processing.tick(this, energy, EnergizingRecipes.all(level), mainNode.isActive(),
                (long)Math.min(Long.MAX_VALUE, EnergizingRecipes.totals(this).rate()*EnergizingUpgrades.multiplier(this)));
        if (level.getGameTime() % 4 == 0) pushAdjacent();
        syncDisplayedInputs();
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
        tag.put("EnergizingWork", processing.save(lookup)); tag.putBoolean("AutoExport", autoExport); tag.putInt("OutputSides", outputSides);
        tag.putDouble("StoredAE", getAECurrentPower()); mainNode.saveToNBT(tag);
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
        super.loadAdditional(tag, lookup);
        multiplicationUpgrade.load(tag, lookup); items = NonNullList.withSize(SIZE, ItemStack.EMPTY); ContainerHelper.loadAllItems(tag, items, lookup);
        processing.load(tag.getCompound("EnergizingWork"), lookup); autoExport = tag.getBoolean("AutoExport"); outputSides = tag.getInt("OutputSides") & 63;
        energy.load(tag.getDouble("StoredAE")); mainNode.loadFromNBT(tag);
    }
}
