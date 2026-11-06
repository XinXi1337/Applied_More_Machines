package com.appliedmoremachines.integration.jei;

import com.mojang.blaze3d.platform.InputConstants;
import mezz.jei.api.gui.inputs.IJeiInputHandler;
import net.minecraft.client.gui.navigation.ScreenRectangle;

final class FactoryPreviewInputHandler implements IJeiInputHandler {
    private static final float SENSITIVITY = 0.7F;
    private final FactoryPreviewRotation rotation;
    private final ScreenRectangle area;

    FactoryPreviewInputHandler(FactoryPreviewRotation rotation, int x, int y, int width, int height) {
        this.rotation = rotation;
        this.area = new ScreenRectangle(x, y, width, height);
    }

    @Override public ScreenRectangle getArea() { return area; }

    @Override
    public boolean handleInput(double mouseX, double mouseY, mezz.jei.api.gui.inputs.IJeiUserInput input) {
        InputConstants.Key key = input.getKey();
        if (!contains(mouseX, mouseY) || key.getType() != InputConstants.Type.MOUSE
                || key.getValue() < 0 || key.getValue() > 1) return false;
        return true;
    }

    @Override
    public boolean handleMouseDragged(double mouseX, double mouseY, InputConstants.Key key,
            double deltaX, double deltaY) {
        if (!contains(mouseX, mouseY) || key.getType() != InputConstants.Type.MOUSE
                || key.getValue() < 0 || key.getValue() > 1) return false;
        rotation.yaw += (float) deltaX * SENSITIVITY;
        rotation.pitch = Math.max(-70.0F, Math.min(70.0F,
                rotation.pitch + (float) deltaY * SENSITIVITY));
        return true;
    }

    private boolean contains(double x, double y) {
        return x >= 0 && x < area.width() && y >= 0 && y < area.height();
    }
}
