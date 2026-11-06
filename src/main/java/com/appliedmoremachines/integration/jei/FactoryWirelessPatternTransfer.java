package com.appliedmoremachines.integration.jei;

import de.mari_023.ae2wtlib.wet.WETMenu;
import java.util.Optional;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;

/** Optional WTLib bridge; its wireless and universal terminals share this exact menu class. */
final class FactoryWirelessPatternTransfer implements IRecipeTransferHandler<WETMenu, FactoryProcessingJeiRecipe> {
    private final FactoryPatternTransferHandler delegate;

    private FactoryWirelessPatternTransfer(IRecipeTransferHandlerHelper helper) {
        delegate = new FactoryPatternTransferHandler(helper);
    }

    static void register(IRecipeTransferRegistration registration) {
        registration.addRecipeTransferHandler(new FactoryWirelessPatternTransfer(registration.getTransferHelper()),
                FactoryJeiPlugin.PROCESSING_TYPE);
    }

    @Override public Class<WETMenu> getContainerClass() { return WETMenu.class; }
    @Override public Optional<MenuType<WETMenu>> getMenuType() { return Optional.of(WETMenu.TYPE); }
    @Override public RecipeType<FactoryProcessingJeiRecipe> getRecipeType() { return FactoryJeiPlugin.PROCESSING_TYPE; }

    @Override public IRecipeTransferError transferRecipe(WETMenu menu, FactoryProcessingJeiRecipe recipe,
            IRecipeSlotsView slots, Player player, boolean maxTransfer, boolean doTransfer) {
        return delegate.transferRecipe(menu, recipe, slots, player, maxTransfer, doTransfer);
    }
}
