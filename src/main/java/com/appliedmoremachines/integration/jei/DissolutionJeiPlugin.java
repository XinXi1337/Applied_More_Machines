package com.appliedmoremachines.integration.jei;

import com.appliedmoremachines.AppliedMoreMachines;
import com.appliedmoremachines.ModBlocks;
import com.appliedmoremachines.dissolution.DissolutionRecipes;
import com.appliedmoremachines.client.DissolutionChamberScreen;
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
import net.neoforged.fml.ModList;

/** JEI and IF remain optional; the machine links to neither JEI nor IF classes. */
@JeiPlugin
public final class DissolutionJeiPlugin implements IModPlugin {
    public static final RecipeType<DissolutionRecipes.Entry> TYPE = RecipeType.create(
            AppliedMoreMachines.MOD_ID, "ae_dissolution_chamber_front", DissolutionRecipes.Entry.class);

    @Override public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(AppliedMoreMachines.MOD_ID, "dissolution_jei");
    }

    private static boolean available() { return ModList.get().isLoaded("industrialforegoing"); }

    @Override public void registerCategories(IRecipeCategoryRegistration registration) {
        if (available()) registration.addRecipeCategories(new DissolutionJeiCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override public void registerRecipes(IRecipeRegistration registration) {
        if (available()) registration.addRecipes(TYPE, DissolutionRecipes.all(Minecraft.getInstance().level));
    }

    @Override public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        if (available()) registration.addRecipeCatalyst(ModBlocks.AE_DISSOLUTION_CHAMBER_FRONT.get(), TYPE);
    }

    @Override public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        if (!available()) return;
        registration.addGuiContainerHandler(DissolutionChamberScreen.class,
                new IGuiContainerHandler<DissolutionChamberScreen>() {
            @Override public Collection<IGuiClickableArea> getGuiClickableAreas(
                    DissolutionChamberScreen screen, double mouseX, double mouseY) {
                if (!screen.isRecipeAreaAvailable()) return List.of();
                return List.of(new IGuiClickableArea() {
                    @Override public Rect2i getArea() {
                        return new Rect2i(94, 36, 24, 18);
                    }
                    @Override public void getTooltip(ITooltipBuilder tooltip) {
                        tooltip.add(Component.translatable("gui.applied_more_machines.show_recipes"));
                    }
                    @Override public void onClick(IFocusFactory focuses, IRecipesGui recipes) {
                        recipes.showTypes(List.of(TYPE));
                    }
                });
            }
        });
    }
}
