package com.appliedmoremachines.integration.jei;

import com.appliedmoremachines.AppliedMoreMachines;
import com.appliedmoremachines.ModBlocks;
import com.appliedmoremachines.client.InfusionPedestaScreen;
import com.appliedmoremachines.processing.SeedRecipe;
import com.appliedmoremachines.processing.SeedRecipes;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.gui.handlers.IGuiClickableArea;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.recipe.IFocusFactory;
import mezz.jei.api.runtime.IRecipesGui;
import java.util.Collection;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** Discovered by JEI only; common startup never links to this optional client API. */
@JeiPlugin
public final class AltarJeiPlugin implements IModPlugin {
    public static final RecipeType<SeedRecipe> TYPE = RecipeType.create(
            AppliedMoreMachines.MOD_ID, "ae_infusion_altar", SeedRecipe.class);

    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(AppliedMoreMachines.MOD_ID, "altar_jei");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new AltarRecipeCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        var level = Minecraft.getInstance().level;
        if (level != null) registration.addRecipes(TYPE, SeedRecipes.get(level));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(ModBlocks.AE_INFUSION_PEDESTA.get(), TYPE);
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGuiContainerHandler(InfusionPedestaScreen.class, new IGuiContainerHandler<InfusionPedestaScreen>() {
            private final IGuiClickableArea area = new IGuiClickableArea() {
                @Override public Rect2i getArea() {
                    return new Rect2i(InfusionPedestaScreen.RECIPE_AREA_X, InfusionPedestaScreen.RECIPE_AREA_Y,
                            InfusionPedestaScreen.RECIPE_AREA_WIDTH, InfusionPedestaScreen.RECIPE_AREA_HEIGHT);
                }

                @Override public void getTooltip(ITooltipBuilder tooltip) {
                    tooltip.add(Component.translatable("gui.applied_more_machines.show_recipes"));
                }

                @Override public void onClick(IFocusFactory focuses, IRecipesGui recipes) {
                    recipes.showTypes(List.of(TYPE));
                }
            };

            @Override public Collection<IGuiClickableArea> getGuiClickableAreas(
                    InfusionPedestaScreen screen, double mouseX, double mouseY) {
                return screen.isRecipeAreaAvailable() ? List.of(area) : List.of();
            }
        });
    }
}
