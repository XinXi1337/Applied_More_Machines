package com.appliedmoremachines.reactor;

import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

/** Fixed block-ID pattern derived from the user schematic; never reads block-state properties. */
public final class ReactorStructure {
    public static final ResourceLocation CASING = id("applied_more_machines:ae_reactors_shell");
    public static final ResourceLocation PATTERN = id("applied_more_machines:ae_reactors_pattern");
    public static final ResourceLocation CONTROLLER = id("applied_more_machines:ae_reactors_controller");
    private final List<Cell> cells;
    private final List<BlockPos> controllers;
    private final List<BlockPos> casings;
    private static final java.util.Map<BlockPos, java.util.Map<String,String>> STATES = new java.util.HashMap<>();

    public interface View {
        boolean loaded(BlockPos pos);
        ResourceLocation block(BlockPos pos);
    }

    public record Cell(BlockPos offset, ResourceLocation block) {}

    private ReactorStructure(List<Cell> cells) {
        this.cells = List.copyOf(cells);
        controllers = cells.stream().filter(cell -> cell.block().equals(CONTROLLER)).map(Cell::offset).toList();
        casings = cells.stream().filter(cell -> cell.block().equals(CASING)).map(Cell::offset).toList();
        if (cells.size() != 1008 || controllers.size() != 2 || casings.size() != 164) {
            throw new IllegalStateException("Invalid AE factory structure definition");
        }
    }

    private static final class Holder {
        static final ReactorStructure INSTANCE = load();
    }

    public static ReactorStructure definition() {
        return Holder.INSTANCE;
    }

    public List<Cell> cells() { return cells; }
    public List<BlockPos> controllerOffsets() { return controllers; }
    public List<BlockPos> casingOffsets() { return casings; }

    public boolean matches(View view, ReactorFormation formation) {
        for (Cell cell : cells) {
            BlockPos pos = formation.position(cell.offset());
            if (!view.loaded(pos)) return false;
            ResourceLocation actual = view.block(pos);
            if (!cell.block().equals(actual)) return false;
        }
        return true;
    }

    public ReactorFormation find(View view, BlockPos controller, ReactorFormation previous) {
        if (previous != null && controllers.stream().anyMatch(offset -> previous.position(offset).equals(controller))
                && matches(view, previous)) return previous;
        for (int turns = 0; turns < 4; turns++) {
            for (BlockPos offset : controllers) {
                var formation = new ReactorFormation(controller.subtract(ReactorFormation.rotate(offset, turns)), turns);
                // Check the two anchors before traversing the rest of the pattern.
                boolean anchors = controllers.stream().allMatch(anchor -> {
                    BlockPos pos = formation.position(anchor);
                    return view.loaded(pos) && CONTROLLER.equals(view.block(pos));
                });
                if (anchors && matches(view, formation)) return formation;
            }
        }
        return null;
    }

    public net.minecraft.world.level.block.state.BlockState state(Cell cell, int turns) {
        var state = net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(cell.block()).defaultBlockState();
        for (var entry : STATES.getOrDefault(cell.offset(), java.util.Map.of()).entrySet()) {
            var property = state.getBlock().getStateDefinition().getProperty(entry.getKey());
            if (property != null) state = property(state, property, entry.getValue());
        }
        return state.rotate(switch (Math.floorMod(turns,4)) {
            case 1 -> net.minecraft.world.level.block.Rotation.CLOCKWISE_90;
            case 2 -> net.minecraft.world.level.block.Rotation.CLOCKWISE_180;
            case 3 -> net.minecraft.world.level.block.Rotation.COUNTERCLOCKWISE_90;
            default -> net.minecraft.world.level.block.Rotation.NONE;
        });
    }
    private static <T extends Comparable<T>> net.minecraft.world.level.block.state.BlockState property(
            net.minecraft.world.level.block.state.BlockState state, net.minecraft.world.level.block.state.properties.Property<T> property, String value) {
        return property.getValue(value).map(v -> state.setValue(property,v)).orElse(state);
    }

    private static ReactorStructure load() {
        // Private schema: keep it outside data-pack directories scanned by other mods.
        String path = "/applied_more_machines/structures/ae_reactors.json";
        try (var input = ReactorStructure.class.getResourceAsStream(path)) {
            if (input == null) throw new IllegalStateException("Missing factory structure: " + path);
            var root = JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject();
            var size = root.getAsJsonArray("size");
            int sx = size.get(0).getAsInt(), sy = size.get(1).getAsInt(), sz = size.get(2).getAsInt();
            var layers = root.getAsJsonArray("layers");
            if (sx != 12 || sy != 11 || sz != 12 || layers.size() != sy) throw new IllegalStateException("Invalid factory dimensions");
            var palette = root.getAsJsonObject("palette");
            root.getAsJsonObject("states").entrySet().forEach(entry -> {
                String[] coordinates = entry.getKey().split(",");
                var properties = new java.util.HashMap<String,String>();
                entry.getValue().getAsJsonObject().entrySet().forEach(p -> properties.put(p.getKey(), p.getValue().getAsString()));
                STATES.put(new BlockPos(Integer.parseInt(coordinates[0]),Integer.parseInt(coordinates[1]),Integer.parseInt(coordinates[2])), properties);
            });
            var cells = new ArrayList<Cell>();
            for (int y = 0; y < sy; y++) {
                var rows = layers.get(y).getAsJsonArray();
                if (rows.size() != sz) throw new IllegalStateException("Invalid factory row count");
                for (int z = 0; z < sz; z++) {
                    String row = rows.get(z).getAsString();
                    if (row.length() != sx) throw new IllegalStateException("Invalid factory row width");
                    for (int x = 0; x < sx; x++) {
                        String symbol = row.substring(x, x + 1);
                        if (symbol.equals(".")) continue;
                        cells.add(new Cell(new BlockPos(x, y, z), id(palette.get(symbol).getAsString())));
                    }
                }
            }
            return new ReactorStructure(cells);
        } catch (Exception error) {
            throw new IllegalStateException("Cannot load AE factory structure", error);
        }
    }

    private static ResourceLocation id(String value) { return ResourceLocation.parse(value); }
}
