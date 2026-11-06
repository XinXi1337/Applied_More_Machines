package com.appliedmoremachines.integration.jei;

import com.appliedmoremachines.AppliedMoreMachines;
import com.appliedmoremachines.ModBlocks;
import com.appliedmoremachines.client.ReactorControllerScreen;
import com.appliedmoremachines.reactor.ReactorStructures;
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
public final class ReactorJeiPlugin implements IModPlugin {
    public static final RecipeType<ReactorJeiRecipe> TYPE = RecipeType.create(
            AppliedMoreMachines.MOD_ID, "ae_reactors", ReactorJeiRecipe.class);
    public static final RecipeType<ReactorProcessingJeiRecipe> PROCESSING_TYPE = RecipeType.create(
            AppliedMoreMachines.MOD_ID, "ae_reactors_processing", ReactorProcessingJeiRecipe.class);

    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(AppliedMoreMachines.MOD_ID, "factory_jei");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        if (ReactorStructures.enabled()) {
            registration.addRecipeCategories(new ReactorJeiRecipeCategory(registration.getJeiHelpers().getGuiHelper()));
            registration.addRecipeCategories(new ReactorProcessingJeiCategory(registration.getJeiHelpers().getGuiHelper()));
        }
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        if (ReactorStructures.enabled()) {
            registration.addRecipes(TYPE, List.of(ReactorJeiRecipe.create()));
            registration.addRecipes(PROCESSING_TYPE, ReactorProcessingJeiRecipes.get(Minecraft.getInstance().level));
        }
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        if (ReactorStructures.enabled()) {
            registration.addRecipeCatalyst(ModBlocks.AE_REACTORS_PATTERN.get(), TYPE);
            registration.addRecipeCatalyst(ModBlocks.AE_REACTORS_CONTROLLER.get(), TYPE);
            registration.addRecipeCatalyst(ModBlocks.AE_REACTORS_SHELL.get(), TYPE);
            registration.addRecipeCatalyst(ModBlocks.AE_REACTORS_PATTERN.get(), PROCESSING_TYPE);
            registration.addRecipeCatalyst(ModBlocks.AE_REACTORS_CONTROLLER.get(), PROCESSING_TYPE);
            registration.addRecipeCatalyst(ModBlocks.AE_REACTORS_SHELL.get(), PROCESSING_TYPE);
        }
    }

    @Override public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
        if (ReactorStructures.enabled()) {
            registration.addRecipeTransferHandler(new ReactorPatternTransferHandler(registration.getTransferHelper()), PROCESSING_TYPE);
            if (ModList.get().isLoaded("ae2wtlib")) ReactorWirelessPatternTransfer.register(registration);
            if (ModList.get().isLoaded("ae2lt")) ReactorTianshuPatternTransfer.register(registration);
        }
    }

    @Override public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        if (!ReactorStructures.enabled()) return;
        registration.addGuiContainerHandler(ReactorControllerScreen.class, new IGuiContainerHandler<ReactorControllerScreen>() {
            @Override public Collection<IGuiClickableArea> getGuiClickableAreas(
                    ReactorControllerScreen screen, double mouseX, double mouseY) {
                if (!screen.isRecipeAreaAvailable()) return List.of();
                return List.of(new IGuiClickableArea() {
                    @Override public Rect2i getArea() {
                        return new Rect2i(ReactorControllerScreen.RECIPE_AREA_X, ReactorControllerScreen.RECIPE_AREA_Y,
                                ReactorControllerScreen.RECIPE_AREA_WIDTH, ReactorControllerScreen.RECIPE_AREA_HEIGHT);
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
