package com.appliedmoremachines.mixin;
import appeng.menu.slot.AppEngSlot;
import com.appliedmoremachines.nativeeight.EightInventories;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(value=AppEngSlot.class,remap=false)
public abstract class EightSlotMixin {
    @Inject(method="getMaxStackSize(Lnet/minecraft/world/item/ItemStack;)I",at=@At("HEAD"),cancellable=true)
    private void amm$limit(ItemStack stack,CallbackInfoReturnable<Integer> cir) {
        var slot=(AppEngSlot)(Object)this;
        if(EightInventories.expanded(slot.getInventory())) cir.setReturnValue(Math.min(slot.getMaxStackSize(),EightInventories.itemLimit(slot.getInventory(),stack)));
    }
    @ModifyVariable(method="remove",at=@At("HEAD"),argsOnly=true)
    private int amm$takeNativeStack(int amount) {
        var slot=(AppEngSlot)(Object)this;
        return EightInventories.expanded(slot.getInventory()) ? Math.min(amount,slot.getItem().getMaxStackSize()) : amount;
    }
}
