package com.appliedmoremachines.enchanting;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;

/** Registry-backed choices and validation shared by the enchanting menu and its server job. */
public final class EnchantingRules {
    public static final long AE_PER_LEVEL = 8_000;

    private EnchantingRules() {}

    public static List<Holder<Enchantment>> catalog(Level level) {
        return level.registryAccess().registryOrThrow(Registries.ENCHANTMENT).holders()
                .<Holder<Enchantment>>map(holder -> holder)
                .sorted(Comparator.comparing(holder -> id(holder).toString()))
                .toList();
    }

    /** The slot may display an empty choice list for an otherwise unsupported item. */
    public static boolean accepts(ItemStack stack) {
        return !stack.isEmpty() && !stack.is(Items.ENCHANTED_BOOK);
    }

    /** Read the saved crafting component rather than virtual, gameplay-only enchantments. */
    public static int existingLevel(ItemStack stack, Holder<Enchantment> enchantment) {
        return EnchantmentHelper.getEnchantmentsForCrafting(stack).getLevel(enchantment);
    }

    public static int maxLevel(Holder<Enchantment> enchantment) {
        return enchantment.value().getMaxLevel();
    }

    public static boolean applicable(ItemStack stack, Holder<Enchantment> enchantment) {
        return accepts(stack) && (stack.is(Items.BOOK) || stack.supportsEnchantment(enchantment));
    }

    /** Existing enchantments and every selected positive target constrain a choice. */
    public static boolean blocked(ItemStack stack, Holder<Enchantment> candidate,
            Map<ResourceLocation, Integer> targets, List<Holder<Enchantment>> catalog) {
        ItemEnchantments existing = EnchantmentHelper.getEnchantmentsForCrafting(stack);
        for (Holder<Enchantment> other : existing.keySet()) {
            if (existing.getLevel(other) > 0 && !candidate.equals(other)
                    && !Enchantment.areCompatible(candidate, other)) {
                return true;
            }
        }
        for (Holder<Enchantment> other : catalog) {
            Integer target = targets.get(id(other));
            if (target != null && target > 0 && !candidate.equals(other)
                    && !Enchantment.areCompatible(candidate, other)) {
                return true;
            }
        }
        return false;
    }

    /** Invalid or unchanged choices cost zero; a valid result always has positive cost. */
    public static long cost(ItemStack stack, Map<ResourceLocation, Integer> targets,
            List<Holder<Enchantment>> catalog) {
        Selection selection = selection(stack, targets, catalog);
        return selection == null ? 0 : selection.cost();
    }

    /** Pure validation suitable for checking a saved pending job without applying mod hooks. */
    public static boolean validTargets(ItemStack stack, Map<ResourceLocation, Integer> targets,
            List<Holder<Enchantment>> catalog) {
        return selection(stack, targets, catalog) != null;
    }

    /** Apply once to a copy after validation; invalid transformations never become a paid result. */
    public static ItemStack result(ItemStack stack, Map<ResourceLocation, Integer> targets,
            List<Holder<Enchantment>> catalog) {
        Selection selection = selection(stack, targets, catalog);
        if (selection == null) return ItemStack.EMPTY;

        ItemStack output = stack.getItem().applyEnchantments(stack.copy(), selection.upgrades());
        if (output.isEmpty() || output.getCount() != 1
                || stack.is(Items.BOOK) && !output.is(Items.ENCHANTED_BOOK)) {
            return ItemStack.EMPTY;
        }

        ItemEnchantments before = EnchantmentHelper.getEnchantmentsForCrafting(stack);
        ItemEnchantments after = EnchantmentHelper.getEnchantmentsForCrafting(output);
        for (Holder<Enchantment> existing : before.keySet()) {
            if (after.getLevel(existing) < before.getLevel(existing)) return ItemStack.EMPTY;
        }
        for (EnchantmentInstance upgrade : selection.upgrades()) {
            if (after.getLevel(upgrade.enchantment) < upgrade.level) return ItemStack.EMPTY;
        }
        return output;
    }

    public static ResourceLocation id(Holder<Enchantment> enchantment) {
        return enchantment.unwrapKey().orElseThrow(
                () -> new IllegalArgumentException("Enchanting choices must belong to the enchantment registry"))
                .location();
    }

    private static Selection selection(ItemStack stack, Map<ResourceLocation, Integer> targets,
            List<Holder<Enchantment>> catalog) {
        if (!accepts(stack) || stack.getCount() != 1 || targets.isEmpty()) return null;
        Map<ResourceLocation, Holder<Enchantment>> byId = new HashMap<>();
        for (Holder<Enchantment> enchantment : catalog) byId.put(id(enchantment), enchantment);

        List<EnchantmentInstance> upgrades = new ArrayList<>();
        long addedLevels = 0;
        for (Map.Entry<ResourceLocation, Integer> entry : targets.entrySet()) {
            Holder<Enchantment> enchantment = byId.get(entry.getKey());
            Integer target = entry.getValue();
            // Even a zero-valued unknown ID is malformed rather than a selectable registry entry.
            if (enchantment == null || target == null || target < 0) return null;
            if (target == 0) continue;
            int previous = existingLevel(stack, enchantment);
            if (target < previous || target > maxLevel(enchantment)
                    || !applicable(stack, enchantment) || blocked(stack, enchantment, targets, catalog)) {
                return null;
            }
            if (target > previous) {
                addedLevels += (long) target - previous;
                upgrades.add(new EnchantmentInstance(enchantment, target));
            }
        }
        if (addedLevels == 0) return null;
        upgrades.sort(Comparator.comparing(upgrade -> id(upgrade.enchantment).toString()));
        return new Selection(addedLevels * AE_PER_LEVEL, List.copyOf(upgrades));
    }

    private record Selection(long cost, List<EnchantmentInstance> upgrades) {}
}
