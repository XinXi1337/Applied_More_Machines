package com.appliedmoremachines.power;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEKey;
import appeng.api.storage.StorageCells;
import appeng.api.storage.cells.StorageCell;
import com.appliedmoremachines.integration.FactoryFluxEnergy;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

/** Use registered cell handlers, including addon and multi-type cells; never identify disks by name. */
public final class HiddenPowerCell {
    private HiddenPowerCell() {}
    public static AEKey key() { return FactoryFluxEnergy.feKey(); }
    public static boolean accepts(ItemStack stack) {
        if (stack.isEmpty() || ModList.get() == null || !ModList.get().isLoaded("appflux")) return false;
        // Probe a copy: badly behaved third-party handlers cannot alter the player's held stack.
        StorageCell cell = StorageCells.getCellInventory(stack.copyWithCount(1), null);
        if (cell == null) return false;
        var source = IActionSource.empty();
        if (cell.extract(key(), 1, Actionable.SIMULATE, source) > 0
                || cell.insert(key(), 1, Actionable.SIMULATE, source) > 0) return true;
        // Full multi-type disks can have no free space and no FE yet. Probe a pristine disk too.
        // Never empty a copied disk: an addon may keep its contents in shared world storage.
        StorageCell empty = StorageCells.getCellInventory(new ItemStack(stack.getItem()), null);
        return empty != null && empty.insert(key(), 1, Actionable.SIMULATE, source) > 0;
    }
}
