package com.appliedmoremachines.mixin;
import com.appliedmoremachines.nativeeight.EightFactoryInventory;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
@Pseudo @Mixin(targets="com.moakiee.ae2lt.machine.overloadfactory.OverloadProcessingFactoryInventory",remap=false)
public abstract class EightFactoryInventoryMixin {
    @ModifyConstant(method={"getOutputCapacityFor","insertIntoOutputArray"},constant=@Constant(intValue=16384))
    private int amm$outputCapacity(int n) { return (Object)this instanceof EightFactoryInventory ? n*8 : n; }
    @ModifyConstant(method="getOutputCapacityFor",constant=@Constant(longValue=16384))
    private long amm$emptyOutputCapacity(long n) { return (Object)this instanceof EightFactoryInventory ? n*8 : n; }
}
