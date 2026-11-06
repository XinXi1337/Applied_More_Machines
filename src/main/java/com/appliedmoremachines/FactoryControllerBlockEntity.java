package com.appliedmoremachines;

import appeng.api.networking.GridHelper;
import appeng.api.networking.GridFlags;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IManagedGridNode;
import appeng.me.helpers.BlockEntityNodeListener;
import appeng.me.helpers.IGridConnectedBlockEntity;
import com.appliedmoremachines.factory.FactoryFormation;
import com.appliedmoremachines.factory.FactoryStructures;
import com.appliedmoremachines.factory.FactoryCraftingProvider;
import com.appliedmoremachines.factory.FactoryProcessingData;
import com.appliedmoremachines.factory.FactoryProcessor;
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
public final class FactoryControllerBlockEntity extends BaseContainerBlockEntity
        implements IGridConnectedBlockEntity {
    public static final int UPGRADE_COUNT = 2;
    public static final int STATUS_IDLE = 0;
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
    private FactoryFormation formation;
    private FactoryFormation processingFormation;
    private List<GenericStack> taskDisplay = List.of();
    private boolean checked;
    private boolean projectionEnabled;
    private int actionStatus;
    private int actionAmount;
    private int dismantleClicks;
    private long lastDismantleClick = Long.MIN_VALUE;
    private UUID lastDismantler;

    private final IManagedGridNode mainNode = GridHelper.createManagedNode(this, BlockEntityNodeListener.INSTANCE)
            .setInWorldNode(true).setFlags(GridFlags.REQUIRE_CHANNEL).setTagName("MainNode").setIdlePowerUsage(0)
            .setVisualRepresentation(ModBlocks.AE_FACTORY_CONTROLLER.get());

    private final ContainerData menuData = new ContainerData() {
        @Override public int get(int index) {
            return switch (index) {
                case 0 -> isFormed() ? 1 : 0;
                case 1 -> FactoryUpgradeCards.parallel(FactoryControllerBlockEntity.this);
                case 2 -> FactoryUpgradeCards.durationTicks(FactoryControllerBlockEntity.this);
                case 3 -> isAeConnected() ? 1 : 0;
                case 4 -> projectionEnabled ? 1 : 0;
                case 5 -> dismantleClicks;
                case 6 -> actionStatus;
                case 7 -> actionAmount;
                case 8 -> FactoryUpgradeCards.count(FactoryControllerBlockEntity.this, FactoryUpgradeCards.Kind.AE_SPEED);
                case 9 -> FactoryUpgradeCards.count(FactoryControllerBlockEntity.this, FactoryUpgradeCards.Kind.OVERLOAD);
                case 10 -> FactoryUpgradeCards.count(FactoryControllerBlockEntity.this, FactoryUpgradeCards.Kind.MEKANISM_SPEED);
                case 11 -> FactoryUpgradeCards.count(FactoryControllerBlockEntity.this, FactoryUpgradeCards.Kind.MEKANISM_STACK);
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

        @Override public int getCount() { return FactoryControllerMenu.DATA_COUNT; }
    };

    public FactoryControllerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.AE_FACTORY_CONTROLLER.get(), pos, state);
        mainNode.addService(ICraftingProvider.class, com.appliedmoremachines.factory.FactoryBatchProviders.create(new FactoryCraftingProvider(this)));
    }

    public FactoryFormation formation() { return formation; }
    public FactoryFormation processingFormation() { return formation != null ? formation : processingFormation; }
    public FactoryProcessor processor() {
        FactoryFormation identity = processingFormation();
        return level instanceof net.minecraft.server.level.ServerLevel server && identity != null
                ? FactoryProcessingData.get(server.getServer()).engine(server, identity) : null;
    }
    public List<GenericStack> taskDisplay() { return taskDisplay; }
    public boolean isFormed() { return formation != null && FactoryStructures.enabled(); }
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
        setChanged();
    }

    public void resetDismantleSequence() {
        dismantleClicks = 0;
        lastDismantleClick = Long.MIN_VALUE;
        lastDismantler = null;
        setChanged();
    }

    public void recordDismantleClick(Player player) {
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
            FactoryOperations.dismantle(this, player);
        } else {
            setActionStatus(STATUS_DISMANTLE_PROGRESS, dismantleClicks);
        }
        setChanged();
    }

    public void toggleProjection() {
        setProjectionState(!projectionEnabled);
        boolean selected = projectionEnabled;
        for (FactoryControllerBlockEntity controller : FactoryUpgradeCards.controllers(this)) {
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
        for (FactoryControllerBlockEntity controller : FactoryUpgradeCards.controllers(this)) {
            if (!controller.mainNode.isReady() || !controller.mainNode.isOnline()
                    || !controller.mainNode.isActive()) continue;
            IGridNode node = controller.mainNode.getNode();
            if (node == null || node.getGrid() == null) continue;

            int factoryControllers = 0;
            for (IGridNode member : node.getGrid().getNodes()) {
                if (member.getOwner() instanceof FactoryControllerBlockEntity) factoryControllers++;
            }
            // The controller always owns a local one-node grid. Only count it as
            // connected when the grid contains at least one non-controller node.
            if (node.getGrid().size() > factoryControllers) return true;
        }
        return false;
    }

    public IGrid aeGrid() {
        FactoryControllerBlockEntity connected = FactoryProcessor.host(this);
        if (connected != null) return connected.mainNode.getGrid();
        if (mainNode.isReady() && mainNode.getGrid() != null) return mainNode.getGrid();
        for (FactoryControllerBlockEntity controller : FactoryUpgradeCards.controllers(this)) {
            if (controller.mainNode.isReady() && controller.mainNode.getGrid() != null) {
                return controller.mainNode.getGrid();
            }
        }
        return null;
    }

    public void serverTick() {
        if (level == null || level.isClientSide) return;
        if (dismantleClicks > 0 && level.getGameTime() - lastDismantleClick > 20L) {
            dismantleClicks = 0;
            lastDismantleClick = Long.MIN_VALUE;
            lastDismantler = null;
            if (actionStatus == STATUS_DISMANTLE_PROGRESS) setActionStatus(STATUS_IDLE, 0);
            setChanged();
        }
        if (!checked || Math.floorMod(level.getGameTime() + worldPosition.asLong(), 20) == 0) {
            checked = true;
            FactoryStructures.refresh(this);
        }
        FactoryProcessor processor = processor();
        if (processor != null) {
            processor.tick(this);
            List<GenericStack> nextDisplay = processor.display(level.registryAccess());
            if (!taskDisplay.equals(nextDisplay)) {
                taskDisplay = nextDisplay;
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
            }
        }
    }

    public void setFormation(FactoryFormation next) {
        boolean changed = !Objects.equals(formation, next);
        formation = next;
        if (next != null) processingFormation = next;
        if (level == null || level.isClientSide) return;
        BlockState state = getBlockState();
        if (!state.is(ModBlocks.AE_FACTORY_CONTROLLER.get())) return;
        boolean formed = next != null;
        if (state.getValue(FactoryControllerBlock.FORMED) != formed) {
            level.setBlock(worldPosition, state.setValue(FactoryControllerBlock.FORMED, formed), Block.UPDATE_CLIENTS);
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
        return FactoryUpgradeCards.canAccept(this, slot, stack);
    }

    @Override public int getMaxStackSize() { return 64; }

    @Override public void setItem(int slot, ItemStack stack) {
        if (slot < 0 || slot >= UPGRADE_COUNT) return;
        if (!stack.isEmpty()) {
            FactoryUpgradeCards.Kind kind = FactoryUpgradeCards.type(stack);
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
        return Component.translatable("block.applied_more_machines.ae_factory_controller");
    }

    @Override protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new FactoryControllerMenu(containerId, inventory, this);
    }

    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        if (formation != null) {
            tag.putLong("FactoryOrigin", formation.origin().asLong());
            tag.putInt("FactoryRotation", formation.quarterTurns());
        }
        tag.putBoolean("ProjectionEnabled", projectionEnabled);
        net.minecraft.nbt.ListTag display = new net.minecraft.nbt.ListTag();
        taskDisplay.forEach(stack -> display.add(GenericStack.writeTag(registries, stack)));
        tag.put("TaskDisplay", display);
        return tag;
    }

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, registries);
        tag.putBoolean("ProjectionEnabled", projectionEnabled);
        if (processingFormation() != null) {
            tag.putLong("ProcessingOrigin", processingFormation().origin().asLong());
            tag.putInt("ProcessingRotation", processingFormation().quarterTurns());
        }
        mainNode.saveToNBT(tag);
    }

    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items = NonNullList.withSize(UPGRADE_COUNT, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items, registries);
        formation = tag.contains("FactoryOrigin", CompoundTag.TAG_LONG)
                ? new FactoryFormation(BlockPos.of(tag.getLong("FactoryOrigin")), tag.getInt("FactoryRotation")) : null;
        projectionEnabled = tag.getBoolean("ProjectionEnabled");
        processingFormation = tag.contains("ProcessingOrigin", CompoundTag.TAG_LONG)
                ? new FactoryFormation(BlockPos.of(tag.getLong("ProcessingOrigin")), tag.getInt("ProcessingRotation")) : formation;
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
