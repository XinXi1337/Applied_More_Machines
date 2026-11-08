package com.appliedmoremachines.integration.jei;

import de.mari_023.ae2wtlib.wet.WETMenu;
import java.util.Optional;
import javax.annotation.ParametersAreNonnullByDefault;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import org.jetbrains.annotations.Nullable;

/** Optional WTLib bridge; its wireless and universal terminals share this exact menu class. */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class ReactorWirelessPatternTransfer implements IRecipeTransferHandler<WETMenu, ReactorProcessingJeiRecipe> {
    private final ReactorPatternTransferHandler delegate;

    private ReactorWirelessPatternTransfer(IRecipeTransferHandlerHelper helper) {
        delegate = new ReactorPatternTransferHandler(helper);
    }

    static void register(IRecipeTransferRegistration registration) {
        registration.addRecipeTransferHandler(new ReactorWirelessPatternTransfer(registration.getTransferHelper()),
                ReactorJeiPlugin.PROCESSING_TYPE);
    }

    @Override public Class<WETMenu> getContainerClass() { return WETMenu.class; }
    @Override public Optional<MenuType<WETMenu>> getMenuType() { return Optional.of(WETMenu.TYPE); }
    @Override public RecipeType<ReactorProcessingJeiRecipe> getRecipeType() { return ReactorJeiPlugin.PROCESSING_TYPE; }

    // Retain the legacy callback for compatibility with older JEI versions.
    @SuppressWarnings({"removal", "deprecation"})
    @Nullable
    @Override public IRecipeTransferError transferRecipe(WETMenu menu, ReactorProcessingJeiRecipe recipe,
            IRecipeSlotsView slots, Player player, boolean maxTransfer, boolean doTransfer) {
        return delegate.transferRecipe(menu, recipe, slots, player, maxTransfer, doTransfer);
    }
}
