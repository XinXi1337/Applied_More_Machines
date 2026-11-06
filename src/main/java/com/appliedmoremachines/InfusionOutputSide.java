package com.appliedmoremachines;

import net.minecraft.core.Direction;

/** Stable relative-side bits shared by saved settings, menu data and buttons. */
public enum InfusionOutputSide {
    FRONT(0), BACK(1), LEFT(2), RIGHT(3), TOP(4), BOTTOM(5);

    public static final int ALL_SIDES = 0x3f;
    private final int bitIndex;

    InfusionOutputSide(int bitIndex) {
        this.bitIndex = bitIndex;
    }

    public int mask() {
        return 1 << bitIndex;
    }

    public int buttonId() {
        return bitIndex + 1;
    }

    /** Matches AE2's horizontal orientation: NORTH front has WEST left and EAST right. */
    public Direction worldDirection(Direction front) {
        return switch (this) {
            case FRONT -> front;
            case BACK -> front.getOpposite();
            case LEFT -> front.getCounterClockWise();
            case RIGHT -> front.getClockWise();
            case TOP -> Direction.UP;
            case BOTTOM -> Direction.DOWN;
        };
    }

    public static InfusionOutputSide fromButtonId(int buttonId) {
        for (InfusionOutputSide side : values()) {
            if (side.buttonId() == buttonId) {
                return side;
            }
        }
        return null;
    }
}
