package com.appliedmoremachines.integration;

import com.appliedmoremachines.items.VirtueTools;
import com.brandon3055.draconicevolution.blocks.tileentity.TileChaosCrystal;
import com.brandon3055.draconicevolution.entity.guardian.DraconicGuardianEntity;
import com.brandon3055.draconicevolution.entity.guardian.DraconicGuardianPartEntity;
import java.util.function.BooleanSupplier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/** Optional DE bridge. Bypass is confined to one synchronous player removal and its own crystal parts. */
public final class DraconicVirtueTools {
    private record Removal(Level level, BlockPos root) {}
    private static final ThreadLocal<Removal> REMOVAL = new ThreadLocal<>();
    private DraconicVirtueTools() {}
    public static void killGuardian(Entity target) {
        DraconicGuardianEntity guardian = target instanceof DraconicGuardianEntity dragon ? dragon
                : target instanceof DraconicGuardianPartEntity part ? part.dragon : null;
        if (guardian != null && !guardian.isRemoved()) guardian.kill();
    }
    public static boolean remove(Level level, BlockPos pos, Player player, BooleanSupplier original) {
        if (!VirtueTools.isPick(player.getMainHandItem()) || !(level.getBlockEntity(pos) instanceof TileChaosCrystal tile))
            return original.getAsBoolean();
        BlockPos root = tile.parentPos.isNull() ? pos : tile.parentPos.get();
        if (!level.hasChunkAt(root) || !(level.getBlockEntity(root) instanceof TileChaosCrystal)) return original.getAsBoolean();
        Removal previous = REMOVAL.get();
        REMOVAL.set(new Removal(level, root.immutable()));
        try { return original.getAsBoolean(); }
        finally { if (previous == null) REMOVAL.remove(); else REMOVAL.set(previous); }
    }
    public static boolean bypass(TileChaosCrystal tile) {
        Removal scope = REMOVAL.get();
        return scope != null && tile.getLevel() == scope.level &&
                (tile.getBlockPos().equals(scope.root) || scope.root.equals(tile.parentPos.get()));
    }
}
