package com.appliedmoremachines.laser;

import com.appliedmoremachines.production.*;

import com.appliedmoremachines.latex.LatexUpgrades;
import java.util.*;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.*;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

/** One paid atomic round, independent fluid rate clocks, no consumed catalysts or overflow loss. */
public final class FluidLaserProcessor {
    public static final int CAPACITY = 64000, COST = 4000;
    public interface Host extends Container {
        FluidStack tankContents(int tank);
        boolean powered();
        void commit(FluidStack[] outputs);
        void dirty();
    }
    private int ticks, duration;
    private final int[] elapsed = new int[3];
    private CompoundTag signature;
    private FluidStack[] planned = empty();
    private static FluidStack[] empty() { return new FluidStack[]{FluidStack.EMPTY,FluidStack.EMPTY,FluidStack.EMPTY}; }
    public int ticks() { return ticks; }
    public int duration() { return duration; }
    public static int amount(int drills, int parallel) { return (10 + 5 * Math.clamp(drills,1,36)) * Math.clamp(parallel,1,7); }
    public static int interval(int amount, int limit) { return limit == Integer.MAX_VALUE ? 0 : (int)(((long)amount * 20 + limit - 1) / limit); }

    public void tick(Host host, boolean active, List<FluidLaserRecipes.Entry> recipes, HolderLookup.Provider lookup) {
        FluidLaserRecipes.Entry[] candidates = new FluidLaserRecipes.Entry[3];
        for (var recipe : recipes) if (FluidLaserSlots.ready(host,recipe.lane()) && candidates[recipe.lane()] == null)
            candidates[recipe.lane()] = recipe;
        CompoundTag current = signature(host,candidates,lookup);
        if (signature != null && !signature.equals(current)) { resetWork(); host.dirty(); }
        if (!active || !host.powered()) return;
        // Any full tank halts the whole machine, even when its catalyst group is temporarily absent.
        for (int lane=0; lane<3 && !MultiplicationUpgrade.active(host); lane++) if (host.tankContents(lane).getAmount() >= CAPACITY) return;
        var upgrades = LatexUpgrades.stats(host,FluidLaserSlots.UPGRADE);
        int rate = FluidLaserSlots.rateLimit(FluidLaserSlots.healingTier(host.getItem(FluidLaserSlots.HEAL)));
        int[] amounts = new int[3], intervals = new int[3];
        boolean any = false;
        for (int lane=0; lane<3; lane++) {
            if (candidates[lane] == null) { elapsed[lane] = 0; continue; }
            any = true;
            amounts[lane] = Math.toIntExact(MultiplicationUpgrade.amount(host, amount(host.getItem(FluidLaserSlots.DRILL+lane).getCount(), 1), upgrades.parallel()));
            intervals[lane] = interval(amount(host.getItem(FluidLaserSlots.DRILL+lane).getCount(), upgrades.parallel()),rate);
        }
        if (!any) { if (duration != 0) { resetWork(); host.dirty(); } return; }
        if (signature == null) {
            int next = Integer.MAX_VALUE;
            for (int lane=0; lane<3; lane++) if (candidates[lane] != null)
                next = Math.min(next, Math.max(upgrades.ticks(true),intervals[lane]-elapsed[lane]));
            planned = empty(); duration = next; ticks = 0; signature = current;
            for (int lane=0; lane<3; lane++) if (candidates[lane] != null && intervals[lane]-elapsed[lane] <= duration)
                planned[lane] = candidates[lane].fluid().copyWithAmount(amounts[lane]);
            host.dirty();
        }
        // Space for every ready group's full batch is required; never reduce parallel or discard excess.
        for (int lane=0; lane<3; lane++) if (candidates[lane] != null && !MultiplicationUpgrade.active(host)) {
            FluidStack present = host.tankContents(lane), result = candidates[lane].fluid();
            if (!present.isEmpty() && !FluidStack.isSameFluidSameComponents(present,result)
                    || present.getAmount() + amounts[lane] > CAPACITY) return;
        }
        if (ticks < duration) {
            ticks++;
            for (int lane=0; lane<3; lane++) if (candidates[lane] != null)
                elapsed[lane] = Math.min(333,elapsed[lane]+1);
            host.dirty();
        }
        if (ticks < duration) return;
        host.commit(Arrays.stream(planned).map(FluidStack::copy).toArray(FluidStack[]::new));
        for (int lane=0; lane<3; lane++) if (!planned[lane].isEmpty()) elapsed[lane] = 0;
        resetWork(); host.dirty();
    }
    private static CompoundTag signature(Host host,FluidLaserRecipes.Entry[] candidates,HolderLookup.Provider lookup) {
        CompoundTag tag = new CompoundTag(); ListTag items = new ListTag(), outputs = new ListTag();
        for (int i=0;i<FluidLaserSlots.SIZE;i++) items.add(host.getItem(i).saveOptional(lookup));
        for (int lane=0;lane<3;lane++) {
            CompoundTag entry = new CompoundTag();
            if (candidates[lane] != null) {
                entry.putString("Id",candidates[lane].id().toString());
                entry.put("Fluid",candidates[lane].fluid().save(lookup));
            }
            outputs.add(entry);
        }
        tag.put("Items",items);tag.put("Outputs",outputs);tag.putBoolean("Multiplication", MultiplicationUpgrade.active(host));return tag;
    }
    private void resetWork() { ticks=0;duration=0;signature=null;planned=empty(); }
    public CompoundTag save(HolderLookup.Provider lookup) {
        CompoundTag tag=new CompoundTag();tag.putIntArray("Elapsed",elapsed);
        if (signature != null) {
            tag.put("Signature",signature.copy());tag.putInt("Ticks",ticks);tag.putInt("Duration",duration);
            ListTag list=new ListTag();for (var fluid:planned) list.add(fluid.saveOptional(lookup));tag.put("Planned",list);
        }
        return tag;
    }
    public void load(CompoundTag tag,HolderLookup.Provider lookup) {
        resetWork();Arrays.fill(elapsed,0);int[] times=tag.getIntArray("Elapsed");
        if (times.length==3) for(int i=0;i<3;i++) elapsed[i]=Math.clamp(times[i],0,333);
        int time=tag.getInt("Duration");ListTag fluids=tag.getList("Planned",Tag.TAG_COMPOUND);
        if (time<1||time>333||fluids.size()!=3||!tag.contains("Signature",Tag.TAG_COMPOUND)) return;
        FluidStack[] restored=empty();boolean any=false;
        for(int i=0;i<3;i++) {
            restored[i]=FluidStack.parseOptional(lookup,fluids.getCompound(i));
            if(restored[i].getAmount()>1995) return;
            any|=!restored[i].isEmpty();
        }
        if(!any)return;
        signature=tag.getCompound("Signature").copy();planned=restored;duration=time;ticks=Math.clamp(tag.getInt("Ticks"),0,time);
    }
}
