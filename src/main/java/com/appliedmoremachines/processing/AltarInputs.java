package com.appliedmoremachines.processing;

import java.util.List;
import java.util.function.Consumer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Stable storage indices: old input/output/upgrade saves keep their meaning. */
public final class AltarInputs {
    public static final int COUNT = 9;
    private static final int[] SLOTS = {0, 1, 2, 8, 9, 10, 11, 12, 13};

    private AltarInputs() {}

    public static int slot(int input) { return SLOTS[input]; }

    public static boolean isInput(int slot) {
        return slot >= 0 && slot < 3 || slot >= 8 && slot < 14;
    }

    public static boolean accepts(List<SeedRecipe> recipes, ItemStack stack) {
        return !stack.isEmpty() && recipes.stream().anyMatch(recipe -> recipe.ingredients().stream()
                .anyMatch(ingredient -> SeedRecipe.acceptsIngredient(ingredient, stack)));
    }

    public static boolean accepts(Level level, ItemStack stack) {
        return level != null && accepts(SeedRecipes.get(level), stack);
    }

    /** Old saves may retain excess items, but replacement stacks cannot increase that excess. */
    public static int storageLimit(ItemStack existing, ItemStack incoming) {
        return Math.max(incoming.getMaxStackSize(),
                ItemStack.isSameItemSameComponents(existing, incoming) ? existing.getCount() : 0);
    }

    /** Safely split legacy excess items using the original maximum and components. */
    public static void splitForExit(ItemStack stack, Consumer<ItemStack> destination) {
        ItemStack remaining = stack.copy();
        while (!remaining.isEmpty()) {
            destination.accept(remaining.split(remaining.getMaxStackSize()));
        }
    }
}
