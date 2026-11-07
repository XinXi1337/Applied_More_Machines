package com.appliedmoremachines.integration;

import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import com.appliedmoremachines.factory.FactoryRecipes;
import com.appliedmoremachines.factory.FactoryMachineTiers;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.ToLongFunction;
import mekanism.api.MekanismAPI;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.recipes.*;
import mekanism.api.recipes.ingredients.ChemicalStackIngredient;
import mekanism.api.recipes.ingredients.FluidStackIngredient;
import mekanism.api.recipes.ingredients.ItemStackIngredient;
import mekanism.common.recipe.MekanismRecipeType;
import mekanism.common.recipe.IMekanismRecipeTypeProvider;
import mekanism.common.tile.machine.TileEntityNutritionalLiquifier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;

/** Native recipe adapter, loaded only when Mekanism is present. */
public final class MekanismFactoryRecipes {
    public static final ResourceLocation SODIUM_HEATING = ResourceLocation.parse("applied_more_machines:sodium_heating");
    public static final ResourceLocation SODIUM = ResourceLocation.parse("mekanism:sodium");
    public static final ResourceLocation SUPERHEATED_SODIUM = ResourceLocation.parse("mekanism:superheated_sodium");
    public static final long SODIUM_AMOUNT = AEFluidKey.AMOUNT_BUCKET;
    private MekanismFactoryRecipes() {}
    private record Need(ToLongFunction<AEKey> amount) {}

    public static FactoryRecipes.Match match(Level level, Set<ResourceLocation> machines,
            Map<AEKey, Long> inputs, Map<AEKey, Long> outputs) {
        var supplemental = sodiumHeating(inputs, outputs);
        if (supplemental != null) return supplemental;
        for (var type : recipeTypes(machines)) for (var holder : type.getRecipeType().getRecipes(level)) {
            var recipe = holder.value();
            if (recipe.isIncomplete()) continue;
            String id = holder.id().toString();
            FactoryRecipes.Match result = nativeMatch(id, recipe, inputs, outputs);
            if (result != null) return result;
        }
        if (machines.stream().map(FactoryMachineTiers::baseMachine)
                .anyMatch(ResourceLocation.parse("mekanism:nutritional_liquifier")::equals)) {
            for (AEKey key : inputs.keySet()) {
                if (!(key instanceof AEItemKey item)) continue;
                var recipe = TileEntityNutritionalLiquifier.getRecipe(item.toStack(1));
                if (recipe != null) {
                    var result = nativeMatch("mekanism:nutritional_liquification/" + key.getId(), recipe, inputs, outputs);
                    if (result != null) return result;
                }
            }
        }
        return null;
    }

    /** Shared by processing and JEI so the fixed structure unlocks exactly the same recipe families. */
    public static Set<IMekanismRecipeTypeProvider<?, ?, ?>> recipeTypes(Set<ResourceLocation> machines) {
        Set<IMekanismRecipeTypeProvider<?, ?, ?>> types = new LinkedHashSet<>();
        for (ResourceLocation actual : machines) {
            ResourceLocation machine = FactoryMachineTiers.baseMachine(actual);
            if (machine.equals(ResourceLocation.fromNamespaceAndPath("mekmm", "ultimate_oxidizing_factory"))) {
                types.add(MekanismRecipeType.OXIDIZING);
                continue;
            }
            if (machine.equals(ResourceLocation.fromNamespaceAndPath("mekmm", "ultimate_crystallizing_factory"))) {
                types.add(MekanismRecipeType.CRYSTALLIZING);
                continue;
            }
            if (!machine.getNamespace().equals("mekanism")) continue;
            switch (machine.getPath()) {
                case "ultimate_smelting_factory" -> types.add(MekanismRecipeType.SMELTING);
                case "ultimate_enriching_factory" -> types.add(MekanismRecipeType.ENRICHING);
                case "ultimate_crushing_factory" -> types.add(MekanismRecipeType.CRUSHING);
                case "ultimate_sawing_factory" -> types.add(MekanismRecipeType.SAWING);
                case "ultimate_purifying_factory" -> types.add(MekanismRecipeType.PURIFYING);
                case "ultimate_injecting_factory" -> types.add(MekanismRecipeType.INJECTING);
                case "ultimate_infusing_factory" -> types.add(MekanismRecipeType.METALLURGIC_INFUSING);
                case "chemical_infuser" -> types.add(MekanismRecipeType.CHEMICAL_INFUSING);
                case "chemical_oxidizer" -> types.add(MekanismRecipeType.OXIDIZING);
                case "chemical_crystallizer" -> types.add(MekanismRecipeType.CRYSTALLIZING);
                case "electrolytic_separator" -> types.add(MekanismRecipeType.SEPARATING);
                case "chemical_dissolution_chamber" -> types.add(MekanismRecipeType.DISSOLUTION);
                case "chemical_washer" -> types.add(MekanismRecipeType.WASHING);
                case "ultimate_compressing_factory" -> types.add(MekanismRecipeType.COMPRESSING);
                case "pressurized_reaction_chamber" -> types.add(MekanismRecipeType.REACTION);
                case "rotary_condensentrator" -> types.add(MekanismRecipeType.ROTARY);
                case "thermal_evaporation_controller" -> types.add(MekanismRecipeType.EVAPORATING);
                default -> { }
            }
        }
        return types;
    }

