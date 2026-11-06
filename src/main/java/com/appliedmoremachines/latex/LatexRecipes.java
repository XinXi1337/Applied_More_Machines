package com.appliedmoremachines.latex;

import java.util.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.fluids.FluidStack;

/** Reflection stays confined here; Industrial Foregoing is optional and its loaded recipe manager is authoritative. */
public final class LatexRecipes {
    public static final ResourceLocation LATEX = ResourceLocation.parse("industrialforegoing:latex");
    public record Extraction(ResourceLocation id, Ingredient input, int amount, boolean fallback) {
        public boolean matches(ItemStack stack) { return stack.is(ItemTags.LOGS) && input.test(stack); }
    }
    public record Rubber(int latex, int water, ItemStack result) {
        public Rubber { result = result.copy(); }
    }
    private record Cache(RecipeManager manager, List<Extraction> recipes) {}
    private static final Map<Level, Cache> CACHE = Collections.synchronizedMap(new WeakHashMap<>());
    private static java.lang.reflect.Field rubberLatex, rubberWater;
    public static void invalidate() { CACHE.clear(); }
    public static List<Extraction> all(Level level) {
        if (level == null || !ModList.get().isLoaded("industrialforegoing")) return List.of();
        Cache cache = CACHE.get(level);
        if (cache != null && cache.manager == level.getRecipeManager()) return cache.recipes;
        List<Extraction> recipes = decode(level.getRecipeManager().getRecipes());
        CACHE.put(level, new Cache(level.getRecipeManager(), recipes));
        return recipes;
    }
    public static List<Extraction> decode(Collection<? extends RecipeHolder<?>> loaded) {
        List<Extraction> result = new ArrayList<>();
        for (RecipeHolder<?> holder : loaded) {
            Object recipe = holder.value();
            if (!recipe.getClass().getName().equals("com.buuz135.industrial.recipe.FluidExtractorRecipe")) continue;
            try {
                FluidStack output = (FluidStack) recipe.getClass().getField("output").get(recipe);
                Ingredient input = (Ingredient) recipe.getClass().getField("input").get(recipe);
                boolean fallback = recipe.getClass().getField("defaultRecipe").getBoolean(recipe);
                if (output.isEmpty() || !BuiltInRegistries.FLUID.getKey(output.getFluid()).equals(LATEX)
                        || input.isEmpty() || input.hasNoItems() || output.getAmount() > Integer.MAX_VALUE / 2000) continue;
                result.add(new Extraction(holder.id(), input, output.getAmount() * 2000, fallback));
            } catch (ReflectiveOperationException ignored) { /* Incompatible optional API: do not invent recipes. */ }
        }
        result.sort(Comparator.comparing(Extraction::fallback).thenComparing(r -> r.id.toString()));
        return List.copyOf(result);
    }
    public static Extraction find(List<Extraction> recipes, ItemStack stack) {
        for (Extraction recipe : recipes) if (recipe.matches(stack)) return recipe;
        return null;
    }
    public static Rubber rubber() {
        if (!ModList.get().isLoaded("industrialforegoing")) return null;
        try {
            if (rubberLatex == null || rubberWater == null) {
                Class<?> tile = Class.forName("com.buuz135.industrial.block.core.tile.LatexProcessingUnitTile");
                rubberLatex = tile.getDeclaredField("AMOUNT_LATEX"); rubberLatex.setAccessible(true);
                rubberWater = tile.getDeclaredField("AMOUNT_WATER"); rubberWater.setAccessible(true);
            }
            var item = BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse("industrialforegoing:dryrubber"));
            int l = rubberLatex.getInt(null), w = rubberWater.getInt(null);
            return l > 0 && w > 0 && item.isPresent() ? new Rubber(l, w, new ItemStack(item.get())) : null;
        } catch (ReflectiveOperationException | LinkageError ignored) { return null; }
    }
    private LatexRecipes() {}
}
