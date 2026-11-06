package com.appliedmoremachines.dissolution;

import appeng.api.config.Actionable;
import appeng.api.networking.energy.IAEPowerStorage;
import com.appliedmoremachines.factory.FactoryEnergy;

/** Fill a private buffer from stored FE first, then the remaining demand from grid AE. */
public final class DissolutionCharging {
    public static void charge(IAEPowerStorage buffer, FactoryEnergy.Source source) {
        double room = buffer.getAEMaxPower() - buffer.getAECurrentPower();
        if (!Double.isFinite(room) || room <= 0) return;
        double ratio = source.aePerFe();
        if (Double.isFinite(ratio) && ratio > 0) {
            long requested = (long) Math.floor(room / ratio);
            if (requested > 0) {
                long taken = Math.clamp(source.fe(requested, false), 0, requested);
                if (taken > 0) buffer.injectAEPower(taken * ratio, Actionable.MODULATE);
            }
        }
        room = buffer.getAEMaxPower() - buffer.getAECurrentPower();
        if (room <= 0) return;
        double taken = source.ae(room, false);
        if (Double.isFinite(taken) && taken > 0)
            buffer.injectAEPower(Math.min(taken, room), Actionable.MODULATE);
    }

    private DissolutionCharging() {}
}
