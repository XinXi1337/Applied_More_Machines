package com.appliedmoremachines;

import appeng.api.implementations.blockentities.PatternContainerGroup;
import appeng.api.inventories.InternalInventory;
import appeng.api.networking.GridFlags;
import appeng.api.networking.GridHelper;
import appeng.api.networking.IGrid;
import appeng.api.networking.IManagedGridNode;
import appeng.api.stacks.AEItemKey;
import appeng.core.definitions.AEItems;
import appeng.helpers.patternprovider.PatternContainer;
import appeng.me.helpers.BlockEntityNodeListener;
import appeng.me.helpers.IGridConnectedBlockEntity;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Network-visible pattern storage used by a formed universal factory. */
public final class ReactorPatternBlockEntity extends BaseContainerBlockEntity
        implements IGridConnectedBlockEntity, PatternContainer {
    public static final int SLOT_COUNT = 36;

    private NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);

    private final IManagedGridNode mainNode = GridHelper.createManagedNode(this, BlockEntityNodeListener.INSTANCE)
            .setInWorldNode(true).setFlags(GridFlags.REQUIRE_CHANNEL).setTagName("MainNode").setIdlePowerUsage(0)
            .setVisualRepresentation(ModBlocks.AE_REACTORS_PATTERN.get());

    // Share the existing Container/NBT inventory with terminals and uploaders.
    private final InternalInventory terminalPatterns = new InternalInventory() {
        @Override public int size() { return SLOT_COUNT; }
        @Override public int getSlotLimit(int slot) { return 1; }
        @Override public ItemStack getStackInSlot(int slot) { return getItem(slot); }
        @Override public boolean isItemValid(int slot, ItemStack stack) { return canPlaceItem(slot, stack); }
        @Override public void setItemDirect(int slot, ItemStack stack) { setItem(slot, stack.copy()); }
        @Override public void sendChangeNotification(int slot) { setChanged(); }
    };

    public ReactorPatternBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.AE_REACTORS_PATTERN.get(), pos, state);
    }

    @Override public IManagedGridNode getMainNode() { return mainNode; }
    @Override public void saveChanges() { setChanged(); }
    @Override public void setChanged() {
        super.setChanged();
        com.appliedmoremachines.reactor.ReactorRecipes.invalidate();
    }
    @Override public IGrid getGrid() { return mainNode.getGrid(); }
    @Override public boolean isVisibleInTerminal() { return mainNode.isActive(); }
    @Override public InternalInventory getTerminalPatternInventory() { return terminalPatterns; }
    @Override public long getTerminalSortOrder() { return worldPosition.asLong(); }

    @Override public PatternContainerGroup getTerminalGroup() {
        return new PatternContainerGroup(AEItemKey.of(ModBlocks.AE_REACTORS_PATTERN.get()), getDisplayName(), List.of());
    }

    @Override public void clearRemoved() {
        super.clearRemoved();
        GridHelper.onFirstTick(this, pattern -> {
            if (!pattern.isRemoved() && pattern.level != null && !pattern.mainNode.isReady()) {
                pattern.mainNode.create(pattern.level, pattern.worldPosition);
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

    @Override public int getContainerSize() { return SLOT_COUNT; }

    @Override public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot >= 0 && slot < SLOT_COUNT && acceptsPattern(stack);
    }

    public static boolean acceptsPattern(ItemStack stack) {
        return !stack.isEmpty() && (stack.is(AEItems.CRAFTING_PATTERN.asItem())
                || stack.is(AEItems.PROCESSING_PATTERN.asItem())
                || stack.is(AEItems.SMITHING_TABLE_PATTERN.asItem())
                || stack.is(AEItems.STONECUTTING_PATTERN.asItem()));
    }

    @Override public int getMaxStackSize() { return 1; }

    @Override public void setItem(int slot, ItemStack stack) {
        if (slot < 0 || slot >= SLOT_COUNT) return;
        if (!stack.isEmpty()) {
            if (!canPlaceItem(slot, stack)) return;
            stack.limitSize(1);
        }
        items.set(slot, stack);
        setChanged();
    }

    @Override protected NonNullList<ItemStack> getItems() { return items; }

    @Override protected void setItems(NonNullList<ItemStack> items) { this.items = items; }

    @Override public void clearContent() {
        super.clearContent();
        setChanged();
    }

    @Override protected Component getDefaultName() {
        return Component.translatable("block.applied_more_machines.ae_reactors_pattern");
    }

    @Override protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new ReactorPatternMenu(containerId, inventory, this);
    }

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, registries);
        mainNode.saveToNBT(tag);
    }

    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items, registries);
        mainNode.loadFromNBT(tag);
    }
}
