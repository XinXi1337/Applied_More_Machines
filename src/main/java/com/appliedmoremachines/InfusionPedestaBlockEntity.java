package com.appliedmoremachines;

import com.appliedmoremachines.production.*;

import appeng.api.config.AccessRestriction;
import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.networking.GridHelper;
import appeng.api.networking.GridFlags;
import appeng.api.networking.IManagedGridNode;
import appeng.api.networking.energy.IAEPowerStorage;
import appeng.me.helpers.BlockEntityNodeListener;
import appeng.me.helpers.IGridConnectedBlockEntity;
import com.appliedmoremachines.processing.InfusionProcessor;
import com.appliedmoremachines.processing.InfusionTiming;
import com.appliedmoremachines.processing.SeedRecipes;
import com.appliedmoremachines.processing.AltarInputs;
import com.appliedmoremachines.automation.AltarAutoExport;
import com.appliedmoremachines.automation.AltarItemHandler;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class InfusionPedestaBlockEntity extends BaseContainerBlockEntity
        implements IGridConnectedBlockEntity, IAEPowerStorage, MultiplicationHost {
    private final MultiplicationUpgrade multiplicationUpgrade = new MultiplicationUpgrade(this);
    @Override public MultiplicationUpgrade multiplicationUpgrade() { return multiplicationUpgrade; }

    public static final int INPUT_COUNT = AltarInputs.COUNT;
    public static final int OUTPUT_SLOT = 3;
    public static final int UPGRADE_START = OUTPUT_SLOT + 1;
    public static final int UPGRADE_COUNT = 4;
    public static final int INVENTORY_SIZE = 14;

    private NonNullList<ItemStack> items = NonNullList.withSize(INVENTORY_SIZE, ItemStack.EMPTY);
    private boolean autoExport;
    private int outputSides;
    private final IItemHandler automation = new AltarItemHandler(this);
    private final InfusionProcessor processing = new InfusionProcessor();
    private final InfusionEnergyStorage energy = new InfusionEnergyStorage(this::setChanged);
    private final IEnergyStorage forgeEnergy = new InfusionForgeEnergyStorage(energy);
    private final IManagedGridNode mainNode = GridHelper.createManagedNode(this, BlockEntityNodeListener.INSTANCE)
            .setInWorldNode(true)
            .setFlags(GridFlags.REQUIRE_CHANNEL)
            .setTagName("MainNode")
            .setIdlePowerUsage(0)
            .setVisualRepresentation(ModBlocks.AE_INFUSION_PEDESTA.get())
            .addService(IAEPowerStorage.class, this);
    private final ContainerData outputSettings = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> autoExport ? 1 : 0;
                case 1 -> outputSides;
                case 2 -> processing.ticks();
                case 3 -> processing.duration();
                default -> throw new IndexOutOfBoundsException(index);
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> autoExport = value != 0;
                case 1 -> outputSides = value & InfusionOutputSide.ALL_SIDES;
                default -> throw new IndexOutOfBoundsException(index);
            }
            setChanged();
        }

        @Override
        public int getCount() {
            return InfusionPedestaMenu.SETTINGS_COUNT;
        }
    };

    public InfusionPedestaBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.AE_INFUSION_PEDESTA.get(), pos, state);
    }

    @Override
    public int getContainerSize() {
        return INVENTORY_SIZE;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return AltarInputs.isInput(slot) && AltarInputs.accepts(level, stack)
                || slot >= UPGRADE_START && slot < UPGRADE_START + UPGRADE_COUNT && InfusionUpgradeCards.accepts(stack);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        // Recipe remainders and synchronized existing stacks must also survive a data-pack reload.
        int limit = AltarInputs.isInput(slot) ? AltarInputs.storageLimit(items.get(slot), stack)
                : slot >= UPGRADE_START && slot < UPGRADE_START + UPGRADE_COUNT ? 1 : stack.getMaxStackSize();
        stack.limitSize(limit);
        items.set(slot, stack);
        setChanged();
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        return super.removeItem(slot, Math.min(amount, getItem(slot).getMaxStackSize()));
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return removeItem(slot, getItem(slot).getMaxStackSize());
    }

    /** Installs into free upgrade slots only and returns the number actually installed. */
    public int installUpgradeCards(ItemStack heldStack) {
        if (level == null || level.isClientSide || !InfusionUpgradeCards.accepts(heldStack)) {
            return 0;
        }
        int installed = 0;
        for (int slot = UPGRADE_START; slot < UPGRADE_START + UPGRADE_COUNT && installed < heldStack.getCount(); slot++) {
            if (getItem(slot).isEmpty()) {
                setItem(slot, heldStack.copyWithCount(1));
                installed++;
            }
        }
        if (installed > 0) {
            setChanged();
        }
        return installed;
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }

    @Override
    public void clearContent() {
        super.clearContent();
        setChanged();
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("block.applied_more_machines.ae_infusion_pedesta");
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory playerInventory) {
        return new InfusionPedestaMenu(containerId, playerInventory, this);
    }

    public ContainerData getOutputSettings() {
        return outputSettings;
    }

    public IItemHandler getAutomationInventory() {
        return automation;
    }

    public IEnergyStorage getForgeEnergyStorage() {
        return forgeEnergy;
    }

    @Override
    public IManagedGridNode getMainNode() {
        return mainNode;
    }

    @Override
    public void saveChanges() {
        setChanged();
    }

    @Override
    public void clearRemoved() {
        super.clearRemoved();
        GridHelper.onFirstTick(this, altar -> {
            if (!altar.isRemoved() && altar.level != null && !altar.mainNode.isReady()) {
                altar.mainNode.create(altar.level, altar.worldPosition);
            }
        });
    }

    @Override
    public void setRemoved() {
        mainNode.destroy();
        super.setRemoved();
    }

    @Override
    public void onChunkUnloaded() {
        mainNode.destroy();
        super.onChunkUnloaded();
    }

    /** Refill from the real grid, then process at most one recipe per server tick. */
    public void serverTick() {
        if (!multiplicationUpgrade.tick()) return;
        double demand = getAEMaxPower() - getAECurrentPower();
        if (demand > 0 && mainNode.isActive()) {
            mainNode.ifPresent(grid -> {
                double received = grid.getEnergyService().extractAEPower(
                        demand, Actionable.MODULATE, PowerMultiplier.ONE);
                injectAEPower(received, Actionable.MODULATE);
            });
        }
        if (level != null && !level.isClientSide) {
            int speed = 0;
            int overload = 0;
            for (int slot = UPGRADE_START; slot < UPGRADE_START + UPGRADE_COUNT; slot++) {
                ItemStack card = getItem(slot);
                if (InfusionUpgradeCards.isOverload(card)) {
                    overload++;
                } else if (InfusionUpgradeCards.accepts(card)) {
                    speed++;
                }
            }
            processing.tick(this, energy, SeedRecipes.get(level), recipe -> recipe.prepare(level, this),
                    InfusionTiming.duration(speed, overload), this::setChanged);
            AltarAutoExport.tick(this, autoExport, outputSides,
                    getBlockState().getValue(InfusionPedestaBlock.FACING), direction -> {
                        BlockPos target = worldPosition.relative(direction);
                        return level.hasChunkAt(target)
                                ? level.getCapability(Capabilities.ItemHandler.BLOCK, target, direction.getOpposite())
                                : null;
                    });
        }
    }

    @Override
    public double injectAEPower(double amount, Actionable mode) {
        return energy.injectAEPower(amount, mode);
    }

    @Override
    public double extractAEPower(double amount, Actionable mode, PowerMultiplier multiplier) {
        return energy.extractAEPower(amount, mode, multiplier);
    }

    @Override
    public double getAEMaxPower() {
        return energy.getAEMaxPower();
    }

    @Override
    public double getAECurrentPower() {
        return energy.getAECurrentPower();
    }

    @Override
    public boolean isAEPublicPowerStorage() {
        return energy.isAEPublicPowerStorage();
    }

    @Override
    public AccessRestriction getPowerFlow() {
        return energy.getPowerFlow();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        multiplicationUpgrade.save(tag, registries);
        ContainerHelper.saveAllItems(tag, items, registries);
        tag.putBoolean("AutoExport", autoExport);
        tag.putInt("OutputSides", outputSides);
        tag.putDouble("StoredAE", getAECurrentPower());
        tag.put("InfusionWork", processing.save(registries));
        mainNode.saveToNBT(tag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        multiplicationUpgrade.load(tag, registries);
        items = NonNullList.withSize(INVENTORY_SIZE, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items, registries);
        autoExport = tag.getBoolean("AutoExport");
        outputSides = tag.contains("OutputSides")
                ? tag.getInt("OutputSides") & InfusionOutputSide.ALL_SIDES
                : 0;
        energy.load(tag.getDouble("StoredAE"));
        processing.load(tag.getCompound("InfusionWork"), registries);
        mainNode.loadFromNBT(tag);
    }
}
