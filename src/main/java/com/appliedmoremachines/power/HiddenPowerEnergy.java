package com.appliedmoremachines.power;

import appeng.api.config.Actionable;
import java.util.function.LongUnaryOperator;

/** AE buffer and the sub-FE conversion remainder. Neither simulation nor rounding creates energy. */
public final class HiddenPowerEnergy {
    public static final double CAPACITY = 64_000_000;
    private final Runnable changed;
    private double stored, remainder;

    public HiddenPowerEnergy(Runnable changed) { this.changed = changed; }
    public double stored() { return stored; }
    public double remainder() { return remainder; }
    public void load(double value, double credit) {
        stored = Double.isFinite(value) ? Math.clamp(value, 0, CAPACITY) : 0;
        remainder = Double.isFinite(credit) && credit > 0 ? credit : 0;
    }
    public double insert(double amount, Actionable mode) {
        if (!Double.isFinite(amount) || amount <= 0) return amount;
        double accepted = Math.min(amount, CAPACITY - stored);
        if (mode == Actionable.MODULATE && accepted > 0) { stored += accepted; changed.run(); }
        return amount - accepted;
    }
    public double extract(double amount, Actionable mode) {
        if (!Double.isFinite(amount) || amount <= 0) return 0;
        double taken = Math.min(amount, stored);
        if (mode == Actionable.MODULATE && taken > 0) { stored -= taken; changed.run(); }
        return taken;
    }

    /** A request-local view prevents SIMULATE from counting the same fractional credit twice. */
    public Draw draw(Actionable mode, double aePerFe) { return new Draw(mode, aePerFe); }
    public final class Draw {
        private final Actionable mode;
        private final double ratio;
        private double credit = remainder;
        private Draw(Actionable mode, double ratio) { this.mode = mode; this.ratio = ratio; }
        public double take(double requested, LongUnaryOperator source) {
            if (!Double.isFinite(requested) || requested <= 0 || !Double.isFinite(ratio) || ratio <= 0) return 0;
            double fromCredit = Math.min(requested, credit);
            credit -= fromCredit;
            double missing = requested - fromCredit;
            long count = missing > 0 ? (long) Math.min(Long.MAX_VALUE, Math.ceil(missing / ratio)) : 0;
            long received = count > 0 ? Math.clamp(source.applyAsLong(count), 0, count) : 0;
            double paid = received * ratio;
            double used = Math.min(missing, paid);
            credit += paid - used;
            if (mode == Actionable.MODULATE && remainder != credit) { remainder = credit; changed.run(); }
            return fromCredit + used;
        }
    }
}
