package com.appliedmoremachines.factory;

import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

/** Fixed block-ID pattern derived from the user schematic; never reads block-state properties. */
public final class FactoryStructure {
    public static final ResourceLocation CASING = id("applied_more_machines:ae_factory_casing");
    public static final ResourceLocation PATTERN = id("applied_more_machines:ae_factory_pattern");
    public static final ResourceLocation CONTROLLER = id("applied_more_machines:ae_factory_controller");
    private final List<Cell> cells;
    private final List<BlockPos> controllers;
    private final List<BlockPos> casings;

    public interface View {
        boolean loaded(BlockPos pos);
        ResourceLocation block(BlockPos pos);
    }

    public record Cell(BlockPos offset, ResourceLocation block) {}

    private FactoryStructure(List<Cell> cells) {
        this.cells = List.copyOf(cells);
        controllers = cells.stream().filter(cell -> cell.block().equals(CONTROLLER)).map(Cell::offset).toList();
        casings = cells.stream().filter(cell -> cell.block().equals(CASING)).map(Cell::offset).toList();
        if (cells.size() != 288 || controllers.size() != 2 || casings.size() != 26) {
            throw new IllegalStateException("Invalid AE factory structure definition");
        }
    }

    private static final class Holder {
        static final FactoryStructure INSTANCE = load();
    }

    public static FactoryStructure definition() {
        return Holder.INSTANCE;
    }

    public List<Cell> cells() { return cells; }
    public List<BlockPos> controllerOffsets() { return controllers; }
    public List<BlockPos> casingOffsets() { return casings; }

    public boolean matches(View view, FactoryFormation formation) {
        for (Cell cell : cells) {
            BlockPos pos = formation.position(cell.offset());
            if (!view.loaded(pos)) return false;
            ResourceLocation actual = view.block(pos);
            if (!FactoryMachineTiers.matches(cell.block(), actual)
                    && !(cell.block().equals(CASING) && PATTERN.equals(actual))) return false;
        }
        return true;
    }

    public FactoryFormation find(View view, BlockPos controller, FactoryFormation previous) {
        if (previous != null && controllers.stream().anyMatch(offset -> previous.position(offset).equals(controller))
                && matches(view, previous)) return previous;
        for (int turns = 0; turns < 4; turns++) {
            for (BlockPos offset : controllers) {
                var formation = new FactoryFormation(controller.subtract(FactoryFormation.rotate(offset, turns)), turns);
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

    /** Returns remaining casing blocks only; replaced pattern assemblies are not highlighted. */
    public List<BlockPos> replaceableCasings(View view, FactoryFormation formation) {
        if (!matches(view, formation)) return List.of();
        return casings.stream().map(formation::position).filter(pos -> CASING.equals(view.block(pos))).toList();
    }

    private static FactoryStructure load() {
        // Private schema: keep it outside data-pack directories scanned by other mods.
        String path = "/applied_more_machines/structures/ae_factory.json";
        try (var input = FactoryStructure.class.getResourceAsStream(path)) {
            if (input == null) throw new IllegalStateException("Missing factory structure: " + path);
            var root = JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject();
            var size = root.getAsJsonArray("size");
            int sx = size.get(0).getAsInt(), sy = size.get(1).getAsInt(), sz = size.get(2).getAsInt();
            var layers = root.getAsJsonArray("layers");
            if (sx != 14 || sy != 5 || sz != 14 || layers.size() != sy) throw new IllegalStateException("Invalid factory dimensions");
            var palette = root.getAsJsonObject("palette");
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
            return new FactoryStructure(cells);
        } catch (Exception error) {
            throw new IllegalStateException("Cannot load AE factory structure", error);
        }
    }

    private static ResourceLocation id(String value) { return ResourceLocation.parse(value); }
}
