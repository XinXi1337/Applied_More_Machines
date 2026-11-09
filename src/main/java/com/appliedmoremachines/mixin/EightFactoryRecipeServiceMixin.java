package com.appliedmoremachines.mixin;
import com.appliedmoremachines.nativeeight.EightFactoryInventory;
import com.moakiee.ae2lt.machine.overloadfactory.OverloadProcessingFactoryInventory;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import com.llamalad7.mixinextras.sugar.Local;
@Pseudo @Mixin(targets="com.moakiee.ae2lt.machine.overloadfactory.recipe.OverloadProcessingRecipeService",remap=false)
public abstract class EightFactoryRecipeServiceMixin {
    @ModifyConstant(method="canAcceptOutputs",constant=@Constant(intValue=1024000))
    private static int amm$fluidCapacity(int n,@Local(argsOnly=true) OverloadProcessingFactoryInventory inv) { return inv instanceof EightFactoryInventory ? n*8 : n; }
    @ModifyConstant(method="maxOutputParallel",constant=@Constant(longValue=1024000))
    private static long amm$parallelCapacity(long n,@Local(argsOnly=true) OverloadProcessingFactoryInventory inv) { return inv instanceof EightFactoryInventory ? n*8 : n; }

    @com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation(method="maxOutputParallel",at=@At(value="INVOKE",target="Lcom/moakiee/ae2lt/machine/overloadfactory/recipe/OverloadProcessingRecipe;rawItemResults()Ljava/util/List;"))
    private static java.util.List<net.minecraft.world.item.ItemStack> amm$unitItems(com.moakiee.ae2lt.machine.overloadfactory.recipe.OverloadProcessingRecipe recipe,
            com.llamalad7.mixinextras.injector.wrapoperation.Operation<java.util.List<net.minecraft.world.item.ItemStack>> original,
            @Local(argsOnly=true) OverloadProcessingFactoryInventory inv) {
        var result=original.call(recipe);
        return inv instanceof EightFactoryInventory eight && eight.multiplied() ? result.stream().map(s->s.copyWithCount(Math.toIntExact(com.appliedmoremachines.production.MultiplicationUpgrade.boost(s.getCount())))).toList() : result;
    }
    @com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation(method="maxOutputParallel",at=@At(value="INVOKE",target="Lcom/moakiee/ae2lt/machine/overloadfactory/recipe/OverloadProcessingRecipe;rawFluidResult()Lnet/neoforged/neoforge/fluids/FluidStack;"))
    private static net.neoforged.neoforge.fluids.FluidStack amm$unitFluid(com.moakiee.ae2lt.machine.overloadfactory.recipe.OverloadProcessingRecipe recipe,
            com.llamalad7.mixinextras.injector.wrapoperation.Operation<net.neoforged.neoforge.fluids.FluidStack> original,
            @Local(argsOnly=true) OverloadProcessingFactoryInventory inv) {
        var result=original.call(recipe);
        return inv instanceof EightFactoryInventory eight && eight.multiplied() && !result.isEmpty() ? result.copyWithAmount(Math.toIntExact(com.appliedmoremachines.production.MultiplicationUpgrade.boost(result.getAmount()))) : result;
    }
    @com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation(method="canAcceptOutputs",at=@At(value="INVOKE",target="Lcom/moakiee/ae2lt/machine/overloadfactory/recipe/OverloadProcessingRecipe;getScaledItemResults(I)Ljava/util/List;"))
    private static java.util.List<net.minecraft.world.item.ItemStack> amm$scaledItems(com.moakiee.ae2lt.machine.overloadfactory.recipe.OverloadProcessingRecipe recipe,int copies,
            com.llamalad7.mixinextras.injector.wrapoperation.Operation<java.util.List<net.minecraft.world.item.ItemStack>> original,
            @Local(argsOnly=true) OverloadProcessingFactoryInventory inv) {
        if(!(inv instanceof EightFactoryInventory eight) || !eight.multiplied()) return original.call(recipe,copies);
        return original.call(recipe,1).stream().map(s->s.copyWithCount(Math.toIntExact(Math.multiplyExact(com.appliedmoremachines.production.MultiplicationUpgrade.boost(s.getCount()),copies)))).toList();
    }
    @com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation(method="canAcceptOutputs",at=@At(value="INVOKE",target="Lcom/moakiee/ae2lt/machine/overloadfactory/recipe/OverloadProcessingRecipe;getScaledFluidResult(I)Lnet/neoforged/neoforge/fluids/FluidStack;"))
    private static net.neoforged.neoforge.fluids.FluidStack amm$scaledFluid(com.moakiee.ae2lt.machine.overloadfactory.recipe.OverloadProcessingRecipe recipe,int copies,
            com.llamalad7.mixinextras.injector.wrapoperation.Operation<net.neoforged.neoforge.fluids.FluidStack> original,
            @Local(argsOnly=true) OverloadProcessingFactoryInventory inv) {
        if(!(inv instanceof EightFactoryInventory eight) || !eight.multiplied()) return original.call(recipe,copies);
        var result=original.call(recipe,1);
        return result.isEmpty()?result:result.copyWithAmount(Math.toIntExact(Math.multiplyExact(com.appliedmoremachines.production.MultiplicationUpgrade.boost(result.getAmount()),copies)));
    }
}
