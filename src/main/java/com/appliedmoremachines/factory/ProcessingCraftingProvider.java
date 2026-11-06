package com.appliedmoremachines.factory;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.AEKey;
import java.util.Map;
import java.util.UUID;
/** Shared CPU batch contract for independent multiblock processors. */
public interface ProcessingCraftingProvider extends ICraftingProvider {
    long batchCapacity(IPatternDetails pattern);
    int freeCapacity();
    boolean acceptBatch(IPatternDetails pattern, Map<AEKey,Long> inputs, long copies, UUID job);
}
