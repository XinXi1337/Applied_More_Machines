package com.appliedmoremachines.laser;

import appeng.api.config.AccessRestriction;
import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.networking.energy.IAEPowerStorage;
import java.util.Objects;

/** One private AE buffer shared by the grid, external charging and the fluid laser base. */
public final class FluidLaserEnergyStorage implements IAEPowerStorage {
    public static final double CAPACITY = 256_000;

    private final Runnable onChanged;
    private double stored;

    public FluidLaserEnergyStorage(Runnable onChanged) {
        this.onChanged = Objects.requireNonNull(onChanged);
    }

    /** Restore saved energy without introducing negative, over-capacity or NaN values. */
    public void load(double amount) {
        stored = Double.isNaN(amount) ? 0 : Math.clamp(amount, 0, CAPACITY);
    }

    @Override
    public double injectAEPower(double amount, Actionable mode) {
        if (!Double.isFinite(amount) || amount <= 0) return amount;
        double accepted = Math.min(amount, CAPACITY - stored);
        if (mode == Actionable.MODULATE && accepted > 0) {
            stored += accepted;
            onChanged.run();
        }
        // AE2 insertion returns the portion that could not be accepted.
        return amount - accepted;
    }

    @Override
    public double extractAEPower(double amount, Actionable mode, PowerMultiplier multiplier) {
        if (!Double.isFinite(amount) || amount <= 0 || multiplier == null) return 0;
        double factor = multiplier.multiply(1);
        if (!Double.isFinite(factor) || factor <= 0) return 0;
        double taken = Math.min(multiplier.multiply(amount), stored);
        if (mode == Actionable.MODULATE && taken > 0) {
            stored -= taken;
            onChanged.run();
        }
        return multiplier.divide(taken);
    }

    @Override
    public double getAECurrentPower() {
        return stored;
    }

    @Override
    public double getAEMaxPower() {
        return CAPACITY;
    }

    @Override
    public boolean isAEPublicPowerStorage() {
        return false;
    }

    @Override
    public AccessRestriction getPowerFlow() {
        return AccessRestriction.READ_WRITE;
    }
}
