package com.appliedmoremachines.client;

import com.appliedmoremachines.AppliedMoreMachines;
import com.appliedmoremachines.InfusionPedestaMenu;
import com.appliedmoremachines.InfusionPedestaBlockEntity;
import com.appliedmoremachines.InfusionOutputSide;
import appeng.client.gui.Icon;
import appeng.core.localization.ButtonToolTips;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public final class InfusionPedestaScreen extends AbstractContainerScreen<InfusionPedestaMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            AppliedMoreMachines.MOD_ID, "textures/gui/ae_infusion_ui.png");
    private static final int TEXTURE_SIZE = 256;
    private static final int TOOLBAR_WIDTH = 24;
    private static final int TOOLBAR_HEIGHT = 44;
    private static final int PROGRESS_BAR_X = 132;
    private static final int PROGRESS_BAR_Y = 40;
    private static final int PROGRESS_BAR_WIDTH = 6;
    private static final int PROGRESS_BAR_HEIGHT = 18;
    public static final int RECIPE_AREA_X = 78;
    public static final int RECIPE_AREA_Y = 34;
    public static final int RECIPE_AREA_WIDTH = 21;
    public static final int RECIPE_AREA_HEIGHT = 32;
    private static final int PANEL_COLOR = 0xffcbccd4;
    private static final int BORDER_COLOR = 0xff413f54;
    private static final int LIGHT_COLOR = 0xfff2f2f2;
    private final List<AltarIconButton> iconButtons = new ArrayList<>();
    private AltarIconButton outputConfigurationButton;
    private boolean configurationOpen;

    public InfusionPedestaScreen(InfusionPedestaMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        imageWidth = InfusionPedestaMenu.IMAGE_WIDTH;
        imageHeight = InfusionPedestaMenu.IMAGE_HEIGHT;
    }

    @Override
    protected void init() {
        super.init();
        iconButtons.clear();
        outputConfigurationButton = null;
        menu.setConfigurationOpen(configurationOpen);
        if (configurationOpen) {
            addIcon(leftPos + 152, topPos + 6,
                    () -> Component.translatable("gui.applied_more_machines.return"),
                    button -> changePage(false), () -> false, null, GuiIconSprites.RETURN);
            addSide(InfusionOutputSide.TOP, 80, 36);
            addSide(InfusionOutputSide.LEFT, 44, 72);
            addSide(InfusionOutputSide.FRONT, 80, 72);
            addSide(InfusionOutputSide.RIGHT, 116, 72);
            addSide(InfusionOutputSide.BOTTOM, 80, 108);
            addSide(InfusionOutputSide.BACK, 116, 108);
        } else {
            addIcon(leftPos - 20, topPos + 4,
                    () -> ButtonToolTips.OpenGuide.text(), button -> AltarGuide.open(minecraft.player),
                    () -> false, () -> Icon.HELP, null);
            addIcon(leftPos - 20, topPos + 24, this::autoExportDescription,
                    button -> sendSetting(InfusionPedestaMenu.TOGGLE_AUTO_EXPORT),
                    menu::isAutoExportEnabled,
                    () -> menu.isAutoExportEnabled() ? Icon.AUTO_EXPORT_ON : Icon.AUTO_EXPORT_OFF, null);
            outputConfigurationButton = addIcon(leftPos - 20, topPos + 44,
                    () -> Component.translatable("gui.applied_more_machines.output_configuration"),
                    button -> changePage(true), () -> false, null, GuiIconSprites.DIRECTION);
        }
        updateOutputConfiguration();
    }

    private AltarIconButton addIcon(int x, int y, Supplier<Component> description,
            net.minecraft.client.gui.components.Button.OnPress onPress, BooleanSupplier selected,
            Supplier<Icon> aeIcon, GuiIconSprites projectIcon) {
        AltarIconButton button = new AltarIconButton(x, y, description, onPress, selected, aeIcon, projectIcon);
        iconButtons.add(button);
        return addRenderableWidget(button);
    }

    /** Queried by the optional JEI handler; the configuration page has no recipe progress control. */
    public boolean isRecipeAreaAvailable() {
        return !configurationOpen && menu.getCarried().isEmpty();
    }

    private void addSide(InfusionOutputSide side, int x, int y) {
        addIcon(leftPos + x, topPos + y, () -> Component.translatable(
                        "gui.applied_more_machines.output_side." + side.name().toLowerCase(Locale.ROOT)),
                button -> sendSetting(side.buttonId()), () -> menu.isOutputSideEnabled(side), null, null);
    }

    private Component autoExportDescription() {
        return Component.empty().append(ButtonToolTips.AutoExport.text()).append("\n")
                .append(menu.isAutoExportEnabled()
                        ? ButtonToolTips.AutoExportOn.text() : ButtonToolTips.AutoExportOff.text());
    }

    private void sendSetting(int buttonId) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, buttonId);
        }
    }

    private void changePage(boolean open) {
        if (open && !menu.isAutoExportEnabled()) {
            return;
        }
        configurationOpen = open;
        sendSetting(open ? InfusionPedestaMenu.CONFIGURATION_PAGE : InfusionPedestaMenu.MAIN_PAGE);
        setFocused(null);
        clearWidgets();
        init();
    }

    private void updateOutputConfiguration() {
        boolean autoExportEnabled = menu.isAutoExportEnabled();
        if (configurationOpen && !autoExportEnabled) {
            changePage(false);
        }
        if (outputConfigurationButton != null) {
            outputConfigurationButton.visible = autoExportEnabled;
            outputConfigurationButton.active = autoExportEnabled;
        }
    }

    private int toolbarHeight() {
        return menu.isAutoExportEnabled() ? TOOLBAR_HEIGHT + 20 : TOOLBAR_HEIGHT;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        // Atlas sprites at x >= 176 are sampled into the panel only.
        graphics.blit(TEXTURE, leftPos, topPos, 0.0F, 0.0F,
                imageWidth, imageHeight, TEXTURE_SIZE, TEXTURE_SIZE);
        if (configurationOpen) {
            // Reuse the supplied panel border, clearing only its interior on this page.
            graphics.fill(leftPos + 2, topPos + 2, leftPos + imageWidth - 2,
                    topPos + imageHeight - 3, PANEL_COLOR);
        } else {
            graphics.fill(leftPos + 14, topPos + 18, leftPos + 72, topPos + 78, PANEL_COLOR);
            for (int row = 0; row < 3; row++) {
                for (int column = 0; column < 3; column++) {
                    graphics.blit(TEXTURE, leftPos + 16 + column * 18, topPos + 21 + row * 18,
                            52.0F, 21.0F, 18, 18, TEXTURE_SIZE, TEXTURE_SIZE);
                }
            }
            drawProgress(graphics);
            int toolbarHeight = toolbarHeight();
            graphics.fill(leftPos - TOOLBAR_WIDTH, topPos, leftPos, topPos + toolbarHeight, BORDER_COLOR);
            graphics.fill(leftPos - TOOLBAR_WIDTH + 1, topPos + 1,
                    leftPos, topPos + toolbarHeight - 1, LIGHT_COLOR);
            graphics.fill(leftPos - TOOLBAR_WIDTH + 2, topPos + 2,
                    leftPos, topPos + toolbarHeight - 2, PANEL_COLOR);
            drawUpgradePanel(graphics);
        }
    }

    private void drawProgress(GuiGraphics graphics) {
        double progress = menu.getProcessingProgress();
        // Crop the cell's padding so even early progress lights part of the crystal.
        int crystalWidth = (int) (13 * progress);
        if (crystalWidth > 0) {
            graphics.blit(TEXTURE, leftPos + 82, topPos + 39, 204.0F, 5.0F,
                    crystalWidth, 23, TEXTURE_SIZE, TEXTURE_SIZE);
        }
        int filled = (int) (PROGRESS_BAR_HEIGHT * progress);
        if (filled > 0) {
            int offset = PROGRESS_BAR_HEIGHT - filled;
            // Source row 33 is transparent; the actual bordered strip is rows 34..51.
            graphics.blit(TEXTURE, leftPos + PROGRESS_BAR_X, topPos + PROGRESS_BAR_Y + offset,
                    176.0F, 34.0F + offset, PROGRESS_BAR_WIDTH, filled, TEXTURE_SIZE, TEXTURE_SIZE);
        }
    }

    private void drawUpgradePanel(GuiGraphics graphics) {
        int x = leftPos + imageWidth;
        int y = topPos;
        int width = InfusionPedestaMenu.UPGRADE_PANEL_WIDTH;
        int height = InfusionPedestaMenu.UPGRADE_PANEL_HEIGHT;
        graphics.fill(x, y, x + width, y + height, BORDER_COLOR);
        graphics.fill(x, y + 1, x + width - 1, y + height - 1, LIGHT_COLOR);
        graphics.fill(x, y + 2, x + width - 2, y + height - 2, PANEL_COLOR);
        for (int upgrade = 0; upgrade < InfusionPedestaBlockEntity.UPGRADE_COUNT; upgrade++) {
            int slotX = leftPos + InfusionPedestaMenu.UPGRADE_X;
            int slotY = topPos + InfusionPedestaMenu.UPGRADE_Y + upgrade * InfusionPedestaMenu.SLOT_SPACING;
            Icon.SLOT_BACKGROUND.getBlitter().dest(slotX - 1, slotY - 1).blit(graphics);
            if (!menu.getSlot(InfusionPedestaMenu.UPGRADE_MENU_START + upgrade).hasItem()) {
                Icon.BACKGROUND_UPGRADE.getBlitter().dest(slotX, slotY).blit(graphics);
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        if (configurationOpen) {
            graphics.drawString(font, Component.translatable("gui.applied_more_machines.output_configuration"),
                    8, 9, BORDER_COLOR, false);
            drawSideLabel(graphics, InfusionOutputSide.TOP, 88, 56);
            drawSideLabel(graphics, InfusionOutputSide.LEFT, 52, 92);
            drawSideLabel(graphics, InfusionOutputSide.FRONT, 88, 92);
            drawSideLabel(graphics, InfusionOutputSide.RIGHT, 124, 92);
            drawSideLabel(graphics, InfusionOutputSide.BOTTOM, 88, 128);
            drawSideLabel(graphics, InfusionOutputSide.BACK, 124, 128);
        }
    }

    private void drawSideLabel(GuiGraphics graphics, InfusionOutputSide side, int centerX, int y) {
        Component label = Component.translatable(
                "gui.applied_more_machines.output_side." + side.name().toLowerCase(Locale.ROOT));
        graphics.drawString(font, label, centerX - font.width(label) / 2, y, BORDER_COLOR, false);
    }

    @Override
    protected boolean hasClickedOutside(double mouseX, double mouseY, int left, int top, int button) {
        if (!configurationOpen && mouseX >= left + imageWidth
                && mouseX < left + imageWidth + InfusionPedestaMenu.UPGRADE_PANEL_WIDTH
                && mouseY >= top && mouseY < top + InfusionPedestaMenu.UPGRADE_PANEL_HEIGHT) {
            return false;
        }
        if (configurationOpen || mouseX >= left - TOOLBAR_WIDTH && mouseX < left
                && mouseY >= top && mouseY < top + toolbarHeight()) {
            return false;
        }
        return super.hasClickedOutside(mouseX, mouseY, left, top, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        boolean wasButtonPress = false;
        for (AltarIconButton iconButton : iconButtons) {
            wasButtonPress |= iconButton.releasePress();
        }
        if (wasButtonPress) {
            setDragging(false);
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void onClose() {
        if (configurationOpen) {
            changePage(false);
        } else {
            super.onClose();
        }
    }

    @Override
    public void removed() {
        menu.setConfigurationOpen(false);
        super.removed();
    }

    @Override
    protected void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        if (!configurationOpen && hoveredSlot != null
                && hoveredSlot.index >= InfusionPedestaMenu.UPGRADE_MENU_START
                && hoveredSlot.index < InfusionPedestaMenu.UPGRADE_MENU_START + InfusionPedestaBlockEntity.UPGRADE_COUNT) {
            List<Component> lines = new ArrayList<>();
            if (hoveredSlot.hasItem() && menu.getCarried().isEmpty()) {
                lines.addAll(getTooltipFromContainerItem(hoveredSlot.getItem()));
                lines.add(Component.empty());
            }
            lines.add(Component.translatable("gui.applied_more_machines.available_upgrades"));
            lines.add(Component.translatable("gui.applied_more_machines.upgrade_limit",
                    Component.translatable("item.ae2.speed_card"), InfusionPedestaBlockEntity.UPGRADE_COUNT)
                    .withStyle(ChatFormatting.GRAY));
            lines.add(Component.translatable("gui.applied_more_machines.upgrade_limit",
                    Component.translatable("gui.applied_more_machines.overload_card"), InfusionPedestaBlockEntity.UPGRADE_COUNT)
                    .withStyle(ChatFormatting.GRAY));
            graphics.renderComponentTooltip(font, lines, mouseX, mouseY);
            return;
        }
        super.renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        updateOutputConfiguration();
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        if (!configurationOpen && isHovering(PROGRESS_BAR_X, PROGRESS_BAR_Y,
                PROGRESS_BAR_WIDTH, PROGRESS_BAR_HEIGHT, mouseX, mouseY)) {
            graphics.renderTooltip(font, Component.translatable(
                    "gui.applied_more_machines.progress", (int) (100 * menu.getProcessingProgress())), mouseX, mouseY);
        }
    }
}
