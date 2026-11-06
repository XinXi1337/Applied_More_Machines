package com.appliedmoremachines.client;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import appeng.client.gui.Icon;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

/** User-drawn button states with either an AE2 symbol or a project atlas symbol. */
final class AltarIconButton extends Button {
    private final Supplier<Icon> aeIcon;
    private final GuiIconSprites projectIcon;
    private final BooleanSupplier selected;
    private final Supplier<Component> description;
    private Component lastDescription;
    private boolean pressed;

    AltarIconButton(int x, int y, Supplier<Component> description, OnPress onPress,
            BooleanSupplier selected, Supplier<Icon> aeIcon, GuiIconSprites projectIcon) {
        super(x, y, GuiIconSprites.SIZE, GuiIconSprites.SIZE,
                description.get(), onPress, Button.DEFAULT_NARRATION);
        this.description = description;
        this.selected = selected;
        this.aeIcon = aeIcon;
        this.projectIcon = projectIcon;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        Component currentDescription = description.get();
        if (!currentDescription.equals(lastDescription)) {
            lastDescription = currentDescription;
            setMessage(currentDescription);
            setTooltip(Tooltip.create(currentDescription));
        }

        GuiIconSprites background = pressed && active ? GuiIconSprites.BUTTON_PRESSED
                : selected.getAsBoolean() ? GuiIconSprites.BUTTON_ON : GuiIconSprites.BUTTON_OFF;
        if (projectIcon != null && !pressed && !selected.getAsBoolean()) {
            projectIcon.draw(graphics, getX(), getY());
        } else {
            background.draw(graphics, getX(), getY());
            if (projectIcon != null) {
                projectIcon.drawCenter(graphics, getX(), getY());
            }
        }
        if (aeIcon != null) {
            aeIcon.get().getBlitter().dest(getX(), getY()).blit(graphics);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean handled = super.mouseClicked(mouseX, mouseY, button);
        pressed = handled;
        return handled;
    }

    boolean releasePress() {
        boolean wasPressed = pressed;
        pressed = false;
        return wasPressed;
    }
}
