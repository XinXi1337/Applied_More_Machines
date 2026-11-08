package com.appliedmoremachines.processing;

import com.appliedmoremachines.production.*;

import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import com.appliedmoremachines.InfusionEnergyStorage;
import java.util.List;
import java.util.function.Function;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;

/** Server-owned work state. Materials and full payment are committed together only at completion. */
public final class InfusionProcessor {
    private List<SeedRecipe> matchedRecipes;
    private NonNullList<ItemStack> matchedInputs;
    private SeedRecipe matchedRecipe;
    private ResourceLocation workRecipe;
    private NonNullList<ItemStack> workInputs = NonNullList.withSize(AltarInputs.COUNT, ItemStack.EMPTY);
    private int ticks;
    private int duration;

    public int ticks() {
        return ticks;
    }

    public int duration() {
        return duration;
    }

    public void tick(Container inventory, InfusionEnergyStorage energy, List<SeedRecipe> recipes,
            Function<SeedRecipe, SeedRecipe.Plan> prepare, int requiredTicks, Runnable onChanged) {
        int previousTicks = ticks;
        int previousDuration = duration;
        ResourceLocation previousRecipe = workRecipe;
        advance(inventory, energy, recipes, prepare, requiredTicks);
        if (previousTicks != ticks || previousDuration != duration
                || !java.util.Objects.equals(previousRecipe, workRecipe)) {
            onChanged.run();
        }
    }

    private void advance(Container inventory, InfusionEnergyStorage energy, List<SeedRecipe> recipes,
            Function<SeedRecipe, SeedRecipe.Plan> prepare, int requiredTicks) {
        // Keep a completed frame until the next tick, without adding a cooldown between recipes.
        if (workRecipe == null) {
            ticks = 0;
            duration = 0;
        }
        boolean reload = matchedRecipes != null && matchedRecipes != recipes;
        if (reload) {
            reset();
        }
        if (matchedRecipes != recipes || !sameInputs(matchedInputs, inventory)) {
            matchedRecipes = recipes;
            matchedInputs = copyInputs(inventory);
            matchedRecipe = null;
            for (SeedRecipe candidate : recipes) {
                if (prepare.apply(candidate) != null) {
                    matchedRecipe = candidate;
                    break;
                }
            }
        }
        if (matchedRecipe == null) {
            reset();
            return;
        }
        if (!matchedRecipe.id().equals(workRecipe) || duration != requiredTicks
                || !sameInputs(workInputs, inventory)) {
            reset();
            workRecipe = matchedRecipe.id();
            workInputs = copyInputs(inventory);
            duration = requiredTicks;
        }
        // Recheck the original recipe's dynamic matching and remainders before any payment.
        SeedRecipe.Plan plan = prepare.apply(matchedRecipe);
        if (plan == null) {
            matchedInputs = null;
            reset();
        } else if ((MultiplicationUpgrade.active(inventory) || fitsOutput(inventory, plan.result())) && energy.getAECurrentPower() >= plan.energyCost()) {
            ticks++;
            if (ticks >= duration) {
                energy.extractAEPower(plan.energyCost(), Actionable.MODULATE, PowerMultiplier.ONE);
                for (int slot = 0; slot < AltarInputs.COUNT; slot++) {
                    inventory.setItem(AltarInputs.slot(slot), plan.inputsAfter().get(slot).copy());
                }
                MultiplicationOutputs.deliver(inventory, new int[]{3},
                        List.of(MultiplicationUpgrade.product(inventory, plan.result(), 1)));
                workRecipe = null;
                matchedInputs = null;
            }
        }
    }

    private static boolean fitsOutput(Container inventory, ItemStack result) {
        ItemStack output = inventory.getItem(3);
        int capacity = Math.min(inventory.getMaxStackSize(), result.getMaxStackSize());
        return result.getCount() <= capacity && (output.isEmpty()
                || ItemStack.isSameItemSameComponents(output, result)
                && output.getCount() <= capacity - result.getCount());
    }

    private void reset() {
        workRecipe = null;
        ticks = 0;
        duration = 0;
    }

    private static boolean sameInputs(List<ItemStack> snapshot, Container inventory) {
        if (snapshot == null || snapshot.size() != AltarInputs.COUNT) {
            return false;
        }
        for (int slot = 0; slot < AltarInputs.COUNT; slot++) {
            if (!ItemStack.matches(snapshot.get(slot), inventory.getItem(AltarInputs.slot(slot)))) {
                return false;
            }
        }
        return true;
    }

    private static NonNullList<ItemStack> copyInputs(Container inventory) {
        NonNullList<ItemStack> result = NonNullList.withSize(AltarInputs.COUNT, ItemStack.EMPTY);
        for (int slot = 0; slot < AltarInputs.COUNT; slot++) {
            result.set(slot, inventory.getItem(AltarInputs.slot(slot)).copy());
        }
        return result;
    }

    public CompoundTag save(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        if (workRecipe != null) {
            tag.putString("Recipe", workRecipe.toString());
            tag.putInt("Ticks", ticks);
            tag.putInt("Duration", duration);
            ContainerHelper.saveAllItems(tag, workInputs, registries);
        }
        return tag;
    }

    public void load(CompoundTag tag, HolderLookup.Provider registries) {
        reset();
        matchedInputs = null;
        matchedRecipes = null;
        matchedRecipe = null;
        workRecipe = ResourceLocation.tryParse(tag.getString("Recipe"));
        if (workRecipe != null) {
            duration = Math.clamp(tag.getInt("Duration"), 1, InfusionTiming.BASE_TICKS);
            ticks = Math.clamp(tag.getInt("Ticks"), 0, duration - 1);
            workInputs = NonNullList.withSize(AltarInputs.COUNT, ItemStack.EMPTY);
            ContainerHelper.loadAllItems(tag, workInputs, registries);
        }
    }
}
