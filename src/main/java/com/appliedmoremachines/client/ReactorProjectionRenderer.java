package com.appliedmoremachines.client;

import com.appliedmoremachines.AppliedMoreMachines;
import com.appliedmoremachines.ReactorComponentBlock;
import com.appliedmoremachines.ReactorControllerBlockEntity;
import com.appliedmoremachines.reactor.ReactorFormation;
import com.appliedmoremachines.reactor.ReactorStructure;
import com.appliedmoremachines.reactor.ReactorStructures;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.pipeline.VertexConsumerWrapper;
import net.neoforged.neoforge.event.level.LevelEvent;

/** Persistent 64% block-model hologram with per-block edges and conflict coloring. */
@EventBusSubscriber(modid = AppliedMoreMachines.MOD_ID, value = Dist.CLIENT)
public final class ReactorProjectionRenderer {
    private static final float ALPHA = 0.64F;
    private static ClientLevel cachedLevel;
    private static long checkedAt = Long.MIN_VALUE;
    private static List<Projection> projections = List.of();

    private ReactorProjectionRenderer() {}

    @SubscribeEvent public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() == cachedLevel) {
            cachedLevel = null;
            checkedAt = Long.MIN_VALUE;
            projections = List.of();
        }
    }

    @SubscribeEvent public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES) return;
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || minecraft.player == null || !ReactorStructures.enabled()) {
            projections = List.of();
            return;
        }
        if (cachedLevel != level || level.getGameTime() - checkedAt >= 5) {
            cachedLevel = level;
            checkedAt = level.getGameTime();
            projections = findProjections(level);
        }
        if (projections.isEmpty()) return;

        PoseStack pose = event.getPoseStack();
        var camera = event.getCamera().getPosition();
        var buffers = minecraft.renderBuffers().bufferSource();
        var faces = buffers.getBuffer(RenderType.translucent());
        pose.pushPose();
        pose.translate(-camera.x, -camera.y, -camera.z);

        for (Projection projection : projections) {
            for (var entry : projection.states().entrySet()) {
                BlockPos pos = entry.getKey();
                boolean conflict = entry.getValue().conflict();
                var state = entry.getValue().state();
                if (state.getRenderShape() != RenderShape.MODEL) continue;
                pose.pushPose();
                pose.translate(pos.getX(), pos.getY(), pos.getZ());
                MultiBufferSource tinted = ignored -> conflict
                        ? new TintedVertexConsumer(faces, 1.0F, 0.16F, 0.16F)
                        : new TintedVertexConsumer(faces, 0.42F, 0.72F, 1.0F);
                minecraft.getBlockRenderer().renderSingleBlock(state, pose, tinted,
                        LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, ModelData.EMPTY, RenderType.translucent());
                pose.popPose();
            }
        }
        buffers.endBatch(RenderType.translucent());

        // BufferSource scratch space is shared. The block pass must be submitted
        // before requesting LINES, otherwise its builder is already closed.
        var lines = buffers.getBuffer(RenderType.lines());
        for (Projection projection : projections) {
            for (var entry : projection.states().entrySet()) {
                AABB box = new AABB(entry.getKey()).inflate(0.004);
                if (entry.getValue().conflict()) {
                    LevelRenderer.renderLineBox(pose, lines, box, 1.0F, 0.08F, 0.08F, 1.0F);
                } else {
                    LevelRenderer.renderLineBox(pose, lines, box, 0.12F, 0.48F, 0.82F, 0.55F);
                }
            }
        }
        pose.popPose();
        buffers.endBatch(RenderType.lines());
    }

    private static List<Projection> findProjections(ClientLevel level) {
        LinkedHashSet<ReactorFormation> formations = new LinkedHashSet<>();
        int cx = Minecraft.getInstance().player.blockPosition().getX() >> 4;
        int cz = Minecraft.getInstance().player.blockPosition().getZ() >> 4;
        for (int x = cx - 3; x <= cx + 3; x++) {
            for (int z = cz - 3; z <= cz + 3; z++) {
                var chunk = level.getChunkSource().getChunk(x, z, ChunkStatus.FULL, false);
                if (chunk == null) continue;
                for (var entity : chunk.getBlockEntities().values()) {
                    if (!(entity instanceof ReactorControllerBlockEntity controller)
                            || !controller.projectionEnabled()
                            || entity.getBlockPos().distToCenterSqr(Minecraft.getInstance().player.position()) > 64 * 64) {
                        continue;
                    }
                    ReactorFormation formation = controller.formation();
                    if (formation == null) {
                        formation = ReactorStructures.prospective(
                                ReactorStructures.view(level),
                                controller.getBlockState().getValue(ReactorComponentBlock.FACING),
                                controller.getBlockPos());
                    }
                    formations.add(formation);
                }
            }
        }

        List<Projection> result = new ArrayList<>();
        for (ReactorFormation formation : formations) {
            Map<BlockPos, BlockStateInfo> states = new LinkedHashMap<>();
            for (ReactorStructure.Cell cell : ReactorStructure.definition().cells()) {
                BlockPos pos = formation.position(cell.offset());
                boolean loaded = level.hasChunkAt(pos);
                var actualState = loaded ? level.getBlockState(pos) : null;
                ResourceLocation actual = loaded ? BuiltInRegistries.BLOCK.getKey(actualState.getBlock()) : null;
                boolean conflict = actual != null && !actualState.isAir()
                        && !cell.block().equals(actual);
                states.put(pos, new BlockStateInfo(
                        ReactorStructure.definition().state(cell, formation.quarterTurns()), conflict));
            }
            result.add(new Projection(states));
        }
        return List.copyOf(result);
    }

    private record Projection(Map<BlockPos, BlockStateInfo> states) {}
    private record BlockStateInfo(net.minecraft.world.level.block.state.BlockState state, boolean conflict) {}

    private static final class TintedVertexConsumer extends VertexConsumerWrapper {
        private final float red;
        private final float green;
        private final float blue;

        private TintedVertexConsumer(VertexConsumer parent, float red, float green, float blue) {
            super(parent);
            this.red = red;
            this.green = green;
            this.blue = blue;
        }

        @Override public VertexConsumer setColor(int red, int green, int blue, int alpha) {
            parent.setColor((int) (red * this.red), (int) (green * this.green),
                    (int) (blue * this.blue), (int) (alpha * ALPHA));
            return this;
        }
    }
}
