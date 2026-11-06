package com.appliedmoremachines.latex;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.Container;

/** Five shared categories, one physical slot per category. Effects are snapshotted per batch. */
public final class LatexUpgrades {
    public static int kind(ItemStack stack) {
        if (stack.isEmpty()) return -1;
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        if (id.equals("ae2:speed_card")) return 0;
        if (id.equals("ae2cs:overload_card")) return 1;
        for (int kind = 2; kind < 5; kind++) {
            String name = new String[]{"speed", "efficiency", "processing"}[kind - 2];
            for (int tier = 3; tier <= 6; tier++)
                if (id.equals("ifeu:" + name + "_addon_" + tier)) return kind;
        }
        return -1;
    }
    public static int limit(ItemStack stack) { return kind(stack) < 2 && kind(stack) >= 0 ? 4 : 1; }
    public static int tier(ItemStack stack) {
        return kind(stack) >= 2 ? Integer.parseInt(BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath().substring(
                BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath().length() - 1)) : 0;
    }
    public static boolean accepts(Container inventory, int slot, ItemStack offered) {
        return accepts(inventory, slot, offered, 2);
    }
    public static boolean accepts(Container inventory, int slot, ItemStack offered, int first) {
        int kind = kind(offered);
        if (slot < first || slot >= first + 5 || kind < 0) return false;
        for (int i = first; i < first + 5; i++) if (i != slot && kind(inventory.getItem(i)) == kind) return false;
        return true;
    }
    public record Stats(int speed, int overload, int speedTier, int efficiencyTier, int processingTier) {
        public int parallel() { return processingTier > 0 ? processingTier + 1 : 1; }
        public int ticks(boolean rubber) {
            double base = rubber ? 160 - 10 * speed - 20 * overload : 1280 - 80 * speed - 160 * overload;
            double speedMultiplier = speedTier > 0 ? 1 - (speedTier - 1) * .1 : 1;
            double efficiencyMultiplier = efficiencyTier > 0 ? 1 - efficiencyTier * .1 : 1;
            return Math.max(1, (int) Math.ceil(Math.max(1, base) * speedMultiplier * efficiencyMultiplier - 1e-9));
        }
    }
    public static Stats stats(Container inventory) {
        return stats(inventory, 2);
    }
    public static Stats stats(Container inventory, int first) {
        int[] values = new int[5];
        for (int i = first; i < first + 5; i++) {
            ItemStack stack = inventory.getItem(i); int kind = kind(stack);
            if (kind >= 0) values[kind] = Math.max(values[kind], kind < 2 ? Math.min(4, stack.getCount()) : tier(stack));
        }
        return new Stats(values[0], values[1], values[2], values[3], values[4]);
    }
    private LatexUpgrades() {}
}
