package com.appliedmoremachines.mixin;

import com.appliedmoremachines.production.*;

import appeng.menu.AEBaseMenu;
import com.appliedmoremachines.inscriber.InscriberOutputActions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = AEBaseMenu.class, remap = false)
public abstract class ParallelInscriberClickMixin {
    @Inject(method = "clicked", at = @At("HEAD"), cancellable = true)
    private void amm$bulkOutputSwap(int slot, int button, ClickType type, Player player, CallbackInfo ci) {
        if (com.appliedmoremachines.nativeeight.EightMenuActions.click((AEBaseMenu)(Object)this,slot,button,type,player)) { ci.cancel(); return; }
        if (type == ClickType.SWAP && InscriberOutputActions.swap((AEBaseMenu) (Object) this, slot, button, player)) ci.cancel();
    }
    @Inject(method = "quickMoveStack", at = @At("HEAD"), cancellable = true)
    private void amm$dedicatedQuickMove(Player player, int slot,
            org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<net.minecraft.world.item.ItemStack> cir) {
        var result = MultiplicationMenus.quickMove((AEBaseMenu) (Object) this, player, slot);
        if (result == null) result = com.appliedmoremachines.nativeeight.EightMenuActions.quickMove((AEBaseMenu)(Object)this,player,slot);
        if (result != null) cir.setReturnValue(result);
    }
}
