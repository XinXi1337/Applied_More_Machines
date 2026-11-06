package com.appliedmoremachines.processing;

import com.appliedmoremachines.integration.MysticalSeedRecipes;
import java.util.Collection;
import java.util.List;
import java.util.WeakHashMap;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import net.neoforged.fml.ModList;

/** Common entry point: the optional bridge is only invoked when Mystical Agriculture is loaded. */
public final class SeedRecipes {
    private static final WeakHashMap<RecipeManager, Cache> CACHES = new WeakHashMap<>();

    private SeedRecipes() {}

    public static synchronized void invalidate() {
        CACHES.clear();
    }

    public static synchronized List<SeedRecipe> get(Level level) {
        if (!ModList.get().isLoaded("mysticalagriculture")) {
            return List.of();
        }
        RecipeManager manager = level.getRecipeManager();
        Collection<RecipeHolder<?>> loaded = manager.getRecipes();
        Cache cache = CACHES.get(manager);
        if (cache == null || cache.loaded != loaded) {
            cache = new Cache(loaded, MysticalSeedRecipes.read(level));
            CACHES.put(manager, cache);
        }
        return cache.recipes;
    }

    private record Cache(Collection<RecipeHolder<?>> loaded, List<SeedRecipe> recipes) {}
}
