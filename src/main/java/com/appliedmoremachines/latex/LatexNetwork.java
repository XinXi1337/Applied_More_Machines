package com.appliedmoremachines.latex;

import appeng.api.stacks.AEItemKey;
import appeng.api.storage.MEStorage;
import java.util.List;
import net.minecraft.world.item.ItemStack;

/** Ranking uses the same loaded recipes as manual extraction and preserves item components. */
public final class LatexNetwork {
    public static AEItemKey bestLog(MEStorage storage, List<LatexRecipes.Extraction> recipes, ItemStack present) {
        AEItemKey best = null; int amount = -1; long quantity = -1;
        for (var entry : storage.getAvailableStacks()) {
            if (!(entry.getKey() instanceof AEItemKey key) || entry.getLongValue() <= 0) continue;
            ItemStack sample = key.toStack(1);
            if (!present.isEmpty() && !ItemStack.isSameItemSameComponents(present, sample)) continue;
            var recipe = LatexRecipes.find(recipes, sample);
            if (recipe != null && (recipe.amount() > amount || recipe.amount() == amount && entry.getLongValue() > quantity
                    || recipe.amount() == amount && entry.getLongValue() == quantity && best != null
                    && key.toString().compareTo(best.toString()) < 0)) {
                best = key; amount = recipe.amount(); quantity = entry.getLongValue();
            }
        }
        return best;
    }
    private LatexNetwork() {}
}
