package com.appliedmoremachines.integration;

import com.appliedmoremachines.fishing.FishingLoot;
import cy.jdkdigital.productivebees.common.recipe.BeeFishingRecipe;
import cy.jdkdigital.productivebees.init.ModRecipeTypes;
import cy.jdkdigital.productivebees.util.BeeCreator;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

/** Optional event-based fishing: return real bee eggs without spawning world entities. */
public final class ProductiveBeesFishing {
    private ProductiveBeesFishing() {}

    public static List<ItemStack> roll(ServerLevel level, int selection) {
        List<BeeFishingRecipe> hits = new ArrayList<>();
        for (var holder : level.getRecipeManager().getAllRecipesFor(ModRecipeTypes.BEE_FISHING_TYPE.get())) {
            BeeFishingRecipe recipe = holder.value();
            // Productive Bees 13.14.0 performs an initial roll plus one extra at
            // neutral fishing luck. Keep that probability, ignoring biome only.
            boolean caught = level.random.nextDouble() < recipe.chance;
            if (!caught) caught = level.random.nextDouble() < recipe.chance;
            if (caught) hits.add(recipe);
        }
        if (hits.isEmpty()) return List.of();
        var recipe = hits.get(level.random.nextInt(hits.size()));
        ItemStack egg = BeeCreator.getSpawnEgg(recipe.output.get().getBeeType());
        return FishingLoot.filterResult(List.of(egg), selection, Set.of());
    }
}
