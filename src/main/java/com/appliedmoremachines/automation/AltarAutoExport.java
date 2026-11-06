package com.appliedmoremachines.automation;

import com.appliedmoremachines.InfusionOutputSide;
import com.appliedmoremachines.InfusionPedestaBlockEntity;
import java.util.function.Function;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

/** Only remove products actually accepted by a selected adjacent inventory. */
public final class AltarAutoExport {
    private AltarAutoExport() {}

    public static void tick(Container inventory, boolean enabled, int sides, Direction front,
            Function<Direction, IItemHandler> adjacent) {
        if (!enabled || sides == 0) return;
        for (InfusionOutputSide side : InfusionOutputSide.values()) {
            ItemStack output = inventory.getItem(InfusionPedestaBlockEntity.OUTPUT_SLOT);
            if (output.isEmpty()) return;
            if ((sides & side.mask()) == 0) continue;
            IItemHandler target = adjacent.apply(side.worldDirection(front));
            if (target == null) continue;
            ItemStack remaining = ItemHandlerHelper.insertItemStacked(target, output.copy(), false);
            if (remaining.getCount() < output.getCount()) {
                inventory.setItem(InfusionPedestaBlockEntity.OUTPUT_SLOT, remaining);
            }
        }
    }
}
