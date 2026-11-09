package com.appliedmoremachines.mixin;
import appeng.client.gui.AEBaseScreen;
import com.appliedmoremachines.nativeeight.NativeEightHost;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
@Pseudo @Mixin(targets="com.glodblock.github.extendedae.client.gui.GuiCrystalAssembler",remap=false)
public abstract class EightAssemblerScreenMixin {
    @ModifyConstant(method="renderTooltip",constant=@Constant(intValue=16000))
    private int amm$tankCapacity(int capacity) { return ((AEBaseScreen<?>)(Object)this).getMenu().getTarget() instanceof NativeEightHost ? capacity*8 : capacity; }
}
