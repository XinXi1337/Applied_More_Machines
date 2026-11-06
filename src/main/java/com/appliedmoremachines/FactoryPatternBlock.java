package com.appliedmoremachines;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Factory pattern assembly with a persistent pattern inventory. */
public final class FactoryPatternBlock extends FactoryComponentBlock implements EntityBlock {
    public static final MapCodec<FactoryPatternBlock> CODEC = simpleCodec(FactoryPatternBlock::new);

    public FactoryPatternBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override public MapCodec<? extends FactoryComponentBlock> codec() { return CODEC; }

    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FactoryPatternBlockEntity(pos, state);
    }

    @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide && placer instanceof Player player
                && level.getBlockEntity(pos) instanceof FactoryPatternBlockEntity pattern) {
            pattern.setOwner(player);
        }
    }

    @Override protected MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof FactoryPatternBlockEntity pattern ? pattern : null;
    }

    @Override protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof FactoryPatternBlockEntity pattern) {
            pattern.setOwner(player);
            player.openMenu(pattern);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (!level.isClientSide && !state.is(newState.getBlock())
                && level.getBlockEntity(pos) instanceof FactoryPatternBlockEntity pattern) {
            for (int slot = 0; slot < pattern.getContainerSize(); slot++) {
                ItemStack stack = pattern.getItem(slot).copy();
                if (!stack.isEmpty()) {
                    Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
                    pattern.setItem(slot, ItemStack.EMPTY);
                }
            }
            pattern.clearContent();
        }
        super.onRemove(state, level, pos, newState, moving);
    }
}
