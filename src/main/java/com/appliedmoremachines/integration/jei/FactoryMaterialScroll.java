package com.appliedmoremachines.integration.jei;

final class FactoryMaterialScroll {
    static final int COLUMNS = 3;
    static final int VISIBLE_ROWS = 6;
    static final int TRACK_HEIGHT = 106;
    static final int THUMB_MIN_HEIGHT = 12;

    private final int rowCount;
    private int firstRow;
    private int dragGrabOffset;
    private boolean dragging;

    FactoryMaterialScroll(int slotCount) {
        rowCount = (slotCount + COLUMNS - 1) / COLUMNS;
    }

    int maxFirstRow() {
        return Math.max(0, rowCount - VISIBLE_ROWS);
    }

    int thumbHeight() {
        if (rowCount <= VISIBLE_ROWS) return TRACK_HEIGHT;
        return Math.max(THUMB_MIN_HEIGHT, TRACK_HEIGHT * VISIBLE_ROWS / rowCount);
    }

    int thumbOffset() {
        int maxFirstRow = maxFirstRow();
        int travel = TRACK_HEIGHT - thumbHeight();
        return maxFirstRow == 0 ? 0 : travel * firstRow / maxFirstRow;
    }

    int firstRow() {
        return firstRow;
    }

    void scroll(int rows) {
        firstRow = Math.max(0, Math.min(maxFirstRow(), firstRow + rows));
    }

    void beginDrag(double mouseY, int thumbTop) {
        dragging = true;
        dragGrabOffset = thumbTop <= mouseY && mouseY < thumbTop + thumbHeight()
                ? (int) mouseY - thumbTop : thumbHeight() / 2;
        dragTo(mouseY);
    }

    void dragTo(double mouseY) {
        if (!dragging) return;
        int travel = TRACK_HEIGHT - thumbHeight();
        int thumbTop = Math.max(0, Math.min(travel,
                (int) Math.round(mouseY - dragGrabOffset)));
        firstRow = travel == 0 ? 0 : (int) Math.round((double) thumbTop * maxFirstRow() / travel);
    }

    void endDrag() {
        dragging = false;
    }

    boolean isDragging() {
        return dragging;
    }
}
