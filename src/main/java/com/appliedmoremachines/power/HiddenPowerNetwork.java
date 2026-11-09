package com.appliedmoremachines.power;

import appeng.api.config.Actionable;
import appeng.api.config.PowerUnit;
import appeng.api.networking.IGrid;
import appeng.api.networking.security.IActionSource;
import com.appliedmoremachines.AEHiddenBackupPowerBlockEntity;
import java.util.*;
import java.util.function.DoubleSupplier;

/** One ordered transaction over all energy-connected grids, including quartz-fiber overlays. */
public final class HiddenPowerNetwork {
    private static final ThreadLocal<Integer> DEPTH = ThreadLocal.withInitial(() -> 0);
    private HiddenPowerNetwork() {}
    public static boolean bypassed() { return DEPTH.get() != 0; }
    public static double bypass(DoubleSupplier operation) {
        int previous = DEPTH.get(); DEPTH.set(previous + 1);
        try { return operation.getAsDouble(); }
        finally { if (previous == 0) DEPTH.remove(); else DEPTH.set(previous); }
    }
    public static double extract(List<IGrid> grids, double amount, Actionable mode) {
        if (!Double.isFinite(amount) || amount <= 0 || bypassed()) return 0;
        return bypass(() -> extractOrdered(grids, amount, mode));
    }
    private static double extractOrdered(List<IGrid> grids, double amount, Actionable mode) {
        var groups = new LinkedHashMap<IGrid, List<AEHiddenBackupPowerBlockEntity>>();
        for (IGrid grid : grids) {
            var hosts = new ArrayList<AEHiddenBackupPowerBlockEntity>();
            for (var host : grid.getMachines(AEHiddenBackupPowerBlockEntity.class))
                if (host.hasChannel() && !host.isRemoved()) hosts.add(host);
            hosts.sort(Comparator.comparingLong(h -> h.getBlockPos().asLong()));
            if (!hosts.isEmpty()) groups.put(grid, hosts);
        }
        if (groups.isEmpty()) return 0;
        double ratio = PowerUnit.FE.convertTo(PowerUnit.AE, 1);
        var draws = new IdentityHashMap<AEHiddenBackupPowerBlockEntity, HiddenPowerEnergy.Draw>();
        double paid = 0;
        // First exhaust ordinary network FE in every enabled grid, never the hidden insert-only mounts.
        for (var group : groups.entrySet()) {
            if (paid >= amount) break;
            var host = group.getValue().getFirst();
            var draw = draws.computeIfAbsent(host, h -> h.energy().draw(mode, ratio));
            paid += draw.take(amount - paid, n -> group.getKey().getStorageService().getInventory()
                    .extract(HiddenPowerCell.key(), n, mode, IActionSource.ofMachine(host)));
        }
        // Then every hidden disk, followed by every private AE reservoir.
        for (var hosts : groups.values()) for (var host : hosts) {
            if (paid >= amount) break;
            var draw = draws.computeIfAbsent(host, h -> h.energy().draw(mode, ratio));
            paid += draw.take(amount - paid, n -> host.extractDisk(n, mode));
        }
        for (var hosts : groups.values()) for (var host : hosts) {
            if (paid >= amount) break;
            paid += host.energy().extract(amount - paid, mode);
        }
        return Math.min(amount, paid);
    }
}
