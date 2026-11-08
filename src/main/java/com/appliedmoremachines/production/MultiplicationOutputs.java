package com.appliedmoremachines.production;

import appeng.api.stacks.*;
import java.util.*;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

/** Output planning is read-only. Commit writes local contents and retains every excess unit. */
public final class MultiplicationOutputs {
    private MultiplicationOutputs() {}
    public record Plan(List<ItemStack> contents, Map<AEKey, Long> overflow) {}
    public static Plan plan(List<ItemStack> current, List<ItemStack> products) {
        List<ItemStack> contents = new ArrayList<>();
        current.forEach(stack -> contents.add(stack.copy()));
        Map<AEKey, Long> spill = new LinkedHashMap<>();
        for (ItemStack product : products) {
            if (product.isEmpty()) continue;
            long left = product.getCount();
            for (int pass = 0; pass < 2; pass++) for (int slot = 0; slot < contents.size() && left > 0; slot++) {
                ItemStack present = contents.get(slot);
                if (pass == 0 ? present.isEmpty() || !ItemStack.isSameItemSameComponents(present, product) : !present.isEmpty()) continue;
                int add = (int) Math.min(left, Math.max(0, Math.min(64, product.getMaxStackSize()) - present.getCount()));
                if (add > 0) { contents.set(slot, product.copyWithCount(present.getCount() + add)); left -= add; }
            }
            if (left > 0) spill.merge(AEItemKey.of(product), left, Math::addExact);
        }
        return new Plan(List.copyOf(contents), Map.copyOf(spill));
    }
    public static void deliver(Container inventory, int[] slots, List<ItemStack> products) {
        Plan plan = plan(Arrays.stream(slots).mapToObj(inventory::getItem).toList(), products);
        if (!plan.overflow().isEmpty() && !(inventory instanceof MultiplicationHost)) throw new IllegalStateException("No overflow owner");
        for (int i = 0; i < slots.length; i++) inventory.setItem(slots[i], plan.contents().get(i));
        retain(inventory, plan.overflow());
    }
    public static void retain(Object host, Map<? extends AEKey, Long> spill) {
        if (!spill.isEmpty()) ((MultiplicationHost) host).multiplicationUpgrade().retain(spill);
    }
    public static FluidStack fluid(Object host, FluidStack present, FluidStack produced, int capacity) {
        if (produced.isEmpty()) return present;
        int local = present.isEmpty() || FluidStack.isSameFluidSameComponents(present, produced)
                ? Math.min(produced.getAmount(), Math.max(0, capacity - present.getAmount())) : 0;
        long spill = (long) produced.getAmount() - local;
        if (spill > 0) ((MultiplicationHost) host).multiplicationUpgrade().retain(AEFluidKey.of(produced), spill);
        return local == 0 ? present : produced.copyWithAmount(present.getAmount() + local);
    }
}
