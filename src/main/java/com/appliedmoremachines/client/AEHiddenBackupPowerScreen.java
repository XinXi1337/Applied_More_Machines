package com.appliedmoremachines.client;

import com.appliedmoremachines.AEHiddenBackupPowerMenu;
import com.appliedmoremachines.AppliedMoreMachines;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public final class AEHiddenBackupPowerScreen extends AbstractContainerScreen<AEHiddenBackupPowerMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(AppliedMoreMachines.MOD_ID,
            "textures/gui/ae_hidden_backup_power_gui.png");
    public AEHiddenBackupPowerScreen(AEHiddenBackupPowerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title); imageWidth = 176; imageHeight = 184;
    }
    @Override protected void renderBg(GuiGraphics g, float tick, int x, int y) {
        g.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
        // The supplied empty-slot glyph is part of the PNG. Cover it under an installed disk.
        if (menu.getSlot(0).hasItem()) g.fill(leftPos + 80, topPos + 42, leftPos + 96, topPos + 58, 0xffadb0c4);
    }
    @Override protected void renderLabels(GuiGraphics g, int x, int y) {}
    @Override public void render(GuiGraphics g, int x, int y, float tick) {
        super.render(g, x, y, tick); renderTooltip(g, x, y);
    }
}
