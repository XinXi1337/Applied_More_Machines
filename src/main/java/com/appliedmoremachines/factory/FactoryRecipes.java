package com.appliedmoremachines.factory;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEFluidKey;
import appeng.crafting.pattern.AEProcessingPattern;
import com.appliedmoremachines.integration.MekanismFactoryRecipes;
import com.appliedmoremachines.integration.FactoryChemicalKeys;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.neoforged.fml.ModList;

/** One native execution, never a ratio-normalized or player-scaled recipe. */
public final class FactoryRecipes {
    public static final int BULK_MULTIPLIER = 1_000;
    private static long generation;
    private FactoryRecipes() {}
    public static void invalidate() { generation++; }
    public static long generation() { return generation; }
    public static boolean chemicalsAvailable() {
        return ModList.get().isLoaded("mekanism") && ModList.get().isLoaded("appmek") && FactoryChemicalKeys.available();
    }
    public record Match(String recipe, Map<AEKey, Long> inputs, Map<AEKey, Long> outputs, Map<AEKey, Long> remainders) {
        public Match(String recipe, Map<AEKey, Long> inputs, Map<AEKey, Long> outputs) {
            this(recipe, inputs, outputs, Map.of());
        }
        /** More native operations per round; encoded quantities and controller time stay native. */
        public int parallelMultiplier() {
            return !inputs.isEmpty() && !outputs.isEmpty()
                    && inputs.keySet().stream().allMatch(FactoryRecipes::bulkKey)
                    && outputs.keySet().stream().allMatch(FactoryRecipes::bulkKey) ? BULK_MULTIPLIER : 1;
        }
    }

    private static boolean bulkKey(AEKey key) {
        return key instanceof AEFluidKey || chemicalsAvailable() && FactoryChemicalKeys.isChemical(key);
    }

    public static Match match(Level level, Set<ResourceLocation> machines, IPatternDetails details,
            Map<AEKey, Long> delivered) {
        if (!(details instanceof AEProcessingPattern pattern) || !ModList.get().isLoaded("mekanism")) return null;
        try {
            Map<AEKey, Long> encoded = FactoryAmounts.of(pattern.getSparseInputs());
            if (delivered != null && !encoded.equals(delivered)) return null;
            Map<AEKey, Long> outputs = FactoryAmounts.of(pattern.getOutputs());
            if (encoded.isEmpty() || outputs.isEmpty()) return null;
            return MekanismFactoryRecipes.match(level, machines, encoded, outputs);
        } catch (ArithmeticException | IllegalArgumentException ex) {
            return null;
        }
    }
}
