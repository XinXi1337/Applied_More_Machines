package com.appliedmoremachines.factory;

import com.appliedmoremachines.AppliedMoreMachines;
import com.appliedmoremachines.ModBlocks;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/** Factory controllers and pattern assemblies predate their block entities in existing chunks. */
@EventBusSubscriber(modid = AppliedMoreMachines.MOD_ID)
public final class FactoryControllerMigration {
    private static final Map<ServerLevel, Set<ChunkPos>> PENDING = new ConcurrentHashMap<>();
    private FactoryControllerMigration() {}

    @SubscribeEvent public static void onChunkLoad(ChunkEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel level) {
            PENDING.computeIfAbsent(level, ignored -> ConcurrentHashMap.newKeySet()).add(event.getChunk().getPos());
        }
    }

    @SubscribeEvent public static void onChunkUnload(ChunkEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) {
            Set<ChunkPos> pending = PENDING.get(level);
            if (pending != null) pending.remove(event.getChunk().getPos());
        }
    }

    @SubscribeEvent public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) PENDING.remove(level);
    }

    @SubscribeEvent public static void afterLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        Set<ChunkPos> pending = PENDING.get(level);
        if (pending == null || pending.isEmpty()) return;
        for (var iterator = pending.iterator(); iterator.hasNext();) {
            ChunkPos pos = iterator.next();
            LevelChunk chunk = level.getChunkSource().getChunkNow(pos.x, pos.z);
            if (chunk == null) continue;
            iterator.remove();
            var sections = chunk.getSections();
            for (int index = 0; index < sections.length; index++) {
                var section = sections[index];
                if (!section.maybeHas(state -> state.is(ModBlocks.AE_FACTORY_CONTROLLER.get())
                        || state.is(ModBlocks.AE_FACTORY_PATTERN.get()))) continue;
                int minY = level.getSectionYFromSectionIndex(index) << 4;
                for (int y = 0; y < 16; y++) for (int z = 0; z < 16; z++) for (int x = 0; x < 16; x++) {
                    var state = section.getBlockState(x, y, z);
                    if (!state.is(ModBlocks.AE_FACTORY_CONTROLLER.get())
                            && !state.is(ModBlocks.AE_FACTORY_PATTERN.get())) continue;
                    var blockPos = new BlockPos(pos.getMinBlockX() + x, minY + y, pos.getMinBlockZ() + z);
                    chunk.getBlockEntity(blockPos, LevelChunk.EntityCreationType.IMMEDIATE);
                }
            }
        }
    }
}
