package com.appliedmoremachines.mixin;
import com.moakiee.ae2lt.menu.OverloadProcessingFactoryMenu;
import com.appliedmoremachines.production.MultiplicationSlot;
import appeng.menu.SlotSemantic;
import net.minecraft.world.inventory.Slot;
import java.util.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
@Pseudo @Mixin(targets="com.moakiee.ae2lt.client.machine.OverloadProcessingFactoryScreen",remap=false)
public abstract class EightFactoryScreenMixin {
    @WrapOperation(method="<init>",at=@At(value="INVOKE",target="Lcom/moakiee/ae2lt/menu/OverloadProcessingFactoryMenu;getSlots(Lappeng/menu/SlotSemantic;)Ljava/util/List;"))
    private List<Slot> amm$panel(OverloadProcessingFactoryMenu menu,SlotSemantic semantic,Operation<List<Slot>> original) {
        var slots=new ArrayList<>(original.call(menu,semantic));
        for(var slot:menu.slots) if(slot instanceof MultiplicationSlot) slots.add(slot);
        return slots;
    }
}
