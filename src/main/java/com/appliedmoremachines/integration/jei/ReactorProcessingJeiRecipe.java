package com.appliedmoremachines.integration.jei;

import java.util.List;
import net.minecraft.resources.ResourceLocation;

/** Display snapshots; alternatives share a cell, while simultaneous products occupy separate cells. */
public record ReactorProcessingJeiRecipe(ResourceLocation id, List<Cell> inputs, List<Cell> outputs) {
    public record Cell(List<?> alternatives) {
        public Cell { alternatives = List.copyOf(alternatives); }
    }

    public ReactorProcessingJeiRecipe {
        inputs = List.copyOf(inputs);
        outputs = List.copyOf(outputs);
    }

    public boolean fits() {
        return !inputs.isEmpty() && inputs.size() <= 9 && !outputs.isEmpty() && outputs.size() <= 9
                && inputs.stream().noneMatch(cell -> cell.alternatives().isEmpty())
                && outputs.stream().noneMatch(cell -> cell.alternatives().isEmpty());
    }
}
