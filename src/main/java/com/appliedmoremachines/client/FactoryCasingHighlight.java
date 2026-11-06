package com.appliedmoremachines.client;

import com.appliedmoremachines.AppliedMoreMachines;
import com.appliedmoremachines.FactoryControllerBlockEntity;
import com.appliedmoremachines.ModBlocks;
import com.appliedmoremachines.factory.FactoryFormation;
import com.appliedmoremachines.factory.FactoryStructure;
import com.appliedmoremachines.factory.FactoryStructures;
import java.util.LinkedHashSet;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.event.level.LevelEvent;

@EventBusSubscriber(modid = AppliedMoreMachines.MOD_ID, value = Dist.CLIENT)
public final class FactoryCasingHighlight {
    private static ClientLevel cachedLevel;
    private static long checkedAt = Long.MIN_VALUE;
    private static List<BlockPos> targets = List.of();
    private FactoryCasingHighlight() {}

    @SubscribeEvent public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() == cachedLevel) {
            cachedLevel = null;
            targets = List.of();
        }
    }

    @SubscribeEvent public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES) return;
        Minecraft minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        ClientLevel level = minecraft.level;
        if (level == null || player == null || !FactoryStructures.enabled()
                || !(player.getMainHandItem().is(ModBlocks.AE_FACTORY_PATTERN_ITEM.get())
                || player.getOffhandItem().is(ModBlocks.AE_FACTORY_PATTERN_ITEM.get()))) {
            cachedLevel = null;
            targets = List.of();
            return;
        }
        if (cachedLevel != level || level.getGameTime() - checkedAt >= 5) {
            cachedLevel = level;
            checkedAt = level.getGameTime();
            var formations = new LinkedHashSet<FactoryFormation>();
            int cx = player.blockPosition().getX() >> 4, cz = player.blockPosition().getZ() >> 4;
            for (int x = cx - 3; x <= cx + 3; x++) for (int z = cz - 3; z <= cz + 3; z++) {
                var chunk = level.getChunkSource().getChunk(x, z, ChunkStatus.FULL, false);
                if (chunk == null) continue;
                for (var entity : chunk.getBlockEntities().values()) {
                    if (entity instanceof FactoryControllerBlockEntity controller && controller.isFormed()) {
                        formations.add(controller.formation());
                    }
                }
            }
            var positions = new LinkedHashSet<BlockPos>();
            var view = FactoryStructures.view(level);
            for (var formation : formations) positions.addAll(FactoryStructure.definition().replaceableCasings(view, formation));
            targets = positions.stream().filter(pos -> pos.distToCenterSqr(player.position()) <= 32 * 32).toList();
        }
        if (targets.isEmpty()) return;
        var pose = event.getPoseStack();
        var camera = event.getCamera().getPosition();
        var buffers = minecraft.renderBuffers().bufferSource();
        var lines = buffers.getBuffer(RenderType.lines());
        pose.pushPose();
        pose.translate(-camera.x, -camera.y, -camera.z);
        for (BlockPos pos : targets) {
            if (!level.getBlockState(pos).is(ModBlocks.AE_FACTORY_CASING.get())) continue;
            LevelRenderer.renderLineBox(pose, lines, new AABB(pos).inflate(0.003), 0.15F, 0.55F, 1.0F, 1.0F);
        }
        pose.popPose();
        buffers.endBatch(RenderType.lines());
    }
}
