package com.appliedmoremachines;
/** Shared whole-reactor limits also cover cards installed before formation. */
public final class ReactorUpgradeMath {
    public record Metrics(int parallel, int durationTicks) {}
    private ReactorUpgradeMath() {}
    public static Metrics calculate(int speed, int overload) {
        speed = Math.clamp(speed, 0, 4); overload = Math.clamp(overload, 0, 4);
        return new Metrics(8 + speed * 64 + overload * 256, 160 - speed * 10 - overload * 20);
    }
}
