package com.appliedmoremachines.integration.jei;

import com.appliedmoremachines.AppliedMoreMachines;
import com.appliedmoremachines.ModBlocks;
import com.appliedmoremachines.client.CraftingCoreScreen;
import java.util.Collection;
import java.util.List;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.handlers.IGuiClickableArea;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.recipe.IFocusFactory;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.runtime.IRecipesGui;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** Discovered only by JEI; no optional JEI types are linked from the machine or screen. */
@JeiPlugin
public final class CoreJeiPlugin implements IModPlugin {
    public static final RecipeType<CoreJeiRecipe> TYPE = RecipeType.create(
            AppliedMoreMachines.MOD_ID, "ae_crafting_core", CoreJeiRecipe.class);

    @Override public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(AppliedMoreMachines.MOD_ID, "core_jei");
    }

    @Override public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new CoreRecipeCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(TYPE, CoreJeiRecipe.get(Minecraft.getInstance().level));
    }

    @Override public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(ModBlocks.AE_CRAFTING_CORE.get(), TYPE);
    }

    @Override public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGuiContainerHandler(CraftingCoreScreen.class, new IGuiContainerHandler<CraftingCoreScreen>() {
            @Override public Collection<IGuiClickableArea> getGuiClickableAreas(
                    CraftingCoreScreen screen, double mouseX, double mouseY) {
                if (!screen.isRecipeAreaAvailable()) return List.of();
                return List.of(new IGuiClickableArea() {
                    @Override public Rect2i getArea() {
                        return new Rect2i(CraftingCoreScreen.RECIPE_AREA_X, CraftingCoreScreen.RECIPE_AREA_Y,
                                CraftingCoreScreen.RECIPE_AREA_WIDTH, CraftingCoreScreen.RECIPE_AREA_HEIGHT);
                    }

                    @Override public void getTooltip(ITooltipBuilder tooltip) {
                        tooltip.add(Component.translatable("gui.applied_more_machines.show_recipes"));
                        tooltip.add(Component.translatable("gui.applied_more_machines.progress", screen.getMenu().progressPercent()));
                    }

                    @Override public void onClick(IFocusFactory focuses, IRecipesGui recipes) {
                        recipes.showTypes(List.of(TYPE));
                    }
                });
            }
        });
    }
}
