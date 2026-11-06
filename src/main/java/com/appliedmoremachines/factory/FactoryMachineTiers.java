package com.appliedmoremachines.factory;

import com.appliedmoremachines.FactoryUpgradeMath;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

/** Exact machine families in the fixed structure; higher tiers do not unlock new recipe families. */
public final class FactoryMachineTiers {
    public enum Tier {
        ULTIMATE("ultimate", 0), ABSOLUTE("absolute", 16), SUPREME("supreme", 16),
        COSMIC("cosmic", 32), INFINITE("infinite", 64);
        private final String id;
        private final int cost;
        private final ResourceLocation installer;
        Tier(String id, int cost) {
            this.id = id; this.cost = cost;
            this.installer = ResourceLocation.parse("mekanism_extras:" + id + "_tier_installer");
        }
        public int cost() { return cost; }
        public String translationKey() { return "gui.applied_more_machines.factory_tier." + id; }
        public ResourceLocation installer() { return installer; }
        public FactoryUpgradeMath.Metrics apply(FactoryUpgradeMath.Metrics base) {
            return switch (this) {
                case ULTIMATE -> base;
                case ABSOLUTE -> new FactoryUpgradeMath.Metrics(base.parallel() + 256, base.durationTicks());
                case SUPREME -> new FactoryUpgradeMath.Metrics(base.parallel() + 512, base.durationTicks());
                case COSMIC -> new FactoryUpgradeMath.Metrics(base.parallel() + 2048, (base.durationTicks() + 1) / 2);
                case INFINITE -> new FactoryUpgradeMath.Metrics((base.parallel() + 2048) * 2, 1);
            };
        }
        public boolean allows(int current) {
            return ordinal() > 0 && (ordinal() < COSMIC.ordinal() ? current >= 0 && current < ordinal()
                    : current == ordinal() - 1);
        }
    }
    private static final Map<ResourceLocation, String> FAMILIES = families();
    private static final Map<ResourceLocation, List<ResourceLocation>> TARGETS = targets();
    private static final Map<ResourceLocation, ResourceLocation> BASES = bases();
    private static final class MachineCells {
        static final List<FactoryStructure.Cell> ALL = FactoryStructure.definition().cells().stream()
                .filter(cell -> FAMILIES.containsKey(cell.block())).toList();
    }
    private FactoryMachineTiers() {}
    private static Map<ResourceLocation, String> families() {
        Map<ResourceLocation, String> result = new LinkedHashMap<>();
        for (String type : List.of("smelting", "enriching", "crushing", "sawing", "purifying", "injecting", "infusing", "compressing")) {
            result.put(id("mekanism:ultimate_" + type + "_factory"), type);
        }
        result.put(id("mekmm:ultimate_oxidizing_factory"), "oxidizing");
        result.put(id("mekanism:chemical_dissolution_chamber"), "dissolving");
        result.put(id("mekanism:chemical_washer"), "washing");
        result.put(id("mekanism:pressurized_reaction_chamber"), "pressurised_reacting");
        result.put(id("mekanism:nutritional_liquifier"), "liquifying");
        return Map.copyOf(result);
    }
    public static ResourceLocation target(ResourceLocation base, Tier tier) {
        var targets = TARGETS.get(base);
        return targets == null ? null : targets.get(tier.ordinal());
    }
    public static int rank(ResourceLocation base, ResourceLocation actual) {
        if (base.equals(actual)) return 0;
        var targets = TARGETS.get(base);
        return targets == null ? -1 : targets.indexOf(actual);
    }
    public static boolean matches(ResourceLocation base, ResourceLocation actual) { return rank(base, actual) >= 0; }

    /** Recipe lookup uses the same base IDs before and after a physical machine replacement. */
    public static ResourceLocation baseMachine(ResourceLocation actual) {
        return BASES.getOrDefault(actual, actual);
    }
    public static Tier installed(FactoryStructure.View view, FactoryFormation formation) {
        return installed(view, formation, BuiltInRegistries.BLOCK::containsKey);
    }
    public static Tier installed(FactoryStructure.View view, FactoryFormation formation, Predicate<ResourceLocation> exists) {
        if (formation == null) return Tier.ULTIMATE;
        for (int i = Tier.INFINITE.ordinal(); i > 0; i--) {
            Tier tier = Tier.values()[i];
            boolean any = false, complete = true;
            for (var cell : MachineCells.ALL) {
                ResourceLocation target = target(cell.block(), tier);
                if (target == null || !exists.test(target)) continue;
                any = true;
                BlockPos pos = formation.position(cell.offset());
                if (!view.loaded(pos) || rank(cell.block(), view.block(pos)) < i) { complete = false; break; }
            }
            if (any && complete) return tier;
        }
        return Tier.ULTIMATE;
    }
    public static Tier installer(ResourceLocation item) {
        for (Tier tier : Tier.values()) if (tier != Tier.ULTIMATE && tier.installer().equals(item)) return tier;
        return null;
    }
    private static Map<ResourceLocation, List<ResourceLocation>> targets() {
        Map<ResourceLocation, List<ResourceLocation>> result = new LinkedHashMap<>();
        FAMILIES.forEach((base, family) -> result.put(base, java.util.Arrays.stream(Tier.values())
                .map(tier -> tier == Tier.ULTIMATE ? base : id("mekanism_extras:" + tier.id + "_" + family + "_factory"))
                .toList()));
        return Map.copyOf(result);
    }
    private static Map<ResourceLocation, ResourceLocation> bases() {
        Map<ResourceLocation, ResourceLocation> result = new LinkedHashMap<>();
        TARGETS.forEach((base, targets) -> targets.forEach(target -> result.put(target, base)));
        return Map.copyOf(result);
    }
    private static ResourceLocation id(String value) { return ResourceLocation.parse(value); }
}
