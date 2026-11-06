package com.appliedmoremachines.integration.jei;

import java.util.List;
import java.util.Optional;
import mezz.jei.api.gui.ingredient.IRecipeSlotDrawable;
import mezz.jei.api.gui.inputs.RecipeSlotUnderMouse;
import mezz.jei.api.gui.widgets.ISlottedRecipeWidget;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenPosition;

final class ReactorMaterialGridWidget implements ISlottedRecipeWidget {
    private static final int X = 118;
    private static final int Y = 12;
    private static final int SLOT_SIZE = 18;
    private static final int TRACK_X = 56;
    private static final int TRACK_Y = 1;
    private final List<IRecipeSlotDrawable> slots;
    private final ReactorMaterialScroll scroll;
    private final ScreenPosition position = new ScreenPosition(X, Y);

    ReactorMaterialGridWidget(List<IRecipeSlotDrawable> slots, ReactorMaterialScroll scroll) {
        this.slots = List.copyOf(slots);
        this.scroll = scroll;
        positionSlots();
    }

    @Override public ScreenPosition getPosition() { return position; }

    @Override
    public void drawWidget(GuiGraphics graphics, double mouseX, double mouseY) {
        positionSlots();
        int firstIndex = scroll.firstRow() * ReactorMaterialScroll.COLUMNS;
        int visibleEnd = Math.min(slots.size(), firstIndex
                + ReactorMaterialScroll.VISIBLE_ROWS * ReactorMaterialScroll.COLUMNS);
        for (int i = firstIndex; i < visibleEnd; i++) {
            IRecipeSlotDrawable slot = slots.get(i);
            int row = i / ReactorMaterialScroll.COLUMNS - scroll.firstRow();
            if (row < 0 || row >= ReactorMaterialScroll.VISIBLE_ROWS) continue;
            slot.draw(graphics, slot.isMouseOver(mouseX, mouseY));
        }

        int thumbTop = TRACK_Y + scroll.thumbOffset();
        int thumbBottom = thumbTop + scroll.thumbHeight();
        graphics.fill(TRACK_X, thumbTop, TRACK_X + 6, thumbBottom, 0xff686b7c);
        graphics.fill(TRACK_X + 1, thumbTop + 1, TRACK_X + 5, thumbBottom - 1, 0xffc7cad8);
        graphics.fill(TRACK_X + 2, thumbTop + 2, TRACK_X + 4, thumbBottom - 2, 0xff9599ab);
    }

    @Override
    public Optional<RecipeSlotUnderMouse> getSlotUnderMouse(double mouseX, double mouseY) {
        for (int row = 0; row < ReactorMaterialScroll.VISIBLE_ROWS; row++) {
            int firstIndex = (scroll.firstRow() + row) * ReactorMaterialScroll.COLUMNS;
            for (int column = 0; column < ReactorMaterialScroll.COLUMNS; column++) {
                int index = firstIndex + column;
                if (index >= slots.size()) return Optional.empty();
                IRecipeSlotDrawable slot = slots.get(index);
                if (slot.isMouseOver(mouseX, mouseY)) {
                    return Optional.of(new RecipeSlotUnderMouse(slot, position));
                }
            }
        }
        return Optional.empty();
    }

    private void positionSlots() {
        for (int i = 0; i < slots.size(); i++) {
            int row = i / ReactorMaterialScroll.COLUMNS - scroll.firstRow();
            int column = i % ReactorMaterialScroll.COLUMNS;
            if (row < 0 || row >= ReactorMaterialScroll.VISIBLE_ROWS) {
                slots.get(i).setPosition(-2048, -2048);
            } else {
                slots.get(i).setPosition(column * SLOT_SIZE + 1, row * SLOT_SIZE + 1);
            }
        }
    }
}
