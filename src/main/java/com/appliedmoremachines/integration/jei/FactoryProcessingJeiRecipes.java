package com.appliedmoremachines.integration.jei;

import com.appliedmoremachines.factory.FactoryStructure;
import com.appliedmoremachines.integration.MekanismFactoryRecipes;
import com.appliedmoremachines.integration.jei.FactoryProcessingJeiRecipe.Cell;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import mekanism.api.MekanismAPI;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.recipes.*;
import mekanism.api.recipes.ingredients.ChemicalStackIngredient;
import mekanism.common.tile.machine.TileEntityNutritionalLiquifier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;

/** Mekanism-only adapter. Viewing chemical recipes does not require the optional AE chemical bridge. */
public final class FactoryProcessingJeiRecipes {
    private FactoryProcessingJeiRecipes() {}

    public static List<FactoryProcessingJeiRecipe> get(Level level) {
        if (level == null) return List.of();
        Set<ResourceLocation> machines = FactoryStructure.definition().cells().stream()
                .map(FactoryStructure.Cell::block).collect(Collectors.toSet());
        List<FactoryProcessingJeiRecipe> result = new ArrayList<>();
        for (var type : MekanismFactoryRecipes.recipeTypes(machines)) {
            for (var holder : type.getRecipeType().getRecipes(level)) result.addAll(display(holder.id(), holder.value()));
        }
        if (machines.contains(ResourceLocation.parse("mekanism:nutritional_liquifier"))) {
            for (var item : BuiltInRegistries.ITEM) {
                var recipe = TileEntityNutritionalLiquifier.getRecipe(new ItemStack(item));
                var itemId = BuiltInRegistries.ITEM.getKey(item);
                if (recipe != null) result.addAll(display(ResourceLocation.fromNamespaceAndPath("mekanism",
                        "nutritional_liquification/" + itemId.getNamespace() + "/" + itemId.getPath()), recipe));
            }
        }
        var registry = MekanismAPI.CHEMICAL_REGISTRY;
        if (registry.containsKey(MekanismFactoryRecipes.SODIUM) && registry.containsKey(MekanismFactoryRecipes.SUPERHEATED_SODIUM)) {
            add(result, MekanismFactoryRecipes.SODIUM_HEATING,
                    List.of(cell(List.of(new ChemicalStack(registry.wrapAsHolder(registry.get(MekanismFactoryRecipes.SODIUM)), MekanismFactoryRecipes.SODIUM_AMOUNT)))),
                    List.of(cell(List.of(new ChemicalStack(registry.wrapAsHolder(registry.get(MekanismFactoryRecipes.SUPERHEATED_SODIUM)), MekanismFactoryRecipes.SODIUM_AMOUNT)))));
        }
        return List.copyOf(result);
    }

