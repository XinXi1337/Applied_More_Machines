package com.appliedmoremachines.client;

import appeng.client.gui.Icon;
import appeng.core.localization.ButtonToolTips;
import appeng.items.tools.GuideItem;
import com.appliedmoremachines.*;
import com.mojang.blaze3d.systems.RenderSystem;
import guideme.GuidesCommon;
import guideme.PageAnchor;
import java.util.*;
import java.util.function.*;
import net.minecraft.ChatFormatting;
import net.neoforged.fml.ModList;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public final class AEEnergizingScreen extends AbstractContainerScreen<AEEnergizingMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(AppliedMoreMachines.MOD_ID,
            "textures/gui/ae_energizing_orb_gui.png");
    private static final ResourceLocation GUIDE = ResourceLocation.fromNamespaceAndPath(AppliedMoreMachines.MOD_ID,
            "ae_energizing_orb.md");
    private static final int PANEL = 0xffcbccd4, BORDER = 0xff413f54, LIGHT = 0xfff2f2f2;
    private static final int MAIN_WIDTH = 176, MAIN_HEIGHT = 184;
    private static final int CONFIG_WIDTH = 156, CONFIG_HEIGHT = 136;
    private final List<AltarIconButton> icons = new ArrayList<>();
    private final List<Button> controls = new ArrayList<>();
    private AltarIconButton configuration;
    private boolean configurationOpen, controlPress;
    public AEEnergizingScreen(AEEnergizingMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title); imageWidth = MAIN_WIDTH; imageHeight = MAIN_HEIGHT;
    }
    public boolean isRecipeAreaAvailable() {
        return !configurationOpen && menu.getCarried().isEmpty();
    }
    private Component text(String suffix) { return Component.translatable("gui.applied_more_machines.latex." + suffix); }
    @Override protected void init() {
        imageWidth = configurationOpen ? CONFIG_WIDTH : MAIN_WIDTH;
        imageHeight = configurationOpen ? CONFIG_HEIGHT : MAIN_HEIGHT;
        super.init(); icons.clear(); controls.clear(); configuration = null;
        menu.setConfigurationOpen(configurationOpen);
        if (configurationOpen) {
            icon(CONFIG_WIDTH - 24, 6, () -> Component.translatable("gui.applied_more_machines.return"), b -> changePage(false), () -> false, null, GuiIconSprites.RETURN);
            addSide(InfusionOutputSide.TOP, 70, 30);
            addSide(InfusionOutputSide.LEFT, 34, 62);
            addSide(InfusionOutputSide.FRONT, 70, 62);
            addSide(InfusionOutputSide.RIGHT, 106, 62);
            addSide(InfusionOutputSide.BOTTOM, 70, 94);
            addSide(InfusionOutputSide.BACK, 106, 94);
        } else {
            icon(-20, 4, () -> ButtonToolTips.OpenGuide.text(), b -> {
                if (minecraft.player != null) GuidesCommon.openGuide(minecraft.player, GuideItem.GUIDE_ID, PageAnchor.page(GUIDE));
            }, () -> false, () -> Icon.HELP, null);
            icon(-20, 24, () -> ButtonToolTips.AutoExport.text(), b -> send(0), menu::autoExport,
                    () -> menu.autoExport() ? Icon.AUTO_EXPORT_ON : Icon.AUTO_EXPORT_OFF, null);
            configuration = icon(-20, 44, () -> Component.translatable("gui.applied_more_machines.output_configuration"), b -> changePage(true), () -> false, null, GuiIconSprites.DIRECTION);

        }
        updateConfiguration();
    }
    private void addSide(InfusionOutputSide side, int x, int y) {
        icon(x, y, () -> sideText(side), b -> send(side.buttonId()),
                () -> menu.isOutputSideEnabled(side), null, null);
    }
    private Component sideText(InfusionOutputSide side) {
        return Component.translatable("gui.applied_more_machines.output_side." + side.name().toLowerCase(Locale.ROOT));
    }
    private AltarIconButton icon(int x, int y, Supplier<Component> label, Button.OnPress action,
            BooleanSupplier selected, Supplier<Icon> ae, GuiIconSprites sprite) {
        var button = new AltarIconButton(leftPos + x, topPos + y, label, action, selected, ae, sprite);
        icons.add(button); controls.add(button); return addRenderableWidget(button);
    }
    private void send(int id) {
        if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
    }
    private void changePage(boolean open) {
        if (open && !menu.autoExport()) return;
        configurationOpen = open; send(open ? AEEnergizingMenu.CONFIG_PAGE : AEEnergizingMenu.MAIN_PAGE);
        setFocused(null); clearWidgets(); init();
    }
    private void updateConfiguration() {
        if (configurationOpen && !menu.autoExport()) changePage(false);
        if (configuration != null) { configuration.visible = menu.autoExport(); configuration.active = menu.autoExport(); }
    }
    @Override protected void renderBg(GuiGraphics g, float tick, int mx, int my) {
        if (configurationOpen) {
            panel(g, leftPos, topPos, imageWidth, imageHeight);
            return;
        }
        g.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
        EnergizingVisuals.progress(g, leftPos + 108, topPos + 40, menu.progress());
        panel(g, leftPos - 24, topPos, 24, menu.autoExport() ? 64 : 44);
        panel(g, leftPos + 176, topPos, 24, MultiplicationScreen.panelHeight(menu, 26));
        for (int i = 0; i < 1; i++) {
            int x = leftPos + 180, y = topPos + 5 + i * 18;
            Icon.SLOT_BACKGROUND.getBlitter().dest(x - 1, y - 1).blit(g);
            if (!menu.getSlot(AEEnergizingMenu.UPGRADE_START + i).hasItem())
                Icon.BACKGROUND_UPGRADE.getBlitter().dest(x, y).blit(g);
        }
    }
    private void panel(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, BORDER); g.fill(x + 1, y + 1, x + w - 1, y + h - 1, LIGHT);
        g.fill(x + 2, y + 2, x + w - 2, y + h - 2, PANEL);
    }
    @Override protected void renderLabels(GuiGraphics g, int mx, int my) {
        if (!configurationOpen) return;
        g.drawString(font, Component.translatable("gui.applied_more_machines.output_configuration"), 8, 9, BORDER, false);
        drawSideLabel(g, InfusionOutputSide.TOP, 78, 50);
        drawSideLabel(g, InfusionOutputSide.LEFT, 42, 82);
        drawSideLabel(g, InfusionOutputSide.FRONT, 78, 82);
        drawSideLabel(g, InfusionOutputSide.RIGHT, 114, 82);
        drawSideLabel(g, InfusionOutputSide.BOTTOM, 78, 114);
        drawSideLabel(g, InfusionOutputSide.BACK, 114, 114);
    }
    private void drawSideLabel(GuiGraphics g, InfusionOutputSide side, int centerX, int y) {
        Component label = sideText(side);
        g.drawString(font, label, centerX - font.width(label) / 2, y, BORDER, false);
    }
    @Override public boolean mouseClicked(double x, double y, int button) {
        for (Button control : controls) if (control.mouseClicked(x, y, button)) { controlPress = true; return true; }
        return super.mouseClicked(x, y, button);
    }
    @Override public boolean mouseReleased(double x, double y, int button) {
        boolean consumed = controlPress; controlPress = false;
        for (var icon : icons) consumed |= icon.releasePress();
        if (consumed) { setDragging(false); return true; }
        return super.mouseReleased(x, y, button);
    }
    @Override protected boolean hasClickedOutside(double x, double y, int left, int top, int button) {
        if (configurationOpen) return false;
        if (x >= left + 176 && x < left + 200 && y >= top && y < top + MultiplicationScreen.panelHeight(menu, 26)) return false;
        if (x >= left - 24 && x < left && y >= top && y < top + (menu.autoExport() ? 64 : 44)) return false;
        return super.hasClickedOutside(x, y, left, top, button);
    }
    @Override public void onClose() { if (configurationOpen) changePage(false); else super.onClose(); }
    @Override public void removed() { menu.setConfigurationOpen(false); super.removed(); }
    @Override public void render(GuiGraphics g, int mx, int my, float tick) {
        updateConfiguration(); super.render(g, mx, my, tick); renderTooltip(g, mx, my);
        if (configurationOpen) return;
    }
    @Override protected void renderTooltip(GuiGraphics g, int mx, int my) {
        if (MultiplicationScreen.tooltip(g, font, hoveredSlot, menu.getCarried(), mx, my, this::getTooltipFromContainerItem)) return;
        if (!configurationOpen && hoveredSlot != null
                && hoveredSlot.index >= AEEnergizingMenu.UPGRADE_START
                && hoveredSlot.index < AEEnergizingMenu.UPGRADE_START + 1
                && menu.getCarried().isEmpty()) {
            List<Component> lines = new ArrayList<>();
            if (hoveredSlot.hasItem()) {
                lines.addAll(getTooltipFromContainerItem(hoveredSlot.getItem()));
                lines.add(Component.empty());
            }
            lines.add(Component.translatable("gui.applied_more_machines.available_upgrades"));
            addUpgradeLine(lines, Component.translatable("item.ae2.speed_card"), 4);
            if (ModList.get().isLoaded("ae2cs")) {
                addUpgradeLine(lines, Component.translatable("gui.applied_more_machines.overload_card"), 4);
            }
            g.renderComponentTooltip(font, lines, mx, my);
            return;
        }
        super.renderTooltip(g, mx, my);
    }
    private void addUpgradeLine(List<Component> lines, Component name, int limit) {
        lines.add(Component.translatable("gui.applied_more_machines.upgrade_limit", name, limit)
                .withStyle(ChatFormatting.GRAY));
    }
}
