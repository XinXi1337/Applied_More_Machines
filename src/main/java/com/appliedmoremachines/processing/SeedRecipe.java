package com.appliedmoremachines.processing;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;

/** Adapter retaining the loaded source recipe's tags, components, result and crafting remainders. */
public record SeedRecipe(ResourceLocation id, Recipe<CraftingInput> source,
                         List<Ingredient> ingredients, int width, int height, List<Integer> quantities) {
    public SeedRecipe(ResourceLocation id, Recipe<CraftingInput> source,
            List<Ingredient> ingredients, int width, int height) {
        this(id, source, ingredients, width, height,
                ingredients.stream().map(ingredient -> ingredient.isEmpty() ? 0 : 1).toList());
    }

    public SeedRecipe {
        ingredients = List.copyOf(ingredients);
        quantities = List.copyOf(quantities);
        if (ingredients.size() != width * height || quantities.size() != ingredients.size()
                || quantities.stream().anyMatch(count -> count < 0)) {
            throw new IllegalArgumentException("Invalid altar recipe dimensions or quantities: " + id);
        }
    }

    /** Keep source positions separate, including repeated unstackable materials. */
    public List<DisplayIngredient> displayInputs() {
        List<DisplayIngredient> inputs = new ArrayList<>(AltarInputs.COUNT);
        for (int i = 0; i < AltarInputs.COUNT; i++) {
            inputs.add(i < ingredients.size()
                    ? new DisplayIngredient(ingredients.get(i), quantities.get(i))
                    : new DisplayIngredient(Ingredient.EMPTY, 0));
        }
        return List.copyOf(inputs);
    }

    public record DisplayIngredient(Ingredient ingredient, int count) {
        public List<ItemStack> stacks() {
            if (count == 0 || ingredient.isEmpty()) return List.of();
            return java.util.Arrays.stream(ingredient.getItems())
                    .filter(stack -> !stack.isEmpty() && !isMissingTagPlaceholder(stack))
                    .map(stack -> stack.copyWithCount(count)).toList();
        }
    }

    /** Empty layout cells are allowed; a required ingredient with no real items is not. */
    public boolean hasAvailableIngredients() {
        for (int position = 0; position < ingredients.size(); position++) {
            if (quantities.get(position) <= 0) continue;
            Ingredient ingredient = ingredients.get(position);
            if (ingredient.hasNoItems() || java.util.Arrays.stream(ingredient.getItems()).noneMatch(stack ->
                    !stack.isEmpty() && !isMissingTagPlaceholder(stack) && ingredient.test(stack))) {
                return false;
            }
        }
        return true;
    }

    private static boolean isMissingTagPlaceholder(ItemStack stack) {
        // NeoForge also sends these display placeholders as ordinary item values to clients.
        var name = stack.get(DataComponents.CUSTOM_NAME);
        return stack.is(Items.BARRIER) && name != null && name.getString().startsWith("Empty Tag: ");
    }

    public static boolean acceptsIngredient(Ingredient ingredient, ItemStack stack) {
        if (stack.isEmpty() || ingredient.isEmpty() || !ingredient.test(stack)) return false;
        // An empty-tag alternative must not allow creative barriers to substitute for real material.
        return !stack.is(Items.BARRIER) || java.util.Arrays.stream(ingredient.getItems())
                .anyMatch(example -> example.is(Items.BARRIER) && !isMissingTagPlaceholder(example));
    }

    public Plan prepare(Level level, Container inventory) {
        return prepare(level, level.registryAccess(), inventory);
    }

    public Plan prepare(Level level, HolderLookup.Provider registries, Container inventory) {
        // Recheck after tag changes too; unavailable requirements must never become free inputs.
        if (!hasAvailableIngredients()) return null;
        List<Integer> positions = new ArrayList<>();
        List<boolean[]> matches = new ArrayList<>();
        int[] counts = new int[AltarInputs.COUNT];
        for (int slot = 0; slot < counts.length; slot++) {
            counts[slot] = inventory.getItem(AltarInputs.slot(slot)).getCount();
        }
        if (quantities.stream().mapToLong(Integer::longValue).sum() > java.util.Arrays.stream(counts).sum()) {
            return null;
        }
        for (int position = 0; position < ingredients.size(); position++) {
            Ingredient ingredient = ingredients.get(position);
            if (!ingredient.isEmpty()) {
                boolean[] accepted = new boolean[counts.length];
                boolean any = false;
                for (int slot = 0; slot < counts.length; slot++) {
                    accepted[slot] = acceptsIngredient(ingredient, inventory.getItem(AltarInputs.slot(slot)));
                    any |= accepted[slot] && counts[slot] > 0;
                }
                if (!any) {
                    return null;
                }
                for (int unit = 0; unit < quantities.get(position); unit++) {
                    positions.add(position);
                    matches.add(accepted);
                }
            }
        }
        int[] assigned = IngredientAllocation.find(counts, matches.toArray(boolean[][]::new));
        if (assigned == null) {
            return null;
        }
        List<ItemStack> expanded = new ArrayList<>();
        int[] sourceSlots = new int[ingredients.size()];
        java.util.Arrays.fill(sourceSlots, -1);
        for (int i = 0; i < ingredients.size(); i++) {
            expanded.add(ItemStack.EMPTY);
        }
        for (int unit = 0; unit < assigned.length; unit++) {
            int position = positions.get(unit);
            sourceSlots[position] = assigned[unit];
            ItemStack selected = inventory.getItem(AltarInputs.slot(assigned[unit]));
            if (expanded.get(position).isEmpty()) {
                expanded.set(position, selected.copyWithCount(1));
            } else if (ItemStack.isSameItemSameComponents(expanded.get(position), selected)) {
                expanded.get(position).grow(1);
            } else {
                return null;
            }
        }
        // Physical input positions never reach the source recipe: rebuild its expected layout
        // from quantity/component matches, so even shaped fallback recipes accept any arrangement.
        var positioned = CraftingInput.ofPositioned(width, height, expanded);
        CraftingInput input = positioned.input();
        if (!source.matches(input, level)) {
            return null;
        }
        ItemStack result = source.assemble(input, registries);
        if (result.isEmpty() || result.getCount() > result.getMaxStackSize()) {
            return null;
        }
        List<ItemStack> after = new ArrayList<>();
        for (int slot = 0; slot < counts.length; slot++) {
            after.add(inventory.getItem(AltarInputs.slot(slot)).copy());
        }
        for (int slot : assigned) {
            after.get(slot).shrink(1);
        }
        var remainders = source.getRemainingItems(input);
        for (int i = 0; i < remainders.size(); i++) {
            ItemStack remainder = remainders.get(i);
            if (remainder.isEmpty()) {
                continue;
            }
            int originalPosition = (i / input.width() + positioned.top()) * width
                    + i % input.width() + positioned.left();
            int preferredSlot = sourceSlots[originalPosition];
            if (!placeRemainder(after, remainder.copy(), preferredSlot)) {
                return null;
            }
        }
        return new Plan(result.copy(), List.copyOf(after));
    }

    private static boolean placeRemainder(List<ItemStack> inputs, ItemStack remainder, int preferred) {
        for (int pass = 0; pass < 2 && !remainder.isEmpty(); pass++) {
            for (int step = 0; step < inputs.size(); step++) {
                int slot = preferred >= 0 ? (preferred + step) % inputs.size() : step;
                ItemStack existing = inputs.get(slot);
                if (pass == 0 && !existing.isEmpty() && ItemStack.isSameItemSameComponents(existing, remainder)) {
                    int moved = Math.max(0, Math.min(remainder.getCount(), existing.getMaxStackSize() - existing.getCount()));
                    existing.grow(moved);
                    remainder.shrink(moved);
                } else if (pass == 1 && existing.isEmpty()) {
                    int moved = Math.min(remainder.getCount(), remainder.getMaxStackSize());
                    inputs.set(slot, remainder.copyWithCount(moved));
                    remainder.shrink(moved);
                }
            }
        }
        return remainder.isEmpty();
    }

    public record Plan(ItemStack result, List<ItemStack> inputsAfter) {
        public long energyCost() {
            return InfusionTiming.AE_PER_CRAFT;
        }
    }
}
