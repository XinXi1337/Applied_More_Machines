package com.appliedmoremachines.mixin;
import com.appliedmoremachines.nativeeight.*;
import com.appliedmoremachines.production.MultiplicationUpgrade;
import com.moakiee.ae2lt.machine.overloadfactory.OverloadProcessingFactoryInventory;
import com.moakiee.ae2lt.machine.overloadfactory.recipe.OverloadProcessingRecipe;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import java.util.List;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
@Pseudo @Mixin(targets="com.moakiee.ae2lt.blockentity.OverloadProcessingFactoryBlockEntity",remap=false)
public abstract class EightFactoryMixin {
    @WrapOperation(method="<init>",at=@At(value="NEW",target="(Ljava/lang/Runnable;)Lcom/moakiee/ae2lt/machine/overloadfactory/OverloadProcessingFactoryInventory;"))
    private OverloadProcessingFactoryInventory amm$inventory(Runnable changed,Operation<OverloadProcessingFactoryInventory> original) {
        return (Object)this instanceof NativeEightHost ? new EightFactoryInventory(changed,(NativeEightHost)this) : original.call(changed);
    }
    @ModifyConstant(method={"<init>","canAcceptFluidOutput"},constant=@Constant(intValue=1024000))
    private int amm$tanks(int capacity) { return (Object)this instanceof NativeEightHost ? capacity*8 : capacity; }
    @WrapOperation(method="completeLockedRecipe(Lcom/moakiee/ae2lt/machine/overloadfactory/recipe/OverloadProcessingLockedRecipe;Lcom/moakiee/ae2lt/machine/overloadfactory/recipe/OverloadProcessingRecipeCandidate;)Z",
        at=@At(value="INVOKE",target="Lcom/moakiee/ae2lt/machine/overloadfactory/recipe/OverloadProcessingRecipe;getScaledItemResults(I)Ljava/util/List;"))
    private List<ItemStack> amm$items(OverloadProcessingRecipe recipe,int parallel,Operation<List<ItemStack>> original) {
        if(!((Object)this instanceof NativeEightHost) || !MultiplicationUpgrade.active(this)) return original.call(recipe,parallel);
        return original.call(recipe,1).stream().map(s->MultiplicationUpgrade.product(this,s,parallel)).toList();
    }
    @WrapOperation(method="completeLockedRecipe(Lcom/moakiee/ae2lt/machine/overloadfactory/recipe/OverloadProcessingLockedRecipe;Lcom/moakiee/ae2lt/machine/overloadfactory/recipe/OverloadProcessingRecipeCandidate;)Z",
        at=@At(value="INVOKE",target="Lcom/moakiee/ae2lt/machine/overloadfactory/recipe/OverloadProcessingRecipe;getScaledFluidResult(I)Lnet/neoforged/neoforge/fluids/FluidStack;"))
    private FluidStack amm$fluid(OverloadProcessingRecipe recipe,int parallel,Operation<FluidStack> original) {
        if(!((Object)this instanceof NativeEightHost) || !MultiplicationUpgrade.active(this)) return original.call(recipe,parallel);
        var unit=original.call(recipe,1); return unit.isEmpty()?unit:unit.copyWithAmount(Math.toIntExact(MultiplicationUpgrade.amount(this,unit.getAmount(),parallel)));
    }
}
