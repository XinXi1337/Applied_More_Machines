package com.appliedmoremachines.integration;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.KeyCounter;
import cn.dancingsnow.neoecoae.api.me.provider.ECOJobAwareCraftingProvider;
import cn.dancingsnow.neoecoae.api.me.provider.ECOParallelCraftingProvider;
import cn.dancingsnow.neoecoae.api.me.provider.ECOFastPathDispatchProvider;
import cn.dancingsnow.neoecoae.api.me.provider.ECOBatchDispatchContext;
import com.appliedmoremachines.factory.FactoryAmounts;
import com.appliedmoremachines.factory.ProcessingCraftingProvider;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/** ECO supplies total extracted inputs and an explicit order ID, including its Omni CPU route. */
public final class FactoryEcoBatch extends FactoryBatchBridge
        implements ECOParallelCraftingProvider, ECOJobAwareCraftingProvider, ECOFastPathDispatchProvider {
    public FactoryEcoBatch(ProcessingCraftingProvider base) { super(base); }
    @Override public int eco$getAvailableParallelSlots() { return base.freeCapacity(); }
    @Override public boolean eco$pushPatternBatch(IPatternDetails pattern, KeyCounter[] total, long count, UUID job) {
        try { return base.acceptBatch(pattern, FactoryAmounts.of(total), count, job); }
        catch (RuntimeException ex) { return false; }
    }
    @Override public boolean pushPattern(IPatternDetails pattern, KeyCounter[] total, UUID job) {
        return eco$pushPatternBatch(pattern, total, 1, job);
    }

    @Override public Preparation eco$prepareFastPath(ECOBatchDispatchContext context) {
        if (context.craftingJobId() == null || !context.containerItems().isEmpty()) return null;
        long capacity = base.batchCapacity(context.pattern());
        if (capacity <= 0) return null;
        var outputs = FactoryAmounts.of(context.pattern().getOutputs());
        if (!outputs.equals(FactoryAmounts.of(context.outputs()))) return null;
        var used = new AtomicBoolean();
        return new Preparation(capacity, null, false, batch -> {
            if (!used.compareAndSet(false, true)) return false;
            try {
                if (!batch.remainingTotal().isEmpty() || !FactoryAmounts.of(batch.outputTotal())
                        .equals(FactoryAmounts.multiply(outputs, batch.craftCount()))) return false;
                return base.acceptBatch(context.pattern(), FactoryAmounts.of(batch.inputTotal()),
                        batch.craftCount(), context.craftingJobId());
            } catch (RuntimeException ex) { return false; }
        });
    }
}
