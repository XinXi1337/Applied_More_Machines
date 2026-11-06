package com.appliedmoremachines.processing;

/** Integer arithmetic keeps additive speed reductions exact, including four overclock cards. */
public final class InfusionTiming {
    public static final int BASE_TICKS = 80;
    public static final int AE_PER_CRAFT = 4_000;

    private InfusionTiming() {}

    public static int duration(int speedCards, int overloadCards) {
        return Math.max(1, BASE_TICKS - 16 * speedCards - 20 * overloadCards);
    }
}
