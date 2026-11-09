package com.appliedmoremachines.integration;

import appeng.api.integrations.igtooltip.ClientRegistration;
import appeng.api.integrations.igtooltip.CommonRegistration;
import appeng.api.integrations.igtooltip.TooltipProvider;
import appeng.integration.modules.igtooltip.blocks.GridNodeStateDataProvider;
import appeng.integration.modules.igtooltip.blocks.PowerStorageDataProvider;
import com.appliedmoremachines.AppliedMoreMachines;
import com.appliedmoremachines.AEHiddenBackupPowerBlock;
import com.appliedmoremachines.AEHiddenBackupPowerBlockEntity;
import net.minecraft.resources.ResourceLocation;

/** AE2 supplies Jade's storage and grid-state rows, with no hard Jade dependency. */
public final class HiddenPowerTooltipProvider implements TooltipProvider {
    private static final ResourceLocation POWER = ResourceLocation.fromNamespaceAndPath(
            AppliedMoreMachines.MOD_ID, "hidden_power_storage");
    private static final ResourceLocation GRID = ResourceLocation.fromNamespaceAndPath(
            AppliedMoreMachines.MOD_ID, "hidden_grid_node");

    @Override
    public void registerCommon(CommonRegistration registration) {
        if (com.appliedmoremachines.ModBlocks.AE_HIDDEN_BACKUP_POWER == null) return;
        // AE2's base-class hook reuses ae2:* data IDs for every class; Jade requires unique IDs.
        registration.addBlockEntityData(POWER, AEHiddenBackupPowerBlockEntity.class, new PowerStorageDataProvider());
        registration.addBlockEntityData(GRID, AEHiddenBackupPowerBlockEntity.class, new GridNodeStateDataProvider());
    }

    @Override
    public void registerClient(ClientRegistration registration) {
        if (com.appliedmoremachines.ModBlocks.AE_HIDDEN_BACKUP_POWER == null) return;
        registration.addBlockEntityBody(AEHiddenBackupPowerBlockEntity.class, AEHiddenBackupPowerBlock.class,
                POWER, new PowerStorageDataProvider());
        registration.addBlockEntityBody(AEHiddenBackupPowerBlockEntity.class, AEHiddenBackupPowerBlock.class,
                GRID, new GridNodeStateDataProvider());
    }
}
