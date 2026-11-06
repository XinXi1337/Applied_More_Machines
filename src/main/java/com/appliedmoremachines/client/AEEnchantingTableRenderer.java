package com.appliedmoremachines.client;

import com.appliedmoremachines.AEEnchantingTableBlockEntity;
import com.appliedmoremachines.AppliedMoreMachines;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.BookModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;

/** Vanilla book geometry and motion with the AE table's own cover texture. */
public final class AEEnchantingTableRenderer implements BlockEntityRenderer<AEEnchantingTableBlockEntity> {
    public static final ResourceLocation BOOK_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            AppliedMoreMachines.MOD_ID, "textures/entity/ae_enchanting_table_book.png");
    private final BookModel bookModel;

    public AEEnchantingTableRenderer(BlockEntityRendererProvider.Context context) {
        bookModel = new BookModel(context.bakeLayer(ModelLayers.BOOK));
    }

    @Override
    public void render(AEEnchantingTableBlockEntity table, float partialTick, PoseStack poses,
            MultiBufferSource buffers, int packedLight, int packedOverlay) {
        poses.pushPose();
        poses.translate(0.5F, 0.75F, 0.5F);
        float time = table.time + partialTick;
        poses.translate(0.0F, 0.1F + Mth.sin(time * 0.1F) * 0.01F, 0.0F);
        float rotationDelta = table.rot - table.oRot;
        while (rotationDelta >= (float) Math.PI) rotationDelta -= (float) (Math.PI * 2);
        while (rotationDelta < (float) -Math.PI) rotationDelta += (float) (Math.PI * 2);
        float rotation = table.oRot + rotationDelta * partialTick;
        poses.mulPose(Axis.YP.rotation(-rotation));
        poses.mulPose(Axis.ZP.rotationDegrees(80.0F));
        float flip = Mth.lerp(partialTick, table.oFlip, table.flip);
        float leftPage = Mth.frac(flip + 0.25F) * 1.6F - 0.3F;
        float rightPage = Mth.frac(flip + 0.75F) * 1.6F - 0.3F;
        float open = Mth.lerp(partialTick, table.oOpen, table.open);
        bookModel.setupAnim(time, Mth.clamp(leftPage, 0.0F, 1.0F),
                Mth.clamp(rightPage, 0.0F, 1.0F), open);
        VertexConsumer vertices = buffers.getBuffer(RenderType.entitySolid(BOOK_TEXTURE));
        bookModel.render(poses, vertices, packedLight, packedOverlay, -1);
        poses.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(AEEnchantingTableBlockEntity table) {
        BlockPos pos = table.getBlockPos();
        return new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 1.5, pos.getZ() + 1);
    }
}
