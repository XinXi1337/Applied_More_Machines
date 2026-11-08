package com.appliedmoremachines.integration.jei;

import com.appliedmoremachines.AppliedMoreMachines;
import com.appliedmoremachines.ModBlocks;
import com.appliedmoremachines.energizing.EnergizingRecipes;
import com.appliedmoremachines.client.AEEnergizingScreen;
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

/** Powah-gated category; JEI stays isolated from common machine classes. */
@JeiPlugin
public final class EnergizingJeiPlugin implements IModPlugin {
    public static final RecipeType<EnergizingRecipes.Entry> TYPE = RecipeType.create(
            AppliedMoreMachines.MOD_ID, "ae_energizing_orb", EnergizingRecipes.Entry.class);

    @Override public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(AppliedMoreMachines.MOD_ID, "energizing_jei");
    }

    private static boolean available() { return ModBlocks.AE_ENERGIZING_ORB != null; }

    @Override public void registerCategories(IRecipeCategoryRegistration registration) {
        if (available()) registration.addRecipeCategories(new EnergizingJeiCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override public void registerRecipes(IRecipeRegistration registration) {
        if (available()) registration.addRecipes(TYPE, EnergizingRecipes.all(Minecraft.getInstance().level));
    }

    @Override public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        if (available()) registration.addRecipeCatalyst(ModBlocks.AE_ENERGIZING_ORB.get(), TYPE);
    }

    @Override public void registerRecipeTransferHandlers(mezz.jei.api.registration.IRecipeTransferRegistration registration) {
        if (!available()) return;
        registration.addRecipeTransferHandler(new EnergizingPatternTransferHandler(registration.getTransferHelper()), TYPE);
        if (ModList.get().isLoaded("ae2wtlib")) EnergizingWirelessPatternTransfer.register(registration);
        if (ModList.get().isLoaded("ae2lt")) EnergizingTianshuPatternTransfer.register(registration);
    }

    @Override public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        if (!available()) return;
        registration.addGuiContainerHandler(AEEnergizingScreen.class,
                new IGuiContainerHandler<AEEnergizingScreen>() {
            @Override public Collection<IGuiClickableArea> getGuiClickableAreas(
                    AEEnergizingScreen screen, double mouseX, double mouseY) {
                if (!screen.isRecipeAreaAvailable()) return List.of();
                return List.of(new IGuiClickableArea() {
                    @Override public Rect2i getArea() {
                        return new Rect2i(108, 40, 28, 20);
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
