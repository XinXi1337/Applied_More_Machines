package com.appliedmoremachines.integration;

import appeng.api.crafting.IPatternDetails;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.stacks.KeyCounter;
import com.appliedmoremachines.factory.ProcessingCraftingProvider;
import com.appliedmoremachines.factory.FactoryJobContext;
import java.util.List;
import java.util.UUID;

abstract class FactoryBatchBridge implements ICraftingProvider {
    protected final ProcessingCraftingProvider base;
    FactoryBatchBridge(ProcessingCraftingProvider base) { this.base = base; }
    protected static UUID currentJob() {
        var link = FactoryJobContext.current();
        return link == null || link.isCanceled() ? null : link.getCraftingID();
    }
    @Override public List<IPatternDetails> getAvailablePatterns() { return base.getAvailablePatterns(); }
    @Override public boolean isBusy() { return base.isBusy(); }
    @Override public boolean pushPattern(IPatternDetails pattern, KeyCounter[] inputs) { return base.pushPattern(pattern, inputs); }
}
