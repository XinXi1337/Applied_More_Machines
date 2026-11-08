package com.appliedmoremachines.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;

/** The supplied textures contain the ghost; only the water and filled item are drawn here. */
public final class BrewingVisuals {
    public static void progress(GuiGraphics graphics, int x, int y, double progress) {
        int filled = (int) (24 * Math.clamp(progress, 0, 1));
        if (filled <= 0) return;
        graphics.enableScissor(x, y + 24 - filled, x + 24, y + 24);
        graphics.pose().pushPose(); graphics.pose().translate(x, y, 0); graphics.pose().scale(1.5F, 1.5F, 1);
        graphics.renderItem(new ItemStack(Items.BREWING_STAND), 0, 0);
        graphics.pose().popPose(); graphics.disableScissor();
    }
    public static void water(GuiGraphics graphics, int x, int y) {
        FluidStack water = new FluidStack(Fluids.WATER, 8000);
        var extension = IClientFluidTypeExtensions.of(Fluids.WATER);
        var texture = extension.getStillTexture(water); if (texture == null) return;
        var sprite = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(texture);
        int tint = extension.getTintColor(water);
        RenderSystem.enableBlend();
        graphics.enableScissor(x, y, x + 16, y + 72);
        for (int row = y + 56; row > y - 16; row -= 16)
            graphics.blit(x, row, 0, 16, 16, sprite, (tint >> 16 & 255) / 255f,
                    (tint >> 8 & 255) / 255f, (tint & 255) / 255f, (tint >>> 24) / 255f);
        graphics.disableScissor();
        for (int i = 0; i < 8; i++) graphics.fill(x, y + 5 + i * 9, x + (i % 2 == 0 ? 4 : 3), y + 6 + i * 9, 0xfff2f2f2);
    }
    private BrewingVisuals() {}
}
