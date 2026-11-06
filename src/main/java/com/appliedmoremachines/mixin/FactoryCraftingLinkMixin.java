package com.appliedmoremachines.mixin;

import appeng.api.networking.crafting.ICraftingLink;
import appeng.crafting.CraftingLink;
import com.appliedmoremachines.factory.FactoryJobContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Capture direct requester cancellation as well as CPU cancellation, even across unloads. */
@Mixin(value = CraftingLink.class, remap = false)
public abstract class FactoryCraftingLinkMixin {
    @Inject(method = "cancel", at = @At("HEAD"))
    private void amm$cancel(CallbackInfo ci) {
        FactoryJobContext.cancel((ICraftingLink) (Object) this);
    }
}
