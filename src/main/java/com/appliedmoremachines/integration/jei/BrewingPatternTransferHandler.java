package com.appliedmoremachines.integration.jei;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.integration.modules.itemlists.EncodingHelper;
import appeng.menu.me.items.PatternEncodingTermMenu;
import com.appliedmoremachines.brewing.BrewingRecipes;
import java.util.*;
import javax.annotation.ParametersAreNonnullByDefault;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.*;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import org.jetbrains.annotations.Nullable;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class BrewingPatternTransferHandler implements IRecipeTransferHandler<PatternEncodingTermMenu, BrewingRecipes.Entry> {
    private final IRecipeTransferHandlerHelper helper;
    public BrewingPatternTransferHandler(IRecipeTransferHandlerHelper helper) { this.helper = helper; }
    @Override public Class<PatternEncodingTermMenu> getContainerClass() { return PatternEncodingTermMenu.class; }
    @Override public Optional<MenuType<PatternEncodingTermMenu>> getMenuType() { return Optional.of(PatternEncodingTermMenu.TYPE); }
    @Override public RecipeType<BrewingRecipes.Entry> getRecipeType() { return BrewingJeiPlugin.TYPE; }
    public static List<List<GenericStack>> inputs(BrewingRecipes.Entry recipe) {
        List<List<GenericStack>> result = new ArrayList<>();
        result.add(List.of(new GenericStack(Objects.requireNonNull(AEItemKey.of(recipe.input())), 3)));
        if (!recipe.ingredient().isEmpty()) result.add(List.of(new GenericStack(Objects.requireNonNull(AEItemKey.of(recipe.ingredient())), 1)));
        return result;
    }
    public static List<GenericStack> outputs(BrewingRecipes.Entry recipe) {
        return List.of(new GenericStack(Objects.requireNonNull(AEItemKey.of(recipe.output())), 3));
    }
    @SuppressWarnings({"removal", "deprecation"})
    @Nullable
    @Override public IRecipeTransferError transferRecipe(PatternEncodingTermMenu menu, BrewingRecipes.Entry recipe,
            IRecipeSlotsView slots, Player player, boolean maxTransfer, boolean doTransfer) {
        if (recipe.input().isEmpty() || recipe.output().isEmpty()) return helper.createInternalError();
        var inputs = inputs(recipe); var outputs = outputs(recipe);
        if (inputs.size() > menu.getProcessingInputSlots().length || outputs.size() > menu.getProcessingOutputSlots().length)
            return helper.createInternalError();
        // One batch even for Shift: output slot 0 is the potion key with amount 3, never three amount-1 keys.
        if (doTransfer) EncodingHelper.encodeProcessingRecipe(menu, inputs, outputs);
        return null;
    }
}
