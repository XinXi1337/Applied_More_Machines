package com.appliedmoremachines.fusion;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

/** Equipment whitelist, without linking the optional Draconic Evolution API. */
public final class CoreInjectors {
    public static final int MAX_COUNT = 16;
    private CoreInjectors() {}

    public static int tier(ItemStack stack) {
        if (stack.isEmpty()) return -1;
        var id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (!id.getNamespace().equals("draconicevolution")) return -1;
        return switch (id.getPath()) {
            case "basic_crafting_injector" -> 0;
            case "wyvern_crafting_injector" -> 1;
            case "awakened_crafting_injector" -> 2;
            case "chaotic_crafting_injector" -> 3;
            default -> -1;
        };
    }

    public static int unlocked(ItemStack stack) {
        return tier(stack) < 0 ? 0 : Math.min(MAX_COUNT, stack.getCount());
    }

    public static int duration(int speed, int overload) {
        return Math.max(1, 160 - 20 * speed - 40 * overload);
    }
}
