package com.appliedmoremachines.dissolution;

import java.util.*;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.DataComponentFluidIngredient;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;

/** Optional IF/IFEU bridge; imports only recipes loaded in the world's manager. */
public final class DissolutionRecipes {
    public record Input(Ingredient ingredient, int count) {}

    /** A null fluid means no fluid is required (precision crafting). */
    public record Entry(ResourceLocation id, List<Ingredient> inputs, SizedFluidIngredient fluid,
                        ItemStack output, FluidStack outputFluid) {
        public Entry {
            inputs = List.copyOf(inputs); output = output.copy(); outputFluid = outputFluid.copy();
        }
        /** Presentation only: preserve complete predicates and split at native stack limits. */
        public List<Input> displayInputs() {
            List<Input> result = new ArrayList<>();
            for (Ingredient ingredient : inputs) {
                int limit = Arrays.stream(ingredient.getItems()).filter(ingredient::test)
                        .mapToInt(stack -> Math.min(64, stack.getMaxStackSize())).min().orElse(1);
                boolean merged = false;
                for (int i = 0; i < result.size(); i++) {
                    Input existing = result.get(i);
                    if (existing.count < limit && existing.ingredient.equals(ingredient)) {
                        result.set(i, new Input(existing.ingredient, existing.count + 1));
                        merged = true; break;
                    }
                }
                if (!merged) result.add(new Input(ingredient, 1));
            }
            return List.copyOf(result);
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
            tag.put("Predicates", Ingredient.CODEC.listOf().encodeStart(
                    lookup.createSerializationContext(NbtOps.INSTANCE), inputs).getOrThrow());
            if (fluid != null) {
                tag.put("FluidPredicate", SizedFluidIngredient.FLAT_CODEC.encodeStart(
                        lookup.createSerializationContext(NbtOps.INSTANCE), fluid).getOrThrow());
                ListTag fluids = new ListTag();
                for (FluidStack stack : fluid.getFluids()) fluids.add(stack.save(lookup));
                tag.put("Fluids", fluids);
            }
            if (!output.isEmpty()) tag.put("Output", output.save(lookup));
            if (!outputFluid.isEmpty()) tag.put("OutputFluid", outputFluid.save(lookup));
            return tag;
        }
    }
    private record Cache(RecipeManager manager, List<Entry> entries) {}
    private static final Map<Level, Cache> CACHE = Collections.synchronizedMap(new WeakHashMap<>());
    private static final String IFEU = "net.yxiao233.ifeu.common.recipe.";
    private static final ResourceLocation AIR = ResourceLocation.parse("ifeu:air");
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
            try {
                Entry entry = read(holder);
                if (entry == null || entry.inputs.size() > 8 * 64
                        || entry.inputs.isEmpty() && entry.fluid == null
                        || entry.inputs.stream().anyMatch(i -> i.isEmpty() || i.hasNoItems())
                        || entry.fluid != null && (entry.fluid.amount() <= 0
                            || entry.fluid.amount() > DissolutionProcessor.CAPACITY || entry.fluid.getFluids().length == 0)
                        || entry.output.isEmpty() && entry.outputFluid.isEmpty()
                        || entry.outputFluid.getAmount() > DissolutionProcessor.CAPACITY
                        || !entry.output.isEmpty() && entry.output.getCount() > 3 * Math.min(64, entry.output.getMaxStackSize())) continue;
                entries.add(entry);
            } catch (ReflectiveOperationException | LinkageError ignored) {
                // Incompatible optional APIs must not create fallback recipes or prevent machine loading.
            }
        }
        entries.sort(Comparator.comparing(e -> e.id.toString()));
        return List.copyOf(entries);
    }
    private static Entry read(RecipeHolder<?> holder) throws ReflectiveOperationException {
        Object recipe = holder.value();
        String type = recipe.getClass().getName();
        if (type.equals("com.buuz135.industrial.recipe.DissolutionChamberRecipe")) {
            var item = (Optional<?>) field(recipe, "output");
            var liquid = (Optional<?>) field(recipe, "outputFluid");
            return new Entry(holder.id(), ingredients(recipe, "input"), (SizedFluidIngredient) field(recipe, "inputFluid"),
                    item.orElse(null) instanceof ItemStack stack ? stack : ItemStack.EMPTY,
                    liquid.orElse(null) instanceof FluidStack stack ? stack : FluidStack.EMPTY);
        }
        if (type.equals(IFEU + "InfuserRecipe")) {
            ItemStack input = (ItemStack) field(recipe, "input");
            if (input.isEmpty() || input.getCount() > 8 * 64) return null;
            return new Entry(holder.id(), Collections.nCopies(input.getCount(), Ingredient.of(input.copyWithCount(1))),
                    fluid((FluidStack) field(recipe, "inputFluid")), (ItemStack) field(recipe, "output"), FluidStack.EMPTY);
        }
        boolean precision = type.equals(IFEU + "PrecisionShapedRecipe") || type.equals(IFEU + "PrecisionShapelessRecipe");
        if (!precision && !type.equals(IFEU + "ShapedRecipe") && !type.equals(IFEU + "ShapelessRecipe")) return null;
        List<Ingredient> inputs = ingredients(recipe, "inputs").stream().filter(i -> !placeholder(i)).toList();
        // The machine deliberately treats precision crafting as 100% successful.
        return new Entry(holder.id(), inputs, precision ? null : fluid((FluidStack) field(recipe, "inputFluid")),
                (ItemStack) field(recipe, "output"), FluidStack.EMPTY);
    }
    private static Object field(Object recipe, String name) throws ReflectiveOperationException {
        return recipe.getClass().getField(name).get(recipe);
    }
    @SuppressWarnings("unchecked")
    private static List<Ingredient> ingredients(Object recipe, String field) throws ReflectiveOperationException {
        return (List<Ingredient>) field(recipe, field);
    }
    private static SizedFluidIngredient fluid(FluidStack stack) {
        return new SizedFluidIngredient(DataComponentFluidIngredient.of(true, stack), stack.getAmount());
    }
    private static boolean placeholder(Ingredient ingredient) {
        if (ingredient.isEmpty()) return true;
        ItemStack[] choices = ingredient.getItems();
        return choices.length > 0 && Arrays.stream(choices)
                .allMatch(stack -> stack.isEmpty() || AIR.equals(BuiltInRegistries.ITEM.getKey(stack.getItem())));
    }
    private DissolutionRecipes() {}
}
