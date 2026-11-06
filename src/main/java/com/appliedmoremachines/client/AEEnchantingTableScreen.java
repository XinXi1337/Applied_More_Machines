package com.appliedmoremachines.client;

import appeng.client.gui.Icon;
import appeng.core.localization.ButtonToolTips;
import com.appliedmoremachines.AEEnchantingTableMenu;
import com.appliedmoremachines.AppliedMoreMachines;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntSupplier;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;

/** The supplied atlas owns every panel/button sprite; selection remains server authoritative. */
public final class AEEnchantingTableScreen extends AbstractContainerScreen<AEEnchantingTableMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            AppliedMoreMachines.MOD_ID, "textures/gui/ae_enchanting_table.png");
    private static final String KEY = "gui.applied_more_machines.enchanting.";
    private static final int ATLAS_SIZE = 256;
    private static final int GUIDE_TAB_WIDTH = 24;
    private static final int GUIDE_TAB_HEIGHT = 24;
    private static final int ROW_X = 32;
    private static final int ROW_Y = 18;
    private static final int ROW_WIDTH = 90;
    private static final int ROW_HEIGHT = 19;
    private static final int VISIBLE_ROWS = 3;
    private static final int SCROLL_X = 162;
    private static final int SCROLL_Y = 17;
    private static final int SCROLL_WIDTH = 6;
    private static final int SCROLL_HEIGHT = 60;
    private static final int SCROLL_INNER_Y = SCROLL_Y + 1;
    private static final int SCROLL_INNER_HEIGHT = SCROLL_HEIGHT - 2;
    private static final int CONFIRM_X = 4;
    private static final int CONFIRM_Y = 59;
    private static final int CONFIRM_WIDTH = 24;
    private static final int CONTROL_SIZE = 16;
    private static final int DECREASE_X = 128;
    private static final int INCREASE_X = 145;
    private static final int ARROW_Y = 60;
    private static final int TEXT_COLOR = 0xff413f54;
    private static final int MUTED_COLOR = 0xff8c8ca0;

    private final List<EnchantmentRow> rowButtons = new ArrayList<>();
    private List<Holder<Enchantment>> entries = List.of();
    private Holder<Enchantment> focusedEnchantment;
    private ItemStack lastInput = ItemStack.EMPTY;
    private GuideButton guideButton;
    private AtlasButton confirmButton;
    private AtlasButton decreaseButton;
    private AtlasButton increaseButton;
    private int scrollOffset;
    private boolean draggingScrollbar;
    private int rowClickButton = -1;
    private double thumbGrabOffset;

    public AEEnchantingTableScreen(AEEnchantingTableMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = AEEnchantingTableMenu.IMAGE_WIDTH;
        imageHeight = AEEnchantingTableMenu.IMAGE_HEIGHT;
    }

    @Override
    protected void init() {
        super.init();
        rowButtons.clear();
        for (int row = 0; row < VISIBLE_ROWS; row++) {
            rowButtons.add(addRenderableWidget(new EnchantmentRow(row)));
        }
        guideButton = addRenderableWidget(new GuideButton());
        confirmButton = addRenderableWidget(new AtlasButton(leftPos + CONFIRM_X, topPos + CONFIRM_Y,
                CONFIRM_WIDTH, CONTROL_SIZE, () -> 0, Component.translatable(KEY + "confirm"),
                () -> {
                    if (!menu.isWorking() && menu.hasChannel() && menu.canConfirm()) {
                        sendAction(AEEnchantingTableMenu.CONFIRM);
                    }
                }));
        decreaseButton = addRenderableWidget(new AtlasButton(leftPos + DECREASE_X, topPos + ARROW_Y,
                CONTROL_SIZE, CONTROL_SIZE, () -> hasShiftDown() ? 64 : 32,
                Component.translatable(KEY + "decrease"), () -> changeFocusedLevel(false)));
        increaseButton = addRenderableWidget(new AtlasButton(leftPos + INCREASE_X, topPos + ARROW_Y,
                CONTROL_SIZE, CONTROL_SIZE, () -> hasShiftDown() ? 48 : 16,
                Component.translatable(KEY + "increase"), () -> changeFocusedLevel(true)));
        refreshState();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        refreshState();
    }

    private void refreshState() {
        ItemStack input = menu.input();
        if (!ItemStack.matches(lastInput, input)) {
            lastInput = input.copy();
            scrollOffset = 0;
            focusedEnchantment = null;
            draggingScrollbar = false;
        }
        entries = List.copyOf(menu.availableEnchantments());
        scrollOffset = Mth.clamp(scrollOffset, 0, maxScroll());
        if (focusedEnchantment != null && !entries.contains(focusedEnchantment)) {
            focusedEnchantment = null;
        }
        for (EnchantmentRow row : rowButtons) {
            Holder<Enchantment> enchantment = entryAt(row.row);
            row.active = enchantment != null && canToggle(enchantment);
            row.setMessage(enchantment == null ? Component.translatable(KEY + "no_enchantments")
                    : enchantment.value().description());
        }
        if (confirmButton != null) {
            confirmButton.active = !menu.isWorking() && menu.hasChannel() && menu.canConfirm();
            decreaseButton.active = canChangeFocusedLevel(false);
            increaseButton.active = canChangeFocusedLevel(true);
            decreaseButton.setMessage(Component.translatable(KEY + (hasShiftDown() ? "minimum" : "decrease")));
            increaseButton.setMessage(Component.translatable(KEY + (hasShiftDown() ? "maximum" : "increase")));
        }
    }

    private Holder<Enchantment> entryAt(int row) {
        int index = scrollOffset + row;
        return index >= 0 && index < entries.size() ? entries.get(index) : null;
    }

    private boolean canToggle(Holder<Enchantment> enchantment) {
        return !menu.isWorking() && !menu.isBlocked(enchantment)
                && (menu.targetLevel(enchantment) > 0 || menu.existingLevel(enchantment) < menu.maxLevel(enchantment));
    }

    private boolean canChangeFocusedLevel(boolean increase) {
        if (focusedEnchantment == null || menu.isWorking() || menu.isBlocked(focusedEnchantment)) return false;
        int level = menu.targetLevel(focusedEnchantment);
        if (level <= 0) return false;
        return increase ? level < menu.maxLevel(focusedEnchantment)
                : level > menu.existingLevel(focusedEnchantment) + 1;
    }

    private void changeFocusedLevel(boolean increase) {
        if (!canChangeFocusedLevel(increase)) return;
        int action = hasShiftDown()
                ? (increase ? AEEnchantingTableMenu.MAXIMUM : AEEnchantingTableMenu.MINIMUM)
                : (increase ? AEEnchantingTableMenu.INCREASE : AEEnchantingTableMenu.DECREASE);
        sendAction(menu.actionId(focusedEnchantment, action));
    }

    private void clickRow(int row, int button) {
        clickRow(row, button, hasShiftDown());
    }

    private void clickRow(int row, int button, boolean shift) {
        Holder<Enchantment> enchantment = entryAt(row);
        if (enchantment == null || menu.isWorking()) return;
        int action;
        if (button == 0) {
            focusedEnchantment = enchantment;
            if (!canToggle(enchantment)) return;
            action = shift ? AEEnchantingTableMenu.MAXIMUM : AEEnchantingTableMenu.INCREASE;
        } else if (button == 1 && menu.targetLevel(enchantment) > 0) {
            action = shift ? AEEnchantingTableMenu.MINIMUM : AEEnchantingTableMenu.DECREASE;
        } else {
            return;
        }
        sendAction(menu.actionId(enchantment, action));
    }

    private void openGuide() {
        if (minecraft != null) EnchantingTableGuide.open(minecraft.player);
    }

    private void sendAction(int action) {
        if (action >= 0 && minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, action);
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0.0F, 0.0F,
                imageWidth, imageHeight, ATLAS_SIZE, ATLAS_SIZE);
        graphics.fill(leftPos - GUIDE_TAB_WIDTH, topPos, leftPos, topPos + GUIDE_TAB_HEIGHT, 0xff858591);
        graphics.fill(leftPos - GUIDE_TAB_WIDTH + 1, topPos + 1, leftPos, topPos + GUIDE_TAB_HEIGHT - 1, 0xffffffff);
        graphics.fill(leftPos - GUIDE_TAB_WIDTH + 2, topPos + 2, leftPos, topPos + GUIDE_TAB_HEIGHT - 2, 0xffe9eaee);
        drawScrollbar(graphics, mouseX, mouseY);
        if (menu.isWorking()) {
            // The authoritative menu also prevents moving this stack while the payment is pending.
            graphics.fill(leftPos + 8, topPos + 38, leftPos + 24, topPos + 54, 0x40514a68);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
    }

    private void drawRowBackground(GuiGraphics graphics, int x, int y, int sourceY) {
        // Preserve both one-pixel borders while fitting the supplied108px rows into the90px frame.
        graphics.blit(TEXTURE, x, y, 0.0F, sourceY, 1, ROW_HEIGHT, ATLAS_SIZE, ATLAS_SIZE);
        graphics.blit(TEXTURE, x + 1, y, ROW_WIDTH - 2, ROW_HEIGHT,
                1.0F, sourceY, 106, ROW_HEIGHT, ATLAS_SIZE, ATLAS_SIZE);
        graphics.blit(TEXTURE, x + ROW_WIDTH - 1, y,
                107.0F, sourceY, 1, ROW_HEIGHT, ATLAS_SIZE, ATLAS_SIZE);
    }

    private void drawScrollbar(GuiGraphics graphics, int mouseX, int mouseY) {
        int stateColumn = maxScroll() == 0 ? 2
                : draggingScrollbar || containsLocal(SCROLL_X, SCROLL_Y, SCROLL_WIDTH, SCROLL_HEIGHT, mouseX, mouseY) ? 1 : 0;
        graphics.blit(TEXTURE, leftPos + SCROLL_X + 1, topPos + thumbY(),
                SCROLL_WIDTH - 2, thumbHeight(), 192.0F + stateColumn * 16, stateColumn,
                14, 14 - stateColumn, ATLAS_SIZE, ATLAS_SIZE);
    }

    private int maxScroll() {
        return Math.max(0, entries.size() - VISIBLE_ROWS);
    }

    private int thumbHeight() {
        return entries.size() <= VISIBLE_ROWS ? SCROLL_INNER_HEIGHT
                : Math.max(8, SCROLL_INNER_HEIGHT * VISIBLE_ROWS / entries.size());
    }

    private int thumbY() {
        int travel = SCROLL_INNER_HEIGHT - thumbHeight();
        return SCROLL_INNER_Y + (maxScroll() == 0 ? 0 : Math.round((float) travel * scrollOffset / maxScroll()));
    }

    private void dragScroll(double mouseY) {
        int travel = SCROLL_INNER_HEIGHT - thumbHeight();
        if (maxScroll() <= 0 || travel <= 0) return;
        double position = mouseY - topPos - SCROLL_INNER_Y - thumbGrabOffset;
        scrollOffset = Mth.clamp((int) Math.round(position * maxScroll() / travel), 0, maxScroll());
        refreshState();
    }

    private boolean containsLocal(int x, int y, int width, int height, double mouseX, double mouseY) {
        return mouseX >= leftPos + x && mouseX < leftPos + x + width
                && mouseY >= topPos + y && mouseY < topPos + y + height;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        refreshState();
        if (button == 0 && containsLocal(SCROLL_X, SCROLL_Y, SCROLL_WIDTH, SCROLL_HEIGHT, mouseX, mouseY)) {
            if (maxScroll() > 0) {
                double localY = mouseY - topPos;
                thumbGrabOffset = localY >= thumbY() && localY < thumbY() + thumbHeight()
                        ? localY - thumbY() : thumbHeight() / 2.0;
                draggingScrollbar = true;
                dragScroll(mouseY);
            }
            return true;
        }
        if (button == 0 || button == 1) {
            for (int row = 0; row < VISIBLE_ROWS; row++) {
                if (containsLocal(ROW_X, ROW_Y + ROW_HEIGHT * row, ROW_WIDTH, ROW_HEIGHT, mouseX, mouseY)) {
                    rowClickButton = button;
                    clickRow(row, button);
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected boolean hasClickedOutside(double mouseX, double mouseY, int left, int top, int button) {
        if (mouseX >= left - GUIDE_TAB_WIDTH && mouseX < left
                && mouseY >= top && mouseY < top + GUIDE_TAB_HEIGHT) return false;
        return super.hasClickedOutside(mouseX, mouseY, left, top, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (draggingScrollbar && button == 0) {
            dragScroll(mouseY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        boolean consumed = draggingScrollbar || button == rowClickButton;
        if (button == rowClickButton) rowClickButton = -1;
        if (button == 0) {
            draggingScrollbar = false;
            if (guideButton != null) consumed |= guideButton.releasePress();
            if (confirmButton != null) consumed |= confirmButton.releasePress();
            if (decreaseButton != null) consumed |= decreaseButton.releasePress();
            if (increaseButton != null) consumed |= increaseButton.releasePress();
        }
        if (consumed) {
            // A GUI-button or scrollbar release must never become an inventory drop outside the panel.
            setDragging(false);
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        refreshState();
        if (containsLocal(31, 17, 137, 60, mouseX, mouseY)) {
            if (scrollY != 0.0) {
                scrollOffset = Mth.clamp(scrollOffset - (int) Math.signum(scrollY), 0, maxScroll());
                refreshState();
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    protected void slotClicked(Slot slot, int slotId, int mouseButton, ClickType clickType) {
        if (menu.isWorking() && slotId == 0) return;
        super.slotClicked(slot, slotId, mouseButton, clickType);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        refreshState();
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    private String ellipsize(String text, int width) {
        if (font.width(text) <= width) return text;
        String ellipsis = "…";
        return font.plainSubstrByWidth(text, Math.max(0, width - font.width(ellipsis))) + ellipsis;
    }

    private final class EnchantmentRow extends Button {
        private final int row;

        private EnchantmentRow(int row) {
            super(leftPos + ROW_X, topPos + ROW_Y + row * ROW_HEIGHT, ROW_WIDTH, ROW_HEIGHT,
                    Component.empty(), button -> clickRow(row, 0), DEFAULT_NARRATION);
            this.row = row;
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            Holder<Enchantment> enchantment = entryAt(row);
            boolean selected = enchantment != null && menu.targetLevel(enchantment) > 0;
            boolean blocked = enchantment == null || menu.isBlocked(enchantment)
                    || menu.existingLevel(enchantment) >= menu.maxLevel(enchantment);
            // These widgets are reused when scrolling; hover/focus belongs to the row position.
            // The selected sprite must follow the authoritative enchantment selection only.
            int sourceY = blocked ? 189 : selected ? 208 : 170;
            drawRowBackground(graphics, getX(), getY(), sourceY);
            if (enchantment == null) {
                if (row == 0 && entries.isEmpty()) {
                    Component hint = Component.translatable(KEY + (menu.input().isEmpty() ? "empty" : "no_enchantments"));
                    graphics.drawString(font, ellipsize(hint.getString(), ROW_WIDTH - 6),
                            getX() + 3, getY() + 5, MUTED_COLOR, false);
                }
                return;
            }
            if (enchantment.equals(focusedEnchantment)) {
                graphics.fill(getX(), getY() + 2, getX() + 1, getY() + ROW_HEIGHT - 2, 0xff855da8);
            }
            int color = blocked ? 0xffa5a1b5 : TEXT_COLOR;
            String level = selected ? Integer.toString(menu.targetLevel(enchantment)) : "";
            int levelWidth = font.width(level);
            int nameWidth = ROW_WIDTH - 8 - (selected ? levelWidth + 3 : 0);
            graphics.drawString(font, ellipsize(enchantment.value().description().getString(), nameWidth),
                    getX() + 3, getY() + 5, color, false);
            if (selected) {
                graphics.drawString(font, level, getX() + ROW_WIDTH - 4 - levelWidth,
                        getY() + 5, color, false);
            }
        }

    }

    private final class GuideButton extends Button {
        private boolean pressed;

        private GuideButton() {
            super(leftPos - 20, topPos + 4, 16, 16, ButtonToolTips.OpenGuide.text(),
                    button -> openGuide(), DEFAULT_NARRATION);
            setTooltip(Tooltip.create(ButtonToolTips.OpenGuide.text()));
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            (pressed ? GuiIconSprites.BUTTON_PRESSED : GuiIconSprites.BUTTON_OFF).draw(graphics, getX(), getY());
            Icon.HELP.getBlitter().dest(getX(), getY()).blit(graphics);
        }

        @Override
        public void onClick(double mouseX, double mouseY, int button) {
            pressed = true;
            super.onClick(mouseX, mouseY, button);
        }

        private boolean releasePress() {
            boolean wasPressed = pressed;
            pressed = false;
            return wasPressed;
        }
    }

    private final class AtlasButton extends Button {
        private final IntSupplier spriteRow;
        private boolean pressed;

        private AtlasButton(int x, int y, int width, int height, IntSupplier spriteRow,
                Component message, Runnable action) {
            super(x, y, width, height, message, button -> action.run(), DEFAULT_NARRATION);
            this.spriteRow = spriteRow;
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            int column = !active || pressed ? 2 : isHoveredOrFocused() ? 1 : 0;
            graphics.blit(TEXTURE, getX(), getY(), getWidth(), getHeight(),
                    192.0F + column * 16, spriteRow.getAsInt(), 16, 16, ATLAS_SIZE, ATLAS_SIZE);
        }

        @Override
        public void onClick(double mouseX, double mouseY, int button) {
            pressed = true;
            super.onClick(mouseX, mouseY, button);
        }

        private boolean releasePress() {
            boolean wasPressed = pressed;
            pressed = false;
            return wasPressed;
        }
    }
}
