package com.appliedmoremachines.integration;

import appeng.api.stacks.AEKey;
import com.glodblock.github.appflux.common.me.key.FluxKey;
import com.glodblock.github.appflux.common.me.key.type.EnergyType;

/** Kept separate so AppliedFlux is never linked on installations without it. */
public final class FactoryFluxEnergy {
    private FactoryFluxEnergy() {}
    public static AEKey feKey() { return FluxKey.of(EnergyType.FE); }
}
