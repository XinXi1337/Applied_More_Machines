package com.appliedmoremachines.laser;

import net.neoforged.fml.ModList;

/** Optional registry gate; no IF or IFEU implementation classes are linked here. */
public final class FluidLaserAvailability {
    public static boolean enabled() { return ModList.get()!=null && ModList.get().isLoaded("ifeu"); }
    private FluidLaserAvailability() {}
}