    /** Factory-only operation: exactly one bucket of chemical sodium becomes superheated sodium. */
    private static FactoryRecipes.Match sodiumHeating(Map<AEKey, Long> inputs, Map<AEKey, Long> outputs) {
        if (inputs.size() != 1 || outputs.size() != 1 || !FactoryRecipes.chemicalsAvailable()) return null;
        var registry = MekanismAPI.CHEMICAL_REGISTRY;
        if (!registry.containsKey(SODIUM) || !registry.containsKey(SUPERHEATED_SODIUM)) return null;
        AEKey input = FactoryChemicalKeys.key(new ChemicalStack(registry.wrapAsHolder(registry.get(SODIUM)), SODIUM_AMOUNT));
        AEKey output = FactoryChemicalKeys.key(new ChemicalStack(registry.wrapAsHolder(registry.get(SUPERHEATED_SODIUM)), SODIUM_AMOUNT));
        if (input == null || output == null || !inputs.equals(Map.of(input, SODIUM_AMOUNT))
                || !outputs.equals(Map.of(output, SODIUM_AMOUNT))) return null;
        return new FactoryRecipes.Match(SODIUM_HEATING.toString(), Map.copyOf(inputs), Map.copyOf(outputs));
    }

    public static long chemicalUsageMultiplier(ItemStackChemicalToObjectRecipe<?> recipe) {
        return recipe.perTickUsage() ? (recipe instanceof ChemicalDissolutionRecipe ? 100 : 200) : 1;
    }

    public static FactoryRecipes.Match nativeMatch(String id, MekanismRecipe<?> recipe,
            Map<AEKey, Long> inputs, Map<AEKey, Long> outputs) {
        if (recipe instanceof ItemStackToItemStackRecipe r) {
            return plan(id, inputs, outputs, List.of(item(r.getInput())), s -> amounts(r.getOutput(item(s.get(0)))));
        }
        if (recipe instanceof ItemStackToChemicalRecipe r) {
            return plan(id, inputs, outputs, List.of(item(r.getInput())), s -> amounts(r.getOutput(item(s.get(0)))));
        }
        if (recipe instanceof ChemicalCrystallizerRecipe r) {
            return plan(id, inputs, outputs, List.of(chemical(r.getInput(), 1)),
                    s -> amounts(r.getOutput(chemical(s.get(0)))));
        }
        if (recipe instanceof SawmillRecipe r) {
            return plan(id, inputs, outputs, List.of(item(r.getInput())), s -> {
                var result = r.getOutput(item(s.get(0)));
                // The user explicitly makes every secondary chance a guaranteed output.
                return amounts(result.getMainOutput(), result.getMaxSecondaryOutput());
            });
        }
        if (recipe instanceof ItemStackChemicalToObjectRecipe<?> r) {
            // Recipe amounts are per tick for these native machines. Their unupgraded
            // operation is 200 ticks (factory) or 100 ticks (dissolution chamber).
            long multiplier = chemicalUsageMultiplier(r);
            return plan(id, inputs, outputs, List.of(item(r.getItemInput()), chemical(r.getChemicalInput(), multiplier)),
                    s -> amounts(r.getOutput(item(s.get(0)), chemical(s.get(1)))));
        }
        if (recipe instanceof ChemicalChemicalToChemicalRecipe r) {
            return plan(id, inputs, outputs, List.of(chemical(r.getLeftInput(), 1), chemical(r.getRightInput(), 1)),
                    s -> amounts(r.getOutput(chemical(s.get(0)), chemical(s.get(1)))));
        }
        if (recipe instanceof ElectrolysisRecipe r) {
            return plan(id, inputs, outputs, List.of(fluid(r.getInput())), s -> {
                var result = r.getOutput(fluid(s.get(0)));
                return amounts(result.left(), result.right());
            });
        }
        if (recipe instanceof FluidChemicalToChemicalRecipe r) {
            return plan(id, inputs, outputs, List.of(fluid(r.getFluidInput()), chemical(r.getChemicalInput(), 1)),
                    s -> amounts(r.getOutput(fluid(s.get(0)), chemical(s.get(1)))));
        }
        if (recipe instanceof FluidToFluidRecipe r) {
            return plan(id, inputs, outputs, List.of(fluid(r.getInput())), s -> amounts(r.getOutput(fluid(s.get(0)))));
        }
        if (recipe instanceof PressurizedReactionRecipe r) {
            return plan(id, inputs, outputs,
                    List.of(item(r.getInputSolid()), fluid(r.getInputFluid()), chemical(r.getInputChemical(), 1)), s -> {
                        var result = r.getOutput(item(s.get(0)), fluid(s.get(1)), chemical(s.get(2)));
                        return amounts(result.item(), result.chemical());
                    });
        }
        if (recipe instanceof RotaryRecipe r) {
            if (r.hasChemicalToFluid()) {
                var result = plan(id + "/to_fluid", inputs, outputs, List.of(chemical(r.getChemicalInput(), 1)),
                        s -> amounts(r.getFluidOutput(chemical(s.get(0)))));
                if (result != null) return result;
            }
            if (r.hasFluidToChemical()) {
                return plan(id + "/to_chemical", inputs, outputs, List.of(fluid(r.getFluidInput())),
                        s -> amounts(r.getChemicalOutput(fluid(s.get(0)))));
            }
        }
        if (recipe instanceof ItemStackToFluidOptionalItemRecipe r) {
            return plan(id, inputs, outputs, List.of(item(r.getInput())), s -> {
                var result = r.getOutput(item(s.get(0)));
                return amounts(result.fluid(), result.optionalItem());
            });
        }
        return null;
    }

