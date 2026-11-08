package com.appliedmoremachines.inscriber;

import appeng.menu.AEBaseMenu;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;

public final class InscriberOutputActions {
    private InscriberOutputActions() {}

    public static boolean swap(AEBaseMenu menu, int slotId, int button, Player player) {
        if (slotId < 0 || slotId >= menu.slots.size() || !(menu.slots.get(slotId) instanceof InscriberOutputSlot slot)
                || slot.getItem().getCount() <= slot.getItem().getMaxStackSize()) return false;
        if ((button < 0 || button >= 9) && button != Inventory.SLOT_OFFHAND) return true;
        // Match AE2's locked-offhand rule and vanilla's output-only restriction.
        if (menu.isPlayerInventorySlotLocked(Inventory.SLOT_OFFHAND) || !slot.mayPickup(player)) return true;
        if (player.getInventory().getItem(button).isEmpty()) {
            var taken = slot.remove(slot.getItem().getMaxStackSize());
            player.getInventory().setItem(button, taken);
            slot.onTake(player, taken);
        }
        return true;
    }
}
