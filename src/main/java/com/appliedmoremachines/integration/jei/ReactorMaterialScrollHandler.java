package com.appliedmoremachines.integration.jei;

import mezz.jei.api.gui.inputs.IJeiGuiEventListener;
import net.minecraft.client.gui.navigation.ScreenRectangle;

final class ReactorMaterialScrollHandler implements IJeiGuiEventListener {
    private static final int PANEL_X = 116;
    private static final int PANEL_Y = 10;
    private static final int PANEL_WIDTH = 65;
    private static final int PANEL_HEIGHT = 112;
    private static final int TRACK_X = PANEL_X + 58;
    private static final int TRACK_Y = PANEL_Y + 3;
    private final ReactorMaterialScroll scroll;

    ReactorMaterialScrollHandler(ReactorMaterialScroll scroll) {
        this.scroll = scroll;
    }

    @Override public ScreenRectangle getArea() { return new ScreenRectangle(0, 0, 183, 126); }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!insidePanel(mouseX, mouseY) || scrollY == 0 || scroll.maxFirstRow() == 0) return false;
        scroll.scroll((int) -Math.signum(scrollY));
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || !insideTrack(mouseX, mouseY) || scroll.maxFirstRow() == 0) return false;
        scroll.beginDrag(mouseY - TRACK_Y, scroll.thumbOffset());
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (!scroll.isDragging()) return false;
        scroll.dragTo(mouseY - TRACK_Y);
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (!scroll.isDragging()) return false;
        scroll.endDrag();
        return true;
    }

    private static boolean insidePanel(double x, double y) {
        return x >= PANEL_X && x < PANEL_X + PANEL_WIDTH
                && y >= PANEL_Y && y < PANEL_Y + PANEL_HEIGHT;
    }

    private static boolean insideTrack(double x, double y) {
        return x >= TRACK_X && x < TRACK_X + 7
                && y >= TRACK_Y && y < TRACK_Y + ReactorMaterialScroll.TRACK_HEIGHT;
    }
}
