package com.appliedmoremachines.mixin;

import com.appliedmoremachines.items.VirtueTools;
import com.appliedmoremachines.integration.DraconicVirtueTools;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.brandon3055.draconicevolution.blocks.ChaosCrystal", remap = false)
public abstract class VirtueChaosCrystalMixin {
    @Inject(method = "getDestroyProgress", at = @At("HEAD"), cancellable = true)
    private void amm$twentyTicks(BlockState state, Player player, BlockGetter level, BlockPos pos, CallbackInfoReturnable<Float> cir) {
        if (VirtueTools.isPick(player.getMainHandItem())) cir.setReturnValue(1.0F / 20);
    }
    @WrapMethod(method = "onDestroyedByPlayer")
    private boolean amm$scopedRemoval(BlockState state, Level level, BlockPos pos, Player player,
            boolean harvest, FluidState fluid, Operation<Boolean> original) {
        return DraconicVirtueTools.remove(level, pos, player, () -> original.call(state, level, pos, player, harvest, fluid));
    }
}
