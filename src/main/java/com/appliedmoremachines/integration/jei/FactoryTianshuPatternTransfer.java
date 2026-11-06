package com.appliedmoremachines.integration.jei;

import com.moakiee.ae2lt.client.tianshu.TianshuRecipeTransferContext;
import com.moakiee.ae2lt.menu.TianshuPatternEncodingTermMenu;
import com.moakiee.ae2lt.menu.TianshuWirelessPatternEncodingTermMenu;
import java.util.Optional;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.fml.ModList;

/** Optional AE2LT bridge: JEI resolves the exact menu class, not its AE2 superclass. */
final class FactoryTianshuPatternTransfer<M extends TianshuPatternEncodingTermMenu>
        implements IRecipeTransferHandler<M, FactoryProcessingJeiRecipe> {
    private final Class<M> menuClass;
    private final MenuType<M> menuType;
    private final FactoryPatternTransferHandler delegate;

    private FactoryTianshuPatternTransfer(Class<M> menuClass, MenuType<M> menuType,
            IRecipeTransferHandlerHelper helper) {
        this.menuClass = menuClass;
        this.menuType = menuType;
        this.delegate = new FactoryPatternTransferHandler(helper);
    }

    static void register(IRecipeTransferRegistration registration) {
        registration.addRecipeTransferHandler(new FactoryTianshuPatternTransfer<>(
                TianshuPatternEncodingTermMenu.class, TianshuPatternEncodingTermMenu.TYPE,
                registration.getTransferHelper()), FactoryJeiPlugin.PROCESSING_TYPE);
        if (ModList.get().isLoaded("ae2wtlib_api") || ModList.get().isLoaded("ae2wtlib")) Wireless.register(registration);
    }

    private static final class Wireless {
        static void register(IRecipeTransferRegistration registration) {
            registration.addRecipeTransferHandler(new FactoryTianshuPatternTransfer<>(
                    TianshuWirelessPatternEncodingTermMenu.class, TianshuWirelessPatternEncodingTermMenu.TYPE,
                    registration.getTransferHelper()), FactoryJeiPlugin.PROCESSING_TYPE);
        }
    }

    @Override public Class<M> getContainerClass() { return menuClass; }
    @Override public Optional<MenuType<M>> getMenuType() { return Optional.of(menuType); }
    @Override public RecipeType<FactoryProcessingJeiRecipe> getRecipeType() { return FactoryJeiPlugin.PROCESSING_TYPE; }

    @Override public IRecipeTransferError transferRecipe(M menu, FactoryProcessingJeiRecipe recipe,
            IRecipeSlotsView slots, Player player, boolean maxTransfer, boolean doTransfer) {
        var error = delegate.transferRecipe(menu, recipe, slots, player, maxTransfer, false);
        if (error != null || !doTransfer) return error;
        // Discard the previous recipe's special encoding state before writing a strict native-ratio pattern.
        TianshuRecipeTransferContext.clear(menu);
        menu.resetProcessingEncoding();
        return delegate.transferRecipe(menu, recipe, slots, player, maxTransfer, true);
    }
}
