package com.appliedmoremachines.integration.jei;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import mezz.jei.api.gui.widgets.IRecipeWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenPosition;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.level.block.state.BlockState;

final class ReactorPreviewWidget implements IRecipeWidget {
    private final ReactorJeiRecipe recipe;
    private final ReactorPreviewRotation rotation;
    private final ScreenPosition position;
    private final int width;
    private final int height;

    ReactorPreviewWidget(ReactorJeiRecipe recipe, ReactorPreviewRotation rotation,
            int x, int y, int width, int height) {
        this.recipe = recipe;
        this.rotation = rotation;
        this.position = new ScreenPosition(x, y);
        this.width = width;
        this.height = height;
    }

    @Override public ScreenPosition getPosition() { return position; }

    @Override
    public void drawWidget(GuiGraphics graphics, double mouseX, double mouseY) {
        graphics.flush();
        RenderSystem.enableDepthTest();
        var pose = graphics.pose();
        pose.pushPose();
        try {
            pose.translate(width / 2.0, height / 2.0, 120.0);
            pose.scale(3.8F, -3.8F, 3.8F);
            pose.mulPose(Axis.XP.rotationDegrees(rotation.pitch));
            pose.mulPose(Axis.YP.rotationDegrees(rotation.yaw));
            pose.translate(-6.0, -5.5, -6.0);

            var minecraft = Minecraft.getInstance();
            BlockRenderDispatcher renderer = minecraft.getBlockRenderer();
            MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
            for (ReactorJeiRecipe.Cell cell : recipe.cells()) {
                BlockState state = cell.state();
                pose.pushPose();
                var offset = cell.offset();
                pose.translate(offset.getX(), offset.getY(), offset.getZ());
                renderer.renderSingleBlock(state, pose, buffers, LightTexture.FULL_BRIGHT,
                        OverlayTexture.NO_OVERLAY);
                pose.popPose();
            }
            buffers.endBatch();
        } finally {
            pose.popPose();
            RenderSystem.disableDepthTest();
        }
    }
}
