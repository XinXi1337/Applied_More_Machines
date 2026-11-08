package com.appliedmoremachines.inscriber;

import appeng.api.inventories.InternalInventory;
import appeng.client.gui.Icon;
import appeng.menu.slot.OutputSlot;
import java.util.Optional;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** The native output-slot partial-pickup guard assumes a single vanilla-sized stack. */
public final class InscriberOutputSlot extends OutputSlot {
    public InscriberOutputSlot(InternalInventory inventory, int slot, Icon icon) { super(inventory, slot, icon); }

    @Override public Optional<ItemStack> tryRemove(int requested, int limit, Player player) {
        if (!mayPickup(player) || requested <= 0 || limit <= 0) return Optional.empty();
        var taken = remove(Math.min(requested, Math.min(limit, getItem().getMaxStackSize())));
        if (taken.isEmpty()) return Optional.empty();
        if (!hasItem()) setByPlayer(ItemStack.EMPTY, taken);
        return Optional.of(taken);
    }
}
