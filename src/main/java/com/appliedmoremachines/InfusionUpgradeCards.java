package com.appliedmoremachines;

import appeng.core.definitions.AEItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** Shared upgrade admission and speed-card classification. */
public final class InfusionUpgradeCards {
    private static final ResourceLocation OVERLOAD_CARD = ResourceLocation.fromNamespaceAndPath("ae2cs", "overload_card");

    private InfusionUpgradeCards() {}

    public static boolean accepts(ItemStack stack) {
        return !stack.isEmpty() && (stack.is(AEItems.SPEED_CARD.asItem())
                || BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(OVERLOAD_CARD));
    }

    public static boolean isOverload(ItemStack stack) {
        return !stack.isEmpty() && BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(OVERLOAD_CARD);
    }
}
