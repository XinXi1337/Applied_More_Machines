package com.appliedmoremachines.integration.jei;

import com.appliedmoremachines.fusion.CoreRecipe;
import com.appliedmoremachines.fusion.CoreRecipes;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Display snapshots only; processing still uses the source recipe's data-aware assembly. */
public record CoreJeiRecipe(CoreRecipe source, ItemStack result) {
    public CoreJeiRecipe { result = result.copy(); }

    public static List<CoreJeiRecipe> get(Level level) {
        if (level == null) return List.of();
        List<CoreJeiRecipe> recipes = new ArrayList<>();
        for (CoreRecipe recipe : CoreRecipes.get(level)) {
            level.getRecipeManager().byKey(recipe.id()).ifPresent(holder -> {
                ItemStack result = holder.value().getResultItem(level.registryAccess());
                if (!result.isEmpty()) recipes.add(new CoreJeiRecipe(recipe, result));
            });
        }
        return List.copyOf(recipes);
    }
}
