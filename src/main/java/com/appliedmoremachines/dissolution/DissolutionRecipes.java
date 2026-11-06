package com.appliedmoremachines.dissolution;

import java.util.*;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;

/** Optional IF bridge. Only recipes present in the world's manager are imported. */
public final class DissolutionRecipes {
    public record Entry(ResourceLocation id, List<Ingredient> inputs, SizedFluidIngredient fluid,
                        ItemStack output, FluidStack outputFluid) {
        public Entry {
            inputs = List.copyOf(inputs); output = output.copy(); outputFluid = outputFluid.copy();
        }
        public CompoundTag definition(HolderLookup.Provider lookup) {
            CompoundTag tag = new CompoundTag();
            tag.putString("Id", id.toString());
            ListTag ingredients = new ListTag();
            for (Ingredient ingredient : inputs) {
                ListTag choices = new ListTag();
                for (ItemStack stack : ingredient.getItems()) choices.add(stack.save(lookup));
                ingredients.add(choices);
            }
            tag.put("Inputs", ingredients);
            // Preserve the ingredient codec too: two predicates may expose identical display candidates.
            tag.put("Predicates", Ingredient.CODEC.listOf().encodeStart(
                    lookup.createSerializationContext(NbtOps.INSTANCE), inputs).getOrThrow());
            tag.put("FluidPredicate", SizedFluidIngredient.FLAT_CODEC.encodeStart(
                    lookup.createSerializationContext(NbtOps.INSTANCE), fluid).getOrThrow());
            ListTag fluids = new ListTag();
            for (FluidStack stack : fluid.getFluids()) fluids.add(stack.save(lookup));
            tag.put("Fluids", fluids);
            if (!output.isEmpty()) tag.put("Output", output.save(lookup));
            if (!outputFluid.isEmpty()) tag.put("OutputFluid", outputFluid.save(lookup));
            return tag;
        }
    }
    private record Cache(RecipeManager manager, List<Entry> entries) {}
    private static final Map<Level, Cache> CACHE = Collections.synchronizedMap(new WeakHashMap<>());
    public static void invalidate() { CACHE.clear(); }
    public static List<Entry> all(Level level) {
        if (level == null) return List.of();
        Cache cache = CACHE.get(level);
        if (cache != null && cache.manager == level.getRecipeManager()) return cache.entries;
        List<Entry> entries = decode(level.getRecipeManager().getRecipes());
        CACHE.put(level, new Cache(level.getRecipeManager(), entries));
        return entries;
    }
    public static List<Entry> decode(Collection<? extends RecipeHolder<?>> recipes) {
        List<Entry> entries = new ArrayList<>();
        for (RecipeHolder<?> holder : recipes) {
            Object recipe = holder.value();
            if (!recipe.getClass().getName().equals("com.buuz135.industrial.recipe.DissolutionChamberRecipe")) continue;
            try {
                @SuppressWarnings("unchecked") List<Ingredient> inputs = (List<Ingredient>) recipe.getClass().getField("input").get(recipe);
                SizedFluidIngredient fluid = (SizedFluidIngredient) recipe.getClass().getField("inputFluid").get(recipe);
                var item = (Optional<?>) recipe.getClass().getField("output").get(recipe);
                var liquid = (Optional<?>) recipe.getClass().getField("outputFluid").get(recipe);
                ItemStack output = item.orElse(null) instanceof ItemStack stack ? stack : ItemStack.EMPTY;
                FluidStack outputFluid = liquid.orElse(null) instanceof FluidStack stack ? stack : FluidStack.EMPTY;
                if (inputs.size() > 8 || inputs.stream().anyMatch(i -> i.isEmpty() || i.hasNoItems())
                        || fluid.amount() <= 0 || fluid.amount() > DissolutionProcessor.CAPACITY || fluid.getFluids().length == 0
                        || output.isEmpty() && outputFluid.isEmpty() || outputFluid.getAmount() > DissolutionProcessor.CAPACITY
                        || !output.isEmpty() && output.getCount() > 3 * Math.min(64, output.getMaxStackSize())) continue;
                entries.add(new Entry(holder.id(), inputs, fluid, output, outputFluid));
            } catch (ReflectiveOperationException | LinkageError ignored) {
                // An incompatible optional mod must not create fallback recipes or prevent machine loading.
            }
        }
        entries.sort(Comparator.comparing(e -> e.id.toString()));
        return List.copyOf(entries);
    }
    private DissolutionRecipes() {}
}
