package com.appliedmoremachines.brewing;

import java.util.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.*;
import net.minecraft.world.level.Level;

/** Uses the same world-owned brewing service as the vanilla brewing stand. */
public final class BrewingRecipes {
    public record Entry(ResourceLocation id, ItemStack input, ItemStack ingredient, ItemStack output, boolean free) {}
    private static final Map<Level, List<Entry>> CACHE = Collections.synchronizedMap(new WeakHashMap<>());

    public static boolean bottle(ItemStack stack) {
        return !stack.isEmpty() && (stack.getItem() instanceof PotionItem || stack.is(Items.GLASS_BOTTLE));
    }
    public static boolean accepts(PotionBrewing brewing, int slot, ItemStack stack) {
        if (slot >= 0 && slot < 3) return bottle(stack);
        return slot == 3 && !stack.isEmpty() && !bottle(stack) && brewing.isIngredient(stack);
    }
    public static ItemStack water() { return PotionContents.createItemStack(Items.POTION, Potions.WATER); }
    public static ItemStack result(PotionBrewing brewing, ItemStack input, ItemStack ingredient) {
        if (!bottle(input) || input.getCount() != 1) return ItemStack.EMPTY;
        if (input.is(Items.GLASS_BOTTLE)) return ingredient.isEmpty() ? water() : ItemStack.EMPTY;
        if (!accepts(brewing, 3, ingredient) || ingredient.getCount() != 1 || !brewing.hasMix(input, ingredient)) return ItemStack.EMPTY;
        ItemStack result = brewing.mix(ingredient.copy(), input.copy());
        // The machine's bottle outputs never silently discard an oversized/custom recipe result.
        return !result.isEmpty() && result.getCount() == 1 && result.getItem() instanceof PotionItem ? result.copy() : ItemStack.EMPTY;
    }
    public static List<Entry> all(Level level) {
        if (level == null) return List.of();
        synchronized (CACHE) { return CACHE.computeIfAbsent(level, BrewingRecipes::collect); }
    }
    private static List<Entry> collect(Level level) {
        PotionBrewing brewing = level.potionBrewing();
        List<ItemStack> ingredients = BuiltInRegistries.ITEM.stream().filter(i -> i.isEnabled(level.enabledFeatures()))
                .map(ItemStack::new).filter(s -> accepts(brewing, 3, s)).toList();
        List<Entry> entries = new ArrayList<>();
        entries.add(new Entry(id("fill_water"), new ItemStack(Items.GLASS_BOTTLE), ItemStack.EMPTY, water(), true));
        var potions = level.registryAccess().registryOrThrow(Registries.POTION);
        for (var potion : potions.holders().toList()) {
            for (Item container : List.of(Items.POTION, Items.SPLASH_POTION, Items.LINGERING_POTION)) {
                ItemStack input = PotionContents.createItemStack(container, potion);
                for (ItemStack ingredient : ingredients) {
                    ItemStack output = result(brewing, input, ingredient);
                    if (output.isEmpty()) continue;
                    String key = BuiltInRegistries.ITEM.getKey(container).getPath() + "/" + potion.key().location().toString().replace(':', '/')
                            + "/" + BuiltInRegistries.ITEM.getKey(ingredient.getItem()).toString().replace(':', '/');
                    entries.add(new Entry(id(key), input.copy(), ingredient.copy(), output, false));
                }
            }
        }
        return List.copyOf(entries);
    }
    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("applied_more_machines", "brewing/" + path); }
    public static void invalidate() { CACHE.clear(); }
    private BrewingRecipes() {}
}
