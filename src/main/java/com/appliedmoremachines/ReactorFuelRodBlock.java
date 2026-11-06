package com.appliedmoremachines;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/** Three-axis fuel rod exterior; the hollow frame must not occlude its interior. */
public final class ReactorFuelRodBlock extends RotatedPillarBlock {
    public static final MapCodec<ReactorFuelRodBlock> CODEC = simpleCodec(ReactorFuelRodBlock::new);

    public ReactorFuelRodBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<? extends ReactorFuelRodBlock> codec() {
        return CODEC;
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    @Override
    protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 1.0F;
    }
}
