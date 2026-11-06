package com.appliedmoremachines.integration;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.KeyCounter;
import com.appliedmoremachines.factory.FactoryAmounts;
import com.appliedmoremachines.factory.ProcessingCraftingProvider;
import com.moakiee.thunderbolt.api.crafting.batch.BatchJobView;
import com.moakiee.thunderbolt.api.crafting.batch.IBatchCraftingProvider;
import com.moakiee.thunderbolt.api.crafting.batch.PreparedBatch;
import java.util.UUID;

/** Thunderbolt/Tianshu pass a ONE-copy template; the CPU owns the already withdrawn bulk. */
public final class FactoryThunderBatch extends FactoryBatchBridge implements IBatchCraftingProvider {
    public FactoryThunderBatch(ProcessingCraftingProvider base) { super(base); }
    @Override public long getBatchCapacity(IPatternDetails pattern) { return base.batchCapacity(pattern); }
    @Override public PreparedBatch prepareBatch(IPatternDetails pattern, KeyCounter[] one, long max, BatchJobView job) {
        UUID id = job == null ? currentJob() : job.craftingId();
        long capacity = id == null ? 0 : Math.min(max, base.batchCapacity(pattern));
        var prototype = FactoryAmounts.of(one);
        return new PreparedBatch() {
            private boolean used;
            @Override public long capacity() { return Math.max(0, capacity); }
            @Override public long push(long count) {
                if (used || count < 1 || count > capacity) return count;
                used = true;
                return base.acceptBatch(pattern, FactoryAmounts.multiply(prototype, count), count, id) ? 0 : count;
            }
        };
    }
    @Override public long pushBatch(IPatternDetails pattern, KeyCounter[] one, long count, BatchJobView job) {
        return prepareBatch(pattern, one, count, job).push(count);
    }
    @Override public long pushBatch(IPatternDetails pattern, KeyCounter[] one, long count) {
        return pushBatch(pattern, one, count, null);
    }
}
