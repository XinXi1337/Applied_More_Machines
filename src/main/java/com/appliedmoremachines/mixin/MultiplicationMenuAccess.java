package com.appliedmoremachines.mixin;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
@Mixin(AbstractContainerMenu.class)
public interface MultiplicationMenuAccess {
    @Invoker("addSlot") Slot amm$addSlot(Slot slot);
}
