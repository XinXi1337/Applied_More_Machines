package com.appliedmoremachines.nativeeight;
import appeng.api.inventories.InternalInventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import java.util.*;
public final class EightInventories {
    private static final Set<InternalInventory> EXPANDED = Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));
    private EightInventories() {}
    public static void track(InternalInventory... inventories) { Collections.addAll(EXPANDED, inventories); }
    public static boolean expanded(Object inventory) { return EXPANDED.contains(inventory); }
    public static int itemLimit(Object inventory, ItemStack stack) {
        return Math.multiplyExact(stack.getMaxStackSize(), expanded(inventory) ? 8 : 1);
    }
    public static void save(InternalInventory inv, CompoundTag tag, String name, Runnable original) {
        if (!expanded(inv)) { original.run(); return; }
        saveContents(inv, tag, name, original);
    }
    public static void saveContents(InternalInventory inv, CompoundTag tag, String name, Runnable original) {
        int[] counts = new int[inv.size()];
        ItemStack[] stacks = new ItemStack[inv.size()];
        for (int i=0;i<counts.length;i++) { stacks[i]=inv.getStackInSlot(i); counts[i]=stacks[i].getCount(); }
        try {
            for (var stack: stacks) if (!stack.isEmpty()) stack.setCount(1);
            original.run();
        } finally {
            for (int i=0;i<counts.length;i++) if (counts[i]>0) stacks[i].setCount(counts[i]);
        }
        tag.putIntArray(name + "_amm_x8_counts", counts);
    }
    public static void load(InternalInventory inv, CompoundTag tag, String name) {
        if (!expanded(inv)) return;
        loadContents(inv, tag, name);
    }
    public static void loadContents(InternalInventory inv, CompoundTag tag, String name) {
        int[] counts=tag.getIntArray(name + "_amm_x8_counts");
        for (int i=0;i<Math.min(inv.size(),counts.length);i++)
            if (!inv.getStackInSlot(i).isEmpty()) inv.getStackInSlot(i).setCount(Math.max(0,counts[i]));
    }
    public static void splitDrops(List<ItemStack> source, List<ItemStack> target) {
        for (var stack:source) for (int left=stack.getCount();left>0;) {
            int n=Math.min(left,stack.getMaxStackSize()); target.add(stack.copyWithCount(n)); left-=n;
        }
    }
}
