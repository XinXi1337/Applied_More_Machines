package com.appliedmoremachines.factory;

import com.appliedmoremachines.factory.FactoryMachineTiers.Tier;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

/** Preflight every replacement, then commit as one controller interaction. */
public final class FactoryTierUpgradePlan {
    public enum Result { SUCCESS, NOT_FORMED, BUSY, COUNT, PREREQUISITE, NO_TARGET, FAILED, ACCESS }
    public record Change(BlockPos pos, ResourceLocation before, ResourceLocation after) {}
    public record Plan(Result result, List<Change> changes) {}
    public interface Replacement {
        boolean apply();
        void restore();
    }
    private FactoryTierUpgradePlan() {}
    public static Plan plan(FactoryStructure.View view, FactoryFormation formation, Tier target,
            int held, boolean busy, Predicate<ResourceLocation> exists) {
        if (formation == null || !FactoryStructure.definition().matches(view, formation)) return new Plan(Result.NOT_FORMED, List.of());
        if (busy) return new Plan(Result.BUSY, List.of());
        if (held < target.cost()) return new Plan(Result.COUNT, List.of());
        List<Change> changes = new ArrayList<>();
        for (var cell : FactoryStructure.definition().cells()) {
            ResourceLocation to = FactoryMachineTiers.target(cell.block(), target);
            if (to == null || !exists.test(to)) continue;
            BlockPos pos = formation.position(cell.offset());
            ResourceLocation actual = view.block(pos);
            int rank = FactoryMachineTiers.rank(cell.block(), actual);
            // The first two installers may fill a partially upgraded structure; never downgrade.
            if (target.ordinal() < Tier.COSMIC.ordinal() && rank == target.ordinal()) continue;
            if (!target.allows(rank)) return new Plan(Result.PREREQUISITE, List.of());
            changes.add(new Change(pos, actual, to));
        }
        return new Plan(changes.isEmpty() ? Result.NO_TARGET : Result.SUCCESS, List.copyOf(changes));
    }
    public static boolean commit(List<? extends Replacement> replacements) {
        int attempted = 0;
        try {
            for (var replacement : replacements) {
                attempted++;
                if (!replacement.apply()) throw new IllegalStateException("Factory machine replacement failed");
            }
            return true;
        } catch (RuntimeException ex) {
            RuntimeException failure = null;
            for (int i = attempted - 1; i >= 0; i--) {
                try { replacements.get(i).restore(); }
                catch (RuntimeException restoreError) { if (failure == null) failure = restoreError; else failure.addSuppressed(restoreError); }
            }
            if (failure != null) throw new IllegalStateException("Could not restore factory upgrade", failure);
            return false;
        }
    }
}
