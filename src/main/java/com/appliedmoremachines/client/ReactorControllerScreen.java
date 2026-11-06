package com.appliedmoremachines.client;

import appeng.client.gui.Icon;
import com.appliedmoremachines.AppliedMoreMachines;
import com.appliedmoremachines.ReactorControllerBlockEntity;
import com.appliedmoremachines.ReactorControllerMenu;
import com.appliedmoremachines.ReactorUpgradeCards;
import com.appliedmoremachines.reactor.ReactorProcessor;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** Compact controller interface; no player inventory slots are part of this panel. */
public final class ReactorControllerScreen extends AbstractContainerScreen<ReactorControllerMenu> {
    public static final int RECIPE_AREA_X = 24;
    public static final int RECIPE_AREA_Y = 27;
    public static final int RECIPE_AREA_WIDTH = 40;
    public static final int RECIPE_AREA_HEIGHT = 28;
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            AppliedMoreMachines.MOD_ID, "textures/gui/ae_factory_controller_gui.png");
    private static final int TEXTURE_SIZE = 256;
    private static final int BORDER = 0xff747783;
    private static final int PANEL = 0xffd8dae2;
    private static final int TEXT = 0xff343344;
    private static final int MUTED = 0xff666776;
    private static final int GOOD = 0xff28693b;
    private static final int BAD = 0xffa12828;
    private static final int BUTTON_Y = 99;
    private static final int BUTTON_WIDTH = 46;
    private static final int BUTTON_HEIGHT = 18;
    private static final int[] BUTTON_X = {13, 65, 117};
    private static final int INFO_X = 94;
    private FactoryProgressMask progressMask;

    public ReactorControllerScreen(ReactorControllerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = ReactorControllerMenu.IMAGE_WIDTH;
        imageHeight = ReactorControllerMenu.IMAGE_HEIGHT;
    }

    /** Navigation is supplied by the optional JEI plugin; carrying an item disables the hit area. */
    public boolean isRecipeAreaAvailable() {
        return menu.getCarried().isEmpty();
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, imageWidth, ReactorControllerMenu.BASE_HEIGHT, 0.0F, 0.0F,
                ReactorControllerMenu.IMAGE_WIDTH, ReactorControllerMenu.BASE_HEIGHT, TEXTURE_SIZE, TEXTURE_SIZE);
        if (progressMask == null) progressMask = new FactoryProgressMask(TEXTURE);
        float progress = menu.progress() / 1000F;
        if (menu.processingState() == ReactorProcessor.WORKING && menu.durationTicks() > 0) {
            progress += partialTick / menu.durationTicks();
        }
        progressMask.draw(graphics, leftPos, topPos, progress);
        drawInventoryPanel(graphics);
        drawUpgradePanel(graphics);

    }

    private void drawInventoryPanel(GuiGraphics graphics) {
        int y = topPos + ReactorControllerMenu.BASE_HEIGHT;
        graphics.fill(leftPos, y, leftPos + imageWidth, topPos + imageHeight, BORDER);
        graphics.fill(leftPos + 1, y + 1, leftPos + imageWidth - 1, topPos + imageHeight - 1, 0xfff7f7fa);
        graphics.fill(leftPos + 2, y + 2, leftPos + imageWidth - 2, topPos + imageHeight - 2, PANEL);
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                Icon.SLOT_BACKGROUND.getBlitter().dest(
                        leftPos + ReactorControllerMenu.PLAYER_X + column * ReactorControllerMenu.SLOT_SPACING - 1,
                        topPos + ReactorControllerMenu.PLAYER_Y + row * ReactorControllerMenu.SLOT_SPACING - 1).blit(graphics);
            }
        }
        for (int column = 0; column < 9; column++) {
            Icon.SLOT_BACKGROUND.getBlitter().dest(
                    leftPos + ReactorControllerMenu.PLAYER_X + column * ReactorControllerMenu.SLOT_SPACING - 1,
                    topPos + ReactorControllerMenu.HOTBAR_Y - 1).blit(graphics);
        }
    }

    private void drawUpgradePanel(GuiGraphics graphics) {
        int x = leftPos + imageWidth;
        int width = ReactorControllerMenu.UPGRADE_PANEL_WIDTH;
        int height = ReactorControllerMenu.UPGRADE_PANEL_HEIGHT;
        graphics.fill(x, topPos, x + width, topPos + height, BORDER);
        graphics.fill(x + 1, topPos + 1, x + width - 1, topPos + height - 1, 0xffffffff);
        graphics.fill(x + 2, topPos + 2, x + width - 2, topPos + height - 2, PANEL);
        for (int slot = 0; slot < ReactorControllerBlockEntity.UPGRADE_COUNT; slot++) {
            int slotX = leftPos + ReactorControllerMenu.UPGRADE_X;
            int slotY = topPos + ReactorControllerMenu.UPGRADE_Y + slot * ReactorControllerMenu.SLOT_SPACING;
            Icon.SLOT_BACKGROUND.getBlitter().dest(slotX - 1, slotY - 1).blit(graphics);
            if (!menu.getSlot(slot).hasItem()) {
                Icon.BACKGROUND_UPGRADE.getBlitter().dest(slotX, slotY).blit(graphics);
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        centered(graphics, Component.translatable("gui.applied_more_machines.factory.structure_short",
                Component.translatable(menu.isFormed()
                        ? "gui.applied_more_machines.factory.formed"
                        : "gui.applied_more_machines.factory.unformed")),
                44, 10, menu.isFormed() ? GOOD : BAD);

        if (menu.isFormed()) {
            graphics.drawString(font, Component.translatable("gui.applied_more_machines.factory.parallel",
                    " " + menu.parallel()), INFO_X, 10, TEXT, false);
            graphics.drawString(font, Component.translatable("gui.applied_more_machines.factory.duration",
                    " " + menu.durationTicks()), INFO_X, 27, TEXT, false);
        } else {
            graphics.drawString(font, Component.translatable("gui.applied_more_machines.factory.parallel", ""),
                    INFO_X, 10, TEXT, false);
            graphics.drawString(font, Component.translatable("gui.applied_more_machines.factory.duration", ""),
                    INFO_X, 27, TEXT, false);
        }
        graphics.drawString(font, Component.translatable("gui.applied_more_machines.factory.ae_status",
                Component.translatable(menu.isAeConnected()
                        ? "gui.applied_more_machines.factory.connected"
                        : "gui.applied_more_machines.factory.disconnected")),
                INFO_X, 44, menu.isAeConnected() ? GOOD : MUTED, false);

        drawTaskText(graphics);
        Component action = statusText();
        if (menu.illegalPatterns() == 0 && !action.getString().isEmpty()) centered(graphics, action, 88, 88, statusColor());


    }

    private void drawTaskText(GuiGraphics graphics) {
        String stateKey = switch (menu.processingState()) {
            case ReactorProcessor.WORKING -> "working";
            case ReactorProcessor.OFFLINE -> "waiting_network";
            case ReactorProcessor.NO_POWER -> "waiting_energy";
            case ReactorProcessor.CHEMICAL_MISSING -> "waiting_chemical";
            case ReactorProcessor.RECIPE_CHANGED -> "waiting_recipe";
            case ReactorProcessor.OUTPUT_FULL -> "waiting_output";
            case ReactorProcessor.UNFORMED -> "waiting_structure";
            default -> menu.queuedTasks() > 0 ? "collecting" : "idle";
        };
        graphics.drawString(font, font.plainSubstrByWidth(Component.translatable(
                        "gui.applied_more_machines.factory." + stateKey).getString(), 155),
                10, 64, TEXT, false);
        String outputs = menu.taskDisplay().stream().map(stack -> stack.what().getDisplayName().getString()
                + " ×" + stack.amount()).collect(java.util.stream.Collectors.joining("，"));
        graphics.drawString(font, font.plainSubstrByWidth(outputs, 155), 10, 76, TEXT, false);
        if (menu.illegalPatterns() > 0) {
            centeredTask(graphics, Component.translatable("gui.applied_more_machines.factory.illegal_pattern",
                    menu.illegalPatterns()), BAD);
        } else if (menu.chemicalPaused() > 0 && menu.actionStatus() == ReactorControllerBlockEntity.STATUS_IDLE) {
            centeredTask(graphics, Component.translatable("gui.applied_more_machines.factory.waiting_chemical"), MUTED);
        }
    }

    private void centeredTask(GuiGraphics graphics, Component component, int color) {
        String text = font.plainSubstrByWidth(component.getString(), 155);
        graphics.drawString(font, text, 88 - font.width(text) / 2, 88, color, false);
    }

    private Component statusText() {
        return switch (menu.actionStatus()) {
            case ReactorControllerBlockEntity.STATUS_TANK_NOT_EMPTY -> Component.translatable("gui.applied_more_machines.reactor.tank_not_empty");
            case ReactorControllerBlockEntity.STATUS_MATERIALS ->
                    Component.translatable("gui.applied_more_machines.factory.not_enough",
                            Math.max(1, menu.actionAmount()));
            case ReactorControllerBlockEntity.STATUS_CONFLICT ->
                    Component.translatable("gui.applied_more_machines.factory.conflict",
                            Math.max(1, menu.actionAmount()));
            case ReactorControllerBlockEntity.STATUS_PLACED ->
                    Component.translatable("gui.applied_more_machines.factory.place_done");
            case ReactorControllerBlockEntity.STATUS_DISMANTLED ->
                    Component.translatable("gui.applied_more_machines.factory.dismantle_done",
                            Math.max(0, menu.actionAmount()));
            case ReactorControllerBlockEntity.STATUS_ALREADY_FORMED ->
                    Component.translatable("gui.applied_more_machines.factory.already_formed");
            case ReactorControllerBlockEntity.STATUS_RETURN_FULL ->
                    Component.translatable("gui.applied_more_machines.factory.return_full");
            case ReactorControllerBlockEntity.STATUS_NOT_FORMED ->
                    Component.translatable("gui.applied_more_machines.factory.dismantle_unformed");
            case ReactorControllerBlockEntity.STATUS_DISMANTLE_PROGRESS ->
                    Component.translatable("gui.applied_more_machines.factory.dismantle_progress",
                            menu.dismantleClicks());
            case ReactorControllerBlockEntity.STATUS_PLACEMENT_FAILED ->
                    Component.translatable("gui.applied_more_machines.factory.place_failed");
            default -> Component.empty();
        };
    }

    private int statusColor() {
        return switch (menu.actionStatus()) {
            case ReactorControllerBlockEntity.STATUS_PLACED,
                    ReactorControllerBlockEntity.STATUS_DISMANTLED -> GOOD;
            case ReactorControllerBlockEntity.STATUS_MATERIALS,
                    ReactorControllerBlockEntity.STATUS_CONFLICT,
                    ReactorControllerBlockEntity.STATUS_RETURN_FULL,
                    ReactorControllerBlockEntity.STATUS_PLACEMENT_FAILED -> BAD;
            default -> TEXT;
        };
    }

    private void centered(GuiGraphics graphics, Component text, int centerX, int y, int color) {
        graphics.drawString(font, text, centerX - font.width(text) / 2, y, color, false);
    }

    private final List<appeng.client.gui.widgets.AE2Button> actionButtons = new ArrayList<>();
    @Override protected void init() {
        super.init(); actionButtons.clear();
        String[] labels = {"project", "place", "dismantle"};
        for (int i = 0; i < labels.length; i++) {
            final int action = 201 + i;
            var button = new appeng.client.gui.widgets.AE2Button(leftPos + BUTTON_X[i], topPos + BUTTON_Y,
                    BUTTON_WIDTH, BUTTON_HEIGHT, Component.translatable("gui.applied_more_machines.factory." + labels[i]),
                    b -> { if (menu.getCarried().isEmpty() && minecraft != null && minecraft.gameMode != null)
                        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, action); });
            actionButtons.add(addRenderableWidget(button));
        }
    }

    @Override
    protected boolean hasClickedOutside(double mouseX, double mouseY, int left, int top, int button) {
        if (mouseX >= left + imageWidth && mouseX < left + imageWidth + ReactorControllerMenu.UPGRADE_PANEL_WIDTH
                && mouseY >= top && mouseY < top + ReactorControllerMenu.UPGRADE_PANEL_HEIGHT) {
            return false;
        }
        return super.hasClickedOutside(mouseX, mouseY, left, top, button);
    }

    @Override
    protected void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        if (isHovering(8, 61, 160, 34, mouseX, mouseY)) {
            List<Component> taskLines = new ArrayList<>();
            taskLines.add(Component.translatable("gui.applied_more_machines.factory.task_counts",
                    menu.activeTasks(), menu.queuedTasks()));
            for (var stack : menu.taskDisplay()) taskLines.add(Component.literal(
                    stack.what().getDisplayName().getString() + " ×" + stack.amount()));
            if (menu.illegalPatterns() > 0) taskLines.add(Component.translatable(
                    "gui.applied_more_machines.factory.illegal_pattern", menu.illegalPatterns()).withStyle(ChatFormatting.RED));
            graphics.renderComponentTooltip(font, taskLines, mouseX, mouseY);
            return;
        }
        if (hoveredSlot != null && hoveredSlot.index >= 0
                && hoveredSlot.index < ReactorControllerBlockEntity.UPGRADE_COUNT) {
            List<Component> lines = new ArrayList<>();
            if (hoveredSlot.hasItem() && menu.getCarried().isEmpty()) {
                lines.addAll(getTooltipFromContainerItem(hoveredSlot.getItem()));
                lines.add(Component.empty());
            }
            lines.add(Component.translatable("gui.applied_more_machines.factory.available_upgrades"));
            for (ReactorUpgradeCards.Kind kind : ReactorUpgradeCards.Kind.values()) {
                lines.add(Component.translatable("gui.applied_more_machines.reactor.upgrade_limit", kind.label(), kind.limit()).withStyle(ChatFormatting.GRAY));
            }
            graphics.renderComponentTooltip(font, lines, mouseX, mouseY);
            return;
        }
        super.renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        actionButtons.forEach(button -> button.active = menu.getCarried().isEmpty());
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
