package com.appliedmoremachines.fusion;

import appeng.api.config.AccessRestriction;
import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.networking.energy.IAEPowerStorage;

/** Private256M AE buffer. Charged recipe work is not withdrawable stored energy. */
public final class CoreEnergyStorage implements IAEPowerStorage {
    public static final double CAPACITY = 256_000_000;
    private final Runnable dirty;
    private double stored;
    public CoreEnergyStorage(Runnable dirty) { this.dirty = dirty; }
    public void load(double amount) { stored = Double.isNaN(amount) ? 0 : Math.clamp(amount, 0, CAPACITY); }
    @Override public double injectAEPower(double amount, Actionable mode) {
        if (!Double.isFinite(amount) || amount <= 0) return amount;
        double accepted = Math.min(amount, CAPACITY - stored);
        if (mode == Actionable.MODULATE && accepted > 0) { stored += accepted; dirty.run(); }
        return amount - accepted;
    }
    @Override public double extractAEPower(double amount, Actionable mode, PowerMultiplier multiplier) {
        if (!Double.isFinite(amount) || amount <= 0) return 0;
        double taken = Math.min(multiplier.multiply(amount), stored);
        if (mode == Actionable.MODULATE && taken > 0) { stored -= taken; dirty.run(); }
        return multiplier.divide(taken);
    }
    @Override public double getAECurrentPower() { return stored; }
    @Override public double getAEMaxPower() { return CAPACITY; }
    @Override public boolean isAEPublicPowerStorage() { return false; }
    @Override public AccessRestriction getPowerFlow() { return AccessRestriction.READ_WRITE; }
}
