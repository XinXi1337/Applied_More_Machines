package com.appliedmoremachines.integration.jei;

import appeng.recipes.AERecipeTypes;
import com.appliedmoremachines.ModBlocks;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;

/** Reuse the installed AE2 JEI integration's native recipes, screen and transfers. */
@JeiPlugin
public final class ParallelInscriberJeiPlugin implements IModPlugin {
    @Override public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath("applied_more_machines", "parallel_inscriber");
    }

    @Override public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        if (ModList.get().isLoaded("ae2jeiintegration")) {
            var type = RecipeType.createFromVanilla(AERecipeTypes.INSCRIBER);
            registration.addRecipeCatalyst(ModBlocks.INSCRIBER_X8.get(), type);
            registration.addRecipeCatalyst(ModBlocks.INSCRIBER_X64.get(), type);
        }
    }
}
