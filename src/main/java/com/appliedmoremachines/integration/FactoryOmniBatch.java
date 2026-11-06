package com.appliedmoremachines.integration;

import appeng.api.stacks.AEKey;
import com.appliedmoremachines.factory.FactoryAmounts;
import com.appliedmoremachines.factory.ProcessingCraftingProvider;
import com.atir.molecularmanipulator.api.crafting.OmniBatchAdmission;
import com.atir.molecularmanipulator.api.crafting.OmniBatchCraftingProvider;
import com.atir.molecularmanipulator.api.crafting.OmniBatchDelivery;
import com.atir.molecularmanipulator.api.crafting.OmniBatchProbe;
import java.util.LinkedHashMap;
import java.util.Map;

/** Native Omni admission avoids its one-at-a-time fallback and acknowledges durable ownership. */
public final class FactoryOmniBatch extends FactoryBatchBridge implements OmniBatchCraftingProvider {
    public FactoryOmniBatch(ProcessingCraftingProvider base) { super(base); }
    @Override public OmniBatchAdmission prepareOmniBatch(OmniBatchProbe probe) {
        long capacity = Math.min(probe.requestedMaxCrafts(), base.batchCapacity(probe.pattern()));
        if (capacity < 2) return null;
        return new OmniBatchAdmission() {
            private boolean used;
            @Override public long maxCrafts() { return capacity; }
            @Override public void close() { used = true; }
            @Override public void commit(OmniBatchDelivery delivery) {
                boolean accepted = false;
                if (!used) {
                    used = true;
                    var request = delivery.request();
                    if (request.craftCount() <= capacity && request.pattern().getDefinition().equals(probe.pattern().getDefinition())) {
                        Map<AEKey, Long> total = new LinkedHashMap<>();
                        try {
                            request.inputs().forEach(input -> total.merge(input.key(), input.amount(), Math::addExact));
                            if (FactoryAmounts.of(request.expectedOutputs()).equals(
                                    FactoryAmounts.multiply(FactoryAmounts.of(request.pattern().getOutputs()), request.craftCount()))) {
                                accepted = base.acceptBatch(request.pattern(), total, request.craftCount(), request.craftingJobId());
                            }
                        } catch (RuntimeException ex) { accepted = false; }
                    }
                }
                if (accepted) delivery.accept(new OmniBatchDelivery.Receipt(
                        OmniBatchDelivery.Ownership.PERSISTED_PROVIDER_QUEUE,
                        base.freeCapacity() == 0 ? OmniBatchDelivery.Backpressure.SATURATED : OmniBatchDelivery.Backpressure.MAY_ACCEPT_MORE));
                else delivery.reject(OmniBatchDelivery.Rejection.reject(OmniBatchDelivery.RejectReason.CAPACITY_CHANGED));
            }
        };
    }
}
