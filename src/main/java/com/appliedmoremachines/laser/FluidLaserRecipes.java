package com.appliedmoremachines.laser;

import java.util.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import net.minecraft.core.registries.BuiltInRegistries;

/** Imports only loaded IF fluid-drill recipes; catalyst structures replace entity/environment checks. */
public final class FluidLaserRecipes {
    public record Entry(ResourceLocation id, int lane, FluidStack fluid) {
        public Entry { fluid = fluid.copyWithAmount(10); }
        @Override public FluidStack fluid() { return fluid.copy(); }
    }
    private record Cache(RecipeManager manager, List<Entry> entries) {}
    private static final Map<Level, Cache> CACHE = Collections.synchronizedMap(new WeakHashMap<>());
    public static void invalidate() { CACHE.clear(); }
    public static List<Entry> all(Level level) {
        if (level == null) return List.of();
        Cache cached = CACHE.get(level);
        if (cached != null && cached.manager == level.getRecipeManager()) return cached.entries;
        List<Entry> entries = decode(level.getRecipeManager().getRecipes());
        CACHE.put(level, new Cache(level.getRecipeManager(), entries));
        return entries;
    }
    public static List<Entry> decode(Collection<? extends RecipeHolder<?>> recipes) {
        List<Entry> entries = new ArrayList<>();
        for (var holder : recipes) {
            Object value = holder.value();
            if (!value.getClass().getName().equals("com.buuz135.industrial.recipe.LaserDrillFluidRecipe")) continue;
            try {
                Ingredient catalyst = (Ingredient) value.getClass().getField("catalyst").get(value);
                SizedFluidIngredient output = (SizedFluidIngredient) value.getClass().getField("output").get(value);
                FluidStack[] fluids = output.getFluids();
                // A fluid tag with multiple alternatives is not a deterministic output.
                if (fluids.length != 1 || fluids[0].isEmpty()) continue;
                for (int lane = 0; lane < 3; lane++) {
                    var id = FluidLaserSlots.expected(FluidLaserSlots.LENS + lane);
                    if (BuiltInRegistries.ITEM.containsKey(id)
                            && catalyst.test(new ItemStack(BuiltInRegistries.ITEM.get(id))))
                        entries.add(new Entry(holder.id(), lane, fluids[0]));
                }
            } catch (ReflectiveOperationException | LinkageError ignored) {
                // Optional IF API mismatch produces no fallback recipe and no hard class linkage.
            }
        }
        entries.sort(Comparator.comparing(e -> e.id().toString()));
        return List.copyOf(entries);
    }
    private FluidLaserRecipes() {}
}
