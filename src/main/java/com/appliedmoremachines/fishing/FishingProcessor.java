package com.appliedmoremachines.fishing;

import com.appliedmoremachines.production.*;

import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.networking.energy.IAEPowerStorage;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntFunction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

/** One saved batch: sample once, pay once, retain the exact stacks until all fit. */
public final class FishingProcessor {
    public static final int OUTPUTS = 27;
    public static final int COST_PER_DRAW = 8_000;
    private int ticks, duration, draws, completedDraws, successfulDraws, mode;
    private boolean paid, multiplied;
    private final List<ItemStack> pending = new ArrayList<>();

    public static int duration(int speed, int overload) { return 200 - 30 * speed - 40 * overload; }
    public static int draws(int speed, int overload) { return 1 + speed + 2 * overload; }
    public int ticks() { return ticks; }
    public int duration() { return duration; }
    public boolean hasBatch() { return duration > 0; }
    public List<ItemStack> paidDrops() { return paid ? pending.stream().map(ItemStack::copy).toList() : List.of(); }

    public void tick(Container inventory, IAEPowerStorage energy, boolean active, boolean rod,
            int speed, int overload, int selectedMode, IntFunction<List<ItemStack>> sample, Runnable dirty) {
        if (!active || !rod) return;
        if (paid) {
            if (insertAll(inventory, pending)) { reset(); dirty.run(); }
            return;
        }
        if (hasBatch() && ((mode ^ selectedMode) & FishingLoot.BLOCK_UNSTACKABLE) != 0) {
            // Apply this independent switch to draws that have not been paid for.
            // Existing paid output is handled above and must never be discarded.
            mode = (mode & 3) | (selectedMode & FishingLoot.BLOCK_UNSTACKABLE);
            completedDraws = successfulDraws = 0;
            pending.clear();
            dirty.run();
        }
        if (energy.getAECurrentPower() <= 0) return;
        if (!hasBatch()) {
            duration = duration(speed, overload);
            draws = draws(speed, overload);
            mode = selectedMode;
            multiplied = MultiplicationUpgrade.active(inventory);
        }
        if (ticks < duration) { ticks++; dirty.run(); }
        if (ticks < duration || energy.getAECurrentPower() < draws * COST_PER_DRAW) return;
        while (completedDraws < draws) {
            List<ItemStack> catchItems = sample.apply(mode).stream().filter(s -> !s.isEmpty()).toList();
            // A probability miss is still one completed attempt. Waiting for a
            // successful catch here traps the progress at 100% and changes odds.
            if (!catchItems.isEmpty()) {
                // One native draw can split a single key across stacks. Round its total once.
                var nativeOutputs = new java.util.LinkedHashMap<appeng.api.stacks.AEItemKey, Long>();
                for (ItemStack stack : catchItems) nativeOutputs.merge(appeng.api.stacks.AEItemKey.of(stack), (long) stack.getCount(), Math::addExact);
                for (var entry : nativeOutputs.entrySet()) {
                    ItemStack stack = entry.getKey().toStack(1);
                    long amount = multiplied ? MultiplicationUpgrade.boost(entry.getValue()) : entry.getValue();
                    // Keep NBT ItemStack counts within the native disk codec limit.
                    while (amount > 0) {
                        int count = (int) Math.min(amount, stack.getMaxStackSize());
                        pending.add(stack.copyWithCount(count)); amount -= count;
                    }
                }
                successfulDraws++;
            }
            completedDraws++;
            dirty.run();
        }
        if (successfulDraws == 0) { reset(); dirty.run(); return; }
        double cost = successfulDraws * COST_PER_DRAW;
        energy.extractAEPower(cost, Actionable.MODULATE, PowerMultiplier.ONE);
        paid = true;
        dirty.run();
        if (insertAll(inventory, pending)) { reset(); dirty.run(); }
    }

    /** Atomic batch insertion preserves components, native limits and already occupied slots. */
    private boolean insertAll(Container inventory, List<ItemStack> stacks) {
        if (multiplied && inventory instanceof MultiplicationHost) {
            MultiplicationOutputs.deliver(inventory, java.util.stream.IntStream.range(0, OUTPUTS).toArray(), stacks);
            return true;
        }
        List<ItemStack> result = new ArrayList<>();
        for (int slot = 0; slot < OUTPUTS; slot++) result.add(inventory.getItem(slot).copy());
        for (ItemStack input : stacks) {
            ItemStack remaining = input.copy();
            for (int pass = 0; pass < 2 && !remaining.isEmpty(); pass++) {
                for (int slot = 0; slot < OUTPUTS && !remaining.isEmpty(); slot++) {
                    ItemStack present = result.get(slot);
                    if (pass == 0 ? present.isEmpty() || !ItemStack.isSameItemSameComponents(present, remaining) : !present.isEmpty()) continue;
                    int limit = Math.min(inventory.getMaxStackSize(remaining), remaining.getMaxStackSize());
                    int amount = Math.min(remaining.getCount(), Math.max(0, limit - present.getCount()));
                    if (amount == 0) continue;
                    if (present.isEmpty()) result.set(slot, remaining.copyWithCount(amount));
                    else present.grow(amount);
                    remaining.shrink(amount);
                }
            }
            if (!remaining.isEmpty()) return false;
        }
        for (int slot = 0; slot < OUTPUTS; slot++) inventory.setItem(slot, result.get(slot));
        return true;
    }

    private void reset() {
        ticks = duration = draws = completedDraws = successfulDraws = mode = 0;
        paid = false; multiplied = false;
        pending.clear();
    }

    public CompoundTag save(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Ticks", ticks); tag.putInt("Duration", duration); tag.putInt("Draws", draws);
        tag.putInt("CompletedDraws", completedDraws); tag.putInt("SuccessfulDraws", successfulDraws); tag.putInt("Mode", mode); tag.putBoolean("Paid", paid);
        ListTag list = new ListTag();
        for (ItemStack stack : pending) if (!stack.isEmpty()) list.add(stack.save(registries));
        tag.put("Pending", list); tag.putBoolean("Multiplied", multiplied);
        return tag;
    }

    public void load(CompoundTag tag, HolderLookup.Provider registries) {
        reset();
        int time = tag.getInt("Duration"), count = tag.getInt("Draws");
        if (time < 40 || time > 200 || count < 1 || count > 9) return;
        duration = time; draws = count; ticks = Math.clamp(tag.getInt("Ticks"), 0, duration);
        completedDraws = Math.clamp(tag.getInt("CompletedDraws"), 0, draws);
        // Old saves counted only successful draws; keep their exact unpaid loot.
        successfulDraws = tag.contains("SuccessfulDraws", Tag.TAG_INT)
                ? Math.clamp(tag.getInt("SuccessfulDraws"), 0, completedDraws) : completedDraws;
        mode = Math.clamp(tag.getInt("Mode"), 0, 7); paid = tag.getBoolean("Paid"); multiplied = tag.getBoolean("Multiplied");
        ListTag list = tag.getList("Pending", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) ItemStack.parse(registries, list.getCompound(i)).filter(s -> !s.isEmpty()).ifPresent(pending::add);
        if (paid && (completedDraws != draws || successfulDraws == 0 || ticks < duration)) reset();
    }
}
