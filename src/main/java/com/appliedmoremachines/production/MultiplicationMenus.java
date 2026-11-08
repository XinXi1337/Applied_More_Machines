package com.appliedmoremachines.production;

import com.appliedmoremachines.processing.AltarTransfers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class MultiplicationMenus {
    private MultiplicationMenus() {}
    /** Null delegates to the original machine transfer logic. */
    public static ItemStack quickMove(AbstractContainerMenu menu, Player player, int index) {
        if (index < 0 || index >= menu.slots.size()) return null;
        var dedicated = menu.slots.stream().filter(s -> s instanceof MultiplicationSlot).findFirst().orElse(null);
        if (dedicated == null) return null;
        Slot source = menu.slots.get(index);
        boolean outgoing = source == dedicated;
        if (!outgoing && !(source.container == player.getInventory() && MultiplicationUpgrade.isCard(source.getItem()))) return null;
        if (!source.isActive() || !source.mayPickup(player) || !source.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = source.getItem(), before = stack.copy();
        var targets = outgoing ? menu.slots.stream().filter(s -> s.container == player.getInventory()).toList().reversed()
                : java.util.List.of(dedicated);
        if (!AltarTransfers.move(stack, targets)) return ItemStack.EMPTY;
        source.setByPlayer(stack.isEmpty() ? ItemStack.EMPTY : stack);
        source.onTake(player, stack);
        return before;
    }
}
