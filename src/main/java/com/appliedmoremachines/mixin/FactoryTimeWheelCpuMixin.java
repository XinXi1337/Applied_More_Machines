package com.appliedmoremachines.mixin;

import appeng.api.crafting.IPatternDetails;
import appeng.api.networking.crafting.ICraftingLink;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.stacks.KeyCounter;
import com.appliedmoremachines.factory.FactoryJobContext;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.moakiee.ae2lt.crafting.timewheel.Ae2LtTimeWheelCraftingCpuLogic", remap = false)
public abstract class FactoryTimeWheelCpuMixin {
    @Shadow public abstract ICraftingLink getLastLink();
    @WrapOperation(method = "tryPushPattern", at = @At(value = "INVOKE", target =
            "Lappeng/api/networking/crafting/ICraftingProvider;pushPattern(Lappeng/api/crafting/IPatternDetails;[Lappeng/api/stacks/KeyCounter;)Z"))
    private boolean amm$push(ICraftingProvider provider, IPatternDetails pattern, KeyCounter[] inputs, Operation<Boolean> original) {
        boolean[] accepted = {false};
        FactoryJobContext.run(getLastLink(), () -> accepted[0] = original.call(provider, pattern, inputs));
        return accepted[0];
    }
    @Inject(method = "finishJob", at = @At("HEAD"))
    private void amm$finish(boolean success, CallbackInfo ci) {
        if (!success) FactoryJobContext.cancel(getLastLink());
    }
}
