package com.appliedmoremachines;

import com.appliedmoremachines.production.*;

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
import appeng.api.config.AccessRestriction;
import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.networking.GridFlags;
import appeng.api.networking.GridHelper;
import appeng.api.networking.IManagedGridNode;
import appeng.api.networking.energy.IAEPowerStorage;
import appeng.me.helpers.BlockEntityNodeListener;
import appeng.me.helpers.IGridConnectedBlockEntity;
import com.appliedmoremachines.fusion.*;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

/** Fusion inventory with equipment-gated inputs and an in-place catalyst/product slot. */
public final class CraftingCoreBlockEntity extends BaseContainerBlockEntity
        implements IGridConnectedBlockEntity, IAEPowerStorage, MultiplicationHost {
    private final MultiplicationUpgrade multiplicationUpgrade = new MultiplicationUpgrade(this);
    @Override public MultiplicationUpgrade multiplicationUpgrade() { return multiplicationUpgrade; }

    public static final int INPUT_COUNT = 16;
    public static final int INJECTOR_SLOT = 16;
    public static final int CENTER_SLOT = 17;
    public static final int OUTPUT_SLOT = CENTER_SLOT;
    public static final int UPGRADE_START = OUTPUT_SLOT + 1;
    public static final int UPGRADE_COUNT = 4;
    public static final int INVENTORY_SIZE = UPGRADE_START + UPGRADE_COUNT;

    private NonNullList<ItemStack> items = NonNullList.withSize(INVENTORY_SIZE, ItemStack.EMPTY);
    private boolean autoExport;
    private int outputSides;
    private boolean result;
    private final CoreProcessor processing = new CoreProcessor();
    private final CoreEnergyStorage energy = new CoreEnergyStorage(this::setChanged);
    private final IEnergyStorage forgeEnergy = new InfusionForgeEnergyStorage(energy);
    private final IItemHandler automation = new CoreItemHandler(this);
    private final IManagedGridNode mainNode = GridHelper.createManagedNode(this, BlockEntityNodeListener.INSTANCE)
            .setInWorldNode(true).setFlags(GridFlags.REQUIRE_CHANNEL).setTagName("MainNode")
            .setIdlePowerUsage(0).setVisualRepresentation(ModBlocks.AE_CRAFTING_CORE.get())
            .addService(IAEPowerStorage.class, this);
    private final ContainerData outputSettings = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> autoExport ? 1 : 0;
                case 1 -> outputSides;
                case 2 -> processing.ticks();
                case 3 -> processing.duration();
                case 4 -> processing.working() ? 1 : 0;
                case 5 -> result ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> autoExport = value != 0;
                case 1 -> outputSides = value & InfusionOutputSide.ALL_SIDES;
                default -> { return; }
            }
            setChanged();
        }

        @Override
        public int getCount() {
            return CraftingCoreMenu.SETTINGS_COUNT;
        }
    };

    public CraftingCoreBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.AE_CRAFTING_CORE.get(), pos, state);
    }

    @Override
    public int getContainerSize() {
        return INVENTORY_SIZE;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot == INJECTOR_SLOT) return !processing.working() && CoreInjectors.tier(stack) >= 0;
        if (slot == CENTER_SLOT) return !result;
        return slot >= 0 && slot < unlockedSlots()
                || slot >= UPGRADE_START && slot < INVENTORY_SIZE && InfusionUpgradeCards.accepts(stack);
    }

    public int unlockedSlots() { return CoreInjectors.unlocked(getItem(INJECTOR_SLOT)); }
    public boolean isResult() { return result; }
    public boolean isWorking() { return processing.working(); }
    public int slotLimit(int slot, ItemStack stack) {
        if (slot == INJECTOR_SLOT) return Math.min(16, stack.getMaxStackSize());
        if (slot >= UPGRADE_START) return 1;
        if (slot == CENTER_SLOT && !result) return Math.min(stack.getMaxStackSize(), Math.max(1, CoreRecipes.catalystLimit(level, stack)));
        return stack.getMaxStackSize();
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        stack.limitSize(Math.max(slotLimit(slot, stack), ItemStack.isSameItemSameComponents(items.get(slot), stack)
                ? items.get(slot).getCount() : 0));
        items.set(slot, stack);
        if (slot == CENTER_SLOT && stack.isEmpty()) result = false;
        setChanged();
    }

    @Override public ItemStack removeItem(int slot, int amount) {
        ItemStack taken = super.removeItem(slot, amount);
        if (slot == CENTER_SLOT && getItem(slot).isEmpty()) result = false;
        return taken;
    }

    @Override public ItemStack removeItemNoUpdate(int slot) {
        ItemStack taken = super.removeItemNoUpdate(slot);
        if (slot == CENTER_SLOT && getItem(slot).isEmpty()) result = false;
        return taken;
    }

    public void restoreResultFlag(boolean value) { result = value; setChanged(); }

    public IItemHandler getAutomationInventory() { return automation; }
    public void arrangeAutomaticInputs() {
        if (level == null || result || processing.working()) return;
        var recipes = CoreRecipes.get(level);
        // Keep an already correct layout, including the player's chosen material positions.
        for (var recipe : recipes) if (recipe.prepare(level, level.registryAccess(), this) != null) return;
        for (var recipe : recipes) {
            var layout = recipe.automaticLayout(level, level.registryAccess(), this);
            if (layout == null) continue;
            for (int slot = 0; slot < INPUT_COUNT; slot++) setItem(slot, layout.get(slot).copy());
            setItem(CENTER_SLOT, layout.get(CENTER_SLOT).copy());
            return;
        }
    }
    public IEnergyStorage getForgeEnergyStorage() { return forgeEnergy; }
    @Override public IManagedGridNode getMainNode() { return mainNode; }
    @Override public void saveChanges() { setChanged(); }
    public void setOwner(Player player) { mainNode.setOwningPlayer(player); }
    @Override public void clearRemoved() {
        super.clearRemoved();
        GridHelper.onFirstTick(this, core -> {
            if (!core.isRemoved() && core.level != null && !core.mainNode.isReady()) core.mainNode.create(core.level, core.worldPosition);
        });
    }
    @Override public void setRemoved() { mainNode.destroy(); super.setRemoved(); }
    @Override public void onChunkUnloaded() { mainNode.destroy(); super.onChunkUnloaded(); }

    public void serverTick() {
        if (!multiplicationUpgrade.tick()) return;
        if (level == null || level.isClientSide) return;
        double demand = getAEMaxPower() - getAECurrentPower();
        if (demand > 0 && mainNode.isActive()) mainNode.ifPresent(grid -> injectAEPower(
                grid.getEnergyService().extractAEPower(demand, Actionable.MODULATE, PowerMultiplier.ONE), Actionable.MODULATE));
        if (result) processing.idle(this::setChanged);
        else {
            int speed = 0, overload = 0;
            for (int slot = UPGRADE_START; slot < INVENTORY_SIZE; slot++) {
                if (InfusionUpgradeCards.isOverload(getItem(slot))) overload++;
                else if (InfusionUpgradeCards.accepts(getItem(slot))) speed++;
            }
            processing.tick(this, energy, CoreRecipes.get(level), recipe -> recipe.prepare(level, level.registryAccess(), this),
                    CoreInjectors.duration(speed, overload), plan -> {
                        for (int slot = 0; slot < INPUT_COUNT; slot++) setItem(slot, plan.materialsAfter().get(slot).copy());
                        // Replace the consumed catalyst before planning output capacity.
                        setItem(CENTER_SLOT, ItemStack.EMPTY);
                        result = true;
                        MultiplicationOutputs.deliver(this, new int[]{CENTER_SLOT},
                                java.util.List.of(MultiplicationUpgrade.product(this, plan.result(), 1)));
                    }, this::setChanged);
        }
        if (result && autoExport && outputSides != 0) {
            var front = getBlockState().getValue(CraftingCoreBlock.FACING);
            for (var side : InfusionOutputSide.values()) {
                if (!result || (outputSides & side.mask()) == 0) continue;
                var direction = side.worldDirection(front);
                var targetPos = worldPosition.relative(direction);
                var target = level.hasChunkAt(targetPos)
                        ? level.getCapability(Capabilities.ItemHandler.BLOCK, targetPos, direction.getOpposite()) : null;
                if (target != null) setItem(CENTER_SLOT, ItemHandlerHelper.insertItemStacked(target, getItem(CENTER_SLOT).copy(), false));
            }
        }
    }
    @Override public double injectAEPower(double amount, Actionable mode) { return energy.injectAEPower(amount, mode); }
    @Override public double extractAEPower(double amount, Actionable mode, PowerMultiplier multiplier) { return energy.extractAEPower(amount, mode, multiplier); }
    @Override public double getAEMaxPower() { return energy.getAEMaxPower(); }
    @Override public double getAECurrentPower() { return energy.getAECurrentPower(); }
    @Override public boolean isAEPublicPowerStorage() { return false; }
    @Override public AccessRestriction getPowerFlow() { return energy.getPowerFlow(); }

    /** Copies into empty slots; the interaction handler consumes only the returned count. */
    public int installUpgradeCards(ItemStack heldStack) {
        if (level == null || level.isClientSide || !InfusionUpgradeCards.accepts(heldStack)) {
            return 0;
        }
        int installed = 0;
        for (int slot = UPGRADE_START; slot < INVENTORY_SIZE && installed < heldStack.getCount(); slot++) {
            if (getItem(slot).isEmpty()) {
                setItem(slot, heldStack.copyWithCount(1));
                installed++;
            }
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
        result = false;
        setChanged();
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("block.applied_more_machines.ae_crafting_core");
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new CraftingCoreMenu(containerId, inventory, this);
    }

    public ContainerData getOutputSettings() {
        return outputSettings;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        multiplicationUpgrade.save(tag, registries);
        ContainerHelper.saveAllItems(tag, items, registries);
        tag.putBoolean("AutoExport", autoExport);
        tag.putInt("OutputSides", outputSides);
        tag.putBoolean("CenterIsResult", result);
        tag.putInt("CoreDataVersion", 2);
        tag.putDouble("StoredAE", getAECurrentPower());
        tag.put("FusionWork", processing.save(registries));
        mainNode.saveToNBT(tag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        multiplicationUpgrade.load(tag, registries);
        items = NonNullList.withSize(INVENTORY_SIZE, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items, registries);
        autoExport = tag.getBoolean("AutoExport");
        outputSides = tag.getInt("OutputSides") & InfusionOutputSide.ALL_SIDES;
        // Before fusion support slot17 was extraction-only: preserve any legacy output as a product.
        result = tag.contains("CenterIsResult") ? tag.getBoolean("CenterIsResult") : !getItem(CENTER_SLOT).isEmpty();
        energy.load(tag.getDouble("StoredAE"));
        processing.load(tag.getCompound("FusionWork"), registries);
        mainNode.loadFromNBT(tag);
    }
}
