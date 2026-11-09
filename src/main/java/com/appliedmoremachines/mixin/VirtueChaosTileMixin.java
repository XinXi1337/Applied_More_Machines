package com.appliedmoremachines.mixin;

import com.appliedmoremachines.integration.DraconicVirtueTools;
import com.brandon3055.draconicevolution.blocks.tileentity.TileChaosCrystal;
import com.brandon3055.brandonscore.lib.datamanager.ManagedBool;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.brandon3055.draconicevolution.blocks.tileentity.TileChaosCrystal", remap = false)
public abstract class VirtueChaosTileMixin {
    @Inject(method = "attemptingBreak", at = @At("HEAD"), cancellable = true)
    private void amm$allowThisRemoval(Player player, CallbackInfoReturnable<Boolean> cir) {
        if (DraconicVirtueTools.bypass((TileChaosCrystal) (Object) this)) cir.setReturnValue(true);
    }
    @WrapOperation(method = "detonate", at = @At(value = "INVOKE", target = "Lcom/brandon3055/brandonscore/lib/datamanager/ManagedBool;get()Z"))
    private boolean amm$doNotRestoreThisCrystal(ManagedBool defeated, Operation<Boolean> original) {
        // Preserve the stored guardian flag; native removal and configured shard drops still run.
        return DraconicVirtueTools.bypass((TileChaosCrystal) (Object) this) || original.call(defeated);
    }
}
