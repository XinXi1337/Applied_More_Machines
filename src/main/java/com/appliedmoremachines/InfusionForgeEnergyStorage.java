package com.appliedmoremachines;

import appeng.api.config.Actionable;
import appeng.api.config.PowerUnit;
import appeng.api.networking.energy.IAEPowerStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;

/** Input-only FE view of the altar's existing AE buffer. */
public final class InfusionForgeEnergyStorage implements IEnergyStorage {
    private final IAEPowerStorage energy;

    public InfusionForgeEnergyStorage(IAEPowerStorage energy) {
        this.energy = energy;
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        double aePerFe = PowerUnit.FE.convertTo(PowerUnit.AE, 1);
        if (maxReceive <= 0 || !Double.isFinite(aePerFe) || aePerFe <= 0) {
            return 0;
        }
        // Round the offer down BEFORE insertion: FE reports whole units, so a
        // fractional final unit must not add energy without charging the sender.
        double room = Math.max(0, energy.getAEMaxPower() - energy.getAECurrentPower());
        int accepted = (int) Math.min(maxReceive, Math.floor(room / aePerFe));
        if (accepted > 0) {
            energy.injectAEPower(accepted * aePerFe,
                    simulate ? Actionable.SIMULATE : Actionable.MODULATE);
        }
        return accepted;
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        return 0;
    }

    @Override
    public int getEnergyStored() {
        return Math.max(0, (int) Math.floor(PowerUnit.AE.convertTo(PowerUnit.FE, energy.getAECurrentPower())));
    }

    @Override
    public int getMaxEnergyStored() {
        return Math.max(0, (int) Math.floor(PowerUnit.AE.convertTo(PowerUnit.FE, energy.getAEMaxPower())));
    }

    @Override
    public boolean canExtract() {
        return false;
    }

    @Override
    public boolean canReceive() {
        return true;
    }
}
