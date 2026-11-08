package com.appliedmoremachines.integration.jei;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.integration.modules.itemlists.EncodingHelper;
import appeng.menu.me.items.PatternEncodingTermMenu;
import com.appliedmoremachines.energizing.EnergizingRecipes;
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
public final class EnergizingPatternTransferHandler implements IRecipeTransferHandler<PatternEncodingTermMenu, EnergizingRecipes.Entry> {
    private final IRecipeTransferHandlerHelper helper;
    public EnergizingPatternTransferHandler(IRecipeTransferHandlerHelper helper) { this.helper = helper; }
    @Override public Class<PatternEncodingTermMenu> getContainerClass() { return PatternEncodingTermMenu.class; }
    @Override public Optional<MenuType<PatternEncodingTermMenu>> getMenuType() { return Optional.of(PatternEncodingTermMenu.TYPE); }
    @Override public RecipeType<EnergizingRecipes.Entry> getRecipeType() { return EnergizingJeiPlugin.TYPE; }
    public static List<List<GenericStack>> inputs(EnergizingRecipes.Entry recipe) {
        List<List<GenericStack>> result = new ArrayList<>();
        for (var ingredient : recipe.ingredients()) result.add(java.util.Arrays.stream(ingredient.getItems())
                .filter(s -> !s.isEmpty()).map(s -> new GenericStack(Objects.requireNonNull(AEItemKey.of(s)), 1)).toList());
        return result;
    }
    public static List<GenericStack> outputs(EnergizingRecipes.Entry recipe) {
        return List.of(new GenericStack(Objects.requireNonNull(AEItemKey.of(recipe.output())), recipe.output().getCount()));
    }
    @SuppressWarnings({"removal", "deprecation"})
    @Nullable
    @Override public IRecipeTransferError transferRecipe(PatternEncodingTermMenu menu, EnergizingRecipes.Entry recipe,
            IRecipeSlotsView slots, Player player, boolean maxTransfer, boolean doTransfer) {
        if (!EnergizingRecipes.valid(recipe)) return helper.createInternalError();
        var inputs = inputs(recipe); var outputs = outputs(recipe);
        if (inputs.size() > menu.getProcessingInputSlots().length || outputs.size() > menu.getProcessingOutputSlots().length)
            return helper.createInternalError();
        // One native recipe, even for Shift. Rods are catalysts and never encoded.
        if (doTransfer) EncodingHelper.encodeProcessingRecipe(menu, inputs, outputs);
        return null;
    }
}
