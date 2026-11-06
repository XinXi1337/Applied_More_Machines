package com.appliedmoremachines.integration;

import appeng.api.integrations.igtooltip.ClientRegistration;
import appeng.api.integrations.igtooltip.CommonRegistration;
import appeng.api.integrations.igtooltip.TooltipProvider;
import appeng.integration.modules.igtooltip.blocks.GridNodeStateDataProvider;
import appeng.integration.modules.igtooltip.blocks.PowerStorageDataProvider;
import com.appliedmoremachines.AppliedMoreMachines;
import com.appliedmoremachines.CraftingCoreBlock;
import com.appliedmoremachines.CraftingCoreBlockEntity;
import net.minecraft.resources.ResourceLocation;

/** Distinct addon IDs avoid collisions with both AE2 and the altar's Jade rows. */
public final class CoreTooltipProvider implements TooltipProvider {
    private static final ResourceLocation POWER = ResourceLocation.fromNamespaceAndPath(AppliedMoreMachines.MOD_ID, "core_power_storage");
    private static final ResourceLocation GRID = ResourceLocation.fromNamespaceAndPath(AppliedMoreMachines.MOD_ID, "core_grid_node");
    @Override public void registerCommon(CommonRegistration registration) {
        registration.addBlockEntityData(POWER, CraftingCoreBlockEntity.class, new PowerStorageDataProvider());
        registration.addBlockEntityData(GRID, CraftingCoreBlockEntity.class, new GridNodeStateDataProvider());
    }
    @Override public void registerClient(ClientRegistration registration) {
        registration.addBlockEntityBody(CraftingCoreBlockEntity.class, CraftingCoreBlock.class, POWER, new PowerStorageDataProvider());
        registration.addBlockEntityBody(CraftingCoreBlockEntity.class, CraftingCoreBlock.class, GRID, new GridNodeStateDataProvider());
    }
}
