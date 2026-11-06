package com.appliedmoremachines.mixin;

import appeng.api.networking.energy.IEnergyService;
import appeng.me.service.CraftingService;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import appeng.api.networking.crafting.ICraftingLink;
import com.appliedmoremachines.factory.FactoryJobContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "net.pedroksl.advanced_ae.common.logic.AdvCraftingCPULogic", remap = false)
public abstract class FactoryAdvancedCpuMixin {
    @Shadow public abstract ICraftingLink getLastLink();

    @WrapMethod(method = "tickCraftingLogic")
    private void amm$tick(IEnergyService energy, CraftingService crafting, Operation<Void> original) {
        FactoryJobContext.run(getLastLink(), () -> original.call(energy, crafting));
    }

    @Inject(method = "finishJob", at = @At("HEAD"))
    private void amm$finish(boolean success, CallbackInfo ci) {
        if (!success) FactoryJobContext.cancel(getLastLink());
    }
}
