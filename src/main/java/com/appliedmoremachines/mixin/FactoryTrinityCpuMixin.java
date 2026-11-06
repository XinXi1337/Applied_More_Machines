package com.appliedmoremachines.mixin;

import appeng.api.networking.crafting.ICraftingLink;
import appeng.api.networking.energy.IEnergyService;
import appeng.me.service.CraftingService;
import com.appliedmoremachines.factory.FactoryJobContext;
import com.fish_dan_.data_energistics.common.crafting.trinity.dispatch.commit.CraftingDispatchWindow;
import com.fish_dan_.data_energistics.common.crafting.trinity.dispatch.governor.CraftingDispatchBudget;
import com.fish_dan_.data_energistics.common.crafting.trinity.dispatch.server.CraftingDispatchStepResult;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Trinity's runtime dispatches steps directly, without passing through tickCraftingLogic. */
@Pseudo
@Mixin(targets = "com.fish_dan_.data_energistics.common.crafting.trinity.execution.cpu.TrinityDataCoreCpuLogic", remap = false)
public abstract class FactoryTrinityCpuMixin {
    @Shadow protected abstract ICraftingLink getLastLink();

    @WrapMethod(method = "dispatchStep")
    private CraftingDispatchStepResult amm$dispatch(IEnergyService energy, CraftingService crafting,
            CraftingDispatchWindow window, CraftingDispatchBudget budget, Operation<CraftingDispatchStepResult> original) {
        return FactoryJobContext.call(getLastLink(), () -> original.call(energy, crafting, window, budget));
    }

    @WrapMethod(method = "tickCraftingLogic")
    private void amm$tick(IEnergyService energy, CraftingService crafting,
            CraftingDispatchWindow window, CraftingDispatchBudget budget, Operation<Void> original) {
        FactoryJobContext.run(getLastLink(), () -> original.call(energy, crafting, window, budget));
    }

    @Inject(method = "finishJob", at = @At("HEAD"))
    private void amm$finish(boolean success, CallbackInfo ci) {
        if (!success) FactoryJobContext.cancel(getLastLink());
    }
}
