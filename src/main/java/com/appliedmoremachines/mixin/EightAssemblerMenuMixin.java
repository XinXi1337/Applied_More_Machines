package com.appliedmoremachines.mixin;
import appeng.menu.slot.AppEngSlot;
import appeng.menu.AEBaseMenu;
import com.appliedmoremachines.nativeeight.NativeEightHost;
import net.minecraft.network.chat.Component;
import java.util.List;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Pseudo @Mixin(targets="com.glodblock.github.extendedae.container.ContainerCrystalAssembler",remap=false)
public abstract class EightAssemblerMenuMixin {
    @Shadow @Final private AppEngSlot tank;
    @Inject(method="<init>",at=@At("RETURN"))
    private void amm$capacityTooltip(CallbackInfo ci) {
        if(((AEBaseMenu)(Object)this).getTarget() instanceof NativeEightHost) tank.setEmptyTooltip(()->List.of(
            Component.translatable("gui.extendedae.crystal_assembler.tank_empty"),
            Component.translatable("gui.extendedae.crystal_assembler.amount",0,128000).withStyle(appeng.core.localization.Tooltips.NORMAL_TOOLTIP_TEXT)));
    }
}
