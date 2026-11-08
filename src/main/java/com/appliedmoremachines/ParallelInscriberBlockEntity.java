package com.appliedmoremachines;

import com.appliedmoremachines.production.*;

import appeng.blockentity.misc.InscriberBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** Native AE2 machine with only its completed batch size changed. */
public final class ParallelInscriberBlockEntity extends InscriberBlockEntity implements MultiplicationHost {
    private final MultiplicationUpgrade multiplicationUpgrade = new MultiplicationUpgrade(this);
    @Override public MultiplicationUpgrade multiplicationUpgrade() { return multiplicationUpgrade; }

    private final int parallel;

    public ParallelInscriberBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int parallel) {
        super(type, pos, state);
        this.parallel = parallel;
    }

    private appeng.api.upgrades.IUpgradeInventory dedicatedNativeUpgrades;
    @Override public appeng.api.upgrades.IUpgradeInventory getUpgrades() {
        var original = super.getUpgrades();
        if (multiplicationUpgrade == null) return original; // Super construction can call virtual methods.
        if (dedicatedNativeUpgrades == null) dedicatedNativeUpgrades = new com.appliedmoremachines.inscriber.InscriberUpgradeInventory(original);
        return dedicatedNativeUpgrades;
    }
    @Override public appeng.api.networking.ticking.TickRateModulation tickingRequest(appeng.api.networking.IGridNode node, int elapsed) {
        // Migrate the previously accepted card without leaving it visible to DE's native output hooks.
        var nativeUpgrades = super.getUpgrades();
        for (int slot = 0; slot < nativeUpgrades.size(); slot++) {
            var stack = nativeUpgrades.getStackInSlot(slot);
            if (MultiplicationUpgrade.isCard(stack) && multiplicationUpgrade.isEmpty()) {
                multiplicationUpgrade.setItem(0, stack.copyWithCount(1));
                nativeUpgrades.setItemDirect(slot, stack.getCount() == 1 ? net.minecraft.world.item.ItemStack.EMPTY
                        : stack.copyWithCount(stack.getCount() - 1));
            }
        }
        if (!multiplicationUpgrade.tick()) return appeng.api.networking.ticking.TickRateModulation.SLOWER;
        return super.tickingRequest(node, elapsed);
    }
    @Override public appeng.api.networking.ticking.TickingRequest getTickingRequest(appeng.api.networking.IGridNode node) {
        return multiplicationUpgrade.hasPending()
                ? new appeng.api.networking.ticking.TickingRequest(appeng.core.settings.TickRates.Inscriber, false)
                : super.getTickingRequest(node);
    }

    public int parallelLimit() { return parallel; }
    public int outputCapacity() { return parallel == 8 ? 512 : 1024; }

    @Override public void saveAdditional(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider lookup) {
        // ItemStack's disk codec limits count to 99. Save its components with count one
        // and the true quantity separately, without changing the item or its stack-size component.
        var output = getInternalInventory().getStackInSlot(3);
        int count = output.getCount();
        try {
            if (!output.isEmpty()) output.setCount(1);
            super.saveAdditional(tag, lookup);
            multiplicationUpgrade.save(tag, lookup);
        } finally {
            if (count > 0) output.setCount(count);
        }
        tag.putInt("ParallelOutputCount", count);
    }

    @Override public void loadTag(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider lookup) {
        super.loadTag(tag, lookup);
        multiplicationUpgrade.load(tag, lookup);
        var output = getInternalInventory().getStackInSlot(3);
        if (!output.isEmpty() && tag.contains("ParallelOutputCount", net.minecraft.nbt.Tag.TAG_INT)) {
            output.setCount(Math.max(0, tag.getInt("ParallelOutputCount")));
        }
    }

    @Override public void addAdditionalDrops(net.minecraft.world.level.Level level, BlockPos pos,
            java.util.List<net.minecraft.world.item.ItemStack> drops) {
        var contents = new java.util.ArrayList<net.minecraft.world.item.ItemStack>();
        super.addAdditionalDrops(level, pos, contents);
        contents.addAll(multiplicationUpgrade.takeDrops());
        for (var stack : contents) {
            int remaining = stack.getCount();
            while (remaining > 0) {
                int amount = Math.min(remaining, stack.getMaxStackSize());
                drops.add(stack.copyWithCount(amount));
                remaining -= amount;
            }
        }
    }
}
