package com.appliedmoremachines.mixin;
import appeng.menu.AEBaseMenu;
import com.appliedmoremachines.nativeeight.NativeEightHost;
import com.appliedmoremachines.production.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Pseudo @Mixin(targets={"com.glodblock.github.extendedae.container.ContainerCrystalAssembler","com.glodblock.github.extendedae.container.ContainerCircuitCutter","io.github.lounode.ae2cs.common.menu.CircuitEtcherMenu","com.moakiee.ae2lt.menu.OverloadProcessingFactoryMenu"},remap=false)
public abstract class EightMenuMixin {
    @Inject(method="<init>",at=@At("RETURN"))
    private void amm$card(CallbackInfo ci) {
        var host=((AEBaseMenu)(Object)this).getTarget();
        if(host instanceof NativeEightHost && MultiplicationUpgrade.enabled()) ((MultiplicationMenuAccess)this).amm$addSlot(new MultiplicationSlot((BlockEntity)host,180,77,()->true));
    }
}
