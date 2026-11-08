package com.appliedmoremachines.client;

import appeng.client.gui.Icon;
import appeng.core.localization.ButtonToolTips;
import com.appliedmoremachines.AppliedMoreMachines;
import com.appliedmoremachines.CraftingCoreBlockEntity;
import com.appliedmoremachines.CraftingCoreMenu;
import com.appliedmoremachines.InfusionOutputSide;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.fml.ModList;

public final class CraftingCoreScreen extends AbstractContainerScreen<CraftingCoreMenu> {
    public static final int RECIPE_AREA_X = 76;
    public static final int RECIPE_AREA_Y = 49;
    public static final int RECIPE_AREA_WIDTH = 24;
    public static final int RECIPE_AREA_HEIGHT = 24;
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            AppliedMoreMachines.MOD_ID, "textures/gui/ae_crafting_core_gui.png");
    private static final int TEXTURE_SIZE = 256;
    private static final int PANEL_SOURCE_WIDTH = 149;
    private static final int PANEL_SOURCE_HEIGHT = 178;
    private static final int TOOLBAR_WIDTH = 24;
    private static final int PANEL_COLOR = 0xffe9eaee;
    private static final int BORDER_COLOR = 0xff858591;
    private static final int LIGHT_COLOR = 0xffffffff;
    private final List<AltarIconButton> iconButtons = new ArrayList<>();
    private AltarIconButton outputConfigurationButton;
    private boolean configurationOpen;

    public CraftingCoreScreen(CraftingCoreMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = CraftingCoreMenu.IMAGE_WIDTH;
        imageHeight = CraftingCoreMenu.IMAGE_HEIGHT;
    }

    @Override
    protected void init() {
        super.init();
        iconButtons.clear();
        outputConfigurationButton = null;
        menu.setConfigurationOpen(configurationOpen);
        configurationOpen = menu.isConfigurationOpen();
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
                    () -> ButtonToolTips.OpenGuide.text(), button -> CoreGuide.open(minecraft.player),
                    () -> false, () -> Icon.HELP, null);
            addIcon(leftPos - 20, topPos + 24, this::autoExportDescription,
                    button -> sendSetting(CraftingCoreMenu.TOGGLE_AUTO_EXPORT), menu::isAutoExportEnabled,
                    () -> menu.isAutoExportEnabled() ? Icon.AUTO_EXPORT_ON : Icon.AUTO_EXPORT_OFF, null);
            outputConfigurationButton = addIcon(leftPos - 20, topPos + 44,
                    () -> Component.translatable("gui.applied_more_machines.output_configuration"),
                    button -> changePage(true), () -> false, null, GuiIconSprites.DIRECTION);
        }
        updateOutputConfiguration();
    }

    private AltarIconButton addIcon(int x, int y, Supplier<Component> description, Button.OnPress onPress,
            BooleanSupplier selected, Supplier<Icon> aeIcon, GuiIconSprites projectIcon) {
        AltarIconButton button = new AltarIconButton(x, y, description, onPress, selected, aeIcon, projectIcon);
        iconButtons.add(button);
        return addRenderableWidget(button);
    }

    /** The optional JEI integration owns recipe navigation and its combined hover tooltip. */
    public boolean isRecipeAreaAvailable() {
        return !configurationOpen && !menu.isConfigurationOpen() && menu.getCarried().isEmpty();
    }

    private void addSide(InfusionOutputSide side, int x, int y) {
        addIcon(leftPos + x, topPos + y, () -> sideDescription(side),
                button -> sendSetting(side.buttonId()), () -> menu.isOutputSideEnabled(side), null, null);
    }

    private Component sideDescription(InfusionOutputSide side) {
        return Component.translatable("gui.applied_more_machines.output_side."
                + side.name().toLowerCase(Locale.ROOT));
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
        if (open && !menu.isAutoExportEnabled()) return;
        configurationOpen = open;
        sendSetting(open ? CraftingCoreMenu.CONFIGURATION_PAGE : CraftingCoreMenu.MAIN_PAGE);
        setFocused(null);
        clearWidgets();
        init();
    }

    private void updateOutputConfiguration() {
        if (configurationOpen && !menu.isAutoExportEnabled()) changePage(false);
        if (outputConfigurationButton != null) {
            outputConfigurationButton.visible = menu.isAutoExportEnabled();
            outputConfigurationButton.active = menu.isAutoExportEnabled();
        }
    }

    private int toolbarHeight() {
        return menu.isAutoExportEnabled() ? 64 : 44;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        // The supplied atlas contains a reduced panel; restore native item-slot dimensions at draw time.
        graphics.blit(TEXTURE, leftPos, topPos, imageWidth, imageHeight, 0.0F, 0.0F,
                PANEL_SOURCE_WIDTH, PANEL_SOURCE_HEIGHT, TEXTURE_SIZE, TEXTURE_SIZE);
        if (configurationOpen) {
            graphics.fill(leftPos + 5, topPos + 5, leftPos + imageWidth - 5,
                    topPos + imageHeight - 5, PANEL_COLOR);
        } else {
            int height = toolbarHeight();
            graphics.fill(leftPos - TOOLBAR_WIDTH, topPos, leftPos, topPos + height, BORDER_COLOR);
            graphics.fill(leftPos - TOOLBAR_WIDTH + 1, topPos + 1, leftPos, topPos + height - 1, LIGHT_COLOR);
            graphics.fill(leftPos - TOOLBAR_WIDTH + 2, topPos + 2, leftPos, topPos + height - 2, PANEL_COLOR);
            drawUpgradePanel(graphics);
            drawFusionState(graphics);
        }
    }

    private void drawFusionState(GuiGraphics graphics) {
        for (int slot = menu.unlockedSlots(); slot < CraftingCoreBlockEntity.INPUT_COUNT; slot++) {
            var material = menu.getSlot(slot);
            if (!material.hasItem()) {
                graphics.fill(leftPos + material.x - 1, topPos + material.y - 1,
                        leftPos + material.x + 17, topPos + material.y + 17, 0xffc4c5cc);
                graphics.fill(leftPos + material.x + 5, topPos + material.y + 7,
                        leftPos + material.x + 11, topPos + material.y + 12, BORDER_COLOR);
                graphics.fill(leftPos + material.x + 6, topPos + material.y + 4,
                        leftPos + material.x + 10, topPos + material.y + 8, BORDER_COLOR);
            }
        }
        int fill = 24 * menu.progressPercent() / 100;
        if (fill > 0) {
            // Tint the supplied cube silhouette, clipped bottom-to-top; never change the user's PNG.
            graphics.enableScissor(leftPos + 76, topPos + 73 - fill, leftPos + 100, topPos + 73);
            graphics.setColor(0.55F, 0.65F, 1.0F, 1.0F);
            graphics.blit(TEXTURE, leftPos, topPos, imageWidth, imageHeight, 0.0F, 0.0F,
                    PANEL_SOURCE_WIDTH, PANEL_SOURCE_HEIGHT, TEXTURE_SIZE, TEXTURE_SIZE);
            graphics.setColor(1, 1, 1, 1);
            graphics.disableScissor();
        }
    }

    private void drawUpgradePanel(GuiGraphics graphics) {
        int x = leftPos + imageWidth;
        int width = CraftingCoreMenu.UPGRADE_PANEL_WIDTH;
        int height = MultiplicationScreen.panelHeight(menu, CraftingCoreMenu.UPGRADE_PANEL_HEIGHT);
        graphics.fill(x, topPos, x + width, topPos + height, BORDER_COLOR);
        graphics.fill(x, topPos + 1, x + width - 1, topPos + height - 1, LIGHT_COLOR);
        graphics.fill(x, topPos + 2, x + width - 2, topPos + height - 2, PANEL_COLOR);
        for (int upgrade = 0; upgrade < CraftingCoreBlockEntity.UPGRADE_COUNT; upgrade++) {
            int slotX = leftPos + CraftingCoreMenu.UPGRADE_X;
            int slotY = topPos + CraftingCoreMenu.UPGRADE_Y + upgrade * CraftingCoreMenu.SLOT_SPACING;
            Icon.SLOT_BACKGROUND.getBlitter().dest(slotX - 1, slotY - 1).blit(graphics);
            if (!menu.getSlot(CraftingCoreMenu.UPGRADE_MENU_START + upgrade).hasItem()) {
                Icon.BACKGROUND_UPGRADE.getBlitter().dest(slotX, slotY).blit(graphics);
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        if (!configurationOpen) return;
        graphics.drawString(font, Component.translatable("gui.applied_more_machines.output_configuration"),
                8, 9, 0xff413f54, false);
        drawSideLabel(graphics, InfusionOutputSide.TOP, 88, 56);
        drawSideLabel(graphics, InfusionOutputSide.LEFT, 52, 92);
        drawSideLabel(graphics, InfusionOutputSide.FRONT, 88, 92);
        drawSideLabel(graphics, InfusionOutputSide.RIGHT, 124, 92);
        drawSideLabel(graphics, InfusionOutputSide.BOTTOM, 88, 128);
        drawSideLabel(graphics, InfusionOutputSide.BACK, 124, 128);
    }

    private void drawSideLabel(GuiGraphics graphics, InfusionOutputSide side, int centerX, int y) {
        Component label = sideDescription(side);
        graphics.drawString(font, label, centerX - font.width(label) / 2, y, 0xff413f54, false);
    }

    @Override
    protected boolean hasClickedOutside(double mouseX, double mouseY, int left, int top, int button) {
        if (!configurationOpen && (mouseX >= left + imageWidth
                && mouseX < left + imageWidth + CraftingCoreMenu.UPGRADE_PANEL_WIDTH
                && mouseY >= top && mouseY < top + MultiplicationScreen.panelHeight(menu, CraftingCoreMenu.UPGRADE_PANEL_HEIGHT)
                || mouseX >= left - TOOLBAR_WIDTH && mouseX < left
                && mouseY >= top && mouseY < top + toolbarHeight())) {
            return false;
        }
        return super.hasClickedOutside(mouseX, mouseY, left, top, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        boolean wasButtonPress = false;
        for (AltarIconButton iconButton : iconButtons) wasButtonPress |= iconButton.releasePress();
        if (wasButtonPress) {
            setDragging(false);
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void onClose() {
        if (configurationOpen) changePage(false);
        else super.onClose();
    }

    @Override
    public void removed() {
        menu.setConfigurationOpen(false);
        super.removed();
    }

    @Override
    protected void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        if (MultiplicationScreen.tooltip(graphics, font, hoveredSlot, menu.getCarried(), mouseX, mouseY, this::getTooltipFromContainerItem)) return;
        if (!configurationOpen && hoveredSlot != null
                && (hoveredSlot.index == CraftingCoreBlockEntity.INJECTOR_SLOT || hoveredSlot.index == CraftingCoreBlockEntity.CENTER_SLOT)) {
            List<Component> lines = new ArrayList<>();
            if (hoveredSlot.hasItem() && menu.getCarried().isEmpty()) {
                lines.addAll(getTooltipFromContainerItem(hoveredSlot.getItem())); lines.add(Component.empty());
            }
            if (hoveredSlot.index == CraftingCoreBlockEntity.INJECTOR_SLOT) {
                lines.add(Component.translatable("gui.applied_more_machines.core_injectors"));
                lines.add(Component.translatable("gui.applied_more_machines.core_unlocked", menu.unlockedSlots()));
                lines.add(Component.translatable(menu.isWorking() ? "gui.applied_more_machines.core_busy"
                        : "gui.applied_more_machines.core_injector_help").withStyle(ChatFormatting.GRAY));
            } else {
                lines.add(Component.translatable("gui.applied_more_machines.core_center"));
                lines.add(Component.translatable("gui.applied_more_machines.core_center_help").withStyle(ChatFormatting.GRAY));
            }
            graphics.renderComponentTooltip(font, lines, mouseX, mouseY);
            return;
        }
        if (!configurationOpen && hoveredSlot != null
                && hoveredSlot.index >= CraftingCoreMenu.UPGRADE_MENU_START
                && hoveredSlot.index < CraftingCoreMenu.UPGRADE_MENU_END) {
            List<Component> lines = new ArrayList<>();
            if (hoveredSlot.hasItem() && menu.getCarried().isEmpty()) {
                lines.addAll(getTooltipFromContainerItem(hoveredSlot.getItem()));
                lines.add(Component.empty());
            }
            lines.add(Component.translatable("gui.applied_more_machines.available_upgrades"));
            lines.add(Component.translatable("gui.applied_more_machines.upgrade_limit",
                    Component.translatable("item.ae2.speed_card"), CraftingCoreBlockEntity.UPGRADE_COUNT)
                    .withStyle(ChatFormatting.GRAY));
            if (ModList.get().isLoaded("ae2cs")) {
                lines.add(Component.translatable("gui.applied_more_machines.upgrade_limit",
                        Component.translatable("gui.applied_more_machines.overload_card"), CraftingCoreBlockEntity.UPGRADE_COUNT)
                        .withStyle(ChatFormatting.GRAY));
            }
            graphics.renderComponentTooltip(font, lines, mouseX, mouseY);
        } else {
            super.renderTooltip(graphics, mouseX, mouseY);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        updateOutputConfiguration();
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        if (!configurationOpen && isHovering(RECIPE_AREA_X, RECIPE_AREA_Y,
                RECIPE_AREA_WIDTH, RECIPE_AREA_HEIGHT, mouseX, mouseY)) {
            if (!ModList.get().isLoaded("jei") || !isRecipeAreaAvailable()) {
                graphics.renderTooltip(font, Component.translatable("gui.applied_more_machines.progress", menu.progressPercent()), mouseX, mouseY);
            }
        } else if (!configurationOpen) {
            for (int slot = menu.unlockedSlots(); slot < CraftingCoreBlockEntity.INPUT_COUNT; slot++) {
                var material = menu.getSlot(slot);
                if (isHovering(material.x - 1, material.y - 1, 18, 18, mouseX, mouseY)) {
                    graphics.renderTooltip(font, Component.translatable("gui.applied_more_machines.core_locked"), mouseX, mouseY);
                    break;
                }
            }
        }
    }
}
