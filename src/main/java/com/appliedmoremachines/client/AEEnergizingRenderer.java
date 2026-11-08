package com.appliedmoremachines.client;

import com.appliedmoremachines.AEEnergizingBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;

public final class AEEnergizingRenderer implements BlockEntityRenderer<AEEnergizingBlockEntity> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            "applied_more_machines", "textures/block/ae_energizing_orb/energy_charge.png");
    private final ModelPart cube;
    private final ItemRenderer itemRenderer;

    public AEEnergizingRenderer(BlockEntityRendererProvider.Context context) {
        itemRenderer = context.getItemRenderer();
        var mesh = new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("cube", CubeListBuilder.create().mirror()
                .addBox(-2.5f, -2.5f, -2.5f, 5, 5, 5), PartPose.ZERO);
        cube = LayerDefinition.create(mesh, 20, 10).bakeRoot().getChild("cube");
    }

    /** Matches Powah's V3d.circled(count, 0.1), with a single input centred. */
    public static Vec3 materialPosition(int index, int count) {
        if (count <= 1) return new Vec3(.5, .6, .5);
        double angle = 2 * Math.PI * index / count;
        return new Vec3(.5 + .1 * Math.cos(angle), .6, .5 + .1 * Math.sin(angle));
    }

    @Override public void render(AEEnergizingBlockEntity be, float tick, PoseStack pose,
            MultiBufferSource buffers, int light, int overlay) {
        var materials = be.getDisplayedMaterials();
        float rotation = be.getLevel() == null ? 0 : -((be.getLevel().getGameTime() % 200 + tick) / 200f) * 360f;
        for (int i = 0; i < materials.size(); i++) {
            Vec3 position = materialPosition(i, materials.size());
            pose.pushPose();
            pose.translate(position.x, position.y, position.z);
            pose.scale(.35f, .35f, .35f);
            pose.mulPose(Axis.YP.rotationDegrees(rotation));
            itemRenderer.renderStatic(materials.get(i), ItemDisplayContext.FIXED,
                    light, overlay, pose, buffers, be.getLevel(), 0);
            pose.popPose();
        }
        // Powah also draws its transparent energy cube after the contained items.
        pose.pushPose();
        pose.translate(.5, .6, .5);
        pose.scale(1.8f, 1.8f, 1.8f);
        cube.render(pose, buffers.getBuffer(owmii.powah.lib.client.util.RenderTypes.entityBlendedNoDept(TEXTURE)), light, overlay);
        pose.popPose();
    }
}
