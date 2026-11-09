package com.appliedmoremachines.mixin;

import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.networking.IGrid;
import appeng.me.Grid;
import appeng.me.service.EnergyService;
import com.appliedmoremachines.power.HiddenEnergyGrid;
import com.appliedmoremachines.power.HiddenPowerNetwork;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import java.util.List;
import org.spongepowered.asm.mixin.*;

@Mixin(value = EnergyService.class, remap = false)
public abstract class HiddenPowerEnergyMixin implements HiddenEnergyGrid {
    @Shadow @Final private Grid grid;
    @Shadow private double tickDrainPerTick;
    @Shadow private List<EnergyService> getConnectedServices() { throw new AssertionError(); }
    @Override public IGrid amm$energyGrid() { return grid; }

    @WrapMethod(method = "extractAEPower")
    private double amm$preferStoredFE(double amount, Actionable mode, PowerMultiplier multiplier,
            Operation<Double> original) {
        if (HiddenPowerNetwork.bypassed() || !Double.isFinite(amount) || amount <= 0)
            return original.call(amount, mode, multiplier);
        double requested = multiplier.multiply(amount);
        if (!Double.isFinite(requested) || requested <= 0) return original.call(amount, mode, multiplier);
        var grids = getConnectedServices().stream()
                .map(service -> ((HiddenEnergyGrid) service).amm$energyGrid()).toList();
        double supplied = HiddenPowerNetwork.extract(grids, requested, mode);
        if (mode == Actionable.MODULATE) tickDrainPerTick += supplied;
        double remaining = requested - supplied;
        double ordinary = remaining > 0 ? original.call(remaining, mode, PowerMultiplier.ONE) : 0;
        return multiplier.divide(supplied + ordinary);
    }
}
