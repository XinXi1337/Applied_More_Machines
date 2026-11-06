package com.appliedmoremachines.dissolution;

import com.appliedmoremachines.latex.LatexUpgrades;
import java.util.*;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.fluids.FluidStack;

/** Plans all consumption, remainders and output space before a single paid commit. */
public final class DissolutionProcessor {
    public static final int CAPACITY = 64000, COST = 4000;
    public interface Host {
        List<ItemStack> inputs();
        List<ItemStack> outputs();
        FluidStack inputFluid();
        FluidStack outputFluid();
        boolean powered();
        void commit(Plan plan);
        void dirty();
    }
    public record Plan(int[] consumed, int fluidConsumed, List<ItemStack> outputs, FluidStack fluidOutput) {}
    private ResourceLocation id;
    private DissolutionRecipes.Entry recipe;
    private CompoundTag definition;
    private List<ItemStack> reserved = List.of();
    private FluidStack reservedFluid = FluidStack.EMPTY;
    private int ticks, duration, parallel;
    public int ticks() { return ticks; }
    public int duration() { return duration; }
    public int parallel() { return parallel; }
    public void tick(Host host, boolean active, LatexUpgrades.Stats upgrades,
                     List<DissolutionRecipes.Entry> recipes, HolderLookup.Provider lookup) {
        if (id != null) {
            var current = recipes.stream().filter(r -> r.id().equals(id)).findFirst().orElse(null);
            if (current == null || current != recipe && !current.definition(lookup).equals(definition)
                    || !matchesReserved(host)) { reset(); host.dirty(); }
            else recipe = current;
        }
        if (!active || !host.powered()) return;
        if (id == null) {
            search: for (var candidate : recipes) for (int p = upgrades.parallel(); p >= 1; p--) {
                Plan plan = plan(host, candidate, p);
                if (plan == null) continue;
                recipe = candidate; id = candidate.id(); definition = candidate.definition(lookup);
                parallel = p; duration = upgrades.ticks(true); ticks = 0;
                List<ItemStack> snapshot = new ArrayList<>();
                for (int i = 0; i < 8; i++) snapshot.add(plan.consumed[i] == 0 ? ItemStack.EMPTY : host.inputs().get(i).copyWithCount(plan.consumed[i]));
                reserved = List.copyOf(snapshot);
                reservedFluid = host.inputFluid().copyWithAmount(plan.fluidConsumed);
                host.dirty(); break search;
            }
        }
        if (id == null) return;
        if (ticks < duration) { ticks++; host.dirty(); }
        if (ticks < duration) return;
        int[] consumed = reserved.stream().mapToInt(ItemStack::getCount).toArray();
        Plan plan = finish(host, recipe, parallel, consumed);
        if (plan == null) return; // Keep the completed batch pending while either output is full.
        host.commit(plan); reset(); host.dirty();
    }
    private boolean matchesReserved(Host host) {
        if (reserved.size() != 8 || !FluidStack.isSameFluidSameComponents(reservedFluid, host.inputFluid())
                || host.inputFluid().getAmount() < reservedFluid.getAmount()) return false;
        for (int i = 0; i < 8; i++) {
            ItemStack expected = reserved.get(i), actual = host.inputs().get(i);
            if (!expected.isEmpty() && (!ItemStack.isSameItemSameComponents(expected, actual) || actual.getCount() < expected.getCount())) return false;
        }
        return true;
    }
    public static Plan plan(Host host, DissolutionRecipes.Entry recipe, int parallel) {
        if (parallel < 1 || parallel > 7) return null;
        long fluid = (long) recipe.fluid().amount() * parallel;
        if (fluid > host.inputFluid().getAmount() || !recipe.fluid().ingredient().test(host.inputFluid())) return null;
        int[] consumed = allocate(host.inputs(), recipe.inputs(), parallel);
        return consumed == null ? null : finish(host, recipe, parallel, consumed);
    }
    private static Plan finish(Host host, DissolutionRecipes.Entry recipe, int parallel, int[] consumed) {
        List<ItemStack> output = new ArrayList<>();
        for (ItemStack stack : host.outputs()) output.add(stack.copy());
        if (!insert(output, recipe.output(), (long) recipe.output().getCount() * parallel)) return null;
        for (int i = 0; i < consumed.length; i++) if (consumed[i] > 0) {
            ItemStack remainder = host.inputs().get(i).copyWithCount(1).getCraftingRemainingItem();
            if (!insert(output, remainder, (long) remainder.getCount() * consumed[i])) return null;
        }
        FluidStack fluidOutput = host.outputFluid().copy(), result = recipe.outputFluid();
        if (!result.isEmpty()) {
            long amount = (long) result.getAmount() * parallel + fluidOutput.getAmount();
            if (amount > CAPACITY || !fluidOutput.isEmpty() && !FluidStack.isSameFluidSameComponents(fluidOutput, result)) return null;
            fluidOutput = result.copyWithAmount((int) amount);
        }
        return new Plan(consumed, recipe.fluid().amount() * parallel, List.copyOf(output), fluidOutput);
    }
    private static boolean insert(List<ItemStack> slots, ItemStack item, long count) {
        if (item.isEmpty() || count <= 0) return true;
        for (int pass = 0; pass < 2; pass++) for (int i = 0; i < slots.size() && count > 0; i++) {
            ItemStack present = slots.get(i);
            if (pass == 0 ? present.isEmpty() || !ItemStack.isSameItemSameComponents(present, item) : !present.isEmpty()) continue;
            int add = (int) Math.min(count, Math.max(0, Math.min(64, item.getMaxStackSize()) - present.getCount()));
            if (add > 0) { slots.set(i, item.copyWithCount(present.getCount() + add)); count -= add; }
        }
        return count == 0;
    }
    /** Integral maximum flow handles overlapping ingredients without reusing any stack count. */
    public static int[] allocate(List<ItemStack> stacks, List<Ingredient> ingredients, int parallel) {
        int n = stacks.size(), m = ingredients.size(), sink = n + m + 1, size = sink + 1;
        int[][] residual = new int[size][size];
        for (int i = 0; i < n; i++) {
            residual[0][1+i] = Math.min(stacks.get(i).getCount(), m * parallel);
            for (int j = 0; j < m; j++) if (ingredients.get(j).test(stacks.get(i))) residual[1+i][1+n+j] = m * parallel;
        }
        for (int j = 0; j < m; j++) residual[1+n+j][sink] = parallel;
        int flow = 0;
        while (flow < m * parallel) {
            int[] previous = new int[size]; Arrays.fill(previous, -1); previous[0] = 0;
            ArrayDeque<Integer> queue = new ArrayDeque<>(); queue.add(0);
            while (!queue.isEmpty() && previous[sink] < 0) {
                int from = queue.remove();
                for (int to = 1; to < size; to++) if (previous[to] < 0 && residual[from][to] > 0) {
                    previous[to] = from; queue.add(to);
                }
            }
            if (previous[sink] < 0) return null;
            int amount = Integer.MAX_VALUE;
            for (int at = sink; at != 0; at = previous[at]) amount = Math.min(amount, residual[previous[at]][at]);
            for (int at = sink; at != 0; at = previous[at]) { residual[previous[at]][at] -= amount; residual[at][previous[at]] += amount; }
            flow += amount;
        }
        int[] result = new int[n];
        for (int i = 0; i < n; i++) result[i] = residual[1+i][0];
        return result;
    }
    private void reset() { id = null; recipe = null; definition = null; reserved = List.of(); reservedFluid = FluidStack.EMPTY; ticks = duration = parallel = 0; }
    public CompoundTag save(HolderLookup.Provider lookup) {
        CompoundTag tag = new CompoundTag();
        if (id == null) return tag;
        tag.putString("Recipe", id.toString()); tag.put("Definition", definition.copy());
        tag.putInt("Ticks", ticks); tag.putInt("Duration", duration); tag.putInt("Parallel", parallel);
        ListTag inputs = new ListTag();
        for (ItemStack stack : reserved) inputs.add(stack.saveOptional(lookup));
        tag.put("Reserved", inputs); tag.put("Fluid", reservedFluid.save(lookup));
        return tag;
    }
    public void load(CompoundTag tag, HolderLookup.Provider lookup) {
        reset();
        var key = ResourceLocation.tryParse(tag.getString("Recipe"));
        int time = tag.getInt("Duration"), p = tag.getInt("Parallel");
        ListTag list = tag.getList("Reserved", Tag.TAG_COMPOUND);
        if (key == null || time < 1 || time > 160 || p < 1 || p > 7 || list.size() != 8 || !tag.contains("Definition", Tag.TAG_COMPOUND)) return;
        FluidStack fluid = FluidStack.parseOptional(lookup, tag.getCompound("Fluid"));
        if (fluid.isEmpty() || fluid.getAmount() > CAPACITY) return;
        id = key; definition = tag.getCompound("Definition").copy(); duration = time; parallel = p;
        ticks = Math.clamp(tag.getInt("Ticks"), 0, time);
        List<ItemStack> inputs = new ArrayList<>();
        for (int i = 0; i < 8; i++) inputs.add(ItemStack.parseOptional(lookup, list.getCompound(i)));
        reserved = List.copyOf(inputs); reservedFluid = fluid;
    }
}
