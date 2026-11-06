package com.appliedmoremachines.reactor;

import com.appliedmoremachines.ReactorControllerBlockEntity;
import java.util.LinkedHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.neoforged.fml.ModList;

/** Authoritative formation updates; optional compatibility uses registry IDs only. */
public final class ReactorStructures {
    private static final ResourceLocation AIR = ResourceLocation.parse("minecraft:air");

    private ReactorStructures() {}

    public static boolean enabled() {
        return ModList.get().isLoaded("bigreactors");
    }

    public static ReactorStructure.View view(Level level) {
        return new ReactorStructure.View() {
            @Override public boolean loaded(BlockPos pos) {
                return !level.isOutsideBuildHeight(pos) && level.hasChunkAt(pos);
            }
            @Override public ResourceLocation block(BlockPos pos) {
                return BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock());
            }
        };
    }

    /** Uses the clicked controller as the schematic's first controller anchor. */
    public static ReactorFormation prospective(Direction facing, BlockPos controller) {
        return prospective(null, facing, controller);
    }

    /**
     * Tries both controller anchors at the clicked controller's facing and keeps
     * the plan with the fewest conflicts, then the fewest missing blocks.
     */
    public static ReactorFormation prospective(ReactorStructure.View view, Direction facing, BlockPos controller) {
        // Blueprint front is east (+X); preserve both fixed controller anchors.
        int turns = switch (facing) { case SOUTH -> 1; case WEST -> 2; case NORTH -> 3; default -> 0; };
        ReactorStructure structure = ReactorStructure.definition();
        ReactorFormation best = null;
        int bestConflicts = Integer.MAX_VALUE;
        int bestMissing = Integer.MAX_VALUE;
        for (BlockPos anchor : structure.controllerOffsets()) {
            ReactorFormation candidate = new ReactorFormation(
                    controller.subtract(ReactorFormation.rotate(anchor, turns)), turns);
            if (view == null) return candidate;
            int conflicts = 0;
            int missing = 0;
            for (ReactorStructure.Cell cell : structure.cells()) {
                BlockPos pos = candidate.position(cell.offset());
                if (!view.loaded(pos)) {
                    conflicts++;
                    continue;
                }
                ResourceLocation actual = view.block(pos);
                if (cell.block().equals(actual)) {
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

    public static void refresh(ReactorControllerBlockEntity controller) {
        Level level = controller.getLevel();
        if (level == null || level.isClientSide) return;
        ReactorFormation next = enabled() ? ReactorStructure.definition().find(
                view(level), controller.getBlockPos(), controller.formation()) : null;
        update(controller, next);
    }

    public static void update(ReactorControllerBlockEntity controller, ReactorFormation next) {
        Level level = controller.getLevel();
        if (level == null || level.isClientSide) return;
        ReactorFormation previous = controller.formation();
        var positions = new LinkedHashSet<BlockPos>();
        positions.add(controller.getBlockPos());
        if (previous != null) ReactorStructure.definition().controllerOffsets().forEach(offset -> positions.add(previous.position(offset)));
        if (next != null) ReactorStructure.definition().controllerOffsets().forEach(offset -> positions.add(next.position(offset)));
        for (BlockPos pos : positions) {
            if (!level.hasChunkAt(pos)) continue;
            if (level.getBlockEntity(pos) instanceof ReactorControllerBlockEntity member) {
                boolean belongsToNext = next != null && ReactorStructure.definition().controllerOffsets()
                        .stream().anyMatch(offset -> next.position(offset).equals(pos));
                if (belongsToNext || member == controller || previous != null && previous.equals(member.formation())) {
                    member.setFormation(belongsToNext ? next : null);
                    if (belongsToNext) member.setProjectionState(controller.projectionEnabled());
                }
            }
        }
    }
}
