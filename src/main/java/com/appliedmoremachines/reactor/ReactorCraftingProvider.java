package com.appliedmoremachines.reactor;

import com.appliedmoremachines.factory.FactoryJobContext;

import appeng.api.crafting.IPatternDetails;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.stacks.KeyCounter;
import appeng.api.stacks.AEKey;
import java.util.Map;
import java.util.UUID;
import com.appliedmoremachines.ReactorControllerBlockEntity;
import java.util.List;

/** The connected controller advertises recipes; both controllers share the world ledger. */
public class ReactorCraftingProvider implements com.appliedmoremachines.factory.ProcessingCraftingProvider {
    private final ReactorControllerBlockEntity owner;
    public ReactorCraftingProvider(ReactorControllerBlockEntity owner) { this.owner = owner; }
    public ReactorProcessor processor() { return owner.processor(); }
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
        ReactorProcessor engine = processor();
        return engine == null ? List.of() : engine.patterns(owner);
    }
    @Override public boolean isBusy() {
        ReactorProcessor engine = processor();
        return engine == null || engine.busy(owner);
    }
    @Override public boolean pushPattern(IPatternDetails pattern, KeyCounter[] inputHolder) {
        ReactorProcessor engine = processor();
        return engine != null && engine.push(owner, pattern, inputHolder);
    }
}
