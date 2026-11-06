package com.appliedmoremachines;

/** Pure factory timing formula, kept independent from Minecraft registries for verification. */
public final class FactoryUpgradeMath {
    public record Metrics(int parallel, int durationTicks) {}

    private FactoryUpgradeMath() {}

    public static Metrics calculate(int aeSpeed, int overload, int mekanismSpeed, int mekanismStack) {
        int reduction = aeSpeed * 10 + overload * 20 + mekanismSpeed * 2;
        int parallel = 8 + aeSpeed * 64 + overload * 256 + mekanismSpeed * 128;
        // Stack upgrades affect the round planner's batch allowance, not duration or base parallel.
        return new Metrics(parallel, Math.max(1, 160 - reduction));
    }
}
