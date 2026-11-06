package com.appliedmoremachines.integration;

import com.appliedmoremachines.processing.SeedRecipe;
import com.blakebr0.mysticalagriculture.api.crafting.IInfusionRecipe;
import com.blakebr0.mysticalagriculture.api.crafting.IAwakeningRecipe;
import net.neoforged.neoforge.common.crafting.DataComponentIngredient;
import com.blakebr0.mysticalagriculture.api.crop.ICropProvider;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Collection;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;

/** All optional mod types stay in this bridge. Reads loaded recipes, never fabricates crop materials. */
public final class MysticalSeedRecipes {
    private MysticalSeedRecipes() {}

    public static List<SeedRecipe> read(Level level) {
        return read(level.getRecipeManager().getRecipes(), level.registryAccess());
    }

    public static List<SeedRecipe> read(Collection<RecipeHolder<?>> loaded, HolderLookup.Provider registries) {
        List<SeedRecipe> infusion = new ArrayList<>();
        List<SeedRecipe> crafting = new ArrayList<>();
        for (var holder : loaded) {
            var recipe = holder.value();
            ItemStack result = recipe.getResultItem(registries);
            if (result.isEmpty()) continue;
            // This applies to altar recipes too, not just the crafting-only seed fallback.
            if (result.getItem() instanceof ICropProvider provider
                    && (provider.getCrop() == null || !provider.getCrop().isEnabled())) continue;
            if (recipe instanceof IInfusionRecipe original) {
                if (original.getAltarIngredient().hasNoItems()) continue;
                List<Ingredient> ingredients = new ArrayList<>();
                ingredients.add(original.getAltarIngredient());
                ingredients.addAll(original.getIngredients());
                infusion.add(new SeedRecipe(holder.id(), original, List.copyOf(ingredients), ingredients.size(), 1));
            } else if (recipe instanceof IAwakeningRecipe original) {
                if (original.getAltarIngredient().hasNoItems()) continue;
                List<Ingredient> ingredients = new ArrayList<>();
                List<Integer> quantities = new ArrayList<>();
                ingredients.add(original.getAltarIngredient());
                quantities.add(1);
                // Native source view: center, four pedestals, four counted essence vessels.
                for (int pedestal = 0; pedestal < 4; pedestal++) {
                    ingredients.add(original.getIngredients().get(pedestal * 2 + 1));
                    quantities.add(1);
                }
                for (ItemStack essence : original.getEssences()) {
                    ingredients.add(DataComponentIngredient.of(true, essence));
                    quantities.add(essence.getCount());
                }
                infusion.add(new SeedRecipe(holder.id(), original, ingredients, 9, 1, quantities));
            } else if (recipe instanceof CraftingRecipe original && isSupportedSeed(result)) {
                // Includes inferium seeds, which have a crafting recipe rather than an infusion recipe.
                int width = original instanceof ShapedRecipe shaped ? shaped.getWidth() : original.getIngredients().size();
                int height = original instanceof ShapedRecipe shaped ? shaped.getHeight() : 1;
                if (width > 0 && height > 0) {
                    crafting.add(new SeedRecipe(holder.id(), original, List.copyOf(original.getIngredients()), width, height));
                }
            }
        }
        infusion.sort(Comparator.comparing(recipe -> recipe.id().toString()));
        crafting.sort(Comparator.comparing(recipe -> recipe.id().toString()));
        infusion.addAll(crafting);
        return infusion.stream().filter(SeedRecipe::hasAvailableIngredients).toList();
    }

    private static boolean isSupportedSeed(ItemStack result) {
        if (!(result.getItem() instanceof ICropProvider provider)) return false;
        var crop = provider.getCrop();
        String namespace = crop.getId().getNamespace();
        int tier = crop.getTier().getValue();
        return (namespace.equals("mysticalagriculture") || namespace.equals("mysticalagradditions"))
                && crop.isEnabled() && tier >= 1 && tier <= 6 && result.getItem() == crop.getSeedsItem();
    }
}
