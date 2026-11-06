package com.appliedmoremachines.fusion;

import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.config.PowerUnit;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;

/** Paid work can exceed buffer capacity; never creates energy or pays again after save/load. */
public final class CoreProcessor {
    private ResourceLocation recipeId;
    private NonNullList<ItemStack> snapshot;
    private List<CoreRecipe> generation;
    private List<CoreRecipe> matchedGeneration;
    private NonNullList<ItemStack> matchedInputs;
    private CoreRecipe matched;
    private int ticks, duration;
    private double paidAE, costAE;
    public int ticks() { return ticks; }
    public int duration() { return duration; }
    public double paidAE() { return paidAE; }
    public boolean working() { return recipeId != null && (paidAE > 0 || ticks > 0); }

    public void tick(Container inventory, CoreEnergyStorage energy, List<CoreRecipe> recipes,
            Function<CoreRecipe, CoreRecipe.Plan> prepare, int requiredTicks,
            Consumer<CoreRecipe.Plan> complete, Runnable dirty) {
        int oldTicks = ticks, oldDuration = duration;
        double oldPaid = paidAE;
        ResourceLocation oldId = recipeId;
        advance(inventory, energy, recipes, prepare, requiredTicks, complete);
        if (oldTicks != ticks || oldDuration != duration || oldPaid != paidAE || !Objects.equals(oldId, recipeId)) dirty.run();
    }

    private void advance(Container inventory, CoreEnergyStorage energy, List<CoreRecipe> recipes,
            Function<CoreRecipe, CoreRecipe.Plan> prepare, int requiredTicks, Consumer<CoreRecipe.Plan> complete) {
        if (generation != null && generation != recipes) reset();
        generation = recipes;
        if (matchedGeneration != recipes || !same(matchedInputs, inventory)) {
            matchedGeneration = recipes;
            matchedInputs = copy(inventory);
            matched = recipes.stream().filter(recipe -> prepare.apply(recipe) != null).findFirst().orElse(null);
        }
        if (matched == null) { reset(); return; }
        double requiredAE = PowerUnit.FE.convertTo(PowerUnit.AE, matched.energyFE());
        if (!Double.isFinite(requiredAE) || requiredAE < 0) { reset(); return; }
        if (!matched.id().equals(recipeId) || !same(snapshot, inventory) || requiredAE != costAE) {
            reset(); recipeId = matched.id(); snapshot = copy(inventory); costAE = requiredAE;
        }
        if (duration != requiredTicks) { duration = requiredTicks; ticks = 0; }
        CoreRecipe.Plan plan = prepare.apply(matched);
        if (plan == null) { reset(); matchedInputs = null; return; }
        double target = ticks + 1 >= duration ? costAE : costAE * (ticks + 1) / duration;
        double due = Math.max(0, target - paidAE);
        if (due > 0) {
            double taken = energy.extractAEPower(due, Actionable.MODULATE, PowerMultiplier.ONE);
            paidAE += taken;
            if (taken < due) return;
            paidAE = Math.max(paidAE, target);
        }
        if (++ticks >= duration) {
            // Revalidated plan + exact full payment: commit once, preserving a100% frame.
            complete.accept(plan);
            recipeId = null; snapshot = null; paidAE = 0; costAE = 0; matchedInputs = null;
        }
    }

    /** A completed product waits in the shared center slot; it must not be crafted as another input. */
    public void idle(Runnable dirty) {
        if (recipeId != null || ticks != 0 || duration != 0) { reset(); dirty.run(); }
    }
    private void reset() { recipeId = null; snapshot = null; ticks = 0; duration = 0; paidAE = 0; costAE = 0; }
    private static boolean same(List<ItemStack> items, Container inventory) {
        if (items == null || items.size() != 18) return false;
        for (int i = 0; i < 18; i++) if (!ItemStack.matches(items.get(i), inventory.getItem(i))) return false;
        return true;
    }
    private static NonNullList<ItemStack> copy(Container inventory) {
        var items = NonNullList.withSize(18, ItemStack.EMPTY);
        for (int i = 0; i < 18; i++) items.set(i, inventory.getItem(i).copy());
        return items;
    }
    public CompoundTag save(HolderLookup.Provider registries) {
        var tag = new CompoundTag();
        if (recipeId != null) {
            tag.putString("Recipe", recipeId.toString()); tag.putInt("Ticks", ticks); tag.putInt("Duration", duration);
            tag.putDouble("PaidAE", paidAE); tag.putDouble("CostAE", costAE);
            ContainerHelper.saveAllItems(tag, snapshot, registries);
        }
        return tag;
    }
    public void load(CompoundTag tag, HolderLookup.Provider registries) {
        reset(); generation = null; matchedGeneration = null; matchedInputs = null; matched = null;
        recipeId = ResourceLocation.tryParse(tag.getString("Recipe"));
        if (recipeId == null) return;
        duration = Math.clamp(tag.getInt("Duration"), 1, 160);
        ticks = Math.clamp(tag.getInt("Ticks"), 0, duration - 1);
        costAE = tag.getDouble("CostAE"); paidAE = tag.getDouble("PaidAE");
        if (!Double.isFinite(costAE) || !Double.isFinite(paidAE) || costAE < 0 || paidAE < 0 || paidAE > costAE) {
            reset(); return;
        }
        snapshot = NonNullList.withSize(18, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, snapshot, registries);
    }
}
