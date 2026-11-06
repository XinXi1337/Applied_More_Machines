package com.appliedmoremachines.client;

import com.mojang.blaze3d.platform.NativeImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/** Reads the user's existing ghost pixels. No generated texture or artwork changes. */
public final class FactoryProgressMask {
    private record Run(int x, int y, int end) {}
    private final List<Run> runs = new ArrayList<>();
    private int minX = Integer.MAX_VALUE, maxX;

    FactoryProgressMask(ResourceLocation texture) {
        this(texture, 16, 26, 63, 31);
    }

    public FactoryProgressMask(ResourceLocation texture, int x, int y, int width, int height) {
        try (var input = Minecraft.getInstance().getResourceManager().getResourceOrThrow(texture).open();
                var image = NativeImage.read(input)) {
            int background = image.getPixelRGBA(x, y);
            int red = background & 255, green = background >> 8 & 255, blue = background >> 16 & 255;
            for (int row = y; row < y + height; row++) {
                int start = -1;
                for (int column = x; column <= x + width; column++) {
                    int pixel = column < x + width ? image.getPixelRGBA(column, row) : background;
                    boolean ghost = (pixel >>> 24) > 0 && (pixel & 255) < red - 8
                            && (pixel >> 8 & 255) < green - 8 && (pixel >> 16 & 255) < blue - 8;
                    if (ghost && start < 0) start = column;
                    if (!ghost && start >= 0) {
                        runs.add(new Run(start, row, column));
                        minX = Math.min(minX, start); maxX = Math.max(maxX, column);
                        start = -1;
                    }
                }
            }
        } catch (IOException ex) {
            com.appliedmoremachines.AppliedMoreMachines.LOGGER.warn("Cannot read factory progress ghost", ex);
        }
    }

    public void draw(GuiGraphics graphics, int left, int top, float progress) {
        if (runs.isEmpty()) return;
        int filled = minX + (int) Math.floor((maxX - minX) * Math.clamp(progress, 0, 1));
        for (Run run : runs) {
            int end = Math.min(run.end(), filled);
            if (end > run.x()) graphics.fill(left + run.x(), top + run.y(), left + end, top + run.y() + 1, 0xffffffff);
        }
    }
}
