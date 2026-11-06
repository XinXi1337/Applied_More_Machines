package com.appliedmoremachines.factory;

import com.appliedmoremachines.FactoryControllerBlockEntity;
import java.util.LinkedHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.neoforged.fml.ModList;

/** Authoritative formation updates; optional compatibility uses registry IDs only. */
public final class FactoryStructures {
    private static final ResourceLocation AIR = ResourceLocation.parse("minecraft:air");
    private static final ResourceLocation OXIDIZING_FACTORY = ResourceLocation.parse("mekmm:ultimate_oxidizing_factory");

    private FactoryStructures() {}

    public static boolean enabled() {
        return ModList.get().isLoaded("mekanism") && BuiltInRegistries.BLOCK.containsKey(OXIDIZING_FACTORY);
    }

    public static FactoryStructure.View view(Level level) {
        return new FactoryStructure.View() {
            @Override public boolean loaded(BlockPos pos) {
                return !level.isOutsideBuildHeight(pos) && level.hasChunkAt(pos);
            }
            @Override public ResourceLocation block(BlockPos pos) {
                return BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock());
            }
        };
    }

    /** Uses the clicked controller as the schematic's first controller anchor. */
    public static FactoryFormation prospective(Direction facing, BlockPos controller) {
        return prospective(null, facing, controller);
    }

    /**
     * Tries both controller anchors at the clicked controller's facing and keeps
     * the plan with the fewest conflicts, then the fewest missing blocks.
     */
    public static FactoryFormation prospective(FactoryStructure.View view, Direction facing, BlockPos controller) {
        // The schematic's front faces local south (+Z). Rotate that side so it
        // faces the player side represented by the controller's FACING state.
        int turns = switch (facing) {
            case WEST -> 1;
            case NORTH -> 2;
            case EAST -> 3;
            default -> 0;
        };
        FactoryStructure structure = FactoryStructure.definition();
        FactoryFormation best = null;
        int bestConflicts = Integer.MAX_VALUE;
        int bestMissing = Integer.MAX_VALUE;
        for (BlockPos anchor : structure.controllerOffsets()) {
            FactoryFormation candidate = new FactoryFormation(
                    controller.subtract(FactoryFormation.rotate(anchor, turns)), turns);
            if (view == null) return candidate;
            int conflicts = 0;
            int missing = 0;
            for (FactoryStructure.Cell cell : structure.cells()) {
                BlockPos pos = candidate.position(cell.offset());
                if (!view.loaded(pos)) {
                    conflicts++;
                    continue;
                }
                ResourceLocation actual = view.block(pos);
                if (FactoryMachineTiers.matches(cell.block(), actual)
                        || cell.block().equals(FactoryStructure.CASING) && FactoryStructure.PATTERN.equals(actual)) {
                    continue;
                }
                if (AIR.equals(actual)) missing++;
                else conflicts++;
            }
            if (conflicts < bestConflicts || conflicts == bestConflicts && missing < bestMissing) {
                best = candidate;
                bestConflicts = conflicts;
                bestMissing = missing;
            }
        }
        return best;
    }

    public static void refresh(FactoryControllerBlockEntity controller) {
        Level level = controller.getLevel();
        if (level == null || level.isClientSide) return;
        FactoryFormation next = enabled() ? FactoryStructure.definition().find(
                view(level), controller.getBlockPos(), controller.formation()) : null;
        update(controller, next);
    }

    public static void update(FactoryControllerBlockEntity controller, FactoryFormation next) {
        Level level = controller.getLevel();
        if (level == null || level.isClientSide) return;
        FactoryFormation previous = controller.formation();
        var positions = new LinkedHashSet<BlockPos>();
        positions.add(controller.getBlockPos());
        if (previous != null) FactoryStructure.definition().controllerOffsets().forEach(offset -> positions.add(previous.position(offset)));
        if (next != null) FactoryStructure.definition().controllerOffsets().forEach(offset -> positions.add(next.position(offset)));
        for (BlockPos pos : positions) {
            if (!level.hasChunkAt(pos)) continue;
            if (level.getBlockEntity(pos) instanceof FactoryControllerBlockEntity member) {
                boolean belongsToNext = next != null && FactoryStructure.definition().controllerOffsets()
                        .stream().anyMatch(offset -> next.position(offset).equals(pos));
                if (belongsToNext || member == controller || previous != null && previous.equals(member.formation())) {
                    member.setFormation(belongsToNext ? next : null);
                    if (belongsToNext) member.setProjectionState(controller.projectionEnabled());
                }
            }
        }
    }
}