    /** Uses the same full-operation amounts and guaranteed secondary outputs as the processor. */
    public static List<FactoryProcessingJeiRecipe> display(ResourceLocation id, MekanismRecipe<?> recipe) {
        List<FactoryProcessingJeiRecipe> result = new ArrayList<>();
        if (recipe.isIncomplete()) return result;
        if (recipe instanceof ItemStackToItemStackRecipe r) {
            add(result, id, List.of(cell(r.getInput().getRepresentations())), List.of(cell(r.getOutputDefinition())));
        } else if (recipe instanceof ItemStackToChemicalRecipe r) {
            add(result, id, List.of(cell(r.getInput().getRepresentations())), List.of(cell(r.getOutputDefinition())));
        } else if (recipe instanceof ChemicalCrystallizerRecipe r) {
            add(result, id, List.of(chemical(r.getInput(), 1)), List.of(cell(r.getOutputDefinition())));
        } else if (recipe instanceof SawmillRecipe r) {
            var secondary = r.getInput().getRepresentations().stream().map(s -> r.getOutput(s).getMaxSecondaryOutput()).toList();
            add(result, id, List.of(cell(r.getInput().getRepresentations())),
                    List.of(cell(r.getMainOutputDefinition()), cell(secondary)));
        } else if (recipe instanceof ItemStackChemicalToObjectRecipe<?> r) {
            add(result, id, List.of(cell(r.getItemInput().getRepresentations()),
                    chemical(r.getChemicalInput(), MekanismFactoryRecipes.chemicalUsageMultiplier(r))), List.of(cell(r.getOutputDefinition())));
        } else if (recipe instanceof ChemicalChemicalToChemicalRecipe r) {
            add(result, id, List.of(chemical(r.getLeftInput(), 1), chemical(r.getRightInput(), 1)), List.of(cell(r.getOutputDefinition())));
        } else if (recipe instanceof ElectrolysisRecipe r) {
            add(result, id, List.of(cell(r.getInput().getRepresentations())), List.of(
                    cell(r.getOutputDefinition().stream().map(ElectrolysisRecipe.ElectrolysisRecipeOutput::left).toList()),
                    cell(r.getOutputDefinition().stream().map(ElectrolysisRecipe.ElectrolysisRecipeOutput::right).toList())));
        } else if (recipe instanceof FluidChemicalToChemicalRecipe r) {
            add(result, id, List.of(cell(r.getFluidInput().getRepresentations()), chemical(r.getChemicalInput(), 1)), List.of(cell(r.getOutputDefinition())));
        } else if (recipe instanceof FluidToFluidRecipe r) {
            add(result, id, List.of(cell(r.getInput().getRepresentations())), List.of(cell(r.getOutputDefinition())));
        } else if (recipe instanceof PressurizedReactionRecipe r) {
            add(result, id, List.of(cell(r.getInputSolid().getRepresentations()), cell(r.getInputFluid().getRepresentations()), chemical(r.getInputChemical(), 1)),
                    List.of(cell(r.getOutputDefinition().stream().map(PressurizedReactionRecipe.PressurizedReactionRecipeOutput::item).toList()),
                            cell(r.getOutputDefinition().stream().map(PressurizedReactionRecipe.PressurizedReactionRecipeOutput::chemical).toList())));
        } else if (recipe instanceof RotaryRecipe r) {
            if (r.hasChemicalToFluid()) add(result, id.withSuffix("/to_fluid"), List.of(chemical(r.getChemicalInput(), 1)), List.of(cell(r.getFluidOutputDefinition())));
            if (r.hasFluidToChemical()) add(result, id.withSuffix("/to_chemical"), List.of(cell(r.getFluidInput().getRepresentations())), List.of(cell(r.getChemicalOutputDefinition())));
        } else if (recipe instanceof ItemStackToFluidOptionalItemRecipe r) {
            add(result, id, List.of(cell(r.getInput().getRepresentations())), List.of(
                    cell(r.getOutputDefinition().stream().map(ItemStackToFluidOptionalItemRecipe.FluidOptionalItemOutput::fluid).toList()),
                    cell(r.getOutputDefinition().stream().map(ItemStackToFluidOptionalItemRecipe.FluidOptionalItemOutput::optionalItem).toList())));
        }
        return result;
    }

    private static Cell chemical(ChemicalStackIngredient ingredient, long multiplier) {
        return cell(ingredient.getRepresentations().stream()
                .map(stack -> stack.copyWithAmount(Math.multiplyExact(stack.getAmount(), multiplier))).toList());
    }

    private static Cell cell(List<?> values) {
        List<Object> copies = new ArrayList<>();
        for (Object value : values) {
            if (value instanceof ItemStack stack && !stack.isEmpty()) copies.add(stack.copy());
            else if (value instanceof FluidStack stack && !stack.isEmpty()) copies.add(stack.copy());
            else if (value instanceof ChemicalStack stack && !stack.isEmpty()) copies.add(stack.copy());
        }
        return new Cell(copies);
    }

    private static void add(List<FactoryProcessingJeiRecipe> result, ResourceLocation id, List<Cell> inputs, List<Cell> outputs) {
        var display = new FactoryProcessingJeiRecipe(id, inputs, outputs.stream().filter(cell -> !cell.alternatives().isEmpty()).toList());
        if (display.fits()) result.add(display);
    }
}
