package com.appliedmoremachines;

import java.util.Map;
import java.util.Set;
import net.neoforged.fml.ModList;

/** Registration-time requirements from machine crafting materials and existing runtime requirements. */
public final class MachineAvailability {
    private static final Map<String, Set<String>> REQUIREMENTS = Map.ofEntries(
            Map.entry("ae_infusion_pedesta", Set.of("mysticalagriculture")),
            Map.entry("ae_crafting_core", Set.of("draconicevolution")),
            Map.entry("ae_enchanting_table", Set.of()),
            Map.entry("ae_fish", Set.of()),
            Map.entry("ae_brewing", Set.of()),
            Map.entry("ae_hidden_backup_power", Set.of("appflux")),
            Map.entry("blade_of_benevolence", Set.of("draconicevolution")),
            Map.entry("pickaxe_of_righteousness", Set.of("draconicevolution")),
            Map.entry("crystal_assembler_x8", Set.of("extendedae")),
            Map.entry("circuit_cutter_x8", Set.of("extendedae")),
            Map.entry("circuit_etcher_x8", Set.of("ae2cs")),
            Map.entry("overload_processing_factory_x8", Set.of("ae2lt")),
            Map.entry("inscriber_x8", Set.of()),
            Map.entry("inscriber_x64", Set.of()),
            Map.entry("ae_energizing_orb", Set.of("powah")),
            Map.entry("ae_factory_pattern", Set.of("mekanism")),
            Map.entry("ae_factory_controller", Set.of("mekanism")),
            Map.entry("ae_factory_casing", Set.of("mekanism")),
            Map.entry("ae_latex_extraction_processor", Set.of("industrialforegoing")),
            Map.entry("ae_dissolution_chamber_front", Set.of("industrialforegoing")),
            Map.entry("ae_fluid_laser_base", Set.of("industrialforegoing", "ifeu")),
            Map.entry("ae_reactors_shell", Set.of("bigreactors")),
            Map.entry("ae_reactors_controller", Set.of("bigreactors")),
            Map.entry("ae_reactors_pattern", Set.of("bigreactors")),
            Map.entry("ae_reactors_fuelrod", Set.of("bigreactors")));

    private MachineAvailability() {}

    public static Set<String> requiredMods(String machineId) {
        var required = REQUIREMENTS.get(machineId);
        if (required == null) throw new IllegalArgumentException("Missing machine requirements: " + machineId);
        return required;
    }

    public static boolean enabled(String machineId) {
        return requiredMods(machineId).stream()
                .allMatch(mod -> ModList.get() != null && ModList.get().isLoaded(mod));
    }
}
