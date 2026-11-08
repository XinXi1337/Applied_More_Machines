package com.appliedmoremachines.inscriber;

import appeng.api.upgrades.IUpgradeInventory;
import com.appliedmoremachines.production.MultiplicationUpgrade;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

/** Native upgrades retain their slots; multiplication belongs exclusively to the extra slot. */
public record InscriberUpgradeInventory(IUpgradeInventory delegate) implements IUpgradeInventory {
    private static boolean multiplication(ItemLike item) { return MultiplicationUpgrade.isCard(new ItemStack(item)); }
    @Override public ItemLike getUpgradableItem() { return delegate.getUpgradableItem(); }
    @Override public int size() { return delegate.size(); }
    @Override public int getSlotLimit(int slot) { return delegate.getSlotLimit(slot); }
    @Override public ItemStack getStackInSlot(int slot) { return delegate.getStackInSlot(slot); }
    @Override public void setItemDirect(int slot, ItemStack stack) { delegate.setItemDirect(slot, stack); }
    @Override public boolean isItemValid(int slot, ItemStack stack) { return !MultiplicationUpgrade.isCard(stack) && delegate.isItemValid(slot, stack); }
    @Override public int getInstalledUpgrades(ItemLike item) { return multiplication(item) ? 0 : delegate.getInstalledUpgrades(item); }
    @Override public int getMaxInstalled(ItemLike item) { return multiplication(item) ? 0 : delegate.getMaxInstalled(item); }
    @Override public void readFromNBT(CompoundTag tag, String name, HolderLookup.Provider lookup) { delegate.readFromNBT(tag, name, lookup); }
    @Override public void writeToNBT(CompoundTag tag, String name, HolderLookup.Provider lookup) { delegate.writeToNBT(tag, name, lookup); }
    @Override public void sendChangeNotification(int slot) { delegate.sendChangeNotification(slot); }
}
