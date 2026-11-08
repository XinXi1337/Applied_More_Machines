package com.appliedmoremachines.production;

import java.util.function.BooleanSupplier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class MultiplicationSlot extends Slot {
    private final BooleanSupplier active;
    public MultiplicationSlot(Object host, int x, int y, BooleanSupplier active) {
        super(host instanceof MultiplicationHost machine ? machine.multiplicationUpgrade() : new MultiplicationUpgrade(null), 0, x, y);
        this.active = active;
    }
    @Override public boolean isActive() { return active.getAsBoolean(); }
    @Override public boolean mayPlace(ItemStack stack) { return isActive() && container.canPlaceItem(0, stack); }
    @Override public boolean mayPickup(Player player) { return isActive(); }
    @Override public int getMaxStackSize() { return 1; }
    @Override public int getMaxStackSize(ItemStack stack) { return 1; }
}
