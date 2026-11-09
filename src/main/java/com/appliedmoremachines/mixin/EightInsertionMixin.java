package com.appliedmoremachines.mixin;
import appeng.api.inventories.InternalInventory;
import com.appliedmoremachines.nativeeight.EightInventories;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
@Mixin(value=InternalInventory.class,remap=false)
public interface EightInsertionMixin {
    @WrapOperation(method="insertItem",at=@At(value="INVOKE",target="Lnet/minecraft/world/item/ItemStack;getMaxStackSize()I"))
    default int amm$itemLimit(ItemStack stack,Operation<Integer> original) {
        return EightInventories.expanded(this) ? EightInventories.itemLimit(this,stack) : original.call(stack);
    }
}
