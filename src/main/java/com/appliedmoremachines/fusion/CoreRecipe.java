package com.appliedmoremachines.fusion;

import com.appliedmoremachines.CraftingCoreBlockEntity;
import com.appliedmoremachines.processing.IngredientAllocation;
import com.appliedmoremachines.processing.SeedRecipe;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;

/** A loaded fusion recipe, with all optional API calls confined to its result callback. */
public record CoreRecipe(ResourceLocation id, Ingredient catalyst, int catalystCount, int tier,
        long energyFE, List<Part> parts, ResultFactory factory) {
    public CoreRecipe { parts = List.copyOf(parts); }
    public record Part(Ingredient ingredient, int count, boolean consume) {}
    public record Plan(ItemStack result, List<ItemStack> materialsAfter) {}
    @FunctionalInterface public interface ResultFactory {
        ItemStack assemble(Level level, HolderLookup.Provider registries, ItemStack center, List<ItemStack> materials, int tier);
    }

    public static boolean accepts(Ingredient ingredient, int count, ItemStack stack) {
        return count > 0 && SeedRecipe.acceptsIngredient(ingredient, stack.copyWithCount(count));
    }

    private static boolean available(Ingredient ingredient, int count) {
        return count > 0 && count <= 64 && !ingredient.hasNoItems()
                && java.util.Arrays.stream(ingredient.getItems()).anyMatch(stack -> accepts(ingredient, count, stack));
    }

    public boolean available() {
        return tier >= 0 && tier <= 3 && energyFE >= 0
                && available(catalyst, catalystCount)
                && parts.stream().allMatch(part -> available(part.ingredient, part.count));
    }

    /** Compact source recipes for JEI only; source matching still receives every original entry. */
    public List<Part> displayParts() {
        List<Part> display = new ArrayList<>();
        for (Part part : parts) {
            int remaining = part.count;
            int limit = java.util.Arrays.stream(part.ingredient.getItems())
                    .filter(stack -> accepts(part.ingredient, part.count, stack))
                    .mapToInt(ItemStack::getMaxStackSize).min().orElse(1);
            for (int i = 0; i < display.size() && remaining > 0; i++) {
                Part existing = display.get(i);
                // Full predicate equality preserves tags, custom components and consume behavior.
                if (existing.consume == part.consume && existing.ingredient.equals(part.ingredient)) {
                    int move = Math.min(remaining, Math.max(0, limit - existing.count));
                    if (move > 0) {
                        display.set(i, new Part(existing.ingredient, existing.count + move, existing.consume));
                        remaining -= move;
                    }
                }
            }
            while (remaining > 0) {
                int move = Math.min(remaining, limit);
                display.add(new Part(part.ingredient, move, part.consume));
                remaining -= move;
            }
        }
        return List.copyOf(display);
    }

    public Plan prepare(Level level, HolderLookup.Provider registries, Container inventory) {
        ItemStack equipment = inventory.getItem(CraftingCoreBlockEntity.INJECTOR_SLOT);
        int unlocked = CoreInjectors.unlocked(equipment);
        int installedTier = CoreInjectors.tier(equipment);
        ItemStack center = inventory.getItem(CraftingCoreBlockEntity.CENTER_SLOT);
        // Injectors unlock physical slots. Repeated recipe entries may share a stack;
        // quantity allocation below decides whether the unlocked inventory is sufficient.
        if (!available() || installedTier < tier
                || center.getCount() < catalystCount || !accepts(catalyst, catalystCount, center)) return null;
        int[] counts = new int[unlocked];
        for (int i = 0; i < counts.length; i++) counts[i] = inventory.getItem(i).getCount();
        List<boolean[]> predicates = new ArrayList<>();
        List<Integer> positions = new ArrayList<>();
        for (int partIndex = 0; partIndex < parts.size(); partIndex++) {
            Part part = parts.get(partIndex);
            boolean[] matches = new boolean[unlocked];
            for (int slot = 0; slot < unlocked; slot++) matches[slot] = accepts(part.ingredient, part.count, inventory.getItem(slot));
            for (int unit = 0; unit < part.count; unit++) { predicates.add(matches); positions.add(partIndex); }
        }
        int[] assigned = IngredientAllocation.find(counts, predicates.toArray(boolean[][]::new));
        if (assigned == null) return null;
        List<ItemStack> selected = new ArrayList<>();
        for (int i = 0; i < parts.size(); i++) selected.add(ItemStack.EMPTY);
        List<ItemStack> after = new ArrayList<>();
        for (int slot = 0; slot < 16; slot++) after.add(inventory.getItem(slot).copy());
        for (int unit = 0; unit < assigned.length; unit++) {
            int position = positions.get(unit), slot = assigned[unit];
            ItemStack stack = inventory.getItem(slot);
            if (selected.get(position).isEmpty()) selected.set(position, stack.copyWithCount(1));
            else if (ItemStack.isSameItemSameComponents(selected.get(position), stack)) selected.get(position).grow(1);
            else return null;
            if (parts.get(position).consume) after.get(slot).shrink(1);
        }
        ItemStack result = factory.assemble(level, registries, center.copyWithCount(catalystCount), selected, installedTier);
        if (result.isEmpty() || result.getCount() > result.getMaxStackSize()) return null;
        for (int part = 0; part < parts.size(); part++) {
            ItemStack used = selected.get(part);
            if (parts.get(part).consume && used.hasCraftingRemainingItem()) {
                for (int unit = 0; unit < used.getCount(); unit++) {
                    if (!returnRemainder(after, used.copyWithCount(1).getCraftingRemainingItem(), unlocked)) return null;
                }
            }
        }
        if (center.hasCraftingRemainingItem()) {
            for (int unit = 0; unit < catalystCount; unit++) {
                if (!returnRemainder(after, center.copyWithCount(1).getCraftingRemainingItem(), unlocked)) return null;
            }
        }
        // The shared center becomes the product. Reserve space for unused catalyst first.
        ItemStack surplus = center.copyWithCount(center.getCount() - catalystCount);
        if (!surplus.isEmpty() && !returnRemainder(after, surplus, unlocked)) return null;
        return new Plan(result.copy(), List.copyOf(after));
    }

    /** Resolve an automatic batch, not the first globally known catalyst (which may be another recipe's material). */
    public List<ItemStack> automaticLayout(Level level, HolderLookup.Provider registries, Container inventory) {
        int unlocked = CoreInjectors.unlocked(inventory.getItem(CraftingCoreBlockEntity.INJECTOR_SLOT));
        if (CoreInjectors.tier(inventory.getItem(CraftingCoreBlockEntity.INJECTOR_SLOT)) < tier) return null;
        List<ItemStack> supplied = new ArrayList<>();
        for (int slot = 0; slot < unlocked; slot++) supplied.add(inventory.getItem(slot).copy());
        supplied.add(inventory.getItem(CraftingCoreBlockEntity.CENTER_SLOT).copy());
        for (ItemStack candidate : supplied) {
            if (!accepts(catalyst, catalystCount, candidate) || catalystCount > candidate.getMaxStackSize()) continue;
            List<ItemStack> remaining = supplied.stream().map(ItemStack::copy).toList();
            int needed = catalystCount;
            for (ItemStack stack : remaining) {
                if (needed > 0 && ItemStack.isSameItemSameComponents(candidate, stack)) {
                    int take = Math.min(needed, stack.getCount()); stack.shrink(take); needed -= take;
                }
            }
            if (needed != 0) continue;
            List<ItemStack> packed = new ArrayList<>();
            for (int slot = 0; slot < unlocked; slot++) packed.add(ItemStack.EMPTY);
            boolean fits = true;
            for (ItemStack stack : remaining) if (!returnRemainder(packed, stack, unlocked)) { fits = false; break; }
            if (!fits) continue;
            var view = new net.minecraft.world.SimpleContainer(CraftingCoreBlockEntity.INVENTORY_SIZE);
            for (int slot = 0; slot < inventory.getContainerSize(); slot++) view.setItem(slot, inventory.getItem(slot).copy());
            for (int slot = 0; slot < unlocked; slot++) view.setItem(slot, packed.get(slot).copy());
            view.setItem(CraftingCoreBlockEntity.CENTER_SLOT, candidate.copyWithCount(catalystCount));
            if (prepare(level, registries, view) == null) continue;
            List<ItemStack> layout = new ArrayList<>();
            for (int slot = 0; slot < 18; slot++) layout.add(view.getItem(slot).copy());
            return List.copyOf(layout);
        }
        return null;
    }

    private static boolean returnRemainder(List<ItemStack> after, ItemStack offered, int unlocked) {
        ItemStack remaining = offered.copy();
        for (int pass = 0; pass < 2; pass++) for (int slot = 0; slot < unlocked && !remaining.isEmpty(); slot++) {
            ItemStack existing = after.get(slot);
            if (pass == 0 && !existing.isEmpty() && ItemStack.isSameItemSameComponents(existing, remaining)) {
                int move = Math.max(0, Math.min(remaining.getCount(), existing.getMaxStackSize() - existing.getCount()));
                existing.grow(move); remaining.shrink(move);
            } else if (pass == 1 && existing.isEmpty()) {
                int move = Math.min(remaining.getCount(), remaining.getMaxStackSize());
                after.set(slot, remaining.copyWithCount(move)); remaining.shrink(move);
            }
        }
        return remaining.isEmpty();
    }
}
