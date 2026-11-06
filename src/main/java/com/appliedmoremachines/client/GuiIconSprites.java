package com.appliedmoremachines.client;

import com.appliedmoremachines.AppliedMoreMachines;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/** The user's six 16x16 frames, in their original order. */
public enum GuiIconSprites {
    BUTTON_OFF(0), BUTTON_ON(1), BUTTON_PRESSED(2), DIRECTION(3), GUIDANCE(4), RETURN(5);

    public static final int SIZE = 16;
    public static final int WIDTH = 96;
    public static final int HEIGHT = 16;
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            AppliedMoreMachines.MOD_ID, "textures/gui/icon_sprites.png");
    private final int u;

    GuiIconSprites(int frame) {
        u = frame * SIZE;
    }

    public void draw(GuiGraphics graphics, int x, int y) {
        graphics.blit(TEXTURE, x, y, (float) u, 0.0F, SIZE, SIZE, WIDTH, HEIGHT);
    }

    public void drawCenter(GuiGraphics graphics, int x, int y) {
        graphics.blit(TEXTURE, x + 2, y + 2, (float) u + 2, 2.0F,
                SIZE - 4, SIZE - 4, WIDTH, HEIGHT);
    }
}
