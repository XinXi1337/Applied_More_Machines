package com.appliedmoremachines.brewing;

import com.appliedmoremachines.InfusionUpgradeCards;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

/** Two shared slots; each card type may occupy only one slot, up to four cards. */
public final class BrewingUpgrades {
    public static boolean isCard(ItemStack stack) { return InfusionUpgradeCards.accepts(stack); }
    public static boolean accepts(Container inventory, int slot, ItemStack stack) {
        if (slot < 7 || slot > 8 || !isCard(stack)) return false;
        ItemStack other = inventory.getItem(slot == 7 ? 8 : 7);
        return !isCard(other) || InfusionUpgradeCards.isOverload(other) != InfusionUpgradeCards.isOverload(stack);
    }
    public static int duration(int speed, int overload) {
        return Math.max(1, 400 - 20 * Math.clamp(speed, 0, 4) - 80 * Math.clamp(overload, 0, 4));
    }
    public static int duration(Container inventory) {
        int speed = 0, overload = 0;
        for (int i = 7; i < 9; i++) {
            ItemStack stack = inventory.getItem(i);
            if (!isCard(stack)) continue;
            if (InfusionUpgradeCards.isOverload(stack)) overload += Math.min(4, stack.getCount());
            else speed += Math.min(4, stack.getCount());
        }
        return duration(speed, overload);
    }
    private BrewingUpgrades() {}
}
