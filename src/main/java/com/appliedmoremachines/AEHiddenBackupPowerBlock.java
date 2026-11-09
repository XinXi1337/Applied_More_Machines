package com.appliedmoremachines;

import com.mojang.serialization.MapCodec;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;

public final class AEHiddenBackupPowerBlock extends Block implements EntityBlock {
    public static final MapCodec<AEHiddenBackupPowerBlock> CODEC = simpleCodec(AEHiddenBackupPowerBlock::new);
    public AEHiddenBackupPowerBlock(BlockBehaviour.Properties properties) { super(properties); }
    @Override public MapCodec<AEHiddenBackupPowerBlock> codec() { return CODEC; }
    @Override public void appendHoverText(ItemStack stack, net.minecraft.world.item.Item.TooltipContext context,
            List<net.minecraft.network.chat.Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(net.minecraft.network.chat.Component.translatable("tooltip.applied_more_machines.ae_hidden_backup_power")
                .withStyle(net.minecraft.ChatFormatting.GRAY));
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AEHiddenBackupPowerBlockEntity(pos, state);
    }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || type != ModBlockEntities.AE_HIDDEN_BACKUP_POWER.get()) return null;
        return (world, pos, current, entity) -> ((AEHiddenBackupPowerBlockEntity) entity).serverTick();
    }
    @Override protected MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof AEHiddenBackupPowerBlockEntity host ? host : null;
    }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof AEHiddenBackupPowerBlockEntity host) player.openMenu(host);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof AEHiddenBackupPowerBlockEntity host) {
            host.restoreItemEnergy(stack);
            if (placer instanceof Player player) host.setOwner(player);
        }
    }
    @Override protected List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        var drops = super.getDrops(state, builder);
        if (builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof AEHiddenBackupPowerBlockEntity host)
            for (ItemStack drop : drops) if (drop.is(asItem())) host.storeItemEnergy(drop);
        return drops;
    }
    @Override protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        if (!level.isClientSide && !state.is(next.getBlock())
                && level.getBlockEntity(pos) instanceof AEHiddenBackupPowerBlockEntity host) {
            var disk = host.removeItemNoUpdate(0);
            com.appliedmoremachines.processing.AltarInputs.splitForExit(disk, stack ->
                    Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack));
            level.updateNeighbourForOutputSignal(pos, this);
        }
        super.onRemove(state, level, pos, next, moving);
    }
}
