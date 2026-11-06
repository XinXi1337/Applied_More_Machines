package com.appliedmoremachines.client;

import appeng.client.gui.Icon;
import com.appliedmoremachines.FactoryPatternMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/** Minimal single-page pattern screen with no priority, toolbar, or upgrade panel. */
public final class FactoryPatternScreen extends AbstractContainerScreen<FactoryPatternMenu> {
    private static final int BORDER = 0xff48465a;
    private static final int EDGE = 0xfff3f3f5;
    private static final int PANEL = 0xffc9cad2;

    public FactoryPatternScreen(FactoryPatternMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = FactoryPatternMenu.IMAGE_WIDTH;
        imageHeight = FactoryPatternMenu.IMAGE_HEIGHT;
        titleLabelX = 8;
        titleLabelY = 6;
        inventoryLabelX = 8;
        inventoryLabelY = 92;
    }

    @Override protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        graphics.fill(x, y, x + imageWidth, y + imageHeight, BORDER);
        graphics.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, EDGE);
        graphics.fill(x + 2, y + 2, x + imageWidth - 2, y + imageHeight - 2, PANEL);
        for (Slot slot : menu.slots) {
            Icon.SLOT_BACKGROUND.getBlitter().dest(x + slot.x - 1, y + slot.y - 1).blit(graphics);
        }
        for (int slotIndex = 0; slotIndex < FactoryPatternMenu.PATTERN_SLOT_COUNT; slotIndex++) {
            Slot slot = menu.getSlot(slotIndex);
            if (!slot.hasItem()) {
                Icon.BACKGROUND_ENCODED_PATTERN.getBlitter().dest(x + slot.x, y + slot.y).blit(graphics);
            }
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
