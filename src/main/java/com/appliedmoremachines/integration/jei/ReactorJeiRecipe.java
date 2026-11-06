package com.appliedmoremachines.integration.jei;

import com.appliedmoremachines.reactor.ReactorStructure;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Registry-backed material counts and block states for the factory structure preview. */
public record ReactorJeiRecipe(List<Material> materials, List<Cell> cells) {
    public ReactorJeiRecipe {
        materials = List.copyOf(materials);
        cells = List.copyOf(cells);
    }

    public static ReactorJeiRecipe create() {
        Map<ResourceLocation, Integer> counts = new TreeMap<>();
        List<Cell> cells = new ArrayList<>();
        for (ReactorStructure.Cell cell : ReactorStructure.definition().cells()) {
            Block block = BuiltInRegistries.BLOCK.getOptional(cell.block()).orElseThrow(
                    () -> new IllegalStateException("Missing factory preview block: " + cell.block()));
            BlockState state = ReactorStructure.definition().state(cell, 0);
            counts.merge(cell.block(), 1, Integer::sum);
            cells.add(new Cell(cell.offset(), state));
        }
        List<Material> materials = counts.entrySet().stream()
                .map(entry -> {
                    Block block = BuiltInRegistries.BLOCK.getOptional(entry.getKey()).orElseThrow();
                    return new Material(entry.getKey(), block, entry.getValue());
                })
                .sorted(Comparator.comparing(Material::id))
                .toList();
        return new ReactorJeiRecipe(materials, cells);
    }

    public record Material(ResourceLocation id, Block block, int count) {}
    public record Cell(BlockPos offset, BlockState state) {}
}
