package com.appliedmoremachines.integration;

import appeng.api.integrations.igtooltip.ClientRegistration;
import appeng.api.integrations.igtooltip.CommonRegistration;
import appeng.api.integrations.igtooltip.TooltipProvider;
import appeng.integration.modules.igtooltip.blocks.GridNodeStateDataProvider;
import appeng.integration.modules.igtooltip.blocks.PowerStorageDataProvider;
import com.appliedmoremachines.AppliedMoreMachines;
import com.appliedmoremachines.AEEnergizingBlock;
import com.appliedmoremachines.AEEnergizingBlockEntity;
import net.minecraft.resources.ResourceLocation;

/** AE2 supplies Jade's storage and grid-state rows, with no hard Jade dependency. */
public final class EnergizingTooltipProvider implements TooltipProvider {
    private static final ResourceLocation POWER = ResourceLocation.fromNamespaceAndPath(
            AppliedMoreMachines.MOD_ID, "energizing_power_storage");
    private static final ResourceLocation GRID = ResourceLocation.fromNamespaceAndPath(
            AppliedMoreMachines.MOD_ID, "energizing_grid_node");

    @Override
    public void registerCommon(CommonRegistration registration) {
        if (com.appliedmoremachines.ModBlocks.AE_ENERGIZING_ORB == null) return;
        // AE2's base-class hook reuses ae2:* data IDs for every class; Jade requires unique IDs.
        registration.addBlockEntityData(POWER, AEEnergizingBlockEntity.class, new PowerStorageDataProvider());
        registration.addBlockEntityData(GRID, AEEnergizingBlockEntity.class, new GridNodeStateDataProvider());
    }

    @Override
    public void registerClient(ClientRegistration registration) {
        if (com.appliedmoremachines.ModBlocks.AE_ENERGIZING_ORB == null) return;
        registration.addBlockEntityBody(AEEnergizingBlockEntity.class, AEEnergizingBlock.class,
                POWER, new PowerStorageDataProvider());
        registration.addBlockEntityBody(AEEnergizingBlockEntity.class, AEEnergizingBlock.class,
                GRID, new GridNodeStateDataProvider());
    }
}
