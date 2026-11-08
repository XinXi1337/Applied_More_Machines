package com.appliedmoremachines;

import com.appliedmoremachines.brewing.BrewingUpgrades;
import net.minecraft.world.InteractionResult;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

public final class BrewingUpgradeInteraction {
    private BrewingUpgradeInteraction() {}
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        var player = event.getEntity(); var level = event.getLevel(); var held = event.getItemStack();
        if (!player.isShiftKeyDown() || player.isSpectator() || !player.getAbilities().mayBuild
                || !BrewingUpgrades.isCard(held) || !level.mayInteract(player, event.getPos())
                || !player.mayUseItemAt(event.getPos(), event.getHitVec().getDirection(), held)
                || !(level.getBlockEntity(event.getPos()) instanceof AEBrewingBlockEntity machine)) return;
        if (!level.isClientSide) {
            int installed = machine.installUpgradeCards(held);
            if (!player.getAbilities().instabuild) held.shrink(installed);
        }
        event.setCanceled(true); event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide));
    }
}
