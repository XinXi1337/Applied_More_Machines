package com.appliedmoremachines.brewing;

import com.appliedmoremachines.production.*;

import appeng.api.config.*;
import appeng.api.networking.energy.IAEPowerStorage;
import java.util.*;
import java.util.function.ToLongBiFunction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionBrewing;

/** One atomic three-bottle operation. Snapshots validate live inventory; they never recreate it. */
public final class BrewingProcessor {
    public static final int COST = 16_000;
    private int ticks, duration;
    // Only a failed/partial actual ME insertion leaves an owned remainder here, never in GUI slots.
    private ItemStack pendingReturn = ItemStack.EMPTY;
    public ItemStack takePendingReturn() {
        ItemStack result = pendingReturn; pendingReturn = ItemStack.EMPTY; return result;
    }
    private void flushReturn(Container inventory, ToLongBiFunction<ItemStack, Actionable> insert) {
        if (pendingReturn.isEmpty()) return;
        int accepted = (int) Math.clamp(insert.applyAsLong(pendingReturn.copy(), Actionable.MODULATE), 0L, pendingReturn.getCount());
        if (accepted > 0) { pendingReturn.shrink(accepted); inventory.setChanged(); }
    }
    private NonNullList<ItemStack> snapshot = NonNullList.withSize(5, ItemStack.EMPTY);
    public int ticks() { return ticks; }
    public int duration() { return duration; }
    public void reset() { ticks = 0; duration = 0; snapshot = NonNullList.withSize(5, ItemStack.EMPTY); }

    public void tick(Container inventory, IAEPowerStorage energy, PotionBrewing brewing, boolean active, int normalDuration,
            ToLongBiFunction<ItemStack, Actionable> insertReturn) {
        if (!pendingReturn.isEmpty()) {
            if (active) flushReturn(inventory, insertReturn);
            if (!pendingReturn.isEmpty()) return;
        }
        ItemStack first = inventory.getItem(0);
        boolean equal = first.getCount() == 1;
        for (int i = 1; i < 3; i++) equal &= ItemStack.matches(first, inventory.getItem(i));
        ItemStack ingredient = inventory.getItem(3);
        ItemStack result = equal ? BrewingRecipes.result(brewing, first, ingredient) : ItemStack.EMPTY;
        if (result.isEmpty()) {
            if (duration != 0) { reset(); inventory.setChanged(); }
            return;
        }
        boolean free = first.is(Items.GLASS_BOTTLE);
        int needed = free ? 1 : normalDuration;
        boolean same = duration == needed && ItemStack.matches(snapshot.get(4), result);
        for (int i = 0; i < 4; i++) same &= ItemStack.matches(snapshot.get(i), inventory.getItem(i));
        if (!same) {
            ticks = 0; duration = needed;
            for (int i = 0; i < 4; i++) snapshot.set(i, inventory.getItem(i).copy());
            snapshot.set(4, result.copy()); inventory.setChanged();
        }
        if (!active || !free && energy.getAECurrentPower() < COST) return;
        // Each physical output must have room for exactly its bottle, preserving native stack limits.
        for (int i = 4; i < 7 && (free || !MultiplicationUpgrade.active(inventory)); i++) {
            ItemStack present = inventory.getItem(i);
            if (!present.isEmpty() && (!ItemStack.isSameItemSameComponents(present, result)
                    || present.getCount() >= Math.min(64, result.getMaxStackSize()))) return;
        }
        ItemStack remainder = !free && ingredient.hasCraftingRemainingItem() ? ingredient.getCraftingRemainingItem() : ItemStack.EMPTY;
        if (!remainder.isEmpty() && remainder.getCount() > remainder.getMaxStackSize()) return;
        ticks = Math.min(duration, ticks + 1); inventory.setChanged();
        if (ticks < duration) return;
        // Do not consume this batch until the network reports room for the complete return.
        if (!remainder.isEmpty() && insertReturn.applyAsLong(remainder.copy(), Actionable.SIMULATE) < remainder.getCount()) return;
        if (!free) energy.extractAEPower(COST, Actionable.MODULATE, PowerMultiplier.ONE);
        for (int i = 0; i < 3; i++) {
            inventory.setItem(i, ItemStack.EMPTY);
            if (free || !MultiplicationUpgrade.active(inventory))
                inventory.setItem(4 + i, result.copyWithCount(inventory.getItem(4 + i).getCount() + 1));
        }
        if (!free && MultiplicationUpgrade.active(inventory))
            MultiplicationOutputs.deliver(inventory, new int[]{4,5,6},
                    List.of(result.copyWithCount(Math.toIntExact(MultiplicationUpgrade.boost(3)))));
        if (!free) inventory.setItem(3, ItemStack.EMPTY);
        pendingReturn = remainder.copy();
        reset(); inventory.setChanged();
        // Commit once, then track only the unsent amount if storage changed since simulation.
        flushReturn(inventory, insertReturn);
    }
    public CompoundTag save(HolderLookup.Provider lookup) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Ticks", ticks); tag.putInt("Duration", duration);
        ContainerHelper.saveAllItems(tag, snapshot, lookup);
        if (!pendingReturn.isEmpty()) tag.put("PendingReturn", pendingReturn.save(lookup));
        return tag;
    }
    public void load(CompoundTag tag, HolderLookup.Provider lookup) {
        pendingReturn = ItemStack.parseOptional(lookup, tag.getCompound("PendingReturn"));
        reset(); duration = Math.clamp(tag.getInt("Duration"), 0, 400);
        ticks = Math.clamp(tag.getInt("Ticks"), 0, duration);
        ContainerHelper.loadAllItems(tag, snapshot, lookup);
    }
}
