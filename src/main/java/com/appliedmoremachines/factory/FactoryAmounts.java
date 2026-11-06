package com.appliedmoremachines.factory;

import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.core.definitions.AEItems;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

/** Exact quantities and components. Unknown optional keys stay in their original NBT. */
public final class FactoryAmounts {
    private FactoryAmounts() {}

    public static Map<AEKey, Long> multiply(Map<AEKey, Long> amounts, long copies) {
        if (copies < 1) throw new IllegalArgumentException("Non-positive copies");
        Map<AEKey, Long> result = new LinkedHashMap<>();
        amounts.forEach((key, amount) -> result.put(key, Math.multiplyExact(amount, copies)));
        return result;
    }

    public static Map<AEKey, Long> of(List<GenericStack> stacks) {
        Map<AEKey, Long> result = new LinkedHashMap<>();
        for (GenericStack stack : stacks) {
            if (stack == null) continue;
            if (stack.amount() <= 0) throw new IllegalArgumentException("Non-positive recipe amount");
            result.merge(stack.what(), stack.amount(), Math::addExact);
        }
        return result;
    }

    public static Map<AEKey, Long> of(KeyCounter[] inputs) {
        Map<AEKey, Long> result = new LinkedHashMap<>();
        for (KeyCounter counter : inputs) for (var entry : counter) {
            if (entry.getLongValue() <= 0) throw new IllegalArgumentException("Non-positive delivered amount");
            result.merge(entry.getKey(), entry.getLongValue(), Math::addExact);
        }
        return result;
    }

    public static ListTag save(Map<AEKey, Long> amounts, HolderLookup.Provider registries) {
        ListTag result = new ListTag();
        amounts.forEach((key, amount) -> {
            if (amount > 0) {
                CompoundTag entry = new CompoundTag();
                entry.put("Key", key.toTagGeneric(registries));
                entry.putLong("Amount", amount);
                result.add(entry);
            }
        });
        return result;
    }

    /** Null means a key's owning mod is unavailable; do not overwrite that list. */
    public static Map<AEKey, Long> load(ListTag list, HolderLookup.Provider registries) {
        Map<AEKey, Long> result = new LinkedHashMap<>();
        try {
            for (Tag value : list) {
                CompoundTag entry = (CompoundTag) value;
                AEKey key = AEKey.fromTagGeneric(registries, entry.getCompound("Key"));
                long amount = entry.getLong("Amount");
                // AE's decoder can return a missing-content item instead of null.
                // It is a recovery placeholder, never real refundable material/output.
                if (key == null || amount <= 0
                        || key instanceof AEItemKey item && item.is(AEItems.MISSING_CONTENT)) return null;
                result.merge(key, amount, Math::addExact);
            }
        } catch (RuntimeException ex) {
            return null;
        }
        return result;
    }
}
