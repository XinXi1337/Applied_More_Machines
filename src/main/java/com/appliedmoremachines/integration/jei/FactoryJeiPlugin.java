package com.appliedmoremachines.integration.jei;

import com.appliedmoremachines.AppliedMoreMachines;
import com.appliedmoremachines.ModBlocks;
import com.appliedmoremachines.client.FactoryControllerScreen;
import com.appliedmoremachines.factory.FactoryStructures;
import java.util.Collection;
import java.util.List;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.handlers.IGuiClickableArea;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.recipe.IFocusFactory;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.runtime.IRecipesGui;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;

/** Optional structure and processing pages; Mekanism adapters are reached only after the availability guard. */
@JeiPlugin
public final class FactoryJeiPlugin implements IModPlugin {
    public static final RecipeType<FactoryJeiRecipe> TYPE = RecipeType.create(
            AppliedMoreMachines.MOD_ID, "ae_factory", FactoryJeiRecipe.class);
    public static final RecipeType<FactoryProcessingJeiRecipe> PROCESSING_TYPE = RecipeType.create(
            AppliedMoreMachines.MOD_ID, "ae_factory_processing", FactoryProcessingJeiRecipe.class);

    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(AppliedMoreMachines.MOD_ID, "factory_jei");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        if (FactoryStructures.enabled()) {
            registration.addRecipeCategories(new FactoryJeiRecipeCategory(registration.getJeiHelpers().getGuiHelper()));
            registration.addRecipeCategories(new FactoryProcessingJeiCategory(registration.getJeiHelpers().getGuiHelper()));
        }
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        if (FactoryStructures.enabled()) {
            registration.addRecipes(TYPE, List.of(FactoryJeiRecipe.create()));
            registration.addRecipes(PROCESSING_TYPE, FactoryProcessingJeiRecipes.get(Minecraft.getInstance().level));
        }
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        if (FactoryStructures.enabled()) {
            registration.addRecipeCatalyst(ModBlocks.AE_FACTORY_PATTERN.get(), TYPE);
            registration.addRecipeCatalyst(ModBlocks.AE_FACTORY_CONTROLLER.get(), TYPE);
            registration.addRecipeCatalyst(ModBlocks.AE_FACTORY_CASING.get(), TYPE);
            registration.addRecipeCatalyst(ModBlocks.AE_FACTORY_PATTERN.get(), PROCESSING_TYPE);
            registration.addRecipeCatalyst(ModBlocks.AE_FACTORY_CONTROLLER.get(), PROCESSING_TYPE);
            registration.addRecipeCatalyst(ModBlocks.AE_FACTORY_CASING.get(), PROCESSING_TYPE);
        }
    }

    @Override public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
        if (FactoryStructures.enabled()) {
            registration.addRecipeTransferListener(FactoryTransferDiagnostics.listener());
            registration.addRecipeTransferHandler(new FactoryPatternTransferHandler(registration.getTransferHelper()), PROCESSING_TYPE);
            if (ModList.get().isLoaded("ae2wtlib")) FactoryWirelessPatternTransfer.register(registration);
            if (ModList.get().isLoaded("ae2lt")) FactoryTianshuPatternTransfer.register(registration);
        }
    }

    @Override public void onRuntimeAvailable(IJeiRuntime runtime) {
        if (FactoryStructures.enabled()) FactoryTransferDiagnostics.runtime(runtime);
    }

    @Override public void onRuntimeUnavailable() {
        FactoryTransferDiagnostics.runtime(null);
    }

    @Override public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        if (!FactoryStructures.enabled()) return;
        registration.addGuiContainerHandler(FactoryControllerScreen.class, new IGuiContainerHandler<FactoryControllerScreen>() {
            @Override public Collection<IGuiClickableArea> getGuiClickableAreas(
                    FactoryControllerScreen screen, double mouseX, double mouseY) {
                if (!screen.isRecipeAreaAvailable()) return List.of();
                return List.of(new IGuiClickableArea() {
                    @Override public Rect2i getArea() {
                        return new Rect2i(FactoryControllerScreen.RECIPE_AREA_X, FactoryControllerScreen.RECIPE_AREA_Y,
                                FactoryControllerScreen.RECIPE_AREA_WIDTH, FactoryControllerScreen.RECIPE_AREA_HEIGHT);
                    }

                    @Override public void getTooltip(ITooltipBuilder tooltip) {
                        tooltip.add(Component.translatable("gui.applied_more_machines.show_recipes"));
                    }

                    @Override public void onClick(IFocusFactory focuses, IRecipesGui recipes) {
                        recipes.showTypes(List.of(PROCESSING_TYPE));
                    }
                });
            }
        });
    }
}
