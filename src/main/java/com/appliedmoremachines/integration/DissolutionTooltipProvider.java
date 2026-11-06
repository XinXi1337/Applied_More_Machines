package com.appliedmoremachines.integration;

import appeng.api.integrations.igtooltip.ClientRegistration;
import appeng.api.integrations.igtooltip.CommonRegistration;
import appeng.api.integrations.igtooltip.TooltipProvider;
import appeng.integration.modules.igtooltip.blocks.GridNodeStateDataProvider;
import appeng.integration.modules.igtooltip.blocks.PowerStorageDataProvider;
import com.appliedmoremachines.AppliedMoreMachines;
import com.appliedmoremachines.DissolutionChamberBlock;
import com.appliedmoremachines.DissolutionChamberBlockEntity;
import net.minecraft.resources.ResourceLocation;

/** AE2 supplies Jade's storage and grid-state rows, with no hard Jade dependency. */
public final class DissolutionTooltipProvider implements TooltipProvider {
    private static final ResourceLocation POWER = ResourceLocation.fromNamespaceAndPath(
            AppliedMoreMachines.MOD_ID, "dissolution_power_storage");
    private static final ResourceLocation GRID = ResourceLocation.fromNamespaceAndPath(
            AppliedMoreMachines.MOD_ID, "dissolution_grid_node");

    @Override
    public void registerCommon(CommonRegistration registration) {
        // AE2's base-class hook reuses ae2:* data IDs for every class; Jade requires unique IDs.
        registration.addBlockEntityData(POWER, DissolutionChamberBlockEntity.class, new PowerStorageDataProvider());
        registration.addBlockEntityData(GRID, DissolutionChamberBlockEntity.class, new GridNodeStateDataProvider());
    }

    @Override
    public void registerClient(ClientRegistration registration) {
        registration.addBlockEntityBody(DissolutionChamberBlockEntity.class, DissolutionChamberBlock.class,
                POWER, new PowerStorageDataProvider());
        registration.addBlockEntityBody(DissolutionChamberBlockEntity.class, DissolutionChamberBlock.class,
                GRID, new GridNodeStateDataProvider());
    }
}
