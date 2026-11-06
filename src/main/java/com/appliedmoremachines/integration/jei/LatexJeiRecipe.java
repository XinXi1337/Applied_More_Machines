package com.appliedmoremachines.integration.jei;

import com.appliedmoremachines.AppliedMoreMachines;
import com.appliedmoremachines.latex.LatexRecipes;
import com.appliedmoremachines.latex.LatexUpgrades;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluids;

/** Base, single-operation snapshots of the same recipes used by the machine. */
public record LatexJeiRecipe(ResourceLocation id, List<ItemStack> logs, int latex, int water, ItemStack result) {
    private static final LatexUpgrades.Stats BASE = new LatexUpgrades.Stats(0, 0, 0, 0, 0);

    public LatexJeiRecipe {
        logs = logs.stream().map(ItemStack::copy).toList();
        result = result.copy();
    }

    public boolean extraction() { return result.isEmpty(); }
    public int ticks() { return BASE.ticks(!extraction()); }

    public static List<LatexJeiRecipe> get(Level level) {
        if (level == null || BuiltInRegistries.FLUID.get(LatexRecipes.LATEX) == Fluids.EMPTY) return List.of();
        return snapshots(LatexRecipes.all(level), LatexRecipes.rubber());
    }

    public static List<LatexJeiRecipe> snapshots(List<LatexRecipes.Extraction> extractions, LatexRecipes.Rubber rubber) {
        List<LatexJeiRecipe> result = new ArrayList<>();
        for (var recipe : extractions) {
            // A default/tag recipe must not advertise logs handled by a higher-priority recipe.
            var logs = Arrays.stream(recipe.input().getItems())
                    .filter(stack -> LatexRecipes.find(extractions, stack) == recipe)
                    .map(stack -> stack.copyWithCount(1)).toList();
            if (!logs.isEmpty() && recipe.amount() > 0) {
                result.add(new LatexJeiRecipe(ResourceLocation.fromNamespaceAndPath(AppliedMoreMachines.MOD_ID,
                        "latex/extraction/" + recipe.id().getNamespace() + "/" + recipe.id().getPath()),
                        logs, recipe.amount(), 0, ItemStack.EMPTY));
            }
        }
        if (rubber != null && rubber.latex() > 0 && rubber.water() > 0 && !rubber.result().isEmpty()) {
            result.add(new LatexJeiRecipe(ResourceLocation.fromNamespaceAndPath(AppliedMoreMachines.MOD_ID,
                    "latex/rubber"), List.of(), rubber.latex(), rubber.water(), rubber.result()));
        }
        return List.copyOf(result);
    }
}
