package com.appliedmoremachines.laser;

import com.appliedmoremachines.latex.LatexUpgrades;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

/** The same exact-position rules drive menu placement, Shift transfer and processing. */
public final class FluidLaserSlots {
    public static final int CATALYSTS = 19, LENS = 19, DRILL = 22, UPGRADE = 25, HEAL = 30, SIZE = 31;
    private static final String[] FIXED = {
        "minecraft:wither_skeleton_skull", "minecraft:wither_skeleton_skull", "minecraft:wither_skeleton_skull",
        "minecraft:soul_sand", "minecraft:soul_sand", "minecraft:soul_sand", "minecraft:soul_sand",
        "minecraft:sculk_shrieker", "minecraft:sculk", "minecraft:deepslate",
        "minecraft:end_crystal", "minecraft:obsidian", "minecraft:end_crystal",
        "minecraft:obsidian", "minecraft:dragon_egg", "minecraft:obsidian",
        "minecraft:end_crystal", "minecraft:obsidian", "minecraft:end_crystal",
        "industrialforegoing:purple_laser_lens", "ifeu:laser_lens_sculk", "ifeu:laser_lens_dragon",
        "industrialforegoing:laser_drill", "industrialforegoing:laser_drill", "industrialforegoing:laser_drill"
    };
    public static ResourceLocation expected(int slot) { return ResourceLocation.parse(FIXED[slot]); }
    public static boolean matches(int slot, ItemStack stack) {
        return slot >= 0 && slot < UPGRADE && !stack.isEmpty()
                && BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(expected(slot));
    }
    public static int healingTier(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        for (int tier = 1; tier <= 6; tier++) if (id.equals("ifeu:heal_addon_" + tier)) return tier;
        return 0;
    }
    public static int rateLimit(int tier) { return tier == 6 ? Integer.MAX_VALUE : 80 << Math.clamp(tier, 0, 5); }
    public static int limit(int slot, ItemStack stack) {
        if (slot >= DRILL && slot < UPGRADE) return 36;
        if (slot >= UPGRADE && slot < HEAL) return LatexUpgrades.limit(stack);
        return 1;
    }
    public static boolean accepts(Container inventory, int slot, ItemStack stack) {
        if (slot < 0 || slot >= SIZE || stack.isEmpty()) return false;
        if (slot < UPGRADE) return matches(slot, stack);
        if (slot == HEAL) return healingTier(stack) > 0;
        return LatexUpgrades.accepts(inventory, slot, stack, UPGRADE);
    }
    public static boolean ready(Container inventory, int lane) {
        if (lane < 0 || lane >= 3 || !matches(LENS + lane, inventory.getItem(LENS + lane))
                || inventory.getItem(LENS + lane).getCount() != 1
                || !matches(DRILL + lane, inventory.getItem(DRILL + lane))
                || inventory.getItem(DRILL + lane).getCount() < 1
                || inventory.getItem(DRILL + lane).getCount() > 36) return false;
        int first = lane == 0 ? 0 : lane == 1 ? 7 : 10;
        int end = lane == 0 ? 7 : lane == 1 ? 10 : 19;
        for (int i = first; i < end; i++)
            if (!matches(i, inventory.getItem(i)) || inventory.getItem(i).getCount() != 1) return false;
        return true;
    }
    private FluidLaserSlots() {}
}
