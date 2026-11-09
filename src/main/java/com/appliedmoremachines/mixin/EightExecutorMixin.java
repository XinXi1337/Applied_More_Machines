package com.appliedmoremachines.mixin;

import appeng.api.inventories.InternalInventory;
import com.appliedmoremachines.nativeeight.NativeEightHost;
import com.appliedmoremachines.production.MultiplicationUpgrade;
import com.glodblock.github.extendedae.api.IRecipeMachine;
import com.glodblock.github.glodium.recipe.RecipeSearchContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import com.llamalad7.mixinextras.sugar.Local;

@Pseudo @Mixin(targets="com.glodblock.github.extendedae.util.RecipeExecutor",remap=false)
public abstract class EightExecutorMixin {
    @Shadow @Final private IRecipeMachine<?,?> machine;

    @ModifyExpressionValue(method="execute",at=@At(value="INVOKE",target="Lnet/minecraft/world/item/ItemStack;copy()Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack amm$product(ItemStack result) {
        return machine instanceof NativeEightHost ? MultiplicationUpgrade.product(machine,result,1) : result;
    }

    // Keep the native clock/payment. At completion, commit up to seven more copies
    // of the same recipe, checking current materials, fluid and full output space each time.
    @SuppressWarnings({"rawtypes", "unchecked"})
    @WrapOperation(method="execute",at=@At(value="INVOKE",target="Lappeng/api/inventories/InternalInventory;insertItem(ILnet/minecraft/world/item/ItemStack;Z)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack amm$parallel(InternalInventory output,int slot,ItemStack unit,boolean simulate,
            Operation<ItemStack> original,@Local RecipeHolder recipe) {
        ItemStack remainder=original.call(output,slot,unit,simulate);
        if(simulate || !(machine instanceof NativeEightHost) || !remainder.isEmpty()) return remainder;
        RecipeSearchContext context=machine.getContext();
        for(int copy=1;copy<8;copy++) {
            if(!context.testRecipe(recipe) || !original.call(output,slot,unit,true).isEmpty()) break;
            context.runRecipe(recipe);
            original.call(output,slot,unit,false);
        }
        return remainder;
    }
}
