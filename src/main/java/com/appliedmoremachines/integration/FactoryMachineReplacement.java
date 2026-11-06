package com.appliedmoremachines.integration;

import com.appliedmoremachines.factory.FactoryTierUpgradePlan;
import mekanism.api.security.IBlockSecurityUtils;
import mekanism.common.tile.base.TileEntityMekanism;
import mekanism.common.upgrade.IUpgradeData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Isolated Mekanism migration. Prepares native upgrade data before changing any world block. */
public final class FactoryMachineReplacement implements FactoryTierUpgradePlan.Replacement {
    private final ServerLevel level;
    private final BlockPos pos;
    private final BlockState before, after;
    private final CompoundTag snapshot;
    private final TileEntityMekanism upgraded;

    private FactoryMachineReplacement(ServerLevel level, BlockPos pos, BlockState before, BlockState after,
            CompoundTag snapshot, TileEntityMekanism upgraded) {
        this.level = level; this.pos = pos; this.before = before; this.after = after;
        this.snapshot = snapshot; this.upgraded = upgraded;
    }

    public static boolean accessible(ServerLevel level, BlockPos pos, Player player) {
        return IBlockSecurityUtils.INSTANCE.canAccessOrDisplayError(player, level, pos);
    }

    public static FactoryMachineReplacement prepare(ServerLevel level, FactoryTierUpgradePlan.Change change) {
        BlockState before = level.getBlockState(change.pos());
        if (!BuiltInRegistries.BLOCK.getKey(before.getBlock()).equals(change.before())) return null;
        if (!(level.getBlockEntity(change.pos()) instanceof TileEntityMekanism old) || !old.playersUsing.isEmpty()) return null;
        Block target = BuiltInRegistries.BLOCK.get(change.after());
        if (!(target instanceof EntityBlock factory)) return null;
        BlockState after = target.withPropertiesOf(before);
        if (!(factory.newBlockEntity(change.pos(), after) instanceof TileEntityMekanism upgraded)) return null;
        IUpgradeData data = old.getUpgradeData(level.registryAccess());
        IUpgradeData expected = upgraded.getUpgradeData(level.registryAccess());
        // MekMM supplies migration data for the original chemical/liquifying machines via its mixins.
        // Reject an unavailable migration path before consuming anything or changing the world.
        if (data == null || expected == null || !expected.getClass().isInstance(data)) return null;
        CompoundTag snapshot = old.saveWithFullMetadata(level.registryAccess());
        upgraded.parseUpgradeData(level.registryAccess(), data);
        upgraded.setBlockState(after);
        return new FactoryMachineReplacement(level, change.pos(), before, after, snapshot, upgraded);
    }

    @Override public boolean apply() {
        if (!level.getBlockState(pos).equals(before)) return false;
        // Detach the old tile so replacement hooks cannot drop inventory already migrated above.
        level.removeBlockEntity(pos);
        if (!level.setBlock(pos, after, Block.UPDATE_CLIENTS)) return false;
        level.removeBlockEntity(pos);
        level.setBlockEntity(upgraded);
        upgraded.setChanged();
        return level.getBlockEntity(pos) == upgraded;
    }

    @Override public void restore() {
        level.removeBlockEntity(pos);
        if (!level.getBlockState(pos).equals(before) && !level.setBlock(pos, before, Block.UPDATE_CLIENTS)) {
            throw new IllegalStateException("Failed restoring factory machine at " + pos);
        }
        BlockEntity restored = BlockEntity.loadStatic(pos, before, snapshot, level.registryAccess());
        if (restored == null) throw new IllegalStateException("Missing factory machine snapshot at " + pos);
        level.removeBlockEntity(pos);
        level.setBlockEntity(restored);
        restored.setChanged();
        level.sendBlockUpdated(pos, before, before, Block.UPDATE_ALL);
    }

    public void finish() {
        upgraded.resyncMasterToBounding();
        upgraded.sendUpdatePacket();
        upgraded.invalidateCapabilitiesFull();
        level.sendBlockUpdated(pos, before, after, Block.UPDATE_ALL);
    }
}
