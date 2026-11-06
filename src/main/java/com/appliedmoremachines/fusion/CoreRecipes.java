package com.appliedmoremachines.fusion;

import com.appliedmoremachines.integration.DraconicCoreRecipes;
import java.util.Collection;
import java.util.List;
import java.util.WeakHashMap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import net.neoforged.fml.ModList;

public final class CoreRecipes {
    private static final WeakHashMap<RecipeManager, Cache> CACHES = new WeakHashMap<>();
    private CoreRecipes() {}
    public static synchronized void invalidate() { CACHES.clear(); }
    public static synchronized List<CoreRecipe> get(Level level) {
        if (level == null || !ModList.get().isLoaded("draconicevolution")) return List.of();
        var manager = level.getRecipeManager();
        var loaded = manager.getRecipes();
        Cache cached = CACHES.get(manager);
        if (cached == null || cached.loaded != loaded) {
            cached = new Cache(loaded, DraconicCoreRecipes.read(loaded));
            CACHES.put(manager, cached);
        }
        return cached.recipes;
    }
    public static int catalystLimit(Level level, ItemStack stack) {
        return catalystLimit(get(level), stack);
    }
    public static int catalystLimit(List<CoreRecipe> recipes, ItemStack stack) {
        return recipes.stream().filter(CoreRecipe::available)
                .filter(recipe -> CoreRecipe.accepts(recipe.catalyst(), recipe.catalystCount(), stack))
                .mapToInt(CoreRecipe::catalystCount).max().orElse(0);
    }
    public static boolean material(Level level, ItemStack stack) {
        return get(level).stream().anyMatch(recipe -> recipe.parts().stream().anyMatch(
                part -> CoreRecipe.accepts(part.ingredient(), part.count(), stack)));
    }
    private record Cache(Collection<RecipeHolder<?>> loaded, List<CoreRecipe> recipes) {}
}
