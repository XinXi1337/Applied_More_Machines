package com.appliedmoremachines.factory;

import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.config.PowerUnit;
import appeng.api.networking.IGrid;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEKey;
import com.appliedmoremachines.integration.FactoryFluxEnergy;
import net.neoforged.fml.ModList;

/** A round's persistent payment credit is denominated in AE. */
public final class FactoryEnergy {
    public static final double ROUND_COST = 8_000;
    private FactoryEnergy() {}

    public interface Source {
        long fe(long amount, boolean simulate);
        double ae(double amount, boolean simulate);
        double aePerFe();
    }

    public static Source source(IGrid grid, IActionSource action) {
        AEKey fe = ModList.get().isLoaded("appflux") ? FactoryFluxEnergy.feKey() : null;
        return new Source() {
            @Override public long fe(long amount, boolean simulate) {
                return fe == null ? 0 : grid.getStorageService().getInventory().extract(fe, amount,
                        simulate ? Actionable.SIMULATE : Actionable.MODULATE, action);
            }
            @Override public double ae(double amount, boolean simulate) {
                return grid.getEnergyService().extractAEPower(amount,
                        simulate ? Actionable.SIMULATE : Actionable.MODULATE, PowerMultiplier.ONE);
            }
            @Override public double aePerFe() { return PowerUnit.FE.convertTo(PowerUnit.AE, 1); }
        };
    }

    /** Simulate full affordability first. Any unexpected partial payment remains credited. */
    public static double pay(Source source, double credit) {
        double missing = Math.max(0, ROUND_COST - credit);
        if (missing < 0.000001) return ROUND_COST;
        double ratio = source.aePerFe();
        long requested = Double.isFinite(ratio) && ratio > 0
                ? (long) Math.floor(missing / ratio) : 0;
        long availableFe = Math.clamp(source.fe(requested, true), 0, requested);
        double remaining = Math.max(0, missing - availableFe * ratio);
        if (source.ae(remaining, true) + 0.000001 < remaining) return credit;
        long extractedFe = Math.clamp(source.fe(availableFe, false), 0, availableFe);
        double paid = extractedFe * ratio;
        remaining = Math.max(0, missing - paid);
        paid += Math.clamp(source.ae(remaining, false), 0, remaining);
        return Math.min(ROUND_COST, credit + paid);
    }
}
