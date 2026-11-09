package com.appliedmoremachines.mixin;
import appeng.menu.AEBaseMenu;
import com.appliedmoremachines.nativeeight.NativeEightHost;
import com.appliedmoremachines.production.MultiplicationMenus;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Pseudo @Mixin(targets="com.moakiee.ae2lt.menu.OverloadProcessingFactoryMenu",remap=false)
public abstract class EightFactoryMenuMixin {
    @ModifyConstant(method={"getInputTankCapacity","getOutputTankCapacity"},constant=@Constant(intValue=1024000))
    private int amm$tankCapacity(int n) { return ((AEBaseMenu)(Object)this).getTarget() instanceof NativeEightHost ? n*8 : n; }
    @Inject(method="quickMoveStack",at=@At("HEAD"),cancellable=true)
    private void amm$cardTransfer(Player player,int slot,CallbackInfoReturnable<ItemStack> cir) {
        var result=MultiplicationMenus.quickMove((AEBaseMenu)(Object)this,player,slot);
        if(result!=null) cir.setReturnValue(result);
    }
}
