package com.appliedmoremachines.laser;

import com.appliedmoremachines.MachineAvailability;

/** Optional registry gate; no IF or IFEU implementation classes are linked here. */
public final class FluidLaserAvailability {
    public static boolean enabled() { return MachineAvailability.enabled("ae_fluid_laser_base"); }
    private FluidLaserAvailability() {}
}
