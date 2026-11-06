package com.appliedmoremachines.latex;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

/** Two independent unpaid jobs. Inputs remain in real slots/tanks until an atomic local commit. */
public final class LatexProcessor {
    public static final int COST = 4000, CAPACITY = 16000;
    public static final int MANUAL = 0, PASSIVE = 1, LATEX = 2, RUBBER = 3;
    public interface Host {
        ItemStack input(); ItemStack output();
        int latex(); int water(); int mode();
        boolean powered();
        void pay(); void consumeLogs(int count); void consumeFluids(int latex, int water);
        void addLatex(int amount); void addRubber(ItemStack result);
        long sendLatex(long amount, boolean simulate);
        void dirty();
    }
    private ItemStack logs = ItemStack.EMPTY, result = ItemStack.EMPTY;
    private int extractionAmount, extractionTicks, extractionDuration;
    private int rubberLatex, rubberWater, rubberTicks, rubberDuration;
    // Only used if a storage accepts less than it simulated. Already paid, never charged again.
    private long pendingNetwork;
    public int extractionTicks() { return extractionTicks; }
    public int extractionDuration() { return extractionDuration; }
    public int rubberTicks() { return rubberTicks; }
    public int rubberDuration() { return rubberDuration; }
    public int reservedLatex() { return rubberLatex; }
    public int reservedWater() { return rubberWater; }
    public boolean locksLogs() { return !logs.isEmpty(); }
    public long pendingNetwork() { return pendingNetwork; }

    public void tick(Host host, boolean active, LatexUpgrades.Stats upgrades,
            LatexRecipes.Extraction extraction, LatexRecipes.Rubber rubber) {
        if (!active) return;
        if (pendingNetwork > 0) {
            long sent = Math.clamp(host.sendLatex(pendingNetwork, false), 0, pendingNetwork);
            if (sent > 0) { pendingNetwork -= sent; host.dirty(); }
        }
        if (!logs.isEmpty() && (!ItemStack.isSameItemSameComponents(logs, host.input())
                || host.input().getCount() < logs.getCount())) { clearExtraction(); host.dirty(); }
        if (!result.isEmpty() && (host.latex() < rubberLatex || host.water() < rubberWater)) {
            clearRubber(); host.dirty();
        }
        if (!result.isEmpty() && host.mode() == LATEX) {
            // This unpaid batch still owns no materials. Release its reservation so all tank latex can be exported.
            clearRubber(); host.dirty();
        }
        if (!host.powered()) return;
        if (logs.isEmpty() && pendingNetwork == 0 && extraction != null && extraction.amount() > 0) {
            int count = Math.min(upgrades.parallel(), Math.min(host.input().getCount(), Integer.MAX_VALUE / extraction.amount()));
            if (count > 0) {
                logs = host.input().copyWithCount(count);
                extractionAmount = extraction.amount() * count;
                extractionDuration = upgrades.ticks(false); extractionTicks = 0; host.dirty();
            }
        }
        if (result.isEmpty() && host.mode() != LATEX && rubber != null) {
            int count = Math.min(upgrades.parallel(), Math.min(host.latex() / rubber.latex(), host.water() / rubber.water()));
            count = Math.min(count, rubber.result().getMaxStackSize() / Math.max(1, rubber.result().getCount()));
            if (count > 0) {
                result = rubber.result().copyWithCount(rubber.result().getCount() * count);
                rubberLatex = rubber.latex() * count; rubberWater = rubber.water() * count;
                rubberDuration = upgrades.ticks(true); rubberTicks = 0; host.dirty();
            }
        }
        if (!logs.isEmpty() && pendingNetwork == 0) {
            if (extractionTicks < extractionDuration) { extractionTicks++; host.dirty(); }
            if (extractionTicks == extractionDuration) finishExtraction(host);
        }
        if (!result.isEmpty() && host.mode() != LATEX && host.powered()) {
            if (rubberTicks < rubberDuration) { rubberTicks++; host.dirty(); }
            if (rubberTicks == rubberDuration && fits(host.output(), result)) {
                host.pay(); host.consumeFluids(rubberLatex, rubberWater); host.addRubber(result.copy());
                clearRubber(); host.dirty();
            }
        }
    }

    private void finishExtraction(Host host) {
        int desired = switch (host.mode()) {
            case LATEX -> 0;
            case PASSIVE -> extractionAmount / 2;
            default -> extractionAmount;
        };
        int local = Math.min(desired, CAPACITY - host.latex());
        long network = (long) extractionAmount - local;
        if (network > 0 && host.sendLatex(network, true) < network) return;
        host.pay(); host.consumeLogs(logs.getCount()); host.addLatex(local);
        pendingNetwork = network;
        clearExtraction(); host.dirty();
        if (pendingNetwork > 0) {
            pendingNetwork -= Math.clamp(host.sendLatex(pendingNetwork, false), 0, pendingNetwork);
            host.dirty();
        }
    }
    public static boolean fits(ItemStack present, ItemStack added) {
        return !added.isEmpty() && (present.isEmpty() || ItemStack.isSameItemSameComponents(present, added))
                && (long) present.getCount() + added.getCount() <= added.getMaxStackSize();
    }
    private void clearExtraction() { logs = ItemStack.EMPTY; extractionAmount = extractionTicks = extractionDuration = 0; }
    private void clearRubber() { result = ItemStack.EMPTY; rubberLatex = rubberWater = rubberTicks = rubberDuration = 0; }
    public CompoundTag save(HolderLookup.Provider lookup) {
        CompoundTag tag = new CompoundTag();
        if (!logs.isEmpty()) tag.put("Logs", logs.save(lookup));
        if (!result.isEmpty()) tag.put("Result", result.save(lookup));
        tag.putInt("ExtractionAmount", extractionAmount); tag.putInt("ExtractionTicks", extractionTicks);
        tag.putInt("ExtractionDuration", extractionDuration); tag.putInt("RubberLatex", rubberLatex);
        tag.putInt("RubberWater", rubberWater); tag.putInt("RubberTicks", rubberTicks);
        tag.putInt("RubberDuration", rubberDuration); tag.putLong("PendingNetwork", pendingNetwork);
        return tag;
    }
    public void load(CompoundTag tag, HolderLookup.Provider lookup) {
        clearExtraction(); clearRubber();
        pendingNetwork = Math.clamp(tag.getLong("PendingNetwork"), 0, Integer.MAX_VALUE);
        ItemStack savedLogs = ItemStack.parseOptional(lookup, tag.getCompound("Logs"));
        int time = tag.getInt("ExtractionDuration"), amount = tag.getInt("ExtractionAmount");
        if (!savedLogs.isEmpty() && savedLogs.getCount() <= 7 && time > 0 && time <= 1280 && amount > 0) {
            logs = savedLogs; extractionDuration = time; extractionAmount = amount;
            extractionTicks = Math.clamp(tag.getInt("ExtractionTicks"), 0, time);
        }
        ItemStack savedResult = ItemStack.parseOptional(lookup, tag.getCompound("Result"));
        time = tag.getInt("RubberDuration"); int l = tag.getInt("RubberLatex"), w = tag.getInt("RubberWater");
        if (!savedResult.isEmpty() && savedResult.getCount() <= savedResult.getMaxStackSize()
                && time > 0 && time <= 160 && l > 0 && l <= CAPACITY && w > 0 && w <= CAPACITY) {
            result = savedResult; rubberDuration = time; rubberLatex = l; rubberWater = w;
            rubberTicks = Math.clamp(tag.getInt("RubberTicks"), 0, time);
        }
    }
}
