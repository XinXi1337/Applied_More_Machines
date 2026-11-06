package com.appliedmoremachines.factory;

import net.minecraft.core.BlockPos;

/** One shared identity for both controllers of a rotated factory. */
public record FactoryFormation(BlockPos origin, int quarterTurns) {
    public FactoryFormation {
        origin = origin.immutable();
        quarterTurns = Math.floorMod(quarterTurns, 4);
    }

    public BlockPos position(BlockPos offset) {
        return origin.offset(rotate(offset, quarterTurns));
    }

    public static BlockPos rotate(BlockPos offset, int turns) {
        int x = offset.getX(), y = offset.getY(), z = offset.getZ();
        return switch (Math.floorMod(turns, 4)) {
            case 1 -> new BlockPos(-z, y, x);
            case 2 -> new BlockPos(-x, y, -z);
            case 3 -> new BlockPos(z, y, -x);
            default -> offset;
        };
    }
}
