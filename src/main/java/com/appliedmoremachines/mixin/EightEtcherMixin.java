package com.appliedmoremachines.mixin;

import com.appliedmoremachines.nativeeight.NativeEightHost;
import com.appliedmoremachines.production.MultiplicationUpgrade;
import appeng.util.inv.AppEngInternalInventory;
import io.github.lounode.ae2cs.common.recipe.circuit_etcher.CircuitEtcherRecipe;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import com.llamalad7.mixinextras.sugar.Local;

@Pseudo @Mixin(targets="io.github.lounode.ae2cs.common.block.entity.CircuitEtcherBlockEntity",remap=false)
public abstract class EightEtcherMixin {
    @Shadow private int[] activeMatch;
    @Shadow protected abstract boolean consumeInputs(CircuitEtcherRecipe recipe,int[] match);

    @WrapOperation(method="serverTick",at=@At(value="INVOKE",target="Lappeng/util/inv/AppEngInternalInventory;insertItem(ILnet/minecraft/world/item/ItemStack;Z)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack amm$product(AppEngInternalInventory inv,int slot,ItemStack stack,boolean simulate,
            Operation<ItemStack> original,@Local CircuitEtcherRecipe recipe) {
        if(!((Object)this instanceof NativeEightHost)) return original.call(inv,slot,stack,simulate);
        ItemStack unit=MultiplicationUpgrade.product(this,stack,1);
        ItemStack remainder=original.call(inv,slot,unit,simulate);
        if(simulate || !remainder.isEmpty()) return remainder;
        // The original first copy already paid for this cycle and consumed its inputs.
        // Reuse its matching slots; only counts change during this synchronous commit.
        int[] match=activeMatch.clone();
        for(int copy=1;copy<8;copy++) {
            if(!original.call(inv,slot,unit,true).isEmpty() || !consumeInputs(recipe,match)) break;
            original.call(inv,slot,unit,false);
        }
        return remainder;
    }
}