    private static Need item(ItemStackIngredient ingredient) {
        return new Need(key -> key instanceof AEItemKey item && ingredient.testType(item.toStack(1))
                ? ingredient.getNeededAmount(item.toStack(1)) : 0);
    }
    private static Need fluid(FluidStackIngredient ingredient) {
        return new Need(key -> key instanceof AEFluidKey fluid && ingredient.testType(fluid.toStack(1))
                ? ingredient.getNeededAmount(fluid.toStack(1)) : 0);
    }
    private static Need chemical(ChemicalStackIngredient ingredient, long multiplier) {
        return new Need(key -> {
            ChemicalStack stack = FactoryChemicalKeys.stack(key, 1);
            return !stack.isEmpty() && ingredient.testType(stack)
                    ? Math.multiplyExact(ingredient.getNeededAmount(stack), multiplier) : 0;
        });
    }
    private static ItemStack item(GenericStack stack) { return ((AEItemKey) stack.what()).toStack(Math.toIntExact(stack.amount())); }
    private static FluidStack fluid(GenericStack stack) { return ((AEFluidKey) stack.what()).toStack(Math.toIntExact(stack.amount())); }
    private static ChemicalStack chemical(GenericStack stack) { return FactoryChemicalKeys.stack(stack.what(), stack.amount()); }

    private static Map<AEKey, Long> amounts(Object... values) {
        Map<AEKey, Long> result = new LinkedHashMap<>();
        for (Object value : values) {
            AEKey key;
            long amount;
            if (value instanceof ItemStack item && !item.isEmpty()) {
                key = AEItemKey.of(item); amount = item.getCount();
            } else if (value instanceof FluidStack fluid && !fluid.isEmpty()) {
                key = AEFluidKey.of(fluid); amount = fluid.getAmount();
            } else if (value instanceof ChemicalStack chemical && !chemical.isEmpty()) {
                key = FactoryChemicalKeys.key(chemical); amount = chemical.getAmount();
                if (key == null) return null;
            } else continue;
            result.merge(key, amount, Math::addExact);
        }
        return result;
    }

    private static FactoryRecipes.Match plan(String id, Map<AEKey, Long> inputs, Map<AEKey, Long> expected,
            List<Need> needs, Function<List<GenericStack>, Map<AEKey, Long>> output) {
        if (assign(0, needs, new LinkedHashMap<>(inputs), new ArrayList<>(), output, expected)) {
            return new FactoryRecipes.Match(id, Map.copyOf(inputs), Map.copyOf(expected));
        }
        return null;
    }

    /** Backtracking handles two ingredients with overlapping chemical tags without reusing material. */
    private static boolean assign(int index, List<Need> needs, Map<AEKey, Long> remaining,
            List<GenericStack> selected, Function<List<GenericStack>, Map<AEKey, Long>> output, Map<AEKey, Long> expected) {
        if (index == needs.size()) {
            if (remaining.values().stream().anyMatch(amount -> amount != 0)) return false;
            return expected.equals(output.apply(selected));
        }
        for (AEKey key : remaining.keySet()) {
            long available = remaining.get(key);
            if (available <= 0) continue;
            long needed = needs.get(index).amount().applyAsLong(key);
            if (needed <= 0 || needed > available) continue;
            remaining.put(key, available - needed);
            selected.add(new GenericStack(key, needed));
            boolean matched = assign(index + 1, needs, remaining, selected, output, expected);
            selected.removeLast();
            remaining.put(key, available);
            if (matched) return true;
        }
        return false;
    }
}
