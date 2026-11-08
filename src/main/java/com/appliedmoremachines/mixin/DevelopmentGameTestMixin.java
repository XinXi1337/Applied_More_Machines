package com.appliedmoremachines.mixin;

import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.gametest.GameTestHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Normal development play runs need not load third-party test-only dependencies. */
@Mixin(value = GameTestHooks.class, remap = false)
public abstract class DevelopmentGameTestMixin {
    @Inject(method = "registerGametests", at = @At("HEAD"), cancellable = true)
    private static void amm$skipAutomaticGameTests(CallbackInfo ci) {
        // The explicit run property keeps dedicated tests and production behavior unchanged.
        if (Boolean.getBoolean("applied_more_machines.skipAutomaticGameTests") && !FMLLoader.isProduction()) {
            ci.cancel();
        }
    }
}
