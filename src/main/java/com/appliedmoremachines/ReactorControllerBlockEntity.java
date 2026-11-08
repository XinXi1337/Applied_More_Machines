package com.appliedmoremachines;

import com.appliedmoremachines.production.*;

import appeng.api.networking.GridHelper;
import appeng.api.networking.GridFlags;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IManagedGridNode;
import appeng.me.helpers.BlockEntityNodeListener;
import appeng.me.helpers.IGridConnectedBlockEntity;
import com.appliedmoremachines.reactor.ReactorFormation;
import com.appliedmoremachines.reactor.ReactorStructures;
import com.appliedmoremachines.reactor.ReactorCraftingProvider;
import com.appliedmoremachines.reactor.ReactorProcessingData;
import com.appliedmoremachines.reactor.ReactorProcessor;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.stacks.GenericStack;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Controller UI state, shared upgrade storage and persistent structure projection. */
public final class ReactorControllerBlockEntity extends BaseContainerBlockEntity
        implements IGridConnectedBlockEntity, MultiplicationHost {
    private final MultiplicationUpgrade multiplicationUpgrade = new MultiplicationUpgrade(this);
    @Override public MultiplicationUpgrade multiplicationUpgrade() { return multiplicationUpgrade; }

    public static final int UPGRADE_COUNT = 1;
    public static final int STATUS_IDLE = 0;
    public static final int STATUS_TANK_NOT_EMPTY = 10;
    public static final int STATUS_MATERIALS = 1;
    public static final int STATUS_CONFLICT = 2;
    public static final int STATUS_PLACED = 3;
    public static final int STATUS_DISMANTLED = 4;
    public static final int STATUS_ALREADY_FORMED = 5;
    public static final int STATUS_RETURN_FULL = 6;
    public static final int STATUS_NOT_FORMED = 7;
    public static final int STATUS_DISMANTLE_PROGRESS = 8;
    public static final int STATUS_PLACEMENT_FAILED = 9;

    private NonNullList<ItemStack> items = NonNullList.withSize(UPGRADE_COUNT, ItemStack.EMPTY);
    private ReactorFormation formation;
    private ReactorFormation processingFormation;
    private List<GenericStack> taskDisplay = List.of();
    private boolean checked;
    private final java.util.Map<appeng.api.stacks.AEKey,Long> structureReturns = new java.util.LinkedHashMap<>();
    private net.minecraft.nbt.ListTag unresolvedReturns = new net.minecraft.nbt.ListTag();
    public void retainStructureReturns(java.util.Map<? extends appeng.api.stacks.AEKey,Long> values) {
        values.forEach((key,amount) -> { if (amount > 0) structureReturns.merge(key,amount,Math::addExact); });
        setChanged();
    }
    public java.util.Map<appeng.api.stacks.AEKey,Long> structureReturns() { return structureReturns; }
    public void flushStructureReturns(Player player) {
        IGrid grid = aeGrid();
        var source = player == null ? appeng.api.networking.security.IActionSource.ofMachine(this)
                : appeng.api.networking.security.IActionSource.ofPlayer(player);
        boolean changed = false;
        for (var entry : new java.util.LinkedHashMap<>(structureReturns).entrySet()) {
            long left = entry.getValue();
            if (grid != null) left -= Math.clamp(grid.getStorageService().getInventory().insert(entry.getKey(),left,
                    appeng.api.config.Actionable.MODULATE,source),0,left);
            if (player != null && entry.getKey() instanceof appeng.api.stacks.AEItemKey item) {
                while (left > 0) {
                    var stack = item.toStack((int)Math.min(left,item.getReadOnlyStack().getMaxStackSize()));
                    int before = stack.getCount(); player.getInventory().add(stack); left -= before - stack.getCount();
                    if (!stack.isEmpty()) break;
                }
            }
            if (left != entry.getValue()) {
                changed = true;
                if (left == 0) structureReturns.remove(entry.getKey()); else structureReturns.put(entry.getKey(),left);
            }
        }
        if (changed) setChanged();
    }

    private boolean projectionEnabled;
    private int actionStatus;
    private int actionAmount;
    private int actionStatusTicks;
    private int dismantleClicks;
    private long lastDismantleClick = Long.MIN_VALUE;
    private UUID lastDismantler;

    private final IManagedGridNode mainNode = GridHelper.createManagedNode(this, BlockEntityNodeListener.INSTANCE)
            .setInWorldNode(true).setFlags(GridFlags.REQUIRE_CHANNEL).setTagName("MainNode").setIdlePowerUsage(0)
            .setVisualRepresentation(ModBlocks.AE_REACTORS_CONTROLLER.get());

    private final ContainerData menuData = new ContainerData() {
        @Override public int get(int index) {
            return switch (index) {
                case 0 -> isFormed() ? 1 : 0;
                case 1 -> ReactorUpgradeCards.parallel(ReactorControllerBlockEntity.this);
                case 2 -> ReactorUpgradeCards.durationTicks(ReactorControllerBlockEntity.this);
                case 3 -> isAeConnected() ? 1 : 0;
                case 4 -> projectionEnabled ? 1 : 0;
                case 5 -> dismantleClicks;
                case 6 -> actionStatus;
                case 7 -> actionAmount;
                case 8 -> ReactorUpgradeCards.count(ReactorControllerBlockEntity.this, ReactorUpgradeCards.Kind.AE_SPEED);
                case 9 -> ReactorUpgradeCards.count(ReactorControllerBlockEntity.this, ReactorUpgradeCards.Kind.OVERLOAD);
                case 10 -> 0;
                case 11 -> 0;
                case 12 -> processor() == null ? 0 : processor().progress();
                case 13 -> processor() == null ? 0 : processor().state();
                case 14 -> processor() == null ? 0 : processor().illegal();
                case 15 -> processor() == null ? 0 : processor().queued();
                case 16 -> processor() == null ? 0 : processor().activeCount();
                case 17 -> processor() == null ? 0 : processor().chemicalPaused();
                case 18, 19, 20, 21 -> (int) (worldPosition.asLong() >>> ((index - 18) * 16)) & 65535;
                case 22 -> processor() == null ? 0 : processor().queued() >>> 16;
                case 23 -> processor() == null ? 0 : processor().activeCount() >>> 16;
                default -> 0;
            };
        }

        @Override public void set(int index, int value) {
            // Menu data is authoritative on the server.
        }

        @Override public int getCount() { return ReactorControllerMenu.DATA_COUNT; }
    };

    public ReactorControllerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.AE_REACTORS_CONTROLLER.get(), pos, state);
        mainNode.addService(ICraftingProvider.class, com.appliedmoremachines.factory.FactoryBatchProviders.create(new ReactorCraftingProvider(this)));
    }

    public ReactorFormation formation() { return formation; }
    public ReactorFormation processingFormation() { return formation != null ? formation : processingFormation; }
    public ReactorProcessor processor() {
        ReactorFormation identity = processingFormation();
        return level instanceof net.minecraft.server.level.ServerLevel server && identity != null
                ? ReactorProcessingData.get(server.getServer()).engine(server, identity) : null;
    }
    public List<GenericStack> taskDisplay() { return taskDisplay; }
    public boolean isFormed() { return formation != null && ReactorStructures.enabled(); }
    public boolean projectionEnabled() { return projectionEnabled; }
    public int actionStatus() { return actionStatus; }
    public int actionAmount() { return actionAmount; }
    public int dismantleClicks() { return dismantleClicks; }
    public ContainerData getMenuData() { return menuData; }
    public ItemStack getUpgrade(int slot) {
        return slot >= 0 && slot < UPGRADE_COUNT ? getItem(slot) : ItemStack.EMPTY;
    }

    public void setActionStatus(int status, int amount) {
        actionStatus = status;
        actionAmount = Math.max(0, amount);
        actionStatusTicks = status == STATUS_IDLE ? 0 : 40;
        setChanged();
    }

    public void resetDismantleSequence() {
        dismantleClicks = 0;
        lastDismantleClick = Long.MIN_VALUE;
        lastDismantler = null;
        setChanged();
    }

    public void recordDismantleClick(Player player) {
        flushStructureReturns(player);
        if (!isFormed()) {
            resetDismantleSequence();
            setActionStatus(STATUS_NOT_FORMED, 0);
            return;
        }
        long now = level == null ? 0 : level.getGameTime();
        boolean samePlayer = player.getUUID().equals(lastDismantler);
        if (!samePlayer || now - lastDismantleClick > 20L) dismantleClicks = 0;
        lastDismantler = player.getUUID();
        lastDismantleClick = now;
        dismantleClicks++;
        if (dismantleClicks >= 8) {
            dismantleClicks = 0;
            lastDismantleClick = Long.MIN_VALUE;
            lastDismantler = null;
            ReactorOperations.dismantle(this, player);
        } else {
            setActionStatus(STATUS_DISMANTLE_PROGRESS, dismantleClicks);
        }
        setChanged();
    }

    public void toggleProjection() {
        setProjectionState(!projectionEnabled);
        boolean selected = projectionEnabled;
        for (ReactorControllerBlockEntity controller : ReactorUpgradeCards.controllers(this)) {
            controller.setProjectionState(selected);
        }
    }

    public void setProjectionState(boolean enabled) {
        if (projectionEnabled == enabled) return;
        projectionEnabled = enabled;
        setChanged();
        if (level != null && !level.isClientSide) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }

    public boolean isAeConnected() {
        for (ReactorControllerBlockEntity controller : ReactorUpgradeCards.controllers(this)) {
            if (!controller.mainNode.isReady() || !controller.mainNode.isOnline()
                    || !controller.mainNode.isActive()) continue;
            IGridNode node = controller.mainNode.getNode();
            if (node == null || node.getGrid() == null) continue;

            int factoryControllers = 0;
            for (IGridNode member : node.getGrid().getNodes()) {
                if (member.getOwner() instanceof ReactorControllerBlockEntity) factoryControllers++;
            }
            // The controller always owns a local one-node grid. Only count it as
            // connected when the grid contains at least one non-controller node.
            if (node.getGrid().size() > factoryControllers) return true;
        }
        return false;
    }

    public IGrid aeGrid() {
        ReactorControllerBlockEntity connected = ReactorProcessor.host(this);
        if (connected != null) return connected.mainNode.getGrid();
        if (mainNode.isReady() && mainNode.getGrid() != null) return mainNode.getGrid();
        for (ReactorControllerBlockEntity controller : ReactorUpgradeCards.controllers(this)) {
            if (controller.mainNode.isReady() && controller.mainNode.getGrid() != null) {
                return controller.mainNode.getGrid();
            }
        }
        return null;
    }

    public void serverTick() {
        if (!multiplicationUpgrade.tick()) return;
        if (level == null || level.isClientSide) return;
        if (actionStatusTicks > 0 && --actionStatusTicks == 0) setActionStatus(STATUS_IDLE, 0);
        if (!structureReturns.isEmpty() && Math.floorMod(level.getGameTime(),20) == 0) flushStructureReturns(null);
        if (dismantleClicks > 0 && level.getGameTime() - lastDismantleClick > 20L) {
            dismantleClicks = 0;
            lastDismantleClick = Long.MIN_VALUE;
            lastDismantler = null;
            setChanged();
        }
        if (!checked || Math.floorMod(level.getGameTime() + worldPosition.asLong(), 20) == 0) {
            checked = true;
            ReactorStructures.refresh(this);
        }
        ReactorProcessor processor = processor();
        if (processor != null) {
            processor.tick(this);
            List<GenericStack> nextDisplay = processor.display(level.registryAccess());
            if (!taskDisplay.equals(nextDisplay)) {
                taskDisplay = nextDisplay;
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
            }
        }
    }

    public void setFormation(ReactorFormation next) {
        boolean changed = !Objects.equals(formation, next);
        formation = next;
        if (next != null) processingFormation = next;
        if (level == null || level.isClientSide) return;
        BlockState state = getBlockState();
        if (!state.is(ModBlocks.AE_REACTORS_CONTROLLER.get())) return;
        boolean formed = next != null;
        if (state.getValue(ReactorControllerBlock.FORMED) != formed) {
            level.setBlock(worldPosition, state.setValue(ReactorControllerBlock.FORMED, formed), Block.UPDATE_CLIENTS);
        } else if (changed) {
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
        if (changed) setChanged();
    }

    public void setOwner(Player player) {
        mainNode.setOwningPlayer(player);
    }

    @Override public IManagedGridNode getMainNode() { return mainNode; }
    @Override public void saveChanges() { setChanged(); }

    @Override public void clearRemoved() {
        super.clearRemoved();
        GridHelper.onFirstTick(this, controller -> {
            if (!controller.isRemoved() && controller.level != null && !controller.mainNode.isReady()) {
                controller.mainNode.create(controller.level, controller.worldPosition);
            }
        });
    }

    @Override public void setRemoved() {
        mainNode.destroy();
        super.setRemoved();
    }

    @Override public void onChunkUnloaded() {
        mainNode.destroy();
        super.onChunkUnloaded();
    }

    @Override public int getContainerSize() { return UPGRADE_COUNT; }

    @Override public boolean canPlaceItem(int slot, ItemStack stack) {
        return ReactorUpgradeCards.canAccept(this, slot, stack);
    }

    @Override public int getMaxStackSize() { return 64; }

    @Override public void setItem(int slot, ItemStack stack) {
        if (slot < 0 || slot >= UPGRADE_COUNT) return;
        if (!stack.isEmpty()) {
            ReactorUpgradeCards.Kind kind = ReactorUpgradeCards.type(stack);
            if (kind == null || !canPlaceItem(slot, stack)) return;
            stack.limitSize(kind.limit());
        }
        items.set(slot, stack);
        setChanged();
    }

    @Override protected NonNullList<ItemStack> getItems() { return items; }
    @Override protected void setItems(NonNullList<ItemStack> items) { this.items = items; }

    @Override public void clearContent() {
        for (int slot = 0; slot < UPGRADE_COUNT; slot++) items.set(slot, ItemStack.EMPTY);
        setChanged();
    }

    @Override protected Component getDefaultName() {
        return Component.translatable("block.applied_more_machines.ae_reactors_controller");
    }

    @Override protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new ReactorControllerMenu(containerId, inventory, this);
    }

    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        if (formation != null) {
            tag.putLong("ReactorOrigin", formation.origin().asLong());
            tag.putInt("ReactorRotation", formation.quarterTurns());
        }
        tag.putBoolean("ProjectionEnabled", projectionEnabled);
        net.minecraft.nbt.ListTag display = new net.minecraft.nbt.ListTag();
        taskDisplay.forEach(stack -> display.add(GenericStack.writeTag(registries, stack)));
        tag.put("TaskDisplay", display);
        return tag;
    }

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        multiplicationUpgrade.save(tag, registries);
        ContainerHelper.saveAllItems(tag, items, registries);
        var returns = com.appliedmoremachines.factory.FactoryAmounts.save(structureReturns,registries);
        returns.addAll(unresolvedReturns.copy()); tag.put("StructureReturns",returns);
        tag.putBoolean("ProjectionEnabled", projectionEnabled);
        if (processingFormation() != null) {
            tag.putLong("ProcessingOrigin", processingFormation().origin().asLong());
            tag.putInt("ProcessingRotation", processingFormation().quarterTurns());
        }
        mainNode.saveToNBT(tag);
    }

    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        multiplicationUpgrade.load(tag, registries);
        items = NonNullList.withSize(UPGRADE_COUNT, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items, registries);
        structureReturns.clear(); unresolvedReturns = new net.minecraft.nbt.ListTag();
        for (var value : tag.getList("StructureReturns",net.minecraft.nbt.Tag.TAG_COMPOUND)) {
            var one = new net.minecraft.nbt.ListTag(); one.add(value.copy());
            var amounts = com.appliedmoremachines.factory.FactoryAmounts.load(one,registries);
            if (amounts == null) unresolvedReturns.add(value.copy());
            else amounts.forEach((key,amount) -> structureReturns.merge(key,amount,Math::addExact));
        }
        formation = tag.contains("ReactorOrigin", CompoundTag.TAG_LONG)
                ? new ReactorFormation(BlockPos.of(tag.getLong("ReactorOrigin")), tag.getInt("ReactorRotation")) : null;
        projectionEnabled = tag.getBoolean("ProjectionEnabled");
        processingFormation = tag.contains("ProcessingOrigin", CompoundTag.TAG_LONG)
                ? new ReactorFormation(BlockPos.of(tag.getLong("ProcessingOrigin")), tag.getInt("ProcessingRotation")) : formation;
        var display = new java.util.ArrayList<GenericStack>();
        for (var value : tag.getList("TaskDisplay", CompoundTag.TAG_COMPOUND)) {
            var stack = GenericStack.readTag(registries, (CompoundTag) value);
            if (stack != null) display.add(stack);
        }
        taskDisplay = List.copyOf(display);
        mainNode.loadFromNBT(tag);
        checked = false;
    }
}
