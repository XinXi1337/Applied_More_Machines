package com.appliedmoremachines.processing;

import java.util.List;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Respect native slot limits, including when extracting legacy oversized stacks. */
public final class AltarTransfers {
    private AltarTransfers() {}

    public static boolean move(ItemStack source, List<Slot> targets) {
        int before = source.getCount();
        for (int pass = 0; pass < 2; pass++) {
            for (Slot target : targets) {
                if (source.isEmpty()) return true;
                if (!target.isActive() || !target.mayPlace(source)) continue;
                ItemStack existing = target.getItem();
                if (pass == 0 && !existing.isEmpty() && ItemStack.isSameItemSameComponents(source, existing)) {
                    int amount = Math.max(0, Math.min(source.getCount(), target.getMaxStackSize(source) - existing.getCount()));
                    existing.grow(amount);
                    source.shrink(amount);
                    target.setChanged();
                } else if (pass == 1 && existing.isEmpty()) {
                    target.setByPlayer(source.split(Math.min(source.getCount(), target.getMaxStackSize(source))));
                }
            }
        }
        return source.getCount() < before;
    }
}
