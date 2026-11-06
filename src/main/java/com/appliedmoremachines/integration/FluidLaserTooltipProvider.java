package com.appliedmoremachines.integration;

import appeng.api.integrations.igtooltip.ClientRegistration;
import appeng.api.integrations.igtooltip.CommonRegistration;
import appeng.api.integrations.igtooltip.TooltipProvider;
import appeng.integration.modules.igtooltip.blocks.GridNodeStateDataProvider;
import appeng.integration.modules.igtooltip.blocks.PowerStorageDataProvider;
import com.appliedmoremachines.AppliedMoreMachines;
import com.appliedmoremachines.FluidLaserBaseBlock;
import com.appliedmoremachines.FluidLaserBaseBlockEntity;
import net.minecraft.resources.ResourceLocation;

/** AE2 supplies Jade's storage and grid-state rows, with no hard Jade dependency. */
public final class FluidLaserTooltipProvider implements TooltipProvider {
    private static final ResourceLocation POWER = ResourceLocation.fromNamespaceAndPath(
            AppliedMoreMachines.MOD_ID, "fluid_laser_power_storage");
    private static final ResourceLocation GRID = ResourceLocation.fromNamespaceAndPath(
            AppliedMoreMachines.MOD_ID, "fluid_laser_grid_node");

    @Override
    public void registerCommon(CommonRegistration registration) {
        // AE2's base-class hook reuses ae2:* data IDs for every class; Jade requires unique IDs.
        registration.addBlockEntityData(POWER, FluidLaserBaseBlockEntity.class, new PowerStorageDataProvider());
        registration.addBlockEntityData(GRID, FluidLaserBaseBlockEntity.class, new GridNodeStateDataProvider());
    }

    @Override
    public void registerClient(ClientRegistration registration) {
        registration.addBlockEntityBody(FluidLaserBaseBlockEntity.class, FluidLaserBaseBlock.class,
                POWER, new PowerStorageDataProvider());
        registration.addBlockEntityBody(FluidLaserBaseBlockEntity.class, FluidLaserBaseBlock.class,
                GRID, new GridNodeStateDataProvider());
    }
}
