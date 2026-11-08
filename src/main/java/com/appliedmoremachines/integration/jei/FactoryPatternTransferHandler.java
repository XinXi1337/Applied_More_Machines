package com.appliedmoremachines.integration.jei;

import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.integration.modules.itemlists.EncodingHelper;
import appeng.menu.me.items.PatternEncodingTermMenu;
import com.appliedmoremachines.integration.FactoryChemicalKeys;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import mekanism.api.chemical.ChemicalStack;
import javax.annotation.ParametersAreNonnullByDefault;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

/** Encode the factory's full-operation amounts through AE's normal ghost-slot packets. */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class FactoryPatternTransferHandler
        implements IRecipeTransferHandler<PatternEncodingTermMenu, FactoryProcessingJeiRecipe> {
    private final IRecipeTransferHandlerHelper helper;

    public FactoryPatternTransferHandler(IRecipeTransferHandlerHelper helper) { this.helper = helper; }
    @Override public Class<PatternEncodingTermMenu> getContainerClass() { return PatternEncodingTermMenu.class; }
    @Override public Optional<MenuType<PatternEncodingTermMenu>> getMenuType() { return Optional.of(PatternEncodingTermMenu.TYPE); }
    @Override public RecipeType<FactoryProcessingJeiRecipe> getRecipeType() { return FactoryJeiPlugin.PROCESSING_TYPE; }

    // Retain the legacy callback for compatibility with older JEI versions.
    @SuppressWarnings({"removal", "deprecation"})
    @Nullable
    @Override public IRecipeTransferError transferRecipe(PatternEncodingTermMenu menu, FactoryProcessingJeiRecipe recipe,
            IRecipeSlotsView slots, Player player, boolean maxTransfer, boolean doTransfer) {
        FactoryTransferDiagnostics.handler(menu, recipe, doTransfer);
        if (!recipe.fits() || recipe.inputs().size() > menu.getProcessingInputSlots().length
                || recipe.outputs().size() > menu.getProcessingOutputSlots().length) return helper.createInternalError();
        List<List<GenericStack>> inputs = new ArrayList<>();
        List<GenericStack> outputs = new ArrayList<>();
        for (var cell : recipe.inputs()) {
            var alternatives = cell.alternatives().stream().map(FactoryPatternTransferHandler::stack).toList();
            if (alternatives.contains(null)) return unavailable();
            inputs.add(alternatives);
        }
        var outputSlots = slots.getSlotViews(RecipeIngredientRole.OUTPUT);
        for (int index = 0; index < recipe.outputs().size(); index++) {
            var alternatives = recipe.outputs().get(index).alternatives().stream()
                    .map(FactoryPatternTransferHandler::stack).toList();
            if (alternatives.contains(null)) return unavailable();
            GenericStack output = alternatives.getFirst();
            if (index < outputSlots.size()) {
                var displayed = outputSlots.get(index).getDisplayedIngredient()
                        .map(value -> stack(value.getIngredient())).orElse(null);
                if (alternatives.contains(displayed)) output = displayed;
            }
            outputs.add(output);
        }
        // A transfer only fills a processing pattern. Shift must not multiply its native ratio.
        if (doTransfer) {
            EncodingHelper.encodeProcessingRecipe(menu, inputs, outputs);
            FactoryTransferDiagnostics.sent(recipe);
        }
        return null;
    }

    private IRecipeTransferError unavailable() {
        return helper.createUserErrorWithTooltip(Component.translatable("gui.applied_more_machines.factory.waiting_chemical"));
    }

    @Nullable
    private static GenericStack stack(Object value) {
        AEKey key;
        long amount;
        if (value instanceof ItemStack item && !item.isEmpty()) {
            key = AEItemKey.of(item); amount = item.getCount();
        } else if (value instanceof FluidStack fluid && !fluid.isEmpty()) {
            key = AEFluidKey.of(fluid); amount = fluid.getAmount();
        } else if (value instanceof ChemicalStack chemical && !chemical.isEmpty()) {
            key = FactoryChemicalKeys.key(chemical); amount = chemical.getAmount();
        } else return null;
        return key == null || amount <= 0 ? null : new GenericStack(key, amount);
    }
}
