package com.appliedmoremachines.integration;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import com.appliedmoremachines.factory.FactoryAmounts;
import com.appliedmoremachines.factory.FactoryJobContext;
import com.appliedmoremachines.factory.ProcessingCraftingProvider;
import com.fish_dan_.data_energistics.api.crafting.dispatch.CountedCraftingAdmission;
import com.fish_dan_.data_energistics.api.crafting.dispatch.CountedCraftingCapacity;
import com.fish_dan_.data_energistics.api.crafting.dispatch.CountedCraftingRoutingMode;
import com.fish_dan_.data_energistics.api.crafting.dispatch.CountedCraftingTarget;
import com.fish_dan_.data_energistics.common.crafting.trinity.dispatch.provider.CountedCraftingProvider;
import it.unimi.dsi.fastutil.objects.ObjectList;
import java.util.Map;
import java.util.OptionalLong;

/** Trinity extracts the admitted total; ownership transfers only after our durable queue accepts it. */
public final class FactoryTrinityBatch extends FactoryBatchBridge implements CountedCraftingProvider {
    public FactoryTrinityBatch(ProcessingCraftingProvider base) { super(base); }

    @Override
    public ObjectList<CountedCraftingCapacity> captureCapacityFast(
            IPatternDetails pattern, KeyCounter[] prototype, long requestedCount) {
        long count = capacity(pattern, requestedCount);
        return ObjectList.of(new CountedCraftingCapacity(CountedCraftingTarget.provider(),
                CountedCraftingRoutingMode.AGGREGATE, OptionalLong.of(count), OptionalLong.of(count)));
    }

    @Override
    public CountedCraftingAdmission prepareBatch(
            IPatternDetails pattern, KeyCounter[] prototype, long requestedCount) {
        var link = FactoryJobContext.current();
        if (link == null || link.isCanceled()) return null;
        var job = link.getCraftingID();
        long count = capacity(pattern, requestedCount);
        if (job == null || count == 0) return null;
        final Map<AEKey, Long> expectedPrototype;
        final Map<AEKey, Long> totalInputs;
        try {
            // Both calls receive ONE operation. Trinity separately extracts the remaining copies.
            expectedPrototype = FactoryAmounts.of(prototype);
            totalInputs = FactoryAmounts.multiply(expectedPrototype, count);
        } catch (RuntimeException ex) { return null; }
        return new CountedCraftingAdmission() {
            private boolean used;
            private boolean transferred;

            @Override public long count() { return count; }
            @Override public boolean hasTransferredInputOwnership() { return transferred; }

            @Override public boolean commit(KeyCounter[] inputs) {
                if (used) return false;
                used = true;
                if (link.isCanceled()) return false;
                try {
                    if (!FactoryAmounts.of(inputs).equals(expectedPrototype)) return false;
                    transferred = base.acceptBatch(pattern, totalInputs, count, job);
                    return transferred;
                } catch (RuntimeException ex) { return false; }
            }
        };
    }

    @Override
    public CountedCraftingAdmission prepareBatchForTarget(
            IPatternDetails pattern, KeyCounter[] prototype, long requestedCount, CountedCraftingTarget target) {
        return CountedCraftingTarget.provider().equals(target) ? prepareBatch(pattern, prototype, requestedCount) : null;
    }

    private long capacity(IPatternDetails pattern, long requestedCount) {
        return pattern == null || requestedCount <= 0 ? 0 : Math.max(0, Math.min(requestedCount, base.batchCapacity(pattern)));
    }
}
