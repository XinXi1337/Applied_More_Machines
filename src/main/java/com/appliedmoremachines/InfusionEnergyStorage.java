package com.appliedmoremachines;

import appeng.api.config.AccessRestriction;
import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.networking.energy.IAEPowerStorage;

/** The altar's private AE buffer, separate from recipe progress and grid storage. */
public final class InfusionEnergyStorage implements IAEPowerStorage {
    public static final double CAPACITY = 8_000;

    private final Runnable onChanged;
    private double stored;

    public InfusionEnergyStorage(Runnable onChanged) {
        this.onChanged = onChanged;
    }

    /** Restore old saves without allowing invalid energy amounts into the buffer. */
    public void load(double amount) {
        stored = Double.isNaN(amount) ? 0 : Math.clamp(amount, 0, CAPACITY);
    }

    @Override
    public double injectAEPower(double amount, Actionable mode) {
        if (!Double.isFinite(amount) || amount <= 0) {
            return amount;
        }
        double accepted = Math.min(amount, CAPACITY - stored);
        if (mode == Actionable.MODULATE && accepted > 0) {
            stored += accepted;
            onChanged.run();
        }
        // AE2 insertion returns the remainder, not the accepted amount.
        return amount - accepted;
    }

    @Override
    public double extractAEPower(double amount, Actionable mode, PowerMultiplier multiplier) {
        if (!Double.isFinite(amount) || amount <= 0) {
            return 0;
        }
        double extracted = Math.min(multiplier.multiply(amount), stored);
        if (mode == Actionable.MODULATE && extracted > 0) {
            stored -= extracted;
            onChanged.run();
        }
        return multiplier.divide(extracted);
    }

    @Override
    public double getAEMaxPower() {
        return CAPACITY;
    }

    @Override
    public double getAECurrentPower() {
        return stored;
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
