package com.appliedmoremachines.mixin;
import appeng.util.inv.AppEngInternalInventory;
import com.appliedmoremachines.nativeeight.EightInventories;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.HolderLookup;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
@Mixin(value=AppEngInternalInventory.class, remap=false)
public abstract class EightInventoryMixin {
    @Inject(method="getSlotLimit",at=@At("RETURN"),cancellable=true)
    private void amm$limit(int slot,CallbackInfoReturnable<Integer> cir) {
        if(EightInventories.expanded(this)) cir.setReturnValue(Math.multiplyExact(cir.getReturnValue(),8));
    }
    @WrapMethod(method="writeToNBT")
    private void amm$save(CompoundTag tag,String name,HolderLookup.Provider lookup,Operation<Void> original) {
        EightInventories.save((AppEngInternalInventory)(Object)this,tag,name,()->original.call(tag,name,lookup));
    }
    @Inject(method="readFromNBT",at=@At("RETURN"))
    private void amm$load(CompoundTag tag,String name,HolderLookup.Provider lookup,CallbackInfo ci) {
        EightInventories.load((AppEngInternalInventory)(Object)this,tag,name);
    }
}
