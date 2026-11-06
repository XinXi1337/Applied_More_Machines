package com.appliedmoremachines.factory;

import appeng.api.crafting.IPatternDetails;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.stacks.KeyCounter;
import appeng.api.stacks.AEKey;
import java.util.Map;
import java.util.UUID;
import com.appliedmoremachines.FactoryControllerBlockEntity;
import java.util.List;

/** The connected controller advertises recipes; both controllers share the world ledger. */
public class FactoryCraftingProvider implements ProcessingCraftingProvider {
    private final FactoryControllerBlockEntity owner;
    public FactoryCraftingProvider(FactoryControllerBlockEntity owner) { this.owner = owner; }
    public FactoryProcessor processor() { return owner.processor(); }
    public long batchCapacity(IPatternDetails pattern) {
        var engine = processor();
        return engine == null ? 0 : engine.batchCapacity(owner, pattern);
    }
    public int freeCapacity() {
        var engine = processor();
        return engine == null ? 0 : engine.freeCapacity(owner);
    }
    public boolean acceptBatch(IPatternDetails pattern, Map<AEKey, Long> inputs, long copies, UUID job) {
        var engine = processor();
        var link = FactoryJobContext.current();
        if (link != null && link.isCanceled() && link.getCraftingID().equals(job)) return false;
        return engine != null && engine.pushBatch(owner, pattern, inputs, copies, job);
    }
    @Override public List<IPatternDetails> getAvailablePatterns() {
        FactoryProcessor engine = processor();
        return engine == null ? List.of() : engine.patterns(owner);
    }
    @Override public boolean isBusy() {
        FactoryProcessor engine = processor();
        return engine == null || engine.busy(owner);
    }
    @Override public boolean pushPattern(IPatternDetails pattern, KeyCounter[] inputHolder) {
        FactoryProcessor engine = processor();
        return engine != null && engine.push(owner, pattern, inputHolder);
    }
}
